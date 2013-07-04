package btwmod.chat.central;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public static final String PROTOCOL_NAME = "btw-json";
	
	public final Map<String, String> userKeys = new HashMap<String, String>();
	public final Map<String, String> serverKeys = new HashMap<String, String>();

	public ChatServer() throws UnknownHostException {
		super();
		init();
	}

	public ChatServer(InetSocketAddress address, int decodercount, List<Draft> drafts) {
		super(address, decodercount, drafts);
		init();
	}

	public ChatServer(InetSocketAddress address, int decoders) {
		super(address, decoders);
		init();
	}

	public ChatServer(InetSocketAddress address, List<Draft> drafts) {
		super(address, drafts);
		init();
	}

	public ChatServer(InetSocketAddress address) {
		super(address);
		init();
	}
	
	protected void init() {
		loadKeys();
	}
	
	public void loadKeys() {
		userKeys.clear();
		serverKeys.clear();
		
		userKeys.put("andrem", "502021");
		serverKeys.put("bts1", "1234");
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		String protocol = handshake.getFieldValue("Sec-WebSocket-Protocol");
		
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to " + conn.getResourceDescriptor());
		
		if (!PROTOCOL_NAME.equalsIgnoreCase(protocol) && false) {
			conn.close(CloseFrame.PROTOCOL_ERROR, "Protocol must be " + PROTOCOL_NAME);
		}
		else {
			validateConn(conn, ResourceConfig.parse(conn.getResourceDescriptor()));
		}
	}
	
	public boolean validateUserKey(String id, String key) {
		if (id == null || key == null)
			return false;
		
		String storedKey = userKeys.get(id);
		return key.equals(storedKey);
	}
	
	public boolean validateServerKey(String id, String key) {
		if (id == null || key == null)
			return false;
		
		String storedKey = serverKeys.get(id);
		return key.equals(storedKey);
	}
	
	public boolean validateConn(WebSocket conn, ResourceConfig config) {
		if ("user".equalsIgnoreCase(config.clientType) && validateUserKey(config.id, config.key)) {
			return true;
		}
		else if ("server".equalsIgnoreCase(config.clientType) && validateServerKey(config.id, config.key)) {
			return true;
		}
		
		conn.close(CloseFrame.NORMAL, "Invalid resource descriptor: " + conn.getResourceDescriptor());
		return false;
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has disconnected" + (reason == null || reason.equals("") ? " (no reason)." : ": " + reason));
	}

	@Override
	public void onMessage(WebSocket conn, String rawMessage) {
		ResourceConfig config = ResourceConfig.parse(conn.getResourceDescriptor());
		if (validateConn(conn, config)) {
			Message message = Message.parse(rawMessage);
			
			if (message != null && message.canSendMessage(config)) {
				System.out.println(conn.getRemoteSocketAddress() + " sent: " + message.toJson());
				
				if (message.canSendMessage(config))
					message.handleMessage(this, conn, config);
			}
			else {
				System.out.println(conn.getRemoteSocketAddress() + " sent invalid message (logged).");
				conn.close(CloseFrame.PROTOCOL_ERROR, "Invalid message received.");
				// TODO: Log invalid messages.
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
