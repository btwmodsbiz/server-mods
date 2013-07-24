package btwmod.centralchat.task;

import btwmods.ChatAPI;

public class TaskAdminMessage implements Runnable {
	
	public final String message;
	
	public TaskAdminMessage(String message) {
		this.message = message;
	}

	@Override
	public void run() {
		ChatAPI.sendChatToAllAdmins(message);
	}

}
