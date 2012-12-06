package btwmod.chunkcleaner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Chunk;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommand;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import net.minecraft.src.WorldServer;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ServerAPI;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.world.IWorldTickListener;
import btwmods.world.WorldTickEvent;

public class mod_ChunkCleaner implements IMod, IWorldTickListener {
	
	public static final int MAX_CHUNKS_CHECKED = 50;
	public static final int CHUNK_RANGE = 12;
	
	private static Random rnd = new Random();
	public int chunksChecked = 5;
	public int checkFrequency = 20;
	public boolean debugLogging = false;
	public boolean enabled = true;
	
	private ICommand commandChunk;

	@Override
	public String getName() {
		return "Chunk Cleaner";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		chunksChecked = Math.min(MAX_CHUNKS_CHECKED, Math.max(1, settings.getInt("chunksChecked", chunksChecked)));
		enabled = settings.getBoolean("enabledOnStart", enabled);
		debugLogging = settings.getBoolean("debugLogging", debugLogging);
		checkFrequency = Math.max(1, settings.getInt("checkFrequency", checkFrequency));
		
		WorldAPI.addListener(this);
		CommandsAPI.registerCommand(commandChunk = new CommandChunk(this), this);
	}

	@Override
	public void unload() throws Exception {
		WorldAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandChunk);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onWorldTick(WorldTickEvent event) {
		if (enabled && event.getType() == WorldTickEvent.TYPE.END && ServerAPI.getTickCounter() % checkFrequency == 0
				&& MinecraftServer.getServer().getConfigurationManager().playerEntityList.size() != 0) {
			
			// Get the list of chunks and its size.
			List<Chunk> chunks = WorldAPI.getLoadedChunks()[event.getWorldIndex()];
			int size = chunks.size();
			
			// Determine where to start in the chunk list.
			int startPos = size <= chunksChecked ? 0 : rnd.nextInt(size - chunksChecked);
			
			// Check the chunks.
			for (int i = startPos; i < Math.min(size, startPos + chunksChecked); i++) {
				checkChunk((WorldServer)event.getWorld(), chunks.get(i));
			}
		}
	}
	
	private void checkChunk(WorldServer world, Chunk chunk) {
		if (!hasPlayerInRange(world, chunk.xPosition, chunk.zPosition)) {
			world.theChunkProviderServer.dropChunk(chunk.xPosition, chunk.zPosition);
			
			if (debugLogging)
				ModLoader.outputInfo("Cleaned chunk: " + chunk.xPosition + "," + chunk.zPosition);
		}
	}
	
	public static boolean hasPlayerInRange(World world, int chunkX, int chunkZ) {
		return !getPlayersWithinRange(world, chunkX, chunkZ, true).isEmpty();
	}
	
	public static List<EntityPlayer> getPlayersWithinRange(World world, int chunkX, int chunkZ) {
		return getPlayersWithinRange(world, chunkX, chunkZ, false);
	}
	
	private static List<EntityPlayer> getPlayersWithinRange(World world, int chunkX, int chunkZ, boolean stopAtFirst) {
		List<EntityPlayer> players = world.playerEntities;
		ArrayList<EntityPlayer> inRange = new ArrayList<EntityPlayer>();
		
		for (EntityPlayer player : players) {
			int playerChunkX = MathHelper.floor_double(player.posX) >> 4;
			int playerChunkZ = MathHelper.floor_double(player.posZ) >> 4;
			
			if (chunkX >= playerChunkX - CHUNK_RANGE && chunkX <= playerChunkX + CHUNK_RANGE && chunkZ >= playerChunkZ - CHUNK_RANGE && chunkZ <= playerChunkZ + CHUNK_RANGE) {
				inRange.add(player);
				
				if (stopAtFirst)
					break;
			}
		}
		
		if (inRange.size() == 0)
			inRange.toString();
		
		return inRange;
	}
}
