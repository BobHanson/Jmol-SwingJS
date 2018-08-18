/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-11-24 15:00:42 -0600 (Sun, 24 Nov 2013) $
 * $Revision: 19010 $
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
package org.openscience.jmol.app.jmolpanel;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Frame;
import java.awt.Window;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.jmol.i18n.GT;
import org.openscience.jmol.app.SplashInterface;

public class Splash extends Window implements SplashInterface {

  private Image splashImage;
  private int imgWidth, imgHeight;
  private static final int BORDERSIZE = 10;
  private static final Color BORDERCOLOR = Color.blue;
  private String status = GT._("Loading...");
  private int textY;
  private int statusTop;
  private static final int STATUSSIZE = 10;
  private static final Color TEXTCOLOR = Color.white;

  public Splash(Frame parent, ImageIcon ii) {

    super(new Frame());
    splashImage = ii.getImage();
    imgWidth = splashImage.getWidth(this);
    imgHeight = splashImage.getHeight(this);
    if (parent == null)
      return;
    showSplashScreen();
    parent.addWindowListener(new WindowListener());
  }

  public void showSplashScreen() {

    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension screenSize = tk.getScreenSize();
    setBackground(BORDERCOLOR);
    int w = imgWidth + (BORDERSIZE * 2);
    int h = imgHeight + (BORDERSIZE * 2) + STATUSSIZE;
    int x = (screenSize.width - w) / 2;
    int y = (screenSize.height - h) / 2;
    setBounds(x, y, w, h);
    statusTop = BORDERSIZE + imgHeight;
    textY = BORDERSIZE + STATUSSIZE + imgHeight + 1;
    setVisible(true);

  }

  @Override
  public void paint(Graphics g) {

    g.drawImage(splashImage, BORDERSIZE, BORDERSIZE, imgWidth, imgHeight,
        this);
    g.setColor(BORDERCOLOR);
    g.fillRect(BORDERSIZE, statusTop, imgWidth, textY);
    g.setColor(TEXTCOLOR);
    g.drawString(status, BORDERSIZE, textY);
  }

  @Override
  public void showStatus(String message) {

    if (message != null) {
      status = message;
      Graphics g = this.getGraphics();
      if (g == null) {
        return;
      }
      g.setColor(BORDERCOLOR);
      g.fillRect(BORDERSIZE, statusTop, imgWidth + BORDERSIZE, textY);
      g.setColor(TEXTCOLOR);
      g.drawString(status, BORDERSIZE, textY);
    }
  }

  class WindowListener extends WindowAdapter {

    @Override
    public void windowActivated(WindowEvent we) {
      setVisible(false);
      dispose();
    }
  }
}
