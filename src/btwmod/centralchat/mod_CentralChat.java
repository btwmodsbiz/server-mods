package btwmod.centralchat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import net.minecraft.src.EntityPlayer;

import btwmod.centralchat.message.Message;
import btwmod.centralchat.message.MessageChat;
import btwmod.centralchat.message.MessageConnect;
import btwmod.centralchat.message.MessageDeath;
import btwmod.centralchat.message.MessageDisconnect;
import btwmod.centralchat.message.MessageEmote;
import btwmods.ChatAPI;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.chat.IPlayerChatListener;
import btwmods.chat.PlayerChatEvent;
import btwmods.io.Settings;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;
import btwmods.server.ITickListener;
import btwmods.server.TickEvent;
import btwmods.util.CaselessKey;
import btwmods.util.ValuePair;

public class mod_CentralChat implements IMod, IPlayerChatListener, ITickListener, IGateway, IPlayerInstanceListener {
	
	private volatile WSGateway wsGateway = null;
	private volatile boolean useGateway = false;
	
	private String serverHost = null;
	private String serverId = null;
	private String serverKey = null;
	
	private int connectAttempts = 1;
	private int connectAttemptsTillFailover = 5;
	
	private long connectWaitSeconds = 1;
	private long connectBufferMax = 10;
	private long connectSecondsPerBuffer = 10;
	
	private URI uri;
	private final BlockingDeque<Message> messageQueue = new LinkedBlockingDeque<Message>();
	
	private boolean startThreads = true;
	private CommandChatColor commandChatColor;
	private CommandChatAlias commandChatAlias;

	public int chatRestoreLines = 30;
	private long chatRestoreTimeout = 20L;
	private Deque<ValuePair<String, Long>> chatRestoreBuffer = new ArrayDeque<ValuePair<String, Long>>();
	private Map<String, Long> loginTime = new HashMap<String, Long>();
	private Map<String, Long> logoutTime = new HashMap<String, Long>();
	private volatile Set<CaselessKey> usernames = new HashSet<CaselessKey>();
	
	@Override
	public String getName() {
		return "Central Chat";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		serverHost = settings.get("serverHost");
		serverId = settings.get("serverId");
		serverKey = settings.get("serverKey");
		chatRestoreLines = settings.getInt("chatRestoreLines", chatRestoreLines);
		chatRestoreTimeout = settings.getLong("chatRestoreTimeout", chatRestoreTimeout);
		
		if (serverHost == null || serverHost.trim().length() == 0)
			return;
		
		if (serverId == null || serverId.trim().length() == 0)
			return;
		
		if (serverKey == null || serverKey.trim().length() == 0)
			return;
		
		String urlString = "ws://" + serverHost + "/gateway/" + serverId + "/" + serverKey;
		try {
			uri = new URI(urlString);
			
			ChatAPI.addListener(this);
			ServerAPI.addListener(this);
			PlayerAPI.addListener(this);
			CommandsAPI.registerCommand(commandChatColor = new CommandChatColor(this), this);
			CommandsAPI.registerCommand(commandChatAlias = new CommandChatAlias(this), this);
		}
		catch (URISyntaxException e) {
			ModLoader.outputError(e, "Invalid URI: " + urlString);
		}
	}

