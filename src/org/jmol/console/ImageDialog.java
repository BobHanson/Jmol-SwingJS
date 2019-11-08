/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-03-20 17:22:16 -0500 (Thu, 20 Mar 2014) $
 * $Revision: 19476 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.console;


import javajs.util.PT;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.GenericImageDialog;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.awt.Platform;
import org.jmol.viewer.Viewer;

public class ImageDialog extends JDialog implements GenericImageDialog, WindowListener, ActionListener {


  private JMenuBar menubar;

  protected Image image;

  protected Viewer vwr;
  protected ImageCanvas canvas;
  private String title;

  private Map<String, GenericImageDialog> imageMap;

  private JmolAppConsoleInterface console;

  public ImageDialog(Viewer vwr, String title, Map<String, GenericImageDialog> imageMap){
    super(Platform.getWindow((Container) vwr.display) instanceof JFrame ? (JFrame) Platform.getWindow((Container) vwr.display) : null, title, false);
    this.vwr = vwr;
    this.setResizable(false);
    console = vwr.getConsole();
    addWindowListener(this);
    this.title = title;
    this.imageMap = imageMap;
    imageMap.put(title, this);
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBackground(new Color(255,0,0));
    canvas = new ImageCanvas();
    wrapper.add(canvas, BorderLayout.CENTER);
    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());
    menubar = new JMenuBar();
    // see app.jmolpanel.jmol.Properties.Jmol-reseources.properties
    menubar.add(createMenu());
    setJMenuBar(menubar);
    container.add(wrapper, BorderLayout.CENTER);
    getContentPane().add(container);
    pack();
    setLocation(100, 100);
    setVisible(true);
  }

  private JMenu createMenu() {

    // Get list of items from resource file:
    String[] itemKeys = PT.getTokens("saveas close");
    // Get label associated with this menu:
    vwr.getConsole();
    JMenu menu = (JMenu) console.newJMenu("file");
    // Loop over the items in this menu:
    for (int i = 0; i < itemKeys.length; i++) {
      String item = itemKeys[i];
        JMenuItem mi = createMenuItem(item);
        menu.add(mi);
    }
    menu.setVisible(true);
    return menu;
  }

  private JMenuItem createMenuItem(String cmd) {
    JMenuItem mi = (JMenuItem) console.newJMenuItem(cmd);
    mi.setActionCommand(cmd);
    mi.addActionListener(this);
    mi.setVisible(true);
    return mi;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("close")) {
      closeMe();
    } else if (cmd.equals("saveas")) {
      saveAs();
    }
  }

  private void saveAs() {
    (new Thread(new Runnable() {
      @Override
      public void run() {
        Map<String, Object>params = new Hashtable<String, Object>();
        String fname = vwr.dialogAsk("Save Image", "jmol.png", params);
        if (fname == null)
          return;
        String type = "PNG";
        int pt = fname.lastIndexOf(".");
        if (pt > 0)
          type = fname.substring(pt + 1).toUpperCase();
        params.put("fileName", fname);
        params.put("type", type);
        params.put("image", image);
        vwr.showString(vwr.processWriteOrCapture(params), false);        
      }
    }) {
    }).start();
  }

  @Override
  public void closeMe() {
    imageMap.remove(title);
    dispose();
  }

  @Override
  public void setImage(Object oimage) {
    if (oimage == null) {
      closeMe();
      return;
    }
    int w = ((Image) oimage).getWidth(null);
    int h = ((Image) oimage).getHeight(null);
    this.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();
    g.setColor(Color.white);
    g.fillRect(0,  0 , w,  h);
    g.drawImage((Image) oimage, 0, 0, null);
    g.dispose();
    setTitle(title + " [" + w + " x " + h + "]");
    Dimension d = new Dimension(w, h);
    canvas.setPreferredSize(d);
    setBackground(Color.WHITE);
    getContentPane().setBackground(Color.WHITE);
    pack();
  }  
  

  class ImageCanvas extends JPanel {
    @Override
    public void paintComponent(Graphics g) {
      System.out.println(image.getClass().getName());
//      System.out.println(((BufferedImage)image).getRGB(0,  0));
      g.setColor(Color.white);
      g.fillRect(0,  0,  image.getWidth(null), image.getHeight(null));
      g.drawImage(image, 0, 0, null);
    }
  }
  
  @Override
  public void windowClosed(WindowEvent e) {
  }
  
  @Override
  public void windowOpened(WindowEvent e) {
  }
  @Override
  public void windowClosing(WindowEvent e) {
    closeMe();
  }
  @Override
  public void windowIconified(WindowEvent e) {
  }
  @Override
  public void windowDeiconified(WindowEvent e) {
  }
  @Override
  public void windowActivated(WindowEvent e) {
  }
  @Override
  public void windowDeactivated(WindowEvent e) {
  }

}
