package jspecview.js2d;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.Map;

import javajs.api.GenericFileInterface;
import javajs.api.GenericMenuInterface;
import javajs.api.GenericMouseInterface;
import javajs.api.GenericPlatform;
import javajs.api.GenericImageDialog;
import javajs.api.PlatformViewer;
import javajs.awt.Font;
import javajs.util.P3;
import javajs.util.AjaxURLStreamHandlerFactory;
import javajs.util.Rdr;
import javajs.util.SB;

import jspecview.api.JSVPanel;
import jspecview.app.GenericMouse;



/**
 * JavaScript 2D canvas version requires Ajax-based URL stream processing.
 * 
 * Jmol "display" --> HTML5 "canvas"
 * Jmol "image" --> HTML5 "canvas" (because we need width and height)
 * Jmol "graphics" --> HTML5 "context(2d)" (one for display, one off-screen for fonts)
 * Jmol "font" --> JmolFont
 * Jmol "fontMetrics" --> HTML5 "context(2d)"
 * (Not fully implemented) 
 * 
 * @author Bob Hanson
 *
 */
public class JsPlatform implements GenericPlatform {
  Object canvas;
  PlatformViewer viewer;
  Object context;
  
	@Override
	public void setViewer(PlatformViewer viewer, Object canvas) {
		Object context = "";
		this.viewer = viewer;
		this.canvas = canvas;
	  /**
	   * @j2sNative
	   * 
     *     if (canvas != null) {
	   *       context = canvas.getContext("2d");
	   *       canvas.imgdata = context.getImageData(0, 0, canvas.width, canvas.height);
	   *       canvas.buf8 = canvas.imgdata.data;
	   *     }
	   */
	  {}
	  if (context != "")
	  	this.context = context;
		//
		try {
		  URL.setURLStreamHandlerFactory(new AjaxURLStreamHandlerFactory());
		} catch (Throwable e) {
		  // that's fine -- already created	
		}
	}

  @Override
	public boolean isSingleThreaded() {
    return true;
  }

  @Override
	public Object getJsObjectInfo(Object[] jsObject, String method, Object[] args) {
    /**
     * we must use Object[] here to hide [HTMLUnknownElement] and [Attribute] from Java2Script
     * @j2sNative
     * 
     * return (method == null ? null : method == "localName" ? jsObject[0]["nodeName"] : args == null ? jsObject[0][method] : jsObject[0][method](args[0]));
     * 
     * 
     */
    {
      return null;
    }
  }

  @Override
	public boolean isHeadless() {
    return false;
  }

  @Override
	public GenericMouseInterface getMouseManager(double privateKey, Object jsvp) {
  	return new GenericMouse((JSVPanel) jsvp);
  }

  // /// Display

	@Override
	public void convertPointFromScreen(Object canvas, P3 ptTemp) {
	  // from JmolMultiTouchClientAdapter.fixXY
		Display.convertPointFromScreen(canvas, ptTemp);
	}

	@Override
	public void getFullScreenDimensions(Object canvas, int[] widthHeight) {
		Display.getFullScreenDimensions(canvas, widthHeight);
	}

  @Override
	public GenericMenuInterface getMenuPopup(String menuStructure,
                                         char type) {
  	return null;
  }

	@Override
	public boolean hasFocus(Object canvas) {
		return Display.hasFocus(canvas);
	}

	@Override
	public String prompt(String label, String data, String[] list,
			boolean asButtons) {
		return Display.prompt(label, data, list, asButtons);
	}

	/**
	 * legacy apps will use this
	 * 
	 * @param context
	 * @param size
	 */
	@Override
	public void renderScreenImage(Object context, Object size) {
		Display.renderScreenImage(viewer, context, size);
	}

  @Override
	public void drawImage(Object context, Object canvas, int x, int y, int width,
                        int height, boolean isDTI) {
    
    // from Viewer.render1
    Image.drawImage(context, canvas, x, y, width, height);
  }

	@Override
	public void requestFocusInWindow(Object canvas) {
		Display.requestFocusInWindow(canvas);
	}

	@Override
	public void repaint(Object canvas) {
		Display.repaint(canvas);
	}

	@Override
	public void setTransparentCursor(Object canvas) {
		Display.setTransparentCursor(canvas);
	}

	@Override
	public void setCursor(int c, Object canvas) {
		Display.setCursor(c, canvas);
	}

	// //// Image

