package btwmod.itemlogger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.src.Enchantment;
import net.minecraft.src.EnchantmentHelper;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.ServerAPI;
import btwmods.WorldAPI;
import btwmods.io.Settings;

public class mod_ItemLogger implements IMod {
	
	private Settings data;
	private ILogger logger = null;
	private AsynchronousFileWriter fileWriter = new AsynchronousFileWriter(this);
	
	private PlayerListener playerListener;
	private WorldListener worldListener;
	private final Set<String> watchedPlayers = new HashSet<String>();
	
	public int locationTrackingFrequency = 20 * 5;
	
	private CommandWatch commandWatch;
	
	@Override
	public String getName() {
		return "Item Logger";
	}

	@Override
	public void init(Settings settings, Settings data) {
		this.data = data;
		
		if (data.hasKey("watchedPlayers")) {
			String[] players = data.get("watchedPlayers").split("[ ;,]");
			for (int i = 0; i < players.length; i++) {
				if (players[i].trim().length() > 0)
					watchedPlayers.add(players[i].toLowerCase().trim());
			}
		}
		
		locationTrackingFrequency = Math.max(20, settings.getInt("locationTrackingFrequency", locationTrackingFrequency));

		if (settings.hasKey("logger")) {
			if (settings.get("logger").equalsIgnoreCase("sql")) {
				logger = new SQLLogger();
			}
		}
		
		if (logger != null) {
			logger.init(this, settings);
			CommandsAPI.registerCommand(commandWatch = new CommandWatch(this), this);
			PlayerAPI.addListener(playerListener = new PlayerListener(this, logger));
			ServerAPI.addListener(playerListener);
			WorldAPI.addListener(worldListener = new WorldListener(this, logger));
		}
	}

	@Override
	public void unload() {
		if (logger != null) {
			CommandsAPI.unregisterCommand(commandWatch);
			ServerAPI.removeListener(playerListener);
			PlayerAPI.removeListener(playerListener);
			WorldAPI.removeListener(worldListener);
		}
	}

	public boolean isWatchedPlayer(EntityPlayer player) {
		return watchedPlayers.contains(player.username.toLowerCase());
	}
	
	public boolean addWatchedPlayer(String username) {
		boolean added = watchedPlayers.add(username.toLowerCase().trim());
		if (added)
			saveWatched();
		
		return added;
	}
	
	public boolean removeWatchedPlayer(String username) {
		boolean removed = watchedPlayers.remove(username.toLowerCase().trim());
		if (removed)
			saveWatched();
		
		return removed;
	}
	
	public String[] getWatchedPlayers() {
		return watchedPlayers.toArray(new String[watchedPlayers.size()]);
	}
	
	private void saveWatched() {
		StringBuilder usernames = new StringBuilder();
		for (String username : watchedPlayers) {
			if (usernames.length() > 0) usernames.append(";");
			usernames.append(username);
		}
		
		data.set("watchedPlayers", usernames.toString());
		
		try {
			data.saveSettings();
		} catch (IOException e) {
			
		}
	}
	
	public void queueWrite(File file, String line) {
		fileWriter.queueWrite(file, line);
	}
	
	public static String getItemStackList(ItemStack[] itemStacks) {
		StringBuilder sb = new StringBuilder();
		ItemStack[] groupedItemStacks = groupItemStacks(itemStacks);
		
		for (ItemStack itemStack : groupedItemStacks) {
			if (sb.length() > 0) sb.append("; ");
			sb.append(getFullItemStackName(itemStack));
		}
		
		return sb.toString();
	}
	
	public static ItemStack[] groupItemStacks(ItemStack[] itemStacks) {
		if (itemStacks.length == 0)
			return itemStacks;
		
		Map<String, ItemStack> groupedItemStacks = new HashMap<String, ItemStack>();
		
		for (int i = 0; i < itemStacks.length; i++) {
			if (itemStacks[i] != null) {
				String key = getUniqueItemStackName(itemStacks[i]);
				ItemStack stack = groupedItemStacks.get(key);
				
				if (stack == null)
					groupedItemStacks.put(key, itemStacks[i].copy());
				else
					stack.stackSize += itemStacks[i].stackSize;
			}
		}
		
		return groupedItemStacks.values().toArray(new ItemStack[groupedItemStacks.size()]);
	}
	
	public static String getUniqueItemStackName(ItemStack itemStack) {
		if (itemStack == null)
			return null;
		
		StringBuilder fullName = new StringBuilder(itemStack.getItemName());
		
		if (itemStack.isItemDamaged())
			fullName.append("#").append(itemStack.getItemDamage());
		
		if (itemStack.isItemEnchanted()) {
			fullName.append(" (");
			int enchant = 1;
			Map<Integer, Integer> enchants = EnchantmentHelper.getEnchantments(itemStack);
			for (Entry<Integer, Integer> entry : enchants.entrySet()) {
				if (enchant > 1) fullName.append(", ");
				Enchantment enchantment = Enchantment.enchantmentsList[entry.getKey().intValue()];
				if (enchantment != null) {
					fullName.append(enchantment.func_77316_c(entry.getValue().intValue()));
				}
				enchant++;
			}
			fullName.append(")");
		}
		
		return fullName.toString();
	}
	
	public static String getFullItemStackName(ItemStack itemStack) {
		if (itemStack == null)
			return null;
		
		return itemStack.stackSize + " x " + getUniqueItemStackName(itemStack);
	}
}
