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

// CHANGES to 'JSVPanel.java'
// University of the West Indies, Mona Campus
//
// 25-06-2007 rjl - bug in ReversePlot for non-continuous spectra fixed
//                - previously, one point less than npoints was displayed
// 25-06-2007 cw  - show/hide/close modified
// 10-02-2009 cw  - adjust for non zero baseline in North South plots
// 24-08-2010 rjl - check coord output is not Internationalised and uses decimal point not comma
// 31-10-2010 rjl - bug fix for drawZoomBox suggested by Tim te Beek
// 01-11-2010 rjl - bug fix for drawZoomBox
// 05-11-2010 rjl - colour the drawZoomBox area suggested by Valery Tkachenko
// 23-07-2011 jak - Added feature to draw the x scale, y scale, x units and y units
//					independently of each other. Added independent controls for the font,
//					title font, title bold, and integral plot color.
// 24-09-2011 jak - Altered drawGraph to fix bug related to reversed highlights. Added code to
//					draw integration ratio annotations
// 03-06-2012 rmh - Full overhaul; code simplification; added support for Jcamp 6 nD spectra

package jspecview.js2d;

import java.io.OutputStream;

import javajs.api.GenericColor;
import org.jmol.awtjs.Font;
import javajs.util.Base64;
import javajs.util.Lst;
import javajs.util.OC;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.GenericPlatform;
import org.jmol.util.Logger;

import jspecview.api.JSVPanel;
import jspecview.api.JSVPdfWriter;
import jspecview.api.js.JSVAppletObject;
import jspecview.common.ExportType;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;


