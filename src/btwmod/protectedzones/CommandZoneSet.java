package btwmod.protectedzones;

import java.util.List;

import btwmods.util.Area;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;

public class CommandZoneSet extends CommandBase {
	
	private final mod_ProtectedZones mod;
	
	public CommandZoneSet(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "zoneset";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 3) {
			Area<ZoneSettings> area = null;
			if (!ZoneSettings.isValidName(args[0])) {
				sender.sendChatToPlayer("The zone name specified is invalid.");
			}
			else if ((area = mod.get(args[0])) == null) {
				sender.sendChatToPlayer("A zone with that name does not exist.");
			}
			else if (area.data.setSetting(args[1], args[2])) {
				sender.sendChatToPlayer("Setting '" + args[1] + "' successfully set.");
				mod.saveAreas();
			}
			else {
				sender.sendChatToPlayer("Invalid value for setting '" + args[1] + "'.");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <zonename> <setting> <value>";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			List names = mod.getZoneNames();
			return getListOfStringsMatchingLastWord(args, (String[])names.toArray(new String[names.size()]));
		}
		else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, ZoneSettings.settings);
		}
		
		return super.addTabCompletionOptions(sender, args);
	}
}
