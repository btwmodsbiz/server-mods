package btwmod.centralchat;

import btwmod.centralchat.message.MessageConnect;
import btwmod.centralchat.message.MessageDisconnect;

public interface IGateway {
	public String getId();
	public void addRestorableChat(String chat);
	public void onConnect(MessageConnect connect);
	public void onDisconnect(MessageDisconnect disconnect);
	public void setAlias(String username, String alias);
	public String[] getUsernames();
}
