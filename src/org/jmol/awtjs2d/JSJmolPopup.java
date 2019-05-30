/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-10-14 11:28:38 -0500 (Fri, 14 Oct 2011) $
 * $Revision: 16354 $
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
package org.jmol.awtjs2d;

import org.jmol.awtjs.swing.JPopupMenu;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.JmolPopup;
import org.jmol.awtjs.swing.Component;
import org.jmol.awtjs.swing.SC;

public class JSJmolPopup extends JmolPopup {
  

  /**
   * The main popup window for the applet -- as JavaScript
   * 
   */
  public JSJmolPopup() {
    helper = new JSPopupHelper(this);
  }

  @Override
  public void menuShowPopup(SC popup, int x, int y) {
    try {
      ((JPopupMenu) popup).show(isTainted ? (Component) vwr.html5Applet : null, x, y);
    } catch (Exception e) {
      // ignore
    }
    isTainted = false;
  }

  @Override
  protected String getUnknownCheckBoxScriptToRun(SC item, String name,
                                         String what, boolean TF) {
    // ModelKit popup only
    return null;
  }
  
  @Override
  protected Object getImageIcon(String fileName) {
    // ModelKit menu only
    return null;
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // TODO
  }




}
