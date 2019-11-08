package org.jmol.awtjs.swing;

import org.jmol.awtjs.swing.BorderLayout;
import org.jmol.awtjs.swing.LayoutManager;
import javajs.util.SB;

public class JPanel extends JComponent {

	//private LayoutManager layoutManager;

	private Grid grid;

	private int nElements;
	private JComponent last;
	
	
	/**
	 * @param manager  ignored. we just use the layout designations with a grid
	 */
	public JPanel(LayoutManager manager) {
		super("JP");
		//this.layoutManager = manager;
		grid = new Grid(10,10);
	}

	public void add(JComponent btn, Object c) {
		last = (++nElements == 1 ? btn : null);
		if (c instanceof String) {
			if (c.equals(BorderLayout.NORTH))
				c = new GridBagConstraints(0, 0, 3, 1, 0, 0, GridBagConstraints.CENTER,
						0, null, 0, 0);
			else if (c.equals(BorderLayout.SOUTH))
				c = new GridBagConstraints(0, 2, 3, 1, 0, 0, GridBagConstraints.CENTER,
						0, null, 0, 0);
			else if (c.equals(BorderLayout.EAST))
				c = new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.EAST,
						0, null, 0, 0);
			else if (c.equals(BorderLayout.WEST))
				c = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST,
						0, null, 0, 0);
			else
				c = new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,
						0, null, 0, 0);
		}
		grid.add(btn, (GridBagConstraints) c);
	}
	
	@Override
	public String toHTML() {
		if (last != null) {
			// only one element
			grid = new Grid(1, 1);
			grid.add(last, new GridBagConstraints(0, 0, 1, 1, 0, 0,
					GridBagConstraints.CENTER, 0, null, 0, 0));
			last = null;
		}
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JPanel' style='"
				+ getCSSstyle(100, 100) + "'>\n");
		sb.append("\n<span id='" + id + "_minimizer' style='width:" + minWidth
				+ "px;height:" + minHeight + "px;'>");
		sb.append(grid.toHTML(id));
		sb.append("</span>");
		sb.append("\n</div>\n");
		return sb.toString();
	}
}
