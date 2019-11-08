/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
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
//          call. Changed behaviour to remove integration after reset view.
// 19-06-2012 BH -changes to printing calls
// 23-06-2012 BH -Major change to Applet code to allow multiple file loads
// 28-06-2012 BH -Overlay/close/view spectrum dialog working SVN 961
// 02-07-2012 BH -show distances between peaks
// 04-07-2012 BH -Ctrl- PgUp/PgDn for 2D spectra 
// 06-07-2012 BH -Views menu implemented
// 08-07-2012 BH -refactoring of Graph, add new NMR dialog boxes
// 14-07-2012 BH -IR peak listing
// 17-07-2012 BH -getProperty key
// 18-07-2012 BH -MAC fix for repaint

package jspecview.applet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JFrame;

import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;
import jspecview.api.AppletFrame;
import jspecview.api.JSVAppletInterface;
import jspecview.api.JSVFileDropper;
import jspecview.api.JSVPanel;
import jspecview.app.JSVApp;
import jspecview.common.JSViewer;
import jspecview.common.Spectrum;
import jspecview.java.AwtMainPanel;
import jspecview.java.AwtPanel;
import netscape.javascript.JSObject;

/**
 * 
 * Entry point for the web.
 * 
 * JSpecView Applet class. For a list of parameters and scripting functionality
 * see the file JSpecView_Applet_Specification.html.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A. D. Walters
 * @author Prof Robert J. Lancashire
 * 
 *         http://blog.gorges.us/2009/03/how-to-enable-keywords-in-eclipse-and-
 *         subversion-svn/ $LastChangedRevision: 1097 $ $LastChangedDate:
 *         2012-07-23 11:10:30 -0500 (Mon, 23 Jul 2012) $
 */

public class JSVApplet extends JApplet implements JSVAppletInterface,
		AppletFrame, KeyListener {

	protected Thread commandWatcherThread;

	public JSVApp app;
	private boolean isStandalone = false;

	private JSViewer viewer;

	private boolean isJNLP;

	/**
	 * 
	 * Initializes applet with parameters and load the <code>JDXSource</code>
	 * called by the browser
	 * 
	 */
	@Override
	public void init() {
		app = new JSVApp(this, false);
		init2();
	}

	protected void init2() {
		isJNLP = (getParameter("syncId") == null);
		viewer = app.vwr;
		viewer.display = getContentPane();
		viewer.scriptQueue = new Lst<String>();
		commandWatcherThread = new Thread(new CommandWatcher());
		commandWatcherThread.setName("CommmandWatcherThread");
		commandWatcherThread.start();
		Logger.info(getAppletInfo());
		if (app.appletReadyCallbackFunctionName != null && viewer.fullName != null)
			callToJavaScript(app.appletReadyCallbackFunctionName, new Object[] {
					viewer.appletName, viewer.fullName, Boolean.TRUE, this });
	}

	private static final long serialVersionUID = 1L;

	@Override
	public boolean isPro() {
		return app.isPro();
	}

	@Override
	public boolean isSigned() {
		return app.isSigned();
	}

	// /////////// public methods called from page or browser ////////////////
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
	public void finalize() {
		System.out.println("JSpecView " + this + " finalized");
	}

	@Override
	public void destroy() {
		System.out.println("destroy called on " + this);
		if (commandWatcherThread != null) {
			commandWatcherThread.interrupt();
			commandWatcherThread = null;
		}
		app.dispose();
		app = null;
    if (isJNLP)
      System.exit(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#getParameter(java.lang.String,
	 * java.lang.String)
	 * 
	 * not used
	 */
	public String getParameter(String key, String def) {
		return isStandalone ? System.getProperty(key, def)
				: (getParameter(key) != null ? getParameter(key) : def);
	}

	/**
	 * Get Applet information
	 * 
	 * @return the String "JSpecView Applet"
	 */
	@Override
	public String getAppletInfo() {
		return JSVApp.getAppletInfo();
	}
	
	// /////////////// JSpecView JavaScript calls ///////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#getSolnColour()
	 */

	@Override
	public String getSolnColour() {
		return app.getSolnColour();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#getCoordinate()
	 */
	@Override
	public String getCoordinate() {
		return app.getCoordinate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#loadInline(java.lang.String)
	 */
	@Override
	public void loadInline(String data) {
		app.loadInline(data);
	}

	@Deprecated
	public String export(String type, int n) {
		return app.exportSpectrum(type, n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#exportSpectrum(java.lang.String,
	 * int)
	 */
	@Override
	public String exportSpectrum(String type, int n) {
		return app.exportSpectrum(type, n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#setFilePath(java.lang.String)
	 */
	@Override
	public void setFilePath(String tmpFilePath) {
		app.setFilePath(tmpFilePath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#setSpectrumNumber(int)
	 */
	@Override
	public void setSpectrumNumber(int i) {
		app.setSpectrumNumber(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#toggleGrid()
	 */
	@Override
	public void toggleGrid() {
		app.toggleGrid();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#toggleCoordinate()
	 */
	@Override
	public void toggleCoordinate() {
		app.toggleCoordinate();
	}

  @Override
  public void togglePointsOnly() {
    app.togglePointsOnly();
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#toggleIntegration()
	 */
	@Override
	public void toggleIntegration() {
		app.toggleIntegration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#addHighlight(double, double, int,
	 * int, int, int)
	 */
	@Override
	public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
		app.addHighlight(x1, x2, r, g, b, a);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#removeAllHighlights()
	 */
	@Override
	public void removeAllHighlights() {
		app.removeAllHighlights();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#removeHighlight(double, double)
	 */
	@Override
	public void removeHighlight(double x1, double x2) {
		app.removeHighlight(x1, x2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#reversePlot()
	 */
	@Override
	public void reversePlot() {
		app.reversePlot();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#script(java.lang.String)
	 */
	@Deprecated
	public void script(String script) {
		app.initParams(script);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#runScript(java.lang.String)
	 */
	@Override
	public void runScript(String script) {
		app.runScript(script);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#syncScript(java.lang.String)
	 */
	@Override
	public void syncScript(String peakScript) {
		app.syncScript(peakScript);
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#writeStatus(java.lang.String)
	 */
	@Override
	public void writeStatus(String msg) {
		app.writeStatus(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jspecview.applet.JSVAppletInterface#getPropertyAsJavaObject(java.lang.String
	 * )
	 */
	@Override
	public Map<String, Object> getPropertyAsJavaObject(String key) {
		return app.getPropertyAsJavaObject(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jspecview.applet.JSVAppletInterface#getPropertyAsJSON(java.lang.String)
	 */
	@Override
	public String getPropertyAsJSON(String key) {
		return app.getPropertyAsJSON(key);
	}

	@Override
	public boolean runScriptNow(String script) {
		return app.runScriptNow(script);
	}

	private DropTargetListener dtl;
	private Component mainPanel;
	private JFrame offWindowFrame;

	@Override
	public void setDropTargetListener(boolean isSigned, JSViewer viewer) {
		if (dtl == null && isSigned)
			dtl = (DropTargetListener) ((JSVFileDropper) viewer.getPlatformInterface("FileDropper")).set(viewer);
	}

	@Override
	public void validateContent(int mode) {
		if ((mode & 1) == 1)
			getContentPane().validate();
		if ((mode & 2) == 2)
			mainPanel.validate();
	}

	@Override
	public void createMainPanel(JSViewer viewer) {
		getContentPane().removeAll();
		mainPanel = (Component) (viewer.mainPanel = new AwtMainPanel(
				new BorderLayout()));
		getContentPane().add(mainPanel);
	}

	@Override
	public void newWindow(boolean isSelected) {
		if (isSelected) {
			offWindowFrame = new JFrame("JSpecView");
			offWindowFrame.setSize(getSize());
			final Dimension d = mainPanel.getSize();
			offWindowFrame.add(mainPanel);
			offWindowFrame.validate();
			offWindowFrame.setVisible(true);
			remove(mainPanel);
			app.siValidateAndRepaint(false);
			offWindowFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					windowClosingEvent(d);
				}
			});
		} else {
			getContentPane().add(mainPanel);
			app.siValidateAndRepaint(false);
			offWindowFrame.removeAll();
			offWindowFrame.dispose();
			offWindowFrame = null;
		}
	}

	protected void windowClosingEvent(Dimension d) {
		mainPanel.setSize(d);
		getContentPane().add(mainPanel);
		setVisible(true);
		app.siValidateAndRepaint(false);
		offWindowFrame.removeAll();
		offWindowFrame.dispose();
		app.siNewWindow(false, true);
	}

	/**
	 * Calls a javascript function given by the function name passing to it the
	 * string parameters as arguments
	 * 
	 * @param callback
	 * @param params
	 * 
	 */
	@Override
	public void callToJavaScript(String callback, Object[] params) {
		try {

			JSObject jso = JSObject.getWindow(this);
			
			if (callback.length() > 0) {
				if (callback.indexOf(".") > 0) {
					String[] mods = PT.split(callback, ".");
					for (int i = 0; i < mods.length - 1; i++) {
						jso = (JSObject) jso.getMember(mods[i]);
					}
					callback = mods[mods.length - 1];
				}
				Logger.info("JSVApplet calling " + jso + " " + callback);
				jso.call(callback, params);
			}
		} catch (Exception npe) {
			Logger.warn("EXCEPTION-> " + npe);
		}
	}

	@Override
	public void setPanelVisible(boolean b) {
		mainPanel.setVisible(b);
	}

	@Override
	public JSVPanel getJSVPanel(JSViewer viewer, Lst<Spectrum> specs) {
		return (specs == null ? AwtPanel.getEmptyPanel(viewer)
				: AwtPanel.getPanelMany(viewer, specs));
	}

	// for the signed applet to load a remote file, it must
	// be using a thread started by the initiating thread;

	class CommandWatcher implements Runnable {
		@Override
		public void run() {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			int commandDelay = 200;
			while (commandWatcherThread != null) {
				try {
					Thread.sleep(commandDelay);
					if (commandWatcherThread != null) {
						Lst<String> q = app.vwr.scriptQueue;
						if (q.size() > 0) {
							String scriptItem = q.removeItemAt(0);
							if (scriptItem != null)
								app.siProcessCommand(scriptItem);
						}
					}
				} catch (InterruptedException ie) {
					Logger.info("CommandWatcher InterruptedException!");
					break;
				} catch (Exception ie) {
					String s = "script processing ERROR:\n\n" + ie.toString();
					for (int i = 0; i < ie.getStackTrace().length; i++) {
						s += "\n" + ie.getStackTrace()[i].toString();
					}
					Logger.info("CommandWatcher Exception! " + s);
					break;
				}
			}
			commandWatcherThread = null;
		}
	}

	@Override
	public void doExitJmol() {
		// n/a
	}

	@Override
	public JSVApp getApp() {
		return app;
	}

	@Override
	public String print(String fileName) {
		return app.print(fileName);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		keyPressedEvent(e.getKeyCode(), e.getKeyChar());		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	/**
   * @param keyCode 
	 * @param keyChar  
   */
	protected void keyPressedEvent(int keyCode, char keyChar) {	
//		String ret = vwr.checkCommandLineForTip(keyChar, commandInput.getText(), true);
//		if (ret != null)
//			commandInput.setText(ret);
//		commandInput.requestFocusInWindow();
}

	@Override
	public String checkScript(String script) {
		String s = app.checkScript(script);
		if (s != null)
			System.out.println(s);
		return s;
	}
	
}
