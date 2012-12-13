package btwmod.livemap;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.World;

public class QueuedRegion {
	public final World world;
	public final int worldIndex;
	public final File location;
	public final int regionX;
	public final int regionZ;
	
	public QueuedRegion(int worldIndex, File location, int regionX, int regionZ) {
		this.worldIndex = worldIndex;
		this.location = location;
		this.regionX = regionX;
		this.regionZ = regionZ;
		
		world = worldIndex < MinecraftServer.getServer().worldServers.length ? MinecraftServer.getServer().worldServers[worldIndex] : null;
	}
}
