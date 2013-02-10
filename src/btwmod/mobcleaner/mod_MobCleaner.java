package btwmod.mobcleaner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.World;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.world.ISpawnLivingListener;
import btwmods.world.IWorldTickListener;
import btwmods.world.SpawnLivingEvent;
import btwmods.world.WorldTickEvent;

public class mod_MobCleaner implements IMod, IWorldTickListener, ISpawnLivingListener {
	
	public static final int MAX_ENTITIES_CHECKED = 200;
	private static Random rnd = new Random();
	
	public double despawnDistance = 130D;
	public int entitiesChecked = 25;
	public int checkFrequency = 20;
	public boolean debugLogging = false;
	public boolean enabled = true;
	
	private CommandMobCleaner commandMobCleaner = null;

	@Override
	public String getName() {
		return "Mob Cleaner";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		entitiesChecked = Math.min(MAX_ENTITIES_CHECKED, Math.max(1, settings.getInt("entitiesChecked", entitiesChecked)));
		enabled = settings.getBoolean("enabledOnStart", enabled);
		debugLogging = settings.getBoolean("debugLogging", debugLogging);
		checkFrequency = Math.max(1, settings.getInt("checkFrequency", checkFrequency));
		
		WorldAPI.addListener(this);
		CommandsAPI.registerCommand(commandMobCleaner = new CommandMobCleaner(this), this);
	}

	@Override
	public void unload() throws Exception {
		WorldAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandMobCleaner);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onWorldTick(WorldTickEvent event) {
		if (enabled && event.getType() == WorldTickEvent.TYPE.END && ServerAPI.getTickCounter() % checkFrequency == 0
				&& MinecraftServer.getServer().getConfigurationManager().playerEntityList.size() != 0) {
			
			// Get the list of entities and its size.
			List entities = event.getWorld().loadedEntityList;
			int size = entities.size();
			
			// Determine where to start in the entity list.
			int startPos = size <= entitiesChecked ? 0 : rnd.nextInt(size - entitiesChecked);
			
			// Check the entities.
			for (int i = startPos; i < Math.min(size, startPos + entitiesChecked); i++) {
				Object obj = entities.get(i);
				if (obj instanceof EntityLiving) {
					EntityLiving entity = (EntityLiving)obj;
					if (!checkEntity(entity) && debugLogging) {
						ModLoader.outputInfo("Despawned entity: " + entity.getEntityName() + " #" + entity.entityId + " from " + Util.getWorldNameFromDimension(entity.worldObj.provider.dimensionId));
					}
				}
			}
		}
	}
	
	public int checkEntities() {
		int count = 0;
		World[] worlds = MinecraftServer.getServer().worldServers;
		for (World world : worlds) {
			count += checkEntities(world);
		}
		return count;
	}
	
	public int checkEntities(World world) {
		int count = 0;
		List entities = world.loadedEntityList;
		for (Object obj : entities) {
			if (obj instanceof EntityLiving) {
				if (!checkEntity((EntityLiving)obj)) {
					count++;
				}
			}
		}
		return count;
	}

	public boolean checkEntity(EntityLiving entity) {
		if (!entity.isDead
				&& entity.isDespawnAllowed()
				&& !entity.isPersistenceRequired()
				&& entity.worldObj.getClosestPlayerToEntity(entity, despawnDistance) == null) {
			
			entity.setDead();
			
			return false;
		}
		
		return true;
	}

	@Override
	public void onSpawnLivingAction(SpawnLivingEvent event) {
		if (event.creatureType == EnumCreatureType.waterCreature) {
			List<EntityLiving> alive = new ArrayList<EntityLiving>();
			for (EntityLiving entity : event.entities)
				if (!entity.isDead)
					alive.add(entity);
			
			int limit = event.validChunks * event.creatureType.getMaxNumberOfCreature() / 256;
			int maxKeep = limit + 10 - event.oldEntityCount;
			if (maxKeep < alive.size()) {
				Collections.shuffle(alive);
				for (int i = maxKeep; i < alive.size(); i++) {	 
					alive.get(i).setDead();
				}
				
				if (debugLogging) {
					ModLoader.outputInfo("Killed " + maxKeep + " to " + alive.size() + " spawned water creatures to fit in limit " + limit + " (+10).");
				}
			}
		}
	}
}
