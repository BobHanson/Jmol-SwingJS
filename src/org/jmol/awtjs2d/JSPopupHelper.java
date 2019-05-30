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


import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;

import org.jmol.awtjs.swing.ButtonGroup;
import org.jmol.awtjs.swing.JCheckBoxMenuItem;
import org.jmol.awtjs.swing.JMenu;
import org.jmol.awtjs.swing.JMenuItem;
import org.jmol.awtjs.swing.JPopupMenu;
import org.jmol.awtjs.swing.JRadioButtonMenuItem;

import org.jmol.awtjs.swing.Component;
import org.jmol.awtjs.swing.SC;
import org.jmol.popup.GenericPopup;
import org.jmol.popup.PopupHelper;

/**
 * For menus, popup-related awt/swing class references are in this file.
 * We can ignore all the event/listener references because JSmol will create a
 * simple object with just the elements getSource and getActionCommand.
 * No need to have the entire classes fleshed out. 
 * 
 * 
 */
  public class JSPopupHelper implements PopupHelper {

  //  (on mouse up)       checkMenuClick(e.getSource(), e.getSource().getActionCommand());
  //  (on entry)          checkMenuFocus(item.getName(), item.getActionCommand(), true);
  //  (on exit)           checkMenuFocus(item.getName(), item.getActionCommand(), false);
  //  (on checkbox click) checkBoxStateChanged(e.getSource());   

  /**
   * used here and by SwingController to refer to the Java 
   * class being handled by this helper.
   */
  GenericPopup popup;
  
  private ButtonGroup buttonGroup;

  public JSPopupHelper(GenericPopup popup) {
    this.popup = popup;
  }

  @Override
  public SC menuCreatePopup(String name, Object applet) {
    JPopupMenu j = new JPopupMenu(name);
    j.setInvoker(applet);
    return j;
  }

  @Override
  public SC getMenu(String name) {
    JMenu jm = new JMenu();
    jm.setName(name);
    return jm;
  }

  @Override
  public SC getMenuItem(String text) {
    return new JMenuItem(text);
  }

  @Override
  public SC getRadio(String name) {
    return new JRadioButtonMenuItem();
  }

  @Override
  public SC getCheckBox(String name) {
    return new JCheckBoxMenuItem();
  }

  @Override
  public void menuAddButtonGroup(SC item) {
    if (item == null) {
      buttonGroup = null;
      return;
    }
    if (buttonGroup == null)
      buttonGroup = new ButtonGroup();
    buttonGroup.add(item);
  }

  
  @Override
  public int getItemType(SC m) {
    return ((JMenuItem) m).btnType;
  }

  @Override
  public void menuInsertSubMenu(SC menu, SC subMenu,
                                int index) {
    ((Component)subMenu).setParent(menu);
  }

  @Override
  public SC getSwingComponent(Object component) {
    return (SC) component;
  }

  @Override
  public void menuClearListeners(SC menu) {
    if (menu != null)
      ((JPopupMenu) menu).disposeMenu();
  }

  public void itemStateChanged(ItemEvent e) {
    popup.menuCheckBoxCallback((SC)e.getSource());
  }

  public void actionPerformed(ActionEvent e) {
    popup.menuClickCallback((SC) e.getSource(), e.getActionCommand());
  }

  @Override
  public Object getButtonGroup() {
    return buttonGroup;
  }

  public void handleEvent(MouseEvent e) {
    String type = "" + e.getID(); 
    if (type == "mouseenter")
      mouseEntered(e);
    else if (type == "mouseleave")
      mouseExited(e);
  }
  
  public void mouseEntered(MouseEvent e) {
    SC jmi = (SC) e.getSource();
    popup.menuFocusCallback(jmi.getName(), jmi.getActionCommand(), true);
  }

  public void mouseExited(MouseEvent e) {
    SC jmi = (SC) e.getSource();
    popup.menuFocusCallback(jmi.getName(), jmi.getActionCommand(), false);
  }

  
}
