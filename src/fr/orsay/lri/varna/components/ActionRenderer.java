
package fr.orsay.lri.varna.components;


import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

public class ActionRenderer extends JButton implements TableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ActionRenderer() {
	}

	public Component getTableCellRendererComponent(JTable table, Object button,
			boolean isSelected, boolean hasFocus, int row, int column) {
		this.setText(button.toString());
		return this;
	}
}

