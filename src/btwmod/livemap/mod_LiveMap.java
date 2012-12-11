package btwmod.livemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;

import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.world.ChunkEvent;
import btwmods.world.IChunkListener;

public class mod_LiveMap implements IMod, IChunkListener {

	private int[] zoomLevels = { 16 };
	private int imageSize = 256;
	private File imageDir = ModLoader.modDataDir;
	private File colorData = new File(ModLoader.modsDir, "livemap-colors.txt");

	private ConcurrentLinkedQueue<Chunk> chunkQueue = new ConcurrentLinkedQueue<Chunk>();
	private volatile ChunkProcessor chunkProcessor = null;

	public final BlockColor[] blockColors = new BlockColor[Block.blocksList.length];

	@Override
	public String getName() {
		return "Live Map";
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

		if (!loadColorData())
			return;

		WorldAPI.addListener(this);
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
	public void unload() throws Exception {
		WorldAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onChunkAction(ChunkEvent event) {
		if (event.getType() == ChunkEvent.TYPE.UNLOADED) {
			chunkQueue.add(event.getChunk());

			if (chunkProcessor == null || !chunkProcessor.isRunning()) {
				new Thread(chunkProcessor = new ChunkProcessor(new MapManager[] { new MapManager(this, 0, imageSize, zoomLevels, blockColors, new File(imageDir, "overworld")) })).start();
			}
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
				while ((chunk = chunkQueue.poll()) != null) {
					renderChunk(chunk);
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
