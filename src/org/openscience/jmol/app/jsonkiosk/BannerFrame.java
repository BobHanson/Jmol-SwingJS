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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A simple class that throws up a white rectangle that has a single centered label.
 * 
 */
public class BannerFrame extends JFrame {

  public BannerFrame(int width, int height) {
    setTitle("Banner");
    setUndecorated(true);
    setBackground(Color.WHITE);
    setSize(width, height);
    setBounds(0, 0, width, height);
    bannerLabel = new JLabel("<html>type exitJmol[enter] to quit</html>", SwingConstants.CENTER);
    bannerLabel.setPreferredSize(getSize());
    bannerLabel.setFont(new Font("Helvetica", Font.BOLD, 30));
    getContentPane().add(bannerLabel, BorderLayout.CENTER);
    setVisible(true);
    // setAlwaysOnTop(true);
  }

  private JLabel bannerLabel;
  
  public void setLabel(String label) {
    if (label != null)
      bannerLabel.setText(label);
    setVisible(label != null);
  }

}
