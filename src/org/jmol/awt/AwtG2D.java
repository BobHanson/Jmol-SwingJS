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

package org.jmol.awt;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import javajs.api.GenericColor;
import org.jmol.awtjs.swing.Font;

import org.jmol.api.GenericGraphics;

/**
 * generic 2D drawing methods -- AWT version
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtG2D implements GenericGraphics {
  
	BasicStroke strokeBasic = new BasicStroke();
  BasicStroke strokeBold = new BasicStroke(2f);

  public AwtG2D() {
	}

  @Override
	public GenericColor getColor4(int r, int g, int b, int a) {
    return new AwtColor(r, g, b, a);
  }
  
  @Override
	public GenericColor getColor3(int r, int g, int b) {
    return new AwtColor(r, g, b);
  }
  
  @Override
	public GenericColor getColor1(int rgb) {
    return new AwtColor(rgb);
  }
  
  /*-----------------GRAPHICS METHODS----------------------------------- */
	@Override
	public void drawString(Object g, String text, int x, int y) {
		//System.out.println("Awtg2d.drawString " + text + " " + x + " " + y);
		((Graphics) g).drawString(text, x, y);
	}
	
	@Override
	public void drawStringRotated(Object g, String text, int x, int y, double angle) {
		angle = angle / 180.0 * Math.PI;
  	((Graphics2D) g).rotate(angle, x, y);
		((Graphics) g).drawString(text, x, y);
  	((Graphics2D) g).rotate(-angle, x, y);
	}

	@Override
	public void setGraphicsColor(Object g, GenericColor c) {
		((Graphics) g).setColor((Color) c);
	}

	@Override
	public Font setFont(Object g, org.jmol.awtjs.swing.Font font) {
		//System.out.println("AwtG2D.setGraphicsFont " + font.getInfo());
		((Graphics) g).setFont((java.awt.Font) font.font);
		return font;
	}

	@Override
	public void drawGrayScaleImage(Object g, Object image2d, int destX0, int destY0,
			int destX1, int destY1, int srcX0, int srcY0, int srcX1, int srcY1) {		
		((Graphics) g).drawImage((Image) image2d, destX0, destY0, destX1, destY1, srcX0, srcY0, srcX1, srcY1, null);
	}

	@Override
	public Object newGrayScaleImage(Object gMain, Object image, int width, int height, int[] buffer) {
		BufferedImage image2D = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_GRAY);
		image2D.getRaster().setSamples(0, 0, width, height, 0,
				buffer);
		return image2D;
	}

	@Override
	public void fillRect(Object g, int x, int y, int width, int height) {
		((Graphics) g).fillRect(x, y, width, height);	
	}

	@Override
	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
		if (path == null) {
			((Graphics) g).drawLine(x0, y0, x1, y1);
		} else {
			path.moveTo(x0, y0);
			path.lineTo(x1, y1);
		}			
	}

	@Override
	public void drawRect(Object g, int x, int y, int xPixels,
			int yPixels) {
		//System.out.println("Awtg2s.drawRect " + x + " " + y + " " + xPixels + " " + yPixels);
		((Graphics) g).drawRect(x, y, xPixels, yPixels);
	}

//	public int getFontHeight(Object g) {
//    return ((Graphics) g).getFontMetrics().getHeight();
//	}
//
//	public int getStringWidth(Object g, String s) {
//  	return (s == null ? 0 : ((Graphics) g).getFontMetrics().stringWidth(s));
//	}

	@Override
	public void drawCircle(Object g, int x, int y, int diameter) {
		((Graphics) g).drawOval(x, y, diameter, diameter);
	}

	@Override
	public void drawPolygon(Object g, int[] axPoints, int[] ayPoints, int nPoints) {
		((Graphics) g).drawPolygon(axPoints, ayPoints, nPoints);
	}

	@Override
	public void fillCircle(Object g, int x, int y, int diameter) {
		((Graphics) g).fillOval(x, y, diameter, diameter);
	}

	@Override
	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		((Graphics) g).fillPolygon(ayPoints, axPoints, nPoints);
	}

	@Override
	public void translateScale(Object g, double x, double y, double scale) {
		//System.out.println("Awtg2d.translateScale " + x + " " + y + " " + scale);
		((Graphics2D) g).translate(x, y);
		((Graphics2D) g).scale(scale, scale);
	}
  
	@Override
	public void setStrokeBold(Object g, boolean tf) {
		((Graphics2D) g).setStroke(tf ? strokeBold : strokeBasic);
	}

	@Override
	public void fillBackground(Object g, GenericColor bgcolor) {
		// not necessary
	}

	@Override
	public void setWindowParameters(int width, int height) {
		// not necessary
	}

	@Override
	public boolean canDoLineTo() {
		return true;
	}

	private GeneralPath path;
	
	@Override
	public void doStroke(Object g, boolean isBegin) {
		if (isBegin || path == null) {
			path = new GeneralPath();
		} else {
			((Graphics2D) g).draw(path);
			path = null;
		}
	}

	@Override
	public void lineTo(Object g, int x2, int y2) {
		path.lineTo(x2, y2);
	}


}
