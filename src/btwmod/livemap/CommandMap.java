package btwmod.livemap;

import java.util.Arrays;
import java.util.List;

import btwmods.Util;
import btwmods.commands.CommandBaseExtended;
import btwmods.io.Settings;

import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;

public class CommandMap extends CommandBaseExtended {
	
	private final mod_LiveMap mod;

	public CommandMap(mod_LiveMap mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " (region | world | remaining | debug) ...";
	}

	@Override
	public String getCommandName() {
		return "livemap";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (isStringMatch(args, 0, "debug")) {
			if (args.length == 1) {
				sender.sendChatToPlayer(mod.getName() + "'s debug messages are " + (mod.debugMessages ? "on" : "off") + ".");
			}
			else if (isBoolean(args, 1)) {
				mod.debugMessages = Settings.getBooleanValue(args[1], mod.debugMessages);
				sender.sendChatToPlayer(mod.getName() + "'s debug messages are now " + (mod.debugMessages ? "on" : "off") + ".");
			}
			else {
				throw new WrongUsageException("/" + getCommandName() + " debug (on | off)", new Object[0]);
			}
		}
		else if (isStringMatch(args, 0, "remaining")) {
			sender.sendChatToPlayer(mod.getName() + " has " + mod.getRegionLoader().getQueueRemaining() + " chunks in the queue.");
		}
		else if (isStringMatch(args, 0, "region")) {
			if (isStringMatch(args, 1, "dequeuelimit") && isInt(args, 2)) {
				sender.sendChatToPlayer(args[1].toLowerCase() + " changed from " + mod.getRegionLoader().regionChunksDequeLimit + " to " + (mod.getRegionLoader().regionChunksDequeLimit = parseIntBounded(sender, args[2], 1, 500)) + ".");
			}
			else if (isStringMatch(args, 1, "dequeueticks") && isInt(args, 2)) {
				sender.sendChatToPlayer(args[1].toLowerCase() + " changed from " + mod.getRegionLoader().regionChunksDequeTicks + " to " + (mod.getRegionLoader().regionChunksDequeTicks = parseIntWithMin(sender, args[2], 1)) + ".");
			}
			else if (isStringMatch(args, 1, "loadedchunkslimit") && isInt(args, 2)) {
				sender.sendChatToPlayer(args[1].toLowerCase() + " changed from " + mod.getRegionLoader().regionChunkQueueThreshold + " to " + + (mod.getRegionLoader().regionChunkQueueThreshold = parseIntBounded(sender, args[2], 1, 5000)) + ".");
			}
			else if (isInt(args, 2) && isInt(args, 3)) {
				int worldIndex;
				
				try {
					worldIndex = Util.getWorldIndexFromName(args[1]);
				}
				catch (IllegalArgumentException e) {
					throw new WrongUsageException("/" + getCommandName() + " region <world> <x> <z>", new Object[0]);
				}

				int x = parseInt(sender, args[2]);
				int z = parseInt(sender, args[3]);
				if (mod.getRegionLoader().queueRegion(worldIndex, x, z, sender))
					sender.sendChatToPlayer("Queued region " + x + "." + z + " (" + (32*32) + " chunks) for " + Util.getWorldNameFromIndex(worldIndex) + " for mapping.");
			}
			else {
				throw new WrongUsageException("/" + getCommandName() + " region (dequeuelimit | dequeueticks | loadedchunkslimit) (show | <num>)", new Object[0]);
			}
		}
		else if (isStringMatch(args, 0, "world") && isWorldName(args, 1)) {
			int worldIndex;
			
			try {
				worldIndex = Util.getWorldIndexFromName(args[1]);
			}
			catch (IllegalArgumentException e) {
				throw new WrongUsageException("/" + getCommandName() + " world <name>", new Object[0]);
			}

			int count = mod.getRegionLoader().queueWorld(worldIndex, sender);
			if (count > 0) {
				sender.sendChatToPlayer("Queued " + count + " region" + (count > 1 ? "s" : "") + " (" + (count*32*32) + " chunks) from " + Util.getWorldNameFromIndex(worldIndex) + " for mapping.");
			}
			else if (count == 0) {
				sender.sendChatToPlayer("No regions were found for " + Util.getWorldNameFromIndex(worldIndex) + ".");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public List getCommandAliases() {
		return Arrays.asList(new String[] { "map" });
	}
}