package btwmod.protectedzones;

import java.util.List;

import btwmods.Util;
import btwmods.util.Area;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WrongUsageException;

public class CommandZoneInfo extends CommandBase {
	
	private final mod_ProtectedZones mod;
	
	public CommandZoneInfo(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "zoneinfo";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			Area<ZoneSettings> area;
			if (!ZoneSettings.isValidName(args[0])) {
				sender.sendChatToPlayer("The zone name specified is invalid.");
			}
			else if ((area = mod.get(args[0])) == null) {
				sender.sendChatToPlayer("A zone with that name does not exist.");
			}
			else {
				sender.sendChatToPlayer(Util.COLOR_RED + "=== Zone " + area.data.name + " ===");
				
				if (area.data.isCube)
					sender.sendChatToPlayer(Util.COLOR_AQUA + "<Cube>" + Util.COLOR_WHITE + " " + area.data.x1 + "," + area.data.y1 + "," + area.data.z1 + " to " + area.data.x2 + "," + area.data.y2 + "," + area.data.z2 + " in " + Util.getWorldNameFromDimension(area.data.dimension));
				else
					sender.sendChatToPlayer(Util.COLOR_AQUA + "<Area>" + Util.COLOR_WHITE + " " + area.data.x1 + "," + area.data.z1 + " to " + area.data.x2 + "," + area.data.z2 + " in " + Util.getWorldNameFromDimension(area.data.dimension));
				
				String settingsHeader = Util.COLOR_AQUA + "<Settings>" + Util.COLOR_WHITE + " ";
				String playersHeader = Util.COLOR_AQUA + "<Players>" + Util.COLOR_WHITE + " ";
				
				List<String> settingMessages = Util.combineIntoMaxLengthMessages(area.data.settingsAsList(), Packet3Chat.maxChatLength, ", ", true);
				if (settingMessages.size() == 1 && (settingsHeader.length() + settingMessages.get(0).length()) <= Packet3Chat.maxChatLength) {
					sender.sendChatToPlayer(settingsHeader + settingMessages.get(0));
				}
				else {
					sender.sendChatToPlayer(settingsHeader);
					for (String message : settingMessages) {
						sender.sendChatToPlayer(message);
					}
				}
				
				List<String> playerMessages = Util.combineIntoMaxLengthMessages(area.data.playersAsList(), Packet3Chat.maxChatLength, ", ", true);
				if (playerMessages.size() == 1 && (playersHeader.length() + playerMessages.get(0).length()) <= Packet3Chat.maxChatLength) {
					sender.sendChatToPlayer(playersHeader + playerMessages.get(0));
				}
				else if (playerMessages.size() > 1) {
					sender.sendChatToPlayer(playersHeader);
					for (String message : playerMessages) {
						sender.sendChatToPlayer(message);
					}
				}
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <zonename>";
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
