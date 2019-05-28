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

import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.modelkit.ModelKitPopupResourceBundle;
import org.jmol.popup.JSSwingPopupHelper;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.PopupResource;
//import org.jmol.util.Elements;
import org.jmol.util.Elements;
import org.jmol.viewer.Viewer;

import javajs.awt.Component;
import javajs.awt.SC;

public class JSModelKitPopup extends JmolGenericPopup {

  private boolean hasUnitCell;

  public JSModelKitPopup() {
    helper = new JSSwingPopupHelper(this);
  }
  
  @Override
  public void jpiInitialize(PlatformViewer vwr, String menu) {
    updateMode = UPDATE_NEVER;
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = new ModelKitPopupResourceBundle(null, null);
    initialize((Viewer) vwr, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }

  @Override
  public void jpiUpdateComputedMenus() {
    hasUnitCell = vwr.getCurrentUnitCell() != null;
    SC menu = htMenus.get("XtalMenu");
    menu.setEnabled(hasUnitCell);
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
  
  /**
   * @j2sOverride
   */
  @Override
  public void menuClickCallback(SC source, String script) {
    if (script.equals("clearQ")) {
      for (SC item : htCheckbox.values()) {
        if (item.getActionCommand().indexOf(":??") < 0)
          continue;        
        menuSetLabel(item, "??");
        item.setActionCommand("_??P!:");
        item.setSelected(false);
      }
      vwr.evalStringQuiet("set picking assignAtom_C");
      return;
    }
    processClickCallback(source, script);
  }

  @Override
  public String menuSetCheckBoxOption(SC item, String name, String what, boolean TF) {
    String element = GT.$("Element?");
    /**
     * @j2sNative
     * 
     * element = prompt(element, "");
     * 
     */
    {}
    if (element == null || Elements.elementNumberFromSymbol(element, true) == 0)
      return null;
    updateButton(item, element, "assignAtom_" + element + "P!:??");
    return "set picking assignAtom_" + element;
  }

  @Override
  protected Object getImageIcon(String fileName) {
    return "org/jmol/modelkit/images/" + fileName;
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // TODO
    
  }
}
