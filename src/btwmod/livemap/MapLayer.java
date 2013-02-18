package btwmod.livemap;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import btwmods.ModLoader;

import net.minecraft.src.Chunk;
import net.minecraft.src.MathHelper;

public class MapLayer {

	public final MapManager map;
	public final File layerDirectory;
	public final int chunksPerImage;
	public final float pixelSize;

	protected Map<String, MapImage> images = new LinkedHashMap<String, MapImage>();

	public MapLayer(MapManager map, File layerDirectory, int chunksPerImage) {
		if (map.imageSize / chunksPerImage == 0)
			throw new IllegalArgumentException("imageSize divided by chunksPerImage cannot be less than 1");
		
		this.map = map;
		this.layerDirectory = layerDirectory;
		this.chunksPerImage = chunksPerImage;
		pixelSize = map.imageSize / chunksPerImage / 16.0F;
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
				ModLoader.outputError("Failed to load image: " + image.colorImageFile.getPath());
			}
		}
		return image;
	}

	public void processChunk(Chunk chunk) throws Exception {
		if (chunk.worldObj.provider.dimensionId == map.dimension) {
			provideImage(chunk).renderChunk(chunk);
		}
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
}
