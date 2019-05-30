package jspecview.js2d;


import javajs.api.GenericColor;
import org.jmol.awtjs.swing.Color;
import jspecview.common.ColorParameters;

public class JsParameters extends ColorParameters {

  public JsParameters() {
	}

	@Override
	protected boolean isValidFontName(String name) {
		// TODO
		return true;
	}
  
  @Override
	public GenericColor getColor1(int rgb) {
    return Color.get1(rgb);
  }

	@Override
	protected GenericColor getColor3(int r, int g, int b) {
		return Color.get3(r, g, b);
	}

  @Override
	public ColorParameters copy(String newName){
    return ((ColorParameters) new JsParameters().setName(newName)).setElementColors(this);
  }

}
