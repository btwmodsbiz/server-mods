package btwmod.centralchat;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;

import btwmod.centralchat.message.MessagePing;
import btwmod.centralchat.message.MessagePong;
import btwmod.centralchat.struct.User;
import btwmod.centralchat.struct.UserAlias;
import btwmods.io.Settings;
import btwmods.util.CaselessKey;
import btwmods.util.ValuePair;

public class ServerController implements IServer {
	
	public static final Pattern pattern = Pattern.compile("/\\$\\{([A-Za-z0-9_]{1,16})\\}/");
	
	public static void main(String[] args) {
		File config = new File(new File("."), "chatserver.dat");
		System.out.println("Starting server...");
		try {
			System.out.println("Loading config from " + config.getCanonicalPath());
		} catch (IOException e1) {
			System.out.println("Loading config from " + config.toString());
		}
		
		try {
			InetSocketAddress address = new InetSocketAddress(8585);
			ServerController controller = new ServerController(config, address);
			controller.start();
			
		} catch (IOException e) {
			System.err.println("Failed (" + e.getClass().getSimpleName() + ") to start server: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected final WSServer wsServer;
	protected final Settings data;
	protected final Map<String, Map<String, String>> loggedInUsers = new HashMap<String, Map<String, String>>();
	
	public int restoreMessagesMax = 50;
	public long chatRestoreTimeout = 20L;
	private Deque<ValuePair<String, Long>> restoreMessages = new ArrayDeque<ValuePair<String, Long>>();
	
	protected final PingManager pingManager;
	
	public ServerController(File dataFile, InetSocketAddress address) throws IOException {
		wsServer = new WSServer(this, address);
		this.data = loadSettings(dataFile);
		new Thread(pingManager = new PingManager(this, 5L, 12L), "").start();
	}
	
	public void start() {
		attachShutDownHook();
		wsServer.start();
		System.out.println("Server started on " + wsServer.getAddress().getHostName() + ":" + wsServer.getPort());
	}
	
	public static Settings loadSettings(File file) throws IOException {
		if (file.isFile())
			return Settings.readSavableSettings(file);
		else
			return new Settings(file);
	}
	
	@Override
	public boolean saveSettings() {
		try {
			synchronized (data) {
				data.saveSettings();
			}
			return true;
			
		} catch (IOException e) {
			System.err.println("Failed (" + e.getClass().getSimpleName() + ") to save data file: " + e.getMessage());
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public void onOpen(WebSocket conn, ResourceConfig config) {
		pingManager.onOpen(conn);
		
		if (config.clientType == ClientType.USER) {
			// Restore chat.
			List<String> messages = new ArrayList<String>();
			synchronized (restoreMessages) {
				for (ValuePair<String, Long> pair : restoreMessages) {
					messages.add(pair.key);
				}
			}
			for (String message : messages) {
				conn.send(message);
			}
		}
	}
	
	@Override
	public void onClose(WebSocket conn, ResourceConfig config) {
		pingManager.onClose(conn);
	}
	
	@Override
	public void onPing(WebSocket conn, ResourceConfig config, MessagePing ping) {
		pingManager.onPing(conn, ping);
	}
	
	@Override
	public void onPong(WebSocket conn, ResourceConfig config, MessagePong pong) {
		pingManager.onPong(conn, pong);
	}

	@Override
	public WebSocket[] getConnections() {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			return connections.toArray(new WebSocket[connections.size()]);
		}
	}

	@Override
	public void addActualUsername(String username) {
		synchronized (data) {
			data.set("ActualUsernames", username, username);
		}
	}

	@Override
	public String getActualUsername(String username) {
		return getActualUsername(username, username);
	}
	
	@Override
	public String getActualUsername(String username, String defaultValue) {
		synchronized (data) {
			String actualUsername = data.get("ActualUsernames", username);
			return actualUsername == null ? defaultValue : actualUsername;
		}
	}
	
	@Override
	public String getUserKey(String id) {
		synchronized (data) {
			return data.get("UserKeys", id);
		}
	}
	
	@Override
	public void addUserKey(String id, String key) {
		synchronized (data) {
			data.set("UserKeys", id, key);
		}
	}
	
	@Override
	public void removeUserKey(String id) {
		synchronized (data) {
			data.removeKey("UserKeys", id);
		}
	}
	
	@Override
	public boolean validateUserKey(String id, String key) {
		if (id == null || key == null)
			return false;

		synchronized (data) {
			String storedKey = data.get("UserKeys", id);
			return key.equals(storedKey);
		}
	}
	
	@Override
	public void addGatewayKey(String id, String key) {
		synchronized (data) {
			data.set("GatewayKeys", id, key);
		}
	}
	
	@Override
	public void removeGatewayKey(String id) {
		synchronized (data) {
			data.removeKey("GatewayKeys", id);
		}
	}
	
	@Override
	public boolean validateGatewayKey(String id, String key) {
		if (id == null || key == null)
			return false;

		synchronized (data) {
			String storedKey = data.get("GatewayKeys", id);
			return key.equals(storedKey);
		}
	}

	@Override
	public boolean isValidConfig(ResourceConfig config) {
		return (config.clientType == ClientType.USER && validateUserKey(config.id, config.key))
				|| (config.clientType == ClientType.GATEWAY && validateGatewayKey(config.id, config.key));
	}

	@Override
	public void sendToAll(String message) {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				try {
					connection.send(message);
				}
				catch (RuntimeException e) {
					System.err.println("An exception (" + e.getClass().getSimpleName() + ") was raised while sending a message to a connection: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void sendToAllForUser(String message, String username) {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				ResourceConfig config = ResourceConfig.parse(connection.getResourceDescriptor());
				if (config.clientType == ClientType.GATEWAY
						|| (config.clientType == ClientType.USER && config.id.equalsIgnoreCase(username))) {
					
					try {
						connection.send(message);
					}
					catch (RuntimeException e) {
						System.err.println("An exception (" + e.getClass().getSimpleName() + ") was raised while sending a message to a connection: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	@Override
	public void sendToAllGateways(String message) {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				ResourceConfig config = ResourceConfig.parse(connection.getResourceDescriptor());
				if (config.clientType == ClientType.GATEWAY) {
					try {
						connection.send(message);
					}
					catch (RuntimeException e) {
						System.err.println("An exception (" + e.getClass().getSimpleName() + ") was raised while sending a message to a connection: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public String getChatColor(String username) {
		synchronized (data) {
			return data.get("ChatColors", username);
		}
	}

	@Override
	public boolean setChatColor(String username, String color) {
		ChatColors chatColor = ChatColors.get(color);
		if (color != null && chatColor == null)
			return false;

		synchronized (data) {
			if (color == null || chatColor.isDefault)
				data.removeKey("ChatColors", username);
			else
				data.set("ChatColors", username, color);
		}
		
		return true;
	}

	@Override
	public String getChatAlias(String username) {
		synchronized (data) {
			return data.get("ChatAliases", username);
		}
	}

	@Override
	public boolean setChatAlias(String username, String alias) {
		synchronized (data) {
			if (alias == null) {
				data.removeKey("ChatAliases", username);
				return true;
			}
			else {
				alias = alias.trim();
				if (alias.length() < 1 || alias.length() > 16)
					return false;
				
				data.set("ChatAliases", username, alias);
				return true;
			}
		}
	}
	
	@Override
	public UserAlias[] getChatAliases() {
		synchronized (data) {
			Set<CaselessKey> usernames = data.getSectionKeys("ChatAliases");
			UserAlias[] aliases = new UserAlias[usernames.size()];
			int i = 0;
			for (CaselessKey username : usernames) { 
				aliases[i++] = new UserAlias(username.key, data.get("ChatAliases", username.key));
			}
			return aliases;
		}
	}
	
	@Override
	public String replaceUsernamesWithAliases(String message) {
		int i = 0;
		StringBuilder ret = new StringBuilder();
		
		Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            MatchResult matchResult = matcher.toMatchResult();
            
            if (matchResult.start() > i)
            	ret.append(message.substring(i, matchResult.start()));
            
            String username = matchResult.group(1);
            String replacement = getChatAlias(username);
            ret.append(replacement == null ? username : replacement);
            
            message = message.substring(0, matchResult.start()) + (replacement == null ? username : replacement) + message.substring(matchResult.end());
            matcher.reset(message);
            i = matcher.end();
        }
        
        if (!matcher.hitEnd())
        	ret.append(message.substring(i));
        
        return message;
	}

	@Override
	public void addLoggedInUser(String gateway, String username) {
		addLoggedInUser(gateway, new String[] { username });
	}

	@Override
	public void addLoggedInUser(String gateway, String[] usernames) {
		synchronized (loggedInUsers) {
			Map<String, String> gatewayList = loggedInUsers.get(gateway);
			if (gatewayList == null)
				loggedInUsers.put(gateway, gatewayList = new HashMap<String, String>());
			
			for (String username : usernames)
				gatewayList.put(username.toLowerCase(), username);
		}
	}

	@Override
	public void removeLoggedInUser(String gateway, String username) {
		synchronized (loggedInUsers) {
			Map<String, String> gatewayList = loggedInUsers.get(gateway);
			if (gatewayList != null)
				gatewayList.remove(username.toLowerCase());
		}
	}

	@Override
	public void removeLoggedInUsers(String gateway) {
		synchronized (loggedInUsers) {
			loggedInUsers.remove(gateway);
		}
	}
	
	@Override
	public User[] getLoggedInUserList() {
		List<User> userList = new ArrayList<User>();
		synchronized (loggedInUsers) {
			for (Entry<String, Map<String, String>> gatewayList : loggedInUsers.entrySet()) {
				for (Entry<String, String> userEntry : gatewayList.getValue().entrySet()) {
					userList.add(new User(userEntry.getValue(), gatewayList.getKey()));
				}
			}
		}
		return userList.toArray(new User[userList.size()]);
	}
	
	@Override
	public User[] getLoggedInUserList(String gateway) {
		List<User> userList = new ArrayList<User>();
		synchronized (loggedInUsers) {
			for (Entry<String, Map<String, String>> gatewayList : loggedInUsers.entrySet()) {
				if (gateway == gatewayList.getKey() || gateway != null && gateway.equalsIgnoreCase(gatewayList.getKey())) {
					for (Entry<String, String> userEntry : gatewayList.getValue().entrySet()) {
						userList.add(new User(userEntry.getValue(), gatewayList.getKey()));
					}
				}
			}
		}
		return userList.toArray(new User[userList.size()]);
	}
	
	@Override
	public boolean disconnectSameClient(WebSocket conn, ResourceConfig config) {
		Collection<WebSocket> connections = wsServer.connections();
		boolean disconnectedClient = false;
		synchronized (connections) {
			for (WebSocket connection : connections) {
				if (connection != conn && config.isSameClient(ResourceConfig.parse(connection.getResourceDescriptor()))) {
					try {
						connection.close(CloseFrame.NORMAL, CloseMessage.LOGIN_OTHER_LOCATION.toString());
						disconnectedClient = true;
					}
					catch (RuntimeException e) { }
				}
			}
		}
		return disconnectedClient;
	}
	
	@Override
	public boolean hasConnectedClient(WebSocket conn, ResourceConfig config) {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				if (conn != connection && config.isSameClient(ResourceConfig.parse(connection.getResourceDescriptor())))
					return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void addRestorableMessage(String message) {
		synchronized (restoreMessages) {
			restoreMessages.add(new ValuePair(message, new Long(System.currentTimeMillis())));
			if (restoreMessages.size() > restoreMessagesMax)
				restoreMessages.pollFirst();
		}
	}
	
	protected void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new ServerShutdownHook(wsServer), "ShutdownHook Thread"));
	}
	
	protected class ServerShutdownHook implements Runnable {
		private final WSServer server;

		public ServerShutdownHook(WSServer server) {
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
