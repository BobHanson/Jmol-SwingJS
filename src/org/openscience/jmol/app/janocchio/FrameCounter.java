/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol.app.janocchio;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

//import BoxLayout;

public class FrameCounter extends JPanel {

  NMR_Viewer viewer;
  int frameNumber;
  int frameCount;
  JLabel label;
  JTextField field;

  /**
   * Constructor
   * 
   * @param viewer
   *        the NMRViewer in which the animation will take place (?)
   */
  public FrameCounter(NMR_Viewer viewer) {

    this.viewer = viewer;
    setLayout(new BorderLayout());

    label = new JLabel();
    label.setText(getLabelText());

    field = new JTextField(5);
    field.setText(String.valueOf(getFrameNumber()));
    field.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        fieldActionPerformed(evt);
      }
    });

    add(label, BorderLayout.WEST);
    add(field, BorderLayout.CENTER);
  }

  private void fieldActionPerformed(ActionEvent evt) {
    int number = Integer.parseInt(field.getText());
    setFrameNumberChangeViewer(number);

  }

  public void setFrameNumberChangeViewer(int number) {
    this.frameNumber = number;
    String command = new String("frame ");
    command = command + String.valueOf(number);
    viewer.script(command);
  }

  public void setFrameNumberFromViewer(int number) {
    this.frameNumber = number;
    field.setText(String.valueOf(number));

  }

  public int getFrameNumber() {
    return frameNumber;
  }

  public void setFrameCount(int number) {
    this.frameCount = number;
    label.setText(getLabelText());
  }

  private String getLabelText() {

    String text = " Frame [" + String.valueOf(frameCount) + " total] :";
    return text;
  }

}
