/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-19 08:25:14 -0500 (Wed, 19 May 2010) $
 * $Revision: 13133 $
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

package org.openscience.jmol.app.jsonkiosk;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A borderless rectangle, like the applet, that contains the application
 * for use in kiosk-style displays, as for example projected on the wall
 * as in http://molecularPlayground.org
 * 
 */
public class KioskFrame extends JFrame {

  public KioskFrame(int x, int y, int width, int height, JPanel kioskPanel) {
    setTitle("KioskFrame");
    setUndecorated(true);
    setBackground(new Color(0, 0, 0, 0));
    setPanel(kioskPanel);
    setSize(width, height);
    setBounds(x, y, width, height);
    setVisible(true);
  }

  public void setPanel(JPanel kioskPanel) {
    if (kioskPanel == null)
      return;
    getContentPane().add(kioskPanel);
  }
  
}
