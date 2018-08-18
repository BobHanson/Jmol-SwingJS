/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-09-11 19:29:26 -0500 (Tue, 11 Sep 2012) $
 * $Revision: 17556 $
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

package org.jmol.appletjs;

import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javajs.api.JSInterface;
import javajs.util.PT;

import org.jmol.api.js.JmolToJSmolInterface;
import org.jmol.util.GenericApplet;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

/**
 * Java2Script rendition of Jmol using HTML5-only or WebGL-based graphics
 * 
 * @author Bob Hanson hansonr@stolaf.edu, Takanori Nakane, with the assistance  
 *         of Jhou Renjian
 * 
 */

public class Jmol extends GenericApplet implements JSInterface {

  private Map<String, Object> htParams;

  /**
   * cannot ignore super because it involves preparing fields
   * @param vwrOptions 
   * 
   */
  public Jmol(Map<String, Object> vwrOptions) {
    htParams = new Hashtable<String, Object>();
    if (vwrOptions == null)
      vwrOptions = new Hashtable<String, Object>();
    this.vwrOptions = vwrOptions;
    for (Map.Entry<String, Object> entry : vwrOptions.entrySet())
      htParams.put(entry.getKey().toLowerCase(), entry.getValue());
    documentBase = "" + vwrOptions.get("documentBase");
    codeBase = "" + vwrOptions.get("codePath");
    isJS = true;
    init(this);
  }
  
  @Override
  public Object setStereoGraphics(boolean isStereo) {
    /**
     * @j2sNative
     * 
     * if (isStereo)
     *   return vwr.apiPlatform.context;
     * 
     */
    {
    }
    return null;
  }

  @Override
  protected void initOptions() {
    vwrOptions.remove("debug");
    vwrOptions.put("fullName", fullName);
    haveDocumentAccess = "true".equalsIgnoreCase(""
        + getValue("allowjavascript", "true"));
    mayScript = true;
  }

  @Override
  protected String getJmolParameter(String paramName) {
    Object o = htParams.get(paramName.toLowerCase());
    return (o == null ? null : "" + o);
  }

  @Override
  protected void doSendJsTextStatus(String message) {
    System.out.println(message);
    // not implemented
  }

  @Override
  protected void doSendJsTextareaStatus(String message) {
    System.out.println(message);
    // not implemented
  }

  @Override
  protected float[][] doFunctionXY(String functionName, int nX, int nY) {
    /*three options:
     * 
     *  nX > 0  and  nY > 0        return one at a time, with (slow) individual function calls
     *  nX < 0  and  nY > 0        return a string that can be parsed to give the list of values
     *  nX < 0  and  nY < 0        fill the supplied float[-nX][-nY] array directly in JavaScript 
     *  
     */

    //System.out.println("functionXY" + nX + " " + nY  + " " + functionName);
    float[][] fxy = new float[Math.abs(nX)][Math.abs(nY)];
    if (!mayScript || !haveDocumentAccess || nX == 0 || nY == 0)
      return fxy;
    try {
      if (nX > 0 && nY > 0) { // fill with individual function calls (slow)
        for (int i = 0; i < nX; i++)
          for (int j = 0; j < nY; j++) {
            /**
             * @j2sNative
             * 
             *            fxy[i][j] = window.eval(functionName)(this.htmlName, i, j);
             */
            {
            }
          }
      } else if (nY > 0) { // fill with parsed values from a string (pretty fast)
        String data;
        /**
         * @j2sNative
         * 
         *            data = window.eval(functionName)(this.htmlName, nX, nY);
         * 
         */
        {
          data = "";
        }
        nX = Math.abs(nX);
        float[] fdata = new float[nX * nY];
        Parser.parseStringInfestedFloatArray(data, null, fdata);
        for (int i = 0, ipt = 0; i < nX; i++) {
          for (int j = 0; j < nY; j++, ipt++) {
            fxy[i][j] = fdata[ipt];
          }
        }
      } else { // fill float[][] directly using JavaScript
        /**
         * @j2sNative
         * 
         *            data = window.eval(functionName)(this.htmlName, nX, nY, fxy);
         * 
         */
        {
          System.out.println(functionName);
        }
      }
    } catch (Exception e) {
      Logger.error("Exception " + e + " with nX, nY: " + nX + " " + nY);
    }
    // for (int i = 0; i < nX; i++)
    // for (int j = 0; j < nY; j++)
    //System.out.println("i j fxy " + i + " " + j + " " + fxy[i][j]);
    return fxy;
  }

  @Override
  protected float[][][] doFunctionXYZ(String functionName, int nX, int nY,
                                      int nZ) {
    float[][][] fxyz = new float[Math.abs(nX)][Math.abs(nY)][Math.abs(nZ)];
    if (!mayScript || !haveDocumentAccess || nX == 0 || nY == 0 || nZ == 0)
      return fxyz;
    try {
      /**
       * @j2sNative
       * 
       *            window.eval(functionName)(this.htmlName, nX, nY, nZ, fxyz);
       * 
       */
      {
      }
    } catch (Exception e) {
      Logger.error("Exception " + e + " for " + functionName
          + " with nX, nY, nZ: " + nX + " " + nY + " " + nZ);
    }
    // for (int i = 0; i < nX; i++)
    // for (int j = 0; j < nY; j++)
    // for (int k = 0; k < nZ; k++)
    //System.out.println("i j k fxyz " + i + " " + j + " " + k + " " + fxyz[i][j][k]);
    return fxyz;
  }

