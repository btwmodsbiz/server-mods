package btwmod.admincommands;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.AnvilChunkLoader;
import net.minecraft.src.ChunkProviderServer;
import net.minecraft.src.IChunkLoader;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WorldServer;
import btwmods.CommandsAPI;
import btwmods.Util;
import btwmods.commands.CommandBaseExtended;

public class CommandHasPendingIO extends CommandBaseExtended {

	@Override
	public String getCommandName() {
		return "haspendingio";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		
		boolean hasIO = false;
		
		MinecraftServer server = MinecraftServer.getServer();
		for (WorldServer world : server.worldServers) {
			IChunkProvider chunkProvider = world.getChunkProvider();
			if (chunkProvider instanceof ChunkProviderServer) {
				IChunkLoader chunkLoader = ((ChunkProviderServer)chunkProvider).getChunkLoader();
				if (chunkLoader instanceof AnvilChunkLoader && (((AnvilChunkLoader)chunkLoader).hasPendingIO())) {
					hasIO = true;
					break;
				}
			}
		}
		
		if (!CommandsAPI.onDoQuietCommand(this, sender, args, hasIO)) {
			sender.sendChatToPlayer(Util.COLOR_YELLOW + "AnvilChunkLoader " + (hasIO ? "has" : "does not have") + " pending IO.");
		}
	}

}
