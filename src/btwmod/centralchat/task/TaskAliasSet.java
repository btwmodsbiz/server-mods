package btwmod.centralchat.task;

import btwmods.ChatAPI;

public class TaskAliasSet implements Runnable {
	
	public final String username;
	public final String alias;
	
	public TaskAliasSet(String username, String alias) {
		this.username = username;
		this.alias = alias;
	}

	@Override
	public void run() {
		if (alias == null)
			ChatAPI.removeAlias(username, true);
		else
			ChatAPI.setAlias(username, alias);
	}
}
