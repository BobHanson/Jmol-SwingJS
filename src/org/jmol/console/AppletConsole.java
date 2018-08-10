/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-02-22 15:14:41 -0600 (Sun, 22 Feb 2015) $
 * $Revision: 20315 $
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

import org.jmol.api.JmolAbstractButton;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javajs.util.PT;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Keymap;
//import javax.swing.text.SimpleAttributeSet;
//import javax.swing.text.StyleConstants;

public class AppletConsole extends JmolConsole {

  //private final SimpleAttributeSet attributesCommand = new SimpleAttributeSet();

  public AppletConsole() {
    input = new ControlEnterTextArea();
    output = new GenericTextPane();
  }
  
  private class GenericTextPane extends JTextPane implements GenericTextArea {
    
    private final Document outputDocument;
    
    GenericTextPane() {
      super();
      outputDocument = getDocument();
    }

 
    @Override
    public void append(String message) {
      try {
        outputDocument.insertString(outputDocument.getLength(), message, null);
      } catch (BadLocationException e) {
        // ignore
      }
      setCaretPosition(outputDocument.getLength());
    }
  }
  

  @Override
  public void start(Viewer vwr) {
    setViewer(vwr);
    Component display = (Component) vwr.display;
    vwrFrame = (display instanceof JFrame ? (JFrame) display : null);
    JFrame jf = new JFrame();
    jf.setSize(600, 400);
    externalContainer = jf;
    setLabels();
    JTextArea ta = (JTextArea) input;
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setDragEnabled(true);
    Keymap map = ta.getKeymap();
    //    KeyStroke shiftCR = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
    //                                               InputEvent.SHIFT_MASK);
    KeyStroke shiftA = KeyStroke.getKeyStroke(KeyEvent.VK_A,
        InputEvent.SHIFT_MASK);
    map.removeKeyStrokeBinding(shiftA);
    ((JTextPane) output).setEditable(false);
    ((JTextPane) output).setDragEnabled(true);
    //    output.setLineWrap(true);
    //    output.setWrapStyleWord(true);
    //StyleConstants.setBold(attributesCommand, true);
    addWindowListener();
    displayConsole();
  }

  @Override
  protected JmolAbstractButton getLabel1() {
    return  new JmolLabel(getLabel("label1"), SwingConstants.CENTER);
  }
  
  //public void finalize() {
  //  System.out.println("Console " + this + " finalize");
  //}

  
  @Override
  protected void layoutWindow(String enabledButtons) {
    //Logger.debug("Console constructor");
    JScrollPane jscrollInput = new JScrollPane((JTextArea)input);
    jscrollInput.setMinimumSize(new Dimension(2, 100));

    JScrollPane jscrollOutput = new JScrollPane((JTextPane) output);
    jscrollOutput.setMinimumSize(new Dimension(2, 100));
    Container c = getPane();
    c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
    JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jscrollOutput,
        jscrollInput);
    jsp.setResizeWeight(.9);
    jsp.setDividerLocation(200);

    jsp.setAlignmentX(Component.CENTER_ALIGNMENT);
    c.add(jsp);

    Container c2 = new Container();
    c2.setLayout(new BoxLayout(c2, BoxLayout.X_AXIS));
    c2.add(Box.createGlue());
    add(c2, editButton);
    add(c2, runButton);
    add(c2, loadButton);
    add(c2, clearInButton);
    add(c2, clearOutButton);
    add(c2, historyButton);
    add(c2, stateButton);
    c2.add(Box.createGlue());
    c.add(c2);
    if (label1 != null) {
      ((JLabel) label1).setAlignmentX(Component.CENTER_ALIGNMENT);
      c.add((JLabel) label1);
    }
    if (externalContainer instanceof JFrame)
      ((JFrame) externalContainer).setJMenuBar(createMenubar());
  }

  private void add(Container c2, JmolAbstractButton b) {
    c2.add((JButton) b);
  }

  private JMenuBar createMenubar() {
    JMenuBar mb = new JMenuBar();
    //addNormalMenuBar(mb);
    mb.add(Box.createHorizontalGlue());
    addHelpMenuBar(mb);
    return mb;
  }
  
  private void addHelpMenuBar(JMenuBar menuBar) {
    JMenu m0 = new KeyJMenu("help", getLabel("help"), menuMap);
    JMenuItem item = createMenuItem("search");
    item.addActionListener(this);
    item.setName("help ?search=?");
    m0.add(item);
    addHelpItems(m0, "commands", "command");
    addHelpItems(m0, "functions", "mathfunc");
    addHelpItems(m0, "parameters", "setparam");
    addHelpItems(m0, "more", "misc");
    menuBar.add(m0);
  }

  private void addHelpItems(JMenu m0, String key, String attr) {
    JMenu m = new KeyJMenu(key, getLabel(key), menuMap);
    String[] commands = T.getTokensLike(attr);
    m0.add(m);
    JMenu m2 = null;
    String firstCommand = null;
    int n = 20;
    for (int i = 0; i < commands.length; i++) {
      String cmd = commands[i];
      if (!PT.isLetter(cmd.charAt(0)))
        continue;
      JMenuItem item = new JMenuItem(cmd);
      item.addActionListener(this);
      item.setName("help " + cmd);
      if (m2 == null) {
        m2 = new JMenu();
        firstCommand = cmd;
        m2.add(item);
        m2.setText(firstCommand);
        continue;
      }
      if ((i % n) + 1 == n) {
        m2.add(item);
        m2.setText(firstCommand + " - " + cmd);
        m.add(m2);
        m2 = null;
        continue;
      }
      m2.add(item);
      if (i + 1 == commands.length) {
        m2.setText(firstCommand + " - " + cmd);
        m.add(m2);
      }
    }
  }

  private JMenuItem createMenuItem(String cmd) {
    return new KeyJMenuItem(cmd, getLabel(cmd), menuMap);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    ((JTextArea) input).requestFocus();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    doAction(e.getSource());
  }

  @Override
  protected void execute(String strCommand) {
    super.execute(strCommand);
    if (strCommand == null)
      ((JTextArea) input).requestFocus();
  }

  class ControlEnterTextArea extends JTextArea implements GenericTextArea {
    @SuppressWarnings("deprecation")
    @Override
    public void processComponentKeyEvent(KeyEvent ke) {
      int kcode = ke.getKeyCode();
      int kid = ke.getID();
      boolean isControlDown = ke.isControlDown();
      int mode = processKey(kcode, kid, isControlDown);
      if ((mode & 1) == 1)
        ke.consume();
      if ((mode & 2) == 2) {
        if (kcode == KeyEvent.VK_ENTER)
          ke.setModifiers(0);
        super.processComponentKeyEvent(ke);
      }
    }
  }
}
