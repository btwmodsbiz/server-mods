package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class Message {
	
	public abstract String getType();

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", getType());
		return obj;
	}
	
	@SuppressWarnings("static-method")
	public boolean canSendMessage(ResourceConfig config) {
		return config.clientType == ClientType.SERVER;
	}
	
	public abstract void handleAsServer(IServer server, WebSocket conn, ResourceConfig config);
	
	@SuppressWarnings("unused")
	public void handleAsClient(IMessageClient messageClient) {
		
	}
	
	public static Message parse(String message) {
		try {
			JsonObject messageJson = new JsonParser().parse(message).getAsJsonObject();
			String type = messageJson.get("type").getAsString();
			
			if ("chat".equalsIgnoreCase(type))
				return new MessageChat(messageJson);
			
			else if ("emote".equalsIgnoreCase(type))
				return new MessageEmote(messageJson);
			
			else if ("connect".equalsIgnoreCase(type))
				return new MessageConnect(messageJson);
			
			else if ("disconnect".equalsIgnoreCase(type))
				return new MessageDisconnect(messageJson);
			
			else if ("death".equalsIgnoreCase(type))
				return new MessageDeath(messageJson);
			
			else if ("chatcolor".equalsIgnoreCase(type))
				return new MessageChatColor(messageJson);
			
			else if ("chatalias".equalsIgnoreCase(type))
				return new MessageChatAlias(messageJson);
			
			else if (MessageUserList.TYPE.equalsIgnoreCase(type))
				return new MessageUserList(messageJson);
		}
		catch (Exception e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String getColorChar(String color) {
		if (color == null) return null;
		
		try {
			return Enum.valueOf(ChatColors.class, color.toUpperCase()).colorChar;
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
}
