package org.jmol.awtjs;

import javajs.awt.Font;

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

	// /// FONT

  // all these are found in awtjs2d.Platform:

//  @Override
//	public int fontStringWidth(Font font, String text) {
//		return JSFont.stringWidth(font, text);
//	}
//
//  @Override
//	public int getFontAscent(Object fontMetrics) {
//		return JSFont.getAscent(fontMetrics);
//	}
//
//  @Override
//	public int getFontDescent(Object fontMetrics) {
//		return JSFont.getDescent(fontMetrics);
//	}
//
//  @Override
//	public Object getFontMetrics(Font font, Object graphics) {
//		return JSFont.getFontMetrics(graphics, font);
//	}
//
//  @Override
//	public Object newFont(String fontFace, boolean isBold, boolean isItalic,
//			float fontSize) {
//		return JSFont.newFont(fontFace, isBold, isItalic, fontSize);
//	}
//
//  @Override
//  public void setViewer(PlatformViewer vwr, Object display) {
//    
//    
//  }
//
//  @Override
//  public boolean isHeadless() {
//    
//    return false;
//  }
//
//  @Override
//  public void convertPointFromScreen(Object display, P3 ptTemp) {
//    
//    
//  }
//
//  @Override
//  public void getFullScreenDimensions(Object display, int[] widthHeight) {
//    
//    
//  }
//
//  @Override
//  public boolean hasFocus(Object display) {
//    
//    return false;
//  }
//
//  @Override
//  public String prompt(String label, String data, String[] list,
//                       boolean asButtons) {
//    
//    return null;
//  }
//
//  @Override
//  public void repaint(Object display) {
//    
//    
//  }
//
//  @Override
//  public void requestFocusInWindow(Object display) {
//    
//    
//  }
//
//  @Override
//  public void setCursor(int i, Object display) {
//    
//    
//  }
//
//  @Override
//  public void setTransparentCursor(Object display) {
//    
//    
//  }
//
//  @Override
//  public GenericMouseInterface getMouseManager(double privateKey, Object display) {
//    
//    return null;
//  }
//
//  @Override
//  public int[] drawImageToBuffer(Object gObj, Object imageOffscreen,
//                                 Object image, int width, int height,
//                                 int bgcolor) {
//    
//    return null;
//  }
//
//  @Override
//  public int getImageWidth(Object image) {
//    
//    return 0;
//  }
//
//  @Override
//  public int getImageHeight(Object image) {
//    
//    return 0;
//  }
//
//  @Override
//  @Deprecated
//  void renderScreenImage(Object g, Object currentSize) {
//    
//    
//  }
//
//  @Override
//  public Object createImage(Object ret) {
//    
//    return null;
//  }
//
//  @Override
//  public int[] grabPixels(Object image, int width, int height, int[] pixels,
//                          int startRow, int nRows) {
//    
//    return null;
//  }
//
//  @Override
//  public boolean waitForDisplay(Object boolIsEcho, Object image)
//      throws InterruptedException {
//    
//    return false;
//  }
//
//  @Override
//  public GenericMenuInterface getMenuPopup(String menuStructure, char type) {
//    
//    return null;
//  }
//
//  @Override
//  public Object getJsObjectInfo(Object[] jsObject, String method, Object[] args) {
//    
//    return null;
//  }
//
//  @Override
//  public boolean isSingleThreaded() {
//    
//    return false;
//  }
//
//  @Override
//  public void notifyEndOfRendering() {
//    
//    
//  }
//
//  @Override
//  public String getDateFormat(String isoType) {
//    
//    return null;
//  }
//
//  @Override
//  public GenericFileInterface newFile(String name) {
//    
//    return null;
//  }
//
//  @Override
//  public Object getBufferedFileInputStream(String name) {
//    
//    return null;
//  }
//
//  @Override
//  public Object getURLContents(URL url, byte[] outputBytes, String post,
//                               boolean asString) {
//    
//    return null;
//  }
//
//  @Override
//  public String getLocalUrl(String fileName) {
//    
//    return null;
//  }
//
//  @Override
//  public GenericImageDialog getImageDialog(String title,
//                                           Map<String, GenericImageDialog> imageMap) {
//    
//    return null;
//  }

}
