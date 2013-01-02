package btwmod.chat;

import java.io.IOException;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.ModLoader;
import btwmods.commands.CommandBaseExtended;

public class CommandChatColor extends CommandBaseExtended {

	private final mod_Chat mod;
	private final String colorList;

	public CommandChatColor(mod_Chat mod) {
		this.mod = mod;
		StringBuilder colorList = new StringBuilder();
		for (String color : mod.getColors()) {
			if (colorList.length() > 0)
				colorList.append("|");
			
			colorList.append(color);
		}
		this.colorList = colorList.toString();
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
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0 && sender instanceof EntityPlayer) {
			String color = mod.getPlayerColor(sender.getCommandSenderName());
			if (color == null)
				sender.sendChatToPlayer("You do not have a color set for your username in global chat.");
			else
				sender.sendChatToPlayer("Your username color in global chat is " + color + ".");
		}
		else if (((args.length == 1 && sender instanceof EntityPlayer) || (args.length == 2 && isFullUsageAllowed(sender))) && mod.isValidColor(args[0])) {
			String username = args.length == 2 ? args[1] : sender.getCommandSenderName();
			
			try {
				if (!mod.setPlayerColor(username, args[0])) {
					throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
				}
			} catch (IOException e) {
				ModLoader.outputError(e, CommandChatColor.class.getSimpleName() + " failed to save settings while trying to change a global color: " + e.getMessage());
			}
			
			String newColor = mod.getPlayerColor(username);
			sender.sendChatToPlayer((args.length == 1 ? "Your" : username + "'s") + " username color in global chat is now " + (newColor == null ? "white" : newColor) + ".");
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}
	
	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " (" + colorList + ")" + (isFullUsageAllowed(sender) ? (sender instanceof EntityPlayer ? " [username]" : " <username>") : "");
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, mod.getColors());
		
		else if (args.length == 2 && isFullUsageAllowed(sender))
			return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		
		return super.addTabCompletionOptions(sender, args);
	}
}
