package btwmod.centralchat;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.commands.CommandBaseExtended;

public class CommandChatKey extends CommandBaseExtended {
	
	private final IGateway gateway;
	
	public CommandChatKey(IGateway gateway) {
		this.gateway = gateway;
	}

	@Override
	public String getCommandName() {
		return "chatkey";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0 && sender instanceof EntityPlayer) {
			gateway.requestKey(sender.getCommandSenderName(), false);
		}
		else if (isStringMatch(args, 0, "new")) {
			gateway.requestKey(sender.getCommandSenderName(), true);
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

}
