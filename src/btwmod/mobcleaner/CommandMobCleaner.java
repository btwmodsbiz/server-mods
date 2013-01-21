package btwmod.mobcleaner;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WorldServer;
import net.minecraft.src.WrongUsageException;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;

public class CommandMobCleaner extends CommandBaseExtended {
	
	private final mod_MobCleaner mod;
	
	public CommandMobCleaner(mod_MobCleaner mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "mobcleaner";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (mod.enabled)
				sender.sendChatToPlayer(mod.getName() + " is checking " + mod.entitiesChecked + " entities every " + mod.checkFrequency + " tick(s)" + (mod.debugLogging ? " and is debug logging" : "") + ".");
			else
				sender.sendChatToPlayer(mod.getName() + " is disabled.");
		}
		else if (isStringMatch(args, 0, "debug") && isBoolean(args, 1)) {
			mod.debugLogging = getBoolean(args, 1, sender);
			sender.sendChatToPlayer(mod.getName() + "'s debug logging is now " + (mod.debugLogging ? "on" : "off") + ".");
		}
		else if (isStringMatch(args, 0, "enabled") && isBoolean(args, 1)) {
			mod.enabled = getBoolean(args, 1, sender);
			sender.sendChatToPlayer(mod.getName() + " is now " + (mod.enabled ? "enabled" : "disabled") + ".");
		}
		else if (isStringMatch(args, 0, "frequency") && isInt(args, 1)) {
			mod.checkFrequency = parseIntWithMin(sender, args[1], 1);
			sender.sendChatToPlayer(mod.getName() + " now checks " + mod.entitiesChecked + " entities every " + mod.checkFrequency + " tick(s).");
		}
		else if (isStringMatch(args, 0, "num") && isInt(args, 1)) {
			mod.entitiesChecked = parseIntBounded(sender, args[1], 1, mod.MAX_ENTITIES_CHECKED);
			sender.sendChatToPlayer(mod.getName() + " now checks " + mod.entitiesChecked + " entities every " + mod.checkFrequency + " tick(s).");
		}
		else if (isStringMatch(args, 0, "check")) {
			if (args.length == 1) {
				for (WorldServer world : MinecraftServer.getServer().worldServers) {
					sender.sendChatToPlayer(mod.getName() + " removed " + mod.checkEntities(world) + " entities from " + Util.getWorldNameFromDimension(world.provider.dimensionId) + ".");
				}
			}
			else if (isWorldName(args, 1)) {
				int index = Util.getWorldIndexFromName(args[1]);
				sender.sendChatToPlayer(mod.getName() + " removed " + mod.checkEntities(MinecraftServer.getServer().worldServers[index]) + " entities from " + Util.getWorldNameFromIndex(index) + ".");
			}
			else {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

}
