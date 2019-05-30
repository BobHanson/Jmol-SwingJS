package org.jmol.awtjs;

import org.jmol.awtjs.swing.Font;

/**
 * 
 * WebGL interface
 * 
 * @author Bob Hanson
 *
 */
public class Platform extends org.jmol.awtjs2d.Platform {

	// differences for WebGL are only in the area of fonts and images
  // (which are not implemented yet, anyway)


  @Override
	public void drawImage(Object g, Object img, int x, int y, int width,
			int height, boolean isDTI) {
    // not used in WebGL version
	}

  @Override
	public int[] getTextPixels(String text, Font font3d, Object gObj,
			Object image, int width, int height, int ascent) {
		return null;
	}

  @Override
	public Object getGraphics(Object image) {
    // n/a
		return null;
	}

  @Override
	public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
    // n/a
		return null;
	}

  @Override
	public Object newBufferedImage(Object image, int w, int h) {
    // n/a
		return null;
	}

  @Override
	public Object newOffScreenImage(int w, int h) {
    // n/a
		return null;
	}


}
