package btwmod.spawncustomizer;

import java.io.IOException;

import net.minecraft.server.MinecraftServer;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;
import btwmods.player.SpawnPosition;

public class mod_SpawnCustomizer implements IMod, IPlayerInstanceListener {
	
	private Settings data = null;
	private CommandSpawn commandSpawn;
	private SpawnPosition globalSpawn = null;
	
	public SpawnPosition getGlobalSpawn() {
		return globalSpawn;
	}
	
	public void setGlobalSpawn(SpawnPosition spawnPosition) throws IOException {
		if (data != null) {
			globalSpawn = spawnPosition;
			
			if (globalSpawn == null) {
				data.removeSection("global");
			}
			else {
				data.setInt("global", "x", globalSpawn.x);
				data.setInt("global", "y", globalSpawn.y);
				data.setInt("global", "z", globalSpawn.z);
				data.setFloat("global", "yaw", globalSpawn.yaw);
				data.setFloat("global", "pitch", globalSpawn.pitch);
				data.saveSettings();
				MinecraftServer.getServer().worldServers[0].getWorldInfo().setSpawnPosition(globalSpawn.x, globalSpawn.y, globalSpawn.z);
			}
		}
	}

	@Override
	public String getName() {
		return "Spawn Customizer";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		this.data = data;
		
		if (data.hasSection("global")) {
			if (data.isInt("global", "x") && data.isInt("global", "y") && data.isInt("global", "z")) {
				globalSpawn = new SpawnPosition(data.getInt("global", "x", 0), data.getInt("global", "y", 0), data.getInt("global", "z", 0),
						data.getFloat("global", "yaw", 0.0F), data.getFloat("global", "pitch", 0.0F));
			}
		}
		
		PlayerAPI.addListener(this);
		CommandsAPI.registerCommand(commandSpawn = new CommandSpawn(this), this);
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandSpawn);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		if (event.getType() == PlayerInstanceEvent.TYPE.GET_DEFAULT_LOCATION) {
			if (globalSpawn != null) {
				event.setSpawnLocation(globalSpawn);
			}
		}
	}
}
