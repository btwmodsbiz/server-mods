package btwmod.centralchat;

public interface IGateway {
	public String getId();
	public void addRestorableChat(String chat);
	public void setAlias(String username, String alias);
	public void removeAllAliases();
	public String[] getUsernames();
	public void onSuccessfulConnect();
	public void onWaitForReconnect();
	public void sendChatToAllPlayers(String message);
	public void sendChatToAllPlayers(String message, String username);
	public void sendChatToAdmins(String message);
}
