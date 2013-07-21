package btwmod.centralchat.message;

import org.java_websocket.WebSocket;

import btwmod.centralchat.ClientType;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageAuth extends Message {
	
	public static final String TYPE = "auth";
	
	public final String action;
	public final ClientType clientType;
	public final String id;
	public final String key;
	
	public MessageAuth(String action, ClientType clientType, String id, String key) {
		this.action = action;
		this.clientType = clientType;
		this.id = id;
		this.key = key;
	}
	
	public MessageAuth(JsonObject json) {
		this.action = json.get("action").getAsString();
		this.clientType = ClientType.get(json.get("clientType").getAsString());
		this.id = json.get("id").getAsString();
		
		JsonElement key = json.get("key");
		this.key = key != null && key.isJsonPrimitive() ? key.getAsString() : null;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("action", this.action);
		obj.addProperty("clientType", this.clientType.toString());
		obj.addProperty("id", this.id);
		
		if (key == null)
			obj.remove("key");
		else
			obj.addProperty("key", this.key);
		
		return obj;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		if ("addKey".equalsIgnoreCase(action) && key != null) {
			if (clientType == ClientType.USER) {
				server.addUserKey(id, key);
				server.addActualUsername(id);
			}
			else if (clientType == ClientType.GATEWAY) {
				server.addGatewayKey(id, key);
			}
		}
		if ("removeKey".equalsIgnoreCase(action)) {
			if (clientType == ClientType.USER) {
				server.removeUserKey(id);
			}
			else if (clientType == ClientType.GATEWAY) {
				server.removeGatewayKey(id);
			}
		}
	}
}