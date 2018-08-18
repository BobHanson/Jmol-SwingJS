package jspecview.js2d;


import javajs.api.GenericColor;
import javajs.util.CU;
import org.jmol.awtjs.swing.AbstractTableModel;
import org.jmol.awtjs.swing.TableColumn;
import javajs.util.BS;
import javajs.util.SB;

class DialogTableModel implements AbstractTableModel {

	String[] columnNames;
	Object[][] data;
	boolean asString;
	int[] widths;
	private int thisCol;
	private boolean tableCellAlignLeft;

	DialogTableModel(String[] columnNames, Object[][] data, boolean asString, boolean tableCellAlignLeft ) {
		this.columnNames = columnNames;
		this.data = data;
		this.asString = asString;
		this.widths = (data.length  == 0 ? new int[0] : new int[data[0].length]);
		this.tableCellAlignLeft = tableCellAlignLeft;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}
	
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		Object o = data[row][col];
		return (asString ? " " + o + " " : o);
	}

	@Override
	public TableColumn getColumn(int i) {
		thisCol = i;
		return this;		
	}
	
	@Override
	public void setPreferredWidth(int n) {
		widths[thisCol] = n;
	}

	@Override
	public void toHTML(SB sb, String id, BS selectedRows) {
		if (data == null || data[0] == null || data[0].length == 0)
			return;
		int nrows = data.length;
		int ncols = columnNames.length;
		for (int i = -1; i < nrows; i++) {
			String rowid = id + "_" + i;
			sb.append("\n<tr id='" + rowid + "' class='JTable_" + (i == -1 ? "header" : "row") + "' style='height:25px'>");
			for (int j = 0; j < ncols; j++) {
				if (i == -1)
					getCellHtml(sb, id + "_h" + j, i, j, columnNames[j], false);		
				else
					getCellHtml(sb, rowid + "_" + j, i, j, data[i][j], selectedRows.get(i));
			}
			sb.append("</tr>");
		}
	}
	
	private void getCellHtml(SB sb, String id, int iRow, int iCol, Object o, boolean isSelected) {
		String style = getCellStyle(id, iRow, iCol, o, isSelected);
		sb.append("<td id='" + id + "'" + style
				+ " onclick=SwingController.click(this)>" + o + "</td>");
	}

	/**
	 * @param id
	 * @param iRow
	 * @param iCol
	 * @param o
	 * @param isSelected 
	 * @return CSS style attribute
	 */
	private String getCellStyle(String id, int iRow, int iCol, Object o, boolean isSelected) {
		String style = "padding:1px 1px 1px 1px";
		if (iRow < 0) {
			style += ";font-weight:bold";
		} else {
			if (o instanceof GenericColor) {
				style += ";background-color:"
						+ CU.toCSSString((GenericColor) o);
			} else {
				if (asString)
					o = " " + o + " ";
				style += ";text-align:";
				if (tableCellAlignLeft)
					style += "left";
				else if (iCol == 0)
					style += "center";
				else
					style += "right";
				style += ";border:" + (isSelected ? 3 : 1) + "px solid #000";
			}
		}
		return " style='" + style + "'";
	}
}
