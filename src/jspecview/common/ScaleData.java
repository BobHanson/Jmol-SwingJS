package jspecview.common;


import java.util.Map;

import javajs.util.Lst;



/**
 * Stores information
 * about scale and range that <code>JSVPanel</code> needs to to display a
 * graph with a single plot. (For graphs that require multiple plots to be
 * overlaid.
 */
public class ScaleData {

  private final static int[] NTICKS = { 2, 5, 10, 10 };
  private final static double[] LOGTICKS = { Math.log10(2), Math.log10(5), 0, 1 };

	private double initMinYOnScale;
	private double initMaxYOnScale;
	private double initMinY;
	private double initMaxY;

	// values for the current expansion in X:
	
  int startDataPointIndex;
  int endDataPointIndex;
  int pointCount;
  double minX;
  double maxX;

	/**
   * First grid x value
   */
  double firstX = Double.NaN;
  

  /**
   * the minimum X value on the scale, usually minX
   */
  public double minXOnScale;

  /**
   * the maximim X value on the scale, usually maxX
   */
  public double maxXOnScale;

	/**
	 * for NMR, the adjustment in chemical shift applied using SHIFTX or SETPEAK or SETX
	 */
	
	double specShift;
  
  /**
   * 
   * The precision (number of decimal places) of the X and Y values
   */
	public int[] precision = new int[2]; 
  public int exportPrecision[] = new int[2];
  


  /**
   * The step values for the X and Y scales
   */
  public double[] steps = new double[2];

  /**
   * the minor tick counts for the X and Y scales
   */
	int[] minorTickCounts = new int[2];

  // Y variables

  public double minYOnScale;
  public double maxYOnScale;

  /**
   * The minimum Y value in the list of coordinates of the graph
   */
  double minY;

  /**
   * The maximum Y value in the list of coordinates of the graph
   */
  double maxY;

	boolean isShiftZoomedY;

  double spectrumScaleFactor = 1;
  double spectrumYRef = 0;
  double userYFactor = 1;  
	double firstY;
  double minY2D, maxY2D;

  private double xFactorForScale;
  private double yFactorForScale;
  

  ScaleData() {
	}

	ScaleData(int iStart, int iEnd) {
		startDataPointIndex = iStart;
		endDataPointIndex = iEnd;
		pointCount = endDataPointIndex - startDataPointIndex + 1;
	}

	/**
	 * Calculates values that <code>JSVPanel</code> needs in order to render a
	 * graph, (eg. scale, min and max values) and stores the values in the class
	 * <code>ScaleData</code>. 
	 * 
	 * @param coords
	 *          the array of coordinates
	 * @param start
	 *          the start index
	 * @param end
	 *          the end index
	 * @param isContinuous 
	 * @param isInverted 
	 * @returns an instance of <code>ScaleData</code>
	 */
	public ScaleData(Coordinate[] coords, int start, int end, boolean isContinuous, boolean isInverted) {
		minX = Coordinate.getMinX(coords, start, end);
		maxX = Coordinate.getMaxX(coords, start, end);
		minY = Coordinate.getMinY(coords, start, end);
		if (minY > 0 && !isContinuous)
			minY = 0; // assumed to be MS data -- we want 0 to be at the bottom
		maxY = Coordinate.getMaxY(coords, start, end);
		setScale(isContinuous, isInverted);
	}

	void setScale(boolean isContinuous, boolean isInverted) {
    setXScale();
    if (!isContinuous)
      maxXOnScale += steps[0] / 2; // MS should not end with line at end
    setYScale(minY, maxY, true, isInverted);
  }

	private void setXScale() {
    double xStep = setScaleParams(minX, maxX, 0);
    firstX = Math.floor(minX / xStep) * xStep;
    if (Math.abs((minX - firstX) / xStep) > 0.0001)
      firstX += xStep;
    minXOnScale = minX;
    maxXOnScale = maxX;
  }

  boolean isYZeroOnScale() {
    return (minYOnScale < spectrumYRef && maxYOnScale > spectrumYRef);
  }

