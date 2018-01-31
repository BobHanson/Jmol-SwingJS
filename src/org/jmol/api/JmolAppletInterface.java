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
package org.jmol.api;

import java.awt.Event;


/**
 * This is the API of methods that are available to JavaScript
 * via LiveConnect to the Jmol applet.
 * 
 * DONT FORGET TO ADD THESE FUNCTIONS TO src/JmolApplet.java !!!
 * 
 */

public interface JmolAppletInterface extends JmolSyncInterface {

  public boolean handleEvent(Event event);  
  public String getAppletInfo();
  public void destroy();

  public Object setStereoGraphics(boolean isStereo);
  public void script(String script);
  public String scriptCheck(String script);
  public String scriptWait(String script);
  public String scriptWaitOutput(String script);
  public String scriptWait(String script, String statusParams);
  public String scriptNoWait(String script);

  /**
   * 
   * @param id
   *     some_id,
   *     filename#id, or
   *     ~fileNo.modelNo
   *     
   *     added ".basemodel" indicates to get the first model in a 
   *     series from a JDX-MOL file.
   *     
   * @return model index if found; 
   *       -2 if file found but model not found 
   *       -2 if no file indicated and no model found 
   *       -1 if no such file
   */
  public int getModelIndexFromId(String id);

  public Object getProperty(String infoType);
  public String getPropertyAsString(String infoType);
  public String getPropertyAsString(String infoType, String paramInfo);
  public String getPropertyAsJSON(String infoType);
  public String getPropertyAsJSON(String infoType, String paramInfo);
  public Object getProperty(String infoType, String paramInfo);
  public String loadInlineString(String strModel, String script, boolean isAppend);
  public String loadInlineArray(String[] strModels, String script, boolean isAppend);
  public String loadDOMNode(Object DOMNode);
  public void notifyAudioEnded(Object htParams);

  // Note -- some Macintosh-based browsers cannot distinguish methods
  // with the same name but with different method signatures
  // so the following are not reliable and are thus deprecated
  
  /**
   * @deprecated
   * @param strModel
   * @return         error or null
   */
  @Deprecated
  public String loadInline(String strModel);

  /**
   * @deprecated
   * @param strModels
   * @return         error or null
   */ 
  @Deprecated
  public String loadInline(String[] strModels);

  /**
   * @deprecated
   * @param strModel
   * @param script
   * @return         error or null
   */
  @Deprecated
  public String loadInline(String strModel, String script);

  /**
   * @deprecated
   * @param strModels
   * @param script
   * @return         error or null
   */
  @Deprecated
  public String loadInline(String[] strModels, String script);
}
