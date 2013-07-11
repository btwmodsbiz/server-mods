package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatColor extends Message {
	
	public final String TYPE = "chatcolor";
	
	public final String action;
	public final String username;
	public final String color;
	
	// Only for responses
	public final Boolean success;
	public final String oldColor;
	
	public MessageChatColor(String action, String username) {
		this(action, username, null);
	}
	
	public MessageChatColor(String action, String username, String color) {
		this(action, username, color, null, null);
	}
	
	public MessageChatColor(String action, String username, String color, Boolean success, String oldColor) {
		this.action = action;
		this.username = username;
		this.color = color;
		this.success = success;
		this.oldColor = oldColor;
	}
	
	public MessageChatColor(JsonObject json) {
		JsonElement color = json.get("color");
		JsonElement success = json.get("success");
		JsonElement oldColor = json.get("oldColor");
		
		this.action = json.get("action").getAsString();
		this.username = json.get("username").getAsString();
		this.color = color != null && color.isJsonPrimitive() ? color.getAsString() : null;
		
		this.success = success == null || !success.isJsonPrimitive() ? null : Boolean.valueOf(success.getAsBoolean());
		this.oldColor = oldColor == null || !oldColor.isJsonPrimitive() ? null : oldColor.getAsString();
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
		
		if (success == null)
			obj.remove("success");
		else
			obj.addProperty("success", success.booleanValue());
		
		if (oldColor == null)
			obj.remove("oldColor");
		else
			obj.addProperty("oldColor", oldColor);
		
		return obj;
	}

	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return config.clientType == ClientType.USER || super.canSendMessage(config);
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		String username = this.username;
		String oldColor = server.getChatColor(username);
		
		// Force user ID for those authenticated as users.
		if (config.clientType == ClientType.USER)
			username = config.id;
		
		if ("set".equalsIgnoreCase(action)) {
			if (server.setChatColor(username, color)) {
				json.addProperty("success", true);
				json.addProperty("oldColor", oldColor == null ? ChatColors.WHITE.toString() : oldColor);
				server.save();
			}
			else {
				json.addProperty("success", false);
			}
		}
		
		// Fail if not action is not 'get' instead of 'set'.
		else if (!"get".equalsIgnoreCase(action))
			return;
		
		json.addProperty("color", server.getChatColor(username));
		
		server.sendToAllForUser(json.toString(), username);
	}

}
