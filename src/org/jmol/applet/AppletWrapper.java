/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-03-15 21:07:07 -0500 (Sun, 15 Mar 2015) $
 * $Revision: 20385 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.applet;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;

//import javax.swing.JApplet;  also works

import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.util.GenericApplet;

/**
 * Using Applet only because originally there was the possibility of non-Swing versions of 
 * the JRE being used. No real difference, I think.
 * 
 */
public class AppletWrapper extends Applet {

  public WrappedApplet wrappedApplet;

  private String preloadImageName;
  private String preloadTextMessage;
  private String previousClassName;
  private int preloadThreadCount;
  private String[] preloadClassNames;

  private int preloadClassIndex;

  private boolean isSigned;
  private boolean needToCompleteInitialization;
  private boolean preloadImageReadyForDisplay;
  private boolean preloadImagePainted;

  private Color bgcolor;
  private Color textColor;
  private Image preloadImage;
  private MediaTracker mediaTracker;

  private long startTime;
  private int clockX;
  private int clockBaseline;
  private int clockWidth;

  private static int MINIMUM_ELAPSED_SECONDS = 1;

  private static String fontFace = "sansserif";
  private static int fontSizeDivisor = 18;
  private int fontSize;
  private Font font;
  private FontMetrics fontMetrics;
  private int fontAscent;
  //private int fontDescent;
  private int fontHeight;
    
  @Override
  public void destroy() {
    //System.out.println("AppletWrapper destroy called");
    try {
      ((GenericApplet) wrappedApplet).destroy();
    } catch (Exception e) {
      // no matter -- Firefox/Mac destroys wrappedApplet for us
    }
    wrappedApplet = null;
    super.destroy();
  }

  public AppletWrapper(String preloadImageName,
                       int preloadThreadCount,
                       String[] preloadClassNames) {
    this.preloadImageName = preloadImageName;
    this.preloadTextMessage = "Loading Jmol applet ...";
    this.preloadThreadCount = preloadThreadCount;
    this.preloadClassNames = preloadClassNames;
    needToCompleteInitialization = true;
    isSigned = false;
    try {
      String imagePath = "" + (getClass().getClassLoader().getResource(preloadImageName));
      isSigned = (imagePath.indexOf("Signed") >= 0);
      System.out.println("appletwrapper isSigned = " + isSigned);
    } catch (Exception e) {
      Logger.error("isSigned false: " + e);
    }
  }

  public boolean isSigned() {
    System.out.println("appletwrapper2 isSigned = " + isSigned);
    return isSigned;
  }
  
  @Override
  public String getAppletInfo() {
    return (wrappedApplet != null ? ((GenericApplet)wrappedApplet).getAppletInfo() : null);
  }

  @Override
  public void init() {
    startTime = System.currentTimeMillis();
    new WrappedAppletLoader(this, isSigned).start();
    for (int i = preloadThreadCount; --i >= 0; )
      new ClassPreloader(this).start();
  }
  
  @Override
  public void update(Graphics g) {
    if (wrappedApplet != null) {
      mediaTracker = null;
      preloadImage = null;
      fontMetrics = null;

      wrappedApplet.update(g);
      return;
    }
    Dimension dim = getSize(); // deprecated, but use it for old JVMs

    if (needToCompleteInitialization)
      completeInitialization(g, dim);

    g.setColor(bgcolor);
    g.fillRect(0, 0, dim.width, dim.height);
    g.setColor(textColor);

    int imageBottom = 0;

    if (!preloadImageReadyForDisplay && mediaTracker != null)
      preloadImageReadyForDisplay = mediaTracker.checkID(0, true);

    if (preloadImageReadyForDisplay) {
      int imageHeight = preloadImage.getHeight(null);
      if (imageHeight > 0) {
        if (10 + imageHeight + fontHeight <= dim.height) {
          g.drawImage(preloadImage, 10, 10, null);
          preloadImagePainted = true;
          imageBottom = 10 + imageHeight;
        }
      }
    }

    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
    if (elapsedTime >= MINIMUM_ELAPSED_SECONDS) {
      int messageBaseline = imageBottom + fontAscent;
      if (messageBaseline < dim.height / 2)
        messageBaseline = dim.height / 2;
      else if (messageBaseline >= dim.height)
        messageBaseline = dim.height - 1;
      g.setFont(font);
      g.drawString(preloadTextMessage, 10, messageBaseline);

      String clockText = "  " + elapsedTime + " seconds";
      clockWidth = fontMetrics.stringWidth(clockText);
      clockX = dim.width - clockWidth - 5;
      if (clockX < 0)
        clockX = 0;
      clockBaseline = dim.height - 5;
      if (Logger.debugging)
        Logger.debug(clockText);
      g.drawString(clockText, clockX, clockBaseline);
    }
  }
  
