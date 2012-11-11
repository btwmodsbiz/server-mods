package btwmod.pvp;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.ServerAPI;
import btwmods.WorldAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerActionListener;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;
import btwmods.player.PlayerInstanceEvent.METADATA;
import btwmods.player.PlayerActionEvent;
import btwmods.server.ITickListener;
import btwmods.server.TickEvent;
import btwmods.server.TickEvent.TYPE;

public class mod_PvPCommand extends CommandBase implements IMod, IPlayerInstanceListener, IPlayerActionListener, ITickListener {
	
	private long minPvpMinutes = 5L;
	private long maxPvpMinutes = 60L;
	
	private Map<String, Long> playerTimers = new LinkedHashMap<String, Long>();
	private MinecraftServer server;
	
	public long getMinPvPMinutes() {
		return minPvpMinutes;
	}

	public long getMaxPvPMinutes() {
		return maxPvpMinutes;
	}

	@Override
	public String getName() {
		return "PvP";
	}

	@Override
	public void init(Settings settings) throws Exception {
		if (settings.isLong("minpvpminutes")) {
			minPvpMinutes = settings.getLong("minpvpminutes");
		}
		if (settings.isLong("maxpvpminutes")) {
			maxPvpMinutes = settings.getLong("maxpvpminutes");
		}
		
		server = MinecraftServer.getServer();
		CommandsAPI.registerCommand(this, this);
		PlayerAPI.addListener(this);
		ServerAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		CommandsAPI.unregisterCommand(this);
		PlayerAPI.removeListener(this);
		ServerAPI.removeListener(this);
	}

	@Override
	public String getCommandName() {
		return "pvp";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		boolean fullUsageAllowed = isFullUsageAllowed(sender);
		
		// List all players that are PvP flagged.
		if (!(sender instanceof EntityPlayer) || (fullUsageAllowed && args.length == 0)) {
			
			// Build a set of online players.
			String[] usernames = server.getConfigurationManager().getAllUsernames();
			
			// Build a list of PvP enabled players. 
			int count = 0;
			for (int i = 0; i < usernames.length; i++) {
				Long endTime = playerTimers.get(usernames[i]);
				if (endTime != null && endTime.longValue() > getCurrentTimeSeconds()) {
					sender.sendChatToPlayer(usernames[i] + " PvP flagged for " + formatTime(endTime - getCurrentTimeSeconds()) + ".");
					count++;
				}
			}
			
			if (count == 0)
				sender.sendChatToPlayer("No players are PvP flagged.");
		}
		
		else if (args.length > 2 || (args.length == 2 && !fullUsageAllowed)) {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
		
		else if (args.length >= 1 && (args[0].equalsIgnoreCase("on") || args[0].matches("^[0-9]{1,10}$"))) {
			EntityPlayer target = args.length == 2 ? server.getConfigurationManager().getPlayerEntity(args[1]) : (EntityPlayer)sender;
			String username = args.length == 2 ? args[1] : target.username;
			
			if (target == null) {
				sender.sendChatToPlayer("Player " + username + " is not online.");
			}
			else {
				long seconds = 60L * (args[0].equalsIgnoreCase("on") ? minPvpMinutes : Long.parseLong(args[0]));
				long remainingSeconds = playerTimers.containsKey(username) ? playerTimers.get(username) - getCurrentTimeSeconds() : 0;
				
				// Enforce a minimum time for PvP
				if (seconds < minPvpMinutes * 60L && !fullUsageAllowed) {
					sender.sendChatToPlayer("You cannot set yourself as PvP for less than " + minPvpMinutes + " minutes.");
				}
				
				// Enforce a maximum time for PvP
				else if (seconds > maxPvpMinutes * 60L && !fullUsageAllowed) {
					sender.sendChatToPlayer("You cannot set yourself as PvP for more than " + maxPvpMinutes + " minutes.");
				}
				
				// Do not allow a player to reduce their PvP time.
				else if (remainingSeconds > seconds && !fullUsageAllowed) {
					if (args[0].equalsIgnoreCase("on"))
						sender.sendChatToPlayer("You're already PvP flagged and it will expire in " + formatTime(remainingSeconds) + ".");
					else
						sender.sendChatToPlayer("Your PvP status will expire in " + formatTime(remainingSeconds) + " and cannot be set lower.");
				}
				
				else {
					playerTimers.put(username, getCurrentTimeSeconds() + seconds);
					WorldAPI.sendEntityEquipmentUpdate(target);
					sender.sendChatToPlayer((target.equals(sender) ? "You" : username) + " will be PvP flagged for " + formatTime(seconds) + ".");
					
					if (!target.equals(sender))
						target.sendChatToPlayer("Your PvP time has been set to " + formatTime(seconds) + ".");
				}
			}
		}
		
		else if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
			EntityPlayer target = args.length == 2 ? server.getConfigurationManager().getPlayerEntity(args[1]) : (EntityPlayer)sender;
			String username = args.length == 2 ? args[1] : target.username;
			
			if (target == null) {
				sender.sendChatToPlayer("Player " + username + " is not online.");
			}
			else {
				long remainingSeconds = playerTimers.containsKey(((EntityPlayer)sender).username)
						? playerTimers.get(((EntityPlayer)sender).username).longValue() - getCurrentTimeSeconds() : 0;
				
				if (remainingSeconds > 0) {
					sender.sendChatToPlayer((target.equals(sender) ? "Your" : username + "'s") + " PvP status will expire in " + formatTime(remainingSeconds) + ".");
				}
				else {
					sender.sendChatToPlayer((target.equals(sender) ? "You are" : username + " is") + " not PvP flagged.");
				}
			}
		}
		
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + (sender instanceof EntityPlayer ? " (on | <minutes> | status)" + (isFullUsageAllowed(sender) ? " [<username>]" : "") : "");
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return !server.isPVPEnabled();
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (sender instanceof EntityPlayer ) {
			if (args.length == 1) {
				return getListOfStringsMatchingLastWord(args, new String[] { "on", "status" });
			}
			else if (args.length == 2 && isFullUsageAllowed(sender)) {
				return getListOfStringsMatchingLastWord(args, server.getAllUsernames());
			}
		}
		
		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		if (event.getType() == PlayerInstanceEvent.TYPE.CHECK_METADATA && event.getMetadata() == PlayerInstanceEvent.METADATA.IS_PVP) {
			// Only handle if we have data about the player's PvP status, and there is remaining PvP time.
			if (isPlayerPvP(event.getPlayerInstance().username)) {
				event.setMetadataValue(new Boolean(true));
			}
		}
		
		else if (event.getType() == PlayerInstanceEvent.TYPE.READ_NBT) {
			try {
				if (event.getNBTTagCompound().hasKey("pvpRemainingSeconds")) {
					// Read in the players remaining PvP time.
					playerTimers.put(event.getPlayerInstance().username, new Long(event.getNBTTagCompound().getLong("pvpRemainingSeconds") + getCurrentTimeSeconds()));
					PlayerAPI.onPlayerMetadataChanged(event.getPlayerInstance(), METADATA.IS_PVP, new Boolean(true));
				}
			}
			catch (Exception e) {
				ModLoader.outputError(e, getClass().getSimpleName() + " failed to read remaining PvP seconds from the " + event.getPlayerInstance().username + "'s NBT data.");
			}
		}
		
		else if (event.getType() == PlayerInstanceEvent.TYPE.WRITE_NBT) {
			if (playerTimers.containsKey(event.getPlayerInstance().username)) {
				long remainingTime = playerTimers.get(event.getPlayerInstance().username).longValue() - getCurrentTimeSeconds();
				
				// Save the players remaining PvP time.
				if (remainingTime > 0)
					event.getNBTTagCompound().setLong("pvpRemainingSeconds", remainingTime);
			}
		}
	}

