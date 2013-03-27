package btwmod.livemap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import net.minecraft.src.Chunk;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ServerAPI;
import btwmods.WorldAPI;
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
	private Queue<Chunk> unloadedChunkQueue = new ConcurrentLinkedQueue<Chunk>();

	private RegionIterator regionQueue = new RegionIterator();
	
	private CommandMap commandMap = null;
	
	private MapManager[] mapManagers = null;

	@Override
	public String getName() {
		return "Live Map";
	}
	
	public boolean getAllowQueuing() {
		return allowQueuing;
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

		// Load block color data.
		if ((blockColors = MapUtil.loadColorData(colorData, this)) == null)
			return;
		
		// Set default colors for any not set by the data file.
		MapUtil.setColorDataDefaults(blockColors);
		
		if ((mapManagers = MapUtil.loadMapManagers(settings, this)) == null) {
			ModLoader.outputError(getName() + " does not have any valid maps specified.");
			return;
		}

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
	
	public int queueWorld(int worldIndex) throws NullPointerException, FileNotFoundException, IOException {
		if (!allowQueuing)
			return 0;
		
		int count = regionQueue.addWorld(worldIndex);
		checkThread();
		return count;
	}
	
	public void queueRegion(int worldIndex, int x, int z) throws NullPointerException, FileNotFoundException {
		if (!allowQueuing)
			return;
		
		regionQueue.addRegion(worldIndex, x, z);
		checkThread();
	}
	
	public void queueChunk(Chunk chunk) {
		if (!allowQueuing)
			return;
		
		unloadedChunkQueue.add(chunk);
		checkThread();
	}
	
	private void checkThread() {
		if (chunkProcessor == null || !chunkProcessor.isRunning()) {
			new Thread(chunkProcessor = new ChunkProcessor(mapManagers), getName() + " Thread").start();
		}
	}
	
	public void clearQueue() {
		unloadedChunkQueue.clear();
		regionQueue.clear();
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
				int debugCount = 0;
				long start = System.currentTimeMillis();
				long nextSave = System.currentTimeMillis() + (5 * 1000);
				long nextDebug = System.currentTimeMillis() + (10 * 1000);
				while (this == chunkProcessor && (chunk = getNextChunk()) != null) {
					renderChunk(chunk);
					count++;
					debugCount++;
					
					if (System.currentTimeMillis() > nextSave) {
						save(false);
						nextSave = System.currentTimeMillis() + (5 * 1000);
					}
					
					if (debugMessages && System.currentTimeMillis() > nextDebug) {
						ModLoader.outputInfo(getName() + " thread rendered " + debugCount + " chunks.");
						nextDebug = System.currentTimeMillis() + (10 * 1000);
						debugCount = 0;
					}
					
					try {
						Thread.sleep(10L);
					} catch (InterruptedException e) {

					}
				}

				save(true);

				if (debugMessages && count > 0)
					ModLoader.outputInfo(getName() + " thread rendered " + count + " total chunks in " + (System.currentTimeMillis() - start) + "ms.");
				
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {

				}
			}

			isRunning = false;
		}
		
		private Chunk getNextChunk() {
			Chunk chunk = unloadedChunkQueue.poll();
			if (chunk == null && regionQueue.hasNext()) {
				chunk = regionQueue.next();
			}
			return chunk;
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
