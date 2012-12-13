package btwmod.livemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.AnvilChunkLoader;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkProviderServer;
import net.minecraft.src.IChunkLoader;
import net.minecraft.src.RegionFile;
import net.minecraft.src.RegionFileCache;
import net.minecraft.src.WorldServer;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ReflectionAPI;
import btwmods.ServerAPI;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.server.ITickListener;
import btwmods.server.TickEvent;
import btwmods.world.ChunkEvent;
import btwmods.world.IChunkListener;

public class mod_LiveMap implements IMod, IChunkListener, ITickListener {
	
	private int regionChunksDequeuedPerSecond = 20;

	private int[] zoomLevels = { 128, 64, 32, 16, 8, 4, 2, 1 };
	private int imageSize = 256;
	private File imageDir = ModLoader.modDataDir;
	private File colorData = new File(ModLoader.modsDir, "livemap-colors.txt");

	private ConcurrentLinkedQueue<Chunk> chunkQueue = new ConcurrentLinkedQueue<Chunk>();
	private volatile ChunkProcessor chunkProcessor = null;
	
	private Deque<QueuedRegion> regionQueue = new ArrayDeque<QueuedRegion>();

	public final BlockColor[] blockColors = new BlockColor[Block.blocksList.length];
	
	private IChunkLoader chunkLoaders[];
	private File chunkLoaderLocations[];
	
	private CommandMap commandMap = null;

	@Override
	public String getName() {
		return "Live Map";
	}
	
	public IChunkLoader getChunkLoader(int worldIndex) {
		if (chunkLoaders != null && worldIndex < chunkLoaders.length && chunkLoaders[worldIndex] != null) {
			return chunkLoaders[worldIndex];
		}
		
		return null;
	}
	
	public File getAnvilSaveLocation(int worldIndex) {
		if (chunkLoaders != null && worldIndex < chunkLoaders.length && chunkLoaders[worldIndex] != null) {
			return chunkLoaderLocations[worldIndex];
		}
		
		return null;
	}
	
	public void queueRegion(int worldIndex, File location, int x, int z) {
		regionQueue.add(new QueuedRegion(worldIndex, location, x, z));
	}

	@Override
	public void onTick(TickEvent event) {
		if (event.getType() == TickEvent.TYPE.END) {
			if (event.getTickCounter() % 20 == 0) {
				QueuedRegion region;
				int polled = 0;
				while (++polled <= regionChunksDequeuedPerSecond && (region = regionQueue.pollLast()) != null) {
					if (region instanceof QueuedRegionChunk) {
						QueuedRegionChunk regionChunk = (QueuedRegionChunk)region;
						//System.out.println("QueuedRegionChunk " + regionChunk.chunkX + "," + regionChunk.chunkZ + "(" + region.regionX + "." + region.regionZ + ")");
						
						if (((WorldServer)regionChunk.world).theChunkProviderServer.chunkExists(regionChunk.chunkX, regionChunk.chunkZ)) {
							System.out.println("Skipped loaded chunk " + regionChunk.chunkX + "." + regionChunk.chunkZ);
						}
						else {
							//System.out.println("Queued rendering of chunk " + regionChunk.chunkX + "." + regionChunk.chunkZ);
							
							try {
								Chunk chunk = getChunkLoader(regionChunk.worldIndex).loadChunk(regionChunk.world, regionChunk.chunkX, regionChunk.chunkZ);
								
								if (chunk == null) {
									ModLoader.outputError(getName() + " failed to get chunk input stream for chunk " + regionChunk.chunkX + "," + regionChunk.chunkZ + " in world " + regionChunk.worldIndex);
								}
								else {
									// Remove any tick updates that were queued.
									regionChunk.world.getPendingBlockUpdates(chunk, true);
									
									queueChunk(chunk);
								}
								
							} catch (Exception e) {
								ModLoader.outputError(getName() + " failed (" + e.getClass().getSimpleName() + ") to load chunk " + regionChunk.chunkX + "," + regionChunk.chunkZ + " for world " + regionChunk.worldIndex + ": " + e.getMessage());
							}
						}
					}
					else {
						RegionFile regionFile = RegionFileCache.createOrLoadRegionFile(region.location, region.regionX << 5, region.regionZ << 5);
						//System.out.println("QueuedRegion " + region.regionX + "." + region.regionZ);
						for (int x = 0; x < 32; x++) {
							for (int z = 0; z < 32; z++) {
								if (regionFile.isChunkSaved(x, z)) {
									//System.out.println("Queued region chunk " + region.regionX + "." + region.regionZ + " (" + x + "," + z + ") " + (region.regionX << 5 | x) + "," + (region.regionZ << 5 | z));
									regionQueue.add(new QueuedRegionChunk(region, region.regionX << 5 | x, region.regionZ << 5 | z));
								}
								else {
									//System.out.println("Skipping region chunk " + region.regionX + "." + region.regionZ + " (" + x + "," + z + ") " + (region.regionX << 5 | x) + "," + (region.regionZ << 5 | z));
								}
							}
						}
						
						//break;
					}
				}
			}
		}
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		imageSize = settings.getInt("imageSize", imageSize);
		if (imageSize < 1 || imageSize % 16 != 0) {
			ModLoader.outputError(getName() + "'s imageSize must be a positive integer that is divisible by 16.", Level.SEVERE);
			return;
		}

		if (settings.hasKey("imageDir")) {
			imageDir = new File(settings.get("imageDir"));
		}

		if (!imageDir.isDirectory()) {
			ModLoader.outputError(getName() + "'s imageDir does not exist or is not a directory.", Level.SEVERE);
			return;
		}

		if (settings.hasKey("colorData")) {
			colorData = new File(settings.get("colorData"));
		}
		
		try {
			Field chunkLoaderField = ReflectionAPI.getPrivateField(ChunkProviderServer.class, "chunkLoader");
			Field chunkSaveLocationField = ReflectionAPI.getPrivateField(AnvilChunkLoader.class, "chunkSaveLocation");
			
			chunkLoaders = new IChunkLoader[MinecraftServer.getServer().worldServers.length];
			chunkLoaderLocations = new File[chunkLoaders.length];
			
			for (int i = 0; i < chunkLoaders.length; i++) {
				chunkLoaders[i] = (IChunkLoader)chunkLoaderField.get(MinecraftServer.getServer().worldServers[i].getChunkProvider());
				chunkLoaderLocations[i] = (File)chunkSaveLocationField.get(chunkLoaders[i]);
			}
		}
		catch (Exception e) {
			ModLoader.outputError(getName() + " failed (" + e.getClass().getSimpleName() + ") to load the chunkLoaders and chunkSaveLocations: " + e.getMessage());
			chunkLoaders = null;
			chunkLoaderLocations = null;
		}

		if (!loadColorData())
			return;

		WorldAPI.addListener(this);
		ServerAPI.addListener(this);
		CommandsAPI.registerCommand(commandMap = new CommandMap(this), this);
	}

