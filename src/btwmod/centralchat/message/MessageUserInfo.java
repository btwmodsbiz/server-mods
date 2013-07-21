package btwmod.centralchat.message;

import org.java_websocket.WebSocket;

import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

import com.google.gson.JsonObject;

public class MessageUserInfo extends MessageUser {

	public static final String TYPE = "userinfo";

	public MessageUserInfo(String username, String gateway) {
		super(username, gateway, null, null);
	}

	public MessageUserInfo(String username, String gateway, String color, String alias) {
		super(username, gateway, color, alias);
	}
	
	public MessageUserInfo(JsonObject json) {
		super(json);
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		
	}
}
