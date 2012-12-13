package btwmod.livemap;

import net.minecraft.src.Chunk;
import net.minecraft.src.MathHelper;

public class MapPos {
	
	public final float pixelsPerChunk;
	
	public final MapLayer mapLayer;
	
	/**
	 * The MapImage's X coordinate.
	 */
	public final int mapImageX;

	/**
	 * The MapImage's Z coordinate.
	 */
	public final int mapImageZ;
	
	/**
	 * The X coordinate of the upper left chunk in the MapImage.
	 */
	public final int mapImageChunkX;
	
	/**
	 * The Z coordinate of the upper left chunk in the MapImage.
	 */
	public final int mapImageChunkZ;
	
	/**
	 * The Chunk's X coordinate.
	 */
	public final int chunkX;

	/**
	 * The Chunk's Z coordinate.
	 */
	public final int chunkZ;

	/**
	 * The X coordinate of the Chunk within the MapImage.
	 */
	public final int chunkOffsetX;

	/**
	 * The X coordinate of the Chunk within the MapImage.
	 */
	public final int chunkOffsetZ;
	
	public MapPos(Chunk chunk, MapLayer mapLayer) {
		this(chunk.xPosition, chunk.zPosition, mapLayer);
	}
	
	public MapPos(int chunkX, int chunkZ, MapLayer mapLayer) {
		this.mapLayer = mapLayer;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		
		mapImageX = MathHelper.floor_float((float)chunkX / (float)mapLayer.chunksPerImage);
		mapImageZ = MathHelper.floor_float((float)chunkZ / (float)mapLayer.chunksPerImage);
		
		mapImageChunkX = mapImageX * mapLayer.chunksPerImage;
		mapImageChunkZ = mapImageZ * mapLayer.chunksPerImage;
		
		chunkOffsetX = chunkX - mapImageChunkX;
		chunkOffsetZ = chunkZ - mapImageChunkZ;
		
		pixelsPerChunk = (float)mapLayer.map.imageSize / (float)mapLayer.chunksPerImage;
	}
	
	public int getX(int x) {
		return MathHelper.floor_float((chunkOffsetX * pixelsPerChunk) + (x * pixelsPerChunk / 16));
	}
	
	public int getZ(int z) {
		return MathHelper.floor_float((chunkOffsetZ * pixelsPerChunk) + (z * pixelsPerChunk / 16));
	}
	
	public boolean isSameMapImage(MapPos pos) {
		return mapImageX == pos.mapImageX && mapImageZ == pos.mapImageZ;
	}
	
	public static MapPos FromPos(int x, int z, MapLayer mapLayer) {
		return new MapPos(x >> 4, z >> 4, mapLayer);
	}
}
