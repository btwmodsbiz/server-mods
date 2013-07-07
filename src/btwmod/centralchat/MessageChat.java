package btwmod.centralchat;

import btwmods.ChatAPI;

import com.google.gson.JsonObject;

public class MessageChat extends MessageUser {
	
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
		return "chat";
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("message", this.message);
		return obj;
	}
	
	@Override
	public void handleAsClient() {
		ChatAPI.sendChatToAllPlayers(username, getFormattedMessage());
	}
	
	protected String getFormattedMessage() {
		return "<" + getDisplayUsername() + "> " + message;
	}
}
