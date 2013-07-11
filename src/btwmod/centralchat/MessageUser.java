package btwmod.centralchat;

import org.java_websocket.WebSocket;

import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class MessageUser extends Message {
	public final String username;
	public final String color;
	public final String alias;
	
	public MessageUser(String username) {
		this(username, null, null);
	}
	
	public MessageUser(String username, String color, String alias) {
		this.username = username;
		this.color = color;
		this.alias = alias;
	}
	
	public MessageUser(JsonObject json) {
		JsonElement color = json.get("color");
		JsonElement alias = json.get("alias");
		
		this.username = json.get("username").getAsString();
		this.color = color != null && color.isJsonPrimitive() ? color.getAsString() : null;
		this.alias = alias != null && alias.isJsonPrimitive() ? alias.getAsString() : null;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("username", this.username);
		
		if (this.color != null)
			obj.addProperty("color", this.color);
		
		if (this.alias != null)
			obj.addProperty("alias", this.alias);
		
		return obj;
	}

	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return config.clientType == ClientType.USER || super.canSendMessage(config);
	}
	
	public String getDisplayUsername() {
		return getDisplayUsername(Util.COLOR_WHITE);
	}
	
	public String getDisplayUsername(String resetColorChar) {
		String displayName = alias == null ? username : alias;
		String colorChar = Message.getColorChar(color);
		return colorChar == null ? displayName : colorChar + displayName + resetColorChar;
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		String username = this.username;
		JsonObject json = toJson();
		
		// Force user ID for those authenticated as users.
		if (config.clientType == ClientType.USER)
			json.addProperty("username", username = config.id);
		
		// Set the user's chat color, if it has one.
		json.addProperty("color", server.getChatColor(username));
		if (json.get("color").isJsonNull())
			json.remove("color");
		
		// Set the user's alias, if it has one.
		json.addProperty("alias", server.getChatAlias(username));
		if (json.get("alias").isJsonNull())
			json.remove("alias");
		
		server.sendToAll(json.toString());
	}
}