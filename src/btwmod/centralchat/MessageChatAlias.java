package btwmod.centralchat;

import net.minecraft.server.MinecraftServer;

import org.java_websocket.WebSocket;

import btwmods.ChatAPI;
import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatAlias extends Message {
	
	public final String TYPE = "chatalias";

	public final String action;
	public final String username;
	public final String alias;
	
	// Only for responses
	public final Boolean success;
	public final String oldAlias;
	
	public MessageChatAlias(String action, String username) {
		this(action, username, null);
	}
	
	public MessageChatAlias(String action, String username, String alias) {
		this(action, username, alias, null, null);
	}
	
	public MessageChatAlias(String action, String username, String alias, Boolean success, String oldAlias) {
		this.action = action;
		this.username = username;
		this.alias = alias;
		this.success = success;
		this.oldAlias = oldAlias;
	}
	
	public MessageChatAlias(JsonObject json) {
		JsonElement alias = json.get("alias");
		JsonElement success = json.get("success");
		JsonElement oldAlias = json.get("oldAlias");
		
		this.action = json.get("action").getAsString();
		this.username = json.get("username").getAsString();
		this.alias = alias == null || !alias.isJsonPrimitive() ? null : alias.getAsString();
		
		this.success = success == null || !success.isJsonPrimitive() ? null : Boolean.valueOf(success.getAsBoolean());
		this.oldAlias = oldAlias == null || !oldAlias.isJsonPrimitive() ? null : oldAlias.getAsString();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("action", action);
		obj.addProperty("username", username);
		
		if (alias == null)
			obj.remove("alias");
		else
			obj.addProperty("alias", alias);
		
		if (success == null)
			obj.remove("success");
		else
			obj.addProperty("success", success.booleanValue());
		
		if (oldAlias == null)
			obj.remove("oldAlias");
		else
			obj.addProperty("oldAlias", oldAlias);
		
		return obj;
	}
	
	@Override
	public void handleAsClient(IMessageClient messageClient) {
		String message = getFormattedMessage();
		if (message != null) {
			MinecraftServer.getServer().getLogAgent().func_98233_a("[" + message.replace(Util.COLOR_YELLOW, "").replace(Util.COLOR_WHITE, "") + "]");
			ChatAPI.sendChatToAllAdmins(message);
			messageClient.setAlias(username, alias);
		}
	}
	
	protected String getFormattedMessage() {
		if ("set".equalsIgnoreCase(action) && success != null) {
			if (success.booleanValue()) {
				if (alias == null && oldAlias == null)
					return Util.COLOR_YELLOW + username + " does not have an alias set.";
				
				else if (alias == null)
					return Util.COLOR_YELLOW + "Removed " + username + "'s alias of " + Util.COLOR_WHITE + oldAlias;
				
				else if (oldAlias == null)
					return Util.COLOR_YELLOW + "Set " + username + "'s alias to " + Util.COLOR_WHITE + alias;
				
				else if (oldAlias == null)
					return Util.COLOR_YELLOW + "Set " + username + "'s alias from " + Util.COLOR_WHITE + oldAlias + Util.COLOR_YELLOW + " to " + Util.COLOR_WHITE + alias;
			}
			else {
				return Util.COLOR_WHITE + alias + Util.COLOR_YELLOW + " is not a valid alias.";
			}
		}
		else if ("get".equalsIgnoreCase(action)) {
			if (alias == null)
				return Util.COLOR_YELLOW + username + " does not have an alias.";
			
			else
				return Util.COLOR_YELLOW + username + "'s alias is " + Util.COLOR_WHITE + alias;
		}
		
		return null;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		JsonObject json = toJson();
		String username = this.username;
		String oldAlias = server.getChatAlias(username);
		
		if ("set".equalsIgnoreCase(action)) {
			if (server.setChatAlias(username, alias)) {
				json.addProperty("success", true);
				json.addProperty("oldAlias", oldAlias);
				server.saveSettings();
			}
			else {
				json.addProperty("success", false);
			}
		}
		
		// Fail if not action is not 'get' instead of 'set'.
		else if (!"get".equalsIgnoreCase(action))
			return;
		
		json.addProperty("alias", server.getChatAlias(username));
		
		server.sendToAllServers(json.toString());
	}
}
