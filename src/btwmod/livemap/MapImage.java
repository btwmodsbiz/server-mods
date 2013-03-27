package btwmod.livemap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import btwmods.ModLoader;
import net.minecraft.src.Chunk;
import net.minecraft.src.EnumSkyBlock;

public class MapImage {

	private final MapLayer mapLayer;
	
	public final int chunkX;
	public final int chunkZ;
	public final String chunkKey;
	
	private BufferedImage colorImage = null;
	private BufferedImage heightImage = null;
	
	public final File colorImageFile;
	public final File heightImageFile;
	
	private boolean requiresSave = true;
	
	public MapImage(MapLayer mapLayer, int chunkX, int chunkZ) throws Exception {
		this.mapLayer = mapLayer;
		
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		chunkKey = mapLayer.getChunkKey(chunkX, chunkZ);
		
		colorImageFile = new File(mapLayer.layerDirectory, chunkKey + ".png");
		heightImageFile = new File(mapLayer.layerDirectory, chunkKey + "h.png");
		
		setBlankImages();
	}
	
	public int getHeightAt(int x, int z) {
		return heightImage == null ? 0 : (heightImage.getRGB(x, z) >> 8 & 0x000000FF);
	}
	
	public boolean loadImages() throws Exception {
		boolean success = true;
		
		colorImage = heightImage = null;
		
		if (colorImageFile.exists() && (colorImage = ImageIO.read(colorImageFile)) == null)
			success = false;
		
		if (colorImage == null)
			colorImage = createBlank();
		
		if (mapLayer.map.hasHeightImage()) {
			if (heightImageFile.exists() && (heightImage = ImageIO.read(heightImageFile)) == null)
				success = false;
			
			if (heightImage == null)
				heightImage = createBlank();
		}
		
		requiresSave = success;
		
		return success;
	}
	
	public void setBlankImages() {
		requiresSave = true;
		colorImage = createBlank();
		
		if (mapLayer.map.hasHeightImage())
			heightImage = createBlank();
	}
	
	private BufferedImage createBlank() {
		return new BufferedImage(mapLayer.map.imageSize, mapLayer.map.imageSize, BufferedImage.TYPE_INT_ARGB);
	}
	
	public boolean isLoaded() {
		return colorImage != null && (!mapLayer.map.hasHeightImage() || heightImage != null);
	}
	
	public void drawPixel(int x, int z, PixelColor colorPixel, PixelColor heightPixel) {
		if (!isLoaded())
			return;
		
		colorImage.setRGB(x, z, colorPixel.asColor().getRGB());
		
		if (heightImage != null)
			heightImage.setRGB(x, z, heightPixel.asColor().getRGB());
	}

	public void renderChunk(Chunk chunk) {
		if (!isLoaded())
			return;
		
		requiresSave = true;
		
		MapPos pos = new MapPos(chunk, mapLayer);
		
		int increment = 16 / (mapLayer.map.imageSize / mapLayer.chunksPerImage);
		
		PixelColor colorPixel = new PixelColor();
		PixelColor heightPixel = new PixelColor();

		byte[] biomes = chunk.getBiomeArray();
		
		for (int xOffset = 0; xOffset < 16; xOffset += increment) {
			for (int zOffset = 0; zOffset < 16; zOffset += increment) {
				
				int pixelX = pos.getX(xOffset);
				int pixelZ = pos.getZ(zOffset);
				
				calculateColor(chunk, xOffset, zOffset, biomes, colorPixel, heightPixel);
				
				// Adjust brightness for height.
				//if (blockId != Block.lavaMoving.blockID && blockId != Block.lavaStill.blockID && blockId != Block.waterMoving.blockID && blockId != Block.waterStill.blockID)
				if (mapLayer.map.depthBrightness)
					depthBrightness(colorPixel, heightPixel);
				
				if (mapLayer.map.heightUndulate)
					heightUndulate(colorPixel, heightPixel, pixelX, pixelZ);
				
				drawPixel(pixelX, pixelZ, colorPixel, heightPixel);
				
				colorPixel.clear();
				heightPixel.clear();
			}
		}
	}
	
	@SuppressWarnings("static-method")
	protected void depthBrightness(PixelColor colorPixel, PixelColor heightPixel) {
		colorPixel.scale((Math.min(1.25F, Math.max(0.75F, getBlockHeight(heightPixel) / .65F / 100.0F))) / 8 * 8);
	}
	
