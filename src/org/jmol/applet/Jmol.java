/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-01-28 23:38:16 -0600 (Sun, 28 Jan 2018) $
 * $Revision: 21814 $
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
import java.awt.Graphics;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.UIManager;

import org.jmol.awt.FileDropper;
import org.jmol.c.CBK;
import org.jmol.util.GenericApplet;
import org.jmol.util.Logger;

import javajs.util.PT;
import netscape.javascript.JSObject; // Java applet only
import swingjs.api.JSUtilI;

/*
 * This applet is produced in both Java and JavaScript using 
 * java2script/SwingJS. org.jmol.appletjs.Jmol is for the legacy JSmol only.
 * 
 * all parameters are optional:
 * 
 * [param name="name" value="jmolApplet0_object" /]
 * 
 * If name is null, it is assumed that this is a JNLP load outside of a browser.
 * 
 * [param name="boxbgcolor" value="#112233" /]
 * 
 * [param name="syncId" value="nnnnn" /]
 * 
 * determines the subset of applets *across pages* that are to be synchronized
 * (usually just a random number assigned by the JavaScript on the page).
 * If this is fiddled with, it still should be a random number, not
 * one that is assigned statically for a given web page.
 * 
 * [param name="menuFile" value="myMenu.mnu" /]
 * 
 * optional file to load containing menu data in the format of Jmol.mnu (Jmol 11.3.15)
 * 
 * [param name="loadInline" value=" | do | it | this | way " /]
 * 
 * [param name="script" value="your-script" /]
 *  // this one flips the orientation and uses RasMol/Chime colors [param
 * name="emulate" value="chime" /]
 *  // this is *required* if you want the applet to be able to // call your
 * callbacks
 * 
 * mayscript="true" is required as an applet/object for any callback, eval, or text/textarea setting)
 *
 * To disable ALL access to JavaScript (as, for example, in a Wiki) 
 * remove the MAYSCRIPT tag or set MAYSCRIPT="false"
 * 
 * To set a maximum size for the applet if resizable:
 *
 * [param name="maximumSize" value="nnnn" /]
 * 
 * 
 * You can specify that the "signed" (privileged, all-permissions) or 
 * "unsigned" (actually, just sandboxed)applet or application should
 * use an independent command thread (EXCEPT for scripts containing 
 * the "javascript" command)  
 * 
 * [param name="useCommandThread" value="true"]
 * 
 * You can specify a language (French in this case) using  
 * 
 * [param name="language" value="fr"]
 * 
 * You can check that it is set correctly using 
 * 
 * [param name="debug" value="true"]
 *  
 *  or
 *  
 * [param name="logLevel" value="5"]
 * 
 * and then checking the console for a message about MAYSCRIPT
 * 
 * In addition, you can turn off JUST EVAL, by setting on the web page
 * 
 * _jmol.noEval = true or Jmol._noEval = true
 * 
 * This allows callbacks but does not allow the script constructs: 
 * 
 *  script javascript:...
 *  javascript ...
 *  x = javascript(...) 
 *  
 * However, this can be overridden by adding an evalCallback function 
 * This MUST be defined along with applet loading using a <param> tag
 * Easiest way to do this is to define
 * 
 * jmolSetCallback("evalCallback", "whateverFunction")
 * 
 * prior to the jmolApplet() command
 * 
 * This is because the signed applet was having trouble finding _jmol in 
 * Protein Explorer
 * 
 * see org.jmol.viewer.JC for callback types.
 * 
 * 
 * new for Jmol 11.9.11:
 * 
 * [param name="multiTouchSparshUI" value="true"]
 * [param name="multiTouchSparshUI-simulated" value="true"]
 * 
 * (signed applet only) loads the SparshUI client adapter
 *  requires JmolMultiTouchDriver.exe (HP TouchSmart computer only)
 *  Uses 127.0.0.1 port 5946 (client) and 5947 (device). 
 *  (see http://code.google.com/p/sparsh-ui/)
 * 
 */

public class Jmol extends GenericApplet implements WrappedApplet {

