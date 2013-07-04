package btwmod.chat.central;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class MessageAuth extends Message {
	public final String action;
	public final String clientType;
	public final String id;
	public final String key;
	
	public MessageAuth(String action, String clientType, String id, String key) {
		super("auth");
		this.action = action;
		this.clientType = clientType;
		this.id = id;
		this.key = key;
	}
	
	public MessageAuth(JsonObject json) {
		this(
			json.get("action").getAsString(),
			json.get("clientType").getAsString(),
			json.get("id").getAsString(),
			json.get("key").getAsString()
		);
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("action", this.action);
		obj.addProperty("clientType", this.clientType);
		obj.addProperty("id", this.id);
		
		if (key != null)
		obj.addProperty("key", this.key);
		return obj;
	}

	@Override
	public void handleMessage(ChatServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		System.out.println(json.toString());
		server.sendToAll(json.toString());
	}
}