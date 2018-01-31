/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-31 09:22:19 -0500 (Fri, 31 Jul 2009) $
 * $Revision: 11291 $
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
package org.jmol.multitouch;


import org.jmol.util.Logger;
import javajs.util.P3;
import org.jmol.viewer.Viewer;

public abstract class JmolMultiTouchClientAdapter implements JmolMultiTouchAdapter {

  protected JmolMultiTouchClient actionManager;
  protected boolean isServer;
  private Viewer vwr;
  private int[] screen = new int[2];  
  
  @Override
  public boolean isServer() {
    return isServer;
  }
  
  // methods Jmol needs -- from vwr.ActionManagerMT

  @Override
  public abstract void dispose();

  @Override
  public boolean setMultiTouchClient(Viewer vwr, JmolMultiTouchClient client,
                              boolean isSimulation) {
    this.vwr = vwr;
    actionManager = client; // ActionManagerMT
    vwr.apiPlatform.getFullScreenDimensions(vwr.display, screen);
    if (Logger.debugging)
      Logger.debug("screen resolution: " + screen[0] + " x " + screen[1]);
    return true;
  }
  
  @Override
  public void mouseMoved(int x, int y) {
    // for debugging purposes
    //System.out.println("mouseMove " + x + " " + y);
  }

  protected P3 ptTemp = new P3();
  protected void fixXY(float x, float y, boolean isAbsolute) {
    ptTemp.set(x * screen[0], y * screen[1], Float.NaN);
    if (isAbsolute)
      vwr.apiPlatform.convertPointFromScreen(vwr.display, ptTemp);
  }
} 
