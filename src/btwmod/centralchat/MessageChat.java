package btwmod.centralchat;

import net.minecraft.server.MinecraftServer;
import btwmods.ChatAPI;

import com.google.gson.JsonObject;

public class MessageChat extends MessageUser {
	
	public final String TYPE = "chat";
	
	public final String message;
	
	public MessageChat(JsonObject json) {
		super(json);
		this.message = json.get("message").getAsString();
	}

	public MessageChat(String username, String message, String color, String alias) {
		super(username, color, alias);
		this.message = message;
	}

	public MessageChat(String username, String message) {
		super(username);
		this.message = message;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("message", this.message);
		return obj;
	}
	
	@Override
	public void handleAsClient() {
		MinecraftServer.getServer().getLogAgent().func_98233_a(getLoggedMessage());
		ChatAPI.sendChatToAllPlayers(username, getFormattedMessage());
	}
	
	protected String getFormattedMessage() {
		return "<" + getDisplayUsername() + "> " + message;
	}
	
	protected String getLoggedMessage() {
		return "<" + username + "> " + message;
	}
}
