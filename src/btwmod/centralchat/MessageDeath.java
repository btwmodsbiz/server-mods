package btwmod.centralchat;

import btwmods.ChatAPI;

import com.google.gson.JsonObject;

public class MessageDeath extends MessageChat {

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
		return "death";
	}

	@Override
	public void handleAsClient() {
		ChatAPI.sendChatToAllPlayers(getFormattedMessage());
	}
	
	protected String getFormattedMessage() {
		return message.replace(username, getDisplayUsername());
	}
}
