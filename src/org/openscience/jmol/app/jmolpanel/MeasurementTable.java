/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-07-22 20:29:48 -0500 (Sun, 22 Jul 2018) $
 * $Revision: 21922 $
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

import org.jmol.i18n.GT;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;

public class MeasurementTable extends JDialog {

  Viewer vwr;
  private JTable measurementTable;
  private MeasurementTableModel measurementTableModel;
  int selectedMeasurementRow = -1;
  JButton deleteButton;
  JButton deleteAllButton;

  /**
   * Constructor
   *
   * @param parentFrame the parent frame
   * @param vwr the JmolViewer in which the animation will take place (?)
   */
  public MeasurementTable(Viewer vwr, JFrame parentFrame) {

    super(parentFrame, GT.$("Measurements"), false);
    this.vwr = vwr;

    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());

    container.add(constructMeasurementTable(),BorderLayout.CENTER);

    JPanel foo = new JPanel();
    foo.setLayout(new BorderLayout());
    foo.add(constructMeasurementButtonPanel(), BorderLayout.WEST);
    foo.add(constructDismissButtonPanel(), BorderLayout.EAST);

    container.add(foo, BorderLayout.SOUTH);
    
    addWindowListener(new MeasurementListWindowListener());

    getContentPane().add(container);
    pack();
    centerDialog();
  }

  JComponent constructMeasurementTable() {
    measurementTableModel = new MeasurementTableModel();
    measurementTable = new JTable(measurementTableModel);

    measurementTable
      .setPreferredScrollableViewportSize(new Dimension(300, 100));

    measurementTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    for (int i = 5; --i > 0; )
      measurementTable.getColumnModel().getColumn(i).setPreferredWidth(15);

    measurementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    measurementTable.setRowSelectionAllowed(true);
    measurementTable.setColumnSelectionAllowed(false);
    ListSelectionModel measurementSelection = measurementTable.getSelectionModel();
    measurementSelection.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) return;
          ListSelectionModel lsm = (ListSelectionModel)e.getSource();
          if (lsm.isSelectionEmpty()) {
            selectedMeasurementRow = -1;
            deleteButton.setEnabled(false);
          } else {
            selectedMeasurementRow = lsm.getMinSelectionIndex();
            deleteButton.setEnabled(true);
          }
        }
      });

    return new JScrollPane(measurementTable);
  }

  JComponent constructMeasurementButtonPanel() {
    JPanel measurementButtonPanel = new JPanel();
    measurementButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

    deleteButton = new JButton(GT.$("Delete"));
    deleteButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          vwr.script("measures delete " + (selectedMeasurementRow + 1) + JC.SCRIPT_EDITOR_IGNORE);
          updateMeasurementTableData();
        }
      });
    deleteButton.setEnabled(false);
    
    deleteAllButton = new JButton(GT.$("DeleteAll"));
    deleteAllButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          vwr.script("measures delete" + JC.SCRIPT_EDITOR_IGNORE);
          updateMeasurementTableData();
        }
      });
    deleteAllButton.setEnabled(false);

    measurementButtonPanel.add(deleteAllButton);
    measurementButtonPanel.add(deleteButton);
    return measurementButtonPanel;
  }

  JComponent constructDismissButtonPanel() {
    JPanel dismissButtonPanel = new JPanel();
    dismissButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    JButton dismissButton = new JButton(GT.$("Dismiss"));
    dismissButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          close();
        }
      });
    dismissButtonPanel.add(dismissButton);
    getRootPane().setDefaultButton(dismissButton);
    return dismissButtonPanel;
  }

  protected void centerDialog() {

    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension size = this.getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  public void close() {
    this.setVisible(false);
  }

  public void activate() {
    updateMeasurementTableData();
    setVisible(true);
  }

  void updateMeasurementTableData() {
    deleteAllButton.setEnabled(vwr.getMeasurementCount() > 0);
    measurementTableModel.fireTableDataChanged();
  }

  class MeasurementListWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      close();
    }
  }

  final Class<? extends String> stringClass = "".getClass();

  class MeasurementTableModel extends AbstractTableModel {

    final String[] measurementHeaders = {
      GT.$("Value"),
      "a", "b", "c", "d", };

    @Override
    public String getColumnName(int col) { 
      return measurementHeaders[col];
    } 
    @Override
    public int getRowCount() { return vwr.getMeasurementCount(); }
    @Override
    public int getColumnCount() { return 5; }

    @Override
    public Class<? extends String> getColumnClass(int col) {
      return stringClass;
    }
    @Override
    public Object getValueAt(int row, int col) {
      //System.out.println("meata " + row + " " + col);
      if (col == 0) {
        deleteAllButton.setEnabled(true);
        return vwr.getMeasurementStringValue(row);
      }
      int[] countPlusIndices = vwr.getMeasurementCountPlusIndices(row);
      if (countPlusIndices == null || col > countPlusIndices[0])
        return null;
      int atomIndex = countPlusIndices[col];
      return (vwr.getAtomInfo(atomIndex >= 0 ? atomIndex : -row * 10 - col));
    }

    @Override
    public boolean isCellEditable(int row, int col) { return false; }
  }

  public void updateTables() {
    updateMeasurementTableData();
  }
}
