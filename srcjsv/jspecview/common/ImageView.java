package jspecview.common;



import javajs.util.Lst;


class ImageView implements XYScaleConverter {
  
  /*
   * The viewPort is related to two coordinate systems, image and screen.
   * 
   * Image Coordinates:
   * 
   *  Note that the last displayed image pixel is (xView2, yView2)
   * 
   *    (0,0)
   *     /---------------------------/
   *     /   (view1)                 /
   *     /      /-------/            /
   *     /      /////////            /
   *     /      /////////            /
   *     /      /-------/            /
   *     /            (view2)        /
   *     /                           /
   *     /---------------------------/
   *                                (width,height)
   *                                
   * Pixel(Screen) Coordinates:
   * 
   *  Note that the last displayed screen pixel is (yPixels - 1, xPixels - 1)
   *    
   *     /---------------------------/
   *     /   (pixel0)                /
   *     /      /-------/            /
   *     /      ///////// yPixels    /
   *     /      /////////            /
   *     /      /-------/            /
   *     /       xPixels             /
   *     /                           /
   *     /---------------------------/
   *                                 
   * 
   * 
   */
  private int[] buf2d;
  private double grayFactorLast;
	private double averageGray;

	int xPixel0;
	int yPixel0, xPixel1, yPixel1;
	int imageWidth, imageHeight, xPixels, yPixels;
	int xPixelZoom1, yPixelZoom1, xPixelZoom2, yPixelZoom2;
	int xView1, yView1, xView2, yView2;
	double minX = Double.NaN, maxX, minY, maxY, minZ, maxZ;
	
	private ScaleData scaleData; // for paper Y axis
  
	void set(ScaleData view) {
    if (Double.isNaN(minX)) {
      minX = view.minX;
      maxX = view.maxX;
    }
    minZ = view.minY;
    maxZ = view.maxY;
    
    scaleData = new ScaleData();
  }

	void setZoom(int xPixel1, int yPixel1, int xPixel2, int yPixel2) {
    xPixelZoom1 = Math.min(xPixel1, xPixel2);
    yPixelZoom1 = Math.min(yPixel1, yPixel2);
    xPixelZoom2 = Math.max(xPixel1, xPixel2);
    yPixelZoom2 = Math.max(yPixel1, yPixel2);
    setView();
  }
  
	void setXY0(Spectrum spec, int xPixel, int yPixel) {
    xPixel0 = xPixel;
    yPixel0 = yPixel;
    xPixel1 = xPixel0 + xPixels - 1;
    yPixel1 = yPixel0 + yPixels - 1;
    setMinMaxY(spec);
  }
 
	void setPixelWidthHeight (int xPixels, int yPixels) {
    this.xPixels = xPixels;
    this.yPixels = yPixels;
  }
  
	void resetView() {
    xView1 = 0;
    yView1 = 0;
    xView2 = imageWidth - 1;
    yView2 = imageHeight - 1;
  }
  
	void setView() {
    if (xPixelZoom1 == 0)
      resetZoom();
    int x1 = toImageX(xPixelZoom1);    
    int y1 = toImageY(yPixelZoom1);    
    int x2 = toImageX(xPixelZoom2);    
    int y2 = toImageY(yPixelZoom2); 
    xView1 = Math.min(x1, x2);
    yView1 = Math.min(y1, y2);
    xView2 = Math.max(x1, x2);
    yView2 = Math.max(y1, y2);
    setScaleData();
    resetZoom();
  }

	void resetZoom() {
    xPixelZoom1 = xPixel0;
    yPixelZoom1 = yPixel0;
    xPixelZoom2 = xPixel1;
    yPixelZoom2 = yPixel1;
  }

	int toImageX(int xPixel) {
    return xView1 + (int) Math.floor((xPixel - xPixel0) / (xPixels - 1.0) * (xView2 - xView1));
  }

	int toImageY(int yPixel) {
    return yView1 + (int) Math.floor((yPixel - yPixel0) / (yPixels - 1.0) * (yView2 - yView1));
  }

	int toImageX0(int xPixel) {
    return Coordinate.intoRange((int) ((1.0 * xPixel - xPixel0) / (xPixels - 1) * (imageWidth - 1)), 0, imageWidth - 1);
  }

	int toImageY0(int yPixel) {
    return Coordinate.intoRange((int) ((1.0 * yPixel - yPixel0) / (yPixels - 1) * (imageHeight - 1)), 0, imageHeight - 1);
  }

	boolean isXWithinRange(int xPixel) {
    return (xPixel >= xPixel0 - 5 && xPixel < xPixel0 + xPixels + 5);
  }

	int toSubspectrumIndex(int yPixel) {
    return Coordinate.intoRange(imageHeight - 1 - toImageY(yPixel), 0, imageHeight - 1);
  }

  
	double toX0(int xPixel) {
    return maxX + (minX - maxX) * (fixX(xPixel) - xPixel0) / (xPixels - 1);
  }
  
	int toPixelX0(double x) {
    //TODO -- assumes reverse axis
    return xPixel1 - (int) ((x - minX) / (maxX - minX) * (xPixels - 1));
  }
  
	int toPixelY0(double ysub) {
    return yPixel1 - (int) (ysub / (imageHeight - 1) * (yPixels - 1));
  }
//
//	int toPixelX(int imageX) {
//    return xPixel0 + (int) ((xPixels - 1) *(1 - 1.0 *  imageX / (imageWidth - 1))); 
//  }

