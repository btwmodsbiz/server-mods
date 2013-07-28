package btwmod.centralchat.message;

import java.util.Random;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import btwmod.centralchat.IGateway;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

public class MessageChatKey extends Message {
	
	private static final Random rnd = new Random();
	public static final String TYPE = "chatkey";
	
	public final String username;
	public final String chatkey;
	public final boolean forced;
	
	public MessageChatKey(String username, String chatkey, boolean forced) {
		this.username = username;
		this.chatkey = chatkey;
		this.forced = forced;
	}
	
	public MessageChatKey(JsonObject json) {
		this.username = json.get("username").getAsString();
		
		JsonElement forced = json.get("forced");
		this.forced = forced == null ? false : forced.getAsBoolean();
		
		JsonElement chatkey = json.get("chatkey");
		this.chatkey = chatkey == null ? null : chatkey.getAsString();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("username", username);
		
		if (chatkey != null)
			obj.addProperty("chatkey", chatkey);
		
		obj.addProperty("forced", forced);
		return obj;
	}
	
	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		String key = server.getUserKey(username);
		if (key == null || forced) {
			server.addUserKey(username, key = Integer.toHexString(0x10000 + rnd.nextInt(0xFFFFF - 0x10000)).toLowerCase());
			server.saveSettings();
		}
		
		server.sendToAllForUser(new MessageChatKey(server.getActualUsername(username), key, forced).toJson().toString(), username);
	}
	
	@Override
	public void handleAsGateway(IGateway gateway) {
		if (chatkey != null) {
			gateway.sendChatToPlayer("Your chatkey is " + chatkey + ".", username);
			gateway.sendChatToPlayer("Login to chat at betterthansolo.com/chat?" + username + "/" + chatkey, username);
		}
	}
}
