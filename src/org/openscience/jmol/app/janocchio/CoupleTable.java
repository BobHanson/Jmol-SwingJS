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
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app.janocchio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.quantum.NMRCalculation;

//import BoxLayout;

public class CoupleTable extends JTabbedPane {

  NMR_Viewer viewer;
  int natomsPerModel;
  String[] labelArray;
  boolean molCDKuptodate = false;
  NmrMolecule calcProps;
  String[][] expCouples;
  JTable coupleTable;
  private CoupleTableModel coupleTableModel;
  private ListSelectionModel coupleSelection;
  int[] selectedCoupleRow = new int[2];
  JButton coupledeleteButton, coupleAddAllButton;
  private JButton coupledeleteAllButton;
  final double degtorad = Math.PI / 180;
  double yellowValue = 2.0; // cutoffs on diff for colouring cells. diff = |calc-exp|
  double redValue = 3.0;
  FrameDeltaDisplay frameDeltaDisplay;
  String CHequation = "was";
  NMRTableCellRenderer colorCellRenderer = new NMRTableCellRenderer();

  public CoupleColourSelectionPanel coupleColourSelectionPanel;
  protected NMR_JmolPanel nmrPanel;

  /**
   * Constructor
   * 
   * @param parentFrame
   *        the parent frame
   * @param nmrPanel
   *        the NMRViewer in which the animation will take place (?)
   */
  public CoupleTable(NMR_JmolPanel nmrPanel, JFrame parentFrame) {

    this.nmrPanel = nmrPanel;
    //super(parentFrame, GT.$("Coupling Constants..."), false);
    viewer = (NMR_Viewer) nmrPanel.vwr;

    JPanel mainTable = new JPanel();

    //JPanel container = new JPanel();
    mainTable.setLayout(new BorderLayout());

    mainTable.add(constructCoupleTable(), BorderLayout.CENTER);

    JPanel foo = new JPanel();
    foo.setLayout(new BorderLayout());
    foo.add(constructCoupleButtonPanel(), BorderLayout.WEST);
    //foo.add(constructDismissButtonPanel(), BorderLayout.EAST);

    mainTable.add(foo, BorderLayout.SOUTH);

    addTab("Table", null, mainTable, "Table of Selected Coupling Constants");
    CoupleParameterSelectionPanel coupleParameterSelectionPanel = new CoupleParameterSelectionPanel(
        this);
    addTab("Parameters", null, coupleParameterSelectionPanel,
        "Parameter Setting");
    coupleColourSelectionPanel = new CoupleColourSelectionPanel(this);
    addTab("Cell Colours", null, coupleColourSelectionPanel,
        "Cell Colour Setting");

    selectedCoupleRow[0] = selectedCoupleRow[1] = -1;

  }

  JComponent constructCoupleTable() {
    coupleTableModel = new CoupleTableModel();
    // Disable sorting by column -- breaks with vRow stuff
    //TableSorter sorter = new TableSorter(coupleTableModel);
    //coupleTable = new JTable(sorter);
    //sorter.setTableHeader(coupleTable.getTableHeader());
    coupleTable = new JTable(coupleTableModel);

    // Enable colouring by cell value
    TableColumn tc = coupleTable.getColumnModel().getColumn(1);

    colorCellRenderer.setYellowLevel(yellowValue);
    colorCellRenderer.setRedLevel(redValue);
    tc.setCellRenderer(colorCellRenderer);

    coupleTable.setPreferredScrollableViewportSize(new Dimension(300, 100));

    coupleTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    for (int i = 5; --i > 0;)
      coupleTable.getColumnModel().getColumn(i).setPreferredWidth(15);

    coupleTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    coupleTable.setRowSelectionAllowed(true);
    coupleTable.setColumnSelectionAllowed(false);
    coupleSelection = coupleTable.getSelectionModel();
    coupleSelection.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting())
          return;
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (lsm.isSelectionEmpty()) {

          selectedCoupleRow[0] = selectedCoupleRow[1] = -1;
          coupledeleteButton.setEnabled(false);
        } else if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
          selectedCoupleRow[0] = lsm.getMinSelectionIndex();
          selectedCoupleRow[1] = lsm.getMaxSelectionIndex();
        } else {
          selectedCoupleRow[0] = lsm.getMinSelectionIndex();
          selectedCoupleRow[1] = lsm.getMaxSelectionIndex();
          coupledeleteButton.setEnabled(true);

        }
      }
    });

    return new JScrollPane(coupleTable);
  }

  JComponent constructCoupleButtonPanel() {
    JPanel coupleButtonPanel = new JPanel();
    coupleButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

    coupleAddAllButton = new JButton(GT.$("Add All"));
    coupleAddAllButton.setToolTipText("Add all 1H-1H Couplings");
    coupleAddAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // All this necessary to delete more than one row at once
        // need to create array of viewer rows, sort, then delete
        viewer.scriptWait("measure search '{[H]}CC{[H]}'");
        updateCoupleTableData();
      }
    });
    coupleAddAllButton.setEnabled(true);
    
    coupledeleteButton = new JButton(GT.$("Del"));
    coupledeleteButton.setToolTipText("Delete Selected Couplings");
    coupledeleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // All this necessary to delete more than one row at once
        // need to create array of viewer rows, sort, then delete
        int ndelete = selectedCoupleRow[1] - selectedCoupleRow[0] + 1;
        int[] deletevRows = new int[ndelete];
        for (int j = 0, i = selectedCoupleRow[0]; i <= selectedCoupleRow[1]; i++) {
          deletevRows[j++] = getViewerRow(i);
        }
        Arrays.sort(deletevRows);
        for (int i = ndelete; --i >= 0;) {
          viewer.deleteMeasurement(deletevRows[i]);
        }

        updateCoupleTableData();
      }
    });
    coupledeleteButton.setEnabled(false);

    coupledeleteAllButton = new JButton(GT.$("Del All"));
    coupledeleteAllButton.setToolTipText("Delete All Couplings");
    coupledeleteAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i = coupleTable.getRowCount(); --i >= 0;) {
          viewer.deleteMeasurement(getViewerRow(i));
        }
        updateCoupleTableData();
      }
    });
    coupledeleteAllButton.setEnabled(false);

    //coupleButtonPanel.add(couplesetRefButton); 

    coupleButtonPanel.add(coupleAddAllButton);
    coupleButtonPanel.add(coupledeleteButton);
    coupleButtonPanel.add(coupledeleteAllButton);
    return coupleButtonPanel;
  }

  protected int getViewerRow(int i) {
    return nmrPanel.getViewerRow(i, NMRCalculation.MODE_CALC_J);
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
    //getRootPane().setDefaultButton(dismissButton);
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
    updateCoupleTableData();
    setVisible(true);
  }

  void updateCoupleTableData() {
    coupledeleteAllButton.setEnabled(viewer.getMeasurementCount() > 0);
    coupleTableModel.fireTableDataChanged();
    calcFrameDelta();
    nmrPanel.clearViewerSelection();
  }

  public int getRowCount() {
    return coupleTableModel.getRowCount();
  }

  public int[] getMeasurementCountPlusIndices(int row) {
    return coupleTableModel.getMeasurementCountPlusIndicesForTableRow(row);
  }

  class CoupleListWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      close();
    }
  }

  class CoupleTableModel extends AbstractTableModel {

    final String[] coupleHeaders = { GT.$("Angle"), GT.$("J"), "Atom 1",
        "Atom 2", "Exp J" };

    @Override
    public String getColumnName(int col) {
      return coupleHeaders[col];
    }

    @Override
    public int getRowCount() {
      natomsPerModel = calcNatomsPerModel();
      int rowCount = 0;

      for (int i = 0; i < viewer.getMeasurementCount(); i++) {
        if (nmrPanel.getViewerMeasurement(i,  NMRCalculation.MODE_CALC_J) != null) {
          ++rowCount;
        }
      }
      return rowCount;
    }

    @Override
    public int getColumnCount() {
      return 5;
    }

    @Override
    public Class<?> getColumnClass(int col) {
      return String.class;
    }

    public int[] getMeasurementCountPlusIndicesForTableRow(int row) {
      return viewer.getMeasurementCountPlusIndices(getViewerRow(row));
    }

    @Override
    public Object getValueAt(int row, int col) {

      // Always check that the CDK conversion is current  
      if (!molCDKuptodate) {
        addMol();
      }
      // Need to convert coupleTable index to viewer index
      int vRow = getViewerRow(row);
      Atom[] atoms = nmrPanel.getViewerMeasurement(vRow, NMRCalculation.MODE_CALC_J);
      double[] dihecouple = calcProps.calcCouple(atoms);
      MeasureCouple measure = (dihecouple == null ? null : new MeasureCouple(
          expCouples[viewer.indexInFrame(atoms[0])][viewer.indexInFrame(atoms[3])], dihecouple[1]));
      if (col == 0) {
        //return  viewer.getMeasurementStringValue(vRow);
        return Measure.formatDistance(dihecouple == null ? 0 : dihecouple[0]
            / degtorad);
      }
      if (col == 1) {

        //DecimalFormat df = new DecimalFormat("0.0000");
        //return df.format(measure.getCalcValue().doubleValue());
        return measure;
      }
      if (col == 4) {
        return (measure == null ? "" : measure.getExpValue());
      }
//      if (col >= countPlusIndices.length) {
//        return null;
//      }
//
      int atomIndex = (col == 2 ? atoms[0].i : atoms[3].i);
      if (atomIndex < 0)
        return "";

      String name = labelArray[atomIndex];
      String reserve = "" + viewer.getAtomNumber(atomIndex) + " "
          + viewer.getAtomName(atomIndex);
      if (name == null) {
        name = reserve;
        return name;
      } else if (name.trim().length() == 0) {
        name = reserve;

      }
      return name;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
      if (col == 4) {
        String val = (String) value;
        if (val.trim().length() == 0) {
          val = null;
        }
        try {
          Double.valueOf(val);
        } catch (Exception e) {
          val = null;
        }
        int vRow = getViewerRow(row);
        int[] countPlusIndices = getMeasurementCountPlusIndices(vRow);
        expCouples[countPlusIndices[1]][countPlusIndices[4]] = val;
        expCouples[countPlusIndices[4]][countPlusIndices[1]] = val;
        //fireTableDataChanged();
        updateCoupleTableData();
      }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
      return (col == 4);
    }
  }

  private void calcFrameDelta() {
    int n = coupleTableModel.getRowCount();
    double frameDelta = 0.0;
    for (int i = 0; i < n; i++) {

      // Use diff set up for frame colouring
      // 

      Measure measure = (Measure) (coupleTableModel.getValueAt(i, 1));
      if (measure != null)
        frameDelta += measure.getDiff();

    }
    frameDeltaDisplay.setFrameDeltaCouple(frameDelta);
  }

  public void updateTables() {
    updateCoupleTableData();
  }

  //  private double getdegfromString(String sDist) {
  //    int l = sDist.length();
  //    String spDist = sDist.substring(0, l - 1);
  //    Double dDist = new Double(spDist);
  //    return dDist.doubleValue();
  //  }

  int calcNatomsPerModel() {
    return nmrPanel.getFrameAtomCount();
  }

  /* This should only be called once the molecule data has been read in */
  public void addMol() {
    calcProps = nmrPanel.getDistanceJMolecule(null, labelArray, false);
    calcProps.setCHequation(CHequation);
    this.molCDKuptodate = true;
  }

  public void setmolCDKuptodate(boolean value) {
    this.molCDKuptodate = value;
  }

  public void allocateLabelArray(int numAtoms) {
    labelArray = new String[numAtoms];
  }

  public void allocateExpCouples(int numAtoms) {
    expCouples = new String[numAtoms][numAtoms];
  }

  public String getExpCouple(int i, int j) {
    return expCouples[i][j];
  }

  public void setExpCouple(String value, int i, int j) {
    if (value.trim().length() == 0) {
      value = null;
    }
    expCouples[i][j] = value;
    expCouples[j][i] = value;
  }

  public void setLabelArray(String[] labelArray) {
    this.labelArray = labelArray;
  }

  public void setRedValue(double value) {
    this.redValue = value;
    colorCellRenderer.setRedLevel(redValue);
  }

  public void setYellowValue(double value) {
    this.yellowValue = value;
    colorCellRenderer.setYellowLevel(yellowValue);
  }

  public double getRedValue() {
    return this.redValue;
  }

  public double getYellowValue() {
    return this.yellowValue;
  }

  public void setCHequation(String eq) {
    this.CHequation = eq;
    setmolCDKuptodate(false);

  }

  public void setFrameDeltaDisplay(FrameDeltaDisplay frameDeltaDisplay) {
    this.frameDeltaDisplay = frameDeltaDisplay;
  }
}
