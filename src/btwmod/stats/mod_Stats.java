package btwmod.stats;

import java.io.File;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.Stat;
import btwmods.StatsAPI;
import btwmods.Util;
import btwmods.io.AsynchronousFileWriter;
import btwmods.io.QueuedWrite;
import btwmods.io.QueuedWriteString;
import btwmods.io.Settings;
import btwmods.measure.Average;
import btwmods.measure.Measurement;
import btwmods.stats.IStatsListener;
import btwmods.stats.StatsEvent;
import btwmods.stats.data.WorldStats;

public class mod_Stats implements IMod, IStatsListener {
	
    private final static long reportingDelayMin = 50L;

	private File publicDirectory = null;
	private File privateDirectory = null;
    private long reportingDelay = 1000L;
	
	private Gson gson;
	private AsynchronousFileWriter fileWriter;
	
	private long lastStatsTime = 0;
	private int lastTickCounter = -1;
	private Average ticksPerSecond = new Average(10);

	@Override
	public String getName() {
		return "Stats";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		reportingDelay = Math.max(reportingDelayMin, settings.getLong("reportingDelay", reportingDelay));
		
		if (settings.hasKey("publicDirectory")) {
			publicDirectory = new File(settings.get("publicDirectory"));
		}
		
		if (publicDirectory == null) {
			ModLoader.outputError(getName() + "'s publicDirectory setting is not set.", Level.SEVERE);
			return;
		}
		else if (!publicDirectory.isDirectory()) {
			ModLoader.outputError(getName() + "'s publicDirectory setting does not point to a directory.", Level.SEVERE);
			return;
		}
		
		if (settings.hasKey("privateDirectory")) {
			privateDirectory = new File(settings.get("privateDirectory"));
		}
		
		if (privateDirectory == null) {
			ModLoader.outputError(getName() + "'s privateDirectory setting is not set.", Level.SEVERE);
			return;
		}
		else if (!privateDirectory.isDirectory()) {
			ModLoader.outputError(getName() + "'s privateDirectory setting does not point to a directory.", Level.SEVERE);
			return;
		}
		
		gson = new GsonBuilder()
			.setPrettyPrinting()
			//.enableComplexMapKeySerialization()
			.create();

		lastStatsTime = System.currentTimeMillis();
		fileWriter = new AsynchronousFileWriter(getName() + " Writer");
		
		StatsAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		StatsAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onStats(StatsEvent event) {
		long currentTime = System.currentTimeMillis();
		long startNano = System.nanoTime();
		
		if (lastStatsTime == 0) {
			lastStatsTime = System.currentTimeMillis();
			return;
		}
		
		if (currentTime - lastStatsTime <= reportingDelay)
			return;
			
		int numTicks = event.tickCounter - lastTickCounter;
		long timeElapsed = currentTime - lastStatsTime;
		ticksPerSecond.record((long)((double)numTicks / (double)timeElapsed * 100000D));
		
		writeBasic(event);
		writeEntities(event);
	
		lastTickCounter = event.tickCounter;
		lastStatsTime = System.currentTimeMillis();
	}
	
	private void writeBasic(StatsEvent event) {
		long start = System.nanoTime();
		
		JsonObject basicStats = new JsonObject();
		basicStats.addProperty("tickNumber", event.tickCounter);
		basicStats.add("tick", averageToJson(event.serverStats.tickTime, true, false));
		basicStats.add("tickSec", averageToJson(ticksPerSecond, false, false));
		
		JsonArray worlds = new JsonArray();
		for (int i = 0, l = event.worldStats.length; i < l; i++) {
			JsonObject worldStats = new JsonObject();
			worldStats.add("tick", averageToJson(event.worldStats[i].averages.get(Stat.WORLD_TICK), true, false));
			
			worldStats.add("tickEntities", averageToJson(event.worldStats[i].averages.get(Stat.ENTITY_UPDATE), true, false));
			worldStats.add("tickBlocks", averageToJson(event.worldStats[i].averages.get(Stat.BLOCK_UPDATE), true, false));
			
			worldStats.add("entities", averageToJson(event.worldStats[i].averages.get(Stat.WORLD_LOADED_ENTITIES), false, false));
			worldStats.add("tileEntities", averageToJson(event.worldStats[i].averages.get(Stat.WORLD_LOADED_TILE_ENTITIES), false, false));
			
			//worldStats.add("tickByEntity", getClassByTime(Stat.ENTITY_UPDATE, event.worldStats[i]));
			
			worlds.add(worldStats);
		}
		
		basicStats.add("worlds", worlds);
		
		basicStats.addProperty("execTime", Util.DECIMAL_FORMAT_3.format((System.nanoTime() - start) * 1.0E-6D));
		
		fileWriter.queueWrite(new QueuedWriteString(new File(publicDirectory, "public.txt"), gson.toJson(basicStats), QueuedWrite.TYPE.OVERWRITE_SAFE));
	}
	
	private void writeEntities(StatsEvent event) {
		for (int i = 0; i < event.worldStats.length; i++) {
			//Map<Class, Set<Integer>>
			for (Measurement measurement : event.worldStats[0].measurements) {
				if (measurement.identifier == Stat.ENTITY_UPDATE) {
					
				}
			}
		}
	}
	
	private static JsonElement getClassByTime(Stat stat, WorldStats worldStats) {
		JsonObject list = new JsonObject();
		for (Entry<Class, Average> entry : worldStats.timeByClass.get(stat).entrySet()) {
			list.add(entry.getKey().getSimpleName(), averageToJson(entry.getValue(), true, false));
		}
		return list;
	}
	
	public static JsonElement averageToJson(Average average, boolean isNano, boolean includeHistory) {
		JsonObject json = new JsonObject();
		
		json.addProperty("average", Util.DECIMAL_FORMAT_3.format(average.getAverage() * (isNano ? 1.0E-6D : 1D)));
		
		if (isNano)
			json.addProperty("latest", Util.DECIMAL_FORMAT_3.format(average.getLatest() * 1.0E-6D));
		else
			json.addProperty("latest", average.getLatest());
		
		if (includeHistory) {
			json.addProperty("resolution", average.getResolution());
			
			JsonArray historyArray = new JsonArray();
			if (average.getTotal() > 0 && average.getTick() >= 0) {
				long[] history = average.getHistory();
				int backIndex = average.getTick() - average.getResolution();
				for (int i = average.getTick(); i >= 0 && i > backIndex; i--) {

					if (isNano)
						historyArray.add(new JsonPrimitive(Util.DECIMAL_FORMAT_3.format(history[i % average.getResolution()] * 1.0E-6D)));
					else
						historyArray.add(new JsonPrimitive(history[i % average.getResolution()]));
				}
			}
			
			json.add("history", historyArray);
		}
		
		return json;
	}

}
