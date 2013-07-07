package btwmod.centralchat;

import org.java_websocket.WebSocket;

import btwmods.Util;

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
		return ResourceConfig.CLIENTTYPE_SERVER.equalsIgnoreCase(config.clientType);
	}
	
	public abstract void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config);
	
	public void handleAsClient() {
		
	}
	
	public static Message parse(String message) {
		try {
			JsonObject messageJson = new JsonParser().parse(message).getAsJsonObject();
			String type = messageJson.get("type").getAsString();
			
			if ("chat".equalsIgnoreCase(type))
				return new MessageChat(messageJson);
			
			else if ("connect".equalsIgnoreCase(type))
				return new MessageConnect(messageJson);
			
			else if ("disconnect".equalsIgnoreCase(type))
				return new MessageDisconnect(messageJson);
		}
		catch (Exception e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String getColorChar(String color) {
		if (color == null) return null;
		if ("black".equalsIgnoreCase(color)) return Util.COLOR_BLACK;
		if ("navy".equalsIgnoreCase(color)) return Util.COLOR_NAVY;
		if ("green".equalsIgnoreCase(color)) return Util.COLOR_GREEN;
		if ("teal".equalsIgnoreCase(color)) return Util.COLOR_TEAL;
		if ("maroon".equalsIgnoreCase(color)) return Util.COLOR_MAROON;
		if ("purple".equalsIgnoreCase(color)) return Util.COLOR_PURPLE;
		if ("gold".equalsIgnoreCase(color)) return Util.COLOR_GOLD;
		if ("silver".equalsIgnoreCase(color)) return Util.COLOR_SILVER;
		if ("grey".equalsIgnoreCase(color)) return Util.COLOR_GREY;
		if ("blue".equalsIgnoreCase(color)) return Util.COLOR_BLUE;
		if ("lime".equalsIgnoreCase(color)) return Util.COLOR_LIME;
		if ("aqua".equalsIgnoreCase(color)) return Util.COLOR_AQUA;
		if ("red".equalsIgnoreCase(color)) return Util.COLOR_RED;
		if ("pink".equalsIgnoreCase(color)) return Util.COLOR_PINK;
		if ("yellow".equalsIgnoreCase(color)) return Util.COLOR_YELLOW;
		//if ("white".equalsIgnoreCase(color)) return Util.COLOR_WHITE;
		return null;
	}
}
