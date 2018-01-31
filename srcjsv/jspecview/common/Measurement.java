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



/**
 * The <code>Measurement</code> class stores an annotation that is a measurement
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class Measurement extends Annotation {
  
	public static final int PT_XY1 = 1;
	public static final int PT_XY2 = 2;
	public static final int PT_INT_LABEL = -5;
	public static final int PT_ON_LINE1 = -1;
	public static final int PT_ON_LINE2 = -2;
	public static final int PT_ON_LINE = 0;
	
	private Coordinate pt2 = new Coordinate();
	protected double value;

	public Measurement setM1(double x, double y, Spectrum spec) {
		setA(x, y, spec, "", false, false, 0, 6);
		setPt2(getXVal(), getYVal());
		return this;
	}

	public Measurement copyM() {
		Measurement m = new Measurement();
		m.setA(getXVal(), getYVal(), spec, text, false, false, offsetX, offsetY);
  	m.setPt2(pt2.getXVal(), pt2.getYVal());
  	return m;
	}

	public Measurement setPt2(Spectrum spec, boolean doSetPt2) {
		this.spec = spec;
		 if (doSetPt2)
				setPt2(getXVal(), getYVal());
		 return this;
	}

	public void setPt2(double x, double y) {
		pt2.setXVal(x);
		pt2.setYVal(y);
		value = Math.abs(x - getXVal());
		text = spec.setMeasurementText(this);
  }
  
	public Spectrum getSpectrum() {
		return spec;
	}
  
	public void setValue(double value) {
  	this.value = value;
		text = spec.setMeasurementText(this);
  }

	public double getValue() {
  	return value;
  }
  
	public double getXVal2() {
		return pt2.getXVal();
	}

	public double getYVal2() {
		return pt2.getYVal();
	}

	@Override
	public void addSpecShift(double dx) {
    setXVal(getXVal() + dx);
    pt2.setXVal(pt2.getXVal() + dx);
	}

	public void setYVal2(double y2) {
		pt2.setYVal(y2);
	}

	public boolean overlaps(double x1, double x2) {
		return (Math.min(getXVal(), getXVal2()) < Math.max(x1, x2) 
    && Math.max(getXVal(), getXVal2()) > Math.min(x1, x2));
	}

  /**
   * Overrides Objects toString() method
   * 
   * @return the String representation of this coordinate
   */
  @Override
  public String toString() {
    return "[" + getXVal() + "," + pt2.getXVal() + "]";
  }


}
