package btwmod.tickmonitor;

import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.StatsAPI;
import btwmods.Util;
import btwmods.io.Settings;
import btwmods.stats.IStatsListener;
import btwmods.stats.StatsEvent;

public class mod_ThreadWatcher implements IMod, IStatsListener {

	private volatile long secondsUntilWarning = 10;
	public Thread modLoaderThread = null;
	private volatile long lastTickEnd = -1;
	
	public volatile WatcherThread watcher = null;

	@Override
	public String getName() {
		return "Thread Watcher";
	}

	@Override
	public void init(Settings settings) {
		modLoaderThread = ModLoader.getInitThread();
		
		if (settings.isLong("secondsuntilwarning")) {
			secondsUntilWarning = Math.max(1L, settings.getLong("secondsuntilwarning"));
		}
		
		StatsAPI.addListener(this);
		
		if (watcher == null) {
			Thread thread = new Thread(watcher = new WatcherThread());
			thread.setName(getName());
			thread.start();
		}
	}

	@Override
	public void unload() {
		watcher = null;
		StatsAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void statsAction(StatsEvent event) {
		lastTickEnd = event.serverStats.lastTickEnd;
	}

	private class WatcherThread implements Runnable {

		@Override
		public void run() {
			try {
				while (watcher == this) {
					long timeSinceLastTick = System.currentTimeMillis() - lastTickEnd;
					if (lastTickEnd >= 0 && timeSinceLastTick > secondsUntilWarning * 1000) {
						net.minecraft.server.MinecraftServer.logger.warning("Main thread hasn't responded for " + Util.DECIMAL_FORMAT_3.format((double)timeSinceLastTick / 1000D) + " sec:\n" + Util.convertStackTrace(modLoaderThread.getStackTrace()));
					}
					
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						
					}
				}
			}
			catch (Throwable e) {
				ModLoader.outputError(e, "StatsAPI thread failed (" + e.getClass().getSimpleName() + "): " + e.getMessage());
				
				if (watcher == this)
					watcher = null;
			}
			
		}
		
	}
}
