package btwmod.livemap;

import java.awt.Color;
import java.io.File;

import net.minecraft.src.Chunk;

import btwmods.ModLoader;
import btwmods.Util;

public class MapManager {
	public final mod_LiveMap mod;
	public final int dimension;
	public final int imageSize;
	public final BlockColor[][] blockColors;
	public final MapLayer[] mapLayers;
	public final boolean heightUndulate;
	public final boolean depthBrightness;
	public final Color baseColor;
	
	public MapManager(mod_LiveMap mod, int dimension, int imageSize, int[] zoomLevels, BlockColor[][] blockColors, File directory, boolean heightUndulate, boolean depthBrightness, Color baseColor) {
		this.mod = mod;
		this.dimension = dimension;
		this.imageSize = imageSize;
		this.blockColors = blockColors;
		this.heightUndulate = heightUndulate;
		this.depthBrightness = depthBrightness;
		this.baseColor = baseColor;
		
		if (Util.getWorldNameFromDimension(dimension) == null)
			throw new IllegalArgumentException("dimension");
		
		if ((imageSize & (imageSize - 1)) != 0)
			throw new IllegalArgumentException("imageSize");
		
		if (zoomLevels.length == 0)
			throw new IllegalArgumentException("zoomLevels");
		
		if (!directory.isDirectory() && !directory.mkdir()) {
			ModLoader.outputError(mod.getName() + " failed to create the map directory: " + directory.getPath());
			mapLayers = new MapLayer[0];
		}
		else {
			mapLayers = new MapLayer[zoomLevels.length];
			for (int i = 0; i < zoomLevels.length; i++) {
				if ((zoomLevels[i] & (zoomLevels[i] - 1)) != 0) {
					throw new IllegalArgumentException("zoomLevels");
				}
				
				File zoomDir = new File(directory, Integer.toString(zoomLevels[i]));
				
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
	
	public boolean hasHeightImage() {
		return heightUndulate;
	}

	public void processChunk(Chunk chunk) {
		for (int i = 0; i < mapLayers.length; i++) {
			if (mapLayers[i] != null) {
				try {
					mapLayers[i].processChunk(chunk);
				} catch (Exception e) {
					ModLoader.outputError(e, mod.getName() + " failed (" + e.getClass().getSimpleName() + ") to process chunk " + chunk.xPosition + "," + chunk.zPosition + " for layer " + (i+1) + ": " + e.getMessage());
				}
			}
		}
	}

	public void saveAndClear() {
		for (int i = 0; i < mapLayers.length; i++) {
			if (mapLayers[i] != null) {
				try {
					mapLayers[i].save();
				} catch (Exception e) {
					ModLoader.outputError(e, mod.getName() + " failed (" + e.getClass().getSimpleName() + ") to save images for layer " + (i+1) + ": " + e.getMessage());
				}
				
				mapLayers[i].clear();
			}
		}
	}
}
