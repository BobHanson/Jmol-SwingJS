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


import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.awt.Platform;
import org.jmol.i18n.GT;
import org.jmol.viewer.FileManager;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FilenameFilter;

import javajs.util.AU;
import javajs.util.Lst;


import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;

public abstract class JmolConsole extends GenericConsole implements ActionListener, WindowListener {

  protected JFrame vwrFrame;
  protected Container externalContainer;

  @Override
  public void dispose() {
    if (externalContainer instanceof Window)
      ((Window) externalContainer).dispose();
    else
      externalContainer.setVisible(false);
  }

  protected Container getPane() {
    return (externalContainer instanceof JFrame ? ((JFrame) externalContainer)
        .getContentPane() : externalContainer);
  }

  @Override
  protected void setTitle() {
    if (externalContainer instanceof JFrame)
      ((JFrame) this.externalContainer).setTitle(getLabel("title"));
    else if (externalContainer instanceof JDialog)
      ((JDialog) externalContainer).setTitle(getLabel("title"));
  }

  @Override
  public void setVisible(boolean isVisible) {
    externalContainer.setVisible(isVisible);
  }

  @Override
  protected JmolAbstractButton setButton(String label) {
    return addButton(new JmolButton(getLabel(label)), label);
  }

  @Override
  protected void updateLabels() {
    labels = null;
    boolean doTranslate = GT.setDoTranslate(true);
    setViewer(vwr);
    defaultMessage = getLabel("default");
    setAbstractButtonLabels(menuMap, labels);
    setTitle();
    if (label1 != null)
      label1.setText(getLabel("label1"));
    GT.setDoTranslate(doTranslate);
  }
  
  protected ScriptEditor scriptEditor;

  @Override
  public JmolScriptEditorInterface getScriptEditor() {
    // is called by vwr during application startup, despite what Eclipse says.
    return (scriptEditor == null ? 
        (scriptEditor = new ScriptEditor(vwr, vwrFrame, this)) : scriptEditor);
  }
  
  //public void finalize() {
  //System.out.println("Console " + this + " finalize");
  //}
  
  @Override
  protected String nextFileName(String stub, int nTab) {
    String sname = FileManager.getLocalPathForWritingFile(vwr, stub, false);
    if (sname == null)
      return null;
    String root = sname.substring(0, sname.lastIndexOf("/") + 1);
    if (sname.startsWith("file:/"))
      sname = sname.substring(6);
    if (sname.indexOf("/") >= 0) {
      if (root.equals(sname)) {
        stub = "";
      } else {
        File dir = new File(sname);
        sname = dir.getParent();
        stub = dir.getName();
      }
    }
    FileChecker fileChecker = new FileChecker(stub);
    try {
      (new File(sname)).list(fileChecker);
      return root + fileChecker.getFile(nTab);
    } catch (Exception e) {
      //
    }
    return null;
  }

  private class FileChecker implements FilenameFilter {
    private String stub;
    private Lst<String> v = new  Lst<String>();
    
    protected FileChecker(String stub) {
      this.stub = stub.toLowerCase();
    }

    @Override
    public boolean accept(File dir, String name) {
      if (!name.toLowerCase().startsWith(stub))
        return false;
      v.addLast(name); 
      return true;
    }
    
    protected String getFile(int n) {
      return AU.sortedItem(v, n);
    }
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    doAction(e.getSource());
  }

  @Override
  protected boolean isMenuItem(Object source) {
    return source instanceof JMenuItem;
  }

  ////////////////////////////////////////////////////////////////
  // window listener stuff to close when the window closes
  ////////////////////////////////////////////////////////////////

  protected void addWindowListener() {
    Window w = Platform.getWindow(externalContainer);
    if (w != null)
      w.addWindowListener(this);
  }


  /**
   * @param we 
   * 
   */
  @Override
  public void windowActivated(WindowEvent we) {
    updateFontSize();
  }

  @Override
  public void windowClosed(WindowEvent we) {
    destroyConsole();
  }

  @Override
  public void windowClosing(WindowEvent we) {
    destroyConsole();
  }

  /**
   * @param we 
   * 
   */
  @Override
  public void windowDeactivated(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  @Override
  public void windowDeiconified(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  @Override
  public void windowIconified(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  @Override
  public void windowOpened(WindowEvent we) {
  }


  @Override
  public Object newJMenu(String key) {
    return new KeyJMenu(key, getLabel(key), null);
  }

  @Override
  public Object newJMenuItem(String key) {
    return new KeyJMenuItem(key, getLabel(key), null);
  }

  public void updateFontSize() {
    // TODO
    
  }



}
