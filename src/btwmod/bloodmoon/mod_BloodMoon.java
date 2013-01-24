package btwmod.bloodmoon;

import java.util.Random;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.WorldServer;
import btwmods.IMod;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.io.Settings;
import btwmods.server.ITickListener;
import btwmods.server.TickEvent;

public class mod_BloodMoon implements IMod, ITickListener {
	
	public static final long CHECK_OFFSET = 12000L;
	public static final long BLOODMOON_START_OFFSET = 13200L;
	public static final long BLOODMOON_END_OFFSET = 22700L;
	
	private WorldServer overworld = null;
	protected long nextCheckTime = -1;
	protected long bloodMoonBegin = -1;
	protected long bloodMoonEnd = -1;
	protected boolean bloodMoonActive = false;
	protected Random rnd = new Random();
	
	public int chanceForBloodMoon = 2;

	@Override
	public String getName() {
		return "Blood Moon";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		overworld = MinecraftServer.getServer().worldServers[0];
		chanceForBloodMoon = Math.max(0, settings.getInt("chanceForBloodMoon", chanceForBloodMoon));
		ServerAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		ServerAPI.removeListener(this);
	}
	
	public long getWorldTime() {
		return overworld.getWorldTime();
	}
	
	public long getNextCheckTime() {
		return nextCheckTime;
	}
	
	public boolean isBloodMoonTonight() {
		return bloodMoonBegin > 0;
	}
	
	public boolean isBloodMoon() {
		return bloodMoonActive;
	}
	
	public void setBloodMoonTonight(boolean broadcastMessage) {
		setBloodMoonTonight(overworld.getWorldTime());
		
		if (broadcastMessage) {
			broadcastOverworldMessage(Util.COLOR_YELLOW + "There is a foul smell to the air...");
		}
	}
	
	public void setBloodMoonTonight(long time) {
		long today = getTodayStart(time);
		bloodMoonBegin = today + BLOODMOON_START_OFFSET;
		bloodMoonEnd = today + BLOODMOON_END_OFFSET;
	}
	
	public void setNextCheck() {
		setNextCheck(overworld.getWorldTime());
	}
	
	public void setNextCheck(long time) {
		long dayStart = time % 24000 < CHECK_OFFSET ? getTodayStart(time) : getTomorrowStart(time);
		nextCheckTime = dayStart + CHECK_OFFSET;
	}
	
	public long getTodayStart() {
		return getTodayStart(overworld.getWorldTime());
	}
	
	public static long getTodayStart(long time) {
		return time / 24000L * 24000L;
	}
	
	public long getTomorrowStart() {
		return getTomorrowStart(overworld.getWorldTime());
	}
	
	public static long getTomorrowStart(long time) {
		return getTodayStart(time) + 24000L;
	}

	@Override
	public void onTick(TickEvent event) {
		if (chanceForBloodMoon > 0 && event.getType() == TickEvent.TYPE.START && event.getTickCounter() % 20 == 0) {
			// Very first tick.
			if (nextCheckTime < 0) {
				setNextCheck();
			}
			else {
				long time = overworld.getWorldTime();
				
				// Reset everything if the time jumped too far backwards or forwards.
				if (Math.abs(time - nextCheckTime) > 24000L) {
					setNextCheck();
					endBloodMoon();
				}
				
				// Attempt to set a bloodmoon for tonight.
				else if (nextCheckTime < time) {
					if (overworld.playerEntities.size() > 0 && rnd.nextInt(chanceForBloodMoon) == 0)
						setBloodMoonTonight(true);
					
					setNextCheck();
				}

				// End the blood moon.
				if (bloodMoonEnd > 0 && bloodMoonEnd <= time) {
					endBloodMoon();
				}
				
				// Begin the blood moon.
				else if (!bloodMoonActive && bloodMoonBegin > 0 && time >= bloodMoonBegin && time < bloodMoonEnd) {
					beginBloodMoon();
				}
			}
		}
	}
	
	protected void beginBloodMoon() {
		broadcastOverworldMessage(Util.COLOR_RED + "A blood moon rises!");
		bloodMoonActive = true;
	}
	
	protected void endBloodMoon() {
		if (bloodMoonActive) {
			broadcastOverworldMessage(Util.COLOR_LIME + "The sun rises! The blood moon has ended.");
		}

		bloodMoonActive = false;
		bloodMoonBegin = -1;
		bloodMoonEnd = -1;
		
		// TODO: Cleanup entities.
	}
	
	protected void broadcastOverworldMessage(String message) {
		if (message != null && message.length() > 0) {
			for (Object obj : overworld.playerEntities) {
				if (obj instanceof EntityPlayerMP) {
					EntityPlayerMP player = (EntityPlayerMP)obj;
					player.sendChatToPlayer(message);
				}
			}
		}
	}

	@Override
	public IMod getMod() {
		return this;
	}
}
