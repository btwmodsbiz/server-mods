package btwmod.protectedzones;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import btwmods.util.Area;
import btwmods.util.Zones;

public class ProtectedZones extends Zones<ZoneSettings> {

	private Map<String, ZoneSettings> byName = new LinkedHashMap<String, ZoneSettings>();
	
	public boolean containsZone(String name) {
		return byName.containsKey(name.toLowerCase());
	}
	
	public boolean containsZone(ZoneSettings zone) {
		return byName.containsKey(zone.name.toLowerCase());
	}

	public ZoneSettings getZone(String name) {
		return byName.get(name.toLowerCase());
	}
	
	public List<String> getZoneNames() {
		List<String> list = new ArrayList<String>();
		for (Entry<String, ZoneSettings> entry : byName.entrySet()) {
			list.add(entry.getValue().name);
		}
		return list;
	}
	
	public List<ZoneSettings> getZones() {
		List<ZoneSettings> list = new ArrayList<ZoneSettings>();
		for (Entry<String, ZoneSettings> entry : byName.entrySet()) {
			list.add(entry.getValue());
		}
		return list;
	}

	@Override
	public boolean add(Area<ZoneSettings> area) {
		if (containsZone(area.data) && super.add(area)) {
			return true;
		}
		
		return false;
	}
	
	public boolean add(ZoneSettings zone) {
		if (!containsZone(zone)) {
			byName.put(zone.name.toLowerCase(), zone);
			addAll(zone.areas);
			return true;
		}
		
		return false;
	}
	
	public boolean removeZone(ZoneSettings zone) {
		return zone != null && removeZone(zone.name);
	}
	
	public boolean removeZone(String name) {
		ZoneSettings removed;
		if (name != null && (removed = byName.remove(name.toLowerCase())) != null) {
			removeAll(removed.areas);
			return true;
		}
		
		return false;
	}

	@Override
	public void clear() {
		super.clear();
		byName.clear();
	}
}
