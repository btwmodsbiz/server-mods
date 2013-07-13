package btwmod.centralchat;

import net.minecraft.server.MinecraftServer;
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
	public void handleAsClient(IMessageClient messageClient) {
		MinecraftServer.getServer().getLogAgent().func_98233_a(getLoggedMessage());
		String message = getFormattedMessage();
		ChatAPI.sendChatToAllPlayers(message);
		messageClient.addRestorableChat(message);
	}
	
	protected String getFormattedMessage() {
		return Util.COLOR_YELLOW + getDisplayUsername(false) + " joined chat" + (server == null ? "" : " on " + server) + ".";
	}
	
	protected String getLoggedMessage() {
		return username + " joined chat" + (server == null ? "" : " on " + server) + ".";
	}
}