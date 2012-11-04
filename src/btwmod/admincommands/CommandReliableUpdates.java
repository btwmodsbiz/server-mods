package btwmod.admincommands;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import net.minecraft.src.mod_FCBetterThanWolves;

public class ReliableUpdatesCommand extends CommandBase {

	@Override
	public String getCommandName() {
		return "reliableupdates";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " [<on|off>]";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendChatToPlayer("Reliable updates are " + (mod_FCBetterThanWolves.fcEnableReliableBlockUpdates ? "on" : "off") + ".");
		}
		else if ((args[0].equalsIgnoreCase("on") && mod_FCBetterThanWolves.fcEnableReliableBlockUpdates)
				|| (args[0].equalsIgnoreCase("off") && !mod_FCBetterThanWolves.fcEnableReliableBlockUpdates)) {
			sender.sendChatToPlayer("Reliable updates are already " + (mod_FCBetterThanWolves.fcEnableReliableBlockUpdates ? "on" : "off") + ".");
		}
		else if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
			mod_FCBetterThanWolves.fcEnableReliableBlockUpdates = args[0].equalsIgnoreCase("on");
			sender.sendChatToPlayer("Reliable updates are now " + (mod_FCBetterThanWolves.fcEnableReliableBlockUpdates ? "on" : "off") + ".");
		}
		else {
			throw new WrongUsageException(this.getCommandUsage(sender), new Object[0]);
		}
	}
}
