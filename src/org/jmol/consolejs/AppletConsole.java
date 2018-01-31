/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-07-21 10:12:08 -0500 (Sat, 21 Jul 2012) $
 * $Revision: 17376 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, www.jmol.org
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
package org.jmol.consolejs;

import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.console.GenericConsole;
import org.jmol.viewer.Viewer;


/**
 * 
 * An interface to Jmol.Console. 
 * 
 *   keyboard events are returned to 
 *   
 *     GenericConsole.processKey(kcode, kid, isControlDown);
 *   
 *   button events are returned to 
 *   
 *     GenericConsole.doAction(source);
 *   
 *   12/24/2012
 *   
 * @author Bob Hanson hansonr@stolaf.edu
 *   
 */
public class AppletConsole extends GenericConsole {

  public AppletConsole() {
  }

  JSConsole jsConsole;

  @Override
  public void start(Viewer vwr) {
    setViewer(vwr);
    setLabels();
    displayConsole(); // will call layoutWindow
  }

  @Override
  protected void layoutWindow(String enabledButtons) {
    /**
     * Implement the window now in HTML.
     * Also set up this.input and this.output.
     * Console can stay hidden at this point.
     * 
     * @j2sNative
     * 
     *            this.jsConsole = new Jmol.Console.JSConsole(this); 
     */
    {  
    }
    setTitle();
  }

  @Override
  protected void setTitle() {
    if (jsConsole != null)
      jsConsole.setTitle(getLabel("title"));
  }

  @Override
  public void setVisible(boolean visible) {
    jsConsole.setVisible(visible);
  }

  @Override
  protected JmolAbstractButton setButton(String text) {
    /**
     * @j2sNative
     * 
     *            return new Jmol.Console.Button(text);
     */
    {
      return null;
    }
  }

  @Override
  public void dispose() {
    setVisible(false);
  }

  @Override
  protected boolean isMenuItem(Object source) {
    //ignore -- no console menu in JSmol (yet)
    return false;
  }

  @Override
  public JmolScriptEditorInterface getScriptEditor() {
    //ignore -- no Script Editor in JSmol
    return null;
  }

  @Override
  protected String nextFileName(String stub, int nTab) {
    //ignore
    return null;
  }

  @Override
  public Object newJMenu(String key) {
    return null;
  }

  @Override
  public Object newJMenuItem(String key) {
    return null;
  }


}
