package btwmod.itemlogger;

import net.minecraft.src.Block;

public class BlockInfo {
	public final Block block;
	public final int dimension;
	public final int x;
	public final int y;
	public final int z;
	
	public BlockInfo(Block block, int dimension, int x, int y, int z) {
		this.block = block;
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
