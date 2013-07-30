package btwmod.centralchat;

import btwmod.centralchat.message.Message;

public interface IGateway {
	public String getId();
	public void addRestorableChat(String chat);
	public void setAlias(String username, String alias);
	public void removeAllAliases();
	public String[] getUsernames();
	public void onSuccessfulConnect();
	public void onWaitForReconnect();
	public void sendChatToAllPlayers(String message);
	public void sendChatToAllPlayers(String message, String senderUsername);
	public void sendChatToPlayer(String message, String targetUsername);
	public void sendChatToAdmins(String message);
	public void requestKey(String username, boolean forceNew);
	public void send(Message message);
}
