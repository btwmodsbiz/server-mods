package btwmod.livemap;

import java.util.List;

import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;

public class CommandMapData extends CommandBaseExtended {
	
	private final mod_MapData mod;

	public CommandMapData(mod_MapData mod) {
		this.mod = mod;
	}

 	@Override
	public String getCommandName() {
		return "mapdata";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0)
			if (mod.enabled)
				sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is saving " + (mod.minimal ? "minimal" : "extended") + " data.");
			else
				sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is disabled.");
		
		else if (args.length == 1 && isStringMatch(args, 0, "disabled")) {
			mod.enabled = false;
			sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is now disabled.");
		}
		
		else if (args.length == 1 && (isStringMatch(args, 0, new String[] { "minimal", "extended" }))) {
			mod.enabled = true;
			mod.minimal = isStringMatch(args, 0, "minimal");
			sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is now saving " + (mod.minimal ? "minimal" : "extended") + " data.");
		}
		
		else if (args.length >= 1 && isStringMatch(args, 0, "playerticks")) {
			if (args.length == 2)
				mod.playerSaveTicks = parseIntWithMin(sender, args[1], 20);
			
			sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is " + (args.length == 2 ? "now " : "") + "saving player data every " + mod.playerSaveTicks + " ticks (+/- " + mod.tickVariance + ").");
		}
		
		else if (args.length >= 1 && isStringMatch(args, 0, "entityticks")) {
			if (args.length == 2)
				mod.entitiesSaveTicks = parseIntWithMin(sender, args[1], 20);
			
			sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is " + (args.length == 2 ? "now " : "") + "saving entity data every " + mod.entitiesSaveTicks + " ticks (+/- " + mod.tickVariance + ").");
		}
		
		else if (args.length >= 1 && isStringMatch(args, 0, "chunkticks")) {
			if (args.length == 2)
				mod.chunksSaveTicks = parseIntWithMin(sender, args[1], 20);
			
			sender.sendChatToPlayer(Util.COLOR_YELLOW + mod.getName() + " is " + (args.length == 2 ? "now " : "") + "saving chunk data every " + mod.chunksSaveTicks + " ticks (+/- " + mod.tickVariance + ").");
		}
		
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " [minimal | extended | disabled | playerticks | entityticks | chunkticks [<ticks>]]";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, new String[] { "minimal", "extended", "disabled", "playerticks", "entityticks", "chunkticks" });
		
		return super.addTabCompletionOptions(sender, args);
	}

}
