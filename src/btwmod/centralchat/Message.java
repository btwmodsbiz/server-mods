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
	
	@SuppressWarnings("unused")
	public JsonObject toJsonCleaned(IServer server, ResourceConfig config) {
		return toJson();
	}
	
	@SuppressWarnings("static-method")
	public boolean canSendMessage(ResourceConfig config) {
		return config.clientType == ClientType.GATEWAY;
	}
	
	public abstract void handleAsServer(IServer server, WebSocket conn, ResourceConfig config);
	
	@SuppressWarnings("unused")
	public void handleAsGateway(IGateway gateway) {
		
	}
	
	public static Message parse(String message) {
		try {
			JsonObject messageJson = new JsonParser().parse(message).getAsJsonObject();
			String type = messageJson.get("type").getAsString();
			
			if (MessageAuth.TYPE.equalsIgnoreCase(type))
				return new MessageAuth(messageJson);
			
			else if (MessageChat.TYPE.equalsIgnoreCase(type))
				return new MessageChat(messageJson);
			
			else if (MessageChatAlias.TYPE.equalsIgnoreCase(type))
				return new MessageChatAlias(messageJson);
			
			else if (MessageChatColor.TYPE.equalsIgnoreCase(type))
				return new MessageChatColor(messageJson);
			
			else if (MessageConnect.TYPE.equalsIgnoreCase(type))
				return new MessageConnect(messageJson);
			
			else if (MessageDeath.TYPE.equalsIgnoreCase(type))
				return new MessageDeath(messageJson);
			
			else if (MessageDisconnect.TYPE.equalsIgnoreCase(type))
				return new MessageDisconnect(messageJson);
			
			else if (MessageEmote.TYPE.equalsIgnoreCase(type))
				return new MessageEmote(messageJson);
			
			else if (MessageGatewayConnect.TYPE.equalsIgnoreCase(type))
				return new MessageGatewayConnect(messageJson);
			
			else if (MessageGatewayDisconnect.TYPE.equalsIgnoreCase(type))
				return new MessageGatewayDisconnect(messageJson);
			
			else if (MessageUserInfo.TYPE.equalsIgnoreCase(type))
				return new MessageUserInfo(messageJson);
			
			else if (MessageUserInfoList.TYPE.equalsIgnoreCase(type))
				return new MessageUserInfoList(messageJson);
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
