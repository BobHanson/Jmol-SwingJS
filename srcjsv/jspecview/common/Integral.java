/**
 * 
 */
package jspecview.common;


public class Integral extends Measurement {

	public Integral setInt(double x1, double y1, Spectrum spec, double value, double x2, double y2) {
		setA(x1, y1, spec, "", false, false, 0, 6);
		setPt2(x2, y2);
    setValue(value);
		return this;
  }
  
	
}