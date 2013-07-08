package btwmod.centralchat;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import btwmods.io.Settings;

public class ChatServer extends WebSocketServer {
	
	public static void main(String[] args) {
		System.out.println("Main: " + Thread.currentThread().getName() + " # " + Thread.currentThread().getId());
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
	private Set<String> chatColors = new HashSet<String>();

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
		
		//addColor("black"); //, Util.COLOR_BLACK, "#000");
		//addColor("navy"); //, Util.COLOR_NAVY, "#00A");
		addColor("green"); //, Util.COLOR_GREEN, "#0A0");
		addColor("teal"); //, Util.COLOR_TEAL, "#0AA");
		addColor("maroon"); //, Util.COLOR_MAROON, "#A00");
		addColor("purple"); //, Util.COLOR_PURPLE, "#A0A");
		addColor("gold"); //, Util.COLOR_GOLD, "#FA0");
		addColor("silver"); //, Util.COLOR_SILVER, "#AAA");
		addColor("grey"); //, Util.COLOR_GREY, "#555");
		addColor("blue"); //, Util.COLOR_BLUE, "#55F");
		addColor("lime"); //, Util.COLOR_LIME, "#5F5");
		addColor("aqua"); //, Util.COLOR_AQUA, "#5FF");
		addColor("red"); //, Util.COLOR_RED, "#F55");
		addColor("pink"); //, Util.COLOR_PINK, "#F5F");
		addColor("yellow"); //, Util.COLOR_YELLOW, "#FF5");
		addColor("white"); //, Util.COLOR_WHITE, "#FFF");
		addColor("off"); //, Util.COLOR_WHITE, "#FFF");
		
		loadKeys();
	}
	
	public static Settings loadSettings(File file) throws IOException {
		if (file.isFile())
			return Settings.readSavableSettings(file);
		else
			return new Settings();
	}
	
	public void loadKeys() {
		addUserKey("andrem", "502021");
		addServerKey("bts1", "1234");
		data.set("ChatAliases", "andrem", "Butts");
		data.set("ChatColors", "andrem", "teal");
	}
	
	private void addColor(String color) {
		//if (!bannedColors.contains(color)) {
			chatColors.add(color.toLowerCase());
		//}
	}
	
	public boolean isValidColor(String color) {
		return color.equalsIgnoreCase("off") || chatColors.contains(color.toLowerCase());
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

	public void setChatColor(String username, String color) {
		data.set("ChatColors", username, color);
	}

	public String getChatAlias(String username) {
		return data.get("ChatAliases", username);
	}

	public void setChatAlias(String username, String alias) {
		data.set("ChatAliases", username, alias);
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
