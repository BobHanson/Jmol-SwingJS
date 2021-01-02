package org.jmol;
/* $RCSfile$
 * $Author: Maria Brandl$
 * $10.10.2005$
 * $Revision$
 *
 * Copyright (C) 2000-2006  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *  
 *
 *  JmolExportExample.java
 *  event tracking in a Jmol-application following
 *  Bob Hanson's concept 
 *  compiled with: javac -classpath `pwd`:/where/ever/jmol-10.2.0/Jmol.jar JmolExportExample.java
 *  ran with: java -classpath `pwd`:/where/ever/jmol-10.2.0/Jmol.jar JmolExportExample
 * */

import java.util.Hashtable;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.jmol.api.JmolStatusListener;
import org.jmol.c.CBK;
import org.openscience.jmol.app.Jmol;

public class Export {
  
  public static void main(String args[]) {
    // build TextField (monitor) to track atom info
    JFrame monitorFrame = new JFrame();
    JTextField monitor = new JTextField("Please load a molecule and click on atoms");
    monitorFrame.getContentPane().add(monitor);
    monitorFrame.pack();

    // build Jmol
    JFrame baseframe = new JFrame();
    // build and register event listener (implementation of JmolStatusListener)
    // point "monitor"-variable in event listener to "monitor"
    MyStatusListener myStatusListener = new MyStatusListener();
    myStatusListener.monitor = monitor;
    Map<String, Object> viewerOptions = new Hashtable<String, Object>();
    viewerOptions.put("statusListener", myStatusListener);
    Jmol.getJmol(baseframe, 300, 300, viewerOptions);
    // showing monitor frame on top of Jmol
    monitorFrame.setVisible(true);
  } 
} 

class MyStatusListener implements JmolStatusListener {
  // JTextField monitor used to broadcast atom tracking out of Jmol
  public JTextField monitor;
  
  @Override
  public boolean notifyEnabled(CBK type) {
    // indicate here any callbacks you will be working with.
    // some of these flags are not tested. See org.jmol.viewer.StatusManager.java
    switch (type) {
    case ECHO:
    case LOADSTRUCT:
    case MESSAGE:
    case PICK:
      return true;
    case ANIMFRAME:
    case APPLETREADY:
    case ATOMMOVED:
    case ERROR:
    case HOVER:
    case IMAGE:
    case MEASURE:
    case MINIMIZATION:
    case SERVICE:
    case RESIZE:
    case SYNC:
    case STRUCTUREMODIFIED:
    case SCRIPT:
    case CLICK:
    case DRAGDROP:
    case EVAL:
      break;
    }
    return false;
  }
  
  @Override
  @SuppressWarnings("incomplete-switch")
  public void notifyCallback(CBK type, Object[] data) {
    // this method as of 11.5.23 gets all the callback notifications for
    // any embedding application or for the applet.
    // see org.jmol.applet.Jmol.java and org.jmol.openscience.app.Jmol.java
    
    // data is an object set up by org.jmol.viewer.StatusManager
    // see that class for details.
    // data[0] is always blank -- for inserting htmlName
    // data[1] is either String (main message) or int[] (animFrameCallback only)
    // data[2] is optional supplemental information such as status info
    //         or sometimes an Integer value
    // data[3] is more optional supplemental information, either a String or Integer
    // etc. 
    
    switch (type) {
    case ECHO:
      sendConsoleEcho((String) data[1]);
      break;
    case LOADSTRUCT:
      String strInfo = (String) data[1];
      System.out.println(strInfo);
      monitor.setText(strInfo);
      break;
    case MESSAGE:
      sendConsoleMessage(data == null ? null : (String) data[1]);
      break;
    case PICK:
      //for example:
      notifyAtomPicked(((Integer) data[2]).intValue(), (String) data[1]);
      break;
    }
  }  

  /**
   * 
   * @param atomIndex
   * @param strInfo
   */
  private void notifyAtomPicked(int atomIndex, String strInfo) {
    monitor.setText(strInfo);
  }


  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#showUrl(java.lang.String)
   */
  @Override
  public void showUrl(String url) {
    System.out.println(url);
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#createImage(java.lang.String, java.lang.String, int)
   */
  /**
   * @param file 
   * @param type 
   * @param quality 
   * 
   */
  public void createImage(String file, String type, int quality) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#functionXY(java.lang.String, int, int)
   */
  @Override
  public float[][] functionXY(String functionName, int nx, int ny) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#functionXY(java.lang.String, int, int)
   */
  @Override
  public float[][][] functionXYZ(String functionName, int nx, int ny, int nz) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#sendConsoleEcho(java.lang.String)
   */
  /**
   * @param strEcho  
   */
  private void sendConsoleEcho(String strEcho) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#sendConsoleMessage(java.lang.String)
   */
  /**
   * @param strStatus  
   */
  private void sendConsoleMessage(String strStatus) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#setCallbackFunction(java.lang.String, java.lang.String)
   */
  @Override
  public void setCallbackFunction(String callbackType, String callbackFunction) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#eval(java.lang.String)
   */
  @Override
  public String eval(String strEval) {
    return null;
  }


  @Override
  public Map<String, Object> getRegistryInfo() {
    return null;
  }

  @Override
  public String createImage(String file, String type, Object text_or_bytes, int quality) {
    return null;
  }

  /**
   * @param type  
   * @param data 
   * @return response
   */
  public String dialogAsk(String type, String data) {
    return null;
  }

  @Override
  public int[] resizeInnerPanel(String data) {
    return null;
  }

  @Override
  public Map<String, Object> getJSpecViewProperty(String type) {
    // TODO
    return null;
  }
}
