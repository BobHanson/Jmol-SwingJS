/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jmol.i18n.GT;

class LogPanel {

  private static JTextArea logArea;
  private static JTextArea miniLogArea;
  private static boolean resetFlag;

  static JPanel getPanel() {
    //Now layout the LogPanel.  It will be added to the tabs in the main class.

    //Create the log first, because the action listeners
    //need to refer to it.
    logArea = new JTextArea(30,20);
    logArea.setMargin(new Insets(5, 5, 5, 5));
    logArea.setEditable(false);
    JScrollPane logScrollPane = new JScrollPane(logArea);

    //Create a panel of the log and its label
    JPanel logPanel = new JPanel();
    logPanel.setLayout(new BorderLayout());
    logPanel.setBorder(BorderFactory.createTitledBorder(GT._("Log and Error Messages:")));
    logPanel.add(logScrollPane);
    return logPanel;
  }

  static JPanel getMiniPanel() {
    JPanel miniPanel = new JPanel();
    miniPanel.setLayout(new BorderLayout());
    miniPanel.setBorder(BorderFactory.createTitledBorder(GT._("Messages (see Log tab for full history):")));
    miniLogArea = new JTextArea(2,20);
    miniLogArea.setEditable(false);
    JScrollPane miniScrollPane = new JScrollPane(miniLogArea);
    miniPanel.add(miniScrollPane);
    return miniPanel;
  }

  static void log(String message) {
    if (resetFlag){
      logArea.setText("");
      miniLogArea.setText("");
    }
    resetFlag = (message.length() == 0);
    logArea.append(message + "\n");
    miniLogArea.append(message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
    miniLogArea.setCaretPosition(miniLogArea.getDocument().getLength());
  }

  static String getText() {
    return logArea.getText();
  }  
}
