package btwmod.livemap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.src.Chunk;
import net.minecraft.src.MathHelper;

public class MapImage {

	private final MapLayer mapLayer;
	
	public final int chunkX;
	public final int chunkZ;
	public final String chunkKey;
	
	private BufferedImage colorImage;
	private BufferedImage heightImage;
	
	public final File colorImageFile;
	public final File heightImageFile;
	
	public MapImage(MapLayer mapLayer, int chunkX, int chunkZ) throws IOException {
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
	
	public void loadImages() throws IOException {
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
		
		/*int imageX = MathHelper.floor_float((float)chunk.xPosition / (float)mapLayer.chunksPerImage);
		int imageZ = MathHelper.floor_float((float)chunk.zPosition / (float)mapLayer.chunksPerImage);
		
		float pixelsPerChunk = (float)mapLayer.map.imageSize / (float)mapLayer.chunksPerImage;
		
		// Determine the chunk coordinates of the upper left chunk in the image.
		int imageChunkX = imageX * mapLayer.chunksPerImage;
		int imageChunkZ = imageZ * mapLayer.chunksPerImage;
		
		// Determine the offset of the chunk within the image.
		int chunkOffsetX = chunk.xPosition - imageChunkX;
		int chunkOffsetZ = chunk.zPosition - imageChunkZ;*/
		
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
					//int x = heightImage.getRGB(pixelX + xOffset, pixelZ + northPosZ);
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
				
				/*int posY = Math.max(0, chunk.getHeightValue(posX, posZ));
				int blockId = chunk.getBlockID(posX, posY, posZ);
				
				while (!isRenderedBlock(blockId) && posY > 0)
					blockId = chunk.getBlockID(posX, --posY, posZ);
					
				if (blockId > 0) {
					BlockColor color = mapLayer.map.blockColors[blockId];
					if (color != null) {
						int pixelX = MathHelper.floor_float((chunkOffsetX * pixelsPerChunk) + (posX * pixelsPerChunk / 16));
						int pixelZ = MathHelper.floor_float((chunkOffsetZ * pixelsPerChunk) + (posZ * pixelsPerChunk / 16));
						
						PixelColor pixelColor = new PixelColor(color);
						
						// Adjust brightness for height.
						//if (blockId != Block.lavaMoving.blockID && blockId != Block.lavaStill.blockID && blockId != Block.waterMoving.blockID && blockId != Block.waterStill.blockID)
						pixelColor.scale((Math.min(1.25F, Math.max(0.75F, (float)(posY / .65) / 100.0F))) / 8 * 8);
						
						// Compare to northern block
						int northPosZ = posZ - 1;
						if (northPosZ >= 0) {
							int northHeight = getHeightAt(chunk, posX, northPosZ);
							if (northHeight > posY)
								pixelColor.scale(0.85F);
							else if (northHeight < posY)
								pixelColor.scale(1.06F);
						}
						
						// Compare to eastern block
						int eastPosX = posX - 1;
						if (eastPosX >= 0) {
							int eastHeight = getHeightAt(chunk, eastPosX, posZ);
							if (eastHeight > posY)
								pixelColor.scale(0.85F);
							else if (eastHeight < posY)
								pixelColor.scale(1.06F);
						}
						
						for (int iX = 0; iX < mapLayer.pixelSize; iX++) {
							for (int iZ = 0; iZ < mapLayer.pixelSize; iZ++) {
								colorImage.setRGB(pixelX + iX, pixelZ + iZ, pixelColor.asColor().getRGB());
								heightImage.setRGB(pixelX + iX, pixelZ + iZ, 255 << 24 | posY << 16 | posY << 8 | posY);
							}
						}
					}
				}*/
			}
		}
	}
	
	protected void calculateColor(Chunk chunk, int x, int z, PixelColor colorPixel, PixelColor heightPixel) {
		int increment = (int)Math.max(1.0F, 1.0F / mapLayer.pixelSize);
		int count = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		int height = 0;
		
		colorPixel.clear();
		heightPixel.clear();
		
		for (int iX = 0; iX < increment; iX++) {
			for (int iZ = 0; iZ < increment; iZ++) {
				int posY = Math.max(0, chunk.getHeightValue(x + iX, z + iZ));
				int blockId = chunk.getBlockID(x + iX, posY, z + iZ);
				
				while (!isRenderedBlock(blockId) && posY > 0)
					blockId = chunk.getBlockID(x + iX, --posY, z + iZ);
					
				if (blockId > 0) {
					count++;
					
					BlockColor blockColor = mapLayer.map.blockColors[blockId];
					if (blockColor != null) {
						red += blockColor.red;
						green += blockColor.green;
						blue += blockColor.blue;
						height += posY;
						if (iX == 0 && iZ == 0) {
							colorPixel.set(blockColor);
							//heightPixel.set((float)posY, (float)posY, (float)posY);
						}
						else {
							//color.composite(blockColor, 0.5F);
							//heightPixel.composite((float)posY, (float)posY, (float)posY, 0.5F);
						}
					}
				}
			}
		}
		
		if (count > 0) {
			colorPixel.set(red / count, green / count, blue / count);
			heightPixel.set(height / count, height / count, height / count);
		}
	}
	
	protected static int getHeightAt(Chunk chunk, int x, int z) {
		int y = Math.max(0, chunk.getHeightValue(x, z));
		int blockId = chunk.getBlockID(x, y, z);
		
		while (!isRenderedBlock(blockId) && y > 0)
			blockId = chunk.getBlockID(x, --y, z);
		
		return blockId > 0 ? y : -1;
	}
	
	protected static boolean isRenderedBlock(int blockId) {
		return blockId != 0; // && blockId != Block.snow.blockID;
	}

	protected void save() throws IOException {
		ImageIO.write(colorImage, "png", colorImageFile);
		ImageIO.write(heightImage, "png", heightImageFile);
	}
}
