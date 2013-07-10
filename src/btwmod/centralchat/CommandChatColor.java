package btwmod.centralchat;

import java.util.Arrays;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.commands.CommandBaseExtended;

public class CommandChatColor extends CommandBaseExtended {

	private final mod_CentralChat mod;
	private final String[] colorNames;

	public CommandChatColor(mod_CentralChat mod) {
		this.mod = mod;
		
		ChatColors[] colors = ChatColors.values();
		colorNames = new String[colors.length];
		for (int i = 0; i < colors.length; i++) {
			colorNames[i] = colors[i].name().toLowerCase();
		}
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1iCommandSender) {
		return true;
	}

	@Override
	public String getCommandName() {
		return "chatcolor";
	}

	@Override
	public List getCommandAliases() {
		return Arrays.asList(new String[] { "chatcolour" });
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0 && sender instanceof EntityPlayer) {
			mod.queueMessage(new MessageChatColor("get", sender.getCommandSenderName()));
		}
		else if (((args.length == 1 && sender instanceof EntityPlayer) || (args.length == 2 && isFullUsageAllowed(sender))) && ChatColors.isValid(args[0])) {
			String username = args.length == 2 ? args[1] : sender.getCommandSenderName();
			mod.queueMessage(new MessageChatColor("set", username, args[0]));
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}
	
	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " (show | <color>" + (isFullUsageAllowed(sender) ? (sender instanceof EntityPlayer ? " [username]" : " <username>") : "") + " )";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, colorNames);
		
		else if (args.length == 2 && isFullUsageAllowed(sender))
			return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		
		return super.addTabCompletionOptions(sender, args);
	}
}
