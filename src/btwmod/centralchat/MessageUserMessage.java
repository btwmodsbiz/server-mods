package btwmod.centralchat;

import org.java_websocket.WebSocket;

import net.minecraft.server.MinecraftServer;
import btwmods.ChatAPI;

import com.google.gson.JsonObject;

public abstract class MessageUserMessage extends MessageUser {
	
	public MessageUserMessage(JsonObject json) {
		super(json);
	}

	public MessageUserMessage(String username, String gateway, String color, String alias) {
		super(username, gateway, color, alias);
	}

	public MessageUserMessage(String username, String gateway) {
		super(username, gateway, null, null);
	}
	
	@Override
	public void handleAsGateway(IGateway gateway) {
		MinecraftServer.getServer().getLogAgent().func_98233_a(getLoggedMessage());
		
		String message = getFormattedMessage();
		ChatAPI.sendChatToAllPlayers(username, message);
		gateway.addRestorableChat(message);
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		server.sendToAll(toJsonCleaned(server, config).toString());
	}
	
	protected abstract String getFormattedMessage();
	
	protected abstract String getLoggedMessage();
}
