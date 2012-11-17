package btwmod.protectedzones;

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
		if ((args.length == 5 || args.length == 7)) {
			ZoneSettings zone;
			
			if (!ZoneSettings.isValidName(args[0])) {
				sender.sendChatToPlayer("The zone name specified is invalid.");
			}
			else if (mod.get(args[0]) != null) {
				sender.sendChatToPlayer("A zone with that name already exists.");
			}
			else {
				if (args.length == 5)
					zone = new ZoneSettings(args[0], parseInt(sender, args[1]), parseInt(sender, args[2]), parseInt(sender, args[3]), parseInt(sender, args[4]));
				else
					zone = new ZoneSettings(args[0], parseInt(sender, args[1]), parseInt(sender, args[2]), parseInt(sender, args[3]), parseInt(sender, args[4]), parseInt(sender, args[5]), parseInt(sender, args[6]));
				
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
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <name> <x1> [<y1>] <z1> <x2> [<y2>] <z2>";
	}
}
