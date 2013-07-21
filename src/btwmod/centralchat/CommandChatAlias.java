package btwmod.centralchat;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmod.centralchat.message.MessageChatAlias;
import btwmods.commands.CommandBaseExtended;

public class CommandChatAlias extends CommandBaseExtended {

	private final mod_CentralChat mod;

	public CommandChatAlias(mod_CentralChat mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "chatalias";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 3 && isStringMatch(args, 0, "set")) {
			mod.queueMessage(MessageChatAlias.buildSet(args[1], args[2], sender.getCommandSenderName()));
		}
		else if (args.length == 2 && isStringMatch(args, 0, "remove")) {
			mod.queueMessage(MessageChatAlias.buildSet(args[1], null, sender.getCommandSenderName()));
		}
		else if (args.length == 1) {
			mod.queueMessage(MessageChatAlias.buildGet(args[0], sender.getCommandSenderName()));
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <username> | ( set <username> <alias> ) | ( remove <username> )";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, new String[] { "set", "remove" });
			
		else if (isStringMatch(args, 0, "set") && args.length == 2)
			return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		
		return super.addTabCompletionOptions(sender, args);
	}
}
