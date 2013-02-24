package btwmod.livemap;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Chunk;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityAnimal;
import net.minecraft.src.EntityGhast;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.EntitySlime;
import net.minecraft.src.MathHelper;
import net.minecraft.src.WorldServer;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.Util;
import btwmods.WorldAPI;
import btwmods.io.AsynchronousFileWriter;
import btwmods.io.Settings;
import btwmods.world.IWorldTickListener;
import btwmods.world.WorldTickEvent;

public class mod_MapData implements IMod, IWorldTickListener {
	
	public boolean enabled = false;
	public boolean minimal = true;
	
	public int playerSaveTicks = 20;
	public int entitiesSaveTicks = 20 * 5;
	public int chunksSaveTicks = 20 * 5;
	public int tickVariance = 10;
	
	private File saveDirectory = null;
	private Random rand = new Random();
	private MinecraftServer server;
	
	private AsynchronousFileWriter fileWriter;
	
	private int[] playerDataTicks = null;
	private int[] entitiesDataTicks = null;
	private int[] chunksDataTicks = null;
	
	private CommandMapData commandMapData;

	@Override
	public String getName() {
		return "Map Data";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		server = MinecraftServer.getServer();
		enabled = settings.getBoolean("MapData", "enabled", enabled);
		minimal = settings.getBoolean("MapData", "minimal", minimal);
		
		playerSaveTicks = Math.max(1, settings.getInt("MapData", "playerSaveTicks", playerSaveTicks));
		entitiesSaveTicks = Math.max(1, settings.getInt("MapData", "entitiesSaveTicks", entitiesSaveTicks));
		chunksSaveTicks = Math.max(1, settings.getInt("MapData", "chunksSaveTicks", chunksSaveTicks));
		tickVariance = Math.max(0, settings.getInt("MapData", "tickVariance", tickVariance));
		
		fileWriter = new AsynchronousFileWriter(getName() + " Writer");
		
		if (settings.hasKey("MapData", "saveDirectory")) {
			saveDirectory = new File(settings.get("MapData", "saveDirectory"));
		}
		
		if (saveDirectory == null) {
			ModLoader.outputError(getName() + "'s [MapData]saveDirectory setting is not set.", Level.SEVERE);
			return;
		}
		else if (!saveDirectory.isDirectory()) {
			ModLoader.outputError(getName() + "'s [MapData]saveDirectory setting does not point to a directory.", Level.SEVERE);
			return;
		}

		playerDataTicks = new int[server.worldServers.length];
		entitiesDataTicks = new int[server.worldServers.length];
		chunksDataTicks = new int[server.worldServers.length];
		
		CommandsAPI.registerCommand(commandMapData = new CommandMapData(this), this);
		WorldAPI.addListener(this);
		//ServerAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		CommandsAPI.unregisterCommand(commandMapData);
		WorldAPI.removeListener(this);
		//ServerAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}
	
	private int getRandomTicks(int baseTicks) {
		return baseTicks - tickVariance + rand.nextInt(tickVariance * 2);
	}

	@Override
	public void onWorldTick(WorldTickEvent event) {
		if (enabled && event.getType() == WorldTickEvent.TYPE.END) {
			WorldServer worldServer = (WorldServer)event.getWorld();
			int worldIndex = event.getWorldIndex();
			
			if ((--playerDataTicks[event.getWorldIndex()]) <= 0) {
				savePlayerData(worldIndex, worldServer);
				playerDataTicks[event.getWorldIndex()] = getRandomTicks(playerSaveTicks);
			}
			
			if (!minimal && (--entitiesDataTicks[event.getWorldIndex()]) <= 0) {
				saveEntitiesData(worldIndex, worldServer);
				entitiesDataTicks[event.getWorldIndex()] = getRandomTicks(entitiesSaveTicks);
			}
			
			if (!minimal && (--chunksDataTicks[event.getWorldIndex()]) <= 0) {
				saveLoadedChunksData(worldIndex);
				chunksDataTicks[event.getWorldIndex()] = getRandomTicks(chunksSaveTicks);
			}
		}
	}
	
	private void savePlayerData(int worldIndex, WorldServer world) {
		long execTime = System.nanoTime();
		
		JsonArray playersJson = new JsonArray();
		for (EntityPlayerMP player : (List<EntityPlayerMP>)world.playerEntities) {
			if (!player.isDead) {
				JsonObject playerJson = new JsonObject();
				playerJson.addProperty("username", player.username);
				playerJson.addProperty("id", player.entityId);
				playerJson.addProperty("x", MathHelper.floor_double(player.posX));
				playerJson.addProperty("y", MathHelper.floor_double(player.posY));
				playerJson.addProperty("z", MathHelper.floor_double(player.posZ));
				playerJson.addProperty("health", player.getHealth());
				playersJson.add(playerJson);
			}
		}

		queueWrite(System.nanoTime() - execTime, worldIndex, "players", "players", playersJson);
	}
	
	private void saveEntitiesData(int worldIndex, WorldServer world) {
		long execTime = System.nanoTime();
		
		JsonArray entitiesJson = new JsonArray();
		JsonArray entitiesLivingJson = new JsonArray();
		
		for (Entity entity : (List<Entity>)world.loadedEntityList) {
			if (!entity.isDead) {
				JsonObject entityJson = new JsonObject();
				entityJson.addProperty("i", entity.entityId);
				//entityJson.addProperty("n", entity.getEntityName());
				//entityJson.addProperty("c", entity.getClass().getSimpleName());
				entityJson.add("c", new JsonClassName(entity));
				entityJson.addProperty("x", MathHelper.floor_double(entity.posX));
				entityJson.addProperty("y", MathHelper.floor_double(entity.posY));
				entityJson.addProperty("z", MathHelper.floor_double(entity.posZ));
				
				if (entity instanceof EntityLiving) {
					EntityLiving entityLiving = (EntityLiving)entity;
					entityJson.addProperty("age", entityLiving.getAge());
					entityJson.addProperty("hp", entityLiving.getHealth());

					if (entityLiving.isPersistenceRequired())
						entityJson.addProperty("persistenceRequired", true);
					
					if (entity instanceof EntityAnimal)
						entityJson.addProperty("iA", 1);
					
					else if (entity instanceof EntityMob || entity instanceof EntityGhast || entity instanceof EntitySlime)
						entityJson.addProperty("iM", 1);
					
					else if (entity instanceof EntityPlayer)
						entityJson.addProperty("iP", 1);
					
					entitiesLivingJson.add(entityJson);
				}
				
				else {
					if (entity instanceof EntityItem) {
						entityJson.addProperty("iI", true);
					}
					
					entitiesJson.add(entityJson);
				}
			}
		}
		
		execTime = System.nanoTime() - execTime;
		
		queueWrite(execTime, worldIndex, "entities", "entities", entitiesJson);
		queueWrite(execTime, worldIndex, "entities_living", "entities", entitiesLivingJson);
	}
	
	private void saveLoadedChunksData(int worldIndex) {
		long execTime = System.nanoTime();
		
		JsonArray chunksJson = new JsonArray();
		Iterator chunkIterator = WorldAPI.getLoadedChunks()[worldIndex].iterator();
		while (chunkIterator.hasNext()) {
			Object obj = chunkIterator.next();
			if (obj instanceof Chunk) {
				Chunk chunk = (Chunk)obj;
				JsonArray chunkJson = new JsonArray();
				chunkJson.add(new JsonPrimitive(chunk.xPosition));
				chunkJson.add(new JsonPrimitive(chunk.zPosition));
				chunksJson.add(chunkJson);
			}
		}

		queueWrite(System.nanoTime() - execTime, worldIndex, "chunks", "chunks", chunksJson);
	}
	
	private void queueWrite(long execTime, int worldIndex, String fileName, String elemName, JsonElement element) {
		JsonObject json = new JsonObject();
		json.addProperty("minimal", minimal);
		json.addProperty("time", System.currentTimeMillis());
		json.addProperty("tick", server.getTickCounter());
		json.addProperty("execTime", Util.DECIMAL_FORMAT_3.format(execTime * 1.0E-6D));
		json.add(elemName, element);
		
		fileWriter.queueWrite(new QueuedWriteJson(new File(saveDirectory, "world" + worldIndex + "_" + fileName + ".txt"), json));
	}
}
