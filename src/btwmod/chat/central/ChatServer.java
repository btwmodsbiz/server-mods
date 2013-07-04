package btwmod.chat.central;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class ChatServer extends WebSocketServer {
	
	public static void main(String[] args) {
		System.out.println("Main: " + Thread.currentThread().getName() + " # " + Thread.currentThread().getId());
		System.out.println("Starting server...");
		
		InetSocketAddress address = new InetSocketAddress(8585);
		ChatServer server = new ChatServer(address);
		server.attachShutDownHook();
		server.start();
		
		System.out.println("Server started on " + server.getAddress().getHostName() + ":" + server.getPort());
	}
	
	public Set<String> serverKeys = new HashSet<String>();

	public ChatServer() throws UnknownHostException {
		super();
	}

	public ChatServer(InetSocketAddress address, int decodercount, List<Draft> drafts) {
		super(address, decodercount, drafts);
	}

	public ChatServer(InetSocketAddress address, int decoders) {
		super(address, decoders);
	}

	public ChatServer(InetSocketAddress address, List<Draft> drafts) {
		super(address, drafts);
	}

	public ChatServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		String protocol = handshake.getFieldValue("Sec-WebSocket-Protocol");
		
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to " + conn.getResourceDescriptor());
		
		if (!"mc-chat".equalsIgnoreCase(protocol) && false) {
			conn.close(CloseFrame.PROTOCOL_ERROR, "Protocol must be mc-chat");
		}
		else {
			validateConn(conn, ResourceConfig.parse(conn.getResourceDescriptor()));
		}
	}
	
	public boolean validateUserKey(String id, String key) {
		return "andrem".equals(id) && "502021".equals(key);
	}
	
	public boolean validateServerKey(String id, String key) {
		return "bts1".equals(id) && "1234".equals(key);
	}
	
	public boolean validateConn(WebSocket conn, ResourceConfig config) {
		if ("user".equalsIgnoreCase(config.mode) && validateUserKey(config.id, config.key)) {
			return true;
		}
		else if ("server".equalsIgnoreCase(config.mode) && validateServerKey(config.id, config.key)) {
			return true;
		}
		
		conn.close(CloseFrame.NORMAL, "Invalid resource descriptor: " + conn.getResourceDescriptor());
		return false;
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has disconnected" + (reason == null || reason.equals("") ? " (no reason)." : ": " + reason));
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		ResourceConfig config = ResourceConfig.parse(conn.getResourceDescriptor());
		if (validateConn(conn, config)) {
			Message messageHandler = Message.parse(message);
			if (messageHandler != null) {
				messageHandler.handleMessage(this, conn, config);
			}
			else {
				// TODO: Report error?
			}
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	public void sendToAll(String message) {
		Collection<WebSocket> connections = this.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				connection.send(message);
			}
		}
	}
	
	public void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new ServerShutdownHook(this), "ShutdownHook Thread"));
	}
	
	private class ServerShutdownHook implements Runnable {
		private final ChatServer server;

		public ServerShutdownHook(ChatServer server) {
			this.server = server;
		}

		@Override
		public void run() {
			System.out.println("Stopping Server...");
			
			try {
				server.stop();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("Server Stopped!");
		}
	}
}
