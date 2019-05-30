package org.jmol.awtjs2d;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.Map;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericImageDialog;
import org.jmol.api.GenericMenuInterface;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.PlatformViewer;
import org.jmol.api.js.JmolToJSmolInterface;
import org.jmol.script.ScriptContext;
import org.jmol.viewer.Viewer;

import org.jmol.awtjs.swing.Font;

import javajs.util.AjaxURLStreamHandlerFactory;
import javajs.util.P3;
import javajs.util.Rdr;
import javajs.util.SB;

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
public class Platform implements GenericPlatform {
  Object canvas;
  PlatformViewer vwr;
  Object context;
  
	@Override
  public void setViewer(PlatformViewer vwr, Object canvas) {
	  /**
	   * @j2sNative
	   * 
     *     this.vwr = vwr;
     *     if (canvas == null) {
     *       canvas = document.createElement('canvas');
     *       this.context = canvas.getContext("2d");
     *     } else {
	   *       this.context = canvas.getContext("2d");
	   *       canvas.imgdata = this.context.getImageData(0, 0, canvas.width, canvas.height);
	   *       canvas.buf8 = canvas.imgdata.data;
	   *     }
	   */
	  {
	    this.vwr = null;
	    context = null;
      canvas = null;
	  }
    this.canvas = canvas;
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
  public GenericMouseInterface getMouseManager(double privateKey, Object display) {
    return new Mouse(privateKey, (Viewer) vwr, display);
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
    String c = (type == 'j' ? "awtjs2d.JSJmolPopup" : "awtjs2d.JSModelKitPopup");
    GenericMenuInterface jmolpopup = (GenericMenuInterface) Interface
        .getOption(c, (Viewer) vwr, "popup");
    try {
      if (jmolpopup != null)
        jmolpopup.jpiInitialize(vwr, menuStructure);
    } catch (Exception e) {
      c = "Exception creating " + c + ":" + e;
      System.out.println(c);
      return null;
    }
    return jmolpopup;
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
		Display.renderScreenImage(vwr, context, size);
	}

  @Override
  public void drawImage(Object context, Object canvas, int x, int y, int width,
                        int height, boolean isDTI) {
    
    // from Viewer.drawImage
    Display.drawImage(context, canvas, x, y, width, height, isDTI);
  }

	@Override
  public void requestFocusInWindow(Object canvas) {
		Display.requestFocusInWindow(canvas);
	}

  @SuppressWarnings({ "null", "unused" })
  @Override
  public void repaint(Object canvas) {

    JmolToJSmolInterface jmol = null;

    /**
     * Jmol.repaint(applet,asNewThread)
     * 
     * should invoke
     * 
     * setTimeout(applet._applet.update(applet._canvas)) // may be 0,0
     * 
     * when it is ready to do so.
     * 
     * @j2sNative
     * 
     *   jmol = (self.Jmol && Jmol.repaint ? Jmol : null);
     * 
     */
    {
    }
    if (jmol != null)
      jmol.repaint(((Viewer) vwr).html5Applet, true);

  }

	@Override
  public void setTransparentCursor(Object canvas) {
		//Display.setTransparentCursor(canvas);
	}

	@Override
  public void setCursor(int c, Object canvas) {
    Jmol().setCursor(((Viewer) vwr).html5Applet, c);
	}

	// //// Image

	/**
	 * Create an "image" that is either a canvas with width/height/buf32 
	 * (from g3d.Platform32) or just an associative array with those (image writing
	 */
	@Override
  public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent, boolean isImageWrite) {
	  if (pBuffer == null) {
      pBuffer = grabPixels(null, 0, 0, null, 0, 0);
      /**
       * @j2sNative
       * 
       * windowWidth = this.canvas.width;
       * windowHeight = this.canvas.height;
       */
      {}
    }
		return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
				windowSize, backgroundTransparent, (isImageWrite ? null : canvas));
	}

  @Override
  public void notifyEndOfRendering() {
  }

	@Override
  public void disposeGraphics(Object gOffscreen) {
	  // N/A
	}

  @Override
  public int[] grabPixels(Object canvas, int width, int height, int[] pixels,
                          int startRow, int nRows) {
    // from PNG and GIF and JPG image creators, also g3d.ImageRenderer.plotImage via drawImageToBuffer
    Object context2d = null;
    boolean isWebGL = (canvas == null);
    
    //TODO - clean this up with Jmol
    /**
     * 
     * (might be just an object with buf32 defined -- WRITE IMAGE)
     * 
     * @j2sNative
     * 
     *            if(isWebGL) { this.canvas = canvas =
     *            Jmol.loadImage(this,"webgl",""
     *            +System.currentTimeMillis(),this
     *            .vwr.html5Applet._canvas.toDataURL(),null,null); width =
     *            canvas.imageWidth; height = canvas.imageHeight;
     *            canvas.imageWidth = 0; }
     * 
     * 
     *            if (canvas.image && (width != canvas.width || height !=
     *            canvas.height)) Jmol.setCanvasImage(canvas, width, height);
     *            if (canvas.buf32) return canvas.buf32; context2d =
     *            canvas.getContext('2d');
     */
    {
      // placeholder for Eclipse referencing
      Jmol().loadImage(this, null, null, null, null);
      Jmol().setCanvasImage(canvas, width, height);
    }
    int[] buf = Image.grabPixels(context2d, width, height);
    /**
     * @j2sNative
     * 
     *            canvas.buf32 = buf;
     * 
     */
    {
    }
    if (isWebGL) // WebGL reports 0 for background
      for (int i = buf.length; --i >= 0;)
        if (buf[i] == 0)
          buf[i] = -1;
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
    /**
     * @j2sNative
     * 
     * return (canvas == null ? this.context : canvas.getContext("2d"));
     */
	  {
	    return null;
	  }
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
    // for text processing;
    return getGraphics(image);
	}

	@Override
  public Object newBufferedImage(Object image, int w, int h) {
    return Jmol().getHiddenCanvas(((Viewer) vwr).html5Applet, "stereoImage", w, h);
	}

	@Override
  public Object newOffScreenImage(int w, int h) {
    return Jmol().getHiddenCanvas(((Viewer) vwr).html5Applet, "textImage", w, h);
	}

  @Override
  public boolean waitForDisplay(Object echoNameAndPath, Object zipBytes)
      throws InterruptedException {
    // not necessary in JavaScript
    return false;
  }
  /**
   * 
   * @param name_path_bytes
   * @return image object or null if asynchronous
   * 
   */
  @Override
  public Object createImage(Object name_path_bytes) {
    String echoName = ((String[]) name_path_bytes)[0];
    String path = ((String[]) name_path_bytes)[1];
    byte[] bytes = ((byte[][]) name_path_bytes)[2];
    Viewer vwr = (Viewer) this.vwr;
    ScriptContext sc = (bytes == null ? vwr.getEvalContextAndHoldQueue(vwr.eval) : null); 
    Object f = null;
	  /**
	   * 
	   * this is important specifically for retrieving images from
	   * files, as in set echo ID myimage "image.gif"
	   * 
	   * return will be immediate, before the image is created, so here there is
	   * no "wait." Instead, we give it a callback 
	   * 
	   * @j2sNative
	   * 
	   * f = function(canvas, pathOrError) { vwr.loadImageData$O$S$S$O(canvas, pathOrError, echoName, sc) };
	   * 
	   * 
	   */	  
	  {
	    // this call is never made - it is just here as an Eclipse proxy for the above callback
	    vwr.loadImageData(bytes, path, echoName, sc);
	  }
	  return Jmol().loadImage(this, echoName, path, bytes, f);
  }
	// /// FONT

	@Override
  public int fontStringWidth(Font font, String text) {
		return JSFont.stringWidth(font, context, text);
	}

	@Override
  public int getFontAscent(Object context) {
		return JSFont.getAscent(context);
	}

	@Override
  public int getFontDescent(Object context) {
		return JSFont.getDescent(context);
	}

	@Override
  public Object getFontMetrics(Font font, Object context) {
		return JSFont.getFontMetrics(font, context == null ? this.context : context);
	}

	@Override
  public Object newFont(String fontFace, boolean isBold, boolean isItalic,
			float fontSize) {
		return JSFont.newFont(fontFace, isBold, isItalic, fontSize, "px");
	}

