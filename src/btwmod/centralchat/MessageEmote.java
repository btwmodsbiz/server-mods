package btwmod.centralchat;

import com.google.gson.JsonObject;

public class MessageEmote extends MessageChat {
	
	public final String TYPE = "emote";
	
	public MessageEmote(JsonObject json) {
		super(json);
	}

	public MessageEmote(String username, String message, String color, String alias) {
		super(username, message, color, alias);
	}

	public MessageEmote(String username, String message) {
		super(username, message);
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	protected String getFormattedMessage() {
		return "* " + getDisplayUsername() + " " + message;
	}
}
