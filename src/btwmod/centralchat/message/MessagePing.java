package btwmod.centralchat.message;

import java.util.concurrent.atomic.AtomicInteger;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import btwmod.centralchat.IGateway;
import btwmod.centralchat.IServer;
import btwmod.centralchat.ResourceConfig;

public class MessagePing extends Message {
	
	private static final AtomicInteger nextId = new AtomicInteger();
	public static final String TYPE = "ping";
	
	public final int id;
	
	public MessagePing(int id) {
		this.id = id;
	}
	
	public MessagePing() {
		this(nextId.incrementAndGet());
	}
	
	public MessagePing(JsonObject json) {
		this.id = json.get("id").getAsInt();
	}
	
	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("id", id);
		return json;
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public boolean canSendMessage(ResourceConfig config) {
		return true;
	}

	@Override
	public void handleAsServer(IServer server, WebSocket conn, ResourceConfig config) {
		conn.send(new MessagePong(id).toJson().toString());
		server.onPing(conn, config, this);
	}

	@Override
	public void handleAsGateway(IGateway gateway) {
		gateway.send(new MessagePong(id));
	}
}