	@Override
	public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent, boolean isImageWrite) {
		return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
				windowSize, backgroundTransparent, (isImageWrite ? null : canvas));
	}

  @Override
	public void notifyEndOfRendering() {
  }

  /**
   * could be byte[] (from ZIP file) or String (local file name) or URL
   * @param data 
   * @return image object
   * 
   */
	@Override
	public Object createImage(Object data) {
	  // N/A in JS
	  return null;
	}

	@Override
	public void disposeGraphics(Object gOffscreen) {
	  // N/A
	}

	@Override
	public int[] grabPixels(Object canvas, int width, int height, 
                          int[] pixels, int startRow, int nRows) {
	  // from PNG and JPG image creators, also g3d.ImageRenderer.plotImage via drawImageToBuffer
	  
	  /**
	   * 
	   * (might be just an object with buf32 defined -- WRITE IMAGE)
	   * 
	   * @j2sNative
	   * 
	   *     if (canvas.image && (width != canvas.width || height != canvas.height))
     *       Jmol._setCanvasImage(canvas, width, height);
	   *     if (canvas.buf32) return canvas.buf32;
	   */
	  {}
    int[] buf = Image.grabPixels(Image.getGraphics(canvas), width, height); 
    /**
     * @j2sNative
     *  
     *  canvas.buf32 = buf;
     * 
     */
    {}
    return buf;
	}

	@Override
	public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
			Object canvas, int width, int height, int bgcolor) {
	  return grabPixels(canvas, width, height, null, 0, 0);
	}

	@Override
	public int[] getTextPixels(String text, Font font3d, Object context,
			Object image, int width, int height, int ascent) {
		return Image.getTextPixels(text, font3d, context, width, height, ascent);
	}

	@Override
	public void flushImage(Object imagePixelBuffer) {
	  // N/A
	}

	@Override
	public Object getGraphics(Object canvas) {
		return (canvas == null ? context : (context = Image.getGraphics(this.canvas = canvas)));
	}

  @Override
	public int getImageHeight(Object canvas) {
		return (canvas == null ? -1 : Image.getHeight(canvas));
	}

	@Override
	public int getImageWidth(Object canvas) {
		return (canvas == null ? -1 : Image.getWidth(canvas));
	}

	@Override
	public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
		return Image.getStaticGraphics(image, backgroundTransparent);
	}

	@Override
	public Object newBufferedImage(Object image, int w, int h) {
    /**
     * @j2sNative
     * 
     *  if (self.Jmol && Jmol._getHiddenCanvas)
     *    return Jmol._getHiddenCanvas(this.vwr.html5Applet, "stereoImage", w, h); 
     */
    {}
    return null;
	}

	@Override
	public Object newOffScreenImage(int w, int h) {
    /**
     * @j2sNative
     * 
     *  if (self.Jmol && Jmol._getHiddenCanvas)
     *    return Jmol._getHiddenCanvas(this.vwr.html5Applet, "textImage", w, h); 
     */
    {}
    return null;
	}

	@Override
	public boolean waitForDisplay(Object echoNameAndPath, Object zipBytes)
			throws InterruptedException {
		// not implmented in JSpecView
	  return false;	    
	}

	// /// FONT

	@Override
	public int fontStringWidth(Font font, String text) {
		return JsFont.stringWidth(font, text);
	}

	@Override
	public int getFontAscent(Object context) {
		return JsFont.getAscent(context);
	}

	@Override
	public int getFontDescent(Object context) {
		return JsFont.getDescent(context);
	}

	@Override
	public Object getFontMetrics(Font font, Object context) {
		return JsFont.getFontMetrics(font, context);
	}

	@Override
	public Object newFont(String fontFace, boolean isBold, boolean isItalic,
			float fontSize) {
		return JsFont.newFont(fontFace, isBold, isItalic, fontSize, "px");
	}

  @Override
  public String getDateFormat(String isoType) {
    /**
     * 
     * Mon Jan 07 2013 19:54:39 GMT-0600 (Central Standard Time)
     * or YYYYMMDDHHmmssOHH'mm'
     * 
     * @j2sNative
     * 
     * if (isoType == null) {
     * } else if (isoType.indexOf("8824") >= 0) {
     *   var d = new Date();
     *   var x = d.toString().split(" ");
     *   var MM = "0" + d.getMonth(); MM = MM.substring(MM.length - 2);
     *   var dd = "0" + d.getDate(); dd = dd.substring(dd.length - 2);
     *   return x[3] + MM + dd + x[4].replace(/\:/g,"") + x[5].substring(3,6) + "'" + x[5].substring(6,8) + "'"   
     * } else if (isoType.indexOf("8601") >= 0){
     *   var d = new Date();
     *   var x = d.toString().split(" ");
     *   var MM = "0" + d.getMonth(); MM = MM.substring(MM.length - 2);
     *   var dd = "0" + d.getDate(); dd = dd.substring(dd.length - 2);
     *   return x[3] + MM + dd + x[4].replace(/\:/g,"") + x[5].substring(3,6) + "'" + x[5].substring(6,8) + "'"   
     * }
     * return ("" + (new Date())).split(" (")[0];
     */
    {
      return null;
    }
  }

  @Override
	public GenericFileInterface newFile(String name) {
    return new JsFile(name);
  }

  @Override
	public Object getBufferedFileInputStream(String name) {
    // n/a for any applet
    return null; 
  }

	@Override
	public Object getURLContents(URL url, byte[] outputBytes, String post,
			boolean asString) {
		Object ret = JsFile.getURLContents(url, outputBytes, post);
		// check for error
		try {
			return (!asString ? ret : ret instanceof String ? ret : ret instanceof SB ? ((SB) ret)
					.toString() : ret instanceof byte[] ? new String((byte[]) ret)
					: new String((byte[]) Rdr.getStreamAsBytes((BufferedInputStream) ret,
							null)));
		} catch (Exception e) {
			return "" + e;
		}
	}

	@Override
	public String getLocalUrl(String fileName) {
		// not used in JSpecView
		return null;
	}

	@Override
	public GenericImageDialog getImageDialog(String title,
			Map<String, GenericImageDialog> imageMap) {
		// TODO Auto-generated method stub
		return null;
	}

  @Override
  public boolean forceAsyncLoad(String filename) {
    // TODO Auto-generated method stub
    return false;
  }

}