	void setYScale(double minY, double maxY, boolean setScaleMinMax,
			boolean isInverted) {
		if (minY == 0 && maxY == 0)
			maxY = 1;
		if (isShiftZoomedY) {
			minY = minYOnScale;
			maxY = maxYOnScale;
		}			
		double yStep = setScaleParams(minY, maxY, 1);
		double dy = (isInverted ? yStep / 2 : yStep / 4);
		double dy2 = (isInverted ? yStep / 4 : yStep / 2);
		if (!isShiftZoomedY) {
  		minYOnScale = (minY == 0 ? 0 : setScaleMinMax ? dy * Math.floor(minY / dy)
	  			: minY);
		  maxYOnScale = (setScaleMinMax ? dy2 * Math.ceil(maxY * 1.05 / dy2) : maxY);
		}
		firstY = (minY == 0 ? 0 : Math.floor(minY / dy) * dy);
		if (minYOnScale < 0 && maxYOnScale > 0) {
			// set a Y value to be 0;
			firstY = 0;
			while (firstY - yStep > minYOnScale)
				firstY -= yStep;
		} else if (minYOnScale != 0 && Math.abs((minY - firstY) / dy) > 0.0001) {
			firstY += dy;
		}

		if (setScaleMinMax) {
			initMinYOnScale = minYOnScale;
			initMaxYOnScale = maxYOnScale;
			initMinY = minY;
			initMaxY = maxY;
		}
	}

  void scale2D(double f) {
		double dy = maxY - minY;
		if (f == 1) {
			maxY = initMaxY;
			minY = initMinY;
			return;
		}
		maxY = minY + dy / f;
	}

  void setXRange(double x1, double x2) {
    minX = x1;
    maxX = x2;
    setXScale();
  }

  private static int getXRange(int i, Coordinate[] xyCoords, double initX, double finalX, int iStart, int iEnd, int[] startIndices, int[] endIndices) {
    int index = 0;
    int ptCount = 0;
    for (index = iStart; index <= iEnd; index++) {
      if (xyCoords[index].getXVal() >= initX) {
        startIndices[i] = index;
        ptCount = 1;
        break;
      }
    }

    // determine endDataPointIndex
    // BH --- this was reproducibly off by one
    while (++index <= iEnd && xyCoords[index].getXVal() <= finalX) {
        ptCount++;
    }
    endIndices[i] = startIndices[i] + ptCount - 1;
    return ptCount;
  }

  /**
   * sets hashNums, formatters, and steps 
   * @param min
   * @param max
   * @param i   0 for X; 1 for Y
   * @return  steps[i]
   */
	private double setScaleParams(double min, double max, int i) {
		// nDiv will be 14, which seems to work well
    double dx = (max == min ? 1 : Math.abs(max - min) / 14);
		double log = Math.log10(Math.abs(dx));
		
		int exp = (int) Math.floor(log);
		
		// set number of decimal places
		exportPrecision[i] = exp;
		precision[i] = (exp <= 0 ? Math.min(8, 1 - exp) : exp > 3 ? -2 : 0); 

		// set number formatter
//		String hash1 = "0.00000000"; // 8
//		String hash = (
//      0 --> 0.0; 1 --> 0.00, etc.
//				exp <= 0 ? hash1.substring(0, Math.min(hash1.length(), Math.abs(exp) + 3))
//				: exp > 3 ? "0.00E0" //-2 
//				: "#"); // 0
//		formatters[i] = JSVTextFormat.getDecimalFormat(hash);
		
		// set step for numbers
    int j = 0;
    double dec = Math.pow(10, log - exp);
    while (dec > NTICKS[j]) {
      j++;
    }
    steps[i] = Math.pow(10, exp) * NTICKS[j];
    
    // set minor ticks count
		log = Math.log10(Math.abs(steps[i] * 1.0001e5));
		double mantissa = log - Math.floor(log);
		int n = 0;
		for (j = 0; j < NTICKS.length; j++)
			if (Math.abs(mantissa - LOGTICKS[j]) < 0.001) {
				n = NTICKS[j];
				break;
			}
		minorTickCounts[i] = n;
		
		return steps[i];
		
  }

	/**
   * Determines if the x coordinate is within the range of coordinates in the
   * coordinate list
   * 
   * @param x
   * @return true if within range
   */
  boolean isInRangeX(double x) {
    return (x >= minX && x <= maxX);
  }

