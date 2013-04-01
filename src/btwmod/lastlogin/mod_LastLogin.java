package btwmod.lastlogin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerInstanceListener;
import btwmods.player.PlayerInstanceEvent;
import btwmods.util.CaselessKey;

public class mod_LastLogin implements IMod, IPlayerInstanceListener {
	
	private static final String SECTION_NAME = "LastLogin";
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMMM d, yyyy");
	private static final SimpleDateFormat timezoneFormat = new SimpleDateFormat("zzz (Z)");
	
	private int failures = 0;
	private int maxLastLogins = 30;
	private File lastLoginFile = new File(ModLoader.modDataDir, "lastlogin.txt");
	private Settings data;
	
	private Map<String, Long> lastLogin = new HashMap<String, Long>(); 
	private LastLoginComparator comparator = new LastLoginComparator();

	@Override
	public String getName() {
		return "Last Login";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		this.data = data;
		
		if (settings.hasKey("lastLoginFile"))
			lastLoginFile = new File(settings.get("lastLoginFile"));
		
		maxLastLogins = settings.getInt("maxLastLogins", maxLastLogins);

		Set<CaselessKey> keys = data.getSectionKeys(SECTION_NAME);
		if (keys != null) {
			for (CaselessKey key : keys) {
				long time = data.getLong(SECTION_NAME, key.key, -1);
				if (data.isLong(SECTION_NAME, key.key)) {
					lastLogin.put(key.key, Long.valueOf(time));
				}
			}
		}
		
		PlayerAPI.addListener(this);
		
		save();
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerInstanceAction(PlayerInstanceEvent event) {
		if (lastLoginFile == null)
			return;
		
		if (event.getType() == PlayerInstanceEvent.TYPE.LOGIN) {
			long time = System.currentTimeMillis();
			lastLogin.put(event.getPlayerInstance().username, time);
			data.setLong(SECTION_NAME, event.getPlayerInstance().username, time);
			data.saveSettings(this);
			save();
		}
	}
	
	private void save() {
		JsonObject json = new JsonObject();
		Date now = new Date();
		json.addProperty("updated", dateFormat.format(now));
		json.addProperty("timezone", timezoneFormat.format(now));
		json.addProperty("max", maxLastLogins);
		
		JsonArray players = new JsonArray();
		
		// Sort list of last logins.
		Set<Entry<String, Long>> entriesSet = lastLogin.entrySet();
		List<Entry<String, Long>> entries = new ArrayList<Entry<String, Long>>();
		entries.addAll(entriesSet);
		Collections.sort(entries, comparator);
		
		for (Entry<String, Long> entry : entries) {
			JsonObject player = new JsonObject();
			player.addProperty("name", entry.getKey());
			player.addProperty("date", dateFormat.format(new Date(entry.getValue().longValue())));
			players.add(player);
			
			if (players.size() >= maxLastLogins)
				break;
		}
		
		json.add("players", players);
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(lastLoginFile, false));
			writer.write(json.toString());
			
		} catch (IOException e) {
			ModLoader.outputError(e, getName() + " failed to save:" + e.getMessage(), Level.SEVERE);
			failures++;
			
			if (failures >= 10) {
				ModLoader.outputError(e, getName() + " failed to save 10 times and has been disabled.", Level.SEVERE);
				try {
					lastLoginFile = null;
					unload();
				} catch (Exception e2) {
					
				}
			}
		}
		
		finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					
				}
			}
		}
	}
	
	private class LastLoginComparator implements Comparator<Entry<String, Long>> {

		@Override
		public int compare(Entry<String, Long> a, Entry<String, Long> b) {
			return b.getValue().compareTo(a.getValue());
		}
		
	}
}
