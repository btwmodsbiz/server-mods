package btwmod.chat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Packet3Chat;

import btwmods.ChatAPI;
import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.ServerAPI;
import btwmods.Util;
import btwmods.chat.IPlayerAliasListener;
import btwmods.chat.IPlayerChatListener;
import btwmods.chat.PlayerAliasEvent;
import btwmods.chat.PlayerChatEvent;
import btwmods.io.Settings;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;
import btwmods.server.ITickListener;
import btwmods.server.TickEvent;
import btwmods.util.CaselessKey;
import btwmods.util.ValuePair;

public class mod_Chat implements IMod, IPlayerChatListener, IPlayerInstanceListener, IPlayerAliasListener, ITickListener {
	
	public final static long aprilFirstStart;
	public final static long aprilFirstEnd;
	
	static {
		Calendar calendar = Calendar.getInstance();

		calendar.set(calendar.get(Calendar.YEAR), 3, 1, 0, 0, 0);
		aprilFirstStart = calendar.getTimeInMillis();
		
		calendar.set(calendar.get(Calendar.YEAR), 3, 1, 23, 59, 59);
		aprilFirstEnd = calendar.getTimeInMillis();
		calendar.getTimeInMillis();
	}
	
	private Random rnd = new Random();

	public String globalMessageFormat = "<%1$s> %2$s";
	public String emoteMessageFormat = "* %1$s %2$s";
	public Set<String> bannedColors = new HashSet<String>();
	public Set<String> bannedUsers = new HashSet<String>();
	
	private Settings data = null;
	private Map<String, String> colorLookup = new HashMap<String, String>();
	private CommandChatColor commandChatColor;
	
	public int chatRestoreLines = 30;
	private long chatRestoreTimeout = 20L;
	private Deque<ValuePair<String, Long>> chatRestoreBuffer = new ArrayDeque<ValuePair<String, Long>>();
	private Map<String, Long> loginTime = new HashMap<String, Long>();
	private Map<String, Long> logoutTime = new HashMap<String, Long>();
	
	private CommandChatAlias commandChatAlias;
	
	private CommandIgnore commandIgnore;
	private CommandUnignore commandUnignore;
	public static final String IGNORE_PREFIX = "ignore_";
	public int defaultIgnoreMinutes = 30;
	public int maxIgnoreMinutes = 120;
	
	private Map<String, String> aprilFirstColors = new HashMap<String, String>();
	public boolean aprilFirstJoke = false;
	private boolean aprilFirstJokeRunning = false;
	
	@Override
	public String getName() {
		return "Chat";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		this.data = data;

		String bannedUsers = settings.get("bannedUsers");
		if (bannedUsers != null)
			for (String bannedUser : bannedUsers.split("[,; ]+"))
				this.bannedUsers.add(bannedUser.toLowerCase().trim());
		
		String bannedColors = settings.get("bannedColors");
		if (bannedColors != null)
			for (String bannedColor : bannedColors.split("[,; ]+"))
				this.bannedColors.add(bannedColor.toLowerCase().trim());
		
		chatRestoreLines = settings.getInt("chatRestoreLines", chatRestoreLines);
		chatRestoreTimeout = settings.getLong("chatRestoreTimeout", chatRestoreTimeout);
		defaultIgnoreMinutes = settings.getInt("defaultIgnoreMinutes", defaultIgnoreMinutes);
		maxIgnoreMinutes = settings.getInt("maxIgnoreMinutes", maxIgnoreMinutes);
		aprilFirstJoke = settings.getBoolean("aprilFirstJoke", aprilFirstJoke);
		
		//addColor("black", Util.COLOR_BLACK);
		//addColor("navy", Util.COLOR_NAVY);
		addColor("green", Util.COLOR_GREEN);
		addColor("teal", Util.COLOR_TEAL);
		addColor("maroon", Util.COLOR_MAROON);
		addColor("purple", Util.COLOR_PURPLE);
		addColor("gold", Util.COLOR_GOLD);
		addColor("silver", Util.COLOR_SILVER);
		addColor("grey", Util.COLOR_GREY);
		addColor("blue", Util.COLOR_BLUE);
		addColor("lime", Util.COLOR_LIME);
		addColor("aqua", Util.COLOR_AQUA);
		addColor("red", Util.COLOR_RED);
		addColor("pink", Util.COLOR_PINK);
		addColor("yellow", Util.COLOR_YELLOW);
		addColor("white", Util.COLOR_WHITE);
		
		ChatAPI.addListener(this);
		PlayerAPI.addListener(this);
		ServerAPI.addListener(this);
		CommandsAPI.registerCommand(commandChatColor = new CommandChatColor(this), this);
		CommandsAPI.registerCommand(commandChatAlias = new CommandChatAlias(this), this);
		CommandsAPI.registerCommand(commandIgnore = new CommandIgnore(this), this);
		CommandsAPI.registerCommand(commandUnignore = new CommandUnignore(this), this);
	}
	
	private void addColor(String color, String colorCode) {
		if (!bannedColors.contains(color)) {
			colorLookup.put(color, colorCode);
		}
	}

	@Override
	public void unload() throws Exception {
		ChatAPI.removeListener(this);
		PlayerAPI.removeListener(this);
		ServerAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandChatColor);
		CommandsAPI.unregisterCommand(commandChatAlias);
		CommandsAPI.unregisterCommand(commandIgnore);
		CommandsAPI.unregisterCommand(commandUnignore);
	}

	@Override
	public IMod getMod() {
		return this;
	}
	
	public boolean setPlayerColor(String username, String color) {
		if (username == null | color == null) {
			return false;
		}
		else if (color.equalsIgnoreCase("off") || color.equalsIgnoreCase("white") || isBannedUser(username)) {
			if (data.removeKey(username.toLowerCase(), "color")) {
				data.saveSettings(this);
				return true;
			}
		}
		else if (isValidColor(color)) {
			data.set(username.toLowerCase(), "color", color.toLowerCase());
			data.saveSettings(this);
			return true;
		}
		
		return false;
	}
	
	public String getPlayerColor(String username) {
		if (aprilFirstJokeRunning) {
			String color = aprilFirstColors.get(username.toLowerCase());
			if (color == null) {
				Set<String> keys = colorLookup.keySet();
				String[] colors = keys.toArray(new String[keys.size()]);
				aprilFirstColors.put(username.toLowerCase(), color = colors[rnd.nextInt(colors.length)]);
			}
			return color;
		}
		
		return username == null ? null : data.get(username.toLowerCase(), "color");
	}
	
	/**
	 * Get the vMC color code for a named color.
	 * 
	 * @param color The named color.
	 * @return The two characters that represent this color in vMC.
	 */
	public String getColorChar(String color) {
		return colorLookup.get(color.toLowerCase());
	}
	
	public boolean isValidColor(String color) {
		return color.equalsIgnoreCase("off") || colorLookup.containsKey(color.toLowerCase());
	}

	public String[] getColors() {
		return colorLookup.keySet().toArray(new String[colorLookup.size()]);
	}
	
	public boolean isBannedUser(String username) {
		return bannedUsers.contains(username.toLowerCase().trim());
	}
	
	public String getAlias(String username) {
		if (aprilFirstJokeRunning) {
			Set<String> whitelist = MinecraftServer.getServer().getConfigurationManager().getWhiteListedPlayers();
			String[] whitelistNames = whitelist.toArray(new String[whitelist.size()]);
			
			if (whitelistNames.length > 0)
				return whitelistNames[rnd.nextInt(whitelistNames.length)];
		}
		
		return username == null ? null : data.get(username.toLowerCase().trim(), "alias");
	}
	
	public boolean setAlias(String username, String alias) {
		alias = alias.trim();
		
		if (alias.length() < 1 || alias.length() > 16)
			return false;
		
		MinecraftServer.getServer().logger.info("Set alias for " + username + " to " + alias);
		data.set(username.toLowerCase().trim(), "alias", alias);
		data.saveSettings(this);
		ChatAPI.setAlias(username, alias);
		return true;
	}
	
	public boolean removeAlias(String username) {
		if (data.removeKey(username.toLowerCase().trim(), "alias")) {
			MinecraftServer.getServer().logger.info("Removed alias for " + username);
			data.saveSettings(this);
			ChatAPI.refreshAlias(username);
			return true;
		}
		
		return false;
	}
	
	public boolean hasAlias(String username) {
		return getAlias(username) != null;
	}
	
	public boolean addIgnore(String username, String ignoredUsername, long minutes) {
		if (minutes <= 0)
			return false;
		
		data.setLong(username.toLowerCase().trim(), IGNORE_PREFIX + ignoredUsername.toLowerCase().trim(), System.currentTimeMillis() + (minutes * 60 * 1000));
		data.saveSettings(this);
		return true;
	}
	
	public boolean isIgnoring(String username, String ignoredUsername) {
		long time = getIgnoreTime(username, ignoredUsername);
		return time > 0 && time > System.currentTimeMillis();
	}
	
	public long getIgnoreTime(String username, String ignoredUsername) {
		return data.getLong(username.toLowerCase().trim(), IGNORE_PREFIX + ignoredUsername.toLowerCase().trim(), -1L);
	}
	
	public boolean removeIgnore(String username, String ignoredUsername) {
		if (data.removeKey(username.toLowerCase().trim(), IGNORE_PREFIX + ignoredUsername.toLowerCase().trim())) {
			data.saveSettings(this);
			return true;
		}
		
		return false;
	}
	
	public List<String> getIgnores(String username) {
		Set<CaselessKey> keys = data.getSectionKeys(username.toLowerCase().trim());
		ArrayList<String> ignoredUsers = new ArrayList<String>();
		
		if (keys != null) {
			for (CaselessKey key : keys) {
				if (key.key.startsWith(IGNORE_PREFIX)) {
					String ignored = key.key.substring(IGNORE_PREFIX.length());
					if (isIgnoring(username, ignored))
						ignoredUsers.add(ignored);
				}
			}
		}
		
		return ignoredUsers;
	}
	
	public void sendIgnoreList(EntityPlayer player, boolean showWhenEmpty) {
		List<String> ignored = getIgnores(player.username);
		if (ignored.size() > 0) {
			String header = Util.COLOR_YELLOW + "You are ignoring: " + Util.COLOR_WHITE;
			
			List<String> messages = Util.combineIntoMaxLengthMessages(ignored, Packet3Chat.maxChatLength, ", ", true);
			
			if (messages.size() == 1 && messages.get(0).length() + header.length() <= Packet3Chat.maxChatLength) {
				player.sendChatToPlayer(header + messages.get(0));
			}
			else {
				player.sendChatToPlayer(header);
				for (String message : messages) {
					player.sendChatToPlayer(message);
				}
			}
		}
		else if (showWhenEmpty) {
			player.sendChatToPlayer(Util.COLOR_YELLOW + "You are not ignoring any players.");
		}
	}

	@Override
	public void onPlayerChatAction(PlayerChatEvent event) {
		if (event.type == PlayerChatEvent.TYPE.HANDLE_GLOBAL || event.type == PlayerChatEvent.TYPE.HANDLE_EMOTE) {
			
			String username = ChatAPI.getUsernameAliased(event.player.username);
			if (username == null)
				username = event.player.username;
			
			// Attempt to get the user's setting.
			String color = getPlayerColor(event.player.username);
			
			if (color != null)
				color = getColorChar(color);
			
			event.setMessage(String.format(event.type == PlayerChatEvent.TYPE.HANDLE_GLOBAL ? globalMessageFormat : emoteMessageFormat,
				color == null
					? username
					: color + username + Util.COLOR_WHITE,
				event.getMessage()
			));
			
			event.sendAsGlobalMessage();
		}
		else if (event.type == PlayerChatEvent.TYPE.GLOBAL) {
			chatRestoreBuffer.add(new ValuePair(event.originalMessage, new Long(System.currentTimeMillis())));
			
			if (chatRestoreBuffer.size() > chatRestoreLines)
				chatRestoreBuffer.pollFirst();
		}
		else if (event.type == PlayerChatEvent.TYPE.SEND_TO_PLAYER_ATTEMPT) {
			if (isIgnoring(event.getTargetPlayer().username, event.player.username) || isIgnoring(event.getTargetPlayer().username, ChatAPI.getUsernameAliased(event.player.username))) {
				event.markNotAllowed();
			}
		}
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		long currentTimeMillis = System.currentTimeMillis();
		
		if (event.getType() == PlayerInstanceEvent.TYPE.LOGIN) {
			String userKey = event.getPlayerInstance().username.toLowerCase();
			
			Long loginSessionStart = loginTime.get(userKey);
			Long lastLogout = logoutTime.get(userKey);
			
			// Reset the login time if they've been logged out longer than the restore timeout.
			if (loginTime == null || lastLogout == null || lastLogout.longValue() < currentTimeMillis - chatRestoreTimeout * 1000L) {
				loginTime.put(userKey, loginSessionStart = new Long(currentTimeMillis));
			}
			else {
				for (ValuePair<String, Long> message : chatRestoreBuffer) {
					if (message.value.longValue() >= loginSessionStart)
						event.getPlayerInstance().sendChatToPlayer(message.key);
				}
			}
			
			sendIgnoreList(event.getPlayerInstance(), false);
				
		}
		else if (event.getType() == PlayerInstanceEvent.TYPE.LOGOUT_POST) {
			if (aprilFirstJokeRunning) {
				ChatAPI.removeAlias(event.getPlayerInstance().username);
				aprilFirstColors.remove(event.getPlayerInstance().username.toLowerCase());
			}
			
			logoutTime.put(event.getPlayerInstance().username.toLowerCase(), new Long(System.currentTimeMillis()));
		}
	}

	@Override
	public void onPlayerAliasAction(PlayerAliasEvent event) {
		String alias = getAlias(event.username);
		if (alias != null)
			event.alias = alias;
	}

	@Override
	public void onTick(TickEvent event) {
		if (event.getType() == TickEvent.TYPE.START && aprilFirstJoke && event.getTickCounter() % (20 * 1) == 0) {
			long now = System.currentTimeMillis();
			boolean inRange = now >= aprilFirstStart && now <= aprilFirstEnd;
			if (!aprilFirstJokeRunning && inRange) {
				ModLoader.outputInfo("April fools chat aliasing beginning...");
				aprilFirstJokeRunning = true;
				ChatAPI.refreshAllAliases();
			}
			else if (aprilFirstJokeRunning && !inRange) {
				ModLoader.outputInfo("April fools chat aliasing ending...");
				aprilFirstJokeRunning = false;
				ChatAPI.refreshAllAliases();
			}
		}
	}
}
