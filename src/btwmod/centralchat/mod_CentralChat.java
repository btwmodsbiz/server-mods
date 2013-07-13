package btwmod.centralchat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import net.minecraft.src.EntityPlayer;

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
import btwmods.util.ValuePair;

public class mod_CentralChat implements IMod, IPlayerChatListener, ITickListener, IMessageClient, IPlayerInstanceListener {
	
	private volatile ChatClient chatClient = null;
	private volatile boolean doFailover = true;
	
	private String serverUri = null;
	private String serverName = null;
	
	private long reconnectWait = 1;
	private long reconnectWaitLong = 10;
	private int connectAttemptsBeforeFailover = 6;
	
	private URI uri;
	private final BlockingDeque<Message> messageQueue = new LinkedBlockingDeque<Message>();
	private int connectAttempts = 0;
	
	private boolean startThreads = true;
	private CommandChatColor commandChatColor;
	private CommandChatAlias commandChatAlias;

	public int chatRestoreLines = 30;
	private long chatRestoreTimeout = 20L;
	private Deque<ValuePair<String, Long>> chatRestoreBuffer = new ArrayDeque<ValuePair<String, Long>>();
	private Map<String, Long> loginTime = new HashMap<String, Long>();
	private Map<String, Long> logoutTime = new HashMap<String, Long>();
	
	@Override
	public String getName() {
		return "Central Chat";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		serverUri = settings.get("serverUri");
		serverName = settings.get("serverName");
		chatRestoreLines = settings.getInt("chatRestoreLines", chatRestoreLines);
		chatRestoreTimeout = settings.getLong("chatRestoreTimeout", chatRestoreTimeout);
		
		if (serverUri == null || serverUri.trim().length() == 0)
			return;
		
		if (serverName == null || serverName.trim().length() == 0)
			return;
		
		try {
			uri = new URI(serverUri);
			
			ChatAPI.addListener(this);
			ServerAPI.addListener(this);
			PlayerAPI.addListener(this);
			CommandsAPI.registerCommand(commandChatColor = new CommandChatColor(this), this);
			CommandsAPI.registerCommand(commandChatAlias = new CommandChatAlias(this), this);
		}
		catch (URISyntaxException e) {
			ModLoader.outputError(e, "Invalid URI: " + serverUri);
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
				queueMessage(new MessageDeath(event.username, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_GLOBAL:
				queueMessage(new MessageChat(event.username, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_EMOTE:
				queueMessage(new MessageEmote(event.username, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_LOGIN_MESSAGE:
				queueMessage(new MessageConnect(event.username, serverName));
				event.markHandled();
				break;
				
			case HANDLE_LOGOUT_MESSAGE:
				queueMessage(new MessageDisconnect(event.username, serverName, null));
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
		
		private final IMessageClient messageClient;
		
		public ConnectionWatcher(IMessageClient messageClient) {
			this.messageClient = messageClient;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				connectAttempts++;
				chatClient = new ChatClient(messageClient, uri);
				
				try {
					if (chatClient.connectBlocking()) {
						doFailover = false;
						connectAttempts = 0;
						ModLoader.outputInfo("Connected to central chat server.");
						ChatAPI.sendChatToAllPlayers(Util.COLOR_YELLOW + "Connected to central chat server.");
						
						chatClient.awaitClose();
						ModLoader.outputInfo("Disconnected from central chat server.");
					}
					else if (connectAttempts <= connectAttemptsBeforeFailover) {
						Thread.sleep(reconnectWait * 1000L);
					}
					else {
						doFailover = true;
						if (connectAttempts == connectAttemptsBeforeFailover + 1) {
							ChatAPI.sendChatToAllPlayers(Util.COLOR_YELLOW + "Disconnected from central chat server.");
						}
						
						Thread.sleep(reconnectWaitLong * 1000L);
					}
				} catch (InterruptedException e) {
					
				}
			}
		}
	}
	
	private class QueueWatcher implements Runnable {
		
		private final IMessageClient messageClient;
		
		public QueueWatcher(IMessageClient messageClient) {
			this.messageClient = messageClient;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Message message = messageQueue.take();
					
					if (doFailover) {
						message.handleAsClient(messageClient);
					}
					else {
						try {
							chatClient.send(message.toJson().toString());
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
				} catch (InterruptedException e) {
					
				}
			}
		}
	}
}