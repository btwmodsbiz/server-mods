package btwmod.chat.central;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class MessageDisconnect extends Message {
	public final String user;
	public final String server;
	public final String reason;
	
	public MessageDisconnect(String user, String server) {
		this(user, server, null);
	}
	
	public MessageDisconnect(String user, String server, String reason) {
		super("chat");
		this.user = user;
		this.server = server;
		this.reason = reason;
	}
	
	public MessageDisconnect(JsonObject json) {
		this(json.get("user").getAsString(), json.get("server").getAsString(), json.has("reason") ? json.get("reason").getAsString() : null);
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("user", this.user);
		obj.addProperty("server", this.server);
		obj.addProperty("reason", this.reason);
		return obj;
	}

	@Override
	public void handleMessage(ChatServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		System.out.println(json.toString());
		server.sendToAll(json.toString());
	}
}