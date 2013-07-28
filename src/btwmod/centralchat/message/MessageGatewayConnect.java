package btwmod.centralchat.message;

import org.java_websocket.WebSocket;

import btwmod.centralchat.IGateway;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MessageGatewayConnect extends Message {
	
	public static final String TYPE = "gatewayconnect";

	@Override
	public String getType() {
		return TYPE;
	}
	
	public final String gateway;
	public final MessageUserInfo[] users;

	public MessageGatewayConnect(String gateway, MessageUserInfo[] users) {
		this.gateway = gateway;
		this.users = users;
	}

	public MessageGatewayConnect(JsonObject json) {
		gateway = json.get("gateway").getAsString();
		
		JsonArray userArray = json.get("users").getAsJsonArray();
		users = new MessageUserInfo[userArray.size()];
		for (int i = 0, len = userArray.size(); i < len; i++) {
			users[i] = new MessageUserInfo(userArray.get(i).getAsJsonObject());
		}
	}
	
	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("gateway", gateway);

		if (users != null) {
			JsonArray userArray = new JsonArray();
			for (MessageUserInfo user : users) {
				userArray.add(user.toJson());
			}
			obj.add("users", userArray);
		}
		
		return obj;
	}
	
	@Override
	public JsonObject toJsonCleaned(IServer server, ResourceConfig config) {
		JsonObject json = super.toJsonCleaned(server, config);
		
		json.addProperty("gateway", config.id);
		
		MessageUserInfo[] cleanedUsers = new MessageUserInfo[users.length];
		for (int i = 0, len = users.length; i < len; i++) {
			String actualUsername = server.getActualUsername(users[i].username);
			cleanedUsers[i] = new MessageUserInfo(actualUsername, config.id, server.getChatColor(actualUsername), server.getChatAlias(actualUsername));
		}

		JsonArray userArray = new JsonArray();
		for (MessageUserInfo user : cleanedUsers) {
			userArray.add(user.toJson());
		}
		json.add("users", userArray);
		
		return json;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		if (users != null) {
			int len = users.length;
			String[] usernames = new String[len];
			for (int i = 0; i < len; i++) {
				usernames[i] = users[i].username;
			}
			server.addLoggedInUser(config.id, usernames);
		}

		// Resend the message to all clients after being cleaned up.
		server.sendToAll(toJsonCleaned(server, config).toString());
		
		// Send a full list of users back to the gateway.
		conn.send(MessageUserInfoList.build(server).toJson().toString());
	}
	
	@Override
	public void handleAsGateway(IGateway gateway) {
		if (!gateway.getId().equalsIgnoreCase(this.gateway)) {
			int len = users == null ? 0 : users.length;
			gateway.sendChatToAllPlayers("Server " + this.gateway + " connected" + (len > 0 ? " making " + len + " user" + (len == 1 ? "" : "s") + " available for chat" : "") + ".");
		}
	}
}
