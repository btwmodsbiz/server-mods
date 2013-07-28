package btwmod.centralchat.task;

import btwmods.ChatAPI;

public class TaskMessageTargetted implements Runnable {
	
	public final String message;
	public final String targetUsername;
	
	public TaskMessageTargetted(String message, String targetUsername) {
		this.message = message;
		this.targetUsername = targetUsername;
	}

	@Override
	public void run() {
		ChatAPI.sendChatToPlayer(targetUsername, message);
	}
}
