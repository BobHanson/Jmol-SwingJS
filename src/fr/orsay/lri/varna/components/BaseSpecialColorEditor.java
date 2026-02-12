
package fr.orsay.lri.varna.components;


import java.awt.Color;
import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import fr.orsay.lri.varna.controlers.ControleurBaseSpecialColorEditor;
import fr.orsay.lri.varna.views.VueBases;

public class BaseSpecialColorEditor extends AbstractCellEditor implements
		TableCellEditor {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Color currentColor;
	private JButton button;
	private JColorChooser colorChooser;
	private JDialog dialog;
	protected static final String EDIT = "edit";
	private VueBases _vueBases;
	private ControleurBaseSpecialColorEditor _controleurSpecialColorEditor;

	public BaseSpecialColorEditor(VueBases vueBases) {
		// Set up the editor (from the table's point of view),
		// which is a button.
		// This button brings up the color chooser dialog,
		// which is the editor from the user's point of view.
		button = new JButton();
		button.setActionCommand(EDIT);
		_controleurSpecialColorEditor = new ControleurBaseSpecialColorEditor(this);
		button.addActionListener(_controleurSpecialColorEditor);
		button.setBorderPainted(false);
		fireEditingStopped();
		_vueBases = vueBases;

		// Set up the dialog that the button brings up.
		colorChooser = new JColorChooser();
		dialog = JColorChooser.createDialog(button, "Pick a Color", true, // modal
				colorChooser, _controleurSpecialColorEditor, // OK button
				// handler
				null); // no CANCEL button handler
	}

	// Implement the one CellEditor method that AbstractCellEditor doesn't.
	public Object getCellEditorValue() {
		return currentColor;
	}

	// Implement the one method defined by TableCellEditor.
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		currentColor = (Color) value;
		return button;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public Color getCurrentColor() {
		return currentColor;
	}

	public JButton getButton() {
		return button;
	}

	public JColorChooser getColorChooser() {
		return colorChooser;
	}

	public JDialog getDialog() {
		return dialog;
	}

	public static String getEDIT() {
		return EDIT;
	}

	public VueBases get_vueBases() {
		return _vueBases;
	}

	public ControleurBaseSpecialColorEditor get_controleurSpecialColorEditor() {
		return _controleurSpecialColorEditor;
	}

	public void setCurrentColor(Color currentColor) {
		this.currentColor = currentColor;
	}

	public void callFireEditingStopped() {
		fireEditingStopped();
	}
}
