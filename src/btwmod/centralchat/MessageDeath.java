package btwmod.centralchat;

import com.google.gson.JsonObject;

public class MessageDeath extends MessageChat {
	
	public final String TYPE = "death";

	public MessageDeath(JsonObject json) {
		super(json);
	}

	public MessageDeath(String username, String gateway, String message, String color, String alias) {
		super(username, gateway, message, color, alias);
	}

	public MessageDeath(String username, String gateway, String message) {
		super(username, gateway, message);
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public JsonObject toJsonCleaned(IServer server, ResourceConfig config) {
		JsonObject json = super.toJsonCleaned(server, config);
		
		if (message != null)
			json.addProperty("message", server.replaceUsernamesWithAliases(message));
		
		return json;
	}
	
	protected String getFormattedMessage() {
		return message;
	}
	
	@Override
	protected String getLoggedMessage() {
		return message;
	}
}
