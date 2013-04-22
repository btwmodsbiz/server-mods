package btwmod.tickmonitor;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.src.ChunkCoordIntPair;
import net.minecraft.src.ICommand;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.StatsAPI;
import btwmods.io.Settings;
import btwmods.measure.Average;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;
import btwmods.stats.IStatsListener;
import btwmods.stats.StatsEvent;
import btwmods.stats.data.BasicStatsMap;
import btwmods.Util;

public class mod_TickMonitor implements IMod, IStatsListener, IPlayerInstanceListener {

	private static int topNumber = 20;
	private static boolean includeHistory = false;
	private static String publicLink = null;
	private static File jsonFile = null; //new File(new File("."), "stats.txt");
    private static long tooLongWarningTime = 1000L;
    private static final long tooLongWarningTimeMin = 250L;
    private static long reportingDelay = 1000L;
    private static long reportingDelayMin = 50L;
	
	public static boolean includeHistory() {
		return includeHistory;
	}
	
	public static int getTopNumber() {
		return topNumber;
	}
	
    private boolean isRunning = true; // TODO: make this false by default.
	private long lastStatsTime = 0;
	private int lastTickCounter = -1;
	private Average statsActionTime = new Average(10);
	private Average statsActionIOTime = new Average(10);
	private Average ticksPerSecond = new Average(10);
	
	private Gson gson;
	
	private ICommand monitorCommand;
	
	public volatile boolean hideChunkCoords = true;
    
	@Override
	public String getName() {
		return "Tick Monitor";
	}

	@Override
	public void init(Settings settings, Settings data) {
		lastStatsTime = System.currentTimeMillis();
		
		gson = new GsonBuilder()
			.registerTypeAdapter(Class.class, new TypeAdapters.ClassAdapter())
			.registerTypeAdapter(ChunkCoordIntPair.class, new TypeAdapters.ChunkCoordIntPairAdapter())
			.registerTypeAdapter(Average.class, new TypeAdapters.AverageTypeAdapter(this))
			.registerTypeAdapter(BasicStatsMap.class, new TypeAdapters.BasicStatsMapAdapter())
			.setPrettyPrinting()
			.enableComplexMapKeySerialization()
			.create();
		
		// Load settings
		if (settings.hasKey("publicLink") && !(new File(settings.get("publicLink")).isDirectory())) {
			publicLink = settings.get("publicLink");
		}
		if (settings.hasKey("jsonFile") && !(new File(settings.get("jsonFile")).isDirectory())) {
			jsonFile = new File(settings.get("jsonFile"));
		}
		isRunning = settings.getBoolean("runOnStartup", isRunning);
		reportingDelay = Math.max(reportingDelayMin, settings.getLong("reportingDelay", reportingDelay));
		tooLongWarningTime = Math.max(tooLongWarningTimeMin, settings.getLong("tooLongWarningTime", tooLongWarningTime));
		hideChunkCoords = settings.getBoolean("hideChunkCoords", hideChunkCoords);
		includeHistory = settings.getBoolean("includeHistory", includeHistory);
		
		PlayerAPI.addListener(this);
		CommandsAPI.registerCommand(monitorCommand = new MonitorCommand(this), this);
		
		// Add the stats listener only if isRunning is true by default.
		if (isRunning)
			StatsAPI.addListener(this);
	}

	@Override
	public void unload() {
		StatsAPI.removeListener(this);
		PlayerAPI.removeListener(this);
		CommandsAPI.unregisterCommand(monitorCommand);
	}
	
