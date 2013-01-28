package btwmod.chat;

import java.util.List;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WrongUsageException;
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
				List<String> ignored = mod.getIgnores(player.username);
				if (ignored == null || ignored.size() == 0) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "You are not ignoring any players.");
				}
				else {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Ignored players:");
					for (String message : Util.combineIntoMaxLengthMessages(ignored, Packet3Chat.maxChatLength, ",", true)) {
						sender.sendChatToPlayer(message);
					}
				}
			}
			else if (args.length == 2 && isStringMatch(args, 1, "remove")) {
				
			}
			else if (args.length == 2) {
				
			}
			else {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
		}
	}

	@Override
	public String getCommandUsage(ICommandSender par1iCommandSender) {
		return "/" + getCommandName() + " [<username> (<time> | remove)]";
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return sender instanceof EntityPlayer;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		return super.addTabCompletionOptions(sender, args);
	}
}
