package btwmod.centralchat;

import net.minecraft.server.MinecraftServer;

import org.java_websocket.WebSocket;

import btwmods.ChatAPI;
import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageChatAlias extends Message {
	
	public static MessageChatAlias buildGet(String username, String requestedBy) {
		return new MessageChatAlias("get", username, null, null, null, requestedBy);
	}
	
	public static MessageChatAlias buildSet(String username, String alias, String requestedBy) {
		return new MessageChatAlias("set", username, alias, null, null, requestedBy);
	}
	
	public static MessageChatAlias buildSuccess(String username, String alias, String oldAlias) {
		return new MessageChatAlias("set", username, alias, Boolean.TRUE, oldAlias, null);
	}
	
	public static MessageChatAlias buildFail(String username, String alias, String requestedBy) {
		return new MessageChatAlias("set", username, alias, Boolean.FALSE, null, requestedBy);
	}
	
	public final static String TYPE = "chatalias";

	public final String action;
	public final String username;
	public final String alias;
	
	// Only for responses
	public final Boolean success;
	public final String oldAlias;
	public final String requestedBy;
	
	protected MessageChatAlias(String action, String username, String alias, Boolean success, String oldAlias, String requestedBy) {
		this.action = action;
		this.username = username;
		this.alias = alias;
		this.success = success;
		this.oldAlias = oldAlias;
		this.requestedBy = requestedBy;
	}
	
	public MessageChatAlias(JsonObject json) {
		this.action = json.get("action").getAsString();
		this.username = json.get("username").getAsString();
		
		JsonElement alias = json.get("alias");
		this.alias = alias == null || !alias.isJsonPrimitive() ? null : alias.getAsString();
		
		JsonElement success = json.get("success");
		this.success = success == null || !success.isJsonPrimitive() ? null : Boolean.valueOf(success.getAsBoolean());

		JsonElement oldAlias = json.get("oldAlias");
		this.oldAlias = oldAlias == null || !oldAlias.isJsonPrimitive() ? null : oldAlias.getAsString();

		JsonElement requestedBy = json.get("requestedBy");
		this.requestedBy = requestedBy == null || !requestedBy.isJsonPrimitive() ? null : requestedBy.getAsString();
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
	public JsonObject toJsonCleaned(IServer server, ResourceConfig config) {
		JsonObject json = super.toJsonCleaned(server, config);
		json.addProperty("username", server.getActualUsername(username));
		return json;
	}
	
	@Override
	public void handleAsGateway(IGateway gateway) {
		String message = getFormattedMessage();
		if (message != null) {
			if ("set".equalsIgnoreCase(action)) {
				if (success == Boolean.TRUE) {
					MinecraftServer.getServer().getLogAgent().func_98233_a("[" + message.replace(Util.COLOR_YELLOW, "").replace(Util.COLOR_WHITE, "") + "]");
					ChatAPI.sendChatToAllPlayers(message);
				}
				else
					ChatAPI.sendChatToAllAdmins(message);
				
				gateway.setAlias(username, alias);
			}
			else if ("get".equalsIgnoreCase(action)) {
				
			}
		}
	}
	
	protected String getFormattedMessage() {
		if ("set".equalsIgnoreCase(action) && success != null) {
			if (success == Boolean.TRUE) {
				if (alias == null && oldAlias == null)
					return Util.COLOR_YELLOW + username + " does not have an alias set.";
				
				else if (alias == null)
					return Util.COLOR_YELLOW + "Removed " + username + "'s alias of " + Util.COLOR_WHITE + oldAlias;
				
				else if (oldAlias == null)
					return Util.COLOR_YELLOW + "Set " + username + "'s alias to " + Util.COLOR_WHITE + alias;
				
				else if (oldAlias == null)
					return Util.COLOR_YELLOW + "Set " + username + "'s alias from " + Util.COLOR_WHITE + oldAlias + Util.COLOR_YELLOW + " to " + Util.COLOR_WHITE + alias;
			}
			else if (success == Boolean.FALSE) {
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
		boolean onlyGateways = false;
		JsonObject json = toJsonCleaned(server, config);
		String oldAlias = server.getChatAlias(username);
		
		if ("set".equalsIgnoreCase(action)) {
			if (server.setChatAlias(username, alias)) {
				json.addProperty("success", true);
				json.addProperty("oldAlias", oldAlias);
				server.saveSettings();
			}
			else {
				onlyGateways = true;
				json.addProperty("success", false);
			}
		}
		
		// Fail if not action is not 'get' instead of 'set'.
		else if (!"get".equalsIgnoreCase(action))
			return;
		
		json.addProperty("alias", server.getChatAlias(username));

		if ("set".equalsIgnoreCase(action)) {
			if (onlyGateways)
				server.sendToAllGateways(json.toString());
			else
				server.sendToAll(json.toString());
		}
		else if ("get".equalsIgnoreCase(action)) {
			server.sendToAllForUser(json.toString(), username);
		}
	}
}
