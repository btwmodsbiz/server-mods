package btwmod.centralchat.message;

import com.google.gson.JsonObject;

public class MessageEmote extends MessageChat {
	
	public static final String TYPE = "emote";
	
	public MessageEmote(JsonObject json) {
		super(json);
	}

	public MessageEmote(String username, String gateway, String message, String color, String alias) {
		super(username, gateway, message, color, alias);
	}

	public MessageEmote(String username, String gateway, String message) {
		super(username, gateway, message);
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	protected String getFormattedMessage() {
		return "* " + getDisplayUsername(true) + " " + message;
	}
	
	@Override
	protected String getLoggedMessage() {
		return "* " + username + " " + message;
	}
}
