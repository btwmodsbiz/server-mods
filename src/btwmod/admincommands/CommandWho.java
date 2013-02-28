package btwmod.admincommands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import btwmods.Util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WrongUsageException;

public class CommandWho extends CommandBase {
	
	private final mod_AdminCommands mod;
	private final MinecraftServer mcServer;
	
	public CommandWho(mod_AdminCommands mod) {
		this.mod = mod;
		mcServer = MinecraftServer.getServer();
	}

	@Override
	public String getCommandName() {
		return "who";
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (!isFullUsageAllowed(sender)) {
			if (args.length != 0)
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);

			List players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
			List<String> playerStrings = new ArrayList<String>();
			
			Iterator playerIterator = players.iterator();
			while (playerIterator.hasNext()) {
				EntityPlayerMP player = (EntityPlayerMP)playerIterator.next();

				long seconds = mod.getTimeSinceLastPlayerAction(player);
				if (seconds >= mod.getSecondsForAFK())
					playerStrings.add(player.username + " (AFK " + formatSeconds(seconds) + ")");
				else
					playerStrings.add(player.username);
			}
			
			List<String> messages = Util.combineIntoMaxLengthMessages(playerStrings, Packet3Chat.maxChatLength, ", ", true);
			for (String message : messages) {
				sender.sendChatToPlayer(message);
			}
		}
		else if (args.length == 0) {
			List players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
			
			if (players.size() == 0) {
				sender.sendChatToPlayer("No players online.");
			}
			else {
				Iterator playerIterator = players.iterator();
				while (playerIterator.hasNext()) {
					EntityPlayerMP player = (EntityPlayerMP)playerIterator.next();
					sender.sendChatToPlayer(getPlayerResult(player));
				}
			}
		} else {
			for (int i = 0; i < args.length; i++) {
				sender.sendChatToPlayer(getPlayerResult(args[i]));
			}
		}
	}

	private String getPlayerResult(String username) {
		EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().getPlayerEntity(username);
		if (player == null)
			return "Player not found: " + username;
		else
			return getPlayerResult(player);
	}

	private String getPlayerResult(EntityPlayerMP player) {
		long seconds = mod.getTimeSinceLastPlayerAction(player);
		return player.username + " in " + player.worldObj.provider.getDimensionName() + " at " + (long)player.posX + " " + (long)player.posY + " "
				+ (long)player.posZ + (seconds >= mod.getSecondsForAFK() ? " (AFK " + formatSeconds(seconds) + ")" : "");
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + (isFullUsageAllowed(sender) ? " [<playername> ...]" : "");
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (isFullUsageAllowed(sender))
			return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		else
			return super.addTabCompletionOptions(sender, args);
	}
	
	private boolean isFullUsageAllowed(ICommandSender sender) {
		return sender instanceof EntityPlayerMP
				? mcServer.getConfigurationManager().areCommandsAllowed(((EntityPlayerMP)sender).username)
				: true;
	}
	
	private static String formatSeconds(long seconds) {
		if (seconds < 60L) {
			return seconds + " seconds";
		}
		else if (seconds < 3600L) {
			return seconds / 60L + "min";
		}
		else {
			return String.format("%dh%02dm", seconds / 3600L, seconds % 3600L / 60L);
		}
	}
}
