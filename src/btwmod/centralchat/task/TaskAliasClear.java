package btwmod.centralchat.task;

import btwmods.ChatAPI;

public class TaskAliasClear implements Runnable {
	@Override
	public void run() {
		ChatAPI.removeAllAliases(true);
	}
}
