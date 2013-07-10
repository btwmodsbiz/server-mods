package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatAlias extends Message {
	
	public final String TYPE = "chatalias";
	
	public final String username;
	public final String alias;
	
	public MessageChatAlias(String username) {
		this(username, null);
	}
	
	public MessageChatAlias(String username, String alias) {
		this.username = username;
		this.alias = alias;
	}
	
	public MessageChatAlias(JsonObject json) {
		JsonElement alias = json.get("alias");
		
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
		obj.addProperty("username", this.username);
		obj.addProperty("alias", this.alias);
		return obj;
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		server.setChatAlias(username, alias);
		server.save();
	}
}
