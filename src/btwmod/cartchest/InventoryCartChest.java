package btwmod.cartchest;

import java.io.IOException;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryEnderChest;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;

public class InventoryCartChest extends InventoryEnderChest {
	
	private final mod_CartChest mod;
	private final EntityPlayer player;
	private boolean isInit = true;
	
	public InventoryCartChest(mod_CartChest mod, EntityPlayer player) {
		this.mod = mod;
		this.player = player;
		
		int count = mod.getStoredMinecarts(player.username);
		for (int i = 0; i < count && i < getSizeInventory(); i++) {
			setInventorySlotContents(i, new ItemStack(Item.minecartEmpty, 1));
		}
		
		isInit = false;
	}

	@Override
	public void onInventoryChanged() {
		if (isInit)
			return;
		
		int count = 0;
		for (int i = 0; i < getSizeInventory(); i++) {
			ItemStack itemStack = getStackInSlot(i);
			if (itemStack != null && itemStack.getItem() == Item.minecartEmpty)
				count++;
		}
		
		try {
			mod.setStoredMinecarts(player.username, count);
		} catch (IOException e) {
			// TODO: Alert ModLoader.
		}
		
		super.onInventoryChanged();
	}

	@Override
	public void closeChest() {
		for (int i = 0; i < getSizeInventory(); i++) {
			ItemStack itemStack = getStackInSlot(i);
			if (itemStack != null && itemStack.getItem() != Item.minecartEmpty) {
				player.dropPlayerItem(itemStack);
			}
		}
		
		super.closeChest();
	}
}
