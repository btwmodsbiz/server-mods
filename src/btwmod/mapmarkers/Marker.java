package btwmod.mapmarkers;

import btwmods.Util;
import btwmods.io.Settings;

public class Marker {
	
	public enum TYPE {
		HOME, POINT;
		
		public static String[] tabCompletion = new String[] { "home" };
		
		public String asHumanReadable() {
			return this.toString().toLowerCase();
		}
	};
	
	public final String username;
	public final int markerIndex;
	public final TYPE type;

	public final int dimension;
	public final int x;
	public final int y;
	public final int z;
	
	private String description = null;
	
	public void setDescription(String description) {
		if (description != null && description.trim().length() == 0)
			description = null;
		
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Marker(String username, int markerIndex, TYPE type, int dimension, int x, int z) {
		this(username, markerIndex, type, dimension, x, -1, z);
	}
	
	public Marker(String username, int markerIndex, TYPE type, int dimension, int x, int y, int z) {
		this.username = username;
		this.markerIndex = markerIndex;
		this.type = type;
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void toSettings(Settings data, String section) {
		data.set(section, "username", username);
		data.setInt(section, "markerIndex", markerIndex);
		data.set(section, "type", type.toString());
		data.setInt(section, "dimension", dimension);
		data.setInt(section, "x", x);
		data.setInt(section, "y", y);
		data.setInt(section, "z", z);
		
		if (description == null)
			data.removeKey(section, "description");
		else
			data.set(section, "description", description);
	}
	
	public static Marker fromSettings(Settings data, String section) {
		String username = data.get(section, "username");
		
		if (username != null && username.trim().length() != 0
			&& data.hasKey(section, "type")
			&& data.isInt(section, "markerIndex")
			&& data.isInt(section, "dimension")
			&& data.isInt(section, "x")
			&& data.isInt(section, "z")) {
			
			TYPE type;
			try {
				type = TYPE.valueOf(data.get(section, "type"));
			}
			catch (IllegalArgumentException e) {
				return null;
			}
			
			int markerIndex = data.getInt(section, "markerIndex", 0);
			int dimension = data.getInt(section, "dimension", 0);
			
			if (markerIndex >= 0 && Util.getWorldNameFromDimension(dimension) != null) {
				int x = data.getInt(section, "x", 0);
				int y = Math.max(-1, data.getInt(section, "y", -1));
				int z = data.getInt(section, "z", 0);
				
				Marker newMarker = new Marker(username, markerIndex, type, dimension, x, y, z);
				newMarker.setDescription(data.get(section, "description"));
				
				return newMarker;
			}
		}
		
		return null;
	}
}
