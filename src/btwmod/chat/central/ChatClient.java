package btwmod.chat.central;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import btwmods.ModLoader;

public class ChatClient extends WebSocketClient {

	public AtomicBoolean connected = new AtomicBoolean(false);
	private final BlockingQueue<Message> inQueue;

	public ChatClient(BlockingQueue<Message> inQueue, URI serverUri, Draft draft, Map<String, String> headers, int connecttimeout) {
		super(serverUri, draft, headers, connecttimeout);
		this.inQueue = inQueue;
	}

	public ChatClient(BlockingQueue<Message> inQueue, URI serverUri, Draft draft) {
		super(serverUri, draft);
		this.inQueue = inQueue;
	}

	public ChatClient(BlockingQueue<Message> inQueue, URI serverURI) {
		super(serverURI);
		this.inQueue = inQueue;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		connected.set(true);
	}

	@Override
	public void onMessage(String rawMessage) {
		Message message = Message.parse(rawMessage);
		if (message == null) {
			ModLoader.outputError("Invalid message received: " + rawMessage);
		}
		/*else if (message.getClass() != MessageChat.class) {
			ModLoader.outputError("Received non-chat: " + rawMessage);
		}*/
		else {
			inQueue.add(message);
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		connected.set(false);
	}

	@Override
	public void onError(Exception ex) {
		connected.set(false);
		ModLoader.outputError(ex, "ChatClient exception (" + ex.getClass().getSimpleName() + "): " + ex.getMessage());
	}
}
