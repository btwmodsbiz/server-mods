package btwmod.livemap;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.src.Block;

public class BlockColor {

	private static Map<String, Set<Block>> blockNameLookup = null;

	public static final float opaque = 1.0F;
	public static final int noAlphaLimit = 0;

	public static Set<Block> getBlocksByName(String name) {
		if (blockNameLookup == null) {
			blockNameLookup = new HashMap<String, Set<Block>>();
			
			for (int i = 0; i < Block.blocksList.length; i++) {
				if (Block.blocksList[i] != null) {
					Set<Block> blocks = blockNameLookup.get(Block.blocksList[i].getBlockName());
					if (blocks == null)
						blockNameLookup.put(Block.blocksList[i].getBlockName(), blocks = new LinkedHashSet<Block>());
					
					blocks.add(Block.blocksList[i]);
				}
			}
		}

		return blockNameLookup.get(name);
	}

	public final String blockName;
	
	public final int red;
	public final int blue;
	public final int green;
	
	public final int biomeId;
	
	public final float alpha;
	public final int alphaLimit;
	public final boolean solidAfterAlphaLimit;
	
	public final int metadata;
	public final boolean hasMetadata;
	
	public final float hue;
	public final float hueAlpha;
	
	private BlockColor asSolid = null;

	public BlockColor(String blockName, int red, int green, int blue, float alpha, int alphaLimit, boolean solidAfterAlphaLimit, int biomeId) {
		this(blockName, red, green, blue, alpha, alphaLimit, solidAfterAlphaLimit, 0.0F, 0, true, biomeId);
	}

	public BlockColor(String blockName, int red, int green, int blue, float alpha, int alphaLimit, boolean solidAfterAlphaLimit, float hueAlpha, int biomeId) {
		this(blockName, red, green, blue, alpha, alphaLimit, solidAfterAlphaLimit, hueAlpha, 0, true, biomeId);
	}

	public BlockColor(String blockName, int red, int green, int blue, float alpha, int alphaLimit, boolean solidAfterAlphaLimit, int metadata, int biomeId) {
		this(blockName, red, green, blue, alpha, alphaLimit, solidAfterAlphaLimit, 0.0F, metadata, true, biomeId);
	}

	public BlockColor(String blockName, int red, int green, int blue, float alpha, int alphaLimit, boolean solidAfterAlphaLimit, float hueAlpha, int metadata, int biomeId) {
		this(blockName, red, green, blue, alpha, alphaLimit, solidAfterAlphaLimit, hueAlpha, metadata, true, biomeId);
	}

	private BlockColor(String blockName, int red, int green, int blue, float alpha, int alphaLimit, boolean solidAfterAlphaLimit, float hueAlpha, int metadata, boolean hasMetadata, int biomeId) {
		this.blockName = blockName;
		
		this.red = red;
		this.green = green;
		this.blue = blue;
		
		this.biomeId = biomeId;
		
		this.alpha = alpha;
		this.alphaLimit = alphaLimit;
		this.solidAfterAlphaLimit = solidAfterAlphaLimit;
		
		this.hue = Color.RGBtoHSB(red, green, blue, null)[0];
		this.hueAlpha = hueAlpha;
		
		this.metadata = metadata;
		this.hasMetadata = hasMetadata;
		
		if (red < 0 || blue < 0 || green < 0 || alpha < 0.0F
				|| red > 255 || blue > 255 || green > 255 || alpha > 1.0F
				|| hueAlpha < 0.0F || hueAlpha > 1.0F)
			
			throw new IllegalArgumentException();
	}
	
	public BlockColor asSolid() {
		if (!isTransparent())
			return this;
		
		if (asSolid == null)
			asSolid = new BlockColor(blockName, red, green, blue, 1.0F, 0, false, 0.0F, metadata, hasMetadata, biomeId);
		
		return asSolid;
	}

	public boolean isTransparent() {
		return alpha < 1.0F;
	}

	public void addTo(PixelColor color) {
		addTo(color, alpha);
	}

	public void addTo(PixelColor color, float alpha) {
		if (hueAlpha > 0.0F)
			color.hue(hue, hueAlpha);
		
		color.composite(red, green, blue, alpha);
	}
	
	public Color asColor(boolean withAlpha) {
		if (withAlpha)
			return new Color(red, green, blue, alpha * 255);
		else
			return new Color(red, green, blue, 255);
	}
	
	public int asRGB() {
		return asColor(false).getRGB();
	}
	
	public int asRGBA() {
		return asColor(true).getRGB();
	}
	
	public static float clamp_float(float par0, float par1, float par2) {
		return par0 < par1 ? par1 : (par0 > par2 ? par2 : par0);
	}
	
	public static BlockColor fromBlock(Block block) {
		if (block == null)
			return null;
		
		int mapColor = block.blockMaterial.materialMapColor.colorValue;
		int red = mapColor >> 16 & 255;
		int green = mapColor >> 8 & 255;
		int blue = mapColor & 255;
		
		return new BlockColor(block.getBlockName(), red, green, blue, BlockColor.opaque, BlockColor.noAlphaLimit, true, -1);
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
		
		int biomeId = -1;
		
		float alpha = opaque;
		int alphaLimit = noAlphaLimit;
		boolean solidAfterAlphaLimit = false;
		
		float hueAlpha = 0.0F;
		
		int metadata = 0;
		boolean hasMetadata = false;

		for (int i = 1; i < columns.length; i++) {
			if (columns[i].equalsIgnoreCase("blockmaterial")) {
				Set<Block> blocks = getBlocksByName(columns[0]);

				// Fail if the block name wasn't found.
				if (blocks == null || blocks.size() == 0)
					return null;

				// TODO: OK that we're just using the first material for the block name?
				for (Block block : blocks) {
					int mapColor = block.blockMaterial.materialMapColor.colorValue;
					red = mapColor >> 16 & 255;
					green = mapColor >> 8 & 255;
					blue = mapColor & 255;
					break;
				}

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
							// Reset this in case alpha is specified twice by accident.
							solidAfterAlphaLimit = false;

							int marker = columns[i].indexOf(':');
							if (marker >= 0) {
								
								// Check for the solid after alpha limit flag.
								if (columns[i].charAt(columns[i].length() - 1) == 's') {
									solidAfterAlphaLimit = true;
									columns[i] = columns[i].substring(0, columns[i].length() - 1);
								}
								
								alphaLimit = Integer.parseInt(columns[i].substring(marker + 1));
								columns[i] = columns[i].substring(0, marker);
							}
							
							if (columns[i].charAt(1) == '+')
								alpha += Float.parseFloat(columns[i].substring(2));
							else if (columns[i].charAt(1) == '-')
								alpha -= Float.parseFloat(columns[i].substring(2));
							else
								alpha = Float.parseFloat(columns[i].substring(1));
							break;
						case 'h':
							if (columns[i].substring(1).indexOf('.') > 0)
								hueAlpha = Float.parseFloat(columns[i].substring(1));
							else
								hueAlpha = (float)Integer.parseInt(columns[i].substring(1)) / 360.0F;
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
						case 'i':
							biomeId = Integer.parseInt(columns[i].substring(1));
							break;
					}
				} catch (NumberFormatException e) {
					return null;
				} catch (IndexOutOfBoundsException e) {
					return null;
				}
			}
		}

		try {
			return new BlockColor(columns[0], red, green, blue, alpha, alphaLimit, solidAfterAlphaLimit, hueAlpha, metadata, hasMetadata, biomeId);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
}
