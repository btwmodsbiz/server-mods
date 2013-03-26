package btwmod.livemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
import btwmods.WorldAPI;
import btwmods.io.IllegalSettingException;
import btwmods.io.MissingSettingException;
import btwmods.io.Settings;
import btwmods.server.IServerStopListener;
import btwmods.server.ServerStopEvent;
import btwmods.world.ChunkEvent;
import btwmods.world.IChunkListener;

public class mod_LiveMap implements IMod, IChunkListener, IServerStopListener {
	
	public final File tempSave = new File(ModLoader.modDataDir, "livemap.temp");

	public volatile int mapImageCacheMax = 20;
	public volatile boolean debugMessages = false;

	private File imageDir = ModLoader.modDataDir;
	private File colorData = new File(ModLoader.modsDir, "livemap-colors.txt");
	private BlockColor[][] blockColors;
	
	private volatile ChunkProcessor chunkProcessor = null;
	
	private boolean allowQueuing = true;
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
	
	public boolean getAllowQueuing() {
		return allowQueuing;
	}
	
	public int getChunkQueueCount() {
		return chunkQueueCount.get();
	}
	
	public RegionLoader getRegionLoader() {
		return regionLoader;
	}
	
	public BlockColor[][] getBlockColors() {
		return blockColors;
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		mapImageCacheMax = Math.min(500, Math.max(10, settings.getInt("mapImageCacheMax", mapImageCacheMax)));
		
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

		// Load block color data.
		if ((blockColors = loadColorData(colorData, true)) == null)
			return;
		
		// Set default colors for any not set by the data file.
		setColorDataDefaults(blockColors);
		
		if (!loadMapManagers(settings)) {
			ModLoader.outputError(getName() + " does not have any valid maps specified.");
			return;
		}
		
		regionLoader = new RegionLoader(this, settings);

		WorldAPI.addListener(this);
		ServerAPI.addListener(this);
		ServerAPI.addListener(regionLoader);
		CommandsAPI.registerCommand(commandMap = new CommandMap(this), this);
	}

	@Override
	public void unload() throws Exception {
		WorldAPI.removeListener(this);
		ServerAPI.removeListener(this);
		ServerAPI.removeListener(regionLoader);
		CommandsAPI.unregisterCommand(commandMap);
	}

	public BlockColor[][] loadColorData(File colorData, boolean logErrors) {
		if (!colorData.isFile()) {
			if (logErrors)
				ModLoader.outputError(getName() + " could not find color data file at: " + colorData.getPath(), Level.SEVERE);
			return null;
		}
		
		List<List<BlockColor>> blockColorsTemp = new ArrayList<List<BlockColor>>();
		for (int i = 0; i < Block.blocksList.length; i++)
			blockColorsTemp.add(null);
		
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
						if (logErrors)
							ModLoader.outputError(getName() + " found an invalid ('" + e.getMessage() + "') colorData entry: " + line, Level.SEVERE);
						return null;
					} catch (ParseException e) {
						if (logErrors)
							ModLoader.outputError(getName() + " found an invalid ('" + e.getMessage() + "') colorData entry: " + line, Level.SEVERE);
						return null;
					}
	
					if (color == null) {
						if (logErrors)
							ModLoader.outputError(getName() + " found an invalid colorData entry: " + line, Level.SEVERE);
						return null;
	
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
			if (logErrors)
				ModLoader.outputError(e, getName() + " failed to read the colorData file: " + e.getMessage(), Level.SEVERE);
			return null;
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {

			}
		}
		
		BlockColor[][] blockColors = new BlockColor[Block.blocksList.length][];
		
		for (int i = 0; i < blockColors.length; i++) {
			List<BlockColor> list = blockColorsTemp.get(i);
			blockColors[i] = list == null || list.size() == 0 ? null : list.toArray(new BlockColor[list.size()]);
		}

