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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FrameDeltaDisplay extends JPanel {

  NMR_Viewer viewer;

  JLabel label;
  JTextField totalfield;

  JTextField coupleWeightField;
  JTextField noeWeightField;

  double coupleWeight = 0.1;
  double noeWeight = 1.0;

  double frameDeltaNoe = 0.0;
  double frameDeltaCouple = 0.0;

  /**
   * Constructor
   * 
   * @param viewer
   *        the NMRViewer in which the animation will take place (?)
   */
  public FrameDeltaDisplay(NMR_Viewer viewer) {

    this.viewer = viewer;

    setLayout(new FlowLayout(FlowLayout.LEFT));

    // Fix as I can't work the layout manager properly
    this.setMaximumSize(new Dimension(1000, 10));

    this.setVisible(false);

    label = new JLabel();
    label.setText(getLabelText());

    totalfield = new JTextField(4);
    totalfield.setEditable(false);

    JLabel coupleWeightLabel = new JLabel();
    coupleWeightLabel.setText("Couple Weight");
    JLabel noeWeightLabel = new JLabel();
    noeWeightLabel.setText("NOE Weight");

    coupleWeightField = new JTextField(3);
    setCoupleWeight(coupleWeight);

    noeWeightField = new JTextField(3);
    setNoeWeight(noeWeight);

    noeWeightField.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        String text = noeWeightField.getText();
        setNoeWeight(Double.parseDouble(text));
      }
    });
    coupleWeightField.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        String text = coupleWeightField.getText();
        setCoupleWeight(Double.parseDouble(text));
      }
    });

    /*populationFrames = new JCheckBox("Display Populated Conformers");
    populationFrames.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            populationFramesActionPerformed(evt);
        }
    });*/

    add(label, BorderLayout.WEST);
    add(totalfield, BorderLayout.EAST);
    add(noeWeightLabel, BorderLayout.EAST);
    add(noeWeightField, BorderLayout.EAST);
    add(coupleWeightLabel, BorderLayout.EAST);
    add(coupleWeightField, BorderLayout.EAST);
    //add(populationFrames,BorderLayout.EAST);
  }

  /*private void populationFramesActionPerformed(ActionEvent evt) {
  }*/

  public void setFrameDeltaNoe(double frameDelta) {

    this.frameDeltaNoe = frameDelta;
    setFieldText();

  }

  public void setFrameDeltaCouple(double frameDelta) {

    this.frameDeltaCouple = frameDelta;
    setFieldText();

  }

  private void setFieldText() {
    DecimalFormat df = new DecimalFormat("#0.00");
    double error = this.noeWeight * this.frameDeltaNoe + this.coupleWeight
        * this.frameDeltaCouple;
    totalfield.setText(df.format(error));
  }

  private String getLabelText() {

    String text = "Error:";
    return text;
  }

  private void setNoeWeight(double weight) {
    this.noeWeight = weight;
    DecimalFormat df = new DecimalFormat("#0.0");
    noeWeightField.setText(df.format(weight));
    setFieldText();
  }

  private void setCoupleWeight(double weight) {
    this.coupleWeight = weight;
    DecimalFormat df = new DecimalFormat("#0.0");
    coupleWeightField.setText(df.format(weight));
    setFieldText();
  }

  public double getNoeWeight() {
    return this.noeWeight;
  }

  public double getCoupleWeight() {
    return this.coupleWeight;
  }

}
