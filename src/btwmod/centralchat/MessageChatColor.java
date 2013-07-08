package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatColor extends Message {
	
	public final String username;
	public final String color;
	
	public MessageChatColor(String username) {
		this(username, null);
	}
	
	public MessageChatColor(String username, String color) {
		this.username = username;
		this.color = color;
	}
	
	public MessageChatColor(JsonObject json) {
		JsonElement color = json.get("color");
		
		this.username = json.get("username").getAsString();
		this.color = color != null && color.isJsonPrimitive() ? color.getAsString() : null;
	}

	@Override
	public String getType() {
		return "chatcolor";
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("username", this.username);
		obj.addProperty("color", this.color);
		return obj;
	}

	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return super.canSendMessage(config) || ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(config.clientType);
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		String username = this.username;
		
		// Force user ID for those authenticated as users.
		if (ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(config.clientType))
			username = config.id;
		
		server.setChatColor(username, color);
	}

}