	int subIndexToPixelY(int subIndex) {
    // yView2 > yView1, but these are imageHeight - 1 - subIndex
    
    double f = 1.0 * (imageHeight - 1 - subIndex - yView1) / (yView2 - yView1);
    int y = yPixel0 + (int) (f * (yPixels - 1));
    return y; 
  }

	int fixSubIndex(int subIndex) {
    return Coordinate.intoRange(subIndex, imageHeight - 1 - yView2, imageHeight - 1 - yView1);
  }

  void setView0(int xp1, int yp1, int xp2, int yp2) {
    int x1 = toImageX0(xp1);
    int y1 = toImageY0(yp1);
    int x2 = toImageX0(xp2);
    int y2 = toImageY0(yp2);
    xView1 = Math.min(x1, x2);
    yView1 = Math.min(y1, y2);
    xView2 = Math.max(x1, x2);
    yView2 = Math.max(y1, y2);
    resetZoom();
  }
  
  /**
   * 
   * @param spec
   * @param forceNew 
   * @return image buffer
   */
  synchronized int[] get2dBuffer(Spectrum spec, boolean forceNew) {
    Lst<Spectrum> subSpectra = spec.getSubSpectra();
    if (subSpectra == null || !subSpectra.get(0).isContinuous())
      return null;
    Coordinate[] xyCoords = spec.getXYCoords();
    int nSpec = subSpectra.size();
    imageWidth = xyCoords.length;
    imageHeight = nSpec;
    double grayFactor = 255 / (maxZ - minZ);
    if (!forceNew && buf2d != null && grayFactor == grayFactorLast)
      return buf2d;
    grayFactorLast = grayFactor;
    int pt = imageWidth * imageHeight;
    int[] buf = (buf2d == null || buf2d.length != pt ? new int[pt] : buf2d);
    double totalGray = 0;
    for (int i = 0; i < nSpec; i++) {
      Coordinate[] points = subSpectra.get(i).xyCoords;
      if (points.length != xyCoords.length)
        return null;
//      double f = subSpectra.get(i).getUserYFactor();
//      if (f !=1)
//      	System.out.println("???? imageveiw");
      for (int j = 0; j < xyCoords.length; j++) {
        double y = points[j].getYVal();
        int gray = 255 - Coordinate.intoRange((int) ((y/* * f*/ - minZ) * grayFactor), 0, 255); 
        buf[--pt] = gray;
        totalGray += gray;
      }
    }
    averageGray = (1 - totalGray / (imageWidth * imageHeight) / 255);
    System.out.println ("Average gray = " + averageGray);
    return (buf2d = buf);
  }
  
  private static final double DEFAULT_MIN_GRAY = 0.05;
	private static final double DEFAULT_MAX_GRAY = 0.30;
	
	int[] adjustView (Spectrum spec, ViewData view) {
  	//double minGray = 0.05;
  	//double maxGray = 0.20;
  	int i = 0;
  	boolean isLow = false;
  	
  	while (((isLow = (averageGray < DEFAULT_MIN_GRAY)) || averageGray > DEFAULT_MAX_GRAY) && i++ < 10) {
      view.scaleSpectrum(-2, isLow ? 2 : 0.5);
      set(view.getScale());
      get2dBuffer(spec, false); 
  	} 
  	return buf2d;
  }
	
	int[] getBuffer() {
		return buf2d;
	}

	void setMinMaxY(Spectrum spec) {
    Lst<Spectrum> subSpectra = spec.getSubSpectra();
    Spectrum spec0 = subSpectra.get(0); 
    maxY = spec0.getY2D();
    minY = subSpectra.get(subSpectra.size() - 1).getY2D();
    if (spec0.y2DUnits.equalsIgnoreCase("Hz")) {
    	maxY /= spec0.freq2dY;
    	minY /= spec0.freq2dY;
    }
    setScaleData();
	}


	private void setScaleData() {
    scaleData.minY = minY;
    scaleData.maxY = maxY;
    scaleData.setYScale(toY(yPixel0), toY(yPixel1), false, false);
	}

	// XYScaleConverter interface
	
	@Override
	public int fixX(int xPixel) {
    return (xPixel < xPixel0 ? xPixel0 : xPixel > xPixel1 ? xPixel1 : xPixel);
  }

	@Override
	public int fixY(int yPixel) {
		return Coordinate.intoRange(yPixel, yPixel0, yPixel1);
	}

	@Override
	public ScaleData getScale() {
		return scaleData;
	}
	
  @Override
  public double toX(int xPixel) {
    return maxX + (minX - maxX) * toImageX(fixX(xPixel)) / (imageWidth - 1);
  }
  
	@Override
	public double toY(int yPixel) {
		int isub = toSubspectrumIndex(yPixel);
    return maxY + (minY - maxY) * isub / (imageHeight - 1);
	}

  @Override
  public int toPixelX(double x) {
    double x0 = toX(xPixel0);
    double x1 = toX(xPixel1);
    return xPixel0 + (int) ((x - x0) / (x1 - x0) * (xPixels - 1));
  }
  
	@Override
	public int toPixelY(double y) {
		double f = (y - scaleData.minYOnScale) / (scaleData.maxYOnScale - scaleData.minYOnScale); 
		return (int) (yPixel0 + f * yPixels);
	}

	@Override
	public int getXPixels() {
  	return xPixels;
  }
  
	@Override
	public int getYPixels() {
  	return yPixels;
  }
  
  @Override
  public int getXPixel0() {
  	return xPixel0;
  }
  
}