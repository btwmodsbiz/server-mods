package btwmod.itemlogger;

import net.minecraft.src.BlockChest;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityVillager;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;

import btwmods.IMod;
import btwmods.entity.EntityEvent;
import btwmods.entity.IEntityListener;
import btwmods.world.BlockEvent;
import btwmods.world.IBlockListener;

public class WorldListener implements IBlockListener, IEntityListener {

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
				logger.containerBroken(event, event.getWorld().provider.dimensionId, event.getX(), event.getY(), event.getZ(), event.getContents());
			}
		}
	}

	@Override
	public void onEntityAction(EntityEvent event) {
		if (event.getType() == EntityEvent.TYPE.ATTACKED) {
			DamageSource source;
			if (event.getEntity() instanceof EntityVillager && (source = event.getDamageSource()) != null) {
				logger.entityAttacked(
					event,
					(EntityLiving)event.getEntity(),
					event.getEntity().dimension,
					MathHelper.floor_double(event.getEntity().posX), 
					MathHelper.floor_double(event.getEntity().posY),
					MathHelper.floor_double(event.getEntity().posZ),
					source
				);
			}
		}
	}

}
