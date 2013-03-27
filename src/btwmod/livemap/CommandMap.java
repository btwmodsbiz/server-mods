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
		return "/" + getCommandName() + " (region | world | remaining | clear | debug) ...";
	}

	@Override
	public String getCommandName() {
		return "livemap";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (isStringMatch(args, 0, "debug")) {
			if (args.length == 1) {
				sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + "'s debug messages are " + (mod.debugMessages ? "on" : "off") + ".");
			}
			else if (isBoolean(args, 1)) {
				mod.debugMessages = Settings.getBooleanValue(args[1], mod.debugMessages);
				sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + "'s debug messages are now " + (mod.debugMessages ? "on" : "off") + ".");
			}
			else {
				throw new WrongUsageException("/" + getCommandName() + " debug (on | off)", new Object[0]);
			}
		}
		else if (isStringMatch(args, 0, "remaining")) {
			sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " has " + mod.getRemainingRegionChunks() + " chunks in the queue.");
		}
		else if (isStringMatch(args, 0, "region")) {
			if (isInt(args, 2) && isInt(args, 3)) {
				int worldIndex;
				int range = isInt(args, 4) ? parseInt(sender, args[4]) : 0;
				
				try {
					worldIndex = Util.getWorldIndexFromName(args[1]);
				}
				catch (IllegalArgumentException e) {
					throw new WrongUsageException("/" + getCommandName() + " region <world> <x> <z> [<radius>]", new Object[0]);
				}

				int x = parseInt(sender, args[2]);
				int z = parseInt(sender, args[3]);
				
				int regions = 0;
				for (int iX = x - range; iX <= x + range; iX++) {
					for (int iZ = z - range; iZ <= z + range; iZ++) {
						try {
							mod.queueRegion(worldIndex, iX, iZ);
							regions++;
						} catch (Exception e) {
							sender.sendChatToPlayer(Util.COLOR_RED + e.getMessage());
						}
					}
				}

				if (regions == 1)
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Queued region " + x + "." + z + " (" + (32*32) + " chunks) for " + Util.getWorldNameFromIndex(worldIndex) + " for mapping.");
				else if (regions > 1)
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Queued " + regions + " regions (" + (regions*32*32) + " chunks) centered on " + x + "." + z + " for " + Util.getWorldNameFromIndex(worldIndex) + " for mapping.");
			}
			else {
				throw new WrongUsageException("/" + getCommandName() + " region <world> <x> <z> [<radius>]", new Object[0]);
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

			try {
				int count = mod.queueWorld(worldIndex);
				if (count > 0) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Queued " + count + " region" + (count > 1 ? "s" : "") + " (" + (count*32*32) + " chunks) from " + Util.getWorldNameFromIndex(worldIndex) + " for mapping.");
				}
				else if (count == 0) {
					sender.sendChatToPlayer(Util.COLOR_RED + "No regions were found for " + Util.getWorldNameFromIndex(worldIndex) + ".");
				}
			} catch (Exception e) {
				sender.sendChatToPlayer(e.getMessage());
			}
		}
		else if (isStringMatch(args, 0, "clear")) {
			mod.clearQueue();
			sender.sendChatToPlayer(Util.COLOR_YELLOW + "Cleared queued regions and unloaded chunks.");
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, new String[] { "debug", "region", "remaining", "world", "clear" });
		}
		else if (args.length == 2 && isStringMatch(args, 0, new String[] { "region", "world" })) {
			return getListOfStringsMatchingLastWord(args, new String[] { "Overworld", "Nether", "TheEnd" });
		}
		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public List getCommandAliases() {
		return Arrays.asList(new String[] { "map" });
	}
}
