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
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

//import BoxLayout;

public class PopulationDisplay extends JPanel {

  NMR_Viewer viewer;
  int frameNumber;
  int storedFrameNumber;
  double[] population;
  int imaxp;
  double maxp = 0.0;
  JLabel label;
  JTextField field;
  JCheckBox populationFrames;

  /**
   * Constructor
   * 
   * @param viewer
   *        the NMRViewer in which the animation will take place (?)
   */
  public PopulationDisplay(NMR_Viewer viewer) {

    this.viewer = viewer;
    setLayout(new FlowLayout(FlowLayout.LEFT));

    // Fix as I can't work the layout manager properly
    this.setMaximumSize(new Dimension(1000, 10));

    this.setVisible(false);

    label = new JLabel();
    label.setText(getLabelText());

    field = new JTextField(5);
    field.setEditable(false);

    populationFrames = new JCheckBox("Display Populated Conformers");
    populationFrames.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        populationFramesActionPerformed(evt);
      }
    });

    add(label, BorderLayout.WEST);
    add(field, BorderLayout.CENTER);
    add(populationFrames, BorderLayout.EAST);
  }

  private void populationFramesActionPerformed(ActionEvent evt) {
    String command;

    if (populationFrames.isSelected()) {
      storedFrameNumber = frameNumber;
      //display frames with large enough population
      command = "frame 0; hide all;";
      for (int i = 1; i < population.length; i++) {
        if (population[i] >= 0.1) {
          String col = getColourString(population[i]);
          if (i == imaxp) {
            command = command + "select */" + i + "; color opaque " + col + ";";
          } else {
            command = command + "select */" + i + "; color translucent " + col
                + ";";
          }

          command = command + "display */" + i + " or displayed;";
        }
      }

    } else {
      // Go back to previous frame and normal operation
      command = "select all; color cpk; frame " + storedFrameNumber
          + "; display all;";
    }
    command = command + "select ALL;";
    viewer.script(command);

  }

  private String getColourString(double p) {

    // Fades from white to blue as population increase

    double val = (1.0 - p) * 255.0;
    int ival = Math.round(Math.round(val));
    return "[" + ival + "," + ival + ",255]";
  }

  public void setFrameNumberFromViewer(int number) {
    this.frameNumber = number;
    if (population != null) {
      field.setText(String.valueOf(population[number]));
    }

  }

  public int getFrameNumber() {
    return frameNumber;
  }

  private String getLabelText() {

    String text = "NAMFIS Population :";
    return text;
  }

  public void addPopulation(double[] population) {
    this.population = population;
    this.setVisible(true);
    field.setText(String.valueOf(population[frameNumber]));
    for (int i = 1; i < population.length; i++) {
      if (population[i] > maxp) {
        maxp = population[i];
        imaxp = i;
      }
    }

  }

  public double[] getPopulation() {
    return this.population;
  }
}
