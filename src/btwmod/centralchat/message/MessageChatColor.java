package btwmod.centralchat.message;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;

import org.java_websocket.WebSocket;

import btwmod.centralchat.ChatColors;
import btwmod.centralchat.ClientType;
import btwmod.centralchat.IGateway;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;
import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatColor extends Message {
	
	public static MessageChatColor buildGet(String username, String requestedBy) {
		return new MessageChatColor("get", username, null, null, null, requestedBy);
	}
	
	public static MessageChatColor buildSet(String username, String color, String requestedBy) {
		return new MessageChatColor("set", username, color, null, null, requestedBy);
	}
	
	public static MessageChatColor buildSuccess(String username, String color, String oldColor) {
		return new MessageChatColor("set", username, color, Boolean.TRUE, oldColor, null);
	}
	
	public static MessageChatColor buildFail(String username, String color, String requestedBy) {
		return new MessageChatColor("set", username, color, Boolean.FALSE, null, requestedBy);
	}
	
	public final static String TYPE = "chatcolor";
	
	public final String action;
	public final String username;
	public final String color;
	public final String requestedBy;
	
	// Only for responses
	public final Boolean success;
	public final String oldColor;
	
	protected MessageChatColor(String action, String username, String color, Boolean success, String oldColor, String requestedBy) {
		this.action = action;
		this.username = username;
		this.color = color;
		this.requestedBy = requestedBy;
		this.success = success;
		this.oldColor = oldColor;
	}
	
	public MessageChatColor(JsonObject json) {
		this.action = json.get("action").getAsString();
		this.username = json.get("username").getAsString();
		
		JsonElement requestedBy = json.get("requestedBy");
		this.requestedBy = requestedBy == null || !requestedBy.isJsonPrimitive() ? null : requestedBy.getAsString();
		
		JsonElement color = json.get("color");
		this.color = color != null && color.isJsonPrimitive() ? color.getAsString() : null;
		
		JsonElement success = json.get("success");
		this.success = success == null || !success.isJsonPrimitive() ? null : Boolean.valueOf(success.getAsBoolean());
		
		JsonElement oldColor = json.get("oldColor");
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
	public void handleAsGateway(IGateway gateway) {
		EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().getPlayerEntity(username);
		if (player != null) {
			String message = getFormattedMessage();
			if (message != null) {
				player.sendChatToPlayer(message);
			}
		}
	}
	
	protected String getFormattedMessage() {
		String colorChar = ChatColors.getChar(color);
		if (colorChar == null) colorChar = "";
		
		String oldColorChar = ChatColors.getChar(oldColor);
		if (oldColorChar == null) oldColorChar = "";
		
		if ("set".equalsIgnoreCase(action) && success != null) {
			if (success.booleanValue()) {
				if (color == null && oldColor == null)
					return Util.COLOR_YELLOW + "Your chat color is already the default.";
				
				else if (color == null)
					return Util.COLOR_YELLOW + "Chat color changed from " + oldColorChar + oldColor + Util.COLOR_YELLOW + " back to the default.";
				
				else if (oldColor == null)
					return Util.COLOR_YELLOW + "Set chat color to " + colorChar + color + Util.COLOR_YELLOW + ".";
				
				else if (color.equalsIgnoreCase(oldColor))
					return Util.COLOR_YELLOW + "Chat color is already set to " + colorChar + color + Util.COLOR_YELLOW + ".";
				
				else
					return Util.COLOR_YELLOW + "Set chat color from " + oldColorChar + oldColor + Util.COLOR_YELLOW + " to " + colorChar + color + Util.COLOR_YELLOW + ".";
			}
			else {
				return Util.COLOR_YELLOW + color + " is not a valid color.";
			}
		}
		else if ("get".equalsIgnoreCase(action)) {
			if (color == null)
				return Util.COLOR_YELLOW + "You does not have a chat color set.";
			
			else
				return Util.COLOR_YELLOW + "Your chat color is " + colorChar + color + Util.COLOR_YELLOW + ".";
		}
		
		return null;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		String username = this.username;
		String oldColor = server.getChatColor(username);
		
		// Force user ID for those authenticated as users.
		if (config.clientType == ClientType.USER)
			username = config.id;
		
		if ("set".equalsIgnoreCase(action)) {
			if (server.setChatColor(username, color)) {
				json.addProperty("success", true);
				json.addProperty("oldColor", oldColor);
				server.saveSettings();
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
