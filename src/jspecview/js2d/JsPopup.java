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
package jspecview.js2d;

import org.jmol.api.PlatformViewer;
import org.jmol.api.SC;
import org.jmol.popup.PopupResource;

import org.jmol.awtjs.swing.Component;
import org.jmol.awtjs.swing.JPopupMenu;
import org.jmol.awtjs2d.JSPopupHelper;

import jspecview.common.JSViewer;
import jspecview.popup.JSVGenericPopup;
import jspecview.popup.JSVPopupResourceBundle;

public class JsPopup extends JSVGenericPopup {

  //  (on mouse up)       checkMenuClick(e.getSource(), e.getSource().getActionCommand());
  //  (on entry)          checkMenuFocus(item.getName(), item.getActionCommand(), true);
  //  (on exit)           checkMenuFocus(item.getName(), item.getActionCommand(), false);
  //  (on checkbox click) checkBoxStateChanged(e.getSource());   

  public JsPopup() {
  	helper = new JSPopupHelper(this);
    // required by reflection
  }

	@Override
	public void jpiInitialize(PlatformViewer viewer, String menu) {
    PopupResource bundle = new JSVPopupResourceBundle();
    initialize((JSViewer) viewer, bundle, menu);
	}

  /**
   * could be frank menu or regular context menu
   * 
   */
  @Override
  public void menuShowPopup(SC popup, int x, int y) {
    try {
      ((JPopupMenu) popup).show(isTainted ? (Component) vwr.getApplet() : null, x, y);
    } catch (Exception e) {
      // ignore
    }
  }
  
	@Override
	protected Object getImageIcon(String fileName) {
		// not used in JSV
		return null;
	}

	@Override
	public void menuFocusCallback(String name, String actionCommand, boolean b) {
		// not used in JSV?
		
	}


}
