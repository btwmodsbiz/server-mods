package btwmod.cartchest;

import java.io.IOException;

import net.minecraft.src.mod_FCBetterThanWolves;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerBlockListener;
import btwmods.player.PlayerBlockEvent;

public class mod_CartChest implements IMod, IPlayerBlockListener {
	
	private Settings data = null;
	private int defaultStoredCount = 1;

	@Override
	public String getName() {
		return "Cart Chest";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		defaultStoredCount = settings.getInt("defaultStoredCount", defaultStoredCount);
		
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
		if (event.getType() == PlayerBlockEvent.TYPE.GET_ENDERCHEST_INVENTORY && event.getWorld().getBlockId(event.getX(), event.getY() - 1, event.getZ()) == mod_FCBetterThanWolves.fcSoulforgedSteelBlock.blockID) {
			event.setEnderChestInventory(new InventoryCartChest(this, event.getPlayer()));
		}
	}
	
	public int getStoredMinecarts(String username) {
		System.out.println("Get " + username + " to " + data.getInt(username.toLowerCase(), 1));
		return data.getInt(username.toLowerCase(), 1);
	}

	public void setStoredMinecarts(String username, int count) throws IOException {
		System.out.println("Set " + username + " to " + count);
		data.setInt(username.toLowerCase(), count);
		data.saveSettings();
	}

}
