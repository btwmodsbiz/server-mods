package btwmod.centralchat;

import org.java_websocket.WebSocket;

import btwmod.centralchat.list.User;
import btwmod.centralchat.list.UserAlias;

public interface IServer {
	public boolean saveSettings();
	
	public void addActualUsername(String username);
	public String getActualUsername(String username);
	public String getActualUsername(String username, String defaultValue);
	
	public void addUserKey(String id, String key);
	public void removeUserKey(String id);
	public boolean validateUserKey(String id, String key);
	
	public void addGatewayKey(String id, String key);
	public void removeGatewayKey(String id);
	public boolean validateGatewayKey(String id, String key);
	
	public boolean isValidConfig(ResourceConfig config);
	
	public void sendToAll(String message);
	public void sendToAllForUser(String message, String username);
	public void sendToAllGateways(String message);
	
	public String getChatColor(String username);
	public boolean setChatColor(String username, String color);
	
	public String getChatAlias(String username);
	public boolean setChatAlias(String username, String alias);
	public UserAlias[] getChatAliases();
	public String replaceUsernamesWithAliases(String message);
	
	public void addLoggedInUser(String gateway, String username);
	public void addLoggedInUser(String gateway, String[] usernames);
	public void removeLoggedInUser(String gateway, String username);
	public void removeLoggedInUsers(String gateway);
	
	public User[] getLoggedInUserList();
	public User[] getLoggedInUserList(String gateway);
	
	public boolean hasConnectedClient(ResourceConfig config);
	public void disconnectSameClient(WebSocket conn, ResourceConfig config);
}