	@Override
	public void unload() throws Exception {
		ChatAPI.removeListener(this);
		ServerAPI.removeListener(this);
		PlayerAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandChatColor);
		CommandsAPI.unregisterCommand(commandChatAlias);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerChatAction(PlayerChatEvent event) {
		switch (event.type) {
			case AUTO_COMPLETE:
			case GLOBAL:
			case HANDLE_CHAT:
			case SEND_TO_PLAYER_ATTEMPT:
				break;
				
			case HANDLE_DEATH_MESSAGE:
				queueMessage(new MessageDeath(event.username, serverId, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_GLOBAL:
				queueMessage(new MessageChat(event.username, serverId, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_EMOTE:
				queueMessage(new MessageEmote(event.username, serverId, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_LOGIN_MESSAGE:
				synchronized (usernames) {
					usernames.add(new CaselessKey(event.username));
				}
				
				queueMessage(new MessageConnect(event.username, serverId));
				event.markHandled();
				break;
				
			case HANDLE_LOGOUT_MESSAGE:
				synchronized (usernames) {
					usernames.remove(new CaselessKey(event.username));
				}
				
				queueMessage(new MessageDisconnect(event.username, serverId, null));
				event.markHandled();
				break;
		}
	}
	
	public void queueMessage(Message message) {
		messageQueue.add(message);
	}
	
	private void startConnectionThread() {
		new Thread(new ConnectionWatcher(this), getName() + ": Connection Watcher").start();
	}
		
	private void startQueueThread() {
		new Thread(new QueueWatcher(this), getName() + ": Queue Watcher").start();
	}

	@Override
	public void onTick(TickEvent event) {
		if (startThreads && event.getType() == TickEvent.TYPE.START) {
			startThreads = false;
			startConnectionThread();
			startQueueThread();
		}
	}
	
	@Override
	public String getId() {
		return serverId;
	}

	@Override
	public void addRestorableChat(String chat) {
		chatRestoreBuffer.add(new ValuePair(chat, new Long(System.currentTimeMillis())));
		
		if (chatRestoreBuffer.size() > chatRestoreLines)
			chatRestoreBuffer.pollFirst();
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		long currentTimeMillis = System.currentTimeMillis();
		
		if (event.getType() == PlayerInstanceEvent.TYPE.LOGIN) {
			String userKey = event.getPlayerInstance().username.toLowerCase();
			
			Long loginSessionStart = loginTime.get(userKey);
			Long lastLogout = logoutTime.get(userKey);
			
			// Reset the login time if they've been logged out longer than the restore timeout.
			if (loginTime == null || lastLogout == null || lastLogout.longValue() < currentTimeMillis - chatRestoreTimeout * 1000L) {
				loginTime.put(userKey, loginSessionStart = new Long(currentTimeMillis));
			}
			else {
				EntityPlayer player = event.getPlayerInstance();
				for (ValuePair<String, Long> message : chatRestoreBuffer) {
					if (message.value.longValue() >= loginSessionStart)
						player.sendChatToPlayer(message.key);
				}
			}
				
		}
		else if (event.getType() == PlayerInstanceEvent.TYPE.LOGOUT_POST) {
			logoutTime.put(event.getPlayerInstance().username.toLowerCase(), new Long(System.currentTimeMillis()));
		}
	}
	
	private class ConnectionWatcher implements Runnable {
		
		private final IGateway gateway;
		
		public ConnectionWatcher(IGateway gateway) {
			this.gateway = gateway;
		}

		@Override
		public void run() {
			long retryBuffer = 1;
			long lastConnectTime = System.currentTimeMillis();
			
			while (!Thread.interrupted()) {
				long currentTime = System.currentTimeMillis();
				retryBuffer = Math.min(connectBufferMax, retryBuffer + (long)Math.floor((currentTime - lastConnectTime) / (connectSecondsPerBuffer * 1000L)));
				
				try {
					if (retryBuffer > 0) {
						retryBuffer--;
						
						wsGateway = new WSGateway(gateway, uri);
						if (wsGateway.connectBlocking()) {
							wsGateway.awaitClose();
							
							if (useGateway) {
								ModLoader.outputInfo("Disconnected from central chat server.");
								ChatAPI.removeAllAliases();
							}
						}
					}
				} catch (InterruptedException e) {
					
				} catch (RuntimeException e) {
					// TODO: Log runtime exceptions.
				}
				
				gateway.onWaitForReconnect();
				
				try {
					Thread.sleep(connectWaitSeconds * 1000L);
				}
				catch (InterruptedException e) {
					
				}
			}
		}
	}
	
	private class QueueWatcher implements Runnable {
		
		private final IGateway gateway;
		
		public QueueWatcher(IGateway gateway) {
			this.gateway = gateway;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Message message = messageQueue.take();
					
					if (useGateway) {
						try {
							wsGateway.send(message.toJson().toString());
						}
						catch (Exception ex) {
							messageQueue.addFirst(message);
							
							// Wait if sending it via the client failed.
							try {
								Thread.sleep(250L);
							} catch (InterruptedException e) {
								
							}
						}
					}
					else {
						message.handleAsGateway(gateway);
					}
				} catch (InterruptedException e) {
					
				}
			}
		}
	}
	
	@Override
	public String[] getUsernames() {
		int len;
		CaselessKey[] list;
		
		synchronized (usernames) {
			len = usernames.size();
			list = usernames.toArray(new CaselessKey[len]);
		}

		String[] ret = new String[len];
		for (int i = 0; i < len; i++) {
			ret[i] = list[i].key;
		}
		return ret;
	}
	
	@Override
	public void setAlias(String username, String alias) {
		if (alias == null)
			ChatAPI.removeAlias(username);
		else
			ChatAPI.setAlias(username, alias);
	}
	
	@Override
	public void onSuccessfulConnect() {
		connectAttempts = 0;
	}
	
	@Override
	public void onWaitForReconnect() {
		if (useGateway && connectAttempts++ >= connectAttemptsTillFailover) {
			useGateway = false;

			ModLoader.outputInfo("Failing over to non-gateway mode.");
			ChatAPI.sendChatToAllPlayers(Util.COLOR_YELLOW + "Disconnected from central chat server.");
		}
	}
}