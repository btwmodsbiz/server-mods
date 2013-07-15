package btwmod.centralchat;

import org.java_websocket.WebSocket;

import net.minecraft.server.MinecraftServer;
import btwmods.ChatAPI;
import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageConnect extends MessageUser {
	
	public final String TYPE = "connect";
	
	public final String gateway;

	public MessageConnect(JsonObject json) {
		super(json);
		
		JsonElement gateway = json.get("gateway");
		this.gateway = gateway != null && gateway.isJsonPrimitive() ? gateway.getAsString() : null;
	}

	public MessageConnect(String username, String gateway, String color, String alias) {
		super(username, color, alias);
		this.gateway = gateway;
	}

	public MessageConnect(String username, String gateway) {
		super(username);
		this.gateway = gateway;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		
		if (this.gateway == null)
			obj.remove("gateway");
		else
			obj.addProperty("gateway", this.gateway);
		
		return obj;
	}
	
	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		super.handleAsServer(server, conn, config);
		toServer(server);
	}
	
	protected void toServer(IServer server) {
		server.addLoggedInUser(this.gateway, username);
	}
	
	@Override
	public void handleAsGateway(IGateway gateway) {
		MinecraftServer.getServer().getLogAgent().func_98233_a(getLoggedMessage());
		String message = getFormattedMessage();
		ChatAPI.sendChatToAllPlayers(message);
		gateway.addRestorableChat(message);
	}
	
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(false) + " joined chat" + (gateway == null ? "" : " on " + gateway) + ".";
	}
	
	protected String getLoggedMessage() {
		return username + " joined chat" + (gateway == null ? "" : " on " + gateway) + ".";
	}
}