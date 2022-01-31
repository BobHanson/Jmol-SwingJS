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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.util.Map;

import javax.swing.JApplet;

import org.jmol.api.Interface;
import org.jmol.api.JmolAppletInterface;
import org.jmol.api.JmolSyncInterface;
import org.jmol.util.GenericApplet;
import org.jmol.util.Logger;

//import javax.swing.JApplet;  also works

import javajs.util.PT;
import javajs.util.SB;

/**
 * Using Applet only because originally there was the possibility of non-Swing versions of 
 * the JRE being used. No real difference, I think.
 * 
 */
public class JmolApplet extends JApplet implements
JmolAppletInterface {
  
  public Jmol jmol;

  private boolean isSigned = true;
  private boolean needToCompleteInitialization = true;

  private Color bgcolor;

  @Override
  public void destroy() {
    //System.out.println("AppletWrapper destroy called");
    try {
      ((GenericApplet) jmol).destroy();
    } catch (Exception e) {
      // no matter -- Firefox/Mac destroys wrappedApplet for us
    }
    jmol = null;
    super.destroy();
  }

  public JmolApplet() {
  }

  public boolean isSigned() {
    System.out.println("appletwrapper2 isSigned = " + isSigned);
    return isSigned;
  }
  
  @Override
  public String getAppletInfo() {
    return (jmol != null ? ((GenericApplet)jmol).getAppletInfo() : null);
  }

  @Override
  public void init() {
    try {
      jmol = (Jmol) Interface.getOption("applet.Jmol", null, null);
      jmol.setApplet(this, isSigned);
    } catch (Exception e) {
      Logger.errorEx("Could not instantiate applet", e);
    }
  }
  
  @Override
  public void update(Graphics g) {
    jmol.update(g);
    if (jmol != null) {
      return;
    }
    Dimension dim = getSize(); // deprecated, but use it for old JVMs

    if (needToCompleteInitialization)
      completeInitialization(g, dim);

    g.setColor(bgcolor);
    g.fillRect(0, 0, dim.width, dim.height);

  }
  
  @Override
  public void paint(Graphics g) {
    if (jmol != null) {
      jmol.paint(g);
      return;
    }
    update(g);
  }

  @Override
  public boolean handleEvent(Event e) {
    if (jmol != null)
      return ((GenericApplet) jmol).handleEvent(e);
    return false;
  }
  
  /**
   * @param g  
   * @param dim 
   * @return true
   */
  private boolean completeInitialization(Graphics g, Dimension dim) {
    needToCompleteInitialization = false;
    String bgcolorName = getParameter("boxbgcolor");
    if (bgcolorName == null)
      bgcolorName = getParameter("bgcolor");
    bgcolor = getColorFromName(bgcolorName);
    return isSigned = true;
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

//  private Color getContrastingBlackOrWhite(Color color) {
//    // return a grayscale value 0-FF using NTSC color luminance algorithm
//    int argb = color.getRGB();
//    int grayscale = ((2989 * (argb >> 16) & 0xFF) +
//                     (5870 * (argb >> 8) & 0xFF) +
//                     (1140 * (argb & 0xFF)) + 500) / 1000;
//    return grayscale < 128 ? Color.white : Color.black;
//  }
//
  /**
   * set a callback either as a function or a function name from JavaScript
   * 
   */
  @Override
  public void setCallback(String name, Object callbackObject) {
    if (jmol != null)
      ((GenericApplet) jmol).setCallback(name, callbackObject);
  }

  @Override
  public String getPropertyAsString(String infoType) {
    return (jmol == null ? null : ""
        + ((GenericApplet) jmol).getPropertyAsString("" + infoType));
  }

  @Override
  public String getPropertyAsString(String infoType, String paramInfo) {
    return (jmol == null ? null : ""
        + ((GenericApplet) jmol).getPropertyAsString("" + infoType, "" + paramInfo));
  }

  @Override
  public String getPropertyAsJSON(String infoType) {
    return (jmol == null ? null : ""
        + ((GenericApplet) jmol).getPropertyAsJSON("" + infoType));
  }

  @Override
  public String getPropertyAsJSON(String infoType, String paramInfo) {
    return (jmol == null ? null : ""
        + ((GenericApplet) jmol).getPropertyAsJSON("" + infoType, "" + paramInfo));
  }

  @Override
  public Map<String, Object> getJSpecViewProperty(String infoType) {
    return null;
  }

  @Override
  public Object getProperty(String infoType, String paramInfo) {
    return (jmol == null ? null : ((GenericApplet) jmol).getProperty(""
        + infoType, "" + paramInfo));
  }

  @Override
  public Object getProperty(String infoType) {
    return (jmol == null ? null : ((GenericApplet) jmol).getProperty(""
        + infoType));
  }

  @Override
  public String loadInlineArray(String[] strModels, String script, boolean isAppend) {
    if (jmol == null || strModels == null || strModels.length == 0)
        return null;
      String s = "" + strModels[0];
      if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
        String[] converted = new String[strModels.length];
        for (int i = 0; i < strModels.length; ++i)
          converted[i] = "" + strModels[i];
        return ((GenericApplet) jmol).loadInlineArray(converted, "" + script, isAppend);
      }
      SB sb = new SB();
      for (int i = 0; i < strModels.length; ++i)
        sb.append(strModels[i]).appendC('\n');
      return ((GenericApplet) jmol).loadInlineString(sb.toString(), "" + script, isAppend);
  }

  @Override
  public String loadInlineString(String strModel, String script, boolean isAppend) {
    return (jmol == null ? null :
      ((GenericApplet) jmol).loadInlineString("" + strModel, "" + script, isAppend));
  }

  // bizarre Mac OS X / Java bug:
  // Mac cannot differentiate between signatures String and String[]
  // so, instead, we deprecate these and go for the above two methods only.

  /**
   * @deprecated
   * @param strModel
   * @return         error or null
   */
  @Override
  @Deprecated
  public String loadInline(String strModel) {
    return (jmol == null ? null :
      ((GenericApplet) jmol).loadInline("" + strModel));
  }

  /**
   * @deprecated
   * @param strModel
   * @param script
   * @return         error or null
   */
  @Override
  @Deprecated
  public String loadInline(String strModel, String script) {
    return (jmol == null ? null :
      ((GenericApplet) jmol).loadInline("" + strModel, "" + script));
  }

  /**
   * @deprecated
   * @param strModels
   * @return         error or null
   */
  @Override
  @Deprecated
  public String loadInline(String[] strModels) {
    return (jmol == null ? null :
      ((GenericApplet) jmol).loadInline(strModels));
  }

  /**
   * @deprecated
   * @param strModels
   * @param script
   * @return         error or null
   */
  @Override
  @Deprecated
  public String loadInline(String[] strModels, String script) {
    return (jmol == null ? null :
      ((GenericApplet) jmol).loadInline(strModels, script));
  }

  @Override
  public String loadDOMNode(Object DOMNode) {
    return (jmol == null ? null : ((GenericApplet) jmol).loadDOMNode(DOMNode));
  }

  @Override
  public void script(String script) {
    //System.out.println("JmolApplet script test " + script + " " + wrappedApplet);
    if (jmol != null)
      ((GenericApplet) jmol).script("" + script);
  }

  @Override
  public void syncScript(String script) {
    if (jmol != null)
      ((GenericApplet) jmol).syncScript("" + script);
  }

  @Override
  public Object setStereoGraphics(boolean isStereo) {
    return (jmol == null ? null : 
        ((GenericApplet) jmol).setStereoGraphics(isStereo));
  }

  @Override
  public String scriptNoWait(String script) {
    if (jmol != null)
      return "" + (((GenericApplet) jmol).scriptNoWait("" + script));
    return null;
  }

  @Override
  public String scriptCheck(String script) {
    if (jmol != null)
      return "" + (((GenericApplet) jmol).scriptCheck("" + script));
    return null;
  }

  @Override
  public String scriptWait(String script) {
    if (jmol != null)
      return "" + (((GenericApplet) jmol).scriptWait("" + script));
    return null;
  }

  @Override
  public String scriptWait(String script, String statusParams) {
    if (statusParams == null)
      statusParams = "";
    if (jmol != null)
      return "" + (((GenericApplet) jmol).scriptWait("" + script, statusParams));
    return null;
  }
  
  @Override
  public String scriptWaitOutput(String script) {
    if (jmol != null)
      return "" + (((GenericApplet) jmol).scriptWaitOutput("" + script));
    return null;
  }

  public void registerApplet(String id, String fullName) {
    // does not work looking for JSVApplet!
    JmolSyncInterface applet = (JmolSyncInterface) getAppletContext().getApplet(id);
    if (applet == null)
      System.out.println("could not find " + id);
    register(fullName, applet);
  }
  
  @Override
  public void register(String id, JmolSyncInterface jsi) {
    if (jmol != null)
      ((GenericApplet) jmol).register(id, jsi);
  }

  @Override
  public int getModelIndexFromId(String id) {
    return (jmol == null ? Integer.MIN_VALUE :  
      ((GenericApplet) jmol).getModelIndexFromId(id));
  }

  @Override
  public void notifyAudioEnded(Object htParams) {
    if (jmol != null)
      ((GenericApplet) jmol).notifyAudioEnded(htParams);   
  }


}
