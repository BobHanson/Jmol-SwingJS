package jspecview.js2d;

import org.jmol.awtjs.swing.JComponent;
import org.jmol.awtjs.swing.JEditorPane;
import org.jmol.awtjs.swing.JDialog;
import org.jmol.awtjs.swing.JLabel;
import org.jmol.awtjs.swing.JScrollPane;
import org.jmol.awtjs.swing.JTable;

import java.awt.Dimension;
import javajs.util.PT;

import jspecview.api.JSVPanel;
import jspecview.api.PlatformDialog;
import jspecview.common.Spectrum;
import jspecview.dialog.JSVDialog;
import jspecview.dialog.DialogManager;

/**
 * A DialogManager for JavaScript. 
 * 
 * @author hansonr
 *
 */
public class JsDialogManager extends DialogManager {

	public JsDialogManager() {
		// for reflection
	}

	@Override
	public PlatformDialog getDialog(JSVDialog jsvDialog) {
		return new JsDialog(this, jsvDialog, registerDialog(jsvDialog));
	}

	@Override
	public String getDialogInput(Object parentComponent, String phrase,
			String title, int msgType, Object icon, Object[] objects,
			String defaultStr) {
		/**
		 * @j2sNative
		 * 
		 * return prompt(phrase, defaultStr);
		 */
		{
		return null;
		}
	}

	@Override
	public void showMessageDialog(Object parentComponent, String msg,
			String title, int msgType) {
		/**
		 * @j2sNative
		 * 
		 *            alert(msg);
		 */
		{

		}
	}

	@Override
	public int[] getLocationOnScreen(Object component) {
		// TODO Auto-generated method stub
		return new int[2];
	}

	@Override
	public int getOptionFromDialog(Object frame, String[] items, JSVPanel jsvp,
			String dialogName, String labelName) {
    // for export and others
	  return vwr.html5Applet.getOption(items, dialogName, labelName);
	}

	/**
	 * Looks a lot like Swing, right? :)
	 * 
	 */
	@Override
	public void showProperties(Object frame, Spectrum spectrum) {
		JDialog dialog = new JDialog();// no manager needed here
		dialog.setTitle("Header Information");
		Object[][] rowData = spectrum.getHeaderRowDataAsArray();
		String[] columnNames = { "Label", "Description" };
		DialogTableModel tableModel = new DialogTableModel(columnNames, rowData,
				false, true);
		JTable table = new JTable(tableModel);
		table.setPreferredScrollableViewportSize(new Dimension(400, 195));
		JScrollPane scrollPane = new JScrollPane(table);
		dialog.getContentPane().add(scrollPane);
		dialog.pack();
		dialog.setVisible(true);
		dialog.toFront(); 
	}

	@Override
	public void showMessage(Object frame, String text, String title) {
		JDialog dialog = new JDialog();
		/**
		 * @j2sNative
		 * 
		 * dialog.manager = this; 
		 */
		{}
		dialog.setTitle(title);
		JComponent pane;
		if (text.indexOf("</div>") >= 0) {
			pane = new JLabel(text);
		} else {
			// JEditorPane sourcePane = new JEditorPane();
			// sourcePane.setText(text);
			// sourcePane.setEditable(false);
			// sourcePane.setFont(new Font(null, Font.BOLD, 12));
			// JScrollPane scrollPane = new JScrollPane(sourcePane);
			// scrollPane.setPreferredSize(new Dimension(500, 400));
			// scrollPane.setMinimumSize(new Dimension(500, 400));
			// JPanel contentPanel = new JPanel(new BorderLayout());
			// contentPanel.add(scrollPane, BorderLayout.CENTER);
			// dialog.getContentPane().add(contentPanel);

			pane = new JEditorPane();
			pane.setText(text);
		}
		dialog.getContentPane().add(pane);
		dialog.pack();
		dialog.setVisible(true);
		dialog.toFront();
	}
	
	/**
	 * Jmol.Swing.click() callback (via SwingController)
	 * @param eventId 
	 */
	public void actionPerformed(String eventId) {
		int pt = eventId.indexOf("/JT");
		if (pt >= 0) {
			int pt2 = eventId.lastIndexOf ("_");
			int pt1 = eventId.lastIndexOf ("_", pt2 - 1);
			int irow = PT.parseInt(eventId.substring(pt1 + 1, pt2));
			int icol = PT.parseInt(eventId.substring(pt2 + 1));
			processTableEvent(eventId.substring(0, pt) + "/ROWCOL", irow, icol, false);
			return;
		}
		processClick(eventId);
	}

}
