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

package jspecview.common;

import javajs.api.GenericColor;



/**
 * ColoredAnnotation is a label on the spectrum; not an integralRegion
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class ColoredAnnotation extends Annotation {

  private GenericColor color;

  public GenericColor getColor() {
    return color;
  }

  public ColoredAnnotation() {
  }
  
  public ColoredAnnotation setCA(
	double x, double y, Spectrum spec, String text, GenericColor color,
      boolean isPixels, boolean is2D, int offsetX, int offsetY) {
    setA(x, y, spec, text, isPixels, is2D, offsetX, offsetY);
    this.color = color;
    return this;
  }

}
