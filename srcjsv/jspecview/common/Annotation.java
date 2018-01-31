/* Copyright (c) 2002-2008 The University of the West Indies
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

// CHANGES to 'IntegrationRatio.java' - Integration Ratio Representation
// University of the West Indies, Mona Campus
// 24-09-2011 jak - Created class as an extension of the Coordinate class
//					to handle the integration ratio value.

package jspecview.common;

import org.jmol.api.GenericGraphics;

import javajs.api.GenericColor;
import javajs.util.CU;
import javajs.util.Lst;



/**
 * The <code>Annotation</code> class stores the spectral x and pixel y values of an
 * annotation text along with its text
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class Annotation extends Coordinate {
  public String text = "";
  protected boolean isPixels;
  boolean is2D;
  int offsetX;
  int offsetY;
  Spectrum spec;
  
  public enum AType { Integration, PeakList, Measurements, OverlayLegend, Views, NONE }
  
  protected Annotation setA(double x, double y, Spectrum spec, String text, boolean isPixels, boolean is2D, int offsetX, int offsetY) {
  	set(x, y);
    this.spec = spec;
    this.text = text;
    this.isPixels = isPixels;
    this.is2D = is2D;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    return this;
  }

	Annotation setSpec(Spectrum spec) {
		this.spec = spec;
		return this;
	}

	void addSpecShift(double dx) {
		setXVal(getXVal() + dx);
	}
	
  boolean isPixels() {
    return isPixels;
  }

  /**
   * Overrides Objects toString() method
   * 
   * @return the String representation of this coordinate
   */
  @Override
  public String toString() {
    return "[" + getXVal() + ", " + getYVal() + "," + text + "]";
  }


	public static Annotation getColoredAnnotation(GenericGraphics g2d,
			Spectrum spec, Lst<String> args, Annotation lastAnnotation) {
		String arg;
		int xPt = 0;
		int yPt = 1;
		int colorPt = 2;
		int textPt = 3;
		int nArgs = args.size();
		try {
			switch (nArgs) {
			default:
				return null;
			case 1:
				arg = args.get(0);
				xPt = yPt = -1;
				if (arg.charAt(0) == '\"') {
					textPt = 0;
					colorPt = -1;
				} else {
					colorPt = 0;
					textPt = -1;
				}
				break;
			case 2:
				xPt = yPt = -1;
				arg = args.get(0);
				if (arg.charAt(0) == '\"') {
					textPt = 0;
					colorPt = 1;
				} else {
					colorPt = 0;
					textPt = 1;
				}
				break;
			case 3:
			case 4:
				// x y "text" or x y color
				// x y color "text" or x y "text" color
				arg = args.get(2);
				if (arg.charAt(0) == '\"') {
					textPt = 2;
					colorPt = (nArgs == 4 ? 3 : -1);
				} else {
					colorPt = 2;
					textPt = (nArgs == 4 ? 3 : -1);
				}
				arg = args.get(2);
				if (arg.charAt(0) == '\"') {
					textPt = 2;
					colorPt = -1;
				} else {
					colorPt = 2;
					textPt = -1;
				}
			}
			if (lastAnnotation == null
					&& (xPt < 0 || yPt < 0 || textPt < 0 || colorPt < 0))
				return null;
			double x = (xPt < 0 ? lastAnnotation.getXVal() : Double.valueOf(
					args.get(xPt)).doubleValue());
			double y = (yPt < 0 ? lastAnnotation.getYVal() : Double.valueOf(
					args.get(yPt)).doubleValue());
			GenericColor color = (colorPt < 0 ? ((ColoredAnnotation) lastAnnotation)
					.getColor() : g2d.getColor1(CU.getArgbFromString(args
					.get(colorPt))));
			String text;
			if (textPt < 0) {
				text = lastAnnotation.text;
			} else {
				text = args.get(textPt);
				if (text.charAt(0) == '\"')
					text = text.substring(1, text.length() - 1);
			}
			return new ColoredAnnotation().setCA(x, y, spec, text, color, false, false, 0, 0);
		} catch (Exception e) {
			return null;
		}
	}

}
