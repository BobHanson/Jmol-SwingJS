package jspecview.dialog;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;

import jspecview.api.JSVPanel;
import jspecview.api.PlatformDialog;
import jspecview.common.Spectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;

/**
 * Dialogs include Integration, PeakListing, Views, OverlayLegend, and Measurements
 * These dialogs have been generalized for platform independence.'
 * 
 * This manager is subclassed as AwtDialogManager and JsDialogManager, which apply their
 * own interpretation of how to create the dialog and get its event callbacks. For any
 * one session, there will be only one DialogManager, created in JSViewer. 
 * 
 * AwtDialogManager will create instances of AwtDialog extends javax.swing.JDialog; 
 * JsDialogManager will create instances of JsDialog extends jspecview.awtj2d.swing.JDialog.
 * 
 * @author hansonr
 *
 */
abstract public class DialogManager {

	protected JSViewer vwr;
	private Map<Object, String> htSelectors;
	protected Map<String, JSVDialog> htDialogs;
	private Map<String, Object> options;

	public DialogManager set(JSViewer viewer) {
		this.vwr = viewer;
		htSelectors = new Hashtable<Object, String>();
		htDialogs = new Hashtable<String, JSVDialog>();
		return this;
	}

	public final static int PLAIN_MESSAGE       = -1; // JOptionPane.PLAIN_MESSAGE
	public final static int ERROR_MESSAGE       =  0; // JOptionPane.ERROR_MESSAGE
	public final static int INFORMATION_MESSAGE =  1; // JOptionPane.INFORMATION_MESSAGE
	public final static int WARNING_MESSAGE     =  2; // JOptionPane.WARNING_MESSAGE
	public final static int QUESTION_MESSAGE    =  3; // JOptionPane.QUESTION_MESSAGE

	abstract public PlatformDialog getDialog(JSVDialog jsvDialog);

	abstract public String getDialogInput(Object parentComponent, String phrase,
			String title, int msgType, Object icon, Object[] objects,
			String defaultStr);

	abstract public int[] getLocationOnScreen(Object component);

	abstract public int getOptionFromDialog(Object frame, String[] items, JSVPanel jsvp,
			String dialogName, String labelName);

	abstract public void showMessageDialog(Object parentComponent, String msg, String title, int msgType);

  abstract public void showProperties(Object frame, Spectrum spectrum);
  
  abstract public void showMessage(Object frame, String text, String title);

  
  /**
   * register the JSV dialog with a unique key to be used as an ID in callbacks
   * optionKeys ending with "!" are one-of-a-kind, such as "views"
   * 
   * @param jsvDialog
   * @return id
   */
	protected String registerDialog(JSVDialog jsvDialog) {
		String id = jsvDialog.optionKey;
		if (!id.endsWith("!"))	
			id += " " + ("" + Math.random()).substring(3);
		if (htDialogs.containsKey(id))
			htDialogs.get(id).dispose();
		htDialogs.put(id, jsvDialog);
		return id;
	}

  public void registerSelector(String selectorName, Object columnSelector) {
		htSelectors.put(columnSelector, selectorName);
	}

	protected String getSelectorName(Object selector) {
		return htSelectors.get(selector);
	}

	public void showSourceErrors(Object frame, JDXSource currentSource) {
		if (currentSource == null) {
			showMessageDialog(frame,
					"Please Select a Spectrum.", "Select Spectrum", WARNING_MESSAGE);
			return;
		}
		String errorLog = currentSource.getErrorLog();
		if (errorLog != null && errorLog.length() > 0)
			showMessage(frame, errorLog, fixTitle(currentSource.getFilePath()));
		else
			showMessageDialog(frame, "No errors found.",
					"Error Log", INFORMATION_MESSAGE);
	}

	public void showSource(Object frame, Spectrum spec) {
	  String filePath = spec.getFilePath();
		if (filePath == null) {
			showMessageDialog(frame, "Please Select a Spectrum", "Select Spectrum",
					WARNING_MESSAGE);
			return;
		}
		if (filePath == "[inline]") {
		  showMessage(null, spec.getInlineData(), "Inline data");
      return;
		}
		try {
			String s = JSVFileManager.getFileAsString(filePath);
			if (vwr.isJS)
				s = PT.rep(s, "<", "&lt;");
			showMessage(null, s, fixTitle(filePath));
		} catch (Exception ex) {
			showMessageDialog(frame, "File Not Found", "SHOWSOURCE", ERROR_MESSAGE);
		}
	}

	/**
	 * processing click event from platform DialogManager
	 * 
	 * @param eventId   dialogId/buttonId starting with "btn", "chk", "cmb", or "txt"
	 */
	
	protected void processClick(String eventId) {
		int pt = eventId.lastIndexOf("/");
		String id = eventId.substring(pt + 1);
		String dialog = eventId.substring(0, pt);
		dialogCallback(dialog, id, null);
	}

	/**
	 * processing table cell click event from platform DialogManager; takes two
	 * hits in AWT -- one a row, the other a column
	 * 
	 * @param eventId
	 *          dialogId/[ROW|COL] or just dialogId
	 * @param index1
	 *          row if just dialogId or (row or col if AWT)
	 * @param index2
	 *          column if just dialogId or -1 if AWT
	 * @param adjusting
	 */
	protected void processTableEvent(String eventId, int index1, int index2,
			boolean adjusting) {
		int pt = eventId.lastIndexOf("/");
		String dialog = eventId.substring(0, pt);
		String selector = eventId.substring(pt + 1);
		String msg = "&selector=" + selector + "&index=" + index1
				+ (index2 < 0 ? "&adjusting=" + adjusting : "&index2=" + index2);
		dialogCallback(dialog, "tableSelect", msg);
	}

	/**
	 * processing window closing event from platform DialogManager
	 * 
	 * @param dialogId
	 */
	protected void processWindowClosing(String dialogId) {
		dialogCallback(dialogId, "windowClosing", null);
		htDialogs.remove(dialogId);
	}

	/**
	 * Send the callback to the appropriate dialog
	 * 
	 * @param dialogId
	 * @param id
	 * @param msg
	 */
	private void dialogCallback(String dialogId, String id, String msg) {
		JSVDialog jsvDialog = htDialogs.get(dialogId);
		if (jsvDialog != null)
			jsvDialog.callback(id, msg);		
	}

	/**
	 * persistent options such as units 
	 * 
	 * @return options
	 */
	Map<String, Object> getDialogOptions() {
		if (options == null)
			options = new Hashtable<String, Object>();
		return options;
	}

	public static String fixTitle(String title) {
		return (title.length() > 50 ? title.substring(0, 50) + "..." : title);
	}

}
