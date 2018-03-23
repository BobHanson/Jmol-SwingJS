/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-30 18:58:33 -0500 (Tue, 30 Jun 2009) $
 * $Revision: 11158 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
package jspecview.common;

import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.GenericGraphics;

import javajs.api.GenericColor;
import javajs.awt.Font;
import javajs.export.PDFCreator;
import javajs.util.CU;

import jspecview.api.JSVPanel;
import jspecview.api.JSVPdfWriter;
import jspecview.common.JSVersion;
import jspecview.common.PrintLayout;

/**
 * A class that creates the PDF document specific to JSpecView using
 * javajs.export.PDFCreator.
 * 
 */
public class PDFWriter implements GenericGraphics, JSVPdfWriter {
 
	private GenericGraphics g2d;
	private String date;
	private PDFCreator pdf;
	private boolean inPath;

	public PDFWriter() {
		pdf = new PDFCreator();
  }

	@Override
	public void createPdfDocument(JSVPanel panel, PrintLayout pl, OutputStream os) {
		boolean isLandscape = pl.layout.equals("landscape");
		date = pl.date;
		pdf.setOutputStream(os);
		g2d = panel.getPanelData().g2d;
		try {
			pdf.newDocument(pl.paperWidth, pl.paperHeight, isLandscape);
	    Map<String, String> ht = new Hashtable<String, String>();
	    ht.put("Producer", JSVersion.VERSION);
	    ht.put("Creator", "JSpecView " + JSVersion.VERSION);
	    ht.put("Author", "JSpecView");
	    if (date != null)
	    	ht.put("CreationDate", date);
	    pdf.addInfo(ht);
			panel.getPanelData().printPdf(this, pl);
			pdf.closeDocument();
		} catch (Exception e) {
			panel.showMessage(e.toString(), "PDF Creation Error");
		}		
	}
	
	@Override
	public boolean canDoLineTo() {
		return true;
	}

	@Override
	public void doStroke(Object g, boolean isBegin) {
		inPath = isBegin;
		if (!inPath)
			pdf.stroke();
	}
	
	@Override
	public void drawCircle(Object g, int x, int y, int diameter) {
		pdf.doCircle(x, y, (int) (diameter/2.0), false);		
	}

	@Override
	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
		pdf.moveto(x0, y0);
		pdf.lineto(x1, y1);
		if (!inPath)
			pdf.stroke();	
	}

	@Override
	public void drawPolygon(Object g, int[] axPoints, int[] ayPoints, int nPoints) {
		pdf.doPolygon(axPoints, ayPoints, nPoints, false);
	}

	@Override
	public void drawRect(Object g, int x, int y, int width, int height) {
		pdf.doRect(x, y, width, height, false);
	}

	@Override
	public void drawString(Object g, String s, int x, int y) {
		pdf.drawStringRotated(s, x, y, 0);
	}

	@Override
	public void drawStringRotated(Object g, String s, int x, int y, double angle) {
		pdf.drawStringRotated(s, x, y, (int) angle);
	}

	@Override
	public void fillBackground(Object g, GenericColor bgcolor) {
		// n/a?
	}

	@Override
	public void fillCircle(Object g, int x, int y, int diameter) {
		pdf.doCircle(x, y, (int) (diameter/2.0), true);				
	}

	@Override
	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		pdf.doPolygon(axPoints, ayPoints, nPoints, true);
	}

	@Override
	public void fillRect(Object g, int x, int y, int width, int height) {
		pdf.doRect(x, y, width, height, true);
	}

	@Override
	public void lineTo(Object g, int x, int y) {
		pdf.lineto(x, y);
	}

  private float[] rgb = new float[3];

  @Override
	public void setGraphicsColor(Object g, GenericColor c) {
	  CU.toRGB3f(c.getRGB(), rgb);
		pdf.setColor(rgb, true);
		pdf.setColor(rgb, false);
	}

	@Override
	public Font setFont(Object g, Font font) {
		String fname = "/Helvetica";// + font.fontFace;
		switch (font.idFontStyle) {
		case Font.FONT_STYLE_BOLD:
			fname += "-Bold";
			break;
		case Font.FONT_STYLE_BOLDITALIC:
			fname += "-BoldOblique";
			break;
		case Font.FONT_STYLE_ITALIC:
			fname += "-Oblique";
			break;
		}
		pdf.setFont(fname, font.fontSizeNominal);
		return font;
	}

	@Override
	public void setStrokeBold(Object g, boolean tf) {
		pdf.setLineWidth(tf ? 2 : 1);		
	}

	@Override
	public void translateScale(Object g, double x, double y, double scale) {
		pdf.translateScale((float) x, (float) y, (float) scale);
	}

	@Override
	public Object newGrayScaleImage(Object g, Object image, int width,
			int height, int[] buffer) {
		pdf.addImageResource(image, width, height, buffer, false);
		return image;
	}

	@Override
	public void drawGrayScaleImage(Object g, Object image, int destX0, int destY0,
			int destX1, int destY1, int srcX0, int srcY0, int srcX1, int srcY1) {
		pdf.drawImage(image, destX0, destY0, destX1, destY1, srcX0, srcY0, srcX1, srcY1);
	}

	////////// defer to JsG2D ///////
	
	@Override
	public void setWindowParameters(int width, int height) {
		// n/a
	}

	@Override
	public GenericColor getColor1(int argb) {
		return g2d.getColor1(argb);
	}

	@Override
	public GenericColor getColor3(int red, int green, int blue) {
		return g2d.getColor3(red, green, blue);
	}

	@Override
	public GenericColor getColor4(int r, int g, int b, int a) {
		return g2d.getColor4(r, g, b, a);
	}

}
