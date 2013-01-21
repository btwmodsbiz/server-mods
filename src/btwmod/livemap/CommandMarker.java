package btwmod.livemap;

import java.util.List;

import net.minecraft.src.CommandNotFoundException;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.MathHelper;
import net.minecraft.src.WrongUsageException;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;

public class CommandMarker extends CommandBaseExtended {
	
	private final mod_MapMarkers mod;
	
	public CommandMarker(mod_MapMarkers mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "marker";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (!(sender instanceof EntityPlayer)) {
			throw new CommandNotFoundException("/" + getCommandName() + " can only be used by players.");
		}
		else if (isStringMatch(args, 0, "help")) {
			processCommandHelp((EntityPlayer)sender, args);
		}
		else if (isStringMatch(args, 0, "set")) {
			processCommandSet((EntityPlayer)sender, args);
		}
		else if (isStringMatch(args, 0, "remove")) {
			processCommandRemove((EntityPlayer)sender, args);
		}
		else if (isStringMatch(args, 0, "list")) {
			processCommandList((EntityPlayer)sender);
		}
		else if (isStringMatch(args, 0, "description")) {
			processCommandDescription((EntityPlayer)sender,args);
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}
	
	private void processCommandHelp(EntityPlayer player, String[] args) {
		player.sendChatToPlayer(getCommandUsage(args.length >= 2 ? args[1] : null));
	}
	
	private void processCommandSet(EntityPlayer player, String[] args) {
		if (isInt(args, 1)) {
			
			int markerIndex = parseIntWithMin(player, args[1], 1) - 1;
			String username = player.username;
			boolean fullUsage = this.isFullUsageAllowed(player);
			long remainingCooldown;
			
			if (!fullUsage && markerIndex + 1 > mod.maxMarkersPerDimension) {
				player.sendChatToPlayer(Util.COLOR_RED + "You cannot set more than " + mod.maxMarkersPerDimension + " markers per dimension.");
			}
			
			else if (!fullUsage && (remainingCooldown = mod.getMarkerCooldownRemaining(username)) > 0L) {
				player.sendChatToPlayer(Util.COLOR_RED + "Markers can only be set every " + Util.formatSeconds(mod.markerCooldownMinutes * 60) + ". You have " + Util.formatSeconds(remainingCooldown) + " remaining.");
			}
			
			else {
				int dimension = player.dimension;
				int x = MathHelper.floor_double(player.posX);
				int y = MathHelper.floor_double(player.posY);
				int z = MathHelper.floor_double(player.posZ);
				Marker.TYPE type = Marker.TYPE.POINT;
				
				if (args.length >= 3) {
					try {
						type = Marker.TYPE.valueOf(args[2].toUpperCase());
					}
					catch (IllegalArgumentException e) {
						throw new WrongUsageException(getCommandUsage("set"), new Object[0]);
					}
				}
				
				mod.setMarker(new Marker(username, markerIndex, type, dimension, x, y ,z));
				mod.saveMarkers();
				
				player.sendChatToPlayer(Util.COLOR_YELLOW + "Set "
						+ Util.getWorldNameFromDimension(dimension)
						+ " marker #" + (markerIndex + 1)
						+ (type == Marker.TYPE.POINT ? "" : " as " + type.asHumanReadable())
						+ " to " + x + ", " + y + ", " + z
						+ "."
					);
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("set"), new Object[0]);
		}
	}
	
	private void processCommandRemove(EntityPlayer player, String[] args) {
		if (isWorldName(args, 1) && isInt(args, 2)) {

			int dimension = Util.getWorldDimensionFromName(args[1]);
			int markerIndex = parseIntWithMin(player, args[2], 1) - 1;
			
			if (mod.removeMarker(player.username.toString(), dimension, markerIndex)) {
				mod.saveMarkers();
				player.sendChatToPlayer(Util.COLOR_YELLOW + "Removed " + Util.getWorldNameFromDimension(dimension) + " marker #" + (markerIndex + 1) + ".");
			}
			else {
				player.sendChatToPlayer(Util.COLOR_RED + "Marker #" + args[1] + " for " + Util.getWorldNameFromDimension(dimension) + " does not exist.");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("remove"), new Object[0]);
		}
	}
	
	private void processCommandList(EntityPlayer player) {
		Marker[] markers = mod.getMarkers(player.username);
		if (markers.length == 0) {
			player.sendChatToPlayer(Util.COLOR_YELLOW + "You have no markers set.");
		}
		else {
			for (Marker marker : markers) {
				player.sendChatToPlayer(Util.COLOR_YELLOW + "Marker"
					+ " #" + (marker.markerIndex + 1)
					+ " for " + Util.getWorldNameFromDimension(marker.dimension)
					+ " at " + marker.x + (marker.y >= 0 ? (", " + marker.y) : "") + ", " + marker.z
					+ (marker.type == Marker.TYPE.POINT ? "" : " as " + marker.type.asHumanReadable())
					+ (marker.getDescription() == null ? "" : " with description"));
			}
		}
	}
	
	private void processCommandDescription(EntityPlayer player, String[] args) {
		if (isWorldName(args, 1) && isInt(args, 2)) {
			
			int dimension = Util.getWorldDimensionFromName(args[1]);
			int markerIndex = parseIntWithMin(player, args[2], 1) - 1;
			
			Marker marker = mod.getMarker(player.username, dimension, markerIndex);
			
			if (marker == null) {
				player.sendChatToPlayer(Util.COLOR_RED + "Marker #" + (markerIndex + 1) + " for " + Util.getWorldNameFromDimension(dimension) + " does not exist.");
			}
			else {
				if (args.length == 3) {
					if (marker.getDescription() == null) {
						player.sendChatToPlayer(Util.COLOR_YELLOW + "Marker #" + (markerIndex + 1) + " for " + Util.getWorldNameFromDimension(dimension) + " does not have a description set.");
					}
					else {
						player.sendChatToPlayer(Util.COLOR_YELLOW + "Description for " + Util.getWorldNameFromDimension(dimension) + " marker #" + (markerIndex + 1) + ":");
						player.sendChatToPlayer(Util.COLOR_YELLOW + marker.getDescription());
					}
				}
				else if (isStringMatch(args, 3, "-")) {
					if (marker.getDescription() == null) {
						player.sendChatToPlayer(Util.COLOR_RED + "Marker #" + (markerIndex + 1) + " for " + Util.getWorldNameFromDimension(dimension) + " does not have a description.");
					}
					else {
						marker.setDescription(null);
						mod.saveMarkers();
						player.sendChatToPlayer(Util.COLOR_YELLOW + "Removed description from " + Util.getWorldNameFromDimension(dimension) + " marker #" + (markerIndex + 1) + ".");
					}
				}
				else {
					StringBuilder sb = new StringBuilder();
					for (int i = 3; i < args.length; i++) {
						if (args[i].trim().length() > 0) {
							if (sb.length() > 0)
								sb.append(" ");
							
							sb.append(args[i].trim());
						}
					}
					
					marker.setDescription(sb.toString());
					mod.saveMarkers();
					
					player.sendChatToPlayer(Util.COLOR_YELLOW + "Changed description for " + Util.getWorldNameFromDimension(dimension) + " marker #" + (markerIndex + 1) + " to:");
					player.sendChatToPlayer(Util.COLOR_YELLOW + (marker.getDescription() == null ? "null" : marker.getDescription()));
				}
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage("description"), new Object[0]);
		}
	}
	
	public String getCommandUsage(String subCommand) {
		if (subCommand != null) {
			if (subCommand.equalsIgnoreCase("set"))
				return "/" + getCommandName() + " set <num> [home]";
			
			else if (subCommand.equalsIgnoreCase("remove"))
				return "/" + getCommandName() + " remove <world> <num>";
			
			else if (subCommand.equalsIgnoreCase("description"))
				return "/" + getCommandName() + " description <world> <num> [- | <description>]";
			
			else if (subCommand.equalsIgnoreCase("list"))
				return "/" + getCommandName() + " list";
		}
		
		return "/" + getCommandName() + " [help] ( set | remove | description | list ) ...";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return getCommandUsage("");
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, new String[] { "set", "remove", "description", "list", "help" });
		
		else if (isStringMatch(args, 0, "help") && args.length == 2) {
			return getListOfStringsMatchingLastWord(args, new String[] { "set", "remove", "description", "list" });
		}
		
		else if (isStringMatch(args, 0, "set") && args.length == 3) {
			return getListOfStringsMatchingLastWord(args, Marker.TYPE.tabCompletion);
		}
		
		else if ((isStringMatch(args, 0, "remove") || isStringMatch(args, 0, "description")) && args.length == 2) {
			return getListOfStringsMatchingLastWord(args, new String[] { "Overworld", "Nether", "TheEnd" });
		}
		
		return super.addTabCompletionOptions(sender, args);
	}

}
