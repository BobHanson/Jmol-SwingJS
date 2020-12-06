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

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;
import swingjs.api.JSUtilI;

import javax.swing.UIManager;

import netscape.javascript.JSObject; // Java applet only

import org.jmol.awt.FileDropper;
import org.jmol.c.CBK;
import org.jmol.util.GenericApplet;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.Viewer;

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

  private Applet applet;

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
  public void setApplet(Applet a, boolean isSigned) {
    appletObject = applet = a;
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
    if (isJNLP)
      System.exit(0);
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
      isSigned |= isJNLP || getBooleanValue("signed", false);
      if (isSigned)
        addValue(vwrOptions, null, "signedApplet", Boolean.TRUE);
      if (getBooleanValue("useCommandThread", isSigned))
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
      if (isJNLP)
        addValue(vwrOptions, null, "isJNLP", Boolean.TRUE);
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
    if (isJS)
      return;
    Applet app = null;
    boolean closed = true;
    for (Map.Entry<String, Object> entry : htRegistry.entrySet()) {
      String theApplet = entry.getKey();
      try {
        app = (Applet) (entry.getValue());
        JSObject theWindow = JSObject.getWindow(app);
        //System.out.print("checking " + app + " window : ");
        closed = ((Boolean) theWindow.getMember("closed")).booleanValue();
        //System.out.println(closed);
        if (closed || theWindow.hashCode() == 0) {
          //error trap
        }
        if (Logger.debugging)
          Logger.debug("Preserving registered applet " + theApplet
              + " window: " + theWindow.hashCode());
      } catch (Exception e) {
        closed = true;
      }
      if (closed) {
        if (Logger.debugging)
          Logger.debug("Dereferencing closed window applet " + theApplet);
        htRegistry.remove(theApplet);
        app.destroy();
      }
    }
  }

  @Override
  protected String getJmolParameter(String paramName) {
    return applet.getParameter(paramName);
  }

  @Override
  protected void doSendJsTextStatus(String message) {
    if (isJS) {
      super.doSendJsTextStatus(message);
      return;
    }
    if (!haveDocumentAccess || statusForm == null || statusText == null)
      return;
    try {

      JSObject jsoWindow = JSObject.getWindow(applet);
      JSObject jsoDocument = (JSObject) jsoWindow.getMember("document");
      JSObject jsoForm = (JSObject) jsoDocument.getMember(statusForm);
      if (statusText != null) {
        JSObject jsoText = (JSObject) jsoForm.getMember(statusText);
        jsoText.setMember("value", message);
      }
    } catch (Exception e) {
      Logger.error("error indicating status at document." + statusForm + "."
          + statusText + ":" + e.toString());
    }
  }

  @Override
  protected void doSendJsTextareaStatus(String message) {
    if (isJS) {
      super.doSendJsTextareaStatus(message);
      return;
    } else /** @j2sIgnore */
    {
      if (!haveDocumentAccess || statusForm == null || statusTextarea == null)
        return;
      try {
        JSObject jsoWindow = JSObject.getWindow(applet);
        JSObject jsoDocument = (JSObject) jsoWindow.getMember("document");
        JSObject jsoForm = (JSObject) jsoDocument.getMember(statusForm);
        if (statusTextarea != null) {
          JSObject jsoTextarea = (JSObject) jsoForm.getMember(statusTextarea);
          if (message == null) {
            jsoTextarea.setMember("value", "");
          } else {
            String info = (String) jsoTextarea.getMember("value");
            jsoTextarea.setMember("value", info + "\n" + message);
          }
        }
      } catch (Exception e) {
        Logger.error("error indicating status at document." + statusForm + "."
            + statusTextarea + ":" + e.toString());
      }
    }
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
  protected String doSendCallback(String callback, Object[] data,
                                  String strInfo) {
    if (isJS) {
      return super.doSendCallback(callback, data, strInfo);
    } else /** @j2sIgnore */
    {
      if (callback == null) {
        // from reading of the DOM by the Java applet only
        getJsObjectInfo(data);
        return null;
      }
      JSObject jso = JSObject.getWindow(applet);
      if (callback.equals("alert")) {
        jso.call(callback, new Object[] { strInfo });
        return "";
      }
      if (callback.length() == 0)
        return "";
      //System.out.println(callback);
      if (callback.indexOf(".") > 0) {
        String[] mods = PT.split(callback, ".");
        //System.out.println(Escape.eAS(mods, true));
        for (int i = 0; i < mods.length - 1; i++) {
          //System.out.println(jso);
          jso = (JSObject) jso.getMember(mods[i]);
          //System.out.println(jso);
        }
        callback = mods[mods.length - 1];
      }
      //System.out.println("OK -- calling " + jso + " " + callback + " " + data);
      return "" + jso.call(callback, data);
    }
  }
  
  /**
   * probably never used -- only by Java applet reading directly from the DOM in an XHTML document.
   * 
   * @j2sIgnore
   * 
   * @param data
   */
  private void getJsObjectInfo(Object[] data) {
    data[0] = null;
    try {
      String method = (String) data[1];
      Object[] jsObject = (Object[]) data[2];
      Object[] args = (Object[]) data[3];
      JSObject DOMNode = (JSObject) jsObject[0];
      if (method == null) {
        String namespaceURI = (String) DOMNode.getMember("namespaceURI");
        String localName = (String) DOMNode.getMember("localName");
        data[0] = "namespaceURI=\"" + namespaceURI + "\" localName=\""
            + localName + "\"";
      } else {
        data[0] = (args == null ? DOMNode.getMember(method) : DOMNode.call(
            method, args));
      }
    } catch (Exception e) {
      //
    }
  }



  private Boolean allowJSEval;
  private JSObject jsoDocument = null;

  @Override
  protected String doEval(String strEval) {
    if (isJS) {
      return super.doEval(strEval);
    } else /** @j2sIgnore */
    {
      JSObject jsoWindow = null;
      if (allowJSEval == null) {
        try {
          jsoDocument = (JSObject) JSObject.getWindow(applet)
              .getMember("document");
          try {
            if (((Boolean) jsoDocument.eval("!!Jmol._noEval")).booleanValue())
              allowJSEval = Boolean.FALSE;
          } catch (Exception e) {
            try {
              if (((Boolean) jsoDocument.eval("!!_jmol.noEval")).booleanValue())
                allowJSEval = Boolean.FALSE;
            } catch (Exception e2) {
              allowJSEval = Boolean.FALSE;
              Logger.error("# no Jmol or _jmol object in evaluating " + strEval
                  + ":" + e.toString());
            }
          }
        } catch (Exception e) {
          if (Logger.debugging)
            Logger.debug(" error setting jsoWindow or jsoDocument:" + jsoWindow
                + ", " + jsoDocument);
          allowJSEval = Boolean.FALSE;
        }
      }
      if (allowJSEval == Boolean.FALSE) {
        jsoDocument = null;
        return "NO EVAL ALLOWED";
      }
      try {
        return "" + jsoDocument.eval(strEval);
      } catch (Exception e) {
        Logger.error("# error evaluating " + strEval + ":" + e.toString());
        return "";
      }
    }
  }

  @Override
  public float[][] doFunctionXY(String functionName, int nX, int nY) {
    if (isJS) {
      return super.doFunctionXY(functionName, nX, nY);
    } else /** @j2sIgnore */{
    /*three options:
     * 
     *  nX > 0  and  nY > 0        return one at a time, with (slow) individual function calls
     *  nX < 0  and  nY > 0        return a string that can be parsed to give the list of values
     *  nX < 0  and  nY < 0        fill the supplied float[-nX][-nY] array directly in JavaScript 
     *  
     */

    //System.out.println("functionXY" + nX + " " + nY  + " " + functionName);
    float[][] fxy = new float[Math.abs(nX)][Math.abs(nY)];
    if (!mayScript || nX == 0 || nY == 0)
      return fxy;
    try {
      JSObject jsoWindow = JSObject.getWindow(applet);
      if (nX > 0 && nY > 0) { // fill with individual function calls (slow)
        for (int i = 0; i < nX; i++)
          for (int j = 0; j < nY; j++) {
            fxy[i][j] = ((Double) jsoWindow.call(functionName, new Object[] {
                htmlName, Integer.valueOf(i), Integer.valueOf(j) }))
                .floatValue();
          }
      } else if (nY > 0) { // fill with parsed values from a string (pretty fast)
        String data = (String) jsoWindow.call(functionName, new Object[] {
            htmlName, Integer.valueOf(nX), Integer.valueOf(nY) });
        //System.out.println(data);
        nX = Math.abs(nX);
        float[] fdata = new float[nX * nY];
        Parser.parseStringInfestedFloatArray(data, null, fdata);
        for (int i = 0, ipt = 0; i < nX; i++) {
          for (int j = 0; j < nY; j++, ipt++) {
            fxy[i][j] = fdata[ipt];
          }
        }
      } else { // fill float[][] directly using JavaScript
        jsoWindow.call(functionName,
            new Object[] { htmlName, Integer.valueOf(nX), Integer.valueOf(nY),
                fxy });
      }
    } catch (Exception e) {
      Logger.error("Exception " + e.getMessage() + " with nX, nY: " + nX + " "
          + nY);
    }
    // for (int i = 0; i < nX; i++)
    // for (int j = 0; j < nY; j++)
    //System.out.println("i j fxy " + i + " " + j + " " + fxy[i][j]);
    return fxy;
    }
  }

  @Override
  public float[][][] doFunctionXYZ(String functionName, int nX, int nY,
                                   int nZ) {
    if (isJS) {
      return super.doFunctionXYZ(functionName, nX, nY, nZ);
    } else /** @j2sIgnore */
    {
      float[][][] fxyz = new float[Math.abs(nX)][Math.abs(nY)][Math.abs(nZ)];
      if (!mayScript || nX == 0 || nY == 0 || nZ == 0)
        return fxyz;
      try {
        JSObject jsoWindow = JSObject.getWindow(applet);
        jsoWindow.call(functionName,
            new Object[] { htmlName, Integer.valueOf(nX), Integer.valueOf(nY),
                Integer.valueOf(nZ), fxyz });
      } catch (Exception e) {
        Logger.error("Exception " + e.getMessage() + " for " + functionName
            + " with nX, nY, nZ: " + nX + " " + nY + " " + nZ);
      }
      // for (int i = 0; i < nX; i++)
      // for (int j = 0; j < nY; j++)
      // for (int k = 0; k < nZ; k++)
      //System.out.println("i j k fxyz " + i + " " + j + " " + k + " " + fxyz[i][j][k]);
      return fxyz;
    }
  }

  @Override
  protected void doShowDocument(URL url) {
    applet.getAppletContext().showDocument(url, "_blank");
  }

  @Override
  protected void doShowStatus(String message) {
    try {
//      System.out.println(message);
      applet.showStatus(PT.rep(PT.split(message, "\n")[0], "'", "\\'"));
      doSendJsTextStatus(message);
    } catch (Exception e) {
      //ignore if page is closing
    }
  }

}
