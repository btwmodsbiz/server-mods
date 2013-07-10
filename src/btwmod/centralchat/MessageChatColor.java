package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatColor extends Message {
	
	public final String TYPE = "chatcolor";
	
	public final String action;
	public final String username;
	public final String color;
	
	public MessageChatColor(String action, String username) {
		this(action, username, null);
	}
	
	public MessageChatColor(String action, String username, String color) {
		this.action = action;
		this.username = username;
		this.color = color;
	}
	
	public MessageChatColor(JsonObject json) {
		JsonElement color = json.get("color");
		
		this.action = json.get("action").getAsString();
		this.username = json.get("username").getAsString();
		this.color = color != null && color.isJsonPrimitive() ? color.getAsString() : null;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("action", this.action);
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
		JsonObject json = toJson();
		String username = this.username;
		
		// Force user ID for those authenticated as users.
		if (ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(config.clientType))
			username = config.id;
		
		if ("set".equalsIgnoreCase(action)) {
			server.setChatColor(username, color);
			server.save();
		}
		
		// Fail if not action is not 'get' instead of 'set'.
		else if (!"get".equalsIgnoreCase(action))
			return;
		
		json.addProperty("color", server.getChatColor(username));
	}

}
