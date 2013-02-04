package btwmod.cubemail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.Item;
import net.minecraft.src.ItemEditableBook;
import net.minecraft.src.ItemFood;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.NBTTagString;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerBlockListener;
import btwmods.player.PlayerBlockEvent;

public class mod_CubeMail implements IMod, IPlayerBlockListener {
	
	public static final Pattern toLine = Pattern.compile("^(?:To|Dear) ([0-9a-z_]{1,16}),", Pattern.CASE_INSENSITIVE);

	@Override
	public String getName() {
		return "Cube Mail";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		PlayerAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onPlayerBlockAction(PlayerBlockEvent event) {
		if (event.getType() == PlayerBlockEvent.TYPE.ACTIVATION_ATTEMPT && event.getPlayer() instanceof EntityPlayerMP) {
			World world = event.getWorld();
			ItemStack heldStack = event.getPlayer().getHeldItem();
			if (world != null && heldStack != null) {
				Item held = heldStack.getItem();
				if (held != null && held instanceof ItemFood && ((ItemFood)held).isWolfsFavoriteMeat()) {
					int above = event.getWorld().getBlockId(event.getX(), event.getY() + 1, event.getZ());
					if (above == Block.chest.blockID) {
						TileEntityChest inventory = (TileEntityChest)world.getBlockTileEntity(event.getX(), event.getY() + 1, event.getZ());
						if (inventory != null) {
							boolean foundValidBook = true;
							
							for (int i = 0, count = inventory.getSizeInventory(); i < count; i++) {
								ItemStack book = inventory.getStackInSlot(i);
								if (book != null && book.getItem() == Item.writtenBook) {
									NBTTagCompound tagCompound = book.getTagCompound();
									if (ItemEditableBook.validBookTagContents(tagCompound)) {
										NBTTagString title = (NBTTagString)tagCompound.getTag("title");
										NBTTagString author = (NBTTagString)tagCompound.getTag("author");
										NBTTagList pages = (NBTTagList)tagCompound.getTag("pages");
										if (pages.tagCount() > 0) {
											NBTTagString firstPage = (NBTTagString)pages.tagAt(0);
											if (firstPage != null && firstPage.data != null) {
												Matcher matcher = toLine.matcher(firstPage.data.trim());
												if (matcher.matches()) {
													String username = matcher.group(1);
													if (username != null) {
														foundValidBook = true;
														inventory.setInventorySlotContents(i, null);
														
														// TODO: Send mail.
													}
												}
											}
										}
									}
								}
							}
							
							if (foundValidBook) {
								if (!((EntityPlayerMP)event.getPlayer()).theItemInWorldManager.isCreative()) {
									heldStack.stackSize--;
								}
								
								event.markHandled();
							}
						}
					}
				}
			}
		}
	}
	
	private boolean processChest(World world, int x, int y, int z) {
		if (world.getBlockId(x, y + 1, z) == Block.chest.blockID) {
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
			if (book != null && book.getItem() == Item.writtenBook) {
				String toUsername = getUsernameFromBook(book);
				if (toUsername != null) {
					foundValidBook = true;
				}
			}
		}
		
		return foundValidBook;
	}
	
	private static String getUsernameFromBook(ItemStack book) {
		NBTTagCompound tagCompound = book.getTagCompound();
		if (ItemEditableBook.validBookTagContents(tagCompound)) {
			//NBTTagString title = (NBTTagString)tagCompound.getTag("title");
			//NBTTagString author = (NBTTagString)tagCompound.getTag("author");
			NBTTagList pages = (NBTTagList)tagCompound.getTag("pages");
			if (pages.tagCount() > 0) {
				NBTTagString firstPage = (NBTTagString)pages.tagAt(0);
				if (firstPage != null && firstPage.data != null) {
					Matcher matcher = toLine.matcher(firstPage.data.trim());
					if (matcher.matches()) {
						return matcher.group(1);
					}
				}
			}
		}
		
		return null;
	}
	
	private void sendBook(String username, ItemStack book) {
		
	}
}
