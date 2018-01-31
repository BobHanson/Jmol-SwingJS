package jspecview.java;


import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import java.util.List;

import javajs.api.GenericColor;

import jspecview.common.ColorParameters;

public class AwtParameters extends ColorParameters {

  public AwtParameters() {
	}

	@Override
	protected boolean isValidFontName(String name) {
    GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
    List<String> fontList = Arrays.asList(g.getAvailableFontFamilyNames());
    for (String s : fontList)
      if (name.equalsIgnoreCase(s))
      	return true;
    return false;
	}
  
  @Override
	public GenericColor getColor1(int rgb) {
    return new AwtColor(rgb);
  }

	@Override
	protected GenericColor getColor3(int r, int g, int b) {
		return new AwtColor(r, g, b);
	}

  @Override
	public ColorParameters copy(String newName){
    return ((ColorParameters) new AwtParameters().setName(newName)).setElementColors(this);
  }

}
