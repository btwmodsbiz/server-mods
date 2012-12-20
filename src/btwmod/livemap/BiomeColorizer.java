package btwmod.livemap;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class BiomeColorizer {
	public enum TYPE { NONE, GRASS, FOLIAGE, WATER };
	
	private int[] grassColors = null;
	private int[] foliageColors = null;
	private int[] waterColors = null;

	public void loadGrassColors(File imageFile) throws Exception {
		BufferedImage image = ImageIO.read(imageFile);

		int width = image.getWidth();
		int height = image.getHeight();
		
		// TODO: Check that the size of the image is correct?
		
		int[] colors = new int[width * height];
		image.getRGB(0, 0, width, height, colors, 0, width);
		
		grassColors = colors;
	}

	public int getGrassColor(double temperature, double rainfall) {
		if (grassColors == null)
			return 0;
		
		rainfall *= temperature;
		int var4 = (int)((1.0D - temperature) * 255.0D);
		int var5 = (int)((1.0D - rainfall) * 255.0D);
		return grassColors[var5 << 8 | var4];
	}

	public void loadFoliageColors(File imageFile) throws Exception {
		BufferedImage image = ImageIO.read(imageFile);

		int width = image.getWidth();
		int height = image.getHeight();
		
		// TODO: Check that the size of the image is correct?
		
		int[] colors = new int[width * height];
		image.getRGB(0, 0, width, height, colors, 0, width);
		
		foliageColors = colors;
	}

	public int getFoliageColor(double temperature, double rainfall) {
		if (foliageColors == null)
			return 0;
		
		rainfall *= temperature;
		int var4 = (int)((1.0D - temperature) * 255.0D);
		int var5 = (int)((1.0D - rainfall) * 255.0D);
		return foliageColors[var5 << 8 | var4];
	}

	public void loadWaterColors(File imageFile) throws Exception {
		BufferedImage image = ImageIO.read(imageFile);

		int width = image.getWidth();
		int height = image.getHeight();
		
		// TODO: Check that the size of the image is correct?
		
		int[] colors = new int[width * height];
		image.getRGB(0, 0, width, height, colors, 0, width);
		
		waterColors = colors;
	}

	public int getWaterColor(double temperature, double rainfall) {
		if (waterColors == null)
			return 0;
		
		rainfall *= temperature;
		int var4 = (int)((1.0D - temperature) * 255.0D);
		int var5 = (int)((1.0D - rainfall) * 255.0D);
		return waterColors[var5 << 8 | var4];
	}
}
