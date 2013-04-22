package btwmod.admincommands;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ICommand;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet102WindowClick;
import net.minecraft.src.Packet14BlockDig;
import net.minecraft.src.Packet15Place;
import net.minecraft.src.Packet16BlockItemSwitch;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.Packet7UseEntity;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.NetworkAPI;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.network.IPacketListener;
import btwmods.network.NetworkType;
import btwmods.network.PacketEvent;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;

public class mod_AdminCommands implements IMod, IPacketListener, IPlayerInstanceListener {
	
	private long secondsForAFK = 300;
	private final Map<String, Long> lastPlayerAction = new HashMap<String, Long>();
	private Set<ICommand> commands = new LinkedHashSet<ICommand>();

	@Override
	public String getName() {
		return "Admin Commands";
	}
	
	public long getSecondsForAFK() {
		return secondsForAFK;
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		NetworkAPI.addListener(this);
		PlayerAPI.addListener(this);
		registerCommand(new CommandWho(this));
		//registerCommand(new DumpTrackedCommand());
		registerCommand(new CommandDumpEntities(settings));
		registerCommand(new CommandClearEntities());
		registerCommand(new CommandMOTD());
		registerCommand(new CommandHasPendingIO());

		// Load settings
		secondsForAFK = settings.getLong("secondsForAFK", secondsForAFK);
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
		NetworkAPI.removeListener(this);
		unregisterCommands();
	}

	@Override
	public void onPacket(PacketEvent event) {
		if (event.getType() == NetworkType.RECEIVED) {
			Packet packet = event.getPacket();
			
			if (packet instanceof Packet102WindowClick
					|| packet instanceof Packet14BlockDig
					|| packet instanceof Packet15Place
					|| packet instanceof Packet3Chat
					|| packet instanceof Packet7UseEntity
					|| packet instanceof Packet16BlockItemSwitch) {
				markPlayerActed(event.getPlayer().username);
			}
		}
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
	
	private void markPlayerActed(String username) {
		lastPlayerAction.put(username, new Long(System.currentTimeMillis()));
	}
	
	public boolean isPlayerAFK(EntityPlayerMP player) {
		return getTimeSinceLastPlayerAction(player) >= secondsForAFK;
	}
	
	public long getTimeSinceLastPlayerAction(EntityPlayerMP player) {
		Long timeSinceLastAction = lastPlayerAction.get(player.username);
		return timeSinceLastAction == null ? 0 : (System.currentTimeMillis() - timeSinceLastAction.longValue()) / 1000L;
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		if (event.getType() == PlayerInstanceEvent.TYPE.LOGIN) {
			markPlayerActed(event.getPlayerInstance().username);
		}
	}
}
