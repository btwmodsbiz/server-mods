package btwmod.chat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.ChatAPI;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;

public class CommandIgnore extends CommandBaseExtended {

	private final mod_Chat mod;

	public CommandIgnore(mod_Chat mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "ignore";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (sender instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)sender;
			if (args.length == 0) {
				mod.sendIgnoreList(player, true);
			}
			else if (args.length == 1 || (args.length == 2 && isInt(args, 1))) {
				if (args[0].equalsIgnoreCase(player.username) || args[0].equalsIgnoreCase(ChatAPI.getUsernameAliased(player.username)))
					throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
				
				int minutes = args.length == 2 ? parseIntBounded(sender, args[1], 1, mod.maxIgnoreMinutes) : mod.defaultIgnoreMinutes;
				mod.addIgnore(player.username, args[0], minutes);
				sender.sendChatToPlayer(Util.COLOR_YELLOW + "Ignoring " + args[0].toLowerCase() + " for " + Util.formatSeconds(minutes * 60L) + ".");
			}
			else {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <username> [<minutes>]";
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return sender instanceof EntityPlayer;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			Set<String> usernames = new TreeSet<String>();
			usernames.addAll(Arrays.asList(ChatAPI.getAllUsernamesAliased()));
			usernames.addAll(Arrays.asList(MinecraftServer.getServer().getAllUsernames()));
			usernames.remove(sender.getCommandSenderName());
			usernames.remove(ChatAPI.getUsernameAliased(sender.getCommandSenderName()));
			return getListOfStringsFromIterableMatchingLastWord(args, usernames);
		}
		else
			return super.addTabCompletionOptions(sender, args);
	}
}
