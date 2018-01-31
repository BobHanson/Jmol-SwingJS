package jspecview.java;

import javax.swing.table.AbstractTableModel;

/**
 * The Table Model for Legend
 */
class DialogTableModel extends AbstractTableModel {
	/**
   * 
   */
	private static final long serialVersionUID = 1L;
	String[] columnNames;
	Object[][] data;
	boolean asString;

	DialogTableModel(String[] columnNames, Object[][] data, boolean asString) {
		this.columnNames = columnNames;
		this.data = data;
		this.asString = asString;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public Object getValueAt(int row, int col) {
		Object o = data[row][col];
		return (asString ? " " + o + " " : o);
	}
	
  @Override
  public Class<?> getColumnClass(int c) {
      return getValueAt(0, c).getClass();
  }

}