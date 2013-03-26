package btwmod.livemap;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.Chunk;

import btwmods.ModLoader;
import btwmods.Util;
import btwmods.io.IllegalSettingException;
import btwmods.io.MissingSettingException;
import btwmods.io.Settings;

public class MapManager {
	public final mod_LiveMap mod;
	public final int dimension;
	public final int imageSize;
	public final BlockColor[][] blockColors;
	public final MapLayer[] mapLayers;
	public final boolean heightUndulate;
	public final boolean depthBrightness;
	public final Color baseColor;
	public final int forcedMinHeight;
	public final int forcedMaxHeight;
	
	public MapManager(mod_LiveMap mod, Settings settings, String section) throws IOException {
		if (!settings.hasKey(section, "directory"))
			throw new MissingSettingException(section, "directory");
		
		if (!settings.isInt(section, "dimension"))
			throw new MissingSettingException(section, "dimension");
		
		this.mod = mod;
		dimension = settings.getInt(section, "dimension", 255);
		imageSize = settings.getInt(section, "imageSize", 256);
		heightUndulate = settings.getBoolean(section, "heightUndulate", true);
		depthBrightness = settings.getBoolean(section, "depthBrightness", true);
		forcedMinHeight = Math.max(0, Math.min(255, settings.getInt(section, "forcedMinHeight", 0)));
		forcedMaxHeight = Math.max(0, Math.min(255, settings.getInt(section, "forcedMaxHeight", 255)));
		
		BlockColor[][] overrides = null;
		if (settings.hasKey(section, "blockColorOverrides"))
			overrides = mod.loadColorData(new File(settings.get(section, "blockColorOverrides")), true);
		
		if (overrides != null)
			this.blockColors = mod.extendColorData(mod.getBlockColors(), overrides, true);
		else
			this.blockColors = mod.getBlockColors();
		
		if (Util.getWorldNameFromDimension(dimension) == null)
			throw new IllegalSettingException(section, "dimension", Integer.toString(dimension));
		
		if (imageSize <= 0 || (imageSize & (imageSize - 1)) != 0)
			throw new IllegalSettingException(section, "imageSize", Integer.toString(imageSize));
		
		Color baseColor = null;
		String baseColorSetting = settings.get(section, "baseColor");
		if (baseColorSetting != null) {
			if (!baseColorSetting.matches("^#[A-Fa-f0-9]{6}$"))
				throw new IllegalSettingException(section, "baseColor", baseColorSetting);
			
			baseColor = new Color(
				Integer.parseInt(baseColorSetting.substring(1, 3), 16),
				Integer.parseInt(baseColorSetting.substring(3, 5), 16),
				Integer.parseInt(baseColorSetting.substring(5, 7), 16)
			);
		}
		this.baseColor = baseColor;
		
		File directory = new File(settings.get(section, "directory"));
		
		// Create the directory for the map, if it does not exist.
		if (!directory.isDirectory() && !directory.mkdir())
			throw new IOException("Failed to mkdir " + directory.getPath());
		
		int chunksPerImage = imageSize;
		int chunksPerMaxZoom = imageSize / 16;

		// Initialize the MapLayers and create the their sub directories, if they do not exist.
		List<MapLayer> layers = new ArrayList<MapLayer>();
		while (chunksPerImage >= chunksPerMaxZoom) {
			File zoomDir = new File(directory, Integer.toString(chunksPerImage));
			
			if (!zoomDir.isDirectory() && !zoomDir.mkdir())
				throw new IOException("Failed to mkdir " + zoomDir.getPath());
			
			layers.add(new MapLayer(this, zoomDir, chunksPerImage));
			
			chunksPerImage >>= 1;
		}
		
		this.mapLayers = layers.toArray(new MapLayer[layers.size()]);
	}
	
	public boolean hasHeightImage() {
		return heightUndulate;
	}
	
	public int getImageCount() {
		int count = 0;
		for (int i = 0; i < mapLayers.length; i++) {
			count += mapLayers[i].getImageCount();
		}
		return count;
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

	public void save(boolean clear) {
		for (int i = 0; i < mapLayers.length; i++) {
			if (mapLayers[i] != null) {
				try {
					mapLayers[i].save();
				} catch (Exception e) {
					ModLoader.outputError(e, mod.getName() + " failed (" + e.getClass().getSimpleName() + ") to save images for layer " + (i+1) + ": " + e.getMessage());
				}
				
				if (clear)
					mapLayers[i].clear();
			}
		}
	}
}
