package btwmod.chunkcleaner;

import java.util.Arrays;
import java.util.List;

import btwmods.Util;
import btwmods.io.Settings;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.World;
import net.minecraft.src.WorldServer;
import net.minecraft.src.WrongUsageException;

public class CommandChunk extends CommandBase {
	
	private final mod_ChunkCleaner mod;

	public CommandChunk(mod_ChunkCleaner mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "chunk";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (mod.enabled)
				sender.sendChatToPlayer("Chunk cleaning is checking " + mod.chunksChecked + " chunk(s) every " + mod.checkFrequency + " tick(s)" + (mod.debugLogging ? " and is debug logging" : "") + ".");
			else
				sender.sendChatToPlayer("Chunk cleaning is off.");
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("debug") && Settings.isBooleanValue(args[1])) {
			mod.debugLogging = Settings.getBooleanValue(args[1], mod.debugLogging);
			sender.sendChatToPlayer("Chunk cleaning debug logging is now " + (mod.debugLogging ? "on" : "off") + ".");
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("enabled") && Settings.isBooleanValue(args[1])) {
			mod.enabled = Settings.getBooleanValue(args[1], mod.enabled);
			sender.sendChatToPlayer("Chunk cleaning is now " + (mod.enabled ? "on" : "off") + ".");
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("frequency")) {
			mod.checkFrequency = parseIntWithMin(sender, args[1], 1);
			sender.sendChatToPlayer("Chunk cleaning now checks " + mod.chunksChecked + " chunk(s) every " + mod.checkFrequency + " tick(s).");
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("num")) {
			mod.chunksChecked = parseIntBounded(sender, args[1], 1, mod_ChunkCleaner.MAX_CHUNKS_CHECKED);
			sender.sendChatToPlayer("Chunk cleaning now checks " + mod.chunksChecked + " chunk(s) every " + mod.checkFrequency + " tick(s).");
		}
		else if (args.length == 4 && args[0].equalsIgnoreCase("check")) {
			int chunkX = parseInt(sender, args[2]) >> 4;
			int chunkZ = parseInt(sender, args[3]) >> 4;
			
			int dimension;
			try {
				dimension = Util.getWorldDimensionFromName(args[1]);
			}
			catch (IllegalArgumentException e) {
				throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
			}
			
			World world = MinecraftServer.getServer().worldServerForDimension(dimension);
			EntityPlayer[] playersWatching = ((WorldServer)world).getPlayerManager().getPlayersWatchingChunk(chunkX, chunkZ);
			
			if (playersWatching == null && world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
				sender.sendChatToPlayer("The chunk is not loaded.");
			}
			else {
				StringBuilder playersWatchingList = new StringBuilder();
				if (playersWatching != null) {
					for (EntityPlayer player : playersWatching) {
						if (playersWatchingList.length() > 0) playersWatchingList.append(", ");
						playersWatchingList.append(player.username);
					}
				}
				
				List<EntityPlayer> players = mod.getPlayersWithinRange(world, chunkX, chunkZ);

				StringBuilder playersList = new StringBuilder();
				for (EntityPlayer player : players) {
					if (playersList.length() > 0) playersList.append(", ");
					playersList.append(player.username);
				}
				
				if (players.isEmpty()) {
					if (playersWatching == null || playersWatching.length == 0)
						sender.sendChatToPlayer("Chunk " + chunkX + "," + chunkZ + " is stale.");
					else {
						sender.sendChatToPlayer("Chunk " + chunkX + "," + chunkZ + " is out of range of players, but thinks the following are watching it:");
						sender.sendChatToPlayer(playersWatchingList.toString());
					}
				}
				else {
					if (playersWatching == null || playersWatching.length == 0) {
						Util.sendInMinimumMessages(sender, Arrays.asList(new String[] { "Chunk not stale because the following are in range:", playersList.toString() }), " ");
					}
					else {
						Util.sendInMinimumMessages(sender, Arrays.asList(new String[] { "Chunk not stale because the following are in range:", playersList.toString() }), " ");
						Util.sendInMinimumMessages(sender, Arrays.asList(new String[] {"...and also the following are watching it:", playersWatchingList.toString() }), " ");
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
		return "/" + getCommandName() + " <enabled|debug|frequency|num|check> ...";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			return super.getListOfStringsMatchingLastWord(args, new String[] { "enabled", "debug", "frequency", "num", "check" });
		}
		else if (args.length == 2 && (args[0].equalsIgnoreCase("enabled") || args[0].equalsIgnoreCase("debug"))) {
			return super.getListOfStringsMatchingLastWord(args, new String[] { "on", "off" });
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("frequency")) {
			return super.getListOfStringsMatchingLastWord(args, new String[] { Integer.toString(mod.checkFrequency) });
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("num")) {
			return super.getListOfStringsMatchingLastWord(args, new String[] { Integer.toString(mod.chunksChecked) });
		}
		
		return super.addTabCompletionOptions(sender, args);
	}

}
