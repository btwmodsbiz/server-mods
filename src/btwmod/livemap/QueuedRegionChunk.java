package btwmod.livemap;

import java.io.File;

public class QueuedRegionChunk extends QueuedRegion {
	public final int chunkX;
	public final int chunkZ;
	
	public QueuedRegionChunk(QueuedRegion region, int chunkX, int chunkZ) {
		this(region.worldIndex, region.location, region.regionX, region.regionZ, chunkX, chunkZ);
	}
	
	public QueuedRegionChunk(int worldIndex, File location, int regionX, int regionZ, int chunkX, int chunkZ) {
		super(worldIndex, location, regionX, regionZ);
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}
}
