package btwmod.itemlogger;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;

import btwmods.IMod;
import btwmods.player.ContainerEvent;
import btwmods.player.DropEvent;
import btwmods.player.IContainerListener;
import btwmods.player.IDropListener;
import btwmods.player.ISlotListener;
import btwmods.player.SlotEvent;

public class PlayerListener implements ISlotListener, IDropListener, IContainerListener {

	private IMod mod;
	private Logger logger;
	
	public PlayerListener(mod_ItemLogger mod, Logger logger) {
		this.mod = mod;
		this.logger = logger;
	}

	@Override
	public IMod getMod() {
		return mod;
	}

	@Override
	public void containerAction(ContainerEvent event) {
		EntityPlayer player = event.getPlayer();
		if (event.getType() == ContainerEvent.TYPE.OPENED) {
			logger.log(Level.INFO, player.username + " at " + (int)player.posX + "/" + (int)player.posY + "/" + (int)player.posZ + " opened " + event.getBlock().getBlockName() + " at " + event.getX() + "/" + event.getY() + "/" + event.getZ(),
					new Object[] { "opened container", event });
		}
		else if (event.getType() == ContainerEvent.TYPE.REMOVED) {
			logger.log(Level.INFO, player.username + " at " + (int)player.posX + "/" + (int)player.posY + "/" + (int)player.posZ + " removed " + event.getBlock().getBlockName() + " at " + event.getX() + "/" + event.getY() + "/" + event.getZ(),
					new Object[] { "removed container", event });
		}
		else if (event.getType() == ContainerEvent.TYPE.PLACED) {
			
		}
	}

	@Override
	public void dropAction(DropEvent event) {
		// TODO: record how and where the player died if inventory is fully ejected.
	}

	@Override
	public void slotAction(SlotEvent event) {
		ItemStack withdrawn = null;
		int withdrawnQuantity = -1;
		
		ItemStack deposited = null;
		int depositedQuantity = -1;
		
		if (event.getType() == SlotEvent.TYPE.ADD) {
			if (event.slotIsContainer()) {
				deposited = event.getSlotItems();
				depositedQuantity = event.getQuantity();
			}
		}
		else if (event.getType() == SlotEvent.TYPE.REMOVE) {
			if (event.slotIsContainer()) {
				withdrawn = event.getHeldItems();
				withdrawnQuantity = event.getQuantity();
			}
		}
		else if (event.getType() == SlotEvent.TYPE.SWITCH) {
			if (event.slotIsContainer()) {
				withdrawn = event.getHeldItems();
				deposited = event.getSlotItems();
			}
			else {
				withdrawn = event.getSlotItems();
				deposited = event.getHeldItems();
			}
			
			withdrawnQuantity = withdrawn.stackSize;
			depositedQuantity = deposited.stackSize;
		}
		else if (event.getType() == SlotEvent.TYPE.TRANSFER) {
			if (event.getSlot().inventory instanceof InventoryPlayer) {
				deposited = event.getOriginalItems();
				depositedQuantity = event.getQuantity();
			}
			else {
				withdrawn = event.getOriginalItems();
				withdrawnQuantity = event.getQuantity();
			}
		}
		else {
			// TODO: use proper logging.
			logger.log(Level.SEVERE, "Unknown slotAction: " + event.getType().toString());
		}
		
		EntityPlayer player = event.getPlayer();
		
		if (withdrawn != null)
			logger.log(Level.INFO, player.username + " at " + (int)player.posX + "/" + (int)player.posY + "/" + (int)player.posZ + " withdrew " + withdrawnQuantity + " " + withdrawn.getItemName() + " from " + event.getContainer().getClass().getSimpleName() + " (" + event.getSlot().inventory.getInvName() + ")",
					new Object[] { "withdrawn", event, withdrawn, withdrawnQuantity });
		
		if (deposited != null)
			logger.log(Level.INFO, player.username + " at " + (int)player.posX + "/" + (int)player.posY + "/" + (int)player.posZ + " deposited " + depositedQuantity + " " + deposited.getItemName() + " into " + event.getContainer().getClass().getSimpleName() + " (" + event.getSlot().inventory.getInvName() + ")",
					new Object[] { "deposited", event, deposited, depositedQuantity });
	}
}