/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class JsPanel implements JSVPanel {

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

	private GenericPlatform apiPlatform;
	@Override
	public GenericPlatform getApiPlatform() {
		return apiPlatform;
	}
	
  private PanelData pd;
  @Override
	public PanelData getPanelData() {
    return pd;
  }

	private GenericMouseInterface mouse;
	private JSViewer vwr;

	String name;
	private GenericColor bgcolor;

  /**
   * Constructs a new JSVPanel
   * @param viewer 
   * 
   * @return this
   */
  public static JsPanel getEmptyPanel(JSViewer viewer) {
    // initial applet with no spectrum but with pop-up capability
  	JsPanel p = new JsPanel(viewer, false);
  	p.pd = null;
    return p;
  }
 
	/**
   * Constructs a <code>JSVPanel</code> with List of spectra and corresponding
   * start and end indices of data points that should be displayed
   * @param viewer 
   * 
   * @param spectra
   *        the List of <code>Graph</code> instances
   * @return this
   */
  public static JsPanel getPanelMany(JSViewer viewer, Lst<Spectrum> spectra) {
  	JsPanel p = new JsPanel(viewer, true);
    p.pd.initMany(spectra, viewer.initialStartIndex, viewer.initialEndIndex);
    return p;
  }

  private JsPanel(JSViewer viewer, boolean withPd) {
  	this.vwr = viewer;
    this.pd = (withPd ? new PanelData(this, viewer) : null);
  	apiPlatform = viewer.apiPlatform;
    mouse = apiPlatform.getMouseManager(0, this);
//  setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}

  @Override
	public String getTitle() {
  	return pd.getTitle();
  }
  
  @Override
	public void dispose() {
    //toolTip = null;
    if (pd != null)
      pd.dispose();
    pd = null;
    mouse.dispose();
    mouse = null;
  }

  @Override
	public void setTitle(String title) {
    pd.title = title;
    this.name = title;
  }

	public void setColorOrFont(ColorParameters ds, ScriptToken st) {
  	pd.setColorOrFont(ds, st);
  }

  @Override
	public void setBackgroundColor(GenericColor color) {
  	bgcolor = color;
  }
  
	///// threading and focus
	
  @Override
	public String getInput(String message, String title, String sval) {
  	String ret = null;
  	/**
  	 * @j2sNative
  	 * 
  	 * ret = prompt(message, sval);
  	 */
  	{
  	}
    getFocusNow(true);
    return ret;
  }

	@Override
	public void showMessage(String msg, String title) {
		Logger.info(msg);
		if (vwr.html5Applet != null)
		  vwr.html5Applet._showStatus(msg, title);
		getFocusNow(true);
	}

	@Override
	public void getFocusNow(boolean asThread) {
		if (pd != null)
			pd.dialogsToFront(null);
	}

	@Override
	public int getFontFaceID(String name) {
		return Font.getFontFaceID("SansSerif");
	}
	
  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  @Override
	public void doRepaint(boolean andTaintAll) {
  	if (pd == null)
  		return;
  	if (andTaintAll)
  		pd.setTaintedAll();
  	// from dialogs, to the system
  	if (!pd.isPrinting)
      vwr.requestRepaint();
  }
  
//  @Override
//	public void update(Graphics g) {
//  	// from the system
//  	// System: Do not clear rectangle -- we are opaque and will take care of that.
//  	// seems unnecessary, but apparently for the Mac it is critical. Still not totally convinced!
//      paint(g);
//  }
 

  /**
   * Overrides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param context
   *        the canvas's context
   */
  @Override
	public void paintComponent(Object context) {

  	
  	
  	// from the system, via update or applet/app repaint
  	
    Object contextFront = null;
    Object contextRear = null;
    /**
     * @j2sNative
     * 
     * 
     * contextFront = context.canvas.frontLayer.getContext("2d");
     * contextRear = context;
     * 
     */
    {}

    if (vwr == null)
      return;

    if (pd == null) {
    	if (bgcolor == null)
    		bgcolor = vwr.g2d.getColor1(-1);
			vwr.g2d.fillBackground(context, bgcolor);
			vwr.g2d.fillBackground(contextRear, bgcolor);
			vwr.g2d.fillBackground(contextFront, bgcolor);
			return;
  	}
  	
    if (pd.graphSets == null || pd.isPrinting)
      return;
    pd.g2d = pd.g2d0;
    pd.drawGraph(context, contextFront, contextRear, getWidth(), getHeight(), false);
    vwr.repaintDone();
  }

  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

	/**
	 * Send a print job of the spectrum to the default printer on the system
	 * 
	 * @param pl
	 *          the layout of the print job
	 * @param os
	 * @param title
	 */
	@Override
	public void printPanel(PrintLayout pl, OutputStream os, String title) {
		pl.title = title;
		pl.date = apiPlatform.getDateFormat("8824");
		pd.setPrint(pl, "Helvetica");
		try {
			((JSVPdfWriter) JSViewer.getInterface("jspecview.common.PDFWriter")).createPdfDocument(this, pl, os);
  	} catch (Exception ex) {
  		showMessage(ex.toString(), "creating PDF");
  	} finally {
			pd.setPrint(null, null);
  	}
	}

	@Override
	public String saveImage(String type, GenericFileInterface file, OC out) {
	
			String fname = file.getName();
			boolean isPNG = type.equals(ExportType.PNG);				
			String s = (isPNG ? "png" : "jpeg");
			//pd.graphToMain(); TODO
			/**
			 * this will probably be png even if jpeg is requested
			 * 
			 * @j2sNative
			 * 
			 *            s = viewer.display.toDataURL(s);
			 *	if (!isPNG && s.contains("/png"))
			 *		fname = fname.split('.jp')[0] + ".png";
			 * 
			 */
			{
			}
			try {
				out = vwr.getOutputChannel(fname, true);
				byte[] data = Base64.decodeBase64(s);
				out.write(data, 0, data.length);
				out.closeChannel();
				return "OK " + out.getByteCount() + " bytes";
			} catch (Exception e) {
				return e.toString();
			}
	}

	@Override
	public boolean hasFocus() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void repaint() {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("unused")
	@Override
	public void setToolTipText(String s) {
		int x = pd.mouseX;
		int y = pd.mouseY;
		if (vwr.html5Applet != null)
		    vwr.html5Applet._showTooltip(s, x, y);
	}

	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return vwr.getHeight();
	}

	@Override
	public int getWidth() {
		return vwr.getWidth();
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFocusable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEnabled(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFocusable(boolean b) {
		// TODO Auto-generated method stub
		
	}

  @Override
  public String toString() {
    return (pd == null ? "<closed>" : "" + pd.getSpectrumAt(0));
  }

  /**
   * called only by JavaScript
   * 
   * @param id
   * @param x
   * @param y
   * @param modifiers
   * @param time
   * @return t/f
   */
  @Override
	public boolean processMouseEvent(int id, int x, int y, int modifiers, long time) {
  	return mouse != null && mouse.processEvent(id, x, y, modifiers, time);
  }

	@Override
	public void processTwoPointGesture(float[][][] touches) {
		if (mouse != null)
			mouse.processTwoPointGesture(touches);
	}

	@Override
	public void showMenu(int x, int y) {
  	vwr.showMenu(x, y);
	}
}
	