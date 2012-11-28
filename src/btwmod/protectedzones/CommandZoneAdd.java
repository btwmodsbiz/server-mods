package btwmod.protectedzones;

import java.util.List;

import btwmods.Util;
import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;

public class CommandZoneAdd extends CommandBase {
	
	private final mod_ProtectedZones mod;
	
	public CommandZoneAdd(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "zoneadd";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if ((args.length == 6 || args.length == 8)) {
			ZoneSettings zone;
			
			if (!ZoneSettings.isValidName(args[0])) {
				sender.sendChatToPlayer("The zone name specified is invalid.");
			}
			else if (mod.get(args[0]) != null) {
				sender.sendChatToPlayer("A zone with that name already exists.");
			}
			else {
				
				int dimension;
				try {
					dimension = Util.getWorldDimensionFromName(args[1]);
				}
				catch (IllegalArgumentException e) {
					throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
				}
				
				if (args.length == 6)
					zone = new ZoneSettings(args[0], dimension, parseInt(sender, args[2]), parseInt(sender, args[3]), parseInt(sender, args[4]), parseInt(sender, args[5]));
				else
					zone = new ZoneSettings(args[0], dimension, parseInt(sender, args[2]), parseInt(sender, args[3]), parseInt(sender, args[4]), parseInt(sender, args[5]), parseInt(sender, args[6]), parseInt(sender, args[7]));
				
				if (!zone.isValid()) {
					sender.sendChatToPlayer("Invalid zone parameters.");
				}
				else if (mod.add(zone)) {
					sender.sendChatToPlayer("Successfully added new zone: " + args[0]);
				}
				else {
					sender.sendChatToPlayer("Failed to add new zone.");
				}
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 2)
			return super.getListOfStringsMatchingLastWord(args, new String[] { "Overworld", "Nether", "TheEnd" });
		
		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <zonename> <dimension> <x1> [<y1>] <z1> <x2> [<y2>] <z2>";
	}
}
