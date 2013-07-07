package btwmod.centralchat;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class MessageChat extends Message {
	public final String username;
	public final String message;
	public final String color;
	public final String alias;
	
	public MessageChat(String username, String message) {
		this(username, message, null, null);
	}
	
	public MessageChat(String username, String message, String color, String alias) {
		super("chat");
		this.username = username;
		this.message = message;
		this.color = color;
		this.alias = alias;
	}
	
	public MessageChat(JsonObject json) {
		this(
			json.get("username").getAsString(),
			json.get("message").getAsString(),
			json.has("color") ? json.get("color").getAsString() : null,
			json.has("alias") ? json.get("alias").getAsString() : null
		);
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("username", this.username);
		obj.addProperty("message", this.message);
		
		if (this.color != null)
			obj.addProperty("color", this.color);
		
		if (this.alias != null)
			obj.addProperty("alias", this.alias);
		
		return obj;
	}

	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return super.canSendMessage(config) || ResourceConfig.CLIENTTYPE_USER.equalsIgnoreCase(config.clientType);
	}

	@Override
	public void handleMessage(ChatServer server, WebSocket conn, ResourceConfig config) {
		String username = this.username;
		JsonObject json = toJson();
		
		// Force user ID for those authenticated as users.
		if ("user".equalsIgnoreCase(config.clientType))
			json.addProperty("username", username = config.id);
		
		// Set the user's chat color, if it has one.
		json.addProperty("color", server.getChatColor(username));
		if (json.get("color").isJsonNull())
			json.remove("color");
		/*if (color != null) {
			String[] colors = server.getColor(color);
			JsonObject colorJson = new JsonObject();
			colorJson.addProperty("char", colors[0]);
			colorJson.addProperty("hex", colors[1]);
			json.add("color", colorJson);
		}
		else if (json.has("color")) {
			json.remove("color");
		}*/
		
		// Set the user's alias, if it has one.
		json.addProperty("alias", server.getChatAlias(username));
		if (json.get("alias").isJsonNull())
			json.remove("alias");
		
		server.sendToAll(json.toString());
	}
}