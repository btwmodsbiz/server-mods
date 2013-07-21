package btwmod.centralchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WrongUsageException;
import btwmod.centralchat.message.MessageChatColor;
import btwmods.Util;
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
		else if (isStringMatch(args, 0, "show")) {
			ChatColors[] colors = ChatColors.values();
			List<String> colorNames = new ArrayList<String>();
			for (ChatColors color : colors) {
				colorNames.add(color.toString().toLowerCase());
			}
			Collections.sort(colorNames);
			
			// Add the color codes to the names.
			for (int i = 0; i < colorNames.size(); i++) {
				colorNames.set(i, ChatColors.getChar(colorNames.get(i)) + colorNames.get(i) + Util.COLOR_WHITE);
			}
			
			int messages = Util.combineIntoMaxLengthMessages(colorNames, Packet3Chat.maxChatLength, ", ", true).size();
			int perMessage = colorNames.size() / messages;
			
			for (int i = 0; i < messages; i++) {
				StringBuilder sb = new StringBuilder();
				for (int k = i; k < perMessage; k++) {
					if (sb.length() > 0) sb.append(", ");
					sb.append(colorNames.get((i * perMessage) + k));
				}
				
				if (i < messages - 1)
					sb.append(", ");
				
				sender.sendChatToPlayer(sb.toString());
			}
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
