package btwmod.livemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import net.minecraft.src.Block;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.io.IllegalSettingException;
import btwmods.io.MissingSettingException;
import btwmods.io.Settings;

public class MapUtil {
	public static BlockColor[][] loadColorData(File colorData, IMod mod) {
		if (!colorData.isFile()) {
			if (mod != null)
				ModLoader.outputError(mod.getName() + " could not find color data file at: " + colorData.getPath(), Level.SEVERE);
			return null;
		}
		
		List<List<BlockColor>> blockColorsTemp = new ArrayList<List<BlockColor>>();
		for (int i = 0; i < Block.blocksList.length; i++)
			blockColorsTemp.add(null);
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(colorData));

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() > 0 && line.trim().charAt(0) != '#') {
					BlockColor color;
					try {
						color = BlockColor.fromConfigLine(line);
					} catch (IllegalArgumentException e) {
						if (mod != null)
							ModLoader.outputError(mod.getName() + " found an invalid ('" + e.getMessage() + "') colorData entry: " + line, Level.SEVERE);
						return null;
					} catch (ParseException e) {
						if (mod != null)
							ModLoader.outputError(mod.getName() + " found an invalid ('" + e.getMessage() + "') colorData entry: " + line, Level.SEVERE);
						return null;
					}
	
					if (color == null) {
						if (mod != null)
							ModLoader.outputError(mod.getName() + " found an invalid colorData entry: " + line, Level.SEVERE);
						return null;
	
					} else {
						Set<Block> blocks = BlockColor.getBlocksByName(color.blockName);
	
						if (blocks == null) {
							// TODO: Report color for block that does not exist?
						}
						else {
							for (Block block : blocks) {
								List<BlockColor> list = blockColorsTemp.get(block.blockID);
								if (list == null) {
									blockColorsTemp.set(block.blockID, list = new ArrayList<BlockColor>());
								}
								list.add(color);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			if (mod != null)
				ModLoader.outputError(e, mod.getName() + " failed to read the colorData file: " + e.getMessage(), Level.SEVERE);
			return null;
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {

			}
		}
		
		BlockColor[][] blockColors = new BlockColor[Block.blocksList.length][];
		
		for (int i = 0; i < blockColors.length; i++) {
			List<BlockColor> list = blockColorsTemp.get(i);
			blockColors[i] = list == null || list.size() == 0 ? null : list.toArray(new BlockColor[list.size()]);
		}

		return blockColors;
	}
	
	public static void setColorDataDefaults(BlockColor[][] blockColors) {
		for (int i = 0, l = blockColors.length; i < l; i++) {
			if (Block.blocksList[i] != null && Block.blocksList[i].getBlockName() != null) {
				
				boolean genericFound = false;
				
				if (blockColors[i] != null) {
					for (BlockColor color : blockColors[i]) {
						if (!color.hasFilter())
							genericFound = true;
					}
				}
				
				if (!genericFound) {
					List<BlockColor> list = new ArrayList<BlockColor>();
					if (blockColors[i] != null)
						list.addAll(Arrays.asList(blockColors[i]));
					
					BlockColor color = BlockColor.fromBlock(Block.blocksList[i]);
					
					// Only add blocks that have a proper color.
					if (color.red != 0 || color.green != 0 || color.blue != 0)
						list.add(BlockColor.fromBlock(Block.blocksList[i]));
					
					if (list.size() > 0)
						blockColors[i] = list.toArray(new BlockColor[list.size()]);
				}
			}
		}
	}
	
	public static BlockColor[][] extendColorData(BlockColor[][] base, BlockColor[][] extensions, boolean override) {
		BlockColor[][] extended = new BlockColor[base.length][];
		
		for (int i = 0, l = base.length; i < l; i++) {
			if (extensions[i] != null) {
				if (override || base[i] == null) {
					extended[i] = new BlockColor[extensions[i].length];
					System.arraycopy(extensions[i], 0, extended[i], 0, extensions[i].length);
				}
				else {
					BlockColor[] merged = new BlockColor[base[i].length + extensions.length];
					System.arraycopy(base[i], 0, merged, 0, base[i].length);
					System.arraycopy(extensions[i], 0, merged, base[i].length, extensions[i].length);
					extended[i] = merged;
				}
			}
			else if (base[i] != null) {
				extended[i] = new BlockColor[base[i].length];
				System.arraycopy(base[i], 0, extended[i], 0, base[i].length);
			}
			else {
				extended[i] = null;
			}
		}
		
		return extended;
	}
	
	public static MapManager[] loadMapManagers(Settings settings, mod_LiveMap mod) {
		if (!settings.hasKey("maps"))
			return null;
		
		List<MapManager> mapsList = new ArrayList<MapManager>();
		
		String[] mapNames = settings.get("maps").split("[,; ]+");
		for (String mapName : mapNames) {
			String section = "map:" + mapName;
			if (!settings.hasSection(section)) {
				ModLoader.outputError(mod.getName() + " is missing settings for map: " + mapName);
			}
			else {
				try {
					mapsList.add(new MapManager(mod, settings, section));
					
				} catch (IOException e) {
					ModLoader.outputError(e, mod.getName() + " failed (" + e.getClass().getSimpleName() + ") to create map " + mapName + ": " + e.getMessage());
					
				} catch (MissingSettingException e) {
					ModLoader.outputError(e, mod.getName() + " failed to create map " + mapName + " as it was missing the " + e.key + " setting.");
					
				} catch (IllegalSettingException e) {
					if (e.key.equalsIgnoreCase("imageSize"))
						ModLoader.outputError(e, mod.getName() + " failed to create map " + mapName + " as the " + e.key + " setting must be a power of 2: " + e.value);
					else
						ModLoader.outputError(e, mod.getName() + " failed to create map " + mapName + " as it had an invalid value for the " + e.key + " setting: " + e.value);
				}
			}
		}
		
		return mapsList.toArray(new MapManager[mapsList.size()]);
	}
}
