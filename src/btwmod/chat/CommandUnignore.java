package btwmod.chat;

import java.util.List;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;

public class CommandUnignore extends CommandBaseExtended {

	private final mod_Chat mod;

	public CommandUnignore(mod_Chat mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "unignore";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (sender instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)sender;
			if (args.length == 1) {
				if (mod.isIgnoring(player.username, args[0]) && mod.removeIgnore(player.username, args[0])) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "You are no longer ignoring " + args[0].toLowerCase() + ".");
				}
				else {
					sender.sendChatToPlayer(Util.COLOR_RED + "You were not ignoring " + args[0].toLowerCase() + ".");
				}
			}
			else {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <username>";
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return sender instanceof EntityPlayer;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsFromIterableMatchingLastWord(args, mod.getIgnores(sender.getCommandSenderName()));
		else
			return super.addTabCompletionOptions(sender, args);
	}
}
