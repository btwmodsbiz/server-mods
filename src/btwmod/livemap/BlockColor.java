package btwmod.livemap;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.src.Block;

public class BlockColor {

	private static Map<String, Block> blockNameLookup = null;

	public static final int defaultAlpha = 255;

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
	public final int alpha;
	public final int red;
	public final int blue;
	public final int green;
	public final int metadata;
	public final boolean hasMetadata;

	public BlockColor(String blockName, int red, int green, int blue, int alpha) {
		this(blockName, red, green, blue, alpha, 0, true);
	}

	public BlockColor(String blockName, int red, int green, int blue, int alpha, int metadata) {
		this(blockName, red, green, blue, alpha, metadata, true);
	}

	private BlockColor(String blockName, int red, int green, int blue, int alpha, int metadata, boolean hasMetadata) {
		this.blockName = blockName;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
		this.metadata = metadata;
		this.hasMetadata = hasMetadata;
	}

	public boolean isTransparent() {
		return alpha < 255;
	}

	public void addTo(PixelColor color) {
		color.composite(red, green, blue, (float)alpha / 255);
	}
	
	public Color asColor(boolean withAlpha) {
		if (withAlpha)
			return new Color(red, green, blue, alpha);
		else
			return new Color(red, green, blue, 255);
	}
	
	public int asRGB() {
		return asColor(false).getRGB();
	}
	
	public int asRGBA() {
		return asColor(true).getRGB();
	}
	
	public static BlockColor fromBlock(Block block) {
		if (block == null)
			return null;
		
		int mapColor = block.blockMaterial.materialMapColor.colorValue;
		int red = mapColor >> 16 & 255;
		int green = mapColor >> 8 & 255;
		int blue = mapColor & 255;
		
		return new BlockColor(block.getBlockName(), red, green, blue, BlockColor.defaultAlpha);
	}

	public static BlockColor fromConfigLine(String line) {
		if (line == null || line.trim().length() < 2 || line.trim().charAt(0) == '#')
			return null;

		String[] columns = line.split("[ \t]+");

		// Make sure the block name is not an empty string.
		if (columns.length > 0 && columns[0].length() == 0)
			return null;

		int red = 0;
		int green = 0;
		int blue = 0;
		int alpha = 0;
		int metadata = 0;
		boolean hasMetadata = false;

		for (int i = 1; i < columns.length; i++) {
			if (columns[i].equalsIgnoreCase("blockmaterial")) {
				Block block = getBlockByName(columns[0]);

				// Fail if the block does not exist.
				if (block == null)
					return null;

				int mapColor = block.blockMaterial.materialMapColor.colorValue;
				red = mapColor >> 16 & 255;
				green = mapColor >> 8 & 255;
				blue = mapColor & 255;

			} else if (columns[i].length() > 0) {
				try {
					switch (columns[i].charAt(0)) {
						case 'r':
							if (columns[i].charAt(1) == '+')
								red += Integer.parseInt(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								red -= Integer.parseInt(columns[i].substring(2));
							else
								red = Integer.parseInt(columns[i].substring(1));
							break;
						case 'g':
							if (columns[i].charAt(1) == '+')
								green += Integer.parseInt(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								green -= Integer.parseInt(columns[i].substring(2));
							else
								green = Integer.parseInt(columns[i].substring(1));
							break;
						case 'b':
							if (columns[i].charAt(1) == '+')
								blue += Integer.parseInt(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								blue -= Integer.parseInt(columns[i].substring(2));
							else
								blue = Integer.parseInt(columns[i].substring(1));
							break;
						case 'a':
							if (columns[i].charAt(1) == '+')
								alpha += Integer.parseInt(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								alpha -= Integer.parseInt(columns[i].substring(2));
							else
								alpha = Integer.parseInt(columns[i].substring(1));
							break;
						case '+':
							red += Math.max(0, Math.min(255, Integer.parseInt(columns[i].substring(1))));
							green += Math.max(0, Math.min(255, Integer.parseInt(columns[i].substring(1))));
							blue += Math.max(0, Math.min(255, Integer.parseInt(columns[i].substring(1))));
							break;
						case '-':
							red -= Math.max(0, Math.min(255, Integer.parseInt(columns[i].substring(1))));
							green -= Math.max(0, Math.min(255, Integer.parseInt(columns[i].substring(1))));
							blue -= Math.max(0, Math.min(255, Integer.parseInt(columns[i].substring(1))));
							break;
						case '#':
							red = Integer.parseInt(columns[i].substring(1, 2), 16);
							green = Integer.parseInt(columns[i].substring(3, 4), 16);
							blue = Integer.parseInt(columns[i].substring(5, 6), 16);
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

		if (red < 0 || blue < 0 || green < 0 || alpha < 0 || red > 255 || blue > 255 || green > 255 || alpha > 255)
			return null;

		return new BlockColor(columns[0], red, green, blue, alpha, metadata, hasMetadata);
	}
}
