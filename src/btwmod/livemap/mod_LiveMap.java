package btwmod.livemap;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;

import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.world.ChunkEvent;
import btwmods.world.IChunkListener;

public class mod_LiveMap implements IMod, IChunkListener {

	private int imageSize = 256;
	private File imageDir = ModLoader.modDataDir;
	private File colorData = new File(ModLoader.modsDir, "livemap-colors.txt");

	private ConcurrentLinkedQueue<Chunk> chunkQueue = new ConcurrentLinkedQueue<Chunk>();
	private volatile ChunkProcessor chunkProcessor = null;

	public final BlockColor[] list = new BlockColor[Block.blocksList.length];

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
						} else if (list[block.blockID] != null) {
							ModLoader.outputError(getName() + " found duplicate colorData entries for: " + block.getBlockName(), Level.SEVERE);
							return false;
						} else {
							list[block.blockID] = color;
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

		// Set colors for blocks not set by the config.
		for (int i = 0; i < list.length; i++) {
			if (Block.blocksList[i] != null && list[i] == null) {
				list[i] = BlockColor.fromBlock(Block.blocksList[i]);
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
				new Thread(chunkProcessor = new ChunkProcessor()).start();
			}
		}
	}

	private class ChunkProcessor implements Runnable {
		private volatile boolean isRunning = true;
		private Map<String, BufferedImage> images = new LinkedHashMap<String, BufferedImage>();

		public boolean isRunning() {
			return isRunning;
		}

		@Override
		public void run() {
			while (this == chunkProcessor) {
				Chunk chunk = null;
				while ((chunk = chunkQueue.poll()) != null) {
					renderChunk(chunk, 16);
				}

				try {
					Thread.sleep(15L * 1000L);
				} catch (InterruptedException e) {

				}
			}

			isRunning = false;
		}

		public void renderChunk(Chunk chunk, int chunksPerImage) {
			int fileX = chunk.xPosition / chunksPerImage;
			int fileZ = chunk.zPosition / chunksPerImage;

			String key = fileX + "/" + fileZ;

			BufferedImage image = images.get(key);
			if (image == null) {
				try {
					image = getImage(fileX, fileZ);
				} catch (IOException e) {
					ModLoader.outputError(e, getName() + " failed to load the existing image at " + getFile(fileX, fileZ).getName());
					chunkQueue.add(chunk);
				}
			}

			if (image != null) {

			}
		}

		public void renderChunk(BufferedImage image, Chunk chunk, int chunksPerImage) {

		}
	}

	private BufferedImage getImage(int x, int z) throws IOException {
		return getImage(getFile(x, z));
	}

	private BufferedImage getImage(File imageFile) throws IOException {
		if (!imageFile.isFile()) {
			return new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
		} else {
			return ImageIO.read(imageFile);
		}
	}

	private File getFile(int x, int z) {
		return new File(imageDir, x + "_" + z + ".png");
	}
}
