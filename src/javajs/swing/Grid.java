package javajs.swing;

import javajs.util.AU;
import javajs.util.SB;


public class Grid {

	private int nrows;
	private int ncols;

	private Cell[][] grid;
	private String renderer;
	

	Grid(int rows, int cols) {
		grid = new Cell[0][0];
	}

	public void add(JComponent btn, GridBagConstraints c) {
		if (c.gridx >= ncols) {
			ncols = c.gridx + 1;
			for (int i = 0; i < nrows; i++) {
				grid[i] = (Cell[]) AU.ensureLength(grid[i], ncols * 2);
			}
		}
		if (c.gridy >= nrows) {
			Cell[][] g = new Cell[c.gridy * 2 + 1][];
			for (int i = 0; i < nrows; i++)
				g[i] = grid[i];
			for (int i = g.length; --i >= nrows;)
				g[i] = new Cell[ncols * 2 + 1];
			grid = g;
			nrows = c.gridy + 1;
		}
		grid[c.gridy][c.gridx] = new Cell(btn, c);
	}

	public String toHTML(String id) {
		SB sb = new SB();
		id += "_grid";
		sb.append("\n<table id='" + id + "' class='Grid' style='width:100%;height:100%'><tr><td style='height:20%;width:20%'></td></tr>");		
		for (int i = 0; i < nrows; i++) {
			String rowid = id + "_" + i;
			sb.append("\n<tr id='" + rowid + "'><td></td>");
			for (int j = 0; j < ncols; j++)
				if (grid[i][j] != null)
					sb.append(grid[i][j].toHTML(rowid + "_" + j));
			sb.append("</tr>");
		}
		sb.append("\n<tr><td style='height:20%;width:20%'></td></tr></table>\n");
		return sb.toString();
	}
}
