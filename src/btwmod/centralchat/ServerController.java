package btwmod.centralchat;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;

import btwmods.io.Settings;

public class ServerController implements IServer {
	
	public static void main(String[] args) {
		System.out.println("Starting server...");
		
		try {
			InetSocketAddress address = new InetSocketAddress(8585);
			ServerController controller = new ServerController(new File(new File("."), "chatserver.dat"), address);
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
	
	public ServerController(File dataFile, InetSocketAddress address) throws IOException {
		wsServer = new WSServer(this, address);
		this.data = loadSettings(dataFile);
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
	public void addActualUsername(String username) {
		synchronized (data) {
			data.set("ActualUsernames", username, username);
		}
	}

	@Override
	public String getActualUsername(String username) {
		return getActualUsername(username, null);
	}
	
	@Override
	public String getActualUsername(String username, String defaultValue) {
		synchronized (data) {
			String actualUsername = data.get("ActualUsernames", username);
			return actualUsername == null ? defaultValue : actualUsername;
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
	public void addServerKey(String id, String key) {
		synchronized (data) {
			data.set("ServerKeys", id, key);
		}
	}
	
	@Override
	public void removeServerKey(String id) {
		synchronized (data) {
			data.removeKey("ServerKeys", id);
		}
	}
	
	@Override
	public boolean validateServerKey(String id, String key) {
		if (id == null || key == null)
			return false;

		synchronized (data) {
			String storedKey = data.get("ServerKeys", id);
			return key.equals(storedKey);
		}
	}

	@Override
	public boolean isValidConfig(ResourceConfig config) {
		return (config.clientType == ClientType.USER && validateUserKey(config.id, config.key))
				|| (config.clientType == ClientType.GATEWAY && validateServerKey(config.id, config.key));
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
	public void sendToAllServers(String message) {
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
	public void addLoggedInUser(String server, String username) {
		addLoggedInUser(server, new String[] { username });
	}

	@Override
	public void addLoggedInUser(String server, String[] usernames) {
		synchronized (loggedInUsers) {
			String key = server;
			Map<String, String> serverList = loggedInUsers.get(key);
			if (serverList == null)
				loggedInUsers.put(key, serverList = new HashMap<String, String>());
			
			for (String username : usernames)
				serverList.put(username.toLowerCase(), username);
		}
	}

	@Override
	public void removeLoggedInUser(String server, String username) {
		synchronized (loggedInUsers) {
			String key = server;
			Map<String, String> serverList = loggedInUsers.get(key);
			if (serverList != null)
				serverList.remove(username.toLowerCase());
		}
	}
	
	@Override
	public MessageUserList getLoggedInUserList() {
		synchronized (loggedInUsers) {
			List<MessageUserList.MessageUserEntry> messageList = new ArrayList<MessageUserList.MessageUserEntry>();
			for (Entry<String, Map<String, String>> serverList : loggedInUsers.entrySet()) {
				for (Entry<String, String> userEntry : serverList.getValue().entrySet()) {
					messageList.add(new MessageUserList.MessageUserEntry(userEntry.getValue(), serverList.getKey(), getChatColor(userEntry.getValue()), getChatAlias(userEntry.getValue())));
				}
			}
			return new MessageUserList(messageList.toArray(new MessageUserList.MessageUserEntry[messageList.size()]));
		}
	}
	
	@Override
	public void disconnectSameClient(WebSocket conn, ResourceConfig config) {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				if (connection != conn && config.isSameClient(ResourceConfig.parse(connection.getResourceDescriptor())))
					connection.close(CloseFrame.NORMAL, CloseMessage.LOGIN_OTHER_LOCATION.toString());
			}
		}
	}
	
	@Override
	public boolean hasConnectedClient(ResourceConfig config) {
		Collection<WebSocket> connections = wsServer.connections();
		synchronized (connections) {
			for (WebSocket connection : connections) {
				if (config.isSameClient(ResourceConfig.parse(connection.getResourceDescriptor())))
					return true;
			}
		}
		
		return false;
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
