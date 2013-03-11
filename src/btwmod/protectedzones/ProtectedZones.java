package btwmod.protectedzones;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import btwmods.util.Area;
import btwmods.util.Zones;

public class ProtectedZones extends Zones<Zone> {

	private Map<String, Zone> byName = new LinkedHashMap<String, Zone>();
	
	public boolean containsZone(String name) {
		return byName.containsKey(name.toLowerCase());
	}
	
	public boolean containsZone(Zone zone) {
		return byName.containsKey(zone.name.toLowerCase());
	}

	public Zone getZone(String name) {
		return byName.get(name.toLowerCase());
	}
	
	public List<String> getZoneNames() {
		List<String> list = new ArrayList<String>();
		for (Entry<String, Zone> entry : byName.entrySet()) {
			list.add(entry.getValue().name);
		}
		return list;
	}
	
	public List<Zone> getZones() {
		List<Zone> list = new ArrayList<Zone>();
		for (Entry<String, Zone> entry : byName.entrySet()) {
			list.add(entry.getValue());
		}
		return list;
	}

	@Override
	public boolean add(Area<Zone> area) {
		if (containsZone(area.data) && super.add(area)) {
			return true;
		}
		
		return false;
	}
	
	public boolean add(Zone zone) {
		if (!containsZone(zone)) {
			zone.setProtectedZones(this);
			byName.put(zone.name.toLowerCase(), zone);
			addAll(zone.areas);
			return true;
		}
		
		return false;
	}
	
	public boolean removeZone(Zone zone) {
		return zone != null && removeZone(zone.name);
	}
	
	public boolean removeZone(String name) {
		Zone removed;
		if (name != null && (removed = byName.remove(name.toLowerCase())) != null) {
			removed.setProtectedZones(null);
			removeAll(removed.areas);
			return true;
		}
		
		return false;
	}

	@Override
	public void clear() {
		super.clear();
		for (Entry<String, Zone> entry : byName.entrySet()) {
			entry.getValue().setProtectedZones(null);
		}
		byName.clear();
	}
}
