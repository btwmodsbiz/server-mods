package btwmod.centralchat;

import net.minecraft.server.MinecraftServer;
import btwmods.ChatAPI;

import com.google.gson.JsonObject;

public class MessageDeath extends MessageChat {
	
	public final String TYPE = "death";

	public MessageDeath(JsonObject json) {
		super(json);
	}

	public MessageDeath(String username, String message, String color, String alias) {
		super(username, message, color, alias);
	}

	public MessageDeath(String username, String message) {
		super(username, message);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void handleAsGateway(IGateway gateway) {
		MinecraftServer.getServer().getLogAgent().func_98233_a(getLoggedMessage());
		String message = getFormattedMessage();
		ChatAPI.sendChatToAllPlayers(message);
		gateway.addRestorableChat(message);
	}
	
	protected String getFormattedMessage() {
		return message.replace(username, getDisplayUsername(false));
	}
	
	@Override
	protected String getLoggedMessage() {
		return message;
	}
}
