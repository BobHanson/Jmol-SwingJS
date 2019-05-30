package org.jmol.awtjs.swing;

import javajs.api.GenericColor;

public class Color implements GenericColor {

	public int argb;


  @Override
  public int getRGB() {
		return argb & 0x00FFFFFF;
	}


  @Override
  public int getOpacity255() {
		return ((argb >> 24) & 0xFF);
	}

	
  @Override
  public void setOpacity255(int a) {
		argb = argb & 0xFFFFFF | ((a & 0xFF) << 24);
	}

	public static GenericColor get1(int rgb) {
		Color c = new Color();
		c.argb = rgb | 0xFF000000;
		return c;
	}

	public static GenericColor get3(int r, int g, int b) {
		return new Color().set4(r, g, b, 0xFF);
	}

	public static GenericColor get4(int r, int g, int b, int a) {
		return new Color().set4(r, g, b, a);
	}

	private GenericColor set4(int r, int g, int b, int a) {
		argb = ((a << 24) | (r << 16) | (g << 8) | b) & 0xFFFFFFFF;
		return this;
	}

  @Override
  public String toString() {
    String s = ("00000000" + Integer.toHexString(argb));
    return "[0x" + s.substring(s.length() - 8, s.length()) + "]";
  }

	
}
