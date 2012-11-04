package btwmod.spawnbeds;

import java.util.List;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.BlockBed;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.BlockEvent;
import btwmods.player.IBlockListener;
import btwmods.player.IInstanceListener;
import btwmods.player.InstanceEvent;

public class BTWModSpawnBeds implements IMod, IBlockListener, IInstanceListener {
	
	@Override
	public String getName() {
		return "Spawn Beds";
	}

	@Override
	public void init(Settings settings) {
		PlayerAPI.addListener(this);
	}

	@Override
	public void unload() {
		PlayerAPI.removeListener(this);
	}

	@Override
	public void blockAction(BlockEvent event) {
		if (event.getType() == BlockEvent.TYPE.ACTIVATED) {
			EntityPlayer player = event.getPlayer();
			if (event.getBlock() instanceof BlockBed && !player.worldObj.isRemote) {
				
				int metadata = event.getMetadata();
				int x = event.getX();
				int y = event.getY();
				int z = event.getZ();
				
				if (!BlockBed.isBlockHeadOfBed(metadata)) {
	                int var11 = BlockBed.getDirection(metadata);
	                x += BlockBed.footBlockToHeadBlockMap[var11][0];
	                z += BlockBed.footBlockToHeadBlockMap[var11][1];
	
	                if (event.getWorld().getBlockId(x, y, z) != event.getBlock().blockID)
	                {
	                    return;
	                }
	
	                metadata = event.getWorld().getBlockMetadata(x, y, z);
	            }
				
				if (!event.getWorld().provider.canRespawnHere()) {
	                double var19 = (double)x + 0.5D;
	                double var21 = (double)y + 0.5D;
	                double var15 = (double)z + 0.5D;
	                event.getWorld().setBlockWithNotify(x, y, z, 0);
	                int var17 = BlockBed.getDirection(metadata);
	                x += BlockBed.footBlockToHeadBlockMap[var17][0];
	                z += BlockBed.footBlockToHeadBlockMap[var17][1];
	
	                if (event.getWorld().getBlockId(x, y, z) == event.getBlock().blockID)
	                {
	                	event.getWorld().setBlockWithNotify(x, y, z, 0);
	                    var19 = (var19 + (double)x + 0.5D) / 2.0D;
	                    var21 = (var21 + (double)y + 0.5D) / 2.0D;
	                    var15 = (var15 + (double)z + 0.5D) / 2.0D;
	                }
	
	                event.getWorld().newExplosion((Entity)null, (double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), 5.0F, true);
	                return;
	            }
				
	            if (player.isPlayerSleeping() || !player.isEntityAlive()) {
	                return; // EnumStatus.OTHER_PROBLEM;
	            }
	
	            if (!player.worldObj.provider.isSurfaceWorld()) {
	                return; // EnumStatus.NOT_POSSIBLE_HERE;
	            }
	
	            if (player.worldObj.isDaytime()) {
	    			player.sendChatToPlayer("... it would be better to sleep at night.");
	                return; // EnumStatus.NOT_POSSIBLE_NOW;
	            }
	
	            if (Math.abs(player.posX - (double)event.getX()) > 3.0D || Math.abs(player.posY - (double)event.getY()) > 2.0D || Math.abs(player.posZ - (double)event.getZ()) > 3.0D) {
	    			player.sendChatToPlayer("... and that bed is too far away.");
	                return; // EnumStatus.TOO_FAR_AWAY;
	            }
	
	            double var4 = 8.0D;
	            double var6 = 5.0D;
	            List var8 = player.worldObj.getEntitiesWithinAABB(EntityMob.class, AxisAlignedBB.getAABBPool().addOrModifyAABBInPool((double)event.getX() - var4, (double)event.getY() - var6, (double)event.getZ() - var4, (double)event.getX() + var4, (double)event.getY() + var6, (double)event.getZ() + var4));
	
	            if (!var8.isEmpty()) {
	            	player.addChatMessage("tile.bed.notSafe");
	    			player.sendChatToPlayer("... there are monsters nearby!");
	                return; // EnumStatus.NOT_SAFE;
	            }
	
				player.setSpawnChunk(new ChunkCoordinates(event.getX(), event.getY(), event.getZ()));
				player.sendChatToPlayer("... but you feel as if this is your new home.");
	
		        //return EnumStatus.OK;
			}
		}
	}

	@Override
	public void instanceAction(InstanceEvent event) {
		if (event.getType() == InstanceEvent.TYPE.RESPAWN) {
			
		}
		else if (event.getType() == InstanceEvent.TYPE.WRITE_NBT) {
			//NBTTagCompound tagCompound = event.getNBTTagCompound();
			//tagCompound.setString("key", "value");
		}
	}

	@Override
	public IMod getMod() {
		return this;
	}
}
