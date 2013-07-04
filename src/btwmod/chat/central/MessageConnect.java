package btwmod.chat.central;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class MessageConnect extends Message {
	public final String user;
	public final String server;
	
	public MessageConnect(String user, String server) {
		super("connect");
		this.user = user;
		this.server = server;
	}
	
	public MessageConnect(JsonObject json) {
		this(json.get("user").getAsString(), json.get("server").getAsString());
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("user", this.user);
		obj.addProperty("server", this.server);
		return obj;
	}

	@Override
	public void handleMessage(ChatServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		System.out.println(json.toString());
		server.sendToAll(json.toString());
	}
}