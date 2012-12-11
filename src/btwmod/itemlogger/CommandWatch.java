package btwmod.itemlogger;

import java.util.Arrays;
import java.util.List;

import btwmods.Util;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;

public class CommandWatch extends CommandBase {

	private final mod_ItemLogger mod;

	public CommandWatch(mod_ItemLogger mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "watch";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
			List<String> names = Arrays.asList(mod.getWatchedPlayers());
			if (names.size() == 0) {
				sender.sendChatToPlayer(mod.getName() + " is not watching any players.");
			}
			else {
				List<String> messages = Util.combineIntoMaxLengthMessages(names, Packet3Chat.maxChatLength, ", ", true);
				for (String message : messages) {
					sender.sendChatToPlayer(message);
				}
			}
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
			if (mod.addWatchedPlayer(args[1].toLowerCase().trim())) {
				sender.sendChatToPlayer(mod.getName() + " is now watching player '" + args[1].toLowerCase().trim() + "'.");
			}
			else {
				sender.sendChatToPlayer(mod.getName() + " is already watching player '" + args[1].toLowerCase().trim() + "'.");
			}
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
			if (mod.removeWatchedPlayer(args[1].toLowerCase().trim())) {
				sender.sendChatToPlayer(mod.getName() + " is no longer watching player '" + args[1].toLowerCase().trim() + "'.");
			}
			else {
				sender.sendChatToPlayer(mod.getName() + " is not watching player '" + args[1].toLowerCase().trim() + "'.");
			}
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <list|add|remove> [name]";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, new String[] { "list", "add", "remove" });
			
		return super.addTabCompletionOptions(sender, args);
	}

}
