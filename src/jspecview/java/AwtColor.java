package jspecview.java;

import java.awt.Color;

import javajs.api.GenericColor;


public class AwtColor extends Color implements GenericColor {

	private int opacity;

  @Override
	public GenericColor addAlpha(int a) {
	  int ca = getAlpha();
	  return (ca == 0xFF ? new AwtColor(getRed(), getGreen(), getBlue(), a) : this);
	}
	
	public AwtColor(int rgb) {
		super(rgb | 0xFF000000);
	}
	
	public AwtColor(int r, int g, int b) {
		super(r, g, b);
		opacity = 255;
	}

	public AwtColor(int r, int g, int b, int a) {
		super(r, g, b, a);
		opacity = a;
	}

	@Override
	public int getOpacity255() {
		return opacity;
	}
	
  @Override
  public String toString() {
    String s = ("00000000" + Integer.toHexString(this.getRGB()));
    return "[0x" + s.substring(s.length() - 8, s.length()) + "]";
  }


}
