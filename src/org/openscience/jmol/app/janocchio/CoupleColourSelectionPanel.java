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
 * CoupleColourSelectionPanel.java
 *
 * Created on 24 September 2006, 17:27
 */
package org.openscience.jmol.app.janocchio;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class CoupleColourSelectionPanel extends JPanel {
  CoupleTable coupleTable;

  public CoupleColourSelectionPanel(CoupleTable coupleTable) {
    this.coupleTable = coupleTable;
    initComponents();
    redField.setText(String.valueOf(coupleTable.getRedValue()));
    yellowField.setText(String.valueOf(coupleTable.getYellowValue()));
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents() {
    jLabel1 = new JLabel();
    jLabel2 = new JLabel();
    redLabel = new JLabel();
    yellowLabel = new JLabel();
    redField = new JTextField();
    yellowField = new JTextField();
    setAllButton = new JButton();

    setAutoscrolls(true);
    setPreferredSize(null);
    jLabel1.setText("Colour Cutoffs");

    jLabel2.setText("Delta = |Calc - Exp| ");

    redLabel.setBackground(new Color(255, 200, 200));
    redLabel.setText("Red if Delta > ");

    yellowLabel.setText("Yellow if Delta > ");

    redField.setBackground(new Color(255, 200, 200));
    redField.setHorizontalAlignment(SwingConstants.RIGHT);
    redField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        redFieldActionPerformed(evt);
      }
    });

    yellowField.setBackground(new Color(255, 255, 0));
    yellowField.setHorizontalAlignment(SwingConstants.RIGHT);
    yellowField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        yellowFieldActionPerformed(evt);
      }
    });

    setAllButton.setText("Set All");
    setAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        setAllButtonActionPerformed(evt);
      }
    });

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(layout
        .createParallelGroup(Alignment.LEADING)
        .addGroup(
            layout.createSequentialGroup().addGap(71, 71, 71)
                .addComponent(jLabel1))
        .addGroup(
            layout
                .createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, 233,
                    GroupLayout.PREFERRED_SIZE))
        .addGroup(
            layout
                .createSequentialGroup()
                .addGap(52, 52, 52)
                .addGroup(
                    layout
                        .createParallelGroup(Alignment.TRAILING)
                        .addComponent(setAllButton)
                        .addGroup(
                            layout
                                .createSequentialGroup()
                                .addComponent(yellowLabel)
                                .addGap(2, 2, 2)
                                .addComponent(yellowField,
                                    GroupLayout.PREFERRED_SIZE, 61,
                                    GroupLayout.PREFERRED_SIZE))
                        .addGroup(
                            layout
                                .createSequentialGroup()
                                .addComponent(redLabel)
                                .addGap(2, 2, 2)
                                .addComponent(redField,
                                    GroupLayout.PREFERRED_SIZE, 61,
                                    GroupLayout.PREFERRED_SIZE)))));
    layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
        .addGroup(
            layout
                .createSequentialGroup()
                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 24,
                    GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, 34,
                    GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addGroup(
                    layout
                        .createParallelGroup(Alignment.BASELINE)
                        .addComponent(redLabel)
                        .addComponent(redField, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(
                    layout
                        .createParallelGroup(Alignment.BASELINE)
                        .addComponent(yellowLabel)
                        .addComponent(yellowField, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE)).addGap(30, 30, 30)
                .addComponent(setAllButton).addContainerGap()));
  }// </editor-fold>//GEN-END:initComponents

  void setAllButtonActionPerformed(ActionEvent evt) {
    yellowFieldActionPerformed(evt);
    redFieldActionPerformed(evt);
  }

  /**
   * @param evt
   */
  void yellowFieldActionPerformed(ActionEvent evt) {
    String text = yellowField.getText();
    coupleTable.setYellowValue(Double.parseDouble(text));
    yellowField.setText(String.valueOf(coupleTable.getYellowValue()));
  }

  /**
   * @param evt
   */
  void redFieldActionPerformed(ActionEvent evt) {
    String text = redField.getText();
    coupleTable.setRedValue(Double.parseDouble(text));
    redField.setText(String.valueOf(coupleTable.getRedValue()));
  }

  public JTextField getYellowField() {
    return yellowField;
  }

  public JTextField getRedField() {
    return redField;
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JLabel jLabel1;
  private JLabel jLabel2;
  private JTextField redField;
  private JLabel redLabel;
  private JButton setAllButton;
  private JTextField yellowField;
  private JLabel yellowLabel;
  // End of variables declaration//GEN-END:variables

}