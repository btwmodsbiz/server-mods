package btwmod.protectedzones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import btwmods.Util;
import btwmods.io.Settings;
import btwmods.util.Area;
import btwmods.util.Cube;

public class Zone {
	
	private final List<Area<Zone>> _areas = new ArrayList<Area<Zone>>();
	public final List<Area<Zone>> areas = Collections.unmodifiableList(_areas);
	
	public final String name;
	public final int dimension;
	
	public final ZonePermissions permissions;
	public final ZoneWhitelist whitelist;
	
	private ProtectedZones protectedZones = null;
	
	public Zone(String name, int dimension) throws IllegalArgumentException {
		if (!isValidName(name))
			throw new IllegalArgumentException("name");
		
		if (Util.getWorldNameFromDimension(dimension) == null)
			throw new IllegalArgumentException("dimension");
		
		this.name = name;
		this.dimension = dimension;
		permissions = new ZonePermissions();
		whitelist = new ZoneWhitelist();
	}
	
	public Zone(Settings settings, String section) throws IllegalArgumentException {
		name = settings.get(section, "name");
		dimension = settings.getInt(section, "dimension", 0);
		
		if (!isValidName(name))
			throw new IllegalArgumentException("name");
		
		if (Util.getWorldNameFromDimension(dimension) == null)
			throw new IllegalArgumentException("dimension");
		
		if (settings.isBoolean(section, "isCube")) {
			boolean isCube = settings.getBoolean(section, "isCube", false);
			
			int x1 = settings.getInt(section, "x1", 0);
			int y1 = settings.getInt(section, "y1", 0);
			int z1 = settings.getInt(section, "z1", 0);
			
			int x2 = settings.getInt(section, "x2", 0);
			int y2 = settings.getInt(section, "y2", 0);
			int z2 = settings.getInt(section, "z2", 0);
			
			if (isCube)
				addCube(x1, y1, z1, x2, y2, z2);
			else
				addArea(x1, z1, x2, z2);
		}

		int areaCount = settings.getInt(section, "areaCount", 0);
		for (int i = 1; i <= areaCount; i++) {
			boolean isCube = settings.getBoolean(section, "area" + i + "_isCube", false);
			
			int x1 = settings.getInt(section, "area" + i + "_x1", 0);
			int y1 = settings.getInt(section, "area" + i + "_y1", 0);
			int z1 = settings.getInt(section, "area" + i + "_z1", 0);
			
			int x2 = settings.getInt(section, "area" + i + "_x2", 0);
			int y2 = settings.getInt(section, "area" + i + "_y2", 0);
			int z2 = settings.getInt(section, "area" + i + "_z2", 0);
			
			if (isCube)
				addCube(x1, y1, z1, x2, y2, z2);
			else
				addArea(x1, z1, x2, z2);
		}
		
		permissions = new ZonePermissions(settings, section);
		whitelist = new ZoneWhitelist(settings, section);
	}
	
	public Zone(Zone zone, String newName) {
		if (!isValidName(newName))
			throw new IllegalArgumentException("name");
		
		name = newName;
		dimension = zone.dimension;
		
		for (Area<Zone> area : zone._areas) {
			_areas.add(area.clone(this));
		}
		
		whitelist = (ZoneWhitelist)zone.whitelist.clone();
		permissions = (ZonePermissions)zone.permissions.clone();
	}
	
	public static boolean isValidName(String name) {
		return name != null && name.matches("^[A-Za-z0-9_\\-]{1,25}$");
	}
	
	public void setProtectedZones(ProtectedZones protectedZones) {
		if (protectedZones != null && this.protectedZones != null)
			throw new IllegalStateException();
		
		this.protectedZones = protectedZones;
	}
	
	public Area<Zone> addArea(int x1, int z1, int x2, int z2) {
		int tmp;
		
		if (x1 > x2) {
			tmp = x2;
			x2 = x1;
			x1 = tmp;
		}
		
		if (z1 > z2) {
			tmp = z2;
			z2 = z1;
			z1 = tmp;
		}
		
		Area<Zone> newArea = new Area<Zone>(x1, z1, x2, z2, this);

		return addArea(newArea) ? newArea : null;
	}
	
	public Cube<Zone> addCube(int x1, int y1, int z1, int x2, int y2, int z2) {
		int tmp;
		
		if (x1 > x2) {
			tmp = x2;
			x2 = x1;
			x1 = tmp;
		}
		
		if (z1 > z2) {
			tmp = z2;
			z2 = z1;
			z1 = tmp;
		}
		
		if (y1 > y2) {
			tmp = y2;
			y2 = y1;
			y1 = tmp;
		}
		
		Cube<Zone> newCube = new Cube<Zone>(x1, y1, z1, x2, y2, z2, this);

		return addArea(newCube) ? newCube : null;
	}
	
	private boolean addArea(Area area) {
		if (!_areas.contains(area) && _areas.add(area)) {
			
			if (protectedZones != null)
				protectedZones.add(area);
			
			return true;
		}
		
		return false;
	}
	
	public Area removeArea(int index) {
		Area area = null;
		if (index >= 0 && index < _areas.size() && (area = _areas.remove(index)) != null && protectedZones != null) {
			protectedZones.remove(area);
		}
		return area;
	}
	
	public boolean isPlayerAllowed(String username, Permission permission) {
		if (permission == Permission.WHITELIST)
			return whitelist.contains(username);
		
		return permission == Permission.ON;
	}
	
	public void saveToSettings(Settings settings, String section) {
		settings.set(section, "name", name);
		settings.setInt(section, "dimension", dimension);
		
		settings.setInt(section, "areaCount", _areas.size());
		for (int i = 1; i <= _areas.size(); i++) {
			Area area = _areas.get(i - 1);
			settings.setInt(section, "area" + i + "_x1", area.x1);
			settings.setInt(section, "area" + i + "_z1", area.z1);
			settings.setInt(section, "area" + i + "_x2", area.x2);
			settings.setInt(section, "area" + i + "_z2", area.z2);
			
			if (area instanceof Cube) {
				Cube cube = (Cube)area;
				settings.setBoolean(section, "area" + i + "_isCube", true);
				settings.setInt(section, "area" + i + "_y1", cube.y1);
				settings.setInt(section, "area" + i + "_y2", cube.y2);
			}
			else {
				settings.removeKey(section, "area" + i + "_isCube");
				settings.removeKey(section, "area" + i + "_y1");
				settings.removeKey(section, "area" + i + "_y2");
			}
		}
		
		permissions.saveToSettings(settings, section);
		whitelist.saveToSettings(settings, section);
	}
	
	public int getAreaIndex(Area area) {
		if (area.data == this) {
			for (int i = 0; i < areas.size(); i++) {
				Area item = areas.get(i);
				if (item.equals(area))
					return i;
			}
		}
		
		return -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dimension;
		result = prime * result + ((name == null) ? 0 : name.toLowerCase().hashCode());
		return result;
	}
}
