package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatAlias extends Message {
	
	public final String TYPE = "chatalias";

	public final String action;
	public final String username;
	public final String alias;
	
	public MessageChatAlias(String action, String username) {
		this(action, username, null);
	}
	
	public MessageChatAlias(String action, String username, String alias) {
		this.action = action;
		this.username = username;
		this.alias = alias;
	}
	
	public MessageChatAlias(JsonObject json) {
		JsonElement alias = json.get("alias");
		
		this.action = json.get("action").getAsString();
		this.username = json.get("username").getAsString();
		this.alias = alias != null && alias.isJsonPrimitive() ? alias.getAsString() : null;
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
		obj.addProperty("alias", this.alias);
		return obj;
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		String username = this.username;
		
		if ("set".equalsIgnoreCase(action)) {
			server.setChatAlias(username, alias);
			server.save();
		}
		
		// Fail if not action is not 'get' instead of 'set'.
		else if (!"get".equalsIgnoreCase(action))
			return;
		
		json.addProperty("color", server.getChatColor(username));
	}
}
