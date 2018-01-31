/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-07-29 06:14:56 -0500 (Wed, 29 Jul 2015) $
 * $Revision: 20664 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import java.util.Map;

public interface JmolStatusListener extends JmolCallbackListener {
/*
 * These methods specifically provide notification from 
 * Viewer.StatusManager to the two main classes, applet or app.
 * so that they can handle them slightly differently. This might be
 * a callback for the applet, for example, but not for the app.
 * ALL vwr-type processing, including status queue writing
 * has been done PRIOR to these functions being called.   Bob Hanson
 * 
 */

  public String eval(String strEval);
  
  /**
   * for isosurface FUNCTIONXY 
   * 
   * @param functionName
   * @param x
   * @param y
   * @return 2D array or null
   */
  public float[][] functionXY(String functionName, int x, int y);
  
  /**
   * for isosurface FUNCTIONXYZ 
   * 
   * @param functionName
   * @param nx
   * @param ny
   * @param nz
   * @return 3D array or null
   */
  public float[][][] functionXYZ(String functionName, int nx, int ny, int nz);

  /**
   * Starting with Jmol 11.8.RC5, for a context where the Jmol application
   * is embedded in another application simply to send the returned message
   * to the application. In this way any application can have access to the WRITE
   * command.
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes information or null indicates message AFTER Jmol creates the image
   * @param quality
   * @return          null (canceled) or a message starting with OK or an error message
   */
  public String createImage(String fileName, String type, Object text_or_bytes, int quality);

  public Map<String, Object> getRegistryInfo();

  public void showUrl(String url);

  public int[] resizeInnerPanel(String data);

  public Map<String, Object> getJSpecViewProperty(String type);

}