	void addSpecShift(double dx) {
		specShift += dx;
		minX += dx;
		maxX += dx;
		minXOnScale += dx;
		maxXOnScale += dx;
		firstX += dx;
	}

//  void setMinMaxY2D(JmolList<JDXSpectrum> subspectra) {
//    minY2D = Double.MAX_VALUE;
//    maxY2D = -Double.MAX_VALUE;
//    for (int i = subspectra.size(); --i >= 0; ) {
//      double d = subspectra.get(i).getY2D();
//      if (d < minY2D)
//        minY2D = d;
//      else if (d > maxY2D)
//        maxY2D = d;
//    }
//  }

	Map<String, Object> getInfo(Map<String, Object> info) {
		info.put("specShift", Double.valueOf(specShift));
		info.put("minX", Double.valueOf(minX));
		info.put("maxX", Double.valueOf(maxX));
		info.put("minXOnScale", Double.valueOf(minXOnScale));
		info.put("maxXOnScale", Double.valueOf(maxXOnScale));
		info.put("minY", Double.valueOf(minY));
		info.put("maxY", Double.valueOf(maxY));
		info.put("minYOnScale", Double.valueOf(minYOnScale));
		info.put("maxYOnScale", Double.valueOf(maxYOnScale));
		info.put("minorTickCountX", Integer.valueOf(minorTickCounts[0]));
		info.put("xStep", Double.valueOf(steps[0]));
		return info;
	}

	void setMinMax(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}
	
  double toX(int xPixel, int xPixel1, boolean drawXAxisLeftToRight) {
		return toXScaled(xPixel, xPixel1, drawXAxisLeftToRight, xFactorForScale);
	}

	double toX0(int xPixel, int xPixel0, int xPixel1, boolean drawXAxisLeftToRight) {
		return toXScaled(xPixel, xPixel1, drawXAxisLeftToRight,
				(maxXOnScale - minXOnScale) / (xPixel1 - xPixel0));
	}

	private double toXScaled(int xPixel, int xPixel1, boolean drawXAxisLeftToRight,
			double factor) {
		return (
				drawXAxisLeftToRight ? maxXOnScale - (xPixel1 - xPixel) * factor 
						                 : minXOnScale + (xPixel1 - xPixel) * factor
						);
	}

	int toPixelX(double dx, int xPixel0, int xPixel1,
			boolean drawXAxisLeftToRight) {
		return toPixelXScaled(dx, xPixel0, xPixel1, drawXAxisLeftToRight, xFactorForScale);
	}

	int toPixelX0(double dx, int xPixel0, int xPixel1, boolean drawXAxisLeftToRight) {
		return toPixelXScaled(dx, xPixel0, xPixel1, drawXAxisLeftToRight, (maxXOnScale - minXOnScale) / (xPixel1 - xPixel0));
	}

	private int toPixelXScaled(double dx, int xPixel0, int xPixel1, boolean drawXAxisLeftToRight,
			double factor) {
		int x = (int) ((dx - minXOnScale) / factor);
		return (drawXAxisLeftToRight ? xPixel0 + x : xPixel1 - x);
	}

	double toY(int yPixel, int yPixel0) {		
		return maxYOnScale + (yPixel0 - yPixel) * yFactorForScale;
	}

	double toY0(int yPixel, int yPixel0, int yPixel1) {
		double factor = (maxYOnScale - minYOnScale) / (yPixel1 - yPixel0);
		double y = maxYOnScale + (yPixel0 - yPixel) * factor;
		return Math.max(minYOnScale, Math.min(y, maxYOnScale));
	}

	int toPixelY(double yVal, int yPixel1) {
		return (Double.isNaN(yVal) ? Integer.MIN_VALUE
				: yPixel1
						- (int) (((yVal - spectrumYRef) * userYFactor + spectrumYRef - minYOnScale) / yFactorForScale));
	}

	int toPixelY0(double y, int yPixel0, int yPixel1) {
		double factor = (maxYOnScale - minYOnScale) / (yPixel1 - yPixel0);
		return (int) (yPixel0 + (maxYOnScale - y) / factor);
	}

