package btwmod.centralchat;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
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
			mod.queueMessage(new MessageChatAlias("set", args[1], args[2]));
			/*if (mod.setAlias(args[1], args[2])) {
				sender.sendChatToPlayer(Util.COLOR_YELLOW + "Set alias for player " + args[1].toLowerCase().trim() + " to " + args[2] + ".");
			}
			else {
				sender.sendChatToPlayer(Util.COLOR_RED + "Failed to set alias for player " + args[1].toLowerCase().trim() + " to " + args[2] + ".");
			}*/
		}
		else if (args.length == 2 && isStringMatch(args, 0, "remove")) {
			mod.queueMessage(new MessageChatAlias("set", args[1]));
			/*if (mod.removeAlias(args[1])) {
				sender.sendChatToPlayer(Util.COLOR_YELLOW + "Removed alias for player " + args[1].toLowerCase().trim());
			}
			else {
				sender.sendChatToPlayer(Util.COLOR_RED + "Player " + args[1] + " does not have an alias set.");
			}*/
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " ( ( set <username> <alias> ) | ( remove <username> ) )";
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
