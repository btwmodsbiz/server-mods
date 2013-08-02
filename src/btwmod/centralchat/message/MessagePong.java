package btwmod.centralchat.message;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import btwmod.centralchat.IGateway;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

public class MessagePong extends MessagePing {

	public static final String TYPE = "pong";

	public MessagePong(int id) {
		super(id);
	}
	
	public MessagePong(JsonObject json) {
		super(json);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		server.onPong(conn, config, this);
	}

	@Override
	public void handleAsGateway(IGateway gateway) {
		
	}
}
