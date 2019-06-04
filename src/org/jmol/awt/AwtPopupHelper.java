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
package org.jmol.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

import org.jmol.api.SC;
import org.jmol.popup.GenericPopup;
import org.jmol.popup.PopupHelper;

/**
 * all popup-related awt/swing class references are in this file.
 */
public class AwtPopupHelper implements
    ActionListener, ItemListener, MouseListener, PopupHelper {

  private GenericPopup popup;

  public AwtPopupHelper(GenericPopup popup) {
    this.popup = popup;
  }
  //  @Override
  //  public void finalize() {
  //    System.out.println("SwingPopup Finalize " + this);
  //  }

  private Map<Object, SC> htSources = new Hashtable<Object, SC>();
  private ButtonGroup buttonGroup; 

  @Override
  public SC menuCreatePopup(String title, Object applet) {
    return AwtSwingComponent.getPopup(title, htSources);
  }

  @Override
  public SC getRadio(String name) {
    return AwtSwingComponent.getRadio(this, name, htSources);
  }

  @Override
  public SC getCheckBox(String name) {
    return AwtSwingComponent.getCheckBox(this, name, htSources);
  }

  @Override
  public SC getMenu(String name) {
    return AwtSwingComponent.getMenu(name, htSources);
  }

  @Override
  public SC getMenuItem(String name) {
    return AwtSwingComponent.getMenuItem(this, name, htSources);
  }

  @Override
  public void menuAddButtonGroup(SC item) {
    if (item == null) {
      buttonGroup = null;
      return;
    }
    if (buttonGroup == null)
      buttonGroup = new ButtonGroup();
    buttonGroup.add(((AwtSwingComponent) item).ab);
  }

  @Override
  public Object getButtonGroup() {
    return buttonGroup;
  }
  
  @Override
  public void menuInsertSubMenu(SC menu, SC subMenu,
                                int index) {
    menu.insert(subMenu, index);
  }

  @Override
  public int getItemType(SC m) {
    if (m == null)
      return 0;
    JComponent jc = ((AwtSwingComponent) m).jc;
    return (jc instanceof JMenu ? 4 
        : jc instanceof JRadioButtonMenuItem ? 3
        : jc instanceof JCheckBoxMenuItem ? 2
        : m.getText() != null ? 1 : 0);
  }
  
  @Override
  public SC getSwingComponent(Object component) {
    return (component == null ? null : htSources.get(component));
  }

  /// Listener methods

  @Override
  public void menuClearListeners(SC c) {
    if (c == null)
      return;
    clearListeners(c.getComponents());
    clearListener(((AwtSwingComponent)c).ab);
  }

  private void clearListener(AbstractButton ab) {
    if (ab != null)
      try {
        ab.removeMouseListener(this);
        ab.removeActionListener(this);
        ab.removeItemListener(this);
      } catch (Exception e) {
        // ignore
      }
  }

  private void clearListeners(Object[] subMenus) {
    for (int i = 0; i < subMenus.length; i++) {
      JComponent m = (JComponent) subMenus[i];
      if (m instanceof JMenu)
        clearListeners(m.getComponents());
    }
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    popup.menuCheckBoxCallback(getSource(e));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    popup.menuClickCallback(getSource(e), e.getActionCommand());
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    if (e.getSource() instanceof JMenuItem) {
      JMenuItem jmi = (JMenuItem) e.getSource();
      popup.menuFocusCallback(jmi.getName(), jmi.getActionCommand(), true);
    }
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if (e.getSource() instanceof JMenuItem) {
      JMenuItem jmi = (JMenuItem) e.getSource();
      popup.menuFocusCallback(jmi.getName(), jmi.getActionCommand(), false);
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  /**
   * returns the SwingComponent wrapper for this item
   * 
   * @param e
   * @return wrapped component
   */
  private SC getSource(EventObject e) {
    return getSwingComponent(e.getSource());
  }

  /**
   * Cause the menu to persist in its open state. Path is set in the setArmed()
   * method of the item, and it is checked in the doClick() method.
   * 
   * Persist only if (a) somewhere in the path of names there is "Persist", and
   * nowhere in that path is "!Persist".
   * 
   * @param item
   * @param path
   */
  public void reinstateMenu(JMenuItem item, MenuElement[] path) {
    String name = "" + item.getName();
    if (name.indexOf("Persist") >= 0 && name.indexOf("!Persist") < 0) {
      popup.jpiShow(popup.thisx, popup.thisy);
      MenuSelectionManager.defaultManager().setSelectedPath(path);
    }
  }

}
