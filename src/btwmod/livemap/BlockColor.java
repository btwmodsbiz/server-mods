package btwmod.livemap;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.src.Block;

public class BlockColor {

	private static Map<String, Block> blockNameLookup = null;

	public static final float defaultAlpha = 1.0F;

	public static Block getBlockByName(String name) {
		if (blockNameLookup == null) {
			blockNameLookup = new HashMap<String, Block>();
			for (int i = 0; i < Block.blocksList.length; i++) {
				blockNameLookup.put(Block.blocksList[i].getBlockName(), Block.blocksList[i]);
			}
		}

		return blockNameLookup.get(name);
	}

	public final String blockName;
	public final float alpha;
	public final float red;
	public final float blue;
	public final float green;
	public final int metadata;
	public final boolean hasMetadata;

	public BlockColor(String blockName, float alpha, float red, float blue, float green) {
		this(blockName, alpha, red, blue, green, 0, true);
	}

	public BlockColor(String blockName, float alpha, float red, float blue, float green, int metadata) {
		this(blockName, alpha, red, blue, green, metadata, true);
	}

	private BlockColor(String blockName, float alpha, float red, float blue, float green, int metadata, boolean hasMetadata) {
		this.blockName = blockName;
		this.alpha = alpha;
		this.red = red;
		this.blue = blue;
		this.green = green;
		this.metadata = metadata;
		this.hasMetadata = hasMetadata;
	}

	public boolean isTransparent() {
		return alpha < 1.0F;
	}

	public void addTo(PixelColor color) {
		color.composite(red, green, blue, alpha);
	}
	
	public static BlockColor fromBlock(Block block) {
		if (block == null)
			return null;
		
		int mapColor = block.blockMaterial.materialMapColor.colorValue;
		float red = (float)(mapColor >> 16 & 255);
		float green = (float)(mapColor >> 8 & 255);
		float blue = (float)(mapColor & 255);
		
		return new BlockColor(block.getBlockName(), red, green, blue, BlockColor.defaultAlpha);
	}

	public static BlockColor fromConfigLine(String line) {
		if (line == null || line.trim().length() < 2 || line.trim().charAt(0) == '#')
			return null;

		String[] columns = line.split("[ \t]+");

		// Make sure the block name is not an empty string.
		if (columns.length > 0 && columns[0].length() == 0)
			return null;

		float red = 0.0F;
		float green = 0.0F;
		float blue = 0.0F;
		float alpha = 0.0F;
		int metadata = 0;
		boolean hasMetadata = false;

		for (int i = 1; i < columns.length; i++) {
			if (columns[i].equalsIgnoreCase("blockmaterial")) {
				Block block = getBlockByName(columns[0]);

				// Fail if the block does not exist.
				if (block == null)
					return null;

				int mapColor = block.blockMaterial.materialMapColor.colorValue;
				red = (float)(mapColor >> 16 & 255);
				green = (float)(mapColor >> 8 & 255);
				blue = (float)(mapColor & 255);

			} else if (columns[i].length() > 0) {
				try {
					switch (columns[i].charAt(0)) {
						case 'r':
							if (columns[i].charAt(1) == '+')
								red += Float.parseFloat(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								red -= Float.parseFloat(columns[i].substring(2));
							else
								red = Float.parseFloat(columns[i].substring(1));
							break;
						case 'g':
							if (columns[i].charAt(1) == '+')
								green += Float.parseFloat(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								green -= Float.parseFloat(columns[i].substring(2));
							else
								green = Float.parseFloat(columns[i].substring(1));
							break;
						case 'b':
							if (columns[i].charAt(1) == '+')
								blue += Float.parseFloat(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								blue -= Float.parseFloat(columns[i].substring(2));
							else
								blue = Float.parseFloat(columns[i].substring(1));
							break;
						case 'a':
							if (columns[i].charAt(1) == '+')
								alpha += Float.parseFloat(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								alpha -= Float.parseFloat(columns[i].substring(2));
							else
								alpha = Float.parseFloat(columns[i].substring(1));
							break;
						case '+':
							red += Math.max(0.0F, Math.min(255.0F, Float.parseFloat(columns[i].substring(1))));
							green += Math.max(0.0F, Math.min(255.0F, Float.parseFloat(columns[i].substring(1))));
							blue += Math.max(0.0F, Math.min(255.0F, Float.parseFloat(columns[i].substring(1))));
							break;
						case '-':
							red -= Math.max(0.0F, Math.min(255.0F, Float.parseFloat(columns[i].substring(1))));
							green -= Math.max(0.0F, Math.min(255.0F, Float.parseFloat(columns[i].substring(1))));
							blue -= Math.max(0.0F, Math.min(255.0F, Float.parseFloat(columns[i].substring(1))));
							break;
						case '#':
							red = (float)Integer.parseInt(columns[i].substring(1, 2), 16);
							green = (float)Integer.parseInt(columns[i].substring(3, 4), 16);
							blue = (float)Integer.parseInt(columns[i].substring(5, 6), 16);
							break;
						case 'm':
							metadata = Integer.parseInt(columns[i].substring(1));
							hasMetadata = true;
							break;
					}
				} catch (NumberFormatException e) {
					return null;
				} catch (IndexOutOfBoundsException e) {
					return null;
				}
			}
		}

		if (red < 0.0F || blue < 0.0F || green < 0.0F || alpha < 0.0F || red > 255.0F || blue > 255.0F || green > 255.0F || alpha > 1.0F)
			return null;

		return new BlockColor(columns[0], red, green, blue, alpha, metadata, hasMetadata);
	}
}
