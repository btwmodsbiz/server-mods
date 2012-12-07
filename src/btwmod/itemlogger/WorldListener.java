package btwmod.itemlogger;

import net.minecraft.src.BlockChest;
import net.minecraft.src.ItemStack;

import btwmods.IMod;
import btwmods.world.BlockEvent;
import btwmods.world.IBlockListener;

public class WorldListener implements IBlockListener {

	private mod_ItemLogger mod;
	private ILogger logger;

	public WorldListener(mod_ItemLogger mod, ILogger logger) {
		this.mod = mod;
		this.logger = logger;
	}

	@Override
	public IMod getMod() {
		return mod;
	}

	@Override
	public void onBlockAction(BlockEvent event) {
		if (logger == null)
			return;
		
		if (event.getType() == BlockEvent.TYPE.BROKEN && event.getBlock() instanceof BlockChest) {
			ItemStack[] contents = event.getContents();
			if (contents != null) {
				logger.containerBroken(event, event.getX(), event.getY(), event.getZ(), event.getContents());
			}
		}
	}

}
