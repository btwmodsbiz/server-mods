package btwmod.centralchat;

public interface IGateway {
	public void addRestorableChat(String chat);
	public void onConnect(MessageConnect connect);
	public void onDisconnect(MessageDisconnect disconnect);
	public void setAlias(String username, String alias);
}
