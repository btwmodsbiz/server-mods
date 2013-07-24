package btwmod.centralchat.task;

public class TaskMessageTargetted implements Runnable {
	
	public final String message;
	public final String senderUsername;
	public final String targetUsername;
	
	public TaskMessageTargetted(String message, String targetUsername) {
		this(message, null, targetUsername);
	}
	
	public TaskMessageTargetted(String message, String senderUsername, String targetUsername) {
		this.message = message;
		this.senderUsername = senderUsername;
		this.targetUsername = targetUsername;
	}

	@Override
	public void run() {
		// TODO
	}
}