	public void setIsRunning(boolean value) {
		if (isRunning == value)
			return;
		
		lastStatsTime = 0;
		lastTickCounter = -1;
		
		if (isRunning)
			StatsAPI.removeListener(this);
		else
			StatsAPI.addListener(this);
		
		isRunning = value;
	}

	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * WARNING: This runs in a separate thread. Be very strict about what this accesses beyond the passed parameter. Do
	 * not access any of the APIs, and be careful how class variables are used outside of statsAction().
	 */
	@Override
	public void onStats(StatsEvent event) {
		long currentTime = System.currentTimeMillis();
		long startNano = System.nanoTime();
		
		if (lastStatsTime == 0) {
			lastStatsTime = System.currentTimeMillis();
		}
		
		else if (currentTime - lastStatsTime > reportingDelay) {
			int numTicks = event.tickCounter - lastTickCounter;
			
			long timeElapsed = currentTime - lastStatsTime;
			long timeSinceLastTick = currentTime - event.serverStats.lastTickEnd;
			ticksPerSecond.record((long)((double)numTicks / (double)timeElapsed * 100000D));

			// Debugging loop to ramp up CPU usage by the thread.
			//for (int i = 0; i < 20000; i++) new String(new char[10000]).replace('\0', 'a');
			
			String json = null;
			if (jsonFile != null)
				json = createJson(event, timeSinceLastTick, numTicks, currentTime);

			long startWriteTime = System.currentTimeMillis();
			long startWriteNano = System.nanoTime();
			
			if (jsonFile != null) {
				try {
					FileWriter jsonWriter = new FileWriter(jsonFile);
					jsonWriter.write(json);
					jsonWriter.close();
				}
				catch (Throwable e) {
					ModLoader.outputError(getName() + " failed to write to " + jsonFile.getPath() + ": " + e.getMessage());
				}
			}
			
			long endNano = System.nanoTime();
			long endTime = System.currentTimeMillis();
			
			if (System.currentTimeMillis() - currentTime > tooLongWarningTime)
				ModLoader.outputError(getName() + " took " + (endTime - currentTime) + "ms (~" + ((endTime - startWriteTime) * 100 / (endTime - currentTime)) + "% disk IO) to process stats. Note: This will *not* slow the main Minecraft server thread.");

			statsActionTime.record(endNano - startNano);
			statsActionIOTime.record(endNano - startWriteNano);
			lastTickCounter = event.tickCounter;
			lastStatsTime = System.currentTimeMillis();
		}
	}
	
	private String createJson(StatsEvent event, long timeSinceLastTick, int numTicks, long currentTime) {
		long jsonTime = System.nanoTime();
		JsonObject jsonObj = new JsonObject();
		jsonObj.addProperty("tickCounter", event.tickCounter);
		jsonObj.addProperty("timeSinceLastTick", timeSinceLastTick);
		jsonObj.addProperty("ticksSinceLastStatsAction", numTicks);
		jsonObj.addProperty("time", Util.DATE_FORMAT_LOGS.format(new Date(currentTime)));
		jsonObj.addProperty("statProfile", StatsAPI.statProfile);
		
		jsonObj.add("statsActionTime", gson.toJsonTree(statsActionTime));
		jsonObj.add("statsActionIOTime", gson.toJsonTree(statsActionIOTime));
		jsonObj.add("ticksPerSecondArray", gson.toJsonTree(ticksPerSecond));
		
		jsonObj.addProperty("jsonTime", "_____JSONTIME_____");
		
		JsonObject serverStats = (JsonObject)gson.toJsonTree(event.serverStats);
		for (Map.Entry<String, JsonElement> entry : serverStats.entrySet()) {
			jsonObj.add(entry.getKey(), entry.getValue());
		}
		
		jsonObj.add("worlds", gson.toJsonTree(event.worldStats));
		
		return gson.toJson(jsonObj).replace("_____JSONTIME_____", Util.DECIMAL_FORMAT_3.format((jsonTime = System.nanoTime() - jsonTime) * 1.0E-6D));
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		if (publicLink != null && event.getType() == PlayerInstanceEvent.TYPE.LOGIN)
			event.getPlayerInstance().sendChatToPlayer("Tick stats are available at " + publicLink);
	}
}
