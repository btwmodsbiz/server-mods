package btwmod.protectedzones;

import java.util.List;

import btwmods.util.Area;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;

public class CommandZonePlayer extends CommandBase {
	
	private final mod_ProtectedZones mod;
	
	public CommandZonePlayer(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "zoneplayer";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 3 && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke"))) {
			Area<ZoneSettings> area;
			if (!ZoneSettings.isValidName(args[1])) {
				sender.sendChatToPlayer("The zone name specified is invalid.");
			}
			else if ((area = mod.get(args[1])) == null) {
				sender.sendChatToPlayer("A zone with that name does not exist.");
			}
			else if (args[0].equalsIgnoreCase("grant")) {
				if (area.data.grantPlayer(args[2])) {
					sender.sendChatToPlayer("Granted full access to player " + args[2] + " for zone '" + area.data.name + "'.");
					mod.saveAreas();
				}
				else
					sender.sendChatToPlayer("Player " + args[2] + " already has full access to zone '" + area.data.name + "'.");
			}
			else if (args[0].equalsIgnoreCase("revoke")) {
				if (area.data.revokePlayer(args[2])) {
					sender.sendChatToPlayer("Revoked full access to player " + args[2] + " from zone '" + area.data.name + "'.");
					mod.saveAreas();
				}
				else
					sender.sendChatToPlayer("Player " + args[2] + " did not have full access to zone '" + area.data.name + "'.");
			}
			else {
				
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " (grant | revoke) <zonename> <player>";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, new String[] { "grant", "revoke" });
		}
		else if (args.length == 2) {
			List names = mod.getZoneNames();
			return getListOfStringsMatchingLastWord(args, (String[])names.toArray(new String[names.size()]));
		}
		else if (args.length == 3) {
			return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		}
		
		return super.addTabCompletionOptions(sender, args);
	}
}
