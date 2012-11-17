package btwmod.protectedzones;

import java.util.List;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;

public class CommandZoneDelete extends CommandBase {
	
	private final mod_ProtectedZones mod;
	
	public CommandZoneDelete(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "zonedel";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			if (!ZoneSettings.isValidName(args[0])) {
				sender.sendChatToPlayer("The zone name specified is invalid.");
			}
			else if (mod.get(args[0]) == null) {
				sender.sendChatToPlayer("A zone with that name does not exist.");
			}
			else if (mod.remove(args[0])) {
				sender.sendChatToPlayer("Successfully removed zone: " + args[0]);
			}
			else {
				sender.sendChatToPlayer("Failed to remove zone.");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <name>";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			List names = mod.getZoneNames();
			return getListOfStringsMatchingLastWord(args, (String[])names.toArray(new String[names.size()]));
		}
		
		return super.addTabCompletionOptions(sender, args);
	}
}
