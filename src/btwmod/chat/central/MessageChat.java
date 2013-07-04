package btwmod.chat.central;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class MessageChat extends Message {
	public final String user;
	public final String message;
	
	public MessageChat(String user, String message) {
		super("chat");
		this.user = user;
		this.message = message;
	}
	
	public MessageChat(JsonObject json) {
		this(json.get("user").getAsString(), json.get("message").getAsString());
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("user", this.user);
		obj.addProperty("message", this.message);
		return obj;
	}

	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return super.canSendMessage(config) || ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(config.clientType);
	}

	@Override
	public void handleMessage(ChatServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		
		// Force user ID for those authenticated as users.
		if ("user".equalsIgnoreCase(config.clientType))
			json.addProperty("user", config.id);
		
		server.sendToAll(json.toString());
	}
}