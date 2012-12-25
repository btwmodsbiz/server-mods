package btwmod.livemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.AnvilChunkLoader;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkProviderServer;
import net.minecraft.src.IChunkLoader;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ReflectionAPI;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.world.ChunkEvent;
import btwmods.world.IChunkListener;

public class mod_LiveMap implements IMod, IChunkListener {
	
	public boolean debugMessages = false;

	private int[] zoomLevels = { 256, 128, 64, 32, 16 };
	private int imageSize = 256;
	private File imageDir = ModLoader.modDataDir;
	public final File tempSave = new File(ModLoader.modDataDir, "livemap.temp");
	private File colorData = new File(ModLoader.modsDir, "livemap-colors.txt");
	public final BlockColor[][] blockColors = new BlockColor[Block.blocksList.length][];
	
	private volatile ChunkProcessor chunkProcessor = null;

	private Queue<Chunk> chunkQueue = new ConcurrentLinkedQueue<Chunk>();
	private AtomicInteger chunkQueueCount = new AtomicInteger();
	
	private IChunkLoader chunkLoaders[];
	private File chunkLoaderLocations[];
	
	private CommandMap commandMap = null;
	
	private RegionLoader regionLoader = null;

	private MapManager[] mapManagers = null;

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
	
	public int getChunkQueueCount() {
		return chunkQueueCount.get();
	}
	
	public RegionLoader getRegionLoader() {
		return regionLoader;
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
		
		if (!loadMapManagers(settings)) {
			ModLoader.outputError(getName() + " does not have any valid maps specified.");
			return;
		}
		
		regionLoader = new RegionLoader(this, settings);

		WorldAPI.addListener(this);
		ServerAPI.addListener(regionLoader);
		CommandsAPI.registerCommand(commandMap = new CommandMap(this), this);
	}

	@Override
	public void unload() throws Exception {
		WorldAPI.removeListener(this);
		ServerAPI.removeListener(regionLoader);
		CommandsAPI.unregisterCommand(commandMap);
	}

	private boolean loadColorData() {
		List<List<BlockColor>> blockColorsTemp = new ArrayList<List<BlockColor>>();
		for (int i = 0; i < Block.blocksList.length; i++)
			blockColorsTemp.add(null);
		
		if (colorData.isFile()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(colorData));
	
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.trim().length() > 0 && line.trim().charAt(0) != '#') {
						BlockColor color;
						try {
							color = BlockColor.fromConfigLine(line);
						} catch (IllegalArgumentException e) {
							ModLoader.outputError(getName() + " found an invalid ('" + e.getMessage() + "') colorData entry: " + line, Level.SEVERE);
							return false;
						} catch (ParseException e) {
							ModLoader.outputError(getName() + " found an invalid ('" + e.getMessage() + "') colorData entry: " + line, Level.SEVERE);
							return false;
						}
		
						if (color == null) {
							ModLoader.outputError(getName() + " found an invalid colorData entry: " + line, Level.SEVERE);
							return false;
		
						} else {
							Set<Block> blocks = BlockColor.getBlocksByName(color.blockName);
		
							if (blocks == null) {
								// TODO: Report color for block that does not exist?
							}
							else {
								for (Block block : blocks) {
									List<BlockColor> list = blockColorsTemp.get(block.blockID);
									if (list == null) {
										blockColorsTemp.set(block.blockID, list = new ArrayList<BlockColor>());
									}
									list.add(color);
								}
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
			if (Block.blocksList[i] != null && Block.blocksList[i].getBlockName() != null) {
				boolean genericFound = false;
				List<BlockColor> list = blockColorsTemp.get(i);
				if (list == null) {
					blockColorsTemp.set(i, list = new ArrayList<BlockColor>());
				}
				else {
					for (BlockColor color : list) {
						if (!color.hasFilter())
							genericFound = true;
					}
				}
				
				if (!genericFound) {
					BlockColor color = BlockColor.fromBlock(Block.blocksList[i]);
					
					// Only add blocks that have a proper color.
					if (color.red != 0 || color.green != 0 || color.blue != 0)
						list.add(BlockColor.fromBlock(Block.blocksList[i]));
				}
				
				if (list.size() > 0)
					blockColors[i] = blockColorsTemp.get(i).toArray(new BlockColor[list.size()]);
			}
		}
		
		for (int i = 0; i < blockColors.length; i++) {
			List<BlockColor> list = blockColorsTemp.get(i);
			blockColors[i] = list == null || list.size() == 0 ? null : blockColorsTemp.get(i).toArray(new BlockColor[list.size()]);
		}

		return true;
	}
	
	private boolean loadMapManagers(Settings settings) {
		
		if (!settings.hasKey("maps")) {
			return false;
		}
		
		List<MapManager> mapsList = new ArrayList<MapManager>();
		
		String[] mapNames = settings.get("maps").split("[,; ]+");
		for (String mapName : mapNames) {
			String section = "map:" + mapName;
			if (!settings.hasSection(section)) {
				ModLoader.outputError(getName() + " is missing the a settings section for map: " + mapName);
			}
			else {
				
				if (!settings.hasKey(section, "directory")) {
					ModLoader.outputError(getName() + " is missing the 'directory' setting for: " + section);
					continue;
				}
				
				File directory = new File(settings.get(section, "directory"));
				
				if (!settings.hasKey(section, "dimension")) {
					ModLoader.outputError(getName() + " is missing the 'dimension' setting for: " + section);
					continue;
				}
				
				int dimension = settings.getInt(section, "dimension", 255);
				
				if (Util.getWorldNameFromDimension(dimension) == null) {
					ModLoader.outputError(getName() + " has an invalid 'dimension' setting for: " + section);
					continue;
				}
				
				int imageSize = settings.getInt(section, "imageSize", 256);
				
				if (imageSize <= 0 || (imageSize & (imageSize - 1)) != 0) {
					ModLoader.outputError(getName() + " must have a 'imageSize' setting that is a power of 2 for: " + section);
					continue;
				}
				
				//new MapManager(this, 0, imageSize, zoomLevels, blockColors, new File(imageDir, "overworld"))
				mapsList.add(new MapManager(this, dimension, imageSize, zoomLevels, blockColors, directory));
			}
		}
		
		mapManagers = mapsList.toArray(new MapManager[mapsList.size()]);
		
		return mapManagers.length > 0;
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
		chunkQueueCount.incrementAndGet();
		
		if (chunkProcessor == null || !chunkProcessor.isRunning()) {
			new Thread(chunkProcessor = new ChunkProcessor(mapManagers), getName() + " Thread").start();
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
				long start = System.currentTimeMillis();
				long saving = 0;
				while ((chunk = chunkQueue.poll()) != null) {
					chunkQueueCount.decrementAndGet();
					renderChunk(chunk);
					
					if (debugMessages && count == 0)
						ModLoader.outputInfo(getName() + " thread rendered chunk " + chunk.xPosition + "," + chunk.zPosition + ".");
					
					// Save and clear the images every so many chunks.
					if (++count % 50 == 0) {
						long startSaving = System.currentTimeMillis();
						save();
						saving += System.currentTimeMillis() - startSaving;
					}
				}

				long startSaving = System.currentTimeMillis();
				save();
				saving += System.currentTimeMillis() - startSaving;

				if (debugMessages && count > 0)
					ModLoader.outputInfo(getName() + " thread rendered " + count + " chunks in " + (System.currentTimeMillis() - start) + "ms (" + saving + "ms saving).");

				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {

				}
			}

			isRunning = false;
		}
		
		private void renderChunk(Chunk chunk) {
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
