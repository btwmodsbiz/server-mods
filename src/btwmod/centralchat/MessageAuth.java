package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageAuth extends Message {
	
	public final String action;
	public final String clientType;
	public final String id;
	public final String key;
	
	public MessageAuth(String action, String clientType, String id, String key) {
		this.action = action;
		this.clientType = clientType;
		this.id = id;
		this.key = key;
	}
	
	public MessageAuth(JsonObject json) {
		this.action = json.get("action").getAsString();
		this.clientType = json.get("clientType").getAsString();
		this.id = json.get("id").getAsString();
		
		JsonElement key = json.get("key");
		this.key = key != null && key.isJsonPrimitive() ? key.getAsString() : null;
	}

	@Override
	public String getType() {
		return "auth";
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("action", this.action);
		obj.addProperty("clientType", this.clientType);
		obj.addProperty("id", this.id);
		
		if (key == null)
			obj.remove("key");
		else
			obj.addProperty("key", this.key);
		
		return obj;
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		if ("addKey".equalsIgnoreCase(action) && key != null) {
			if (ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(clientType)) {
				server.addUserKey(id, key);
			}
			else if (ResourceConfig.CLIENTTYPE_SERVER.equalsIgnoreCase(clientType)) {
				server.addServerKey(id, key);
			}
		}
		if ("removeKey".equalsIgnoreCase(action)) {
			if (ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(clientType)) {
				server.removeUserKey(id);
			}
			else if (ResourceConfig.CLIENTTYPE_SERVER.equalsIgnoreCase(clientType)) {
				server.removeServerKey(id);
			}
		}
	}
}