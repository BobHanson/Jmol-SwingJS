/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.unused;


public abstract class AwtDialogAnnotation  {
//
//	private static final long serialVersionUID = 1L;
//
//	private JPanel leftPanel, rightPanel;
//	private JSplitPane mainSplitPane;
//	private JButton showHideButton; // text changeable
//	private JTable dataTable;
//	ListSelectionModel columnSelector;
//	private int tableRCflag = 0;
//
//	protected JComboBox<String> combo1; // measurement listing
//	protected JComboBox<String> chkbox1; // peaks
//	protected JTextField txt1; // peaks and int
//	protected JTextField txt2; // int
//
//	private boolean addClearBtn, addCombo1;
//
//	/**
//	 * Constructor for AwtIntegralListDialog, AwtMeasurementListDialog, and
//	 * AwtPeakListDialog
//	 * 
//	 * @param title
//	 * @param viewer
//	 * @param spec
//	 *          the parent panel
//	 * @param type
//	 */
//	protected AwtDialogAnnotation(String title, JSViewer viewer,
//			JDXSpectrum spec, AType type) {
//		super(null, title, false);
//		addCombo1 = type.equals(AType.Measurements);
//		addClearBtn = !type.equals(AType.PeakList);
//
//		setResizable(true);
//		params = new DialogParams(type, this, viewer, spec, new PParameters(
//				"MeasurementData"));
//		params.setup();
//		initDialog();
//		pack();
//		setVisible(true);
//	}
//
//	// //// frame construction ////////
//
//	private void initDialog() {
//		leftPanel = new JPanel(new GridBagLayout());
//		leftPanel.setMinimumSize(new Dimension(200, 300));
//		DialogManager dialogHelper = new DialogManager().set(params.thisKey,
//				params.options, leftPanel, new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						if (e.getActionCommand().equals("Units")) {
//							params.setPrecision(combo1.getSelectedIndex());
//						} else if (e.getSource() instanceof JTextField) {
//							params.eventApply();
//						}
//					}
//				});
//		addUniqueControls(dialogHelper);
//		params.getUnitOptions();
//		if (addCombo1)
//			combo1 = dialogHelper.addSelectOption("Units", null, params.unitOptions,
//					params.unitPtr.intValue(), params.addUnits);
//		// txtFontSize = ((DialogHelper dialogHelper)).addInputOption("FontSize",
//		// "Font Size", null, null, "10");
//
//		dialogHelper.addButton("Apply", new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				params.eventApply();
//			}
//		});
//
//		showHideButton = dialogHelper.addButton("Show", new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				JButton b = (JButton) e.getSource();
//				params.eventShowHide(b.getText().equals("Show"));
//			}
//		});
//
//		if (addClearBtn)
//			dialogHelper.addButton("Clear", new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					params.clear();
//				}
//			});
//
//		dialogHelper = null;
//
//		rightPanel = new JPanel();
//		JScrollPane scrollPane = new JScrollPane(rightPanel);
//
//		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
//		mainSplitPane.setOneTouchExpandable(true);
//		mainSplitPane.setResizeWeight(0);
//		mainSplitPane.setRightComponent(scrollPane);
//		mainSplitPane.setLeftComponent(leftPanel);
//		setPreferredSize(new Dimension(600, 370)); // golden ratio
//		getContentPane().removeAll();
//		getContentPane().add(mainSplitPane);
//		checkEnables();
//	}
//
//	/**
//	 * @param dialogHelper
//	 */
//	protected void addUniqueControls(DialogManager dialogHelper) {
//		// int and peak only
//	}
//
//	// ///// action interfaces /////////
//
//	synchronized public void valueChanged(ListSelectionEvent e) {
//		try {
//			ListSelectionModel lsm = (ListSelectionModel) e.getSource();
//			if (e.getValueIsAdjusting()) {
//				if (lsm == columnSelector) {
//					params.iColSelected = lsm.getLeadSelectionIndex();
//					tableRCflag = 1;
//				} else {
//					params.iRowSelected = lsm.getLeadSelectionIndex();
//					tableRCflag = 2;
//				}
//				return;
//			}
//			if ((lsm == columnSelector) != (tableRCflag == 1))
//				return;
//			params.tableCellSelect(params.iRowSelected, params.iColSelected);
//		} catch (Exception ee) {
//			// ignore
//		}
//	}
//
//	// /////// general interface to the outside world ////////
//
//	public AType getAType() {
//		return params.thisType;
//	}
//
//	public void setSpecShift(double dx) {
//		params.setSpecShift(dx);
//	}
//
//	public MeasurementData getData() {
//		return params.getData();
//	}
//
//	public void setFields() {
//		params.setFields();
//	}
//
//	public String getKey() {
//		return params.key;
//	}
//
//	public void setKey(String key) {
//		params.key = key;
//	}
//
//	public JDXSpectrum getSpectrum() {
//		return params.spec;
//	}
//
//	public boolean getState() {
//		return params.isON;
//	}
//
//	public void setState(boolean b) {
//		params.isON = b;
//	}
//
//	public void update(Coordinate clicked) {
//		params.update(clicked);
//	}
//
//	// ////// interface to DialogParams////////
//
//	public void checkEnables() {
//		boolean isShow = params.checkVisible();
//		showHideButton.setText(isShow ? "Hide" : "Show");
//	}
//
//	public void createTable(String[][] data, String[] header, int[] widths) {
//		try {
//			params.tableData = data;
//			rightPanel.removeAll();
//			JScrollPane scrollPane = new JScrollPane(
//					dataTable = new DialogManager().getDataTable(this, data, header,
//							widths, leftPanel.getHeight() - 50));
//			mainSplitPane.setRightComponent(scrollPane);
//		} catch (Exception e) {
//			// not perfect.
//		}
//		validate();
//		repaint();
//	}
//
//	public void setTableSelectionEnabled(boolean enabled) {
//		dataTable.setCellSelectionEnabled(enabled);
//	}
//
//	public void setTableSelectionInterval(int i, int j) {
//		dataTable.getSelectionModel().setSelectionInterval(i, j);
//	}
//
//	public Parameters getParameters() {
//		return params.myParams;
//	}
//
//	public void showMessage(String msg) {
//		JOptionPane.showMessageDialog(this, msg);
//	}
//
//	public void setThreshold(double y) {
//		txt1.setText(params.getThreasholdText(y));
//	}
//
//	public void setComboSelected(int i) {
//		chkbox1.setSelectedIndex(i);
//	}
//
//	public void applyFromFields() {
//		params.apply(null);
//	}
//
//	public void setParamsFromFields() {
//		params.setParams(null);
//	}
//
//	public void loadDataFromFields() {
//		params.loadData(null);
//	}
//
//	public void tableCellSelectedEvent(int iRow, int iCol) {
//		params.tableCellSelect(iRow, iCol);
//	}
//
//	public AnnotationDialog reEnable() {
//		params.reEnable();
//		return this;
//	}
//	
//	public void setData(AnnotationData data) {
//		params.setData(data);
//	}
//	
//	// ///// unused but required:
//
//	public void windowActivated(WindowEvent arg0) {
//		// n/a
//	}
//
//	public void windowClosed(WindowEvent arg0) {
//		// n/a
//	}
//
//	public void windowClosing(WindowEvent arg0) {
//		params.done();
//	}
//
//	public void windowDeactivated(WindowEvent arg0) {
//		// n/a
//	}
//
//	public void windowDeiconified(WindowEvent arg0) {
//		// n/a
//	}
//
//	public void windowIconified(WindowEvent arg0) {
//		// n/a
//	}
//
//	public void windowOpened(WindowEvent arg0) {
//		// n/a
//	}
//
//	public static AnnotationDialog get(AType type, JDXSpectrum spec,
//			JSViewer viewer) {
//		switch (type) {
//		case Integration:
//			return new AwtDialogIntegrals("Integration for " + spec, viewer, spec);
//		case Measurements:
//			return new AwtDialogMeasurements("Measurements for " + spec, viewer, spec);
//		case PeakList:
//			return new AwtDialogPeakList("Peak List for " + spec, viewer, spec);
//		default:
//			return null;
//		}
//	}

}
