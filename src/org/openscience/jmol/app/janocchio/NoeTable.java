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
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComboBox;
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
import org.jmol.quantum.NMRCalculation;

//import BoxLayout;


public class NoeTable extends JTabbedPane {

//  AtomContainer molCDK;
  
  NMR_JmolPanel nmrPanel;
  NMR_Viewer viewer;
  
  String[] labelArray;
  boolean molCDKuptodate = false;
  int natomsPerModel;
  String[][] expNoes;
  String[][] expDists; // expNoes and expDists are separate user inputs. You can use one, both or neither
  boolean lexpNoes = true;
  NmrMolecule calcProps;
  // NMR parameters for NoeMatrix
  // These are the defaults
  double freq = 400; // Spec. frequency/MHz
  double tau = 80; // correlation time/ps
  double tMix = 0.5; // mixing time/s
  double cutoff = 10; // distance beyond which proton pairs are not considered/A
  double rhoStar = 0.1; // constant term in relaxation rate/s-1
  boolean noesy = true; // NOESY(true) or ROESY

  double yellowValue = 0.2; // cutoffs on diff for colouring cells. diff = |log(calc/exp)|
  double redValue = 0.4;
  FrameDeltaDisplay frameDeltaDisplay;
  NMRTableCellRenderer colorCellRenderer = new NMRTableCellRenderer();

  JTable noeTable;
  private NoeTableModel noeTableModel;
  private ListSelectionModel noeSelection;
  int[] selectedNoeRow = new int[2];

  JButton noedeleteButton;
  private JButton noedeleteAllButton;
  JButton noesetRefButton;

  JComboBox<String> expOrDistButton;

  double noeNPrefValue = 1.0; // Default for the NOEPROM spin matrix calculation
  int[] noeNPrefIndices = new int[2]; // atoms for reference NOE distance
  double noeExprefValue = 1.0; // Default for the Experimental reference value

  boolean lrefSingle = true; // Normalisation method. True if use single, selected
                             // pair, false if always divide by the i,i diagonal

  public NoeParameterSelectionPanel noeParameterSelectionPanel;
  public NoeColourSelectionPanel noeColourSelectionPanel;

  /**
   * Constructor
   * 
   * @param parentFrame
   *        the parent frame
   * @param nmrPanel
   *        the NMRViewer in which the animation will take place (?)
   */
  public NoeTable(NMR_JmolPanel nmrPanel, JFrame parentFrame) {

    //super(parentFrame, GT.$("Noes..."), false);
    this.nmrPanel = nmrPanel;
    viewer = (NMR_Viewer) nmrPanel.vwr;

    JPanel mainTable = new JPanel();
    mainTable.setLayout(new BorderLayout());

    mainTable.add(constructNoeTable(), BorderLayout.CENTER);

    JPanel foo = new JPanel();
    foo.setLayout(new BorderLayout());
    foo.add(constructNoeButtonPanel(), BorderLayout.CENTER);
    // NOE panel permanently there, but can be collapsed as it is in a SplitPanel
    //foo.add(constructDismissButtonPanel(), BorderLayout.EAST);

    mainTable.add(foo, BorderLayout.SOUTH);

    addTab("Table", null, mainTable, "Table of Selected NOEs");
    noeParameterSelectionPanel = new NoeParameterSelectionPanel(this);
    addTab("Parameters", null, noeParameterSelectionPanel, "Parameter Setting");

    noeColourSelectionPanel = new NoeColourSelectionPanel(this);
    addTab("Cell Colours", null, noeColourSelectionPanel, "Cell Colour Setting");

    selectedNoeRow[0] = selectedNoeRow[1] = -1;
    noeNPrefIndices[0] = noeNPrefIndices[1] = -1;
  }

  JComponent constructNoeTable() {
    noeTableModel = new NoeTableModel();
    // Disable sorting by column -- this breaks with vRow stuff
    //TableSorter sorter = new TableSorter(noeTableModel);
    //noeTable = new JTable(sorter);
    //sorter.setTableHeader(noeTable.getTableHeader());
    noeTable = new JTable(noeTableModel);

    // Enable colouring by cell value
    TableColumn tc = noeTable.getColumnModel().getColumn(1);

    setYellowValue(yellowValue);
    setRedValue(redValue);
    tc.setCellRenderer(colorCellRenderer);
    // Colour both NOEs and distances according to their experimental values
    tc = noeTable.getColumnModel().getColumn(0);
    tc.setCellRenderer(colorCellRenderer);

    noeTable.setPreferredScrollableViewportSize(new Dimension(300, 100));

    noeTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    for (int i = 5; --i > 0;)
      noeTable.getColumnModel().getColumn(i).setPreferredWidth(15);

    noeTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    noeTable.setRowSelectionAllowed(true);
    noeTable.setColumnSelectionAllowed(false);
    noeSelection = noeTable.getSelectionModel();
    noeSelection.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting())
          return;
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (lsm.isSelectionEmpty()) {
          selectedNoeRow[0] = selectedNoeRow[1] = -1;
          noedeleteButton.setEnabled(false);
        } else if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
          selectedNoeRow[0] = lsm.getMinSelectionIndex();
          selectedNoeRow[1] = lsm.getMaxSelectionIndex();
          noesetRefButton.setEnabled(false);
        } else {
          selectedNoeRow[0] = lsm.getMinSelectionIndex();
          selectedNoeRow[1] = lsm.getMaxSelectionIndex();
          noedeleteButton.setEnabled(true);
          noesetRefButton.setEnabled(true);
        }
      }
    });

    return new JScrollPane(noeTable);
  }

  JComponent constructNoeButtonPanel() {
    JPanel noeButtonPanel = new JPanel();
    noeButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

    noedeleteButton = new JButton(GT.$("Del"));
    noedeleteButton.setToolTipText("Delete Selected NOEs");
    noedeleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        // All this necessary to delete more than one row at once
        // need to create array of viewer rows, sort, then delete
        int ndelete = selectedNoeRow[1] - selectedNoeRow[0] + 1;
        int[] deletevRows = new int[ndelete];
        for (int pt = 0, n = selectedNoeRow[1], i = selectedNoeRow[0]; i <= n; i++) {
          int vRow = getViewerRow(i);
          deletevRows[pt] = vRow;
          pt++;

        }
        Arrays.sort(deletevRows);
        for (int i = ndelete - 1; i >= 0; i--) {
          viewer.deleteMeasurement(deletevRows[i]);
        }
        updateNoeTableData();
        viewer.script("background white");
      }
    });
    noedeleteButton.setEnabled(false);

    noesetRefButton = new JButton(GT.$("Ref"));
    noesetRefButton.setToolTipText("Set Reference NOE");

    noesetRefButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int vRow = getViewerRow(selectedNoeRow[0]);
        //noerefDist = getnmfromString(viewer.getMeasurementStringValue(vRow));
        int[] countPlusIndices = viewer.getMeasurementCountPlusIndices(vRow);
        noeNPrefIndices[0] = countPlusIndices[1];
        noeNPrefIndices[1] = countPlusIndices[2];
        noeNPrefValue = calcProps.getJmolNoe(countPlusIndices[1],
            countPlusIndices[2]);

        String val = expNoes[countPlusIndices[1]][countPlusIndices[2]];
        if (val == null) {
          noeExprefValue = 1.0;
        } else {
          noeExprefValue = (Double.valueOf(val)).doubleValue();

          if (!((noeExprefValue < 0.0) || (noeExprefValue > 0.0))) {
            noeExprefValue = 1.0;
          }
        }
        //noerefIndex = selectedNoeRow;
        updateNoeTableData();
      }
    });

    noesetRefButton.setEnabled(false);

    noedeleteAllButton = new JButton(GT.$("Del All"));
    noedeleteAllButton.setToolTipText("Delete All NOEs");
    noedeleteAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i = noeTable.getRowCount() - 1; i >= 0; i--) {
          int vRow = getViewerRow(i);
          viewer.deleteMeasurement(vRow);
        }
        updateNoeTableData();
      }
    });
    noedeleteAllButton.setEnabled(false);

    String[] labels = { "Exp NOEs", "Exp Dists" };
    expOrDistButton = new JComboBox<String>(labels);
    expOrDistButton
        .setToolTipText("Choose either experimental NOEs or experimental distances to display and enter");
    expOrDistButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        @SuppressWarnings("unchecked")
        JComboBox<String> cb = (JComboBox<String>) e.getSource();
        int sel = cb.getSelectedIndex();
        if (sel == 0) {
          lexpNoes = true;
        } else {
          lexpNoes = false;
        }
        updateNoeTableStructure();
        updateNoeTableData();
      }
    });

    noeButtonPanel.add(noesetRefButton);

    noeButtonPanel.add(noedeleteButton);
    noeButtonPanel.add(noedeleteAllButton);
    noeButtonPanel.add(expOrDistButton);
    return noeButtonPanel;
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
    updateNoeTableData();
    setVisible(true);
  }

  void updateNoeTableData() {
    noedeleteAllButton.setEnabled(viewer.getMeasurementCount() > 0);
    noeTableModel.fireTableDataChanged();
    calcFrameDelta();
    nmrPanel.clearViewerSelection();
  }

  void updateNoeTableStructure() {
    // Called when column header is changed
    noeTableModel.fireTableStructureChanged();
    // Need to reapply custom rendering, for some reason
    TableColumn tc = noeTable.getColumnModel().getColumn(1);
    tc.setCellRenderer(colorCellRenderer);
    tc = noeTable.getColumnModel().getColumn(0);
    tc.setCellRenderer(colorCellRenderer);
  }

  public int getRowCount() {
    return noeTableModel.getRowCount();
  }

  public int[] getMeasurementCountPlusIndices(int row) {
    return noeTableModel.getMeasurementCountPlusIndices(row);
  }

  class NoeListWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      close();
    }
  }

  class NoeTableModel extends AbstractTableModel {

    final String[] noeHeaders = { GT.$("Distance/A"), GT.$("NOE"), "Atom 1",
        "Atom 2", "Exp NOE", "Exp Distance/A" };

    @Override
    public String getColumnName(int col) {
      if (col < 4) {
        return noeHeaders[col];
      }
      if (lexpNoes) {
        return noeHeaders[4];
      }
      return noeHeaders[5];
    }

    @Override
    public int getRowCount() {
      
      natomsPerModel = nmrPanel.getFrameAtomCount();
      int rowCount = 0;

      for (int i = 0; i < viewer.getMeasurementCount(); i++) {

//        int[] countPlusIndices = viewer.getMeasurementCountPlusIndices(i);
        if (checkNoe(i)) {
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
      //Object dum = null;
      //return dum.getClass();
    }

    public int[] getMeasurementCountPlusIndices(int row) {
      int vRow = getViewerRow(row);
      int[] countPlusIndices = viewer.getMeasurementCountPlusIndices(vRow);
      return countPlusIndices;
    }

    @Override
    public Object getValueAt(int row, int col) {
      // Always check that the CDK conversion is current  
      if (!molCDKuptodate) {
        addMol();
      }
      // Need to convert noeTable index to viewer index
      int vRow = getViewerRow(row);
      int[] m = viewer.getMeasurementCountPlusIndices(vRow);

      int a1 = m[1];
      int a2 = m[2];

      double noeNP = calcProps.getJmolNoe(a1, a2);
      double noeNPref = 1.0;
      
      int base = viewer.getFrameBase(a1);
      
      int f1 = a1 - base;
      int f2 = a2 - base;
      

      // Normalise to selected single measurment
      if (lrefSingle) {
        if (noeNPrefIndices[0] != -1 && noeNPrefIndices[1] != -1) {
          noeNPref = calcProps.getJmolNoe(noeNPrefIndices[0], noeNPrefIndices[1]);
        }
      } else {
        // Normalise to diagonal
        noeNPref = noeNP;
      }
      noeNP /= noeNPref;
      String expNOE = expNoes[f1][f2];
      String expDist = expDists[f1][f2];

      double dExpNOE;
      try {
        dExpNOE = (Double.valueOf(expNOE)).doubleValue();
        if (lrefSingle) {
          dExpNOE /= noeExprefValue;
        }
        expNOE = Measure.formatExpNOE(dExpNOE);
      } catch (Exception e) {
        //return expNOE;
      }

      double distNP = calcProps.getJmolDistance(a1, a2);
      MeasureNoe measure = new MeasureNoe(expNOE, noeNP);
      MeasureDist measured = new MeasureDist(expDist, distNP);

      if (col == 0) {
        /*double d = calcProps.calcDistance(numAtom1, numAtom2);
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(d);*/
        return measured;
      }
      if (col == 1) {
        return measure;
      }
      if (col == 4) {
        if (lexpNoes) {
          return measure.getExpValue();
        }
        return measured.getExpValue();
      }
      if (col >= m[0] + 2)
        return null;

      int atomIndex = m[col - 1];
      String name = labelArray[atomIndex];
      String reserve = "" + viewer.getAtomNumber(atomIndex) + " "
          + viewer.getAtomName(atomIndex);
      return (name == null || name.trim().length() == 0 ? reserve : name.trim());
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
        int[] m = viewer.getMeasurementCountPlusIndices(vRow);
        if (lexpNoes) {
          expNoes[m[1]][m[2]] = val;
          expNoes[m[2]][m[1]] = val;
        } else {
          expDists[m[1]][m[2]] = val;
          expDists[m[2]][m[1]] = val;
        }
        //fireTableDataChanged();
        updateNoeTableData();
      }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
      return (col == 4);
    }
  }

  private void calcFrameDelta() {
    int n = noeTableModel.getRowCount();
    double frameDelta = 0.0;
    for (int i = 0; i < n; i++) {

      // Use diff set up for frame colouring

      if (lexpNoes) {
        frameDelta += ((Measure) (noeTableModel.getValueAt(i, 1))).getDiff();
      } else {
        frameDelta += ((Measure) (noeTableModel.getValueAt(i, 0))).getDiff();
      }

    }
    frameDeltaDisplay.setFrameDeltaNoe(frameDelta);
  }

  public void updateTables() {
    updateNoeTableData();
  }

  //  private double getnmfromString(String sDist) {
  //    String[] splitDist = sDist.split(" ");
  //    Double dDist = null;
  //    if (splitDist[1].equals("nm")) {
  //       dDist = new Double(splitDist[0]);
  //    }
  //    else {           
  //      System.out.println("Distance not in nm");
  //    }
  //    return dDist.doubleValue();
  //  }

  protected int getViewerRow(int i) {
    return nmrPanel.getViewerRow(i, NMRCalculation.MODE_CALC_NOE);
  }

  boolean checkNoe(int i) {
    return (nmrPanel.getViewerMeasurement(i, NMRCalculation.MODE_CALC_NOE) != null); 
                   
    
//    
//                   int[] countPlusIndices = 
//    
//    if (countPlusIndices[0] == 2 && countPlusIndices[1] < natomsPerModel) {
//      org.jmol.modelset.Atom ja = viewer.getAtomAt(countPlusIndices[1]);
//      org.jmol.modelset.Atom jb = viewer.getAtomAt(countPlusIndices[2]);
//      if ((ja.getElementSymbol()).equals("H") 
//          && (jb.getElementSymbol()).equals("H")) {
//        return true;
//      }
//    }
//    return false;
  }

  /**
   * TODO This assumes we have the same structure in each model.
   * 
   * @return average number of atoms per model ??
   */
  int calcNatomsPerModel() {
    int nmodel = viewer.getModelCount();
    return (nmodel > 0 ? viewer.getAtomCount() / nmodel : 0);
  }

  /* This should only be called once the molecule data has been read in */
  public void addMol() {
    calcProps = nmrPanel.getDistanceJMolecule(null, labelArray, true);
    calcProps.setCorrelationTimeTauPS(tau);
    calcProps.setMixingTimeSec(tMix);
    calcProps.setNMRfreqMHz(freq);
    calcProps.setCutoffAng(cutoff);
    calcProps.setRhoStar(rhoStar);
    calcProps.setNoesy(noesy);
    calcProps.calcNOEs();
    molCDKuptodate = true;

  }

  public void setmolCDKuptodate(boolean value) {
    this.molCDKuptodate = value;
  }

  public void allocateLabelArray(int numAtoms) {
    this.labelArray = new String[numAtoms];
  }

  public void allocateExpNoes(int numAtoms) {
    expNoes = new String[numAtoms][numAtoms];
    expDists = new String[numAtoms][numAtoms];
  }

  public String getExpNoe(int i, int j) {
    return expNoes[i][j];
  }

  public String getExpDist(int i, int j) {
    return expDists[i][j];
  }

  public void setExpNoe(String value, int i, int j) {
    if (value.trim().length() == 0) {
      value = null;
    }
    expNoes[i][j] = value;
    expNoes[j][i] = value;
  }

  public void setExpDist(String value, int i, int j) {
    if (value.trim().length() == 0) {
      value = null;
    }
    expDists[i][j] = value;
    expDists[j][i] = value;
  }

  public void setLabelArray(String[] labelArray) {
    this.labelArray = labelArray;
  }

  /**
   * set the correlation time to be used in the NOE calculation
   * 
   * @param t
   *        the correlation time in seconds. Typical value would be 80E-12.
   */
  public void setCorrelationTime(double t) {
    tau = t;
    setmolCDKuptodate(false);
  }

  /**
   * sets the mixing time for the NOE experiment
   * 
   * @param t
   *        the mixing time in seconds. Typically 0.5-1.5 seconds for small
   *        molecules
   */
  public void setMixingTime(double t) {
    tMix = t;
    setmolCDKuptodate(false);

  }

  /**
   * set the NMR frequency for the NOE simulation
   * 
   * @param f
   *        the frequency in MHz
   */
  public void setNMRfreq(double f) {
    freq = f;
    setmolCDKuptodate(false);
  }

  /**
   * sets the cutoff distance beyond which atom interactions are not considered
   * 
   * @param c
   *        the cutoff distance in Angstroms
   */
  public void setCutoff(double c) {
    cutoff = c;
    setmolCDKuptodate(false);
  }

  public void setRhoStar(double c) {
    rhoStar = c;
    setmolCDKuptodate(false);
  }

  public void setNoesy(boolean b) {
    noesy = b;
    setmolCDKuptodate(false);
  }

  public void setlrefSingle(boolean l) {
    lrefSingle = l;
  }

  /**
   * get the correlation time in seconds
   * 
   * @return the correlation time in seconds
   */
  public double getCorrelationTime() {
    return tau;
  }

  /**
   * get the mixing time
   * 
   * @return the mixing time in seconds
   */
  public double getMixingTime() {
    return tMix;
  }

  /**
   * gets the NMR frequency
   * 
   * @return the NMR frequency in MHz
   */
  public double getNMRfreq() {
    return freq;
  }

  /**
   * get the cutoff distance
   * 
   * @return the cutoff in Angstroms
   */
  public double getCutoff() {
    return cutoff;
  }

  public double getRhoStar() {
    return rhoStar;
  }

  public boolean getNoesy() {
    return noesy;
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

  public int[] getnoeNPrefIndices() {
    return this.noeNPrefIndices;
  }

  public void setNoeNPrefIndices(int[] noeNPrefIndices) {
    this.noeNPrefIndices = noeNPrefIndices;
  }

  public double getNoeExprefValue() {
    return this.noeExprefValue;
  }

  public void setNoeExprefValue(double value) {
    this.noeExprefValue = value;
  }

  public void setFrameDeltaDisplay(FrameDeltaDisplay frameDeltaDisplay) {
    this.frameDeltaDisplay = frameDeltaDisplay;
  }

  public boolean getlexpNoes() {
    return this.lexpNoes;
  }

}
