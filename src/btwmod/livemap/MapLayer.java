package btwmod.livemap;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import btwmods.ModLoader;

import net.minecraft.src.Chunk;
import net.minecraft.src.MathHelper;

public class MapLayer {

	public final MapManager map;
	public final File layerDirectory;
	public final int chunksPerImage;

	protected final MapImageCache<String> images;

	public MapLayer(MapManager map, File layerDirectory, int chunksPerImage) {
		if (map.imageSize / chunksPerImage == 0)
			throw new IllegalArgumentException("imageSize divided by chunksPerImage cannot be less than 1");
		
		this.map = map;
		this.layerDirectory = layerDirectory;
		this.chunksPerImage = chunksPerImage;
		images = new MapImageCache<String>(this);
	}
	
	public MapImage provideImage(Chunk chunk) throws Exception {
		return provideImage(chunk.xPosition, chunk.zPosition);
	}

	private MapImage provideImage(int chunkX, int chunkZ) throws Exception {
		String fileName = getChunkKey(chunkX, chunkZ) + ".png";
		MapImage image = images.get(fileName);
		if (image == null) {
			images.put(fileName, image = new MapImage(this, chunkX, chunkZ));
			
			if (!image.loadImages()) {
				// TODO: Queue up the images for the chunks if failed.
				ModLoader.outputError(map.mod.getName() + " failed to load image: " + image.colorImageFile.getPath());
			}
		}
		return image;
	}

	public void processChunk(Chunk chunk) throws Exception {
		if (chunk.worldObj.provider.dimensionId == map.dimension) {
			provideImage(chunk).renderChunk(chunk);
		}
	}
	
	public int getImageCount() {
		return images.size();
	}

	protected String getChunkKey(Chunk chunk) {
		return getChunkKey(chunk.xPosition, chunk.zPosition);
	}

	protected String getChunkKey(int chunkX, int chunkZ) {
		return MathHelper.floor_float((float)chunkX / (float)chunksPerImage) + "_" + MathHelper.floor_float((float)chunkZ / (float)chunksPerImage);
	}
	
	protected void save() throws Exception {
		for (Entry<String, MapImage> entry : images.entrySet()) {
			entry.getValue().save();
		}
	}
	
	protected void clear() {
		images.clear();
	}

	public void onCacheRemoved(MapImage image) {
		try {
			image.save();
		} catch (IOException e) {
			ModLoader.outputError(e, map.mod.getName() + " failed (" + e.getClass().getSimpleName() + ") to save image to temp file for dimension " + map.dimension + " chunk " + image.chunkX + "/" + image.chunkZ + ": " + e.getMessage());
		}
	}
}
