package btwmod.centralchat;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.java_websocket.WebSocket;

import btwmods.ChatAPI;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MessageUserList extends Message {
	
	public static final String TYPE = "userlist";
	
	public final MessageUserEntry[] users;

	@Override
	public String getType() {
		return TYPE;
	}

	public MessageUserList(MessageUserEntry[] users) {
		this.users = users;
	}
	
	public MessageUserList(JsonObject json) {
		JsonArray usersArray = json.get("users").getAsJsonArray();
		users = new MessageUserEntry[usersArray.size()];
		for (int i = 0, len = usersArray.size(); i < len; i++) {
			users[i] = new MessageUserEntry(usersArray.get(0).getAsJsonObject());
		}
	}
	
	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		JsonArray userArray = new JsonArray();
		for (MessageUserEntry message : users) {
			userArray.add(message.toJson());
		}
		obj.add("users", userArray);
		return obj;
	}

	@Override
	public void handleAsServer(ChatServer server, WebSocket conn, ResourceConfig config) {
		
	}
	
	@Override
	public void handleAsClient(IMessageClient messageClient) {
		Map<String, String> aliasMap = new HashMap<String, String>();
		for (MessageUserEntry entry : users)
			aliasMap.put(entry.username.toLowerCase(), entry.alias);
		
		for (Entry<String, String> entry : aliasMap.entrySet())
			if (entry.getValue() != null)
				messageClient.setAlias(entry.getKey(), entry.getValue());
		
		int len = aliasMap.size();
		if (len > 0)
			ChatAPI.sendChatToAllPlayers(aliasMap.size() + " user" + (len == 1 ? "" : "s") + " available via the chat server.");
	}

	public static class MessageUserEntry extends MessageConnect {
		
		public final String TYPE = "userlist-entry";

		@Override
		public String getType() {
			return TYPE;
		}

		public MessageUserEntry(String username, String server) {
			super(username, server);
		}

		public MessageUserEntry(String username, String server, String color, String alias) {
			super(username, server, color, alias);
		}
		
		public MessageUserEntry(JsonObject json) {
			super(json);
		}

		@Override
		public JsonObject toJson() {
			JsonObject obj = super.toJson();
			obj.remove("type");
			return obj;
		}
	}
}
