package btwmod.centralchat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.minecraft.server.MinecraftServer;

import btwmods.ChatAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.chat.IPlayerChatListener;
import btwmods.chat.PlayerChatEvent;
import btwmods.io.Settings;
import btwmods.server.ITickListener;
import btwmods.server.TickEvent;

public class mod_CentralChat implements IMod, IPlayerChatListener, ITickListener {
	
	private ChatClient chatClient = null;
	
	public int disconnectTimeout = 5;
	private int ticksSinceDisconnect = 0;
	private boolean queueMessages = false;
	
	public BlockingQueue<Message> outQueue = new LinkedBlockingQueue<Message>();
	public BlockingQueue<Message> inQueue = new LinkedBlockingQueue<Message>();

	@Override
	public String getName() {
		return "Central Chat";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		try {
			chatClient = new ChatClient(inQueue, new URI("ws://localhost:8585/server/bts1/1234"));
			ChatAPI.addListener(this);
			ServerAPI.addListener(this);
			chatClient.connect();
			
			new Thread(new QueueWatcher(outQueue, new IMessageHandler() {
				@Override
				public void handleMesage(Message message) {
					System.out.println("Handle OUT Msg: " + message.toJson().toString());
					chatClient.send(message.toJson().toString());
				}
			})).start();
			
			new Thread(new QueueWatcher(inQueue, new IMessageHandler() {
				@Override
				public void handleMesage(Message message) {
					System.out.println("Handle IN Msg: " + message.toJson().toString());
					if (message.getClass() == MessageChat.class) {
						MessageChat chatMessage = (MessageChat)message;
						
						String username = chatMessage.alias == null
								? chatMessage.username
								: chatMessage.alias;
						
						// Attempt to get the user's setting.
						String color = chatMessage.color;
						
						//if (color != null)
						//	color = chatClient.getColorChar(color);
						
						ChatAPI.sendChatToAllPlayers(chatMessage.username, "<" +
							(color == null ? username : color + username + Util.COLOR_WHITE) +
							"> " + chatMessage.message);
					}
				}
			})).start();
		}
		catch (Exception e) {
			ModLoader.outputError(e, "Failed to start chat client.");
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
				break;
			case GLOBAL:
				break;
			case HANDLE_CHAT:
				if (queueMessages) {
					System.out.println("Queueing out from " + event.username + ": " + event.getMessage());
					outQueue.add(new MessageChat(event.username, event.getMessage()));
					event.markHandled();
				}
				break;
			case HANDLE_DEATH_MESSAGE:
				break;
			case HANDLE_EMOTE:
				break;
			case HANDLE_GLOBAL:
				break;
			case HANDLE_LOGIN_MESSAGE:
				break;
			case HANDLE_LOGOUT_MESSAGE:
				break;
			case SEND_TO_PLAYER_ATTEMPT:
				break;
		}
	}

	@Override
	public void onTick(TickEvent event) {
		if (event.getType() == TickEvent.TYPE.START) {
			if (queueMessages && !chatClient.connected.get()) {
				if (++ticksSinceDisconnect > disconnectTimeout * 20) {
					queueMessages = false;
					
					ChatAPI.sendChatToAllPlayers(Util.COLOR_YELLOW + "Disconnected from central chat server.");
					ModLoader.outputInfo("Disconnected from central chat server.");
					
					List<Message> messages = new ArrayList<Message>();
					outQueue.drainTo(messages);
					// TODO: Process drained messages.
				}
			}
			else if (!queueMessages && chatClient.connected.get()) {
				if (ticksSinceDisconnect > 0) {
					ChatAPI.sendChatToAllPlayers(Util.COLOR_YELLOW + "Reconnected to central chat server.");
					ModLoader.outputInfo("Reconnected to central chat server.");
				}
				else {
					ModLoader.outputInfo("Connected to central chat server.");
				}
				
				ticksSinceDisconnect = 0;
				queueMessages = true;
			}
		}
	}
}
