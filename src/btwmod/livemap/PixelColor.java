package btwmod.livemap;

public class PixelColor {

	public float alpha;
	public float red;
	public float green;
	public float blue;

	public PixelColor() {
		clear();
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
}
