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

package jspecview.java;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javajs.api.GenericColor;
import org.jmol.awtjs.swing.Font;

import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.PT;

import javax.imageio.ImageIO;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.jmol.api.GenericFileInterface;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.GenericPlatform;
import org.jmol.util.Logger;

import jspecview.api.JSVPanel;
import jspecview.api.JSVPdfWriter;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;

/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Faceyf
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtPanel extends JPanel implements JSVPanel, Printable {
  private static final long serialVersionUID = 1L;
  
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

	private JSViewer vwr;
	private GenericMouseInterface mouse;

  private GenericColor bgcolor;
	
  /**
   * Constructs a new JSVPanel
   * @param viewer 
   * 
   * @return this
   */
  public static AwtPanel getEmptyPanel(JSViewer viewer) {
    // initial applet with no spectrum but with pop-up capability
  	return new AwtPanel(viewer, false);
  }
 
  /**
   * Constructs a new JSVPanel
   * @param viewer 
   * 
   * @param spectrum
   *        the spectrum
   * @return this
   */
  public static AwtPanel getPanelOne(JSViewer viewer, Spectrum spectrum) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
  	ToolTipManager.sharedInstance().setInitialDelay(0);
  	AwtPanel p = new AwtPanel(viewer, true);
    p.pd.initOne(spectrum);
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
  public static AwtPanel getPanelMany(JSViewer viewer, Lst<Spectrum> spectra) {
  	AwtPanel p = new AwtPanel(viewer, true);
    p.pd.initMany(spectra, viewer.initialStartIndex, viewer.initialEndIndex);
    return p;
  }

  private AwtPanel(JSViewer viewer, boolean withPD) {
  	this.vwr = viewer;
    this.pd = (withPD ? new PanelData(this, viewer) : null);
  	this.apiPlatform = viewer.apiPlatform;
    mouse = apiPlatform.getMouseManager(0, this);
    setBorder(BorderFactory.createLineBorder(Color.BLACK));
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
  }

  @Override
	public void setTitle(String title) {
    pd.title = title;
    setName(title);
  }

	public void setColorOrFont(ColorParameters ds, ScriptToken st) {
  	pd.setColorOrFont(ds, st);
  }

  @Override
	public void setBackgroundColor(GenericColor color) {
  	setBackground((Color) (bgcolor = color));
  }
  
  public GenericColor getBackgroundColor() {
  	return bgcolor;
  }
  
  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  @Override
	public void doRepaint(boolean andTaintAll) {
  	if (andTaintAll)
  		pd.setTaintedAll();
  	// to the system
  	if (!pd.isPrinting)
      vwr.requestRepaint();
  }
  
  @Override
	public void update(Graphics g) {
  	// from the system
  	// System: Do not clear rectangle -- we are opaque and will take care of that.
  	// seems unnecessary, but apparently for the Mac it is critical. Still not totally convinced!
      paint(g);
  }
 

  /**
   * Overrides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  @Override
  public void paintComponent(Graphics g) {
  	
  	// from the system, via update or applet/app repaint
  	
    if (vwr == null || pd == null || pd.graphSets == null || pd.isPrinting)
      return;
    
    super.paintComponent(g); // paint background
    try {
    	// despite the above catch for pd == null, it can still get here 
    	pd.g2d = pd.g2d0;
    	pd.drawGraph(g, g, g, getWidth(), getHeight(), false);
    } catch (Exception e) {
    	System.out.println("Exception while painting " + e);
    	e.printStackTrace();
    }
    
    vwr.repaintDone();
  }
  
  @Override
	public String getInput(String message, String title, String sval) {
    String ret = (String) JOptionPane.showInputDialog(this, message, title,
        JOptionPane.QUESTION_MESSAGE, null, null, sval);
    getFocusNow(true);
    return ret;
  }

	@Override
	public void showMessage(String msg, String title) {
		Logger.info(msg);
		if (title != null)
			JOptionPane.showMessageDialog(this, msg, title, (msg.startsWith("<html>") ? JOptionPane.INFORMATION_MESSAGE 
					: JOptionPane.PLAIN_MESSAGE));	
		getFocusNow(true);
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

		// MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

		pl.title = title;
		pl.date = apiPlatform.getDateFormat("8824");
		pd.setPrint(pl, os == null ? pl.font : "Helvetica");

		/* Create a print job */

		try {
			PrinterJob pj = (os == null ? PrinterJob.getPrinterJob() : null);
			if (pj != null) {
				if (title.length() > 30)
					title = title.substring(0, 30);
				pj.setJobName(title);
				pj.setPrintable(this);
			}
			if (pj == null || pj.printDialog()) {
				try {
					if (pj == null) {
						Dimension d = getDimension((MediaSizeName) pl.paper);
						pl.paperWidth = d.width;
						pl.paperHeight = d.height;
						((JSVPdfWriter) JSViewer.getInterface("jspecview.common.PDFWriter")).createPdfDocument(this, pl, os);
					} else {
						PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
						aset
								.add(pl.layout.equals("landscape") ? OrientationRequested.LANDSCAPE
										: OrientationRequested.PORTRAIT);
						aset.add((Attribute) pl.paper);
						pj.print(aset);
					}
				} catch (PrinterException ex) {
					String s = ex.getMessage();
					if (s == null)
						return;
					s = PT.rep(s, "not accepting job.", "not accepting jobs.");
					// not my fault -- Windows grammar error!
					showMessage(s, "Printer Error");
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			pd.setPrint(null, null);
		}
	}

	private Dimension getDimension(MediaSizeName paper) {
		// ftp://ftp.pwg.org/pub/pwg/media-sizes/pwg-media-size-03.pdf
		// at 72 dpi we have...
		if (paper == MediaSizeName.NA_LETTER) {
			return new Dimension((int) (8.5 * 72), 11 * 72);
		}
		if (paper == MediaSizeName.NA_LEGAL) {
			return new Dimension((int) (8.5 * 72), 14 * 72);
		}
		if (paper == MediaSizeName.ISO_A4) {
			return new Dimension((int) (210 / 25.4 * 72), (int) (297 / 25.4 * 72));
		}
		// if (paper == MediaSizeName.ISO_B4) {
		return new Dimension((int) (250 / 25.4 * 72), (int) (353 / 25.4 * 72));
	}

  /**
   * Implements method print in interface printable
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param pf
   *        the <code>PageFormat</code> object
   * @param pi
   *        the page index -- -1 for PDF creation
   * @return an int that depends on whether a print was successful
   * @throws PrinterException
   */
  @Override
	public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
  	return pd.print(g, pf.getImageableHeight(), pf.getImageableWidth(), 
  			pf.getImageableX(), pf.getImageableY(),
  			pf.getPaper().getHeight(), pf.getPaper().getWidth(), 
  			pf.getOrientation() == PageFormat.PORTRAIT, pi);
  }

	@Override
	public int getFontFaceID(String name) {
		return Font.getFontFaceID("SansSerif");
	}
	

	@Override
	public String saveImage(String type, GenericFileInterface file, OC out) {
		String msg = "OK";
    try {
	    Image image = createImage(getWidth(), getHeight());
	    paint(image.getGraphics());
			ImageIO.write((RenderedImage) image, type, (File) file);
		} catch (IOException e) {
			msg = e.toString();
			showMessage(msg, "Error Saving Image");
		}
		return null;
	}

	///// threading and focus
	
	@Override
	public void getFocusNow(boolean asThread) {
		if (asThread)
			SwingUtilities.invokeLater(new RequestThread());
		else
  		requestFocusInWindow();
		if (pd != null)
			pd.dialogsToFront(null);
	}

  public class RequestThread implements Runnable {
		@Override
		public void run() {
			requestFocusInWindow();
		}
  }

	
	///////////
	
  @Override
  public String toString() {
    return pd.getSpectrumAt(0).toString();
  }

	@Override
	public boolean processMouseEvent(int id, int x, int y, int modifiers,
			long time) {
		return mouse.processEvent(id, x, y, modifiers, time);
	}

	@Override
	public void processTwoPointGesture(float[][][] touches) {
		// n/a
	}

	@Override
	public void showMenu(int x, int y) {
  	vwr.showMenu(x, y);
	}

	@Override
	public void paintComponent(Object display) {
		super.paintComponent((Graphics) display);
	}

}
