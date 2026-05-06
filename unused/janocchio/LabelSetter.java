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
 *
 */
package org.openscience.jmol.app.janocchio;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

//import BoxLayout;

public class LabelSetter extends JPanel {

  NMR_Viewer viewer;
  NoeTable noeTable;
  CoupleTable coupleTable;
  String[] labelArray;
  //int numAtoms;
  int selectedAtomIndex;
  JLabel label;
  JTextField field;
  int minindex = 1;

  public LabelSetter(NMR_Viewer viewer, NoeTable noeTable,
      CoupleTable coupleTable) {

    this.viewer = viewer;
    this.noeTable = noeTable;
    this.coupleTable = coupleTable;
    setLayout(new BorderLayout());

    label = new JLabel();
    label.setText(getLabelText());

    field = new JTextField(5);

    field.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        fieldActionPerformed(evt);
      }
    });

    add(label, BorderLayout.WEST);
    add(field, BorderLayout.CENTER);
  }

  /**
   * @param evt
   */
  void fieldActionPerformed(ActionEvent evt) {

    labelArray[selectedAtomIndex] = field.getText();
    String command = setLabelString(selectedAtomIndex,
        labelArray[selectedAtomIndex]);
    viewer.script(command);

    noeTable.setLabelArray(labelArray);
    noeTable.setmolCDKuptodate(false); // If labels become equivalent, need to recalculate NoeMatrix
    coupleTable.setLabelArray(labelArray);
    noeTable.updateTables();
    coupleTable.updateTables();
  }

  public void setSelectedAtomIndex(int number) {
    this.selectedAtomIndex = number;
    label.setText(getLabelText());
    field.setText(labelArray[number]);
  }

  public void allocateLabelArray(int numAtoms) {
    labelArray = new String[numAtoms];
  }

  private String getLabelText() {

    String text = " Atom " + String.valueOf(selectedAtomIndex + minindex)
        + " Label ";
    return text;
  }

  public String[] getLabelArray() {
    return labelArray;
  }

  public int getMinindex() {
    return minindex;
  }

  public String setLabel(int i, String label) {
    labelArray[i] = label;
    return setLabelString(i, label);
  }

  public String setLabelString(int i, String label) {
    String command = "select (atomno=" + String.valueOf(i + minindex) + ");";
    if (label == null || label.trim().length() == 0) {
      command = command + "label off";
    } else {
      command = command + "label " + label;
    }
    command = command + ";set display NORMAL; select ALL;";
    return command;
  }
}
