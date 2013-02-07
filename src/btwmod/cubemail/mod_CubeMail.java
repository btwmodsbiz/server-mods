package btwmod.cubemail;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.Item;
import net.minecraft.src.ItemFood;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerBlockListener;
import btwmods.player.PlayerBlockEvent;

public class mod_CubeMail implements IMod, IPlayerBlockListener {
	
	public static final Pattern toLine = Pattern.compile("^(?:To|Dear) ([0-9a-z_]{1,16}),", Pattern.CASE_INSENSITIVE);
	
	private Settings data = null;

	@Override
	public String getName() {
		return "Cube Mail";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		this.data = data;
		PlayerAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerBlockAction(PlayerBlockEvent event) {
		if (event.getType() == PlayerBlockEvent.TYPE.ACTIVATION_ATTEMPT && event.getPlayer() instanceof EntityPlayerMP) {
			World world = event.getWorld();
			ItemStack heldStack = event.getPlayer().getHeldItem();
			if (world != null && heldStack != null) {
				Item held = heldStack.getItem();
				if (held != null && held instanceof ItemFood && ((ItemFood)held).isWolfsFavoriteMeat()) {
					
					if (processChest(event.getWorld(), event.getX(), event.getY() + 1, event.getZ())) {
						if (!((EntityPlayerMP)event.getPlayer()).theItemInWorldManager.isCreative()) {
							heldStack.stackSize--;
						}
						
						event.markHandled();
					}
				}
			}
		}
	}
	
	private boolean processChest(World world, int x, int y, int z) {
		if (world.getBlockId(x, y, z) == Block.chest.blockID) {
			TileEntityChest inventory = (TileEntityChest)world.getBlockTileEntity(x, y, z);
			if (inventory != null) {
				return processInventory(inventory);
			}
		}
		
		return false;
	}
	
	private boolean processInventory(TileEntityChest inventory) {
		boolean foundValidBook = false;
		
		for (int i = 0, count = inventory.getSizeInventory(); i < count; i++) {
			ItemStack book = inventory.getStackInSlot(i);
			//System.out.println(book == null ? "null" : book.getItemName());
			if (book != null && book.getItem() == Item.writtenBook) {
				BookData bookData = BookData.fromItemStack(book);
				if (bookData != null && sendBook(bookData)) {
					System.out.println("Sent book: " + bookData.title + " by " + bookData.author);
					foundValidBook = true;
					//inventory.setInventorySlotContents(i, null);
				}
			}
		}
		
		return foundValidBook;
	}
	
	private boolean sendBook(BookData bookData) {
		String section = "to_" + bookData.toUsername.toLowerCase();
		int nextID = data.getInt(section, "count", 0) + 1;
		
		try {
			data.set(section, "mail" + nextID, bookData.serialize());
			data.setInt(section, "count", nextID);
			data.saveSettings(this);
			return true;
			
		} catch (UnsupportedEncodingException e) {
			ModLoader.outputError(e, getName() + " failed to convert bookData to base64: " + e.getMessage());
		}
		
		return false;
	}
}
