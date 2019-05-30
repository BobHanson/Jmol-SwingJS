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

package org.jmol.awtjs2d;

import javajs.api.GenericColor;
import org.jmol.awtjs.swing.Color;
import org.jmol.awtjs.swing.Font;
import javajs.util.CU;


import org.jmol.api.GenericGraphics;

/**
 * generic 2D drawing methods -- JavaScript version
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class JsG2D implements GenericGraphics {

	private int windowWidth;
	private int windowHeight;

  public JsG2D() {
	}

  @Override
	public GenericColor getColor4(int r, int g, int b, int a) {
    return Color.get4(r, g, b, a);
  }
  
  @Override
	public GenericColor getColor3(int r, int g, int b) {
    return Color.get3(r, g, b);
  }
  
  @Override
	public GenericColor getColor1(int rgb) {
    return Color.get1(rgb);
  }

	@Override
	public Object newGrayScaleImage(Object context, Object image, int width, int height, int[] grayBuffer) {
		return Platform.Jmol().newGrayScaleImage(context, image, width, height, grayBuffer);
	}

	@Override
	public void drawGrayScaleImage(Object g, Object image, int destX0, int destY0,
			int destX1, int destY1, int srcX0, int srcY0, int srcX1, int srcY1) {

		float iw, ih;
		/**
		 * @j2sNative
		 * 
		 * iw = image.w;
		 * ih = image.h;
		 * 
		 */ 
		{
			ih = iw = 0;
		}

		float dw = (destX1 - destX0 + 1);
		float dh  = (destY1 - destY0 + 1);
		float sw = (srcX1 - srcX0 + 1);
		float sh = (srcY1 - srcY0 + 1);
		float x = -srcX0 * dw / sw;
		float w = iw * dw / sw;
		float y = -srcY0* dh / sh;
		float h = ih * dh / sh;

		/**
		 * @j2sNative
		 * 
		 * image.width = w;
		 * image.height = h;
		 * var div = image.div;
		 * var layer = image.layer;
		 * layer.style.left = destX0 + "px";
		 * layer.style.top = destY0 + "px";
		 * layer.style.width = dw + "px";
		 * layer.style.height = dh+ "px";
		 * div.style.left= x + "px";
		 * div.style.top = y + "px";
		 * div.style.width = w + "px";
		 * div.style.height = h + "px";
		 */
		{
			System.out.println(x + y + h + w);
		}
	}

	@Override
	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
	  // g is a canvas context
		@SuppressWarnings("unused")
		boolean inPath = this.inPath;
		/**
		 * @j2sNative
		 * 
		 *            if (!inPath) g.beginPath(); 
		 *            g.moveTo(x0, y0); 
		 *            g.lineTo(x1, y1); 
		 *            if (!inPath) g.stroke();
		 * 
		 */
		{}
	}

	@Override
	public void drawCircle(Object g, int x, int y, int diameter) {
		/**
		 * @j2sNative
		 * 
		 *    var r = diameter/2;
		 * 		g.beginPath();
		 *    g.arc(x + r, y + r, r, 0, 2 * Math.PI, false);
		 *    g.stroke();
		 */
		{	
		}
		
	}

	@Override
	public void drawPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		doPoly(g, ayPoints, axPoints, nPoints, false);
	}

	/**
	 * @param g 
	 * @param axPoints  
	 * @param ayPoints 
	 * @param nPoints 
	 * @param doFill 
	 */
	private void doPoly(Object g, int[] axPoints, int[] ayPoints, int nPoints,
			boolean doFill) {
		/**
		 * @j2sNative
		 * 
		 * g.beginPath();
		 * g.moveTo(axPoints[0], ayPoints[0]);
		 * 
		 * for (var i = 1; i < nPoints; i++)
		 *   g.lineTo(axPoints[i], ayPoints[i]);
     * if (doFill)
     *   g.fill();
     * else
     *   g.stroke();
		 * 
		 */
		{
		}
	}

	@Override
	public void drawRect(Object g, int x, int y, int width,
			int height) {
		/**
		 * @j2sNative
		 * 
		 * g.beginPath();
     * g.rect(x ,y, width, height);
     * g.stroke();
		 * 
		 */
		{
		}
	}

	@Override
	public void drawString(Object g, String s, int x, int y) {
		/**
		 * @j2sNative
		 * 
		 * g.fillText(s,x,y);
		 */
		{
			
		}
	}

	@Override
	public void drawStringRotated(Object g, String s, int x, int y, double angle) {
		// n/a (printing only)
	}

	boolean isShifted;// private, but only JavaScript
	
	@Override
	public void fillBackground(Object g, GenericColor bgcolor) {
		if (bgcolor == null) {
			/**
			 *
			 *  reduce antialiasing, thank you, http://www.rgraph.net/docs/howto-get-crisp-lines-with-no-antialias.html
			 *  
			 * @j2sNative
			 * 
			 * if (!this.isShifted) {
			 *   g.translate(-0.5, -0.5);
			 *   this.isShifted = true;
			 * }  
			 * g.clearRect(0,0, this.windowWidth, this.windowHeight);
			 * return;
			 * 
			 */
			{				
			}
		}
		setGraphicsColor(g, bgcolor);
		fillRect(g, 0, 0, windowWidth, windowHeight);
	}

	@Override
	public void fillCircle(Object g, int x, int y, int diameter) {
		/**
		 * @j2sNative
		 * 
		 *    var r = diameter/2;
		 * 		g.beginPath();
		 *    g.arc(x + r, y + r, r, 0, 2 * Math.PI, false);
		 *    g.fill();
		 */
		{	
		}
	
	}

	@Override
	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		doPoly(g, ayPoints, axPoints, nPoints, true);
	}

	@Override
	public void fillRect(Object g, int x, int y, int width, int height) {
		/**
		 * @j2sNative
		 * 
		 * g.fillRect(x, y, width, height);
		 * 
		 */
		{
		}
	}

	@Override
	public void setGraphicsColor(Object g, GenericColor c) {
		String s = CU.toCSSString(c);
		/**
		 * @j2sNative
		 * 
		 * g.fillStyle = g.strokeStyle = s;
		 */
		{
			System.out.println(s);
		}
	}

	@Override
	public Font setFont(Object g, Font font) {
		String s = font.getInfo();
		int pt = s.indexOf(" ");
		s = s.substring(0, pt) + "px" + s.substring(pt);
		/**
		 * @j2sNative
		 * 
		 * g.font = s;
		 */
		{
		}
		return  font;
	}

	@Override
	public void setStrokeBold(Object g, boolean tf) {
		/**
		 * @j2sNative
		 *
		 * g.lineWidth = (tf ? 2 : 1);
		 * 
		 */
		{

		}

	}

	@Override
	public void setWindowParameters(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	@Override
	public void translateScale(Object g, double x, double y, double scale) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canDoLineTo() {
		return true;
	}

	boolean inPath;
	
	@Override
	public void doStroke(Object g, boolean isBegin) {
		inPath = isBegin;
		/**
		 *  
		 * @j2sNative
		 * 
		 * if (isBegin) {
		 * 	 g.beginPath();
		 * } else {
		 *   g.stroke();
		 * }
		 * 
		 */
		{}
	}

	@Override
	public void lineTo(Object g, int x2, int y2) {
		/**
		 * @j2sNative
		 * 
		 * g.lineTo(x2, y2);
		 * 
		 */
		{}
	}

}
