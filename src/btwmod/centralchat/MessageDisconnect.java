package btwmod.centralchat;

import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageDisconnect extends MessageConnect {
	
	public final String TYPE = "disconnect";
	
	public final String reason;

	public MessageDisconnect(JsonObject json) {
		super(json);
		
		JsonElement reason = json.get("reason");
		this.reason = reason != null && reason.isJsonPrimitive() ? reason.getAsString() : null;
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
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		
		if (this.reason == null)
			obj.remove("reason");
		else
			obj.addProperty("reason", this.reason);
		
		return obj;
	}
	
	@Override
	protected void toServer(ChatServer server) {
		server.onDisconnectMessage(this);
	}
	
	@Override
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(false) + " left chat" + (server == null ? "" : " on " + server) + ".";
	}
	
	@Override
	protected String getLoggedMessage() {
		return username + " left chat" + (server == null ? "" : " on " + server) + ".";
	}
}