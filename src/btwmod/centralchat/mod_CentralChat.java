package btwmod.centralchat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import btwmods.ChatAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.chat.IPlayerChatListener;
import btwmods.chat.PlayerChatEvent;
import btwmods.io.Settings;

public class mod_CentralChat implements IMod, IPlayerChatListener {
	
	private String serverUri = null;
	private String serverName = null;
	private MessageManager messageManager;
	
	public long reconnectWait = 1;
	public long reconnectWaitLong = 10;
	public int connectAttemptsBeforeFailover = 6;

	@Override
	public String getName() {
		return "Central Chat";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		serverUri = "ws://localhost:8585/server/bts1/1234"; //settings.get("serverUri");
		serverName = "Alpha"; //settings.get("serverName");
		
		if (serverUri == null || serverUri.trim().length() == 0)
			return;
		
		if (serverName == null || serverName.trim().length() == 0)
			return;
		
		try {
			messageManager = new MessageManager(new URI(serverUri));
			
			ChatAPI.addListener(this);
			ServerAPI.addListener(this);
			
			messageManager.start();
		}
		catch (URISyntaxException e) {
			ModLoader.outputError(e, "Invalid URI: " + serverUri);
		}
	}

	@Override
	public void unload() throws Exception {
		ChatAPI.removeListener(this);
		ServerAPI.removeListener(this);
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
				messageManager.queueMessage(new MessageDeath(event.username, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_GLOBAL:
				messageManager.queueMessage(new MessageChat(event.username, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_EMOTE:
				messageManager.queueMessage(new MessageEmote(event.username, event.getMessage()));
				event.markHandled();
				break;
				
			case HANDLE_LOGIN_MESSAGE:
				messageManager.queueMessage(new MessageConnect(event.username, serverName));
				event.markHandled();
				break;
				
			case HANDLE_LOGOUT_MESSAGE:
				messageManager.queueMessage(new MessageDisconnect(event.username, serverName, null));
				event.markHandled();
				break;
		}
	}
	
	private class MessageManager {
		
		private volatile ChatClient chatClient = null;
		private volatile boolean doFailover = true;
		
		private final URI uri;
		private final BlockingDeque<Message> messageQueue = new LinkedBlockingDeque<Message>();
		private int connectAttempts = 0;

		public MessageManager(URI uri) {
			this.uri = uri;
		}
		
		public MessageManager start() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.interrupted()) {
						connectAttempts++;
						ModLoader.outputInfo("Connecting to central chat server...");
						chatClient = new ChatClient(uri);
						
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
			}, getName() + ": Connection Watcher").start();
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.interrupted()) {
						try {
							Message message = messageQueue.take();
							
							if (doFailover) {
								message.handleAsClient();
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
			}, getName() + ": Queue Watcher").start();
			
			return this;
		}
		
		public void queueMessage(Message message) {
			messageQueue.add(message);
		}
	}
}