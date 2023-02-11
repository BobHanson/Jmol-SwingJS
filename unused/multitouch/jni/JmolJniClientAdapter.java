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
package org.jmol.multitouch.jni;

//import org.jmol.multitouch.JmolMultiTouchClient;
import org.jmol.multitouch.JmolMultiTouchClientAdapter;
//import org.jmol.viewer.Viewer;

public class JmolJniClientAdapter extends JmolMultiTouchClientAdapter {

  // not implemented. A stub only for a potential Java Native Interface.
  // not currently in applet.classes
  // would need to set isServer flag

//  static {
//    System.loadLibrary("JmolMultiTouchJNI");
//  }  
// 
//  native void nativeMethod(); // should report "In C\nIn Java\n"

  @Override
  public void dispose() {
    //TODO
  }

  /*
  @Override
  public boolean setMultiTouchClient(Viewer vwr, JmolMultiTouchClient client,
                                  boolean isSimulation) {
    try {
      // in principle, we could set up our own device driver here
      // and probably talk to it using ports. SparshUI is easier.
      //TODO
      nativeMethod();
      return true;
    } catch (Exception e) {
      System.out.println("JmolJniClientAdapter error -- nativeMethod");
    }
    return false;
  }
  
  public void callback() {
    // from nativeMethod -- test of callback to Java from C++
    //TODO
    System.out.println("In Java");
  }
*/
}
