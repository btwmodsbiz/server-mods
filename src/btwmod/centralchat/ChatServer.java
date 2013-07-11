package btwmod.centralchat;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import btwmods.io.Settings;

public class ChatServer extends WebSocketServer {
	
	public static void main(String[] args) {
		System.out.println("Starting server...");
		
		InetSocketAddress address = new InetSocketAddress(8585);
		ChatServer server;
		try {
			server = new ChatServer(new File(new File("."), "chatserver.dat"), address);
			server.attachShutDownHook();
			server.start();
			System.out.println("Server started on " + server.getAddress().getHostName() + ":" + server.getPort());
			
		} catch (IOException e) {
			System.err.println("Failed (" + e.getClass().getSimpleName() + ") to start server: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	//public static final String PROTOCOL_NAME = "btw-json";
	
	private final Settings data;

	public ChatServer(File dataFile) throws IOException {
		super();
		this.data = loadSettings(dataFile);
		init();
	}

	public ChatServer(File dataFile, InetSocketAddress address, int decodercount, List<Draft> drafts) throws IOException {
		super(address, decodercount, drafts);
		this.data = loadSettings(dataFile);
		init();
	}

	public ChatServer(File dataFile, InetSocketAddress address, int decoders) throws IOException {
		super(address, decoders);
		this.data = loadSettings(dataFile);
		init();
	}

	public ChatServer(File dataFile, InetSocketAddress address, List<Draft> drafts) throws IOException {
		super(address, drafts);
		this.data = loadSettings(dataFile);
		init();
	}

	public ChatServer(File dataFile, InetSocketAddress address) throws IOException {
		super(address);
		this.data = loadSettings(dataFile);
		init();
	}
	
	protected void init() {
		
	}
	
	public static Settings loadSettings(File file) throws IOException {
		if (file.isFile())
			return Settings.readSavableSettings(file);
		else
			return new Settings(file);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		//String protocol = handshake.getFieldValue("Sec-WebSocket-Protocol");
		
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to " + conn.getResourceDescriptor());
		
		/*if (!PROTOCOL_NAME.equalsIgnoreCase(protocol) && false) {
			conn.close(CloseFrame.PROTOCOL_ERROR, "Protocol must be " + PROTOCOL_NAME);
		}
		else {*/
			validateConn(conn, ResourceConfig.parse(conn.getResourceDescriptor()));
		//}
	}
	
	public void addUserKey(String id, String key) {
		data.set("UserKeys", id, key);
	}
	
	public void removeUserKey(String id) {
		data.removeKey("UserKeys", id);
	}
	
	public boolean validateUserKey(String id, String key) {
		if (id == null || key == null)
			return false;
		
		String storedKey = data.get("UserKeys", id);
		return key.equals(storedKey);
	}
	
	public void addServerKey(String id, String key) {
		data.set("ServerKeys", id, key);
	}
	
	public void removeServerKey(String id) {
		data.removeKey("ServerKeys", id);
	}
	
	public boolean validateServerKey(String id, String key) {
		if (id == null || key == null)
			return false;

		String storedKey = data.get("ServerKeys", id);
		return key.equals(storedKey);
	}
	
	public boolean validateConn(WebSocket conn, ResourceConfig config) {
		if (config.clientType == ClientType.USER && validateUserKey(config.id, config.key)) {
			return true;
		}
		else if (config.clientType == ClientType.SERVER && validateServerKey(config.id, config.key)) {
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
				System.out.println("<" + conn.getRemoteSocketAddress() + "> " + message.toJson());
				
				if (message.canSendMessage(config))
					message.handleAsServer(this, conn, config);
			}
			else {
				System.out.println(conn.getRemoteSocketAddress() + " sent invalid message (logged).");
				System.err.println(">>" + rawMessage);
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
	
	public void sendToAllForUser(String message, String username) {
		Collection<WebSocket> connections = this.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				ResourceConfig config = ResourceConfig.parse(connection.getResourceDescriptor());
				if (config.clientType == ClientType.SERVER
						|| (config.clientType == ClientType.USER && config.id.equalsIgnoreCase(username))) {
					
					connection.send(message);
				}
			}
		}
	}
	
	public void sendToAllServers(String message) {
		Collection<WebSocket> connections = this.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				ResourceConfig config = ResourceConfig.parse(connection.getResourceDescriptor());
				if (config.clientType == ClientType.SERVER) {
					connection.send(message);
				}
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

	public String getChatColor(String username) {
		return data.get("ChatColors", username);
	}

	public boolean setChatColor(String username, String color) {
		ChatColors chatColor = ChatColors.get(color);
		if (color != null && chatColor == null)
			return false;
		
		if (color == null || chatColor.isDefault)
			data.removeKey("ChatColors", username);
		else
			data.set("ChatColors", username, color);
		
		return true;
	}

	public String getChatAlias(String username) {
		return data.get("ChatAliases", username);
	}

	public boolean setChatAlias(String username, String alias) {
		alias = alias.trim();
		if (alias.length() < 1 || alias.length() > 16)
			return false;
		
		data.set("ChatAliases", username, alias);
		return true;
	}
	
	public boolean save() {
		try {
			data.saveSettings();
			return true;
			
		} catch (IOException e) {
			System.err.println("Failed (" + e.getClass().getSimpleName() + ") to save data file: " + e.getMessage());
			e.printStackTrace();
		}
		
		return false;
	}
}
