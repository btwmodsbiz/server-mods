package btwmod.livemap;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.src.Chunk;
import net.minecraft.src.MathHelper;

public class MapLayer {

	public final MapManager map;
	public final File layerDirectory;
	public final int chunksPerImage;

	protected Map<String, MapImage> images = new LinkedHashMap<String, MapImage>();

	public MapLayer(MapManager map, File layerDirectory, int chunksPerImage) {
		this.map = map;
		this.layerDirectory = layerDirectory;
		this.chunksPerImage = chunksPerImage;
		
		if (map.imageSize / chunksPerImage == 0)
			throw new IllegalArgumentException("imageSize divided by chunksPerImage cannot be less than 1");
	}
	
	public MapImage provideImage(Chunk chunk) throws IOException {
		return provideImage(chunk.xPosition, chunk.zPosition);
	}

	private MapImage provideImage(int chunkX, int chunkZ) throws IOException {
		String fileName = getChunkKey(chunkX, chunkZ) + ".png";
		MapImage image = images.get(fileName);
		if (image == null) {
			images.put(fileName, image = new MapImage(this, chunkX, chunkZ));
		}
		return image;
	}

	public void processChunk(Chunk chunk) throws IOException {
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
	
	protected void save() throws IOException {
		for (Entry<String, MapImage> entry : images.entrySet()) {
			System.out.println("Saving " + entry.getKey());
			entry.getValue().save();
		}
	}
	
	protected void clear() {
		images.clear();
	}
}
