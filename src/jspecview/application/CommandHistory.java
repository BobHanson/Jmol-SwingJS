/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.application;

import java.awt.event.KeyEvent;


import javajs.util.Lst;

import javax.swing.JTextField;

import jspecview.common.JSViewer;

/**
 * CommandHistory keeps a command history and responds to key events
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class CommandHistory {

  private JSViewer vwr;
  private JTextField input;
  private Lst<String> cmdList = new Lst<String>();
  private int cmdPt = -1;
  private int cmdOffset = 0;

  public CommandHistory(JSViewer viewer, JTextField commandInput) {
    this.vwr = viewer;
    input = commandInput;
    // TODO Auto-generated constructor stub
  }
  
  public void keyPressed(int keyCode) {
    switch (keyCode) {
    case KeyEvent.VK_ESCAPE:
      input.setText("");
      return;
    case KeyEvent.VK_ENTER:
      String cmd = input.getText();
      addCommand(cmd);
      vwr.runScript(cmd);
      input.setText("");
      input.requestFocusInWindow(); 
      return;
    case KeyEvent.VK_UP:
    case KeyEvent.VK_DOWN:
      String s = recallCommand(keyCode == KeyEvent.VK_UP);
      if (s != null)
        input.setText(s);
      break;
    }
  }
  
  /**
   * 
   * @param isPrevious
   * @return command
   */
  private synchronized String recallCommand(boolean isPrevious) {
    cmdOffset = 0;
    if (isPrevious) {
      if (cmdPt < 0 || cmdPt == cmdList.size())
        return "";
      cmdOffset = -1;
    } else {
      if (cmdPt <= 0)
        return "";      
      if (--cmdPt < 0)
        cmdPt = 0;
    }
    String cmd = cmdList.get(isPrevious ? cmdPt++ : cmdPt);
    //dumpList();
    return cmd;
  }

  private void addCommand(String cmd) {
    cmdPt += cmdOffset;
    cmdOffset = 0;
    if (cmdPt < 0)
      cmdPt = 0;
    if (cmdPt < cmdList.size() && cmdList.get(cmdPt).equals(cmd))
        return;
    cmdList.add(cmdPt, cmd);
    //dumpList();
  }

  void dumpList() {
    for (int i = 0; i < cmdList.size(); i++)
      System.out.println((i == cmdPt ? ">" : "") + i + "\t" + cmdList.get(i));
    System.out.println ("");
  }

  
  
}
