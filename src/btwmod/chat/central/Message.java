package btwmod.chat.central;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class Message {
	public final String type;
	
	protected Message(String type) {
		this.type = type;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", this.type);
		return obj;
	}
	
	public abstract void handleMessage(ChatServer server, WebSocket conn, ResourceConfig config);
	
	public static Message parse(String message) {
		try {
			JsonObject messageJson = new JsonParser().parse(message).getAsJsonObject();
			String type = messageJson.get("type").getAsString();
			
			if ("chat".equalsIgnoreCase(type))
				return new MessageChat(messageJson);
			
			else if ("connect".equalsIgnoreCase(type))
				return new MessageConnect(messageJson);
		}
		catch (Exception e) {
			
		}
		
		return null;
	}
}
