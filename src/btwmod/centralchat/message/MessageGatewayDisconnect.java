package btwmod.centralchat.message;

import org.java_websocket.WebSocket;

import btwmod.centralchat.IGateway;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;
import btwmods.ChatAPI;

import com.google.gson.JsonObject;

public class MessageGatewayDisconnect extends MessageGatewayConnect {
	
	public static final String TYPE = "gatewaydisconnect";

	@Override
	public String getType() {
		return TYPE;
	}

	public MessageGatewayDisconnect(JsonObject json) {
		super(json);
	}

	public MessageGatewayDisconnect(String gateway, MessageUserInfo[] users) {
		super(gateway, users);
	}
	
	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return false;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		// Not necessary to clean up the JSON as this originates only from the server.
		server.sendToAll(toJson().toString());
	}
	
	@Override
	public void handleAsGateway(IGateway gateway) {
		if (!gateway.getId().equalsIgnoreCase(this.gateway)) {
			int len = users == null ? 0 : users.length;
			ChatAPI.sendChatToAllPlayers("Server " + this.gateway + " disconnected" + (len > 0 ? " making " + len + " user" + (len == 1 ? "" : "s") + " no longer available for chat" : "") + ".");
		}
	}
}