  @Override
  protected void doShowDocument(URL url) {
    String[] surl = PT.split(url.toString(), "?POST?");
    if (surl.length == 1) {
      /**
       * @j2sNative
       * 
       *            window.open(surl[0]);
       * 
       */
    {}
     return;
      }
   
    String f = "<form id=f method=POST action='" + surl[0] + "'>";
    f += "<input type='hidden' name='name' value='nmr-1h-prediction' id='name'>";
    f += "<input type='submit' value='working...'>";
    String[] fields = surl[1].split("&");
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];
      int pt = field.indexOf("=");
      String name = field.substring(0, pt);
      String value = field.substring(pt);
      if (value.indexOf("\n") >= 0) {
        f +="<textarea style='display:none' name=" + name + ">" + value +"</textarea>";
      } else {
        f +="<input type=hidden name=" + name + " value=\""+ value +"\">";
      }
    }
    f += "</form>";
    /**
     * @j2sNative
     * var w=window.open("");w.document.write(f);w.document.getElementById("f").submit();
     * 
     */
    {
      System.out.println(f + url);
    }
  }

  @Override
  protected String doSendCallback(String callback, Object[] data, String strInfo) {
    if (callback == null || callback.length() == 0) {
    } else if (callback.equals("alert")) {
      /**
       * @j2sNative alert(strInfo); return "";
       */
      {
        System.out.println(strInfo);
      }
    } else {
      String[] tokens = PT.split(callback, ".");
      /**
       * @j2sNative
       * 
       *            try{ 
       *            var o = window[tokens[0]]; 
       *            for (var i = 1; i < tokens.length; i++) 
       *              o = o[tokens[i]];
       *            for (var i = 0; i < data.length; i++) 
       *              data[i] && data[i].booleanValue && (data[i] = data[i].booleanValue());
       *            return o.apply(null,data)
       *            } catch (e) { System.out.println(callback + " failed " + e); }
       */
      {
        System.out.println(tokens + " " + data);
      }
    }
    return "";
  }

  /**
   * return RAW JAVASCRIPT OBJECT, NOT A STRING 
   */
  @Override
  protected String doEval(String strEval) {
    try {
      /**
       * 
       * @j2sNative
       * 
       *            return window.eval(strEval);
       */
      {
      }
    } catch (Exception e) {
      Logger.error("# error evaluating " + strEval + ":" + e.toString());
    }
    return "";
  }

  @Override
  protected void doShowStatus(String message) {
    try {
      System.out.println(message);
    } catch (Exception e) {
      //ignore if page is closing
    }
  }

  /**
   * This method is only called by JmolGLmol applet._refresh();
   * 
   * @return enough data to update a WebGL view
   * 
   */
  public Object getGLmolView() {
    return viewer.getGLmolView();
  }

  /**
   * possibly called from JSmolApplet.js upon start up
   *  
   * @param fileName
   * @return error or null
   */
  
  public String openFile(String fileName) {
    return viewer.openFile(fileName);
  }

  // JSInterface -- methods called from JSmol JavaScript library
  
  @Override
  public int cacheFileByName(String fileName, boolean isAdd) {
    return viewer.cacheFileByName(fileName, isAdd);
  }

  @Override
  public void cachePut(String key, Object data) {
    viewer.cachePut(key, data);
  }

  @Override
  public String getFullName() {
    return fullName;
  }

  @Override
  public boolean processMouseEvent(int id, int x, int y, int modifiers,
                                   long time) {
    return viewer.processMouseEvent(id, x, y, modifiers, time);
  }

  @Override
  public void setDisplay(Object canvas) {
    viewer.setDisplay(canvas);
  }

  @Override
  public boolean setStatusDragDropped(int mode, int x, int y, String fileName) {
    return viewer.setStatusDragDropped(mode, x, y, fileName);
  }

  @Override
  public void startHoverWatcher(boolean enable) {
    viewer.startHoverWatcher(enable);
  }

  @Override
  public void update() {
    viewer.updateJS();
  }

  @Override
  public void openFileAsyncSpecial(String fileName, int flags) {
    viewer.openFileAsyncSpecial(fileName, flags);
  }

  @Override
  public void processTwoPointGesture(float[][][] touches) {
    viewer.processTwoPointGesture(touches);
  }

  @Override
  public void setScreenDimension(int width, int height) {
    viewer.setScreenDimension(width, height);
  }

  @Override
  public int[] resizeInnerPanel(String data) {
    float[] dims = new float[2];
    Parser.parseStringInfestedFloatArray(data, null, dims);
    resizeDisplay((int) dims[0], (int) dims[1]);
    return new int[] { (int) dims[0], (int) dims[1] };
  }

  void resizeDisplay(int width, int height) {
    JmolToJSmolInterface jmol = org.jmol.awtjs2d.Platform.Jmol();
    jmol.resizeApplet(viewer.html5Applet, new int[] {width, height});
  }

}
