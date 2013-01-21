package btwmod.itemlogger;

import net.minecraft.src.Block;
import net.minecraft.src.Container;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import btwmods.io.Settings;
import btwmods.player.ContainerEvent;
import btwmods.player.DropEvent;
import btwmods.player.PlayerActionEvent;
import btwmods.player.PlayerBlockEvent;
import btwmods.player.PlayerInstanceEvent;
import btwmods.player.SlotEvent;
import btwmods.world.BlockEvent;

public interface ILogger {

	public void init(mod_ItemLogger mod, Settings settings);
	public void containerOpened(ContainerEvent event, EntityPlayer player, Block block, int dimension, int x, int y, int z);
	public void containerRemoved(ContainerEvent event, EntityPlayer player, Block block, int dimension, int x, int y, int z);
	public void withdrew(SlotEvent event, EntityPlayer player, ItemStack withdrawn, int withdrawnQuantity, Container container, IInventory inventory, BlockInfo lastContainerOpened);
	public void deposited(SlotEvent event, EntityPlayer player, ItemStack deposited, int depositedQuantity, Container container, IInventory inventory, BlockInfo lastContainerOpened);
	public void containerBroken(BlockEvent event, int dimension, int x, int y, int z, ItemStack[] contents);
	public void playerEdit(PlayerBlockEvent event, EntityPlayer player, int direction, int dimension, int x, int y, int z, ItemStack itemStack);
	public void playerRemove(PlayerBlockEvent event, EntityPlayer player, int dimension, int x, int y, int z);
	public void playerPosition(EntityPlayer player, int dimension, int x, int y, int z);
	public void playerLogin(PlayerInstanceEvent event, EntityPlayer player, int dimension, int x, int y, int z, boolean isLogout);
	public void playerDeath(PlayerInstanceEvent event, EntityPlayer player, int dimension, int x, int y, int z, String deathMessage);
	public void playerDropAll(DropEvent event, EntityPlayer player, int dimension, int x, int y, int z, InventoryPlayer inventory);
	public void playerDropItem(DropEvent event, EntityPlayer player, int dimension, int x, int y, int z, ItemStack itemStack);
	public void playerPickupItem(DropEvent event, EntityPlayer player, int dimension, int x, int y, int z, ItemStack itemStack);
	public void playerUseEntity(PlayerActionEvent event, EntityPlayer player, int dimension, int x, int y, int z, Entity entity, int entityX, int entityY, int entityZ, boolean isAttack);
	public void playerUseItem(PlayerActionEvent event, EntityPlayer player, int dimension, int x, int y, int z, ItemStack itemStack);
}
