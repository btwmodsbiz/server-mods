package btwmod.centralchat.message;

import btwmod.centralchat.ClientType;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

import com.google.gson.JsonObject;

public class MessageChat extends MessageUserMessage {
	
	public static final String TYPE = "chat";
	
	public final String message;
	
	public MessageChat(JsonObject json) {
		super(json);
		this.message = json.get("message").getAsString();
	}

	public MessageChat(String username, String gateway, String message, String color, String alias) {
		super(username, gateway, color, alias);
		this.message = message;
	}

	public MessageChat(String username, String gateway, String message) {
		super(username, gateway, null, null);
		this.message = message;
	}
	
	@Override
	public JsonObject toJsonCleaned(IServer server, ResourceConfig config) {
		JsonObject json = super.toJsonCleaned(server, config);
		json.addProperty("message", message.trim().substring(0, Math.min(100, message.trim().length())));
		return json;
	}
	
	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("message", message);
		return json;
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	protected String getFormattedMessage() {
		return "<" + getDisplayUsername(true) + "> " + message;
	}
	
	@Override
	protected String getLoggedMessage() {
		return "<" + username + "> " + message;
	}
	
	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return config.clientType == ClientType.USER || super.canSendMessage(config);
	}
}