	void setXYScale(int xPixels, int yPixels, boolean isInverted) {
		double yRef = spectrumYRef;
		double f = spectrumScaleFactor;
		boolean useInit = (f != 1 || isShiftZoomedY); 
		double minY = (useInit ? initMinYOnScale :  this.minY);
		double maxY = (useInit ? initMaxYOnScale :  this.maxY);
		if (useInit && yRef < minY)
			yRef = minY;
		if (useInit && yRef > maxY)
			yRef = maxY;
		setYScale((minY - yRef) / f + yRef, (maxY - yRef) / f + yRef, f == 1, isInverted);
  	xFactorForScale = (maxXOnScale - minXOnScale) / (xPixels - 1);
  	yFactorForScale = (maxYOnScale - minYOnScale) / (yPixels - 1);
	}

	static void copyScaleFactors(ScaleData[] sdFrom, ScaleData[] sdTo) {
		for (int i = 0; i < sdFrom.length; i++) {
			sdTo[i].spectrumScaleFactor = sdFrom[i].spectrumScaleFactor;
			sdTo[i].spectrumYRef = sdFrom[i].spectrumYRef;
			sdTo[i].userYFactor = sdFrom[i].userYFactor;
			sdTo[i].specShift = sdFrom[i].specShift;
			sdTo[i].isShiftZoomedY = sdFrom[i].isShiftZoomedY;
		}
	}
	
	static void copyYScales(ScaleData[] sdFrom, ScaleData[] sdTo) {
		for (int i = 0; i < sdFrom.length; i++) {
			sdTo[i].initMinYOnScale = sdFrom[i].initMinYOnScale;
			sdTo[i].initMaxYOnScale = sdFrom[i].initMaxYOnScale;
			sdTo[i].minY = sdFrom[i].minY;
			sdTo[i].maxY = sdFrom[i].maxY;
			if (sdFrom[i].isShiftZoomedY) {
				// precalculated from shift-zoomBox1D or pin1Dy
				sdTo[i].isShiftZoomedY = true;
			  sdTo[i].minYOnScale = sdFrom[i].minYOnScale;
				sdTo[i].maxYOnScale = sdFrom[i].maxYOnScale;
			}
		}
	}

  /**
   * 
   * @param graphsTemp
   * @param initX
   * @param finalX
   * @param minPoints
   * @param startIndices  to fill
   * @param endIndices    to fill
   * @return true if OK
   */
	static boolean setDataPointIndices(Lst<Spectrum> graphsTemp,
			double initX, double finalX, int minPoints, int[] startIndices,
			int[] endIndices) {
		int nSpectraOK = 0;
		int nSpectra = graphsTemp.size();
		for (int i = 0; i < nSpectra; i++) {
			Coordinate[] xyCoords = graphsTemp.get(i).getXYCoords();
			if (ScaleData.getXRange(i, xyCoords, initX, finalX, 0,
					xyCoords.length - 1, startIndices, endIndices) >= minPoints)
				nSpectraOK++;
		}
		return (nSpectraOK == nSpectra);
	}

  
	static void fixScale(Map<Double, String> map) {
		if (map.isEmpty())
			return;
		while (true) {
			for (Map.Entry<Double, String> entry : map.entrySet()) {
				String s = entry.getValue();
				int pt = s.indexOf("E");
				if (pt >= 0)
					s = s.substring(0, pt);
				if (s.indexOf(".") < 0)
					return;
				if (!s.endsWith("0") && !s.endsWith("."))
					return;
			}
			for (Map.Entry<Double, String> entry : map.entrySet()) {
				String s = entry.getValue();
				int pt = s.indexOf("E");
				if (pt >= 0)
  				entry.setValue(s.substring(0, pt - 1) + s.substring(pt));
				else
  				entry.setValue(s.substring(0, s.length() - 1));
			}			
		}
	}

	void scaleBy(double f) {
		if (isShiftZoomedY) {
			double center = (isYZeroOnScale() ? spectrumYRef : (minYOnScale + maxYOnScale) / 2);
			minYOnScale = center - (center - minYOnScale) / f;
			maxYOnScale = center - (center - maxYOnScale) / f;
		} else {
			spectrumScaleFactor *= f;	
		}
	}
}