  private boolean isUpdating;
  private boolean showPaintTime;

  private int timeLast, timeCount, timeTotal;
  private int lastMotionEventNumber;
  private long timeBegin;

  private FileDropper dropper;

  private JApplet applet;

  static {
    /**
     *  @j2sNative
     *  
     *  
     *  self.Jmol || (Jmol = self.J2S); 
     *  Jmol._isSwingJS = true; Jmol._isAWTjs = true;
     */
    
  }

  public static JSUtilI jsutil;

  static {
    try {
      if (isJS) {
        jsutil = ((JSUtilI) Class.forName("swingjs.JSUtil").newInstance());
      }

    } catch (Exception e) {
      System.err.println("Assets could not create swinjs.JSUtil instance");
    }
  }


  public Jmol() {
    //
  }

  /*
   * see below public String getAppletInfo() { return appletInfo; }
   * 
   * static String appletInfo = GT.$("Jmol Applet. Part of the OpenScience
   * project. " + "See http://www.jmol.org for more information");
   */
  @Override
  public void setApplet(Object a, boolean isSigned) {
    appletObject= a;
    applet = (JApplet) a;
    this.isSigned = isSigned;
    init(appletObject);
    if (isSigned) {
      //System.out.println("starting file dropper");
      try {
        dropper = new FileDropper(null, viewer, null);
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  @Override
  public void paint(Graphics g) {
    //paint is invoked for system-based updates (obscurring, for example)
    //Opera has a bug in relation to displaying the Java Console. 
    update(g, "paint ");
  }

  @Override
  public void update(Graphics g) {
    //update is called in response to repaintManager's repaint() request.
    update(g, "update");
  }

  /*
   * miguel 2004 11 29
   * 
   * WARNING! DANGER!
   * 
   * I have discovered that if you call JSObject.getWindow().toString() on
   * Safari v125.1 / Java 1.4.2_03 then it breaks or kills Safari I filed Apple
   * bug report #3897879
   * 
   * Therefore, do *not* call System.out.println("" + jsoWindow);
   */

  @Override
  public void destroy() {
    super.destroy();
    if (dropper != null) {
      dropper.dispose();
      dropper = null;
    }
    System.out.println("Jmol applet " + fullName + " destroyed");
  }

  @Override
  public Object setStereoGraphics(boolean isStereo) {
    return (isStereo ? applet.getGraphics() : null);
  }

  @Override
  protected void initOptions() {
//    if (isJS) {
//      super.initOptions();
//      return;
//    }
//    /** @j2sIgnore */
    {

      String ms = getJmolParameter("mayscript");
      if (ms == null)
        ms = getJmolParameter("allowjavascript");
      mayScript = (ms != null) && (!ms.equalsIgnoreCase("false"));
      // using JApplet calls for older installations
      URL base = applet.getDocumentBase();
      documentBase = (base == null ? getValue("documentBase", null)
          : base.toString());
      base = applet.getCodeBase();
      codeBase = (base == null
          ? getValue("codePath", getValue("codeBase", null))
          : base.toString());
      if (codeBase != null && !codeBase.endsWith("/"))
        codeBase += "/";
      vwrOptions = new Hashtable<String, Object>();
      isSigned = true;//|= isJNLP || getBooleanValue("signed", false);
      if (isSigned)
        addValue(vwrOptions, null, "signedApplet", Boolean.TRUE);
      if (getBooleanValue("useCommandThread", isSigned && !isJS))
        addValue(vwrOptions, null, "useCommandThread", Boolean.TRUE);
      String options = "";
      if (isSigned && getBooleanValue("multiTouchSparshUI-simulated", false))
        options += "-multitouch-sparshui-simulated";
      else if (isSigned && getBooleanValue("multiTouchSparshUI", false)) // true for testing JmolAppletSignedMT.jar
        options += "-multitouch-sparshui";
      addValue(vwrOptions, null, "options", options);
      addValue(vwrOptions, null, "display", applet);
      addValue(vwrOptions, null, "fullName", fullName);
      addValue(vwrOptions, null, "documentBase", documentBase);
      addValue(vwrOptions, null, "codePath", codeBase);
      if (getBooleanValue("noScripting", false))
        addValue(vwrOptions, null, "noScripting", Boolean.TRUE);
      addValue(vwrOptions, "MaximumSize", "maximumSize", null);
      addValue(vwrOptions, "JmolAppletProxy", "appletProxy", null);
      addValue(vwrOptions, "documentLocation", null, null);
      try {
        UIManager
            .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Throwable exc) {
        System.err.println("Error loading L&F: " + exc);
      }
      if (Logger.debugging) {
        Logger.debug("checking for jsoWindow mayScript=" + mayScript);
      }
      if (mayScript) {
        mayScript = haveDocumentAccess = false;
        JSObject jsoWindow = null;
        JSObject jsoDocument = null;
        try {
          jsoWindow = JSObject.getWindow(applet);
          if (Logger.debugging) {
            Logger.debug("jsoWindow=" + jsoWindow);
          }
          if (jsoWindow == null) {
            Logger.error(
                "jsoWindow returned null ... no JavaScript callbacks :-(");
          } else {
            mayScript = true;
          }
          jsoDocument = (JSObject) jsoWindow.getMember("document");
          if (jsoDocument == null) {
            Logger.error(
                "jsoDocument returned null ... no DOM manipulations :-(");
          } else {
            haveDocumentAccess = true;
          }
        } catch (Exception e) {
          // ignore
        }
        if (Logger.debugging) {
          Logger.debug("jsoWindow:" + jsoWindow + " jsoDocument:" + jsoDocument
              + " mayScript:" + mayScript + " haveDocumentAccess:"
              + haveDocumentAccess);
        }
        if (!isJS)
           cleanRegistry();
      }
    }
  }

  private void addValue(Map<String, Object> info, String key, String putKey,
                        Object value) {
    if (key != null)
      value = getValue(key, null);
    if (value != null)
      info.put(putKey == null ? key : putKey, value);
    boolean haveCallback = false;
    // these are set by vwr.setStringProperty() from setValue
    for (CBK item : CBK.values()) {
      if (callbacks.get(item) != null) {
        haveCallback = true;
        break;
      }
    }
    // the following is not continued in Jmol 14 JavaScript

    if (haveCallback || statusForm != null || statusText != null) {
      if (!mayScript)
        Logger
            .warn("MAYSCRIPT missing -- all applet JavaScript calls disabled");
    }
    statusForm = getValue("StatusForm", null);
    statusText = getValue("StatusText", null); // text
    statusTextarea = getValue("StatusTextarea", null); // textarea

    if (statusForm != null && statusText != null) {
      Logger.info("applet text status will be reported to document."
          + statusForm + "." + statusText);
    }
    if (statusForm != null && statusTextarea != null) {
      Logger.info("applet textarea status will be reported to document."
          + statusForm + "." + statusTextarea);
    }

  }

  /**
   * From back in the good odl days when applets could talk across tabs to each other!
   * 
   * @j2sIgnore
   */
  @Deprecated
  synchronized private static void cleanRegistry() {
    return;
  }

  @Override
  protected String getJmolParameter(String paramName) {
    return applet.getParameter(paramName);
  }

  /**
   * 
   * @param g
   * @param source
   *        for debugging only
   */
  private void update(Graphics g, String source) {
    if (viewer == null) // it seems that this can happen at startup sometimes
      return;
    if (isUpdating)
      return;

    //Opera has been known to allow entry to update() by one thread
    //while another thread is doing a paint() or update(). 

    //for now, leaving out the "needRendering" idea

    isUpdating = true;
    if (showPaintTime)
      startPaintClock();
    Dimension size = new Dimension();
    applet.getSize(size);
    viewer.setScreenDimension(size.width, size.height);
    if (!isStereoSlave)
      viewer.renderScreenImageStereo(g, true, size.width, size.height);
    if (showPaintTime) {
      stopPaintClock();
      showTimes(10, 10, g);
    }
    isUpdating = false;
  }

  // code to record last and average times
  // last and average of all the previous times are shown in the status window

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
    int motionEventNumber = viewer.getMotionEventNumber();
    if (lastMotionEventNumber != motionEventNumber) {
      lastMotionEventNumber = motionEventNumber;
      timeCount = timeTotal = 0;
      timeLast = -1;
    }
  }

  private void stopPaintClock() {
    int time = (int) (System.currentTimeMillis() - timeBegin);
    if (timeLast != -1) {
      timeTotal += timeLast;
      ++timeCount;
    }
    timeLast = time;
  }

  private String fmt(int num) {
    if (num < 0)
      return "---";
    if (num < 10)
      return "  " + num;
    if (num < 100)
      return " " + num;
    return "" + num;
  }

  private void showTimes(int x, int y, Graphics g) {
    int timeAverage = (timeCount == 0) ? -1 : (timeTotal + timeCount / 2)
        / timeCount; // round, don't truncate
    g.setColor(Color.green);
    g.drawString(fmt(timeLast) + "ms : " + fmt(timeAverage) + "ms", x, y);
  }

  //  @Override
  //  public String loadNodeId(String nodeId) {
  //    if (!haveDocumentAccess)
  //      return "ERROR: NO DOCUMENT ACCESS";
  //    if (nodeId == null)
  //      return null;
  //    // Retrieve Node ...
  //    // First try to find by ID
  //    Object[] idArgs = { nodeId };
  //    JSObject tryNode = null;
  //    try {
  //      JSObject jsoWindow = JSObject.getWindow(appletWrapper);
  //      JSObject jsoDocument = (JSObject) jsoWindow.getMember("document");
  //      tryNode = (JSObject) jsoDocument.call("getElementById", idArgs);
  //
  //      // But that relies on a well-formed CML DTD specifying ID search.
  //      // Otherwise, search all cml:cml nodes.
  //      if (tryNode == null) {
  //        Object[] searchArgs = { "http://www.xml-cml.org/schema/cml2/core",
  //            "cml" };
  //        JSObject tryNodeList = (JSObject) jsoDocument.call(
  //            "getElementsByTagNameNS", searchArgs);
  //        if (tryNodeList != null) {
  //          for (int i = 0; i < ((Number) tryNodeList.getMember("length"))
  //              .intValue(); i++) {
  //            tryNode = (JSObject) tryNodeList.getSlot(i);
  //            Object[] idArg = { "id" };
  //            String idValue = (String) tryNode.call("getAttribute", idArg);
  //            if (nodeId.equals(idValue))
  //              break;
  //            tryNode = null;
  //          }
  //        }
  //      }
  //    } catch (Exception e) {
  //      return "" + e;
  //    }
  //    return (tryNode == null ? "ERROR: No CML node" : loadDOMNode(tryNode));
  //  }

  @Override
  public int[] resizeInnerPanel(String data) {
    if (isJS)
      super.resizeInnerPanel(data);
    return null;
  }

  @Override
  protected String doSendCallback(CBK type, Object callback, Object[] data,
                                  String strInfo) {
    if (isJS) {
      return super.doSendCallback(type, callback, data, strInfo);
    } 
    return null;
  }
  
  @Override
  protected String doEval(String strEval) {
    if (isJS) {
      return super.doEval(strEval);
    }
    return null;
  }

  @Override
  public float[][] functionXY(String functionName, int nX, int nY) {
    if (isJS) {
      return super.functionXY(functionName, nX, nY);
    }
    return null;
  }

  @Override
  public float[][][] functionXYZ(String functionName, int nX, int nY,
                                   int nZ) {
    if (isJS) {
      return super.functionXYZ(functionName, nX, nY, nZ);
    }
    return null;
  }

  @Override
  protected void doShowDocument(URL url) {
    applet.getAppletContext().showDocument(url, "_blank");
  }

  @Override
  protected void doShowStatus(String message) {
    try {
      if (!isJS)
        applet.showStatus(PT.rep(PT.split(message, "\n")[0], "'", "\\'"));
    } catch (Exception e) {
      //ignore if page is closing
    }
  }

}
