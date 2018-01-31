/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-08-30 00:22:54 -0500 (Sat, 30 Aug 2014) $
 * $Revision: 19965 $
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

import org.jmol.api.Interface;
import org.jmol.util.Logger;

class WrappedAppletLoader extends Thread {

  private Applet applet;
  private boolean isSigned;

  //private final static int minimumLoadSeconds = 0;

  WrappedAppletLoader(Applet applet, boolean isSigned) {
    this.applet = applet;
    this.isSigned = isSigned;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    if (Logger.debugging) {
      Logger.debug("WrappedAppletLoader.run(org.jmol.applet.Jmol)");
    }
    TickerThread tickerThread = new TickerThread(applet);
    tickerThread.start();
    try {
      WrappedApplet jmol = ((AppletWrapper) applet).wrappedApplet = (WrappedApplet) Interface.getOption("applet.Jmol", null, null);
      jmol.setApplet(applet, isSigned);
    } catch (Exception e) {
      Logger.errorEx("Could not instantiate applet", e);
    }
    long loadTimeSeconds = (System.currentTimeMillis() - startTime + 500) / 1000;
    if (Logger.debugging)
      Logger.debug("applet load time = " + loadTimeSeconds + " seconds");
    tickerThread.keepRunning = false;
    tickerThread.interrupt();
    applet.repaint();
  }
}

class TickerThread extends Thread {
  Object applet;
  boolean keepRunning = true;

  TickerThread(Applet applet) {
    this.applet = applet;
    this.setName("AppletLoaderTickerThread");
  }

  @Override
  public void run() {
    do {
      try {
        Thread.sleep(999);
      } catch (InterruptedException ie) {
        break;
      }
      ((AppletWrapper) applet).repaintClock();
    } while (keepRunning);
  }
}