	@Override
	public void unload() throws Exception {
		WorldAPI.removeListener(this);
		ServerAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandMap);
	}

	private boolean loadColorData() {
		if (colorData.isFile()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(colorData));
	
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.trim().length() > 0 && line.trim().charAt(0) != '#') {
						BlockColor color = BlockColor.fromConfigLine(line);
		
						if (color == null) {
							ModLoader.outputError(getName() + " found an invalid colorData entry: " + line, Level.SEVERE);
							return false;
		
						} else {
							Block block = BlockColor.getBlockByName(color.blockName);
		
							if (block == null) {
								// TODO: Report color for block that does not exist?
							} else if (blockColors[block.blockID] != null) {
								ModLoader.outputError(getName() + " found duplicate colorData entries for: " + block.getBlockName(), Level.SEVERE);
								return false;
							} else {
								blockColors[block.blockID] = color;
							}
						}
					}
				}
			} catch (IOException e) {
				ModLoader.outputError(e, getName() + " failed to read the colorData file: " + e.getMessage(), Level.SEVERE);
				return false;
			} finally {
				try {
					if (reader != null)
						reader.close();
				} catch (IOException e) {
	
				}
			}
		}

		// Set colors for blocks not set by the config.
		for (int i = 0; i < blockColors.length; i++) {
			if (Block.blocksList[i] != null && blockColors[i] == null) {
				blockColors[i] = BlockColor.fromBlock(Block.blocksList[i]);
			}
		}

		return true;
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onChunkAction(ChunkEvent event) {
		if (event.getType() == ChunkEvent.TYPE.UNLOADED) {
			queueChunk(event.getChunk());
		}
	}
	
	public void queueChunk(Chunk chunk) {
		chunkQueue.add(chunk);

		if (chunkProcessor == null || !chunkProcessor.isRunning()) {
			new Thread(chunkProcessor = new ChunkProcessor(new MapManager[] { new MapManager(this, 0, imageSize, zoomLevels, blockColors, new File(imageDir, "overworld")) }), getName() + " Thread").start();
		}
	}

	private class ChunkProcessor implements Runnable {
		
		private final MapManager[] maps;
		
		private volatile boolean isRunning = true;

		public boolean isRunning() {
			return isRunning;
		}
		
		public ChunkProcessor(MapManager[] maps) {
			this.maps = maps;
		}

		@Override
		public void run() {
			while (this == chunkProcessor) {
				
				Chunk chunk = null;
				int count = 0;
				while ((chunk = chunkQueue.poll()) != null) {
					renderChunk(chunk);
					
					if (++count % 50 == 0)
						save();
				}
				
				save();

				try {
					Thread.sleep(2L * 1000L);
				} catch (InterruptedException e) {

				}
			}

			isRunning = false;
		}
		
		private void renderChunk(Chunk chunk) {
			//System.out.println("renderChunk " + chunk.xPosition + "," + chunk.zPosition);
			for (int i = 0; i < maps.length; i++) {
				maps[i].processChunk(chunk);
			}
		}
		
		protected void save() {
			for (int i = 0; i < maps.length; i++) {
				maps[i].saveAndClear();
			}
		}
	}
}
