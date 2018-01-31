/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.openscience.jmol.app;

import javajs.util.PT;

import org.jmol.util.Escape;
import org.jmol.viewer.Viewer;

public class JmolData {
  
  /*
   * no Java Swing to be found. No implementation of any graphics or 
   * containers at all. No shapes, no export, no writing of images,
   *  -- only the model and its associated data.
   * 
   * Just a great little answer machine that can load models, 
   * do scripted analysis of their structures, and spit out text
   * 
   */

  public JmolApp jmolApp;
  public Viewer vwr;
  
  public static JmolData getJmol(int width, int height, String commandOptions) {
    JmolApp jmolApp = new JmolApp();
    jmolApp.startupHeight = height;
    jmolApp.startupWidth = width;
    jmolApp.haveConsole = false;
    jmolApp.isDataOnly = true;
    //jmolApp.info.put("exit", Boolean.TRUE);

    String[] args = PT.split(commandOptions, " "); // doesn't allow for double-quoted 
    jmolApp.parseCommandLine(args);
    return new JmolData(jmolApp);
  }

  public static void main(String[] args) {
    // note that -o-x are implied, but -n is not. 
    // in this case -n means "no GRAPHICS" for speed
    JmolApp jmolApp = new JmolApp();
    jmolApp.isDataOnly = true;
    jmolApp.haveConsole = false;
    //jmolApp.haveDisplay = false;
    jmolApp.info.put("exit", Boolean.TRUE);
    jmolApp.info.put("isDataOnly", Boolean.TRUE);
    jmolApp.parseCommandLine(args);
    if (!jmolApp.isSilent) {
      System.out.println("JmolData using command options " + Escape.e(args));
      System.out.println(jmolApp.info);
//      if (jmolApp.info.containsKey("noDisplay"))
//        jmolApp.info.put("noGraphics", Boolean.TRUE);
//      else
//        System.out.println("Add -n (no GRAPHICS) for faster performance if you are not creating images.");
    }
    new JmolData(jmolApp);
  }
  
  private JmolData(JmolApp jmolApp) {
    this.jmolApp = jmolApp;
    vwr = new Viewer(jmolApp.info);
    vwr.setScreenDimension(jmolApp.startupWidth, jmolApp.startupHeight);
    vwr.setWidthHeightVar();
    jmolApp.startViewer(vwr, null, true);
  }
  
}  

