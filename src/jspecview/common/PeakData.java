package jspecview.common;

import java.util.Map;

import javajs.util.DF;


import jspecview.common.Annotation.AType;

/**
 * 
 * a data structure for peak lists
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *
 */
public class PeakData extends MeasurementData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PeakData(AType type, Spectrum spec) {
		super(type, spec);
	}

	private double thresh;

	private double minY;

	private double maxY;
	public double getThresh() {
		return thresh;
	}
	
	private final static String[] HNMR_HEADER = new String[] { "peak", "shift/ppm", "intens" , "shift/hz", "diff/hz", "2-diff", "3-diff" };

	@Override
	public String[] getDataHeader() {
		return (spec.isHNMR() ? HNMR_HEADER : new String[] { "peak", spec.getXUnits(), spec.getYUnits() }); 		
	}


	@Override
	public String[][] getMeasurementListArray(String units) {
		String[][] data = new String[size()][];
		double[] last = new double[] {-1e100, 1e100, 1e100};
		double[] ddata;
		for (int pt = 0, i = size(); --i >= 0; pt++) {
			ddata = spec.getPeakListArray(get(i), last, maxY);
			if (ddata.length == 2)
				data[pt] = new String[] {
					"" + (pt + 1),  
					DF.formatDecimalDbl(ddata[0], 2),
					DF.formatDecimalDbl(ddata[1], 4) 
				};
			else // 1HNMR
				data[pt] = new String[] {
					"" + (pt + 1), 
					DF.formatDecimalDbl(ddata[0], 4),
					DF.formatDecimalDbl(ddata[1], 4), 
					DF.formatDecimalDbl(ddata[2], 2), 
					(ddata[3] == 0 ? "" : DF.formatDecimalDbl(ddata[3], 2)),
					(ddata[4] == 0 ? "" : DF.formatDecimalDbl(ddata[4], 2)),
					(ddata[5] == 0 ? "" : DF.formatDecimalDbl(ddata[5], 2))
				};
		}
		return data;
	}
	
	@Override
	public double[][] getMeasurementListArrayReal(String units) {
		double[][] data = new double[size()][];
		double[] last = new double[] {-1e100, 1e100, 1e100};
		for (int pt = 0, i = size(); --i >= 0; pt++)
			data[pt] = spec.getPeakListArray(get(i), last, maxY);
		return data;
	}

	@Override
	public void getInfo(Map<String, Object> info) {
		info.put("interpolation", myParams.peakListInterpolation);
		info.put("threshold", Double.valueOf(myParams.peakListThreshold));
		super.getInfo(info);
	}

	public void setPeakList(Parameters p, int precision, ScaleData view) {
		this.precision = (precision == Integer.MIN_VALUE ? spec.getDefaultUnitPrecision() : precision);
		Coordinate[] xyCoords = spec.getXYCoords();
		if (xyCoords.length < 3)
			return;
		clear();
		if (p != null) {
  		myParams.peakListInterpolation = p.peakListInterpolation;
	  	myParams.peakListThreshold = p.peakListThreshold;
		}
		boolean doInterpolate = (myParams.peakListInterpolation.equals("parabolic"));
		boolean isInverted = spec.isInverted();
		minY = view.minYOnScale;
		maxY = view.maxYOnScale;
		double minX = view.minXOnScale;
		double maxX = view.maxXOnScale;
		thresh = myParams.peakListThreshold;
		if (Double.isNaN(thresh))
			thresh = myParams.peakListThreshold = (minY + maxY) / 2;
		double yLast = 0;
		double[] y3 = new double[] { xyCoords[0].getYVal(),
				yLast = xyCoords[1].getYVal(), 0 };
		int n = 0;
		if (isInverted)
			for (int i = 2; i < xyCoords.length; i++) {
				double y = y3[i % 3] = xyCoords[i].getYVal();
				if (yLast < thresh && y3[(i - 2) % 3] > yLast && yLast < y) {
					double x = (doInterpolate ? Coordinate.parabolicInterpolation(
							xyCoords, i - 1) : xyCoords[i - 1].getXVal());
					if (x >= minX || x <= maxX) {
						PeakPick m = new PeakPick().setValue(x, y, spec, null, 0);
						addLast(m);
						if (++n == 100)
							break;
					}
				}
				yLast = y;
			}
		else
			for (int i = 2; i < xyCoords.length; i++) {
				double y = y3[i % 3] = xyCoords[i].getYVal();
				if (yLast > thresh && y3[(i - 2) % 3] < yLast && yLast > y) {
					double x = (doInterpolate ? Coordinate.parabolicInterpolation(
							xyCoords, i - 1) : xyCoords[i - 1].getXVal());
					if (x >= minX && x <= maxX) {
						PeakPick m = new PeakPick().setValue(x, y, spec, DF.formatDecimalDbl(x, precision), x);
						addLast(m);
						if (++n == 100)
							break;
					}
				}
				yLast = y;
			}
	}
	


}
