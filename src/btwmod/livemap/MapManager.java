package btwmod.livemap;

import java.io.File;
import java.io.IOException;

import net.minecraft.src.Chunk;

import btwmods.ModLoader;

public class MapManager {
	public final mod_LiveMap mod;
	public final int dimension;
	public final int imageSize;
	public final BlockColor[] blockColors;
	public final MapLayer[] mapLayers;
	
	public MapManager(mod_LiveMap mod, int dimension, int imageSize, int[] zoomLevels, BlockColor[] blockColors, File directory) {
		this.mod = mod;
		this.dimension = dimension;
		this.imageSize = imageSize;
		this.blockColors = blockColors;
		
		if (!directory.isDirectory() && !directory.mkdir()) {
			ModLoader.outputError(mod.getName() + " failed to create the map directory: " + directory.getPath());
			mapLayers = new MapLayer[0];
		}
		else {
			mapLayers = new MapLayer[zoomLevels.length];
			for (int i = 0; i < zoomLevels.length; i++) {
				File zoomDir = new File(directory, Integer.toString(i + 1));
				
				if (!zoomDir.isDirectory() && !zoomDir.mkdir()) {
					mapLayers[i] = null;
					ModLoader.outputError(mod.getName() + " failed to create the directory for zoom level " + (i + 1) + ": " + zoomDir.getPath());
				}
				else {
					mapLayers[i] = new MapLayer(this, zoomDir, zoomLevels[i]);
				}
			}
		}
	}

	public void processChunk(Chunk chunk) {
		for (int i = 0; i < mapLayers.length; i++) {
			if (mapLayers[i] != null) {
				try {
					mapLayers[i].processChunk(chunk);
				} catch (IOException e) {
					// TODO: Handle error properly.
					e.printStackTrace();
				}
			}
		}
	}

	public void saveAndClear() {
		for (int i = 0; i < mapLayers.length; i++) {
			if (mapLayers[i] != null) {
				try {
					mapLayers[i].save();
				} catch (IOException e) {
					// TODO: Handle error properly.
					e.printStackTrace();
				}
				
				mapLayers[i].clear();
			}
		}
	}
}
