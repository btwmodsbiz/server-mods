package btwmod.centralchat;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import btwmods.ModLoader;

public class WSClient extends WebSocketClient {

	private final IMessageClient messageClient;
	private final CountDownLatch closeLatch = new CountDownLatch(1);

	public WSClient(IMessageClient messageClient, URI serverUri, Draft draft, Map<String, String> headers, int connecttimeout) {
		super(serverUri, draft, headers, connecttimeout);
		this.messageClient = messageClient;
	}

	public WSClient(IMessageClient messageClient, URI serverUri, Draft draft) {
		super(serverUri, draft);
		this.messageClient = messageClient;
	}

	public WSClient(IMessageClient messageClient, URI serverURI) {
		super(serverURI);
		this.messageClient = messageClient;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		
	}

	@Override
	public void onMessage(String rawMessage) {
		Message message = Message.parse(rawMessage);
		if (message == null) {
			ModLoader.outputError("Invalid message received: " + rawMessage);
		}
		else {
			message.handleAsClient(messageClient);
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		closeLatch.countDown();
	}

	@Override
	public void onError(Exception ex) {
		
	}

	public void awaitClose() throws InterruptedException {
		closeLatch.await();
	}
}
