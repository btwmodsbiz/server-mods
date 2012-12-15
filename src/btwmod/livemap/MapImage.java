package btwmod.livemap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;

public class MapImage {

	private final MapLayer mapLayer;
	
	public final int chunkX;
	public final int chunkZ;
	public final String chunkKey;
	
	private BufferedImage colorImage;
	private BufferedImage heightImage;
	
	public final File colorImageFile;
	public final File heightImageFile;
	
	public MapImage(MapLayer mapLayer, int chunkX, int chunkZ) throws Exception {
		this.mapLayer = mapLayer;
		
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		chunkKey = mapLayer.getChunkKey(chunkX, chunkZ);
		
		colorImageFile = new File(mapLayer.layerDirectory, chunkKey + ".png");
		heightImageFile = new File(mapLayer.layerDirectory, chunkKey + "h.png");
		
		loadImages();
	}
	
	public int getHeightAt(int x, int z) {
		return heightImage.getRGB(x, z) & 0xFF;
	}
	
	public void loadImages() throws Exception {
		if (colorImageFile.exists()) {
			colorImage = ImageIO.read(colorImageFile);
		} else {
			colorImage = new BufferedImage(mapLayer.map.imageSize, mapLayer.map.imageSize, BufferedImage.TYPE_INT_ARGB);
		}

		if (heightImageFile.exists()) {
			heightImage = ImageIO.read(heightImageFile);
		} else {
			heightImage = new BufferedImage(mapLayer.map.imageSize, mapLayer.map.imageSize, BufferedImage.TYPE_INT_ARGB);
		}
	}
	
	public void drawPixels(int x, int z, PixelColor colorPixel, PixelColor heightPixel) {
		for (int iX = 0; iX < mapLayer.pixelSize; iX++) {
			for (int iZ = 0; iZ < mapLayer.pixelSize; iZ++) {
				colorImage.setRGB(x + iX, z + iZ, colorPixel.asColor().getRGB());
				heightImage.setRGB(x + iX, z + iZ, heightPixel.asColor().getRGB());
			}
		}
	}

	public void renderChunk(Chunk chunk) {
		MapPos pos = new MapPos(chunk, mapLayer);
		
		int increment = (int)Math.max(1.0F, 1.0F / mapLayer.pixelSize);
		
		PixelColor colorPixel = new PixelColor();
		PixelColor heightPixel = new PixelColor();
		
		for (int xOffset = 0; xOffset < 16; xOffset += increment) {
			for (int zOffset = 0; zOffset < 16; zOffset += increment) {
				
				int pixelX = pos.getX(xOffset);
				int pixelZ = pos.getZ(zOffset);
				
				calculateColor(chunk, xOffset, zOffset, colorPixel, heightPixel);
				
				// Adjust brightness for height.
				//if (blockId != Block.lavaMoving.blockID && blockId != Block.lavaStill.blockID && blockId != Block.waterMoving.blockID && blockId != Block.waterStill.blockID)
				colorPixel.scale((Math.min(1.25F, Math.max(0.75F, (float)(heightPixel.red / .65) / 100.0F))) / 8 * 8);
				
				float darken = 0.85F;
				float ligthen = 1.06F;
				
				if (mapLayer.pixelSize < 1)
					darken = 0.93F;
				
				// Compare to northern block
				int northPosZ = zOffset - 1;
				if (northPosZ >= 0) {
					//if (mapLayer.pixelSize < 1)
					//	colorPixel.composite(new Color(colorImage.getRGB(posX, northPosZ)), 0.25F);
					
					int northHeight = getHeightAt(chunk, xOffset, northPosZ);
					//int x = heightImage.getRGB(pixelX, pixelZ - 1);
					//if (x > 0)
					//	Integer.toString(x);
					if (northHeight > heightPixel.red)
						colorPixel.scale(darken);
					else if (northHeight < heightPixel.red)
						colorPixel.scale(ligthen);
				}
				
				// Compare to eastern block
				int eastPosX = xOffset - 1;
				if (eastPosX >= 0) {
					int eastHeight = getHeightAt(chunk, eastPosX, zOffset);
					if (eastHeight > heightPixel.red)
						colorPixel.scale(darken);
					else if (eastHeight < heightPixel.red)
						colorPixel.scale(ligthen);
				}
				
				drawPixels(pixelX, pixelZ, colorPixel, heightPixel);
				
				colorPixel.clear();
				heightPixel.clear();
			}
		}
	}
	
	protected void calculateColor(Chunk chunk, int x, int z, PixelColor colorPixel, PixelColor heightPixel) {
		float count = 0.0F;
		int red = 0;
		int green = 0;
		int blue = 0;
		float alpha = 0;
		int height = 0;
		
		colorPixel.clear();
		heightPixel.clear();
		
		BlockColor[] stack;

		int increment = (int)Math.max(1.0F, 1.0F / mapLayer.pixelSize);
		
		for (int iX = 0; iX < increment; iX++) {
			for (int iZ = 0; iZ < increment; iZ++) {
				int posY = Math.max(0, chunk.getHeightValue(x, z));
				
				while (chunk.getBlockID(x + iX, posY, z + iZ) == 0 && posY > 0)
					posY--;
				
				// Determine the color for the exact block location.
				stack = getBlockColorStack(chunk, x + iX, posY, z + iZ);
				if (stack.length > 0) {
					count++;
					
					PixelColor stackColor = new PixelColor();
					for (int i = stack.length - 1; i >= 0; i--) {
						stackColor.composite(stack[i]);
					}
					
					// Add to the average color for the map pixel.
					red += stackColor.red * stackColor.alpha;
					green += stackColor.green * stackColor.alpha;
					blue += stackColor.blue * stackColor.alpha;
					alpha += stackColor.alpha;
					height += posY;
				}
			}
		}
		
		if (count > 0.0F) {
			colorPixel.set(red / count, green / count, blue / count, alpha);
			heightPixel.set(height / count, height / count, height / count);
		}
		else {
			System.out.println("Unable to calc color for " + x + "," + z + " for chunk " + chunk.xPosition + "," + chunk.zPosition);
		}
	}
	
	protected BlockColor[] getBlockColorStack(Chunk chunk, int x, int y, int z) {
		List<BlockColor> colorStack = new ArrayList<BlockColor>();
		int waterBlocks = 0;
		
		while (y >= 0) {
			int blockId = chunk.getBlockID(x, y, z);
			if (isRenderedBlock(blockId)) {
				BlockColor blockColor = mapLayer.map.blockColors[blockId];
				if (blockColor != null) {
					
					// Only allow up to 5 water blocks.
					if ((Block.waterMoving.blockID != blockId && Block.waterStill.blockID != blockId) || ++waterBlocks <= 5) {
						colorStack.add(blockColor);
						
						// Stop once we hit a opaque block.
						if (!blockColor.isTransparent())
							break;
					}
				}
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
		
		if (blockId > 0) {
			BlockColor blockColor = mapLayer.map.blockColors[blockId];
			if (blockColor == null || (blockColor.red == 0 && blockColor.green == 0 && blockColor.blue == 0)) {
				return false;
			}
		}
		
		return blockId != 0; // && blockId != Block.snow.blockID;
	}

	protected void save() throws Exception {
		ImageIO.write(colorImage, "png", colorImageFile);
		ImageIO.write(heightImage, "png", heightImageFile);
	}
}
