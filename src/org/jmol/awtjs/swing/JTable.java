package javajs.swing;

import javajs.awt.Dimension;
import javajs.util.BS;
import javajs.util.SB;


public class JTable extends JComponent implements ListSelectionModel, ColumnSelectionModel {

	private AbstractTableModel tableModel;
	private BS bsSelectedCells;
	private BS bsSelectedRows;
	
	boolean rowSelectionAllowed;
	boolean cellSelectionEnabled;
  Object selectionListener;

	public JTable(AbstractTableModel tableModel) {
		super("JT");
		this.tableModel = tableModel;
		this.bsSelectedCells = new BS();
		this.bsSelectedRows = new BS();
	}

	@Override
  public ListSelectionModel getSelectionModel() {
		return this;
	}

	public ColumnSelectionModel getColumnModel() {
		return this;
	}

	public void setPreferredScrollableViewportSize(Dimension dimension) {
		this.width = dimension.width;
		this.height = dimension.height;
	}

	public void clearSelection() {
		bsSelectedCells.clearAll();
		bsSelectedRows.clearAll();
	}

	public void setRowSelectionAllowed(boolean b) {
		rowSelectionAllowed = b;
	}

	public void setRowSelectionInterval(int i, int j) {
		bsSelectedRows.clearAll();
		bsSelectedRows.setBits(i, j);
		bsSelectedCells.clearAll();
	}

	public void setCellSelectionEnabled(boolean enabled) {
		cellSelectionEnabled = enabled;
	}

	/** 
	 * It will be the function of the JavaScript on the 
	 * page to do with selectionListener what is desired.
	 * 
	 * @param listener 
	 * 
	 */
	@Override
  public void addListSelectionListener(Object listener) {
		selectionListener = listener;
	}

	@Override
  public TableColumn getColumn(int i) {
		return tableModel.getColumn(i);
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("\n<table id='" + id + "_table' class='JTable' >");
		tableModel.toHTML(sb, id, bsSelectedRows);
		sb.append("\n</table>\n");
		return sb.toString();
	}
}