//  @Override
//  public String getDateFormat(boolean isoiec8824) {
//    /**
//     * 
//     * Mon Jan 07 2013 19:54:39 GMT-0600 (Central Standard Time)
//     * or YYYYMMDDHHmmssOHH'mm'
//     * 
//     * @j2sNative
//     * 
//     * if (isoiec8824) {
//     *   var d = new Date();
//     *   var x = d.toString().split(" ");
//     *   var MM = "0" + d.getMonth(); MM = MM.substring(MM.length - 2);
//     *   var dd = "0" + d.getDate(); dd = dd.substring(dd.length - 2);
//     *   return x[3] + MM + dd + x[4].replace(/\:/g,"") + x[5].substring(3,6) + "'" + x[5].substring(6,8) + "'"   
//     * }
//     * return ("" + (new Date())).split(" (")[0];
//     */
//    {
//      return null;
//    }
//  }
	
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
     *   var MM = "0" + (1 + d.getMonth()); MM = MM.substring(MM.length - 2);
     *   var dd = "0" + d.getDate(); dd = dd.substring(dd.length - 2);
     *   return x[3] + MM + dd + x[4].replace(/\:/g,"") + x[5].substring(3,6) + "'" + x[5].substring(6,8) + "'"   
     * } else if (isoType.indexOf("8601") >= 0){
     *   var d = new Date();
     *   var x = d.toString().split(" ");
     *   // Firefox now doing this?
     *   if (x.length == 1)
     *     return x;
     *   var MM = "0" + (1 + d.getMonth()); MM = MM.substring(MM.length - 2);
     *   var dd = "0" + d.getDate(); dd = dd.substring(dd.length - 2);
     *   return x[3] + '-' + MM + '-' + dd + 'T' + x[4]   
     * }
     * return ("" + (new Date())).split(" (")[0];
     */
    {
      return null;
    }
  }

  @Override
  public GenericFileInterface newFile(String name) {
    return new JSFile(name);
  }

  @Override
  public Object getBufferedFileInputStream(String name) {
    // n/a for any applet
    return null; 
  }


  @Override
  public Object getURLContents(URL url, byte[] outputBytes, String post,
      boolean asString) {
    return getURLContentsStatic(url, outputBytes, post, asString);
  }

  /**
   * In case this needs to be performed directly, without interface
   * @param url
   * @param outputBytes
   * @param post
   * @param asString
   * @return String or byte[] or javajs.util.SB 
   */
  public static Object getURLContentsStatic(URL url, byte[] outputBytes, String post,
      boolean asString) {
    Object ret = JSFile.getURLContents(url, outputBytes, post);
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
    // n/a (dialogs only)
    return null;
  }

  @Override
  public GenericImageDialog getImageDialog(String title,
                                        Map<String, GenericImageDialog> imageMap) {
    return Image.getImageDialog((Viewer) vwr, title, imageMap);
  }

  public static JmolToJSmolInterface Jmol() {
    /**
     * @j2sNative
     *
     * return Jmol;
     * 
     */
    {
      return null;
    }
  }

  @Override
  public boolean forceAsyncLoad(String filename) {
    return Jmol().isBinaryUrl(filename);
  }


}
