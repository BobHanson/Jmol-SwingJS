package jspecview.java;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton; //import javax.swing.JCheckBox;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jspecview.api.JSVPanel;
import jspecview.api.PlatformDialog;
import jspecview.common.Spectrum;
import jspecview.dialog.JSVDialog;
import jspecview.dialog.DialogManager;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AwtDialogManager extends DialogManager implements
		ListSelectionListener, WindowListener, ActionListener, FocusListener {

	public AwtDialogManager() {
	}

	@Override
	public PlatformDialog getDialog(JSVDialog jsvDialog) {
		return new AwtDialog(this, jsvDialog,
				registerDialog(jsvDialog));
	}

	@Override
	public String getDialogInput(Object parentComponent, String phrase,
			String title, int msgType, Object icon, Object[] objects,
			String defaultStr) {
		return (String) JOptionPane.showInputDialog((Component) parentComponent,
				phrase, title, msgType, (Icon) icon, objects, defaultStr);
	}

	@Override
	public int getOptionFromDialog(Object frame, String[] items, JSVPanel jsvp,
			String title, String label) {
		final JDialog dialog = new JDialog((JFrame) frame, title, true);
		dialog.setResizable(false);
		dialog.setSize(200, 100);
		Component panel = (Component) jsvp;
		dialog.setLocation((panel.getLocation().x + panel.getSize().width) / 2,
				(panel.getLocation().y + panel.getSize().height) / 2);
		final JComboBox<String> cb = new JComboBox<String>(items);
		Dimension d = new Dimension(120, 25);
		cb.setPreferredSize(d);
		cb.setMaximumSize(d);
		cb.setMinimumSize(d);
		JPanel p = new JPanel(new FlowLayout());
		JButton button = new JButton("OK");
		p.add(cb);
		p.add(button);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(new JLabel(label, SwingConstants.CENTER),
				BorderLayout.NORTH);
		dialog.getContentPane().add(p);
		final int ret[] = new int[] { Integer.MIN_VALUE };
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ret[0] = cb.getSelectedIndex();
				dialog.dispose();
			}
		});
		dialog.setVisible(true);
		dialog.dispose();
		return ret[0];
	}

	@Override
	public int[] getLocationOnScreen(Object component) {
		Point pt = ((Component) component).getLocationOnScreen();
		return new int[] { pt.x, pt.y };
	}

	@Override
	public void showMessageDialog(Object parentComponent, String msg,
			String title, int msgType) {
		JOptionPane.showMessageDialog((Component) parentComponent, msg, title,
				msgType);
	}

	@Override
	public void showProperties(Object frame, Spectrum spectrum) {
		Object[][] rowData = spectrum.getHeaderRowDataAsArray();
		String[] columnNames = { "Label", "Description" };
		JTable table = new JTable(rowData, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(400, 195));
		JScrollPane scrollPane = new JScrollPane(table);
		JOptionPane.showMessageDialog((Component) frame, scrollPane,
				"Header Information", PLAIN_MESSAGE);
	}

	@Override
	public void showMessage(Object frame, String text, String title) {
		JDialog dialog = new JDialog((Frame) null, title, true);
		dialog.setLayout(new BorderLayout());
		JTextArea t = new JTextArea();
		t.setText(text);
		t.setCaretPosition(0);
		t.setFont(new Font(null, Font.PLAIN, 12));
		JScrollPane scrollPane = new JScrollPane(t);
		scrollPane.setPreferredSize(new Dimension(600, 400));
		scrollPane.setMinimumSize(new Dimension(600, 400));
		dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
		dialog.pack();
		dialog.setVisible(true);
	}
	
		
	/**
	 * ListSelectionListener callback
	 */
	@Override
	synchronized public void valueChanged(ListSelectionEvent e) {
		boolean adjusting = e.getValueIsAdjusting();
		ListSelectionModel lsm = (ListSelectionModel) e.getSource();
		String selector = getSelectorName(lsm);
		if (selector == null)
			return;
		int index = lsm.getLeadSelectionIndex();
		processTableEvent(selector, index, -1, adjusting);
	}

	/**
	 * ActionListener callback
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		processClick(((Component) e.getSource()).getName());
	}

	@Override
	public void focusGained(FocusEvent e) {
		processClick(((AwtDialog) e.getSource()).registryKey + "/FOCUS");		
	}


	/**
	 * WindowListener callback
	 */
	@Override
	public void windowClosing(WindowEvent e) {
		processWindowClosing(((Component) e.getSource()).getName());
	}

	//// required but unused
	
	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

	@Override
	public void focusLost(FocusEvent e) {
	}

}
