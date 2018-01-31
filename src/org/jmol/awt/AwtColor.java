package org.jmol.awt;

import java.awt.Color;

import javajs.api.GenericColor;


public class AwtColor extends Color implements GenericColor {

	private int opacity;

	public GenericColor get4(int r, int g, int b, int a) {
		return new AwtColor(r, g, b, a);
	}

	public GenericColor get3(int r, int g, int b) {
		return new AwtColor(r, g, b);
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
	public void setOpacity255(int a) {
		opacity = a % 256;
	}
	
}
