package btwmod.protectedzones;

import java.util.ArrayList;
import java.util.List;

import btwmods.io.Settings;

public class ZonePermissions {
	public static final String[] settings = {
		"protectEdits",
		"allowDoors",
		"allowContainers",
		"allowHeads",
		"allowOps",
		
		"protectEntities",
		"allowMooshroom",
		"allowVillagers",
		
		"protectExplosions",
		"protectBurning"
	};
	
	public Permission protectEdits = Permission.OFF;
	public Permission allowDoors = Permission.ON;
	public Permission allowContainers = Permission.OFF;
	public Permission allowHeads = Permission.OFF;
	public boolean allowOps = false;
	
	public Permission protectEntities = Permission.OFF;
	public boolean allowMooshroom = false;
	public boolean allowVillagers = false;
	
	public boolean protectExplosions = false;
	public boolean protectBurning = false;
	
	public boolean sendDebugMessages = false;
	
	public ZonePermissions() {
		
	}
	
	public ZonePermissions(Settings settings, String section) {
		if (settings != null) {
			// Old style protectBlocks
			if (settings.getBoolean(section, "protectBlocks", false)) {
				protectEdits = Permission.ON;
				protectExplosions = true;
				protectBurning = true;
			}
			
			protectEdits = settings.getEnum(Permission.class, section, "protectEdits", protectEdits);
			allowOps = settings.getBoolean(section, "allowOps", allowOps);
			allowDoors = settings.getEnum(Permission.class, section, "allowDoors", allowDoors);
			allowContainers = settings.getEnum(Permission.class, section, "allowContainers", allowContainers);
			allowHeads = settings.getEnum(Permission.class, section, "allowHeads", allowHeads);
			
			protectEntities = settings.getEnum(Permission.class, section, "protectEntities", protectEntities);
			allowMooshroom = settings.getBoolean(section, "allowMooshroom", allowMooshroom);
			allowVillagers = settings.getBoolean(section, "allowVillagers", allowVillagers);
			
			protectExplosions = settings.getBoolean(section, "protectExplosions", protectExplosions);
			protectBurning = settings.getBoolean(section, "protectBurning", protectBurning);
		}
	}
	
	public boolean set(String name, String value) {
		if (name.equalsIgnoreCase("protectEdits") && Settings.isEnumValue(Permission.class, value.toUpperCase())) {
			protectEdits = Settings.getEnumValue(Permission.class, value.toUpperCase(), protectEdits);
		}
		else if (name.equalsIgnoreCase("allowDoors") && Settings.isEnumValue(Permission.class, value.toUpperCase())) {
			allowDoors = Settings.getEnumValue(Permission.class, value.toUpperCase(), allowDoors);
		}
		else if (name.equalsIgnoreCase("allowOps") && Settings.isBooleanValue(value)) {
			allowOps = Settings.getBooleanValue(value, allowOps);
		}
		else if (name.equalsIgnoreCase("allowContainers") && Settings.isEnumValue(Permission.class, value.toUpperCase())) {
			allowContainers = Settings.getEnumValue(Permission.class, value.toUpperCase(), allowContainers);
		}
		else if (name.equalsIgnoreCase("allowHeads") && Settings.isEnumValue(Permission.class, value.toUpperCase())) {
			allowHeads = Settings.getEnumValue(Permission.class, value.toUpperCase(), allowHeads);
		}
		
		else if (name.equalsIgnoreCase("protectEntities") && Settings.isEnumValue(Permission.class, value.toUpperCase())) {
			protectEntities = Settings.getEnumValue(Permission.class, value.toUpperCase(), protectEntities);
		}
		else if (name.equalsIgnoreCase("allowMooshroom") && Settings.isBooleanValue(value)) {
			allowMooshroom = Settings.getBooleanValue(value, allowMooshroom);
		}
		else if (name.equalsIgnoreCase("allowVillagers") && Settings.isBooleanValue(value)) {
			allowVillagers = Settings.getBooleanValue(value, allowVillagers);
		}
		
		else if (name.equalsIgnoreCase("protectBurning") && Settings.isBooleanValue(value)) {
			protectBurning = Settings.getBooleanValue(value, protectBurning);
		}
		
		else if (name.equalsIgnoreCase("protectExplosions") && Settings.isBooleanValue(value)) {
			protectExplosions = Settings.getBooleanValue(value, protectExplosions);
		}
		
		else if (name.equalsIgnoreCase("debug") && Settings.isBooleanValue(value)) {
			sendDebugMessages = Settings.getBooleanValue(value, sendDebugMessages);
		}
		else {
			return false;
		}
		
		return true;
	}
	
