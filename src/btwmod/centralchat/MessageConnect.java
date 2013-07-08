package btwmod.centralchat;

import btwmods.ChatAPI;
import btwmods.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageConnect extends MessageUser {
	
	public final String TYPE = "connect";
	
	public final String server;

	public MessageConnect(JsonObject json) {
		super(json);
		
		JsonElement server = json.get("server");
		this.server = server != null && server.isJsonPrimitive() ? server.getAsString() : null;
	}

	public MessageConnect(String username, String server, String color, String alias) {
		super(username, color, alias);
		this.server = server;
	}

	public MessageConnect(String username, String server) {
		super(username);
		this.server = server;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		
		if (this.server == null)
			obj.remove("server");
		else
			obj.addProperty("server", this.server);
		
		return obj;
	}
	
	@Override
	public void handleAsClient() {
		ChatAPI.sendChatToAllPlayers(getFormattedMessage());
	}
	
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(Util.COLOR_YELLOW) + " joined chat" + (server == null ? "" : " on " + server) + ".";
	}
}