package btwmod.chat;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import btwmods.CommandsAPI;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.Util;
import btwmods.io.Settings;
import btwmods.player.IPlayerChatListener;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerChatEvent;
import btwmods.player.PlayerInstanceEvent;
import btwmods.util.CaselessKey;
import btwmods.util.ValuePair;

public class mod_Chat implements IMod, IPlayerChatListener, IPlayerInstanceListener {

	public String globalMessageFormat = "<%1$s> %2$s";
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
	public static final String IGNORE_PREFIX = "ignore_";
	
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
		
		PlayerAPI.addListener(this);
		CommandsAPI.registerCommand(commandChatColor = new CommandChatColor(this), this);
		CommandsAPI.registerCommand(commandChatAlias = new CommandChatAlias(this), this);
		CommandsAPI.registerCommand(commandIgnore = new CommandIgnore(this), this);
	}
	
	private void addColor(String color, String colorCode) {
		if (!bannedColors.contains(color)) {
			colorLookup.put(color, colorCode);
		}
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
		CommandsAPI.unregisterCommand(commandChatColor);
		CommandsAPI.unregisterCommand(commandChatAlias);
		CommandsAPI.unregisterCommand(commandIgnore);
	}

	@Override
	public IMod getMod() {
		return this;
	}
	
	public boolean setPlayerColor(String username, String color) throws IOException {
		if (username == null | color == null) {
			return false;
		}
		else if (color.equalsIgnoreCase("off") || color.equalsIgnoreCase("white") || isBannedUser(username)) {
			if (data.removeKey(username.toLowerCase(), "color")) {
				data.saveSettings();
				return true;
			}
		}
		else if (isValidColor(color)) {
			data.set(username.toLowerCase(), "color", color.toLowerCase());
			data.saveSettings();
			return true;
		}
		
		return false;
	}
	
	public String getPlayerColor(String username) {
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
		return username == null ? null : data.get(username.toLowerCase().trim(), "alias");
	}
	
	public boolean setAlias(String username, String alias) throws IOException {
		alias = alias.trim();
		
		if (alias.length() < 1 || alias.length() > 16)
			return false;
		
		data.set(username.toLowerCase().trim(), "alias", alias);
		data.saveSettings();
		return true;
	}
	
	public boolean removeAlias(String username) throws IOException {
		if (data.removeKey(username.toLowerCase().trim(), "alias")) {
			data.saveSettings();
			return true;
		}
		
		return false;
	}
	
	public boolean hasAlias(String username) {
		return getAlias(username) != null;
	}
	
	public boolean addIgnore(String username, String ignoredUsername, long minutes) throws IOException {
		if (minutes <= 0)
			return false;
		
		data.setLong(username.toLowerCase().trim(), IGNORE_PREFIX + ignoredUsername.toLowerCase().trim(), System.currentTimeMillis() + (minutes * 1000));
		data.saveSettings();
		return true;
	}
	
	public boolean isIgnoring(String username, String ignoredUsername) {
		return getIgnoreTime(username, ignoredUsername) > 0;
	}
	
	public long getIgnoreTime(String username, String ignoredUsername) {
		return data.getLong(username.toLowerCase().trim(), IGNORE_PREFIX + ignoredUsername.toLowerCase().trim(), -1L);
	}
	
	public boolean removeIgnore(String username, String ignoredUsername) throws IOException {
		if (data.removeKey(username.toLowerCase().trim(), IGNORE_PREFIX + ignoredUsername.toLowerCase().trim())) {
			data.saveSettings();
			return true;
		}
		
		return false;
	}
	
	public List<String> getIgnores(String username) {
		Set<CaselessKey> keys = data.getSectionKeys(username.toLowerCase().trim());
		
		if (keys != null) {
			ArrayList<String> ignoredUsers = new ArrayList<String>();
			for (CaselessKey key : keys) {
				if (key.key.startsWith(IGNORE_PREFIX)) {
					ignoredUsers.add(key.key.substring(IGNORE_PREFIX.length()));
				}
			}
			return ignoredUsers;
		}
		
		return null;
	}

	@Override
	public void onPlayerChatAction(PlayerChatEvent event) {
		if (event.type == PlayerChatEvent.TYPE.HANDLE_GLOBAL) {
			
			String username = getAlias(event.player.username);
			if (username == null)
				username = event.player.username;
			
			// Attempt to get the user's setting.
			String color = data.get(event.player.username.toLowerCase(), "color");
			
			if (color != null)
				color = getColorChar(color);
			
			event.setMessage(String.format(globalMessageFormat,
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
		}
		else if (event.getType() == PlayerInstanceEvent.TYPE.LOGOUT) {
			logoutTime.put(event.getPlayerInstance().username.toLowerCase(), new Long(System.currentTimeMillis()));
		}
	}
}