		return blockColors;
	}
	
	@SuppressWarnings("static-method")
	public void setColorDataDefaults(BlockColor[][] blockColors) {
		for (int i = 0, l = blockColors.length; i < l; i++) {
			if (Block.blocksList[i] != null && Block.blocksList[i].getBlockName() != null) {
				
				boolean genericFound = false;
				
				if (blockColors[i] != null) {
					for (BlockColor color : blockColors[i]) {
						if (!color.hasFilter())
							genericFound = true;
					}
				}
				
				if (!genericFound) {
					List<BlockColor> list = new ArrayList<BlockColor>();
					if (blockColors[i] != null)
						list.addAll(Arrays.asList(blockColors[i]));
					
					BlockColor color = BlockColor.fromBlock(Block.blocksList[i]);
					
					// Only add blocks that have a proper color.
					if (color.red != 0 || color.green != 0 || color.blue != 0)
						list.add(BlockColor.fromBlock(Block.blocksList[i]));
					
					if (list.size() > 0)
						blockColors[i] = list.toArray(new BlockColor[list.size()]);
				}
			}
		}
	}
	
	@SuppressWarnings("static-method")
	public BlockColor[][] extendColorData(BlockColor[][] base, BlockColor[][] extensions, boolean override) {
		BlockColor[][] extended = new BlockColor[base.length][];
		
		for (int i = 0, l = base.length; i < l; i++) {
			if (extensions[i] != null) {
				if (override || base[i] == null) {
					extended[i] = new BlockColor[extensions[i].length];
					System.arraycopy(extensions[i], 0, extended[i], 0, extensions[i].length);
				}
				else {
					BlockColor[] merged = new BlockColor[base[i].length + extensions.length];
					System.arraycopy(base[i], 0, merged, 0, base[i].length);
					System.arraycopy(extensions[i], 0, merged, base[i].length, extensions[i].length);
					extended[i] = merged;
				}
			}
			else if (base[i] != null) {
				extended[i] = new BlockColor[base[i].length];
				System.arraycopy(base[i], 0, extended[i], 0, base[i].length);
			}
			else {
				extended[i] = null;
			}
		}
		
		return extended;
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
				ModLoader.outputError(getName() + " is missing settings for map: " + mapName);
			}
			else {
				try {
					mapsList.add(new MapManager(this, settings, section));
					
				} catch (IOException e) {
					ModLoader.outputError(e, getName() + " failed (" + e.getClass().getSimpleName() + ") to create map " + mapName + ": " + e.getMessage());
					
				} catch (MissingSettingException e) {
					ModLoader.outputError(e, getName() + " failed to create map " + mapName + " as it was missing the " + e.key + " setting.");
					
				} catch (IllegalSettingException e) {
					if (e.key.equalsIgnoreCase("imageSize"))
						ModLoader.outputError(e, getName() + " failed to create map " + mapName + " as the " + e.key + " setting must be a power of 2: " + e.value);
					else
						ModLoader.outputError(e, getName() + " failed to create map " + mapName + " as it had an invalid value for the " + e.key + " setting: " + e.value);
				}
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
		if (!allowQueuing)
			return;
		
		chunkQueue.add(chunk);
		chunkQueueCount.incrementAndGet();
		
		if (chunkProcessor == null || !chunkProcessor.isRunning()) {
			new Thread(chunkProcessor = new ChunkProcessor(mapManagers), getName() + " Thread").start();
		}
	}
	
	public void clearQueue() {
		chunkQueue.clear();
		chunkQueueCount.set(0);
	}

	@Override
	public void onServerStop(ServerStopEvent event) {
		switch (event.getType()) {
			case PRE:
				allowQueuing = false;
				clearQueue();
				break;
			
			case POST:
				ChunkProcessor thread = chunkProcessor;
				chunkProcessor = null;
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					
				}
				
				if (thread != null && thread.isRunning()) {
					ModLoader.outputInfo(getName() + " is waiting for images to save...");
					
					while (thread.isRunning()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							
						}
					}
				}
				break;
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
				long nextSave = System.currentTimeMillis() + (5 * 1000);
				while (this == chunkProcessor && (chunk = chunkQueue.poll()) != null) {
					chunkQueueCount.decrementAndGet();
					renderChunk(chunk);
					
					if (System.currentTimeMillis() > nextSave) {
						save(false);
						nextSave = System.currentTimeMillis() + (5 * 1000);
					}
				}

				save(true);

				if (debugMessages && count > 0)
					ModLoader.outputInfo(getName() + " thread rendered " + count + " chunks in " + (System.currentTimeMillis() - start) + "ms.");
				
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
		
		protected void save(boolean clear) {
			for (int i = 0; i < maps.length; i++) {
				maps[i].save(clear);
			}
		}
	}
}
