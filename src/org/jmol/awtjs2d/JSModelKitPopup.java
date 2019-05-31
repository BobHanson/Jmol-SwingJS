/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
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
import org.jmol.modelkit.ModelKitPopup;
import org.jmol.util.Elements;
import org.jmol.awtjs.swing.Component;
import org.jmol.awtjs.swing.SC;

public class JSModelKitPopup extends ModelKitPopup {

  public JSModelKitPopup() {
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
  protected Object getImageIcon(String fileName) {
    return "org/jmol/modelkit/images/" + fileName;
  }

  
  @Override
  public void menuClickCallback(SC source, String script) {
    /** @j2sNatve */{}
    // Hack for old transpiler, which can skip an empty call to super
    // but then if THAT method also calls super, we get the wrong object ref.
    super.menuClickCallback(source, script);
  }

  @Override
  public void menuCheckBoxCallback(SC source) {
    /** @j2sNatve */{}
    // Hack for old transpiler
    super.menuCheckBoxCallback(source);
  }

}
