package btwmod.livemap;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import btwmods.Util;

import net.minecraft.src.AnvilChunkLoader;
import net.minecraft.src.CommandBase;
import net.minecraft.src.IChunkLoader;
import net.minecraft.src.ICommandSender;

public class CommandMap extends CommandBase {
	
	private final mod_LiveMap mod;

	public CommandMap(mod_LiveMap mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName();
	}

	@Override
	public String getCommandName() {
		return "livemap";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		int worldIndex = Util.getWorldDimensionFromName("Overworld");
		
		IChunkLoader loader = mod.getChunkLoader(worldIndex);
		if (loader != null && loader instanceof AnvilChunkLoader) {
			
			File location = mod.getAnvilSaveLocation(worldIndex);
			if (location != null) {
				
				File region = new File(location, "region");
				if (region.isDirectory()) {
					
					File[] files = region.listFiles();
					if (files != null) {
						
						int count = 0;
						for (File file : files) {
							if (file.isFile() && file.getName().matches("^r\\.[\\-0-9]+\\.[\\-0-9]+\\.mca$")) {
								String[] split = file.getName().split("\\.");
								mod.queueRegion(worldIndex, location, Integer.parseInt(split[1]), Integer.parseInt(split[2]));
								count++;
							}
						}
						
						sender.sendChatToPlayer("Queued " + count + " region(s) to be processed.");
					}
				}
			}
		}
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public List getCommandAliases() {
		return Arrays.asList(new String[] { "map" });
	}
}