  @Override
  public void paint(Graphics g) {
    if (wrappedApplet != null) {
      wrappedApplet.paint(g);
      return;
    }
    update(g);
  }

  @Override
  public boolean handleEvent(Event e) {
    if (wrappedApplet != null)
      return ((GenericApplet) wrappedApplet).handleEvent(e);
    return false;
  }
  
  public synchronized String getNextPreloadClassName() {
    if (preloadClassNames == null ||
        preloadClassIndex == preloadClassNames.length)
      return null;
    String className = preloadClassNames[preloadClassIndex++];
    if (className.charAt(0) == '.') {
      int lastDot = previousClassName.lastIndexOf('.');
      String previousPackageName = previousClassName.substring(0, lastDot);
      className = previousPackageName + className;
    }
    return previousClassName = className;
  }

  protected void repaintClock() {
    if (! preloadImagePainted || clockBaseline == 0)
      repaint();
    else
      repaint(clockX, clockBaseline - fontAscent, clockWidth, fontHeight);
  }

  private boolean completeInitialization(Graphics g, Dimension dim) {
    needToCompleteInitialization = false;
    try {
      if (Logger.debugging) {
        Logger.debug("loadImage:" + preloadImageName);
      }
      URL urlImage = getClass().getClassLoader().getResource(preloadImageName);
      Logger.info("urlImage=" + urlImage);
      if (urlImage != null) {
        preloadImage = Toolkit.getDefaultToolkit().getImage(urlImage);
        if (Logger.debugging) {
          Logger.debug("successfully loaded " + preloadImageName);
          Logger.debug("preloadImage=" + preloadImage);
        }
        mediaTracker = new MediaTracker(this);
        mediaTracker.addImage(preloadImage, 0);
        mediaTracker.checkID(0, true);
      }
    } catch (Exception e) {
      Logger.error("getImage failed: " + e);
    }
    String bgcolorName = getParameter("boxbgcolor");
    if (bgcolorName == null)
      bgcolorName = getParameter("bgcolor");
    bgcolor = getColorFromName(bgcolorName);
    textColor = getContrastingBlackOrWhite(bgcolor);

    fontSize = dim.height / fontSizeDivisor;
    if (fontSize < 7)
      fontSize = 7;
    if (fontSize > 30)
      fontSize = 30;

    while (true) {
      font = new Font(fontFace, Font.PLAIN, fontSize);
      fontMetrics = g.getFontMetrics(font);
      if (fontMetrics.stringWidth(preloadTextMessage) + 10 < dim.width)
        break;
      if (fontSize < 8)
        break;
      fontSize -= 2;
    }
    fontHeight = fontMetrics.getHeight();
    fontAscent = fontMetrics.getAscent();
    return isSigned;
  }

  private final static String[] colorNames = {
    "aqua", "black", "blue", "fuchsia",
    "gray", "green", "lime", "maroon",
    "navy", "olive", "purple", "red",
    "silver", "teal", "white", "yellow"
  };

  private final static Color[] colors = {
    Color.cyan, Color.black, Color.blue, Color.magenta,
    Color.gray, new Color(0,128,0), Color.green, new Color(128,0,0),
    new Color(0,0,128), new Color(128,128,0), new Color(128,0,128), Color.red,
    Color.lightGray, new Color(0,128,128), Color.white, Color.yellow
  };
  

  private Color getColorFromName(String strColor) {
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = PT.parseIntRadix(strColor.substring(1, 3), 16);
          int grn = PT.parseIntRadix(strColor.substring(3, 5), 16);
          int blu = PT.parseIntRadix(strColor.substring(5, 7), 16);
          return new Color(red, grn, blu);
        } catch (NumberFormatException e) {
        }
      } else {
        strColor = strColor.toLowerCase().intern();
        for (int i = colorNames.length; --i >= 0; )
          if (strColor == colorNames[i])
            return colors[i];
      }
    }
    return Color.black;
  }

  private Color getContrastingBlackOrWhite(Color color) {
    // return a grayscale value 0-FF using NTSC color luminance algorithm
    int argb = color.getRGB();
    int grayscale = ((2989 * (argb >> 16) & 0xFF) +
                     (5870 * (argb >> 8) & 0xFF) +
                     (1140 * (argb & 0xFF)) + 500) / 1000;
    return grayscale < 128 ? Color.white : Color.black;
  }
  
}
