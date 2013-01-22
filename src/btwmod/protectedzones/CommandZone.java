package btwmod.protectedzones;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WrongUsageException;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;
import btwmods.util.Area;
import btwmods.util.Cube;

public class CommandZone extends CommandBaseExtended {
	
	private final mod_ProtectedZones mod;
	
	public CommandZone(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "zone";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (isStringMatch(args, 0, "create")) {
			processCommandCreate(sender, args);
		}
		else if (isStringMatch(args, 0, "destroy")) {
			processCommandDestroy(sender, args);
		}
		else if (isStringMatch(args, 0, "set")) {
			processCommandSet(sender, args);
		}
		else if (isStringMatch(args, 0, "add")) {
			processCommandAdd(sender, args);
		}
		else if (isStringMatch(args, 0, "remove")) {
			processCommandRemove(sender, args);
		}
		else if (isStringMatch(args, 0, "info")) {
			processCommandInfo(sender, args);
		}
		else if (isStringMatch(args, 0, "list")) {
			processCommandList(sender, args);
		}
		else if (isStringMatch(args, 0, "grant")) {
			processCommandGrant(sender, args);
		}
		else if (isStringMatch(args, 0, "revoke")) {
			processCommandGrant(sender, args);
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}
	
	public void processCommandCreate(ICommandSender sender, String[] args) {
		if (args.length == 3 && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);

			ZoneSettings zone = null;
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if (mod.get(dimension, args[2]) != null) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name already exists.");
			}
			else {
				try {
					zone = new ZoneSettings(args[2], dimension);
				}
				catch (IllegalArgumentException e) {
					
				}
				
				if (zone == null) {
					sender.sendChatToPlayer(Util.COLOR_RED + "Invalid zone parameters.");
				}
				else if (mod.add(zone)) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Successfully created new zone: " + Util.COLOR_WHITE + args[2]);
				}
				else {
					sender.sendChatToPlayer(Util.COLOR_RED + "Failed to add new zone.");
				}
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("create"), new Object[0]);
		}
	}
	
	public void processCommandDestroy(ICommandSender sender, String[] args) {
		if (args.length == 3 && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if (!mod.remove(dimension, args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name does not exist.");
			}
			else {
				sender.sendChatToPlayer(Util.COLOR_YELLOW + "Successfully removed zone: " + Util.COLOR_WHITE + args[2]);
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("destroy"), new Object[0]);
		}
	}
	
	public void processCommandSet(ICommandSender sender, String[] args) {
		if (args.length == 5 && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			
			ZoneSettings settings = null;
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if ((settings = mod.get(dimension, args[2])) == null) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name does not exist.");
			}
			else if (settings.setSetting(args[3], args[4])) {
				sender.sendChatToPlayer(Util.COLOR_YELLOW + "Setting '" + args[3] + "' successfully set.");
				mod.saveAreas();
			}
			else {
				sender.sendChatToPlayer(Util.COLOR_RED + "Invalid value for setting '" + args[3] + "'.");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("set"), new Object[0]);
		}
	}
	
	public void processCommandAdd(ICommandSender sender, String[] args) {
		if ((args.length == 7 || args.length == 9) && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			
			ZoneSettings settings = null;
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if ((settings = mod.get(dimension, args[2])) == null) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name does not exist.");
			}
			else if (args.length == 7) {
				int x1 = parseInt(sender, args[3]);
				int z1 = parseInt(sender, args[4]);
				int x2 = parseInt(sender, args[5]);
				int z2 = parseInt(sender, args[6]);
				
				Area area = settings.addArea(x1, z1, x2, z2);
				
				if (area != null) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Added area to " + settings.name + ": " + area.x1 + "," + + area.z1 + " to " + area.x2 + "," + area.z2);
				}
				else {
					sender.sendChatToPlayer(Util.COLOR_RED + "An area with the same dimensions already exists for the zone.");
				}
			}
			else {
				int x1 = parseInt(sender, args[3]);
				int y1 = parseIntBounded(sender, args[4], 0, 256);
				int z1 = parseInt(sender, args[5]);
				int x2 = parseInt(sender, args[6]);
				int y2 = parseIntBounded(sender, args[7], 0, 256);
				int z2 = parseInt(sender, args[8]);
				
				Cube cube = settings.addCube(x1, y1, z1, x2, y2, z2);
				
				if (cube != null) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Added cube to zone " + settings.name + ": " + cube.x1 + "," + cube.y1 + "," + cube.z1 + " to " + cube.x2 + "," + cube.y2 + "," + cube.z2);
				}
				else {
					sender.sendChatToPlayer(Util.COLOR_RED + "A cube with the same dimensions already exists for the zone.");
				}
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("add"), new Object[0]);
		}
	}
	
	public void processCommandRemove(ICommandSender sender, String[] args) {
		if (args.length == 4 && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			
			ZoneSettings settings = null;
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if ((settings = mod.get(dimension, args[2])) == null) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name does not exist.");
			}
			else {
				int areaNum = parseIntWithMin(sender, args[3], 1);
				if (settings.removeArea(areaNum - 1)) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Removed area #" + areaNum + " from zone " + settings.name + ".");
				}
				else {
					sender.sendChatToPlayer(Util.COLOR_RED + "Zone " + settings.name + " does not have an area #" + areaNum + ".");
				}
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("remove"), new Object[0]);
		}
	}
	
	public void processCommandInfo(ICommandSender sender, String[] args) {
		if (args.length == 3 && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			
			ZoneSettings settings;
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if ((settings = mod.get(dimension, args[2])) == null) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name does not exist.");
			}
			else {
				sender.sendChatToPlayer(Util.COLOR_RED + "=== Zone " + settings.name + " in " + Util.getWorldNameFromDimension(settings.dimension) + " ===");
				
				for (int i = 1; i <= settings.areas.size(); i++) {
					Area area = settings.areas.get(i - 1);
					if (area instanceof Cube) {
						Cube cube = (Cube)area;
						sender.sendChatToPlayer(Util.COLOR_AQUA + "<Area #" + i + ">" + Util.COLOR_WHITE + " " + cube.x1 + "," + cube.y1 + "," + cube.z1 + " to " + cube.x2 + "," + cube.y2 + "," + cube.z2);
					}
					else {
						sender.sendChatToPlayer(Util.COLOR_AQUA + "<Area #" + i + ">" + Util.COLOR_WHITE + " " + area.x1 + "," + area.z1 + " to " + area.x2 + "," + area.z2);
					}
				}
				
				String settingsHeader = Util.COLOR_AQUA + "<Settings>" + Util.COLOR_WHITE + " ";
				String playersHeader = Util.COLOR_AQUA + "<Whitelist>" + Util.COLOR_WHITE + " ";
				
				List<String> settingMessages = Util.combineIntoMaxLengthMessages(settings.settingsAsList(), Packet3Chat.maxChatLength, ", ", true);
				if (settingMessages.size() == 1 && (settingsHeader.length() + settingMessages.get(0).length()) <= Packet3Chat.maxChatLength) {
					sender.sendChatToPlayer(settingsHeader + settingMessages.get(0));
				}
				else {
					sender.sendChatToPlayer(settingsHeader);
					for (String message : settingMessages) {
						sender.sendChatToPlayer(message);
					}
				}
				
				List<String> playerMessages = Util.combineIntoMaxLengthMessages(settings.playersAsList(), Packet3Chat.maxChatLength, ", ", true);
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
			throw new WrongUsageException(getCommandUsage("info"), new Object[0]);
		}
	}
	
	public void processCommandList(ICommandSender sender, String[] args) {
		if (args.length > 2 || !isWorldName(args, 1))
			throw new WrongUsageException(getCommandUsage("list"), new Object[0]);
		
		int dimension = Util.getWorldDimensionFromName(args[1]);
		
		List<String> zoneNames = mod.getZoneNames(dimension);
		
		if (zoneNames.size() == 0) {
			sender.sendChatToPlayer(Util.COLOR_YELLOW + "There are no zones for " + Util.getWorldNameFromDimension(dimension) + ".");
		}
		else {
			String headerShort = Util.COLOR_YELLOW + Util.getWorldNameFromDimension(dimension) + " Zones: " + Util.COLOR_WHITE;
			String headerLong = Util.COLOR_YELLOW + Util.getWorldNameFromDimension(dimension) + " Zones (Page XX/YY): " + Util.COLOR_WHITE;
			int page = args.length == 3 ? parseIntWithMin(sender, args[2], 1) : 1;
			int maxListSize = Packet3Chat.maxChatLength - Math.max(headerShort.length(), headerLong.length());
			
			List<String> pages = Util.combineIntoMaxLengthMessages(zoneNames, maxListSize, ", ", false);
			
			page = Math.min(page, pages.size());
			
			sender.sendChatToPlayer((pages.size() == 1 ? headerShort : headerLong.replaceAll("XX/YY", page + "/" + pages.size()))
					+ pages.get(Math.min(page, pages.size()) - 1));
		}
	}
	
	public void processCommandGrant(ICommandSender sender, String[] args) {
		if (args.length == 4 && isWorldName(args, 1)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			
			ZoneSettings settings;
			if (!ZoneSettings.isValidName(args[2])) {
				sender.sendChatToPlayer(Util.COLOR_RED + "The zone name specified is invalid.");
			}
			else if ((settings = mod.get(dimension, args[2])) == null) {
				sender.sendChatToPlayer(Util.COLOR_RED + "A zone with that name does not exist.");
			}
			
			else if (args[0].equalsIgnoreCase("grant")) {
				if (settings.grantPlayer(args[3])) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Whitelisted player " + args[3] + " for zone " + settings.name + ".");
					mod.saveAreas();
				}
				else
					sender.sendChatToPlayer(Util.COLOR_RED + "Player " + args[3] + " is already whitelisted for zone " + settings.name + ".");
			}
			
			else if (args[0].equalsIgnoreCase("revoke")) {
				if (settings.revokePlayer(args[3])) {
					sender.sendChatToPlayer(Util.COLOR_YELLOW + "Removed player " + args[3] + " from zone " + settings.name + "'s whitelist.");
					mod.saveAreas();
				}
				else
					sender.sendChatToPlayer(Util.COLOR_RED + "Player " + args[3] + " was not whitelisted for zone " + settings.name + ".");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(args[0]), new Object[0]);
		}
	}
	
	public String getCommandUsage(String subCommand) {
		if (subCommand != null) {
			if (subCommand.equalsIgnoreCase("create"))
				return "/" + getCommandName() + " create <dimension> <zonename>";
			
			else if (subCommand.equalsIgnoreCase("destroy"))
				return "/" + getCommandName() + " destroy <dimension> <zonename>";
			
			else if (subCommand.equalsIgnoreCase("set"))
				return "/" + getCommandName() + " set <dimension> <zonename> <setting> <value>";
			
			else if (subCommand.equalsIgnoreCase("add"))
				return "/" + getCommandName() + " add <dimension> <zonename> <x1> [<y1>] <z1> <x2> [<y2>] <z2>";
			
			else if (subCommand.equalsIgnoreCase("remove"))
				return "/" + getCommandName() + " remove <dimension> <zonename> <areanum>";
			
			else if (subCommand.equalsIgnoreCase("info"))
				return "/" + getCommandName() + " info <dimension> <zonename>";
			
			else if (subCommand.equalsIgnoreCase("list"))
				return "/" + getCommandName() + " list <dimension> [<page>]";
			
			else if (subCommand.equalsIgnoreCase("grant"))
				return "/" + getCommandName() + " grant <dimension> <zonename> <username>";
			
			else if (subCommand.equalsIgnoreCase("revoke"))
				return "/" + getCommandName() + " revoke <dimension> <zonename> <username>";
		}
		
		return "/" + getCommandName() + " [help] ( create | destroy | set | add | remove | info | list | grant | revoke ) ...";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return getCommandUsage("");
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, new String[] { "help", "create", "destroy", "set", "add", "remove", "info", "list", "grant", "revoke" });
		
		else if (args.length == 2 && isStringMatch(args, 0, "help")) {
			return getListOfStringsMatchingLastWord(args, new String[] { "create", "destroy", "set", "add", "remove", "info", "list", "grant", "revoke" });
		}
		
		// <dimension>
		else if (args.length == 2 && isStringMatch(args, 0, new String[] { "create", "destroy", "set", "add", "remove", "info", "list", "grant", "revoke" })) {
			return getListOfStringsMatchingLastWord(args, new String[] { "Overworld", "Nether", "TheEnd" });
		}
		
		// <zonename>
		else if (args.length == 3 && isWorldName(args, 1) && (isStringMatch(args, 0, new String[] { "destroy", "set", "add", "remove", "info", "grant", "revoke" }))) {
			List names = mod.getZoneNames(Util.getWorldDimensionFromName(args[1]));
			return getListOfStringsMatchingLastWord(args, (String[])names.toArray(new String[names.size()]));
		}
		
		// <setting>
		else if (args.length == 4 && isStringMatch(args, 0, "set")) {
			return getListOfStringsMatchingLastWord(args, ZoneSettings.settings);
		}
		
		// <value>
		else if (args.length == 5 && isStringMatch(args, 0, "set")) {
			return getListOfStringsMatchingLastWord(args, new String[] { "on", "whitelist", "off" });
		}
		
		else if (args.length == 4 && isStringMatch(args, 0, "grant")) {
			return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		}
		
		else if (args.length == 4 && isStringMatch(args, 0, "revoke")) {
			int dimension;
			
			try {
				dimension = Util.getWorldDimensionFromName(args[1]);
			}
			catch (IllegalArgumentException e) {
				return super.addTabCompletionOptions(sender, args);
			}
			
			ZoneSettings settings = mod.get(dimension, args[2]);
			if (settings == null)
				return super.addTabCompletionOptions(sender, args);
			
			return getListOfStringsFromIterableMatchingLastWord(args, settings.playersAsList());
		}
		
		return super.addTabCompletionOptions(sender, args);
	}
}
