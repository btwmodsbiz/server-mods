package btwmod.livemap;

public class PixelColor {
	
	public static final float defaultAlpha = 1.0F;

	public float red;
	public float green;
	public float blue;
	public float alpha;

	public PixelColor() {
		clear();
	}

	public PixelColor(float red, float green, float blue) {
		this(red, green, blue, defaultAlpha);
	}

	public PixelColor(float red, float green, float blue, float alpha) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}

	public void composite(float red, float green, float blue, float alpha) {
		this.red += (red - this.red) * alpha;
		this.green += (green - this.green) * alpha;
		this.blue += (blue - this.blue) * alpha;
		this.alpha += (1.0F - this.alpha) * alpha;
	}

	public void composite(float red, float green, float blue, float alpha, float brightness) {
		composite(red * brightness, green * brightness, blue *  brightness, alpha);
	}

	public void clear() {
		alpha = red = green = blue = 0.0F;
	}
	
	public static int getLuminance(float grey) {
		return (int)Math.round(.299F * grey + .587F * grey + .114F * grey);
	}
	
	public static int getLuminance(int grey) {
		return (int)Math.round(.299F * grey + .587F * grey + .114F * grey);
	}
}