	protected void heightUndulate(PixelColor colorPixel, PixelColor heightPixel, int xOffset, int zOffset) {
		float darken = 0.85F;
		float lighten = 1.06F;
		
		if (mapLayer.map.imageSize / mapLayer.chunksPerImage < 8)
			darken = 0.91F;
		
		else if (mapLayer.map.imageSize / mapLayer.chunksPerImage < 16)
			darken = 0.88F;
		
		// Compare to northern block
		int northPosZ = zOffset - 1;
		if (northPosZ >= 0) {
			int northHeight = getHeightAt(xOffset, northPosZ);
			if (northHeight > 0) {
				if (northHeight > getBlockHeight(heightPixel))
					colorPixel.scale(darken);
				else if (northHeight < getBlockHeight(heightPixel))
					colorPixel.scale(lighten);
			}
		}
		
		// Compare to eastern block
		int eastPosX = xOffset - 1;
		if (eastPosX >= 0) {
			int eastHeight = getHeightAt(eastPosX, zOffset);
			if (eastHeight > 0) {
				if (eastHeight > getBlockHeight(heightPixel))
					colorPixel.scale(darken);
				else if (eastHeight < getBlockHeight(heightPixel))
					colorPixel.scale(lighten);
			}
		}
	}
	
	protected void calculateColor(Chunk chunk, int x, int z, byte[] biomes, PixelColor colorPixel, PixelColor heightPixel) {
		float count = 0.0F;
		int red = 0;
		int green = 0;
		int blue = 0;
		float alpha = 0;
		int height = 0;
		int blockLight = 0;
		int biomeId = 255;
		int[] biomeIdCounts = new int[256];
		boolean nightMode = mapLayer.map.nightMode;
		
		Color baseColor = mapLayer.map.baseColor;
		
		colorPixel.clear();
		heightPixel.clear();

		int increment = 16 / (mapLayer.map.imageSize / mapLayer.chunksPerImage);
		
		for (int iX = 0; iX < increment; iX++) {
			for (int iZ = 0; iZ < increment; iZ++) {
				int posY = mapLayer.map.forcedMinHeight == mapLayer.map.forcedMaxHeight
					? mapLayer.map.forcedMinHeight
					: Math.min(mapLayer.map.forcedMaxHeight, Math.max(mapLayer.map.forcedMinHeight, chunk.getHeightValue(x + iX, z +iZ)));
				
				while (chunk.getBlockID(x + iX, posY, z + iZ) == 0 && posY > 0)
					posY--;
				
				// Get the stack of BlockColors for this exact block location.
				BlockColor[] stack = getBlockColorStack(chunk, x + iX, posY, z + iZ, biomes == null ? -1 : biomes[z << 4 | x] & 255);
				if (stack.length > 0) {
					count++;
					
					// Create a composite color from the BlockColor stack.
					PixelColor stackColor = new PixelColor();
					int lastNonClear = 0;
					for (int i = stack.length - 1; i >= 0; i--) {
						if (stack[i] != null && stack[i].alpha > 0.0F) {
							lastNonClear = i;
							
							stack[i].addTo(stackColor);
							if (biomes != null && biomes[z << 4 | x] != 255) {
								if (biomeId == 255 || biomeIdCounts[biomeId] < ++biomeIdCounts[biomes[z << 4 | x]]) {
									biomeId = biomes[z << 4 | x];
								}
							}
						}
					}
					
					// Add to the average color for the map pixel.
					red += stackColor.red * stackColor.alpha;
					green += stackColor.green * stackColor.alpha;
					blue += stackColor.blue * stackColor.alpha;
					alpha += stackColor.alpha;
					height += posY - lastNonClear;
					
					if (nightMode)
						blockLight += chunk.getSavedLightValue(EnumSkyBlock.Block, x + iX, posY - lastNonClear + 1, z +iZ);
				}
			}
		}
		
		if (count > 0.0F) {
			if (nightMode) {
				blockLight /= count;
				blockLight += 3;
				red *= blockLight / 26F;
				green *= blockLight / 26F;
				blue *= blockLight / 26F;
			}
			
			colorPixel.set(red / count, green / count, blue / count, alpha / count);
			heightPixel.set(new Color(0, Math.round(height / count), biomeId));
			
			// TODO: this is just here as a reminder. Need to put the baseColor _under_ the colorPixel.
			/*if (colorPixel.alpha < 1.0F && baseColor != null) {
				red = baseColor.getRed();
				green = baseColor.getGreen();
				blue = baseColor.getBlue();
				alpha = baseColor.getAlpha();
			}*/
			
			if (getBlockHeight(heightPixel) != Math.round(height / count))
				ModLoader.outputError("HeightPixel height " + getBlockHeight(heightPixel) + " (color: " + heightPixel.red + "," + heightPixel.green + "," + heightPixel.blue + ") doesn't match " + Math.round(height / count) + " for " + x + "," + z + " for chunk " + chunk.xPosition + "," + chunk.zPosition + " in layer " + mapLayer.chunksPerImage);
		}
		else {
			System.out.println("Unable to calc color for " + x + "," + z + " for chunk " + chunk.xPosition + "," + chunk.zPosition);
		}
	}
	
	protected BlockColor[] getBlockColorStack(Chunk chunk, int x, int y, int z, int biomeId) {
		List<BlockColor> colorStack = new ArrayList<BlockColor>();
		int[] alphaLimits = null;
		
		while (y >= 0) {
			int blockId = chunk.getBlockID(x, y, z);
			if (isRenderedBlock(blockId)) {
				BlockColor blockColor = BlockColor.fromBlockList(mapLayer.map.blockColors[blockId], blockId, biomeId, chunk, x, y, z);
				if (blockColor != null) {
					
					// Only create the array when needed.
					if (blockColor.alphaLimit > 0 && alphaLimits == null)
						alphaLimits = new int[mapLayer.map.blockColors.length];
					
					if (blockColor.alphaLimit <= 0 || ++alphaLimits[blockId] <= blockColor.alphaLimit) {
						colorStack.add(blockColor);
						
						// Stop once we hit a opaque block.
						if (!blockColor.isTransparent())
							break;
					}
					else if (blockColor.alphaLimit > 0 && blockColor.solidAfterAlphaLimit && alphaLimits[blockId] > blockColor.alphaLimit) {
						colorStack.add(blockColor.asSolid());
						break;
					}
				}
			}
			else {
				colorStack.add(null);
			}
			
			y--;
		}
		
		return colorStack.toArray(new BlockColor[colorStack.size()]);
	}
	
	protected int getHeightAt(Chunk chunk, int x, int z) {
		int y = Math.max(0, chunk.getHeightValue(x, z));
		int blockId = chunk.getBlockID(x, y, z);
		
		while (!isRenderedBlock(blockId) && y > 0)
			blockId = chunk.getBlockID(x, --y, z);
		
		return blockId > 0 ? y : -1;
	}
	
	protected boolean isRenderedBlock(int blockId) {
		return blockId > 0 && blockId < mapLayer.map.blockColors.length && mapLayer.map.blockColors[blockId] != null;
	}

	protected void save() throws IOException {
		if (!requiresSave)
			return;
		
		boolean success = true;
		if (ImageIO.write(colorImage, "png", mapLayer.map.mod.tempSave)) {
			if (!colorImageFile.exists() || colorImageFile.delete()) {
				if (mapLayer.map.mod.tempSave.renameTo(colorImageFile)) {
					colorImageFile.setReadable(true, false);
				}
				else {
					success = false;
					ModLoader.outputError(mapLayer.map.mod.getName() + "'s " + MapImage.class.getSimpleName() + " failed to move the temp image to: " + colorImageFile.getPath());
				}
			}
			else {
				success = false;
				ModLoader.outputError(mapLayer.map.mod.getName() + "'s " + MapImage.class.getSimpleName() + " failed to delete the old image at: " + colorImageFile.getPath());
			}
		}
		else {
			success = false;
			ModLoader.outputError(mapLayer.map.mod.getName() + "'s " + MapImage.class.getSimpleName() + " failed to save the color image to the temp file for " + chunkX + "," + chunkZ);
		}
		
		if (heightImage != null) {
			if (ImageIO.write(heightImage, "png", mapLayer.map.mod.tempSave)) {
				if (!heightImageFile.exists() || heightImageFile.delete()) {
					if (mapLayer.map.mod.tempSave.renameTo(heightImageFile)) {
						heightImageFile.setReadable(true, false);
					}
					else {
						success = false;
						ModLoader.outputError(mapLayer.map.mod.getName() + "'s " + MapImage.class.getSimpleName() + " failed to move the temp image to: " + heightImageFile.getPath());
					}
				}
				else {
					success = false;
					ModLoader.outputError(mapLayer.map.mod.getName() + "'s " + MapImage.class.getSimpleName() + " failed to delete the old image at: " + heightImageFile.getPath());
				}
			}
			else {
				success = false;
				ModLoader.outputError(mapLayer.map.mod.getName() + "'s " + MapImage.class.getSimpleName() + " failed to save the height image to the temp file for " + chunkX + "," + chunkZ);
			}
		}
		
		if (success)
			requiresSave = false;
	}
	
	public static int getBlockHeight(PixelColor color) {
		return Math.round(color.green);
	}
	
	public static int getBlockBiome(PixelColor color) {
		return Math.round(color.blue);
	}
}
