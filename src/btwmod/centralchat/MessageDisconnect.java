package btwmod.centralchat;

import org.java_websocket.WebSocket;

import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageDisconnect extends MessageUserMessage {
	
	public final String TYPE = "disconnect";
	
	public final String reason;

	public MessageDisconnect(JsonObject json) {
		super(json);
		
		JsonElement reason = json.get("reason");
		this.reason = reason != null && reason.isJsonPrimitive() ? reason.getAsString() : null;
	}

	public MessageDisconnect(String username, String gateway, String reason, String color, String alias) {
		super(username, gateway, color, alias);
		this.reason = reason;
	}

	public MessageDisconnect(String username, String gateway, String reason) {
		super(username, gateway);
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
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		super.handleAsServer(server, conn, config);
		server.removeLoggedInUser(gateway, username);
	}
	
	@Override
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(false) + " left chat" + (gateway == null ? "" : " on " + gateway) + ".";
	}
	
	@Override
	protected String getLoggedMessage() {
		return username + " left chat" + (gateway == null ? "" : " on " + gateway) + ".";
	}
}