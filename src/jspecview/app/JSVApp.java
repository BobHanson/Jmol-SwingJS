/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General private
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General private License for more details.
 *
 *  You should have received a copy of the GNU Lesser General private
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'JSVApplet.java' - Web Application GUI
// University of the West Indies, Mona Campus
//
// 09-10-2007 commented out calls for exporting data
//            this was causing security issues with JRE 1.6.0_02 and 03
// 13-01-2008 in-line load JCAMP-DX file routine added
// 22-07-2008 reinstated calls for exporting since Ok with JRE 1.6.0_05
// 25-07-2008 added module to predict colour of solution
// 08-01-2010 need bugfix for protected static reverseplot
// 17-03-2010 fix for NMRShiftDB CML files
// 11-06-2011 fix for LINK files and reverseplot 
// 23-07-2011 jak - Added parameters for the visibility of x units, y units,
//            x scale, and y scale.  Added parameteres for the font,
//            title font, and integral plot color.  Added a method
//            to reset view from a javascript call.
// 24-09-2011 jak - Added parameter for integration ratio annotations.
// 08-10-2011 jak - Add a method to toggle integration from a javascript
//          call. Changed behaviour to remove integration after reset
//          view.

package jspecview.app;

import java.net.URL;
import java.util.Map;

import javajs.api.JSInterface;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.util.Logger;

import jspecview.api.AppletFrame;
import jspecview.api.JSVAppInterface;
import jspecview.api.JSVPanel;
import jspecview.api.PanelListener;
import jspecview.api.js.JSVAppletObject;
import jspecview.common.JSVersion;
import jspecview.common.Spectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.PanelData;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.common.PeakPickEvent;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;

import jspecview.source.JDXSource;

/**
 * JSpecView Applet class. For a list of parameters and scripting functionality
 * see the file JSpecView_Applet_Specification.html.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A. D. Walters
 * @author Prof Robert J. Lancashire
 */

public class JSVApp implements PanelListener, JSVAppInterface {

	public static final String CREDITS = "Authors:\nProf. Robert M. Hanson,\nD. Facey, K. Bryan, C. Walters, Prof. Robert J. Lancashire and\nvolunteer developers through sourceforge.";

	public JSVApp(AppletFrame appletFrame, boolean isJS) {
		this.appletFrame = appletFrame;
		initViewer(isJS);
		initParams(appletFrame.getParameter("script"));
	}

	private void initViewer(boolean isJS) {
		vwr = new JSViewer(this, true, isJS);
		appletFrame.setDropTargetListener(isSigned(), vwr);
		URL path = appletFrame.getDocumentBase();
		JSVFileManager.setDocumentBase(vwr, path);
	}

	protected AppletFrame appletFrame;

	boolean isNewWindow;

	// ------- settable parameters ------------

	public String appletReadyCallbackFunctionName;

	private String coordCallbackFunctionName;
	private String loadFileCallbackFunctionName;
	private String peakCallbackFunctionName;
	private String syncCallbackFunctionName;
	public JSViewer vwr;

	// ///// parameter set/get methods

	@Override
	public boolean isPro() {
		return isSigned();
	}

	@Override
	public boolean isSigned() {
		/**
		 * @j2sNative
		 * 
		 * return true;
		 */
		{
		return false;
		}
	}

	public AppletFrame getAppletFrame() {
		return appletFrame;
	}

	// ///////////////////////////////////////

	public void dispose() {
		try {
			vwr.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ///////////// private methods called from page or browser JavaScript calls
	// ////////////////
	//
	//
	// Notice that in all of these we use getSelectedPanel(), not selectedJSVPanel
	// That's because the methods aren't overridden in JSVAppletPro, and in that
	// case
	// we want to select the panel from MainFrame, not here. Thus, when the
	// Advanced...
	// tab is open, actions from outside of Jmol act on the MainFrame, not here.
	//
	// BH - 8.3.2012

	@Override
	public Map<String, Object> getPropertyAsJavaObject(String key) {
		return vwr.getPropertyAsJavaObject(key);
	}

	@Override
	public String getPropertyAsJSON(String key) {
		return PT.toJSON(null, getPropertyAsJavaObject(key));
	}

	/**
	 * Method that can be called from another applet or from javascript to return
	 * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
	 * 
	 * @return A String representation of the coordinate
	 */
	@Override
	public String getCoordinate() {
		return vwr.getCoordinate();
	}

	/**
	 * Loads in-line JCAMP-DX data into the existing applet window
	 * 
	 * @param data
	 *          String
	 */
	@Override
	public void loadInline(String data) {
		// newAppletPanel();
		siOpenDataOrFile(data, null, null, null, -1, -1, true, null, null);
		appletFrame.validateContent(3);
	}

	/**
	 * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, DIFDUP, FIX, AML,
	 * CML
	 * 
	 * @param type
	 * @param n
	 * @return data
	 * 
	 */
	@Override
	public String exportSpectrum(String type, int n) {
		return vwr.export(type, n);
	}

	@Override
	public void setFilePath(String tmpFilePath) {
		runScript("load " + PT.esc(tmpFilePath));
	}

	/**
	 * Sets the spectrum to the specified block number
	 * 
	 * @param n
	 */
	@Override
	public void setSpectrumNumber(int n) {
		runScript(ScriptToken.SPECTRUMNUMBER + " " + n);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles reversing the plot on a <code>JSVPanel</code>
	 */
	@Override
	public void reversePlot() {
		toggle(ScriptToken.REVERSEPLOT);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles the grid on a <code>JSVPanel</code>
	 */
	@Override
	public void toggleGrid() {
		toggle(ScriptToken.GRIDON);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles the coordinate on a <code>JSVPanel</code>
	 */
	@Override
	public void toggleCoordinate() {
		toggle(ScriptToken.COORDINATESON);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles the integration graph of a <code>JSVPanel</code>.
	 */
	@Override
	public void toggleIntegration() {
		toggle(ScriptToken.INTEGRATE);
	}

	private void toggle(ScriptToken st) {
		if (vwr.selectedPanel != null)
			runScript(st + " TOGGLE");
	}

	/**
	 * Method that can be called from another applet or from javascript that adds
	 * a highlight to a portion of the plot area of a <code>JSVPanel</code>
	 * 
	 * @param x1
	 *          the starting x value
	 * @param x2
	 *          the ending x value
	 * @param r
	 *          the red portion of the highlight color
	 * @param g
	 *          the green portion of the highlight color
	 * @param b
	 *          the blue portion of the highlight color
	 * @param a
	 *          the alpha portion of the highlight color
	 */
	@Override
	public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
		runScript("HIGHLIGHT " + x1 + " " +x2 + " " + r + " " + g +  " " + b + " " + a); 
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * removes a highlight from the plot area of a <code>JSVPanel</code>
	 * 
	 * @param x1
	 *          the starting x value
	 * @param x2
	 *          the ending x value
	 */
	@Override
	public void removeHighlight(double x1, double x2) {
		runScript("HIGHLIGHT " + x1 + " " + x2 + " OFF"); 
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * removes all highlights from the plot area of a <code>JSVPanel</code>
	 */
	@Override
	public void removeAllHighlights() {
		runScript("HIGHLIGHT OFF"); 
	}

	@Override
	public void syncScript(String peakScript) {
		vwr.syncScript(peakScript);
	}

	/**
	 * Writes a message to the status label
	 * 
	 * @param msg
	 *          the message
	 */
	@Override
	public void writeStatus(String msg) {
		Logger.info(msg);
		// statusTextLabel.setText(msg);
	}

	// //////////////////////// PRIVATE or SEMIPRIVATE METHODS

	/**
	 * starts or restarts applet display from scratch or from a JSVApplet.script()
	 * JavaScript command
	 * 
	 * Involves a two-pass sequence through parsing the parameters, because order
	 * is not important in this sort of call.
	 * 
	 * To call a script and have commands execute in order, use
	 * 
	 * JSVApplet.runScript(script)
	 * 
	 * instead
	 * 
	 * @param params
	 */
	public void initParams(String params) {
		vwr.parseInitScript(params);
		newAppletPanel();
		vwr.setPopupMenu(vwr.allowMenu, vwr.parameters
				.getBoolean(ScriptToken.ENABLEZOOM));
		if (vwr.allowMenu) {
			vwr.closeSource(null);
		}
		runScriptNow(params);
	}

	private void newAppletPanel() {
		Logger.info("newAppletPanel");
		appletFrame.createMainPanel(vwr);
	}

	private JSVPanel prevPanel;

	// //////////// JSVAppletPopupMenu calls

	@Override
	public void repaint() {
		
		@SuppressWarnings("unused")
		JSVAppletObject applet = (vwr == null ? null : vwr.html5Applet);
    /**
     * Jmol._repaint(applet,asNewThread)
     * 
     * should invoke 
     * 
     *   setTimeout(applet._applet.updateJS(applet._canvas)) // may be 0,0
     *   
     * when it is ready to do so.
     * 
     * @j2sNative
     * 
     * applet && self.Jmol && Jmol._repaint &&(Jmol._repaint(applet,true));
     * 
     */
		{
			appletFrame.repaint();
		}
	}
	
	/**
	 * 
	 * @param width
	 * @param height
	 */
	public void updateJS(int width, int height) {
		
	}

	@Override
	public boolean runScriptNow(String params) {
		return vwr.runScriptNow(params);
	}

	/**
	 * fires peakCallback ONLY if there is a peak found
	 * 
	 * fires coordCallback ONLY if there is no peak found or no peakCallback
	 * active
	 * 
	 * if (peakFound && havePeakCallback) { do the peakCallback } else { do the
	 * coordCallback }
	 * 
	 * Is that what we want?
	 * 
	 */
	private void checkCallbacks() {
		if (coordCallbackFunctionName == null && peakCallbackFunctionName == null)
			return;
		Coordinate coord = new Coordinate();
		Coordinate actualCoord = (peakCallbackFunctionName == null ? null
				: new Coordinate());
		// will return true if actualcoord is null (just doing coordCallback)
		if (!vwr.pd().getPickedCoordinates(coord, actualCoord))
			return;
		int iSpec = vwr.mainPanel.getCurrentPanelIndex();
		if (actualCoord == null)
			appletFrame.callToJavaScript(coordCallbackFunctionName, new Object[] {
					Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
					Integer.valueOf(iSpec + 1) });
		else
			appletFrame
					.callToJavaScript(
							peakCallbackFunctionName,
							new Object[] { Double.valueOf(coord.getXVal()),
									Double.valueOf(coord.getYVal()),
									Double.valueOf(actualCoord.getXVal()),
									Double.valueOf(actualCoord.getYVal()),
									Integer.valueOf(iSpec + 1) });
	}

	// /////////// MISC methods from interfaces /////////////

	/**
	 * called by Pro's popup window Advanced...
	 * 
	 * @param filePath
	 */
	public void doAdvanced(String filePath) {
		// only for JSVAppletPro
	}

	// //////////////// PanelEventInterface

	/**
	 * called by notifyPeakPickedListeners in JSVPanel
	 */
	@Override
	public void panelEvent(Object eventObj) {
		if (eventObj instanceof PeakPickEvent) {
			vwr.processPeakPickEvent(eventObj, false);
		} else if (eventObj instanceof ZoomEvent) {
		} else if (eventObj instanceof SubSpecChangeEvent) {
		}
	}

	/**
	 * Returns the calculated colour of a visible spectrum (Transmittance)
	 * 
	 * @return Color
	 */

	@Override
	public String getSolnColour() {
		return vwr.getSolutionColorStr(true);
	}
	
  /**
   * File has been loaded or model has been changed or atom picked.
   * This is a call to Jmol.View for view sets (new in Jmol 14.1.8)
   * 
   * @param msg  
   * 
   */
  private void updateJSView(String msg) {
  	
  	JSVAppletObject applet = vwr.html5Applet;
		JSVPanel panel = (applet == null ? null : vwr.selectedPanel);
    /**
     * @j2sNative
     * 
     * if (!applet || applet._viewSet == null) return;
     * 
     */
    {}
    applet._updateView(panel, msg);
  }

	/**
	 * @param msg
	 */
	@Override
	public synchronized void syncToJmol(String msg) {
		updateJSView(msg);
		if (syncCallbackFunctionName == null)
			return;
		Logger.info("JSVApp.syncToJmol JSV>Jmol " + msg);
		appletFrame.callToJavaScript(syncCallbackFunctionName, new Object[] { vwr.fullName, msg });
	}

	@Override
	public void setVisible(boolean b) {
		appletFrame.setPanelVisible(b);
	}

	@Override
	public void setCursor(int id) {
		vwr.apiPlatform.setCursor(id, appletFrame);
	}

	@Override
	public void runScript(String script) {
		vwr.runScript(script);
	}

	@Override
	public Lst<String> getScriptQueue() {
		return vwr.scriptQueue;
	}

	
	// ///////////// JSApp/MainFrame ScriptInterface /////////////

	@Override
	public void siSetCurrentSource(JDXSource source) {
		vwr.currentSource = source;
	}

	@Override
	public void siSendPanelChange() {
		if (vwr.selectedPanel == prevPanel)
			return;
		prevPanel = vwr.selectedPanel;
		vwr.sendPanelChange();
	}

	/**
	 * Shows the applet in a Frame
	 * 
	 * @param isSelected
	 */
	@Override
	public void siNewWindow(boolean isSelected, boolean fromFrame) {
		isNewWindow = isSelected;
		if (fromFrame) {
			if (vwr.jsvpPopupMenu != null)
				vwr.jsvpPopupMenu.setSelected("Window", false);
		} else {
			appletFrame.newWindow(isSelected);
		}
	}

	@Override
	public void siValidateAndRepaint(boolean isAll) {
		PanelData pd = vwr.pd();
		if (pd != null)
			pd.setTaintedAll();
		appletFrame.validate();
		repaint();
	}

	/**
	 * Loads a new file into the existing applet window
	 * 
	 * @param filePath
	 */
	@Override
	public void siSyncLoad(String filePath) {
		newAppletPanel();
		Logger.info("JSVP syncLoad reading " + filePath);
		siOpenDataOrFile(null, null, null, filePath, -1, -1, false, null, null);
		appletFrame.validateContent(3);
	}

	/*
	 * private void interruptQueueThreads() { if (commandWatcherThread != null)
	 * commandWatcherThread.interrupt(); }
	 */
	@Override
	public void siOpenDataOrFile(Object data, String name,
			Lst<Spectrum> specs, String url, int firstSpec, int lastSpec,
			boolean isAppend, String script, String id) {
		switch (vwr.openDataOrFile(data, name, specs, url, firstSpec, lastSpec,
				isAppend, id)) {
		case JSViewer.FILE_OPEN_OK:
			if (script != null)
				runScript(script);
			break;
		case JSViewer.FILE_OPEN_ALREADY:
			return;
		default:	
			siSetSelectedPanel(null);
			return;
		}

		if (vwr.jsvpPopupMenu != null)
			vwr.jsvpPopupMenu
					.setCompoundMenu(vwr.panelNodes, vwr.allowCompoundMenu);

		Logger.info(appletFrame.getAppletInfo() + " File "
				+ vwr.currentSource.getFilePath() + " Loaded Successfully");

	}

	/**
	 * overloaded in JSVAppletPro
	 * 
	 * @param scriptItem
	 */
	@Override
	public void siProcessCommand(String scriptItem) {
		vwr.runScriptNow(scriptItem);
	}

	@Override
	public void siSetSelectedPanel(JSVPanel jsvp) {
		vwr.mainPanel.setSelectedPanel(vwr, jsvp, vwr.panelNodes);
		vwr.selectedPanel = jsvp;
		vwr.spectraTree.setSelectedPanel(this, jsvp);
		if (jsvp == null) {
			vwr.selectedPanel = jsvp = appletFrame.getJSVPanel(vwr, null);
			vwr.mainPanel.setSelectedPanel(vwr, jsvp, null);
		}
		appletFrame.validate();
		if (jsvp != null) {
			jsvp.setEnabled(true);
			jsvp.setFocusable(true);
		}
	}

	@Override
	@SuppressWarnings("incomplete-switch")
	public void siExecSetCallback(ScriptToken st, String value) {
		switch (st) {
		case APPLETREADYCALLBACKFUNCTIONNAME:
			appletReadyCallbackFunctionName = value;
			break;
		case LOADFILECALLBACKFUNCTIONNAME:
			loadFileCallbackFunctionName = value;
			break;
		case PEAKCALLBACKFUNCTIONNAME:
			peakCallbackFunctionName = value;
			break;
		case SYNCCALLBACKFUNCTIONNAME:
			syncCallbackFunctionName = value;
			break;
		case COORDCALLBACKFUNCTIONNAME:
			coordCallbackFunctionName = value;
			break;
		}
	}

	@Override
	public String siLoaded(String value) {
		if (loadFileCallbackFunctionName != null)
			appletFrame.callToJavaScript(loadFileCallbackFunctionName, new Object[] { vwr.appletName,
					value });
		updateJSView(null);
		return null;
	}
		

	@Override
	public void siExecHidden(boolean b) {
		// ignored
	}

	@Override
	public void siExecScriptComplete(String msg, boolean isOK) {
	  if (!isOK)
	  	vwr.showMessage(msg);
		siValidateAndRepaint(false);
	}

	@Override
	public void siUpdateBoolean(ScriptToken st, boolean TF) {
		// ignored -- this is for setting buttons and menu items
	}

	@Override
	public void siCheckCallbacks(String title) {
		checkCallbacks();
	}

	// /////// multiple source changes ////////

	@Override
	public void siNodeSet(PanelNode panelNode) {
		appletFrame.validateContent(2);
		siValidateAndRepaint(false); // app does not do repaint here
	}

	@Override
	public void siSourceClosed(JDXSource source) {
		// n/a;
	}

	@Override
	public JSVPanel siGetNewJSVPanel(Spectrum spec) {
		if (spec == null) {
			vwr.initialEndIndex = vwr.initialStartIndex = -1;
			return null;
		}
		Lst<Spectrum> specs = new Lst<Spectrum>();
		specs.addLast(spec);
		JSVPanel jsvp = appletFrame.getJSVPanel(vwr, specs);
		jsvp.getPanelData().addListener(this);
		vwr.parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	@Override
	public JSVPanel siGetNewJSVPanel2(Lst<Spectrum> specs) {
		if (specs == null) {
			vwr.initialEndIndex = vwr.initialStartIndex = -1;
			return appletFrame.getJSVPanel(vwr, null);
		}
		JSVPanel jsvp = appletFrame.getJSVPanel(vwr, specs);
		vwr.initialEndIndex = vwr.initialStartIndex = -1;
		jsvp.getPanelData().addListener(this);
		vwr.parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	@Override
	public void siSetPropertiesFromPreferences(JSVPanel jsvp,
			boolean includeMeasures) {
		vwr.checkAutoIntegrate();
	}

	// not applicable to applet:

	@Override
	public void siSetLoaded(String fileName, String filePath) {
		//n/a
	}

	@Override
	public void siSetMenuEnables(PanelNode node, boolean isSplit) {
	}

	@Override
	public void siUpdateRecentMenus(String filePath) {
	}

	@Override
	public void siExecTest(String value) {
	  String data = "";//##TITLE= Acetophenone\n##JCAMP-DX= 5.01\n##DATA TYPE= MASS SPECTRUM\n##DATA CLASS= XYPOINTS\n##ORIGIN= UWI, Mona, JAMAICA\n##OWNER= public domain\n##LONGDATE= 2012/02/19 22:20:06.0416 -0600 $$ export date from JSpecView\n##BLOCK_ID= 4\n##$URL= http://wwwchem.uwimona.edu.jm/spectra\n##SPECTROMETER/DATA SYSTEM= Finnigan\n##.INSTRUMENT PARAMETERS= LOW RESOLUTION\n##.SPECTROMETER TYPE= TRAP\n##.INLET= GC\n##.IONIZATION MODE= EI+\n##MOLFORM= C 8 H 8 O\n##$MODELS= \n<Models>\n<ModelData id=\"acetophenone\" type=\"MOL\">\nacetophenone\nDSViewer          3D                             0\n\n17 17  0  0  0  0  0  0  0  0999 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  1\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  2\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  3\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  4\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  5\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  6\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  7\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  8\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  9\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0 10\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0 11\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0 12\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0 13\n-2.3832   -1.3197   -0.0010 C   0  0  0  0  0  0  0  0  0 14\n-2.0973   -1.8988    0.9105 H   0  0  0  0  0  0  0  0  0 15\n-2.0899   -1.9018   -0.9082 H   0  0  0  0  0  0  0  0  0 16\n-3.4920   -1.1799   -0.0059 H   0  0  0  0  0  0  0  0  0 17\n1  2  1  0  0  0\n2  5  4  0  0  0\n2  4  4  0  0  0\n3 12  1  0  0  0\n4  7  4  0  0  0\n5  6  4  0  0  0\n6 10  1  0  0  0\n6  3  4  0  0  0\n7  3  4  0  0  0\n7 11  1  0  0  0\n8  4  1  0  0  0\n9  5  1  0  0  0\n13  1  2  0  0  0\n14 16  1  0  0  0\n14  1  1  0  0  0\n14 15  1  0  0  0\n17 14  1  0  0  0\nM  END\n</ModelData>\n<ModelData id=\"2\" type=\"MOL\">\nacetophenone m/z 120\nDSViewer          3D                             0\n\n17 17  0  0  0  0  0  0  0  0999 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  1\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  2\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  3\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  4\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  5\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  6\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  7\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  8\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  9\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0 10\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0 11\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0 12\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0 13\n-2.3832   -1.3197   -0.0010 C   0  0  0  0  0  0  0  0  0 14\n-2.0973   -1.8988    0.9105 H   0  0  0  0  0  0  0  0  0 15\n-2.0899   -1.9018   -0.9082 H   0  0  0  0  0  0  0  0  0 16\n-3.4920   -1.1799   -0.0059 H   0  0  0  0  0  0  0  0  0 17\n1  2  1  0  0  0\n2  5  4  0  0  0\n2  4  4  0  0  0\n3 12  1  0  0  0\n4  7  4  0  0  0\n5  6  4  0  0  0\n6 10  1  0  0  0\n6  3  4  0  0  0\n7  3  4  0  0  0\n7 11  1  0  0  0\n8  4  1  0  0  0\n9  5  1  0  0  0\n13  1  2  0  0  0\n14 16  1  0  0  0\n14  1  1  0  0  0\n14 15  1  0  0  0\n17 14  1  0  0  0\nM  END\nacetophenone m/z 105\n\ncreated with ArgusLab version 4.0.1\n13 13  0  0  0  0  0  0  0  0  0 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0  0  0  0\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0  0  0  0\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0  0  0  0\n1  2  1  0  0  0  0\n1  8  2  0  0  0  0\n2  4  4  0  0  0  0\n2  5  4  0  0  0  0\n3  6  4  0  0  0  0\n3  7  4  0  0  0  0\n3 13  1  0  0  0  0\n4  7  4  0  0  0  0\n4  9  1  0  0  0  0\n5  6  4  0  0  0  0\n5 10  1  0  0  0  0\n6 11  1  0  0  0  0\n7 12  1  0  0  0  0\nM  END\nacetophenone m/z 77\n\ncreated with ArgusLab version 4.0.1\n11 11  0  0  0  0  0  0  0  0  0 V2000\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0  0  0  0\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0  0  0  0\n1  3  4  0  0  0  0\n1  4  4  0  0  0  0\n2  5  4  0  0  0  0\n2  6  4  0  0  0  0\n2 11  1  0  0  0  0\n3  6  4  0  0  0  0\n3  7  1  0  0  0  0\n4  5  4  0  0  0  0\n4  8  1  0  0  0  0\n5  9  1  0  0  0  0\n6 10  1  0  0  0  0\nM  END\n</ModelData>\n</Models>\n##$PEAKS= \n<Peaks type=\"MS\" xUnits=\"M/Z\" yUnits=\"RELATIVE ABUNDANCE\" >\n<PeakData id=\"1\" title=\"molecular ion (~120)\" peakShape=\"sharp\" model=\"2.1\"  xMax=\"121\" xMin=\"119\"  yMax=\"100\" yMin=\"0\" />\n<PeakData id=\"2\" title=\"fragment 1 (~105)\" peakShape=\"sharp\" model=\"2.2\"  xMax=\"106\" xMin=\"104\"  yMax=\"100\" yMin=\"0\" />\n<PeakData id=\"3\" title=\"fragment 2 (~77)\" peakShape=\"sharp\" model=\"2.3\"  xMax=\"78\" xMin=\"76\"  yMax=\"100\" yMin=\"0\" />\n</Peaks>\n##XUNITS= M/Z\n##YUNITS= RELATIVE ABUNDANCE\n##XFACTOR= 1E0\n##YFACTOR= 1E0\n##FIRSTX= 0\n##FIRSTY= 0\n##LASTX= 121\n##NPOINTS= 19\n##XYPOINTS= (XY..XY)\n0.000000, 0.000000 \n38.000000, 5.200000 \n39.000000, 8.000000 \n43.000000, 21.900000 \n50.000000, 20.200000 \n51.000000, 41.900000 \n52.000000, 4.000000 \n63.000000, 3.800000 \n74.000000, 6.600000 \n75.000000, 3.700000 \n76.000000, 4.600000 \n77.000000, 100.000000 \n78.000000, 10.400000 \n89.000000, 1.000000 \n91.000000, 1.000000 \n105.000000, 80.800000 \n106.000000, 6.000000 \n120.000000, 23.100000 \n121.000000, 2.000000 \n##END=";
		loadInline(data);
	}

	@Override
	public String print(String fileName) {
		return vwr.print(fileName);
	}

	public String checkScript(String script) {
		return vwr.checkScript(script);
	}

	public static String getAppletInfo() {
		return "JSpecView Applet " + JSVersion.VERSION + "\n\n" + CREDITS;
	}

}
