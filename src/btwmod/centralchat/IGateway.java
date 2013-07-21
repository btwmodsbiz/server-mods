package btwmod.centralchat;

public interface IGateway {
	public String getId();
	public void addRestorableChat(String chat);
	public void setAlias(String username, String alias);
	public String[] getUsernames();
	public void onSuccessfulConnect();
	public void onWaitForReconnect();
}
