package btwmod.centralchat;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import btwmods.ModLoader;

public class WSGateway extends WebSocketClient {

	private final IGateway gateway;
	private final CountDownLatch closeLatch = new CountDownLatch(1);

	public WSGateway(IGateway gateway, URI serverUri, Draft draft, Map<String, String> headers, int connecttimeout) {
		super(serverUri, draft, headers, connecttimeout);
		this.gateway = gateway;
	}

	public WSGateway(IGateway gateway, URI serverUri, Draft draft) {
		super(serverUri, draft);
		this.gateway = gateway;
	}

	public WSGateway(IGateway gateway, URI serverURI) {
		super(serverURI);
		this.gateway = gateway;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		try {
			String[] usernames = gateway.getUsernames();
			MessageUserInfo[] users = new MessageUserInfo[usernames.length];
			for (int i = 0, len = usernames.length; i < len; i++) {
				users[i] = new MessageUserInfo(usernames[i], null);
			}
			send(new MessageGatewayConnect(gateway.getId(), users).toJson().toString());
		}
		catch (RuntimeException e) {
			
		}
	}

	@Override
	public void onMessage(String rawMessage) {
		Message message = Message.parse(rawMessage);
		if (message == null) {
			ModLoader.outputError("Invalid message received: " + rawMessage);
		}
		else {
			message.handleAsGateway(gateway);
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
