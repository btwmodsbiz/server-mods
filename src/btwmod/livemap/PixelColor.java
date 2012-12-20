package btwmod.livemap;

import java.awt.Color;

public class PixelColor {
	
	public static final float defaultAlpha = 1.0F;

	public float red;
	public float green;
	public float blue;
	public float alpha;
	
	private float[] hsbvals = new float[3];

	public PixelColor() {
		clear();
	}

	public PixelColor(float red, float green, float blue) {
		this(red, green, blue, defaultAlpha);
	}
	
	public PixelColor(Color color) {
		this((float)color.getRed(), (float)color.getGreen(), (float)color.getBlue(), (float)color.getAlpha());
	}
	
	public PixelColor(BlockColor color) {
		this((float)color.red, (float)color.green, (float)color.blue, color.alpha);
	}

	public PixelColor(float red, float green, float blue, float alpha) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
		checkValues();
	}

	public PixelColor composite(float red, float green, float blue, float alpha) {
		this.red += (red - this.red) * alpha;
		this.green += (green - this.green) * alpha;
		this.blue += (blue - this.blue) * alpha;
		this.alpha += (1.0F - this.alpha) * alpha;
		checkValues();
		return this;
	}

	public PixelColor composite(float red, float green, float blue, float alpha, float brightness) {
		return composite(red * brightness, green * brightness, blue *  brightness, alpha);
	}

	public PixelColor composite(Color color, float alpha) {
		return composite(color.getRed(), color.getGreen(), color.getBlue(), (float)color.getAlpha() / 255.0F * alpha);
	}
	
	public float getHue() {
		return hsbvals[0];
	}
	
	public int getHueInt() {
		return Math.round(getHue() * 360);
	}
	
	public float getSaturation() {
		return hsbvals[1];
	}
	
	public int getSaturationInt() {
		return Math.round(getSaturation() * 100);
	}
	
	public float getBrightness() {
		return hsbvals[2];
	}
	
	public int getBrightnessInt() {
		return Math.round(getBrightness() * 100);
	}
	
	public PixelColor hue(int hue) {
		return hue(hue, 1.0F);
	}
	
	public PixelColor hue(float hue, float alpha) {
		float[] hsb = Color.RGBtoHSB(Math.round(red), Math.round(green), Math.round(blue), null);
		int rgb = Color.HSBtoRGB(hue, hsb[1], hsb[2]);
		return composite((float)(rgb >> 16 & 255), (float)(rgb >> 8 & 255), (float)(rgb & 255), alpha);
	}
	
	public PixelColor scale(float multiplier) {
		red *= multiplier;
		green *= multiplier;
		blue *= multiplier;
		checkValues();
		return this;
	}

	public PixelColor clear() {
		alpha = red = green = blue = 0.0F;
		return this;
	}
	
	public Color asColor() {
		return new Color(Math.round(red), Math.round(green), Math.round(blue), Math.round(alpha * 255.0F));
	}

	public void set(float red, float green, float blue) {
		set(red, green, blue, defaultAlpha);
	}
	
	public void set(Color color) {
		set((float)color.getRed(), (float)color.getGreen(), (float)color.getBlue(), (float)color.getAlpha() / 255.0F);
	}
	
	public void set(BlockColor color) {
		set((float)color.red, (float)color.green, (float)color.blue, color.alpha);
	}

	public void set(float red, float green, float blue, float alpha) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
		checkValues();
	}
	
	public static int getLuminance(int grey) {
		return (int)Math.round(.299 * grey + .587 * grey + .114 * grey);
	}
	
	private void checkValues() {
		red = Math.min(255.0F, Math.max(0.0F, red));
		green = Math.min(255.0F, Math.max(0.0F, green));
		blue = Math.min(255.0F, Math.max(0.0F, blue));
		alpha = Math.min(1.0F, Math.max(0.0F, alpha));
		
		Color.RGBtoHSB(Math.round(red), Math.round(green), Math.round(blue), hsbvals);
	}
}
