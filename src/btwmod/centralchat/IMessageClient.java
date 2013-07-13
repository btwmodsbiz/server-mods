package btwmod.centralchat;

public interface IMessageClient {
	public void addRestorableChat(String chat);
	public void onConnect(MessageConnect connect);
	public void onDisconnect(MessageDisconnect disconnect);
}