	@Override
	public void onPlayerAction(PlayerActionEvent event) {
		if (event.getType() == PlayerActionEvent.TYPE.ATTACK) {
			if (event.getAttackSource().getEntity() instanceof EntityPlayer && event.getAttackTarget() instanceof EntityPlayer) {
				EntityPlayer source = (EntityPlayer)event.getAttackSource().getEntity();
				if (playerTimers.containsKey(source.username)) {
					// Force a minimum amount of remaining time when a PvP player does damage to another player.
					playerTimers.put(source.username, Math.max(playerTimers.get(source.username).longValue(), getCurrentTimeSeconds() + minPvpMinutes * 60L));
				}
			}
		}
	}
	
	public boolean isPlayerPvP(String username) {
		return playerTimers.containsKey(username) && playerTimers.get(username).longValue() > getCurrentTimeSeconds();
	}
	
	public boolean isFullUsageAllowed(ICommandSender sender) {
		return sender instanceof EntityPlayerMP
				? server.getConfigurationManager().areCommandsAllowed(((EntityPlayerMP)sender).username)
				: true;
	}
	
	protected static long getCurrentTimeSeconds() {
		return System.currentTimeMillis() / 1000L;
	}

	protected static String formatTime(long seconds) {
		StringBuilder sb = new StringBuilder();
		
		if (seconds >= 60L) {
			sb.append(seconds / 60L).append(" minute");
			if (seconds / 60L != 1)
				sb.append("s");
		}
		
		if (seconds % 60L != 0) {
			if (sb.length() > 0) sb.append(" ");
			
			sb.append(seconds % 60L).append(" second");
			if (seconds % 60L != 1)
				sb.append("s");
		}
		
		if (sb.length() == 0)
			sb.append(seconds == 0 ? "0 seconds" : "1 second");
		
		return sb.toString();
	}

	@Override
	public void onTick(TickEvent event) {
		if (event.getType() == TYPE.START && event.getTickCounter() % 20 == 0 && playerTimers.size() > 0) {
			long currentSeconds = getCurrentTimeSeconds();
			
			Iterator<Map.Entry<String, Long>> iterator = playerTimers.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Long> entry = iterator.next();
				if (entry.getValue() <= currentSeconds) {
					EntityPlayer player = server.getConfigurationManager().getPlayerEntity(entry.getKey());
					iterator.remove();
					
					if (player != null) {
						PlayerAPI.onPlayerMetadataChanged(player, METADATA.IS_PVP);
						player.sendChatToPlayer("You are no longer PvP flagged.");
					}
				}
			}
		}
	}
}