	public String get(String name) {
		if (name.equalsIgnoreCase("protectEdits")) {
			return protectEdits.toString().toLowerCase();
		}
		else if (name.equalsIgnoreCase("allowDoors")) {
			return allowDoors.toString().toLowerCase();
		}
		else if (name.equalsIgnoreCase("allowOps")) {
			return allowOps ? "on" : "off";
		}
		else if (name.equalsIgnoreCase("allowContainers")) {
			return allowContainers.toString().toLowerCase();
		}
		else if (name.equalsIgnoreCase("allowHeads")) {
			return allowHeads.toString().toLowerCase();
		}
		
		else if (name.equalsIgnoreCase("protectEntities")) {
			return protectEntities.toString().toLowerCase();
		}
		else if (name.equalsIgnoreCase("allowMooshroom")) {
			return allowMooshroom ? "on" : "off";
		}
		else if (name.equalsIgnoreCase("allowVillagers")) {
			return allowVillagers ? "on" : "off";
		}
		
		else if (name.equalsIgnoreCase("protectBurning")) {
			return protectBurning ? "on" : "off";
		}
		
		else if (name.equalsIgnoreCase("protectExplosions")) {
			return protectExplosions ? "on" : "off";
		}
		
		else if (name.equalsIgnoreCase("debug")) {
			return sendDebugMessages ? "on" : "off";
		}
		
		return null;
	}
	
	public void saveToSettings(Settings settings, String section) {
		settings.set(section, "protectEdits", protectEdits.toString());
		settings.setBoolean(section, "allowOps", allowOps);
		settings.set(section, "allowDoors", allowDoors.toString());
		settings.set(section, "allowContainers", allowContainers.toString());
		settings.set(section, "allowHeads", allowHeads.toString());
		
		settings.set(section, "protectEntities", protectEntities.toString());
		settings.setBoolean(section, "allowMooshroom", allowMooshroom);
		settings.setBoolean(section, "allowVillagers", allowVillagers);
		
		settings.setBoolean(section, "protectBurning", protectBurning);
		settings.setBoolean(section, "protectExplosions", protectExplosions);
	}

	public List<String> asList() {
		List<String> list = new ArrayList<String>();
		
		list.add("protectEdits(" + protectEdits.toString().toLowerCase() + ")");
		list.add("allowOps(" + (allowOps ? "on" : "off") + ")");
		list.add("allowDoors(" + allowDoors.toString().toLowerCase() + ")");
		list.add("allowContainers(" + allowContainers.toString().toLowerCase() + ")");
		list.add("allowHeads(" + allowHeads.toString().toLowerCase() + ")");
		
		list.add("protectEntities(" + (protectEntities.toString().toLowerCase()) + ")");
		list.add("allowMooshroom(" + (allowMooshroom ? "on" : "off") + ")");
		list.add("allowVillagers(" + (allowVillagers ? "on" : "off") + ")");
		
		list.add("protectBurning(" + (protectBurning ? "on" : "off") + ")");
		list.add("protectExplosions(" + (protectExplosions ? "on" : "off") + ")");
		
		if (sendDebugMessages)
			list.add("debug(on)");
		
		return list;
	}

	@Override
	protected Object clone() {
		ZonePermissions clone = new ZonePermissions();
		
		clone.protectEdits = protectEdits;
		clone.allowDoors = allowDoors;
		clone.allowContainers = allowContainers;
		clone.allowHeads = allowHeads;
		clone.allowOps = allowOps;
		
		clone.protectEntities = protectEntities;
		clone.allowMooshroom = allowMooshroom;
		clone.allowVillagers = allowVillagers;
		
		clone.protectExplosions = protectExplosions;
		clone.protectBurning = protectBurning;
		
		clone.sendDebugMessages = sendDebugMessages;
		
		return clone;
	}
}
