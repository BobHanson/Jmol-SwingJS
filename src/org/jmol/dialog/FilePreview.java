/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-09-13 21:23:44 -0500 (Sat, 13 Sep 2008) $
 * $Revision: 9891 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.dialog;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;

import javajs.util.PT;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

/**
 * File previsualisation before opening
 */
public class FilePreview extends JPanel implements PropertyChangeListener {

  JCheckBox active, append, cartoons;
  JFileChooser chooser;
  private static boolean pdbCartoonChecked = true;
  private FPPanel display;
  Viewer vwr;

  /**
   * Constructor
   * 
   * @param vwr
   * @param fileChooser
   *        File chooser
   * @param allowAppend
   * @param vwrOptions
   */
  public FilePreview(Viewer vwr, JFileChooser fileChooser, 
      boolean allowAppend, Map<String, Object> vwrOptions) {
    super();
    this.vwr = vwr;
    chooser = fileChooser;

    // Create a box to do the layout
    Box box = Box.createVerticalBox();

    // Add a checkbox to activate / deactivate preview
    active = new JCheckBox(GT.$("Preview"), false);
    active.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doPreviewAction(active.isSelected());
      }
    });
    box.add(active);

    // Add a preview area
    display = new FPPanel(vwrOptions);
    display.setPreferredSize(new Dimension(80, 80));
    display.setMinimumSize(new Dimension(50, 50));
    box.add(display);

    if (allowAppend) {
      // Add a checkbox to append data
      append = new JCheckBox(GT.$("Append models"), false);
      box.add(append);
      cartoons = new JCheckBox(GT.$("PDB cartoons"), pdbCartoonChecked);
      cartoons.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (active.isSelected()) {
            doUpdatePreview(chooser.getSelectedFile());
          }
        }
      });
      box.add(cartoons);

    }

    // Add the preview to the File Chooser
    add(box);
    fileChooser.setAccessory(this);
    fileChooser.addPropertyChangeListener(this);
  }

  protected void doPreviewAction(boolean selected) {
    doUpdatePreview(selected ? chooser.getSelectedFile() : null);
  }

  /**
   * @return Indicates if Append is selected.
   */
  public boolean isAppendSelected() {
    return (append != null && append.isSelected());
  }

  /**
   * @return Indicates if Cartoons is selected.
   */
  public boolean isCartoonsSelected() {
    return pdbCartoonChecked = (cartoons != null && cartoons.isSelected());
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (active.isSelected()) {
      String prop = evt.getPropertyName();
      if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
        doUpdatePreview((File) evt.getNewValue());
      }
    }
  }

  /**
   * Update preview
   * 
   * @param file
   *        File selected
   */
  void doUpdatePreview(File file) {
    String script;
    if (file == null) {
      script = "zap";
    } else {
      String fileName = file.getAbsolutePath();
      //System.out.println("updatePreview "+ fileName + " " + chooser.getSelectedFile());
      String url = vwr.getLocalUrl(fileName);
      //System.out.println("updatePreview + " + fileName + " " + url);
      if (url != null)
        fileName = url;
      //doesn't update from use input?
      fileName = fileName.replace('\\', '/');
      script = " \"" + fileName + "\"";
      if (fileName.indexOf(".spt") >= 0) {
        script = "script " + script;
      } else {
        script = PT.rep((String) display.vwr
            .getP("defaultdropscript"), "\"%FILE\"", script + " 1");
        script = PT.rep(script, "%ALLOWCARTOONS", ""
            + (isCartoonsSelected() && !isAppendSelected()));
        System.out.println(script);
      }
    }
    display.vwr.evalStringQuiet(script);
    //display.repaint();
  }

  private static class FPPanel extends JPanel {
    Viewer vwr;

    FPPanel(Map<String, Object> info) {
      info.put("previewOnly", Boolean.TRUE);
      Object display = info.get("display");
      info.put("display", this);
      vwr = new Viewer(info);
      info.put("display", display);
    }

    final Dimension currentSize = new Dimension();

    @Override
    public void paint(Graphics g) {
      getSize(currentSize);
      vwr.setScreenDimension(currentSize.width, currentSize.height);
      Rectangle rectClip = new Rectangle();
      g.getClipBounds(rectClip);
      vwr.renderScreenImage(g, currentSize.width, currentSize.height);
    }
  }

  public void setPreviewOptions(boolean TF) {
    if (append == null)
      return;
    append.setVisible(TF);
    cartoons.setVisible(TF);
  }
}
