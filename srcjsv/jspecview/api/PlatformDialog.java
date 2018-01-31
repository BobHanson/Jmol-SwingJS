package jspecview.api;

public interface PlatformDialog {

	void setIntLocation(int[] loc);

	Object addButton(String name, String title);

	Object addCheckBox(String name, String title, int level, boolean isSelected);

	Object addTextField(String name, String label, String value,
			String units, String defaultValue, boolean visible);

  Object addSelectOption(String name, String label,
			String[] info, int iPt, boolean visible);
	
	void createTable(Object[][] data, String[] header, int[] widths);

	void setPreferredSize(int width, int height);

	void endLayout();

	void startLayout();

	void dispose();

	void pack();
	
	
	//// get/set methods ////

	void setEnabled(Object btn, boolean b);

	String getText(Object txt);

	void setText(Object showHideButton, String label);

	Object getSelectedItem(Object combo);

	int getSelectedIndex(Object combo1);

	void setSelectedIndex(Object combo, int i);

	boolean isVisible();

	void setVisible(boolean visible);
	
	void setFocus(boolean tf);

	boolean isSelected(Object chkbox);

	void setSelected(Object chkbox, boolean b);

	void setCellSelectionEnabled(boolean enabled);

	void setTitle(String title);

	void selectTableRow(int i);

	void repaint();

	
}
