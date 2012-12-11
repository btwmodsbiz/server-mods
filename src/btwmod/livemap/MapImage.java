package btwmod.livemap;

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
			heightImage = new BufferedImage(mapLayer.map.imageSize, mapLayer.map.imageSize, BufferedImage.TYPE_BYTE_GRAY);
		}
	}

	public void renderChunk(Chunk chunk) {
		int imageX = MathHelper.floor_float((float)chunk.xPosition / (float)mapLayer.chunksPerImage);
		int imageZ = MathHelper.floor_float((float)chunk.zPosition / (float)mapLayer.chunksPerImage);
		
		float pixelsPerChunk = (float)mapLayer.map.imageSize / (float)mapLayer.chunksPerImage;
		
		// Determine the chunk coordinates of the upper left chunk in the image.
		int imageChunkX = imageX * mapLayer.chunksPerImage;
		int imageChunkZ = imageZ * mapLayer.chunksPerImage;
		
		// Determine the offset of the chunk within the image.
		int chunkOffsetX = chunk.xPosition - imageChunkX;
		int chunkOffsetZ = chunk.zPosition - imageChunkZ;
		
		for (int posX = 0; posX < 16; posX++) {
			for (int posZ = 0; posZ < 16; posZ++) {
				int posY = Math.max(0, chunk.getHeightValue(posX, posZ));
				int blockId = chunk.getBlockID(posX, posY, posZ);
				
				while (blockId == 0 && posY > 0)
					blockId = chunk.getBlockID(posX, --posY, posZ);
					
				if (blockId > 0) {
					BlockColor color = mapLayer.map.blockColors[blockId];
					if (color != null) {
						int pixelX = MathHelper.floor_float((chunkOffsetX * pixelsPerChunk) + (posX * pixelsPerChunk / 16));
						int pixelZ = MathHelper.floor_float((chunkOffsetZ * pixelsPerChunk) + (posZ * pixelsPerChunk / 16));
						
						colorImage.setRGB(pixelX, pixelZ, color.asRGB());
						
						posY = PixelColor.getLuminance(posY);
						heightImage.setRGB(pixelX, pixelZ, 255 << 24 | posY << 16 | posY << 8 | posY);
					}
				}
			}
		}
	}

	protected void save() throws IOException {
		ImageIO.write(colorImage, "png", colorImageFile);
		ImageIO.write(heightImage, "png", heightImageFile);
	}
}
