package btwmod.protectedzones;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import btwmods.io.Settings;

public class ZoneWhitelist extends HashSet<String> {
	
	public ZoneWhitelist() {
		
	}
	
	public ZoneWhitelist(Settings settings, String section) {
		if (settings != null) {
			String list = settings.get(section, "whitelist");
			
			if (list == null)
				list = settings.get(section, "allowedPlayers");
			
			if (list != null) {
				String[] split = list.split(";");
				for (String player : split) {
					if (!player.trim().equals("")) {
						add(player);
					}
				}
			}
		}
	}
	
	@Override
	public boolean add(String username) {
		return super.add(username.trim().toLowerCase());
	}

	@Override
	public boolean contains(Object username) {
		if (username != null && username.getClass() == String.class)
			return super.contains(username.toString().trim().toLowerCase());
		
		return false;
	}

	@Override
	public boolean remove(Object username) {
		if (username != null && username.getClass() == String.class)
			return super.remove(username.toString().trim().toLowerCase());
		
		return false;
	}
	
	public void saveToSettings(Settings settings, String section) {
		if (settings != null) {
			StringBuilder sb = new StringBuilder();
			for (String player : this) {
				if (sb.length() > 0) sb.append(";");
				sb.append(player);
			}
			settings.set(section, "whitelist", sb.toString());
		}
	}
	
	public List<String> asList() {
		return Arrays.asList(toArray(new String[size()]));
	}
}
