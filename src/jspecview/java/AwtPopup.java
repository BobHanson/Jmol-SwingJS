/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package jspecview.java;

import java.awt.Container;

import javax.swing.JPopupMenu;

import org.jmol.popup.AwtSwingPopupHelper;
import org.jmol.popup.PopupResource;

import javajs.awt.SC;

import org.jmol.api.PlatformViewer;
import org.jmol.popup.AwtSwingComponent;

import jspecview.common.JSViewer;
import jspecview.popup.JSVPopupResourceBundle;
import jspecview.popup.JSVGenericPopup;

public class AwtPopup extends JSVGenericPopup  {

	public AwtPopup() {
		helper = new AwtSwingPopupHelper(this);		
	}
	
	// @Override
	// public void finalize() {
	// System.out.println("SwingPopup Finalize " + this);
	// }

	@Override
	public void jpiInitialize(PlatformViewer viewer, String menu) {
    PopupResource bundle = new JSVPopupResourceBundle();
    initialize((JSViewer) viewer, bundle, menu);
	}

  @Override
  public void menuFocusCallback(String name, String cmd, boolean isFocus) {
    // no focus callback here
  }

	@Override
	public void menuShowPopup(SC popup, int x, int y) {
		try {
			((JPopupMenu) ((AwtSwingComponent) popup).jc).show((Container) thisJsvp, x, y);
		} catch (Exception e) {
			System.out.println(e);
		}
	}
  
  @Override
  protected String menuSetCheckBoxOption(SC item, String name, String what, boolean TF) {
    // ModelKit only
    return null;
  }


	public void checkMenuFocus(String name, String cmd, boolean isFocus) {
		if (name.indexOf("Focus") < 0)
			return;
		if (isFocus)
			vwr.runScript(cmd);
	}


	@Override
	protected Object getImageIcon(String fileName) {
		// not used in JSV
		return null;
	}


}