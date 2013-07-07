package btwmod.centralchat;

import btwmods.Util;

import com.google.gson.JsonObject;

public class MessageDisconnect extends MessageConnect {
	
	public final String reason;

	public MessageDisconnect(JsonObject json) {
		super(json);
		this.reason = json.get("reason").getAsString();
	}

	public MessageDisconnect(String username, String server, String reason, String color, String alias) {
		super(username, server, color, alias);
		this.reason = reason;
	}

	public MessageDisconnect(String username, String server, String reason) {
		super(username, server);
		this.reason = reason;
	}

	@Override
	public String getType() {
		return "disconnect";
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("reason", this.reason);
		return obj;
	}
	
	@Override
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(Util.COLOR_YELLOW) + " left chat on " + server + ".";
	}
}