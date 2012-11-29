package btwmod.protectedzones;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityMooshroom;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityVillager;
import net.minecraft.src.ICommand;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ServerCommandManager;
import net.minecraft.src.World;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.Util;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerActionListener;
import btwmods.player.PlayerActionEvent;
import btwmods.player.PlayerBlockEvent;
import btwmods.player.IPlayerBlockListener;
import btwmods.util.Area;
import btwmods.world.BlockEvent;
import btwmods.world.EntityEvent;
import btwmods.world.IBlockListener;
import btwmods.world.IEntityListener;

public class mod_ProtectedZones implements IMod, IPlayerBlockListener, IBlockListener, IPlayerActionListener, IEntityListener {
	
	public enum ACTION { PLACE, DIG, BROKEN, ACTIVATE, EXPLODE, ATTACK_ENTITY, USE_ENTITY, CHECK_PLAYER_EDIT, IS_ENTITY_INVULNERABLE };
	private Map<String, Area<ZoneSettings>> areasByName = new TreeMap<String, Area<ZoneSettings>>();
	private ProtectedZones[] zones;
	
	private MinecraftServer server;
	private ServerCommandManager commandManager;
	
	private Set<ICommand> commands = new LinkedHashSet<ICommand>();
	
	private Settings data;
	
	private Set ops;
	
	private boolean alwaysAllowOps = true;

	@Override
	public String getName() {
		return "Protected Zones";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		server = MinecraftServer.getServer();
		commandManager = (ServerCommandManager)server.getCommandManager();
		
		PlayerAPI.addListener(this);
		WorldAPI.addListener(this);
		registerCommand(new CommandZoneAdd(this));
		registerCommand(new CommandZoneDelete(this));
		registerCommand(new CommandZoneList(this));
		registerCommand(new CommandZoneSet(this));
		registerCommand(new CommandZonePlayer(this));
		registerCommand(new CommandZoneInfo(this));
		
		alwaysAllowOps = settings.getBoolean("alwaysAllowOps", alwaysAllowOps);
		
		ops = server.getConfigurationManager().getOps();
		
		this.data = data;
		
		zones = new ProtectedZones[server.worldServers.length];
		for (int i = 0; i < server.worldServers.length; i++) {
			zones[i] = new ProtectedZones();
		}
		
		int zoneCount = data.getInt("count", 0);
		for (int i = 1; i <= zoneCount; i++) {
			if (data.hasSection("zone" + i) && !add(new ZoneSettings(data.getSectionAsSettings("zone" + i)), false)) {
				ModLoader.outputError(getName() + " failed to load zone " + i + " as it has a duplicate name or has invalid dimensions.");
			}
		}
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
		WorldAPI.removeListener(this);
		unregisterCommands();
	}

	@Override
	public IMod getMod() {
		return this;
	}
	
	private void registerCommand(ICommand command) {
		commands.add(command);
		CommandsAPI.registerCommand(command, this);
	}
	
	private void unregisterCommands() {
		for (ICommand command : commands) {
			CommandsAPI.unregisterCommand(command);
		}
	}
	
	public boolean add(ZoneSettings zoneSettings) {
		return add(zoneSettings, true);
	}
	
	private boolean add(ZoneSettings zoneSettings, boolean doSave) {
		if (zoneSettings != null && zoneSettings.isValid() && !areasByName.containsKey(zoneSettings.name.toLowerCase())) {
			Area<ZoneSettings> area = zoneSettings.toArea();
			areasByName.put(zoneSettings.name.toLowerCase(), area);
			zones[Util.getWorldIndexFromDimension(zoneSettings.dimension)].add(area);
			
			if (doSave)
				saveAreas();
			
			return true;
		}
		return false;
	}
	
	public boolean remove(String name) {
		if (name != null) {
			Area<ZoneSettings> area = areasByName.get(name.toLowerCase());
			if (area != null) {
				areasByName.remove(name.toLowerCase());
				zones[Util.getWorldIndexFromDimension(area.data.dimension)].remove(area);
				saveAreas();
				return true;
			}
		}
		
		return false;
	}
	
	public Area<ZoneSettings> get(String name) {
		return areasByName.get(name.toLowerCase());
	}
	
	public List<String> getZoneNames() {
		ArrayList names = new ArrayList(areasByName.keySet());
		Collections.sort(names);
		return names;
	}
	
	public static boolean isProtectedEntityType(ACTION action, Entity entity) {
		if (entity instanceof EntityLiving) {
			
			if (entity instanceof EntityVillager && action != ACTION.USE_ENTITY)
				return true;
			
			if (entity instanceof EntityMooshroom && action != ACTION.USE_ENTITY)
				return true;
			
			return false;
		}
		else if (entity instanceof EntityItem) {
			return false;
		}
		
		return true;
	}
	
	public boolean isPlayerGloballyAllowed(String username) {
		return alwaysAllowOps && ops.contains(username.trim().toLowerCase());
	}
	
	protected boolean isPlayerZoneAllowed(String username, ZoneSettings settings) {
		if (settings.allowOps && ops.contains(username.trim().toLowerCase()))
			return true;
		
		if (settings.isPlayerAllowed(username))
			return true;
		
		return false;
	}
	
	public boolean isProtectedEntity(ACTION action, EntityPlayer player, Entity entity, int x, int y, int z) {
		if (isProtectedEntityType(action, entity) && (player == null || !isPlayerGloballyAllowed(player.username))) {
			List<Area<ZoneSettings>> areas = zones[Util.getWorldIndexFromDimension(entity.worldObj.provider.dimensionId)].get(x, y, z);
			
			for (Area<ZoneSettings> area : areas) {
				if (area.data != null && area.data.protectEntities) {
					if (player != null) {
						if (isPlayerZoneAllowed(player.username, area.data))
							return false;
						
						Item heldItem = null;
						if (action == ACTION.USE_ENTITY && entity instanceof EntityMooshroom && area.data.allowMooshroom && (heldItem = player.getHeldItem().getItem()) != null && heldItem == Item.shears)
							return true;
					}
					
					if (area.data.sendDebugMessages)
						commandManager.notifyAdmins(server, 0, "Protect " + entity.getEntityName() + " " + action + " " + x + "," + y + "," + z + (player == null ? "" : " by " + player.username), new Object[0]);
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isProtectedBlock(ACTION action, EntityPlayer player, ItemStack itemStack, Block block, World world, int x, int y, int z) {
		if (player == null || !isPlayerGloballyAllowed(player.username)) {
			List<Area<ZoneSettings>> areas = zones[Util.getWorldIndexFromDimension(world.provider.dimensionId)].get(x, y, z);
			
			for (Area<ZoneSettings> area : areas) {
				if (area.data != null && area.data.protectBlocks) {
					
					if (player != null && isPlayerZoneAllowed(player.username, area.data))
						return false;
					
					if (action == ACTION.ACTIVATE) {
						if (area.data.allowDoors && (block == Block.doorWood || block == Block.trapdoor))
							return false;
					}
					
					if (area.data.sendDebugMessages) {
						String message = "Protect" 
								+ " " + action
								+ (block == null ? "" : " " + block.getBlockName())
								+ (itemStack == null ? "" : " " + itemStack.getItemName())
								+ " " + x + "," + y + "," + z;
						
						if (player == null)
							commandManager.notifyAdmins(server, 0, message + (player == null ? "" : " by " + player.username), new Object[0]);
						else
							player.sendChatToPlayer(message);
					}
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isProtectedBlock(ACTION action, EntityPlayer player, ItemStack itemStack, Block block, World world, int x, int y, int z, int direction) {
		
		switch (direction) {
			case 0:
				y--;
				break;
			case 1:
				y++;
				break;
			case 2:
				z--;
				break;
			case 3:
				z++;
				break;
			case 4:
				x--;
			case 5:
				x++;
				break;
		}
		
		return isProtectedBlock(action, player, itemStack, block, world, x, y, z);
	}

	@Override
	public void onPlayerBlockAction(PlayerBlockEvent event) {
		ACTION action = null;
		boolean checkDirectionAdjusted = false;
		
		switch (event.getType()) {
			case ACTIVATED:
				break;
			case ACTIVATION_ATTEMPT:
				action = ACTION.ACTIVATE;
				break;
			case REMOVE_ATTEMPT:
				action = ACTION.DIG;
				break;
			case PLACE_ATTEMPT:
				action = ACTION.PLACE;
				checkDirectionAdjusted = true;
				break;
			case CHECK_PLAYEREDIT:
				action = ACTION.CHECK_PLAYER_EDIT;
				checkDirectionAdjusted = true;
				break;
		}
		
		if (action != null && (
				isProtectedBlock(action, event.getPlayer(), event.getItemStack(), event.getBlock(), event.getWorld(), event.getX(), event.getY(), event.getZ())
				|| (checkDirectionAdjusted && isProtectedBlock(action, event.getPlayer(), event.getItemStack(), event.getBlock(), event.getWorld(), event.getX(), event.getY(), event.getZ(), event.getDirection()))
			)) {
			
			if (event.getType() == PlayerBlockEvent.TYPE.ACTIVATION_ATTEMPT)
				event.markHandled();
			else
				event.markNotAllowed();
		}
	}

	@Override
	public void onBlockAction(BlockEvent event) {
		if (event.getType() == BlockEvent.TYPE.EXPLODE_ATTEMPT) {
			if (isProtectedBlock(ACTION.EXPLODE, null, null, event.getBlock(), event.getWorld(), event.getX(), event.getY(), event.getZ())) {
				event.markNotAllowed();
			}
		}
	}

	@Override
	public void onPlayerAction(PlayerActionEvent event) {
		if (event.getType() == PlayerActionEvent.TYPE.PLAYER_USE_ENTITY_ATTEMPT) {
			if (isProtectedEntity(event.isLeftClick() ? ACTION.ATTACK_ENTITY : ACTION.USE_ENTITY, event.getPlayer(), event.getEntity(), MathHelper.floor_double(event.getEntity().posX), MathHelper.floor_double(event.getEntity().posY), MathHelper.floor_double(event.getEntity().posZ))) {
				event.markNotAllowed();
			}
		}
	}

	@Override
	public void onEntityAction(EntityEvent event) {
		if (event.getType() == EntityEvent.TYPE.IS_ENTITY_INVULNERABLE) {
			if (isProtectedEntity(ACTION.IS_ENTITY_INVULNERABLE, null, event.getEntity(), event.getX(), event.getY(), event.getZ())) {
				event.markIsInvulnerable();
			}
		}
		/*else if (event.getType() == EntityEvent.TYPE.EXPLODE_ATTEMPT) {
			if (isProtectedEntity(ACTION.EXPLODE, null, event.getEntity(), MathHelper.floor_double(event.getEntity().posX), MathHelper.floor_double(event.getEntity().posY), MathHelper.floor_double(event.getEntity().posZ))) {
				event.markNotAllowed();
			}
		}*/
	}
	
	public void saveAreas() {
		data.clear();
		
		int size = areasByName.size(), i = 1;
		data.setInt("count", size);
		for (Map.Entry<String, Area<ZoneSettings>> entry : areasByName.entrySet()) {
			entry.getValue().data.saveToSettings(data, "zone" + i);
			i++;
		}
		
		try {
			data.saveSettings();
		} catch (IOException e) {
			ModLoader.outputError(e, getName() + " failed (" + e.getClass().getSimpleName() + ") to save to the data file: " + e.getMessage());
		}
	}
}
