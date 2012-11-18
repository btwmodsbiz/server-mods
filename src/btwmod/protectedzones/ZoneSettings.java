package btwmod.protectedzones;

import btwmods.io.Settings;
import btwmods.util.Area;
import btwmods.util.Cube;

public class ZoneSettings {
	public final String name;
	
	public final boolean isCube;
	
	public final int x1;
	public final int y1;
	public final int z1;
	public final int x2;
	public final int y2;
	public final int z2;
	
	public ZoneSettings(String name, int x1, int z1, int x2, int z2) {
		this.name = name;
		isCube = false;
		this.x1 = x1;
		this.y1 = 0;
		this.z1 = z1;
		this.x2 = x2;
		this.y2 = 0;
		this.z2 = z2;
	}
	
	public ZoneSettings(String name, int x1, int y1, int z1, int x2, int y2, int z2) {
		this.name = name;
		isCube = true;
		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
	}
	
	public ZoneSettings(Settings settings) {
		name = settings.get("name");
		isCube = settings.getBoolean("isCube", false);
		
		x1 = settings.getInt("x1", 0);
		z1 = settings.getInt("z1", 0);
		x2 = settings.getInt("x2", x1 - 1); // Hint: -1 marks this value as invalid.
		z2 = settings.getInt("z2", z1 - 1);
		
		y1 = isCube ? settings.getInt("y1", 0) : 0;
		y2 = isCube ? settings.getInt("y2", y1 - 1) : y1 - 1;
	}
	
	public boolean isValid() {
		return isValidName(name) && x1 <= x2 && z1 <= z2 && (!isCube || y1 <= y2);
	}
	
	public static boolean isValidName(String name) {
		return name != null && name.matches("^[A-Za-z0-9_\\-]+$");
	}
	
	public Area<ZoneSettings> toArea() {
		return isCube
			? new Cube<ZoneSettings>(x1, y1, z1, x2, y2, z2, this)
			: new Area<ZoneSettings>(x1, z1, x2, z2, this);
	}
	
	public void saveToSettings(Settings settings, String section) {
		settings.set(section, "name", name);
		
		settings.setInt(section, "x1", x1);
		settings.setInt(section, "z1", z1);
		settings.setInt(section, "x2", x2);
		settings.setInt(section, "z2", z2);
		
		settings.setBoolean(section, "isCube", isCube);
		
		if (isCube) {
			settings.setInt(section, "y1", y1);
			settings.setInt(section, "y2", y2);
		}
		else {
			settings.removeKey(section, "y1");
			settings.removeKey(section, "y2");
		}
	}
}