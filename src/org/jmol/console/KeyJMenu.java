/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-25 02:42:30 -0500 (Thu, 25 Jun 2009) $
 * $Revision: 11113 $
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
package org.jmol.console;


import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jmol.api.JmolAbstractButton;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

public class KeyJMenu extends JMenu implements JmolAbstractButton {

  private String key;
  
  @Override
  public String getKey() {
    return key;
  }
  
  public String[] itemKeys;

  public KeyJMenu(String key, String label, Map<String, Object> menuMap) {
    super(GenericConsole.getLabelWithoutMnemonic(label));
    GenericConsole.map(this, this.key = key, label, menuMap);
  }
  
  @Override
  public void addConsoleListener(Object console) {
  }

  public void createItemKeys(JmolPanel jmolPanel) {
    if (itemKeys == null)
      return;
    //System.out.println("late binding for " + getActionCommand());
    // Loop over the items in this menu:
    for (int i = 0; i < itemKeys.length; i++) {
      String item = itemKeys[i];
      if (item.equals("-")) {
        addSeparator();
      } else if (item.endsWith("Menu")) {
        add(jmolPanel.createMenu(item));
      } else {
        JMenuItem mi = jmolPanel.createMenuItem(item);
        add(mi);
      }
    }
    itemKeys = null;
  }


}

