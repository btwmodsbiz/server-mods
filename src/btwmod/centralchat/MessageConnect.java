package btwmod.centralchat;

import btwmods.ChatAPI;
import btwmods.Util;

import com.google.gson.JsonObject;

public class MessageConnect extends MessageUser {
	
	public final String server;

	public MessageConnect(JsonObject json) {
		super(json);
		this.server = json.get("server").getAsString();
	}

	public MessageConnect(String username, String server, String color, String alias) {
		super(username, color, alias);
		this.server = server;
	}

	public MessageConnect(String username, String server) {
		super(username);
		this.server = server;
	}

	@Override
	public String getType() {
		return "connect";
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("server", this.server);
		return obj;
	}
	
	@Override
	public void handleAsClient() {
		ChatAPI.sendChatToAllPlayers(getFormattedMessage());
	}
	
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(Util.COLOR_YELLOW) + " joined chat on " + server + ".";
	}
}