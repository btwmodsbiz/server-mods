package btwmod.centralchat;

import org.java_websocket.WebSocket;
import btwmods.Util;

import com.google.gson.JsonObject;

public class MessageConnect extends MessageUserMessage {
	
	public static final String TYPE = "connect";

	@Override
	public String getType() {
		return TYPE;
	}

	public MessageConnect(JsonObject json) {
		super(json);
	}

	public MessageConnect(String username, String gateway) {
		super(username, gateway, null, null);
	}

	public MessageConnect(String username, String gateway, String color, String alias) {
		super(username, gateway, color, alias);
	}
	
	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		super.handleAsServer(server, conn, config);
		server.addLoggedInUser(this.gateway, username);
	}
	
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(false) + " joined chat" + (gateway == null ? "" : " on " + gateway) + ".";
	}
	
	protected String getLoggedMessage() {
		return username + " joined chat" + (gateway == null ? "" : " on " + gateway) + ".";
	}
}