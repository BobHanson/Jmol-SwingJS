package jspecview.js2d;

import java.util.Hashtable;
import java.util.Map;

import javax.swing.SwingConstants;

import jspecview.api.PlatformDialog;
import javajs.awt.Dimension;
import javajs.awt.Color;
import javajs.swing.FlowLayout;
import javajs.swing.GridBagConstraints;
import javajs.swing.GridBagLayout;
import javajs.swing.Insets;
import javajs.swing.JButton;
import javajs.swing.JCheckBox;
import javajs.swing.JComboBox;
import javajs.swing.JComponent;
import javajs.swing.JDialog;
import javajs.swing.JLabel;
import javajs.swing.JPanel;
import javajs.swing.JScrollPane;
import javajs.swing.JSplitPane;
import javajs.swing.JTable;
import javajs.swing.JTextField;
import javajs.swing.ListSelectionModel;
import jspecview.common.Annotation.AType;
import jspecview.dialog.DialogManager;
import jspecview.dialog.JSVDialog;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class JsDialog extends JDialog implements PlatformDialog {

	protected String optionKey;
	protected String registryKey;

	protected Map<String, Object> options;
	protected DialogManager manager;

  private AType type;
	protected JPanel leftPanel;
	private JSplitPane mainSplitPane;

	private JPanel rightPanel;
	private JPanel thisPanel;
	private JTable dataTable;
	private int iRow;
	private boolean haveColors;


	protected boolean tableCellAlignLeft;

	private boolean haveTwoPanels = true;

	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	
	private Insets panelInsets = new Insets(0, 0, 2, 2);
	protected int selectedRow = -1;
	
	public JsDialog(DialogManager manager, JSVDialog jsvDialog, String registryKey) {
		super();
		defaultHeight = 350;
  	this.manager = manager;
		this.registryKey = registryKey;
		optionKey = jsvDialog.optionKey;
		type = jsvDialog.getAType();
		options = jsvDialog.options;
		if (options == null)
			options = new Hashtable<String, Object>();
		getContentPane().setBackground(Color.get3(230, 230, 230));
		toFront(); 
	}
	
	public void onFocus() {
		toFront();
		}
		
	@Override
	public void setFocus(boolean tf) {
		//setBackground(tf ? Color.BLUE : Color.GRAY);
		if (tf) {
			//requestFocus();
			toFront();
		}
	}
		

	@Override
	public Object addButton(String name, String text) {
		JButton	btn = new JButton();
		btn.setPreferredSize(new Dimension(120, 25));
		btn.setText(text);
		btn.setName(registryKey + "/" + name);
		btn.addActionListener(manager);
		thisPanel.add(btn, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
		return btn;
	}

	@Override
	public void dispose() {
		// just identifying when this happens. 
		super.dispose();
	}
	@Override
	public Object addCheckBox(String name, String title, int level,
			boolean isSelected) {
		if (name == null) {
			// reset row counter
			iRow = 0;
			thisPanel = rightPanel;
			return null;
		}
  	JCheckBox cb = new JCheckBox();
  	cb.setSelected(isSelected);
  	cb.setText(title);
  	cb.setName(registryKey + "/" + name);
		cb.addActionListener(manager);
    Insets insets = new Insets(0, 20 * level, 2, 2);
    thisPanel.add(cb, new GridBagConstraints(0, iRow++, 1, 1, 0.0, 0.0,
    		GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    return cb;
	}

	private void addPanelLine(String name, String label, JComponent obj,
			String units) {
		thisPanel.add(new JLabel(label == null ? name : label),
				new GridBagConstraints(0, iRow, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, panelInsets, 0, 0));
		if (units == null) {
			thisPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, panelInsets, 0, 0));
		} else {
			thisPanel.add(obj, new GridBagConstraints(1, iRow, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, panelInsets, 0, 0));
			thisPanel.add(new JLabel(units), new GridBagConstraints(2, iRow, 1, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, panelInsets,
					0, 0));
		}
		iRow++;
	}

	@Override
	public Object addSelectOption(String name, String label,
			String[] info, int iPt, boolean visible) {
		JComboBox<String> combo = new JComboBox<String>(info);
		combo.setSelectedIndex(iPt);
		combo.setName(registryKey + "/" + name);
		if (visible) {
			combo.addActionListener(manager);
			addPanelLine(name, label, combo, null);
	  }
		return combo;
	}

	//// get/set methods ////
	
	@Override
	public Object addTextField(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		String key = optionKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField(value);
		obj.setName(registryKey + "/" + name);
		if (visible) {
			obj.setPreferredSize(new Dimension(45, 15));
			obj.addActionListener(manager);
  		addPanelLine(name, label, obj, units);
		}
		return obj;
	}

	@Override
	public void createTable(Object[][] data, String[] header, int[] widths) {
		try {
			JScrollPane scrollPane = new JScrollPane(
					dataTable = getDataTable(data, header,
							widths, (leftPanel == null ? defaultHeight  : leftPanel.getHeight() - 50)));
			if (mainSplitPane == null) {
				getContentPane().add(scrollPane);				 
			} else {
				mainSplitPane.setRightComponent(scrollPane);
			}
		} catch (Exception e) {
			// not perfect.
		}
		validate();
		repaint();
	}


	@Override
	public void endLayout() {
		getContentPane().removeAll();
		getContentPane().add(mainSplitPane);
		pack();
	}

	private synchronized JTable getDataTable(Object[][] data,
			String[] columnNames, int[] columnWidths, int height) {
		selectedRow = -1;
		DialogTableModel tableModel = new DialogTableModel(columnNames, data,
				!haveColors, tableCellAlignLeft);
		JTable table = new JTable(tableModel);
		// table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// if (haveColors)
		// table.setDefaultRenderer(JSVColor.class, new ColorRenderer());
		// table.setDefaultRenderer(String.class, new TitleRenderer());
		// table.setCellSelectionEnabled(true);
		ListSelectionModel selector = table.getSelectionModel();
		selector.addListSelectionListener(manager);
		manager.registerSelector(registryKey + "/ROW", selector);
		selector = table.getColumnModel().getSelectionModel();
		selector.addListSelectionListener(manager);
		manager.registerSelector(registryKey + "/COLUMN", selector);
		int n = 0;
		for (int i = 0; i < columnNames.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
			n += columnWidths[i];
		}
		//table.setPreferredScrollableViewportSize(new Dimension(n, height));
		return table;
	}

	@Override
	public int getSelectedIndex(Object c) {
		return ((JComboBox<?>) c).getSelectedIndex();
	}

	@Override
	public Object getSelectedItem(Object combo) {
		return ((JComboBox<?>)combo).getSelectedItem();
	}

	@Override
	public String getText(Object o) {
		return ((JComponent) o).getText();	
	}

	
	@Override
	public boolean isSelected(Object chkbox) {
		return ((JCheckBox) chkbox).isSelected();
	}
	
	@Override
	public void selectTableRow(int i) {
		selectedRow = i;
		dataTable.clearSelection();
		if (selectedRow >= 0) {
			dataTable.setRowSelectionAllowed(true);
			dataTable.setRowSelectionInterval(selectedRow, selectedRow + 1);
			repaint();
		}
	}

	@Override
	public void setCellSelectionEnabled(boolean enabled) {
		dataTable.setCellSelectionEnabled(enabled);
	}

  //// Table-related methods ////
	
	
	@Override
	public void setEnabled(Object btn, boolean b) {
		((JComponent) btn).setEnabled(b);
	}

	@Override
	public void setIntLocation(int[] loc) {
		Dimension d = new Dimension(0, 0);
		/**
		 * @j2sNative
		 * 
		 * SwingController.getScreenDimensions(d);
		 * 
		 * 
		 */
		{
		}
		
		loc[0] = Math.min(d.width - 50, loc[0]);
		loc[1] = Math.min(d.height - 50, loc[1]);
		setLocation(loc);
	}

	@Override
	public void setPreferredSize(int width, int height) {
		setPreferredSize(new Dimension(width, height));
	}

	@Override
	public void setSelected(Object chkbox, boolean b) {
		((JCheckBox) chkbox).setSelected(b);
	}

	@Override
	public void setSelectedIndex(Object combo, int i) {
		((JComboBox<?>) combo).setSelectedIndex(i);
	}

	@Override
	public void setText(Object o, String text) {
		((JComponent) o).setText(text);
	}

	@Override
	public void startLayout() {
		setPreferredSize(new Dimension(600, 370)); // golden ratio
    getContentPane().removeAll();
		thisPanel = rightPanel = new JPanel(new FlowLayout());
		switch (type) {
		case Integration:
		case Measurements:
		case PeakList:
		case NONE:
			break;
		case OverlayLegend:
			tableCellAlignLeft = true;
			haveColors = true;
			haveTwoPanels = false;
			break;
		case Views:
			rightPanel = new JPanel(new GridBagLayout());
		}
		if (haveTwoPanels) {
			thisPanel = leftPanel = new JPanel(new GridBagLayout());
			leftPanel.setMinimumSize(new Dimension(200, 300));
			mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			//mainSplitPane.setOneTouchExpandable(true);
			//mainSplitPane.setResizeWeight(0);
			mainSplitPane.setLeftComponent(leftPanel);
			mainSplitPane.setRightComponent(new JScrollPane(rightPanel));
		}
			
	}

	protected int getColumnCentering(int column) {
		return tableCellAlignLeft ? SwingConstants.LEFT : column == 0 ? SwingConstants.CENTER
				: SwingConstants.RIGHT;
	}


}
