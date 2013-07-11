package btwmod.centralchat;

import btwmods.Util;

public enum ChatColors {

	BLACK(Util.COLOR_BLACK, "#000"),
	NAVY(Util.COLOR_NAVY, "#00A"),
	GREEN(Util.COLOR_GREEN, "#0A0"),
	TEAL(Util.COLOR_TEAL, "#0AA"),
	MAROON(Util.COLOR_MAROON, "#A00"),
	PURPLE(Util.COLOR_PURPLE, "#A0A"),
	GOLD(Util.COLOR_GOLD, "#FA0"),
	SILVER(Util.COLOR_SILVER, "#AAA"),
	GREY(Util.COLOR_GREY, "#555"),
	BLUE(Util.COLOR_BLUE, "#55F"),
	LIME(Util.COLOR_LIME, "#5F5"),
	AQUA(Util.COLOR_AQUA, "#5FF"),
	RED(Util.COLOR_RED, "#F55"),
	PINK(Util.COLOR_PINK, "#F5F"),
	YELLOW(Util.COLOR_YELLOW, "#FF5"),
	WHITE(Util.COLOR_WHITE, "#FFF", true),
	OFF(Util.COLOR_WHITE, "#FFF", true);
	
	public final String colorChar;
	public final String colorHex;
	public final boolean isDefault;
	
	private ChatColors(String colorChar, String colorHex, boolean isDefault) {
		this.colorChar = colorChar;
		this.colorHex = colorHex;
		this.isDefault = isDefault;
	}
	
	private ChatColors(String colorChar, String colorHex) {
		this(colorChar, colorHex, false);
	}
	
	public static String getChar(String color) {
		ChatColors match = get(color);
		return match == null ? null : match.colorChar;
	}
	
	public static String getHex(String color) {
		ChatColors match = get(color);
		return match == null ? null : match.colorHex;
	}
	
	public static ChatColors get(String color) {
		if (color == null)
			return null;
		
		try {
			return Enum.valueOf(ChatColors.class, color.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static boolean isValid(String color) {
		return get(color) != null;
	}
}
