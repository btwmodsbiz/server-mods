package btwmod.centralchat.task;

import btwmods.ChatAPI;

public class TaskMessage implements Runnable {
	
	public final String message;
	public final String senderUsername;
	
	public TaskMessage(String message) {
		this(message, null);
	}
	
	public TaskMessage(String message, String senderUsername) {
		this.message = message;
		this.senderUsername = senderUsername;
	}

	@Override
	public void run() {
		if (senderUsername == null)
			ChatAPI.sendChatToAllPlayers(message);
		else
			ChatAPI.sendChatToAllPlayers(senderUsername, message);
	}
}
