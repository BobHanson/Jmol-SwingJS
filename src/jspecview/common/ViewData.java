package jspecview.common;

import javajs.util.Lst;





/**
 * Stores information that <code>GraphSet</code> needs 
 * to display a view with one or more spectra. 
 * 
 */
public class ViewData {

	private ScaleData[] scaleData;
	
	public ScaleData[] getScaleData() {
		return scaleData;
	}

	private ScaleData thisScale;
 
	public ScaleData getScale() {
		return thisScale;
	}

  private int nSpectra;
  private int iThisScale;
	private Lst<Spectrum> spectra;

	/**
	 * 
	 * @param spectra
	 *          an array of spectra
	 * @param yPt1
	 * @param yPt2
	 * @param startList
	 *          the start indices
	 * @param endList
	 *          the end indices
	 * @param isContinuous 
	 * @param is2D 
	 * @returns an instance of <code>MultiScaleData</code>
	 */
  public ViewData(Lst<Spectrum> spectra, double yPt1, double yPt2,
			int[] startList, int[] endList, boolean isContinuous, boolean is2D) {
		nSpectra = (is2D ? 1 : spectra.size());
		scaleData = new ScaleData[nSpectra];
		for (int j = 0; j < nSpectra; j++)
			scaleData[j] = new ScaleData(startList[j], endList[j]);
		init(spectra, yPt1, yPt2, isContinuous);
	}
  
  public ViewData(Lst<Spectrum> spectra, double yPt1, double yPt2, 
			boolean isContinuous) {
		// forced subsets
		nSpectra = spectra.size();
		int n = spectra.get(0).getXYCoords().length; // was - 1 
		scaleData = new ScaleData[1];
		scaleData[0] = new ScaleData(0, n - 1);
		init(spectra, yPt1, yPt2, isContinuous);
	}

  void init(Lst<Spectrum> spectra, 
  		double yPt1, double yPt2, boolean isContinuous) {
  	if (spectra == null)
  		spectra = this.spectra;
  	else
  		this.spectra = spectra;
		thisScale = scaleData[iThisScale = 0];
		for (int i = 0; i < scaleData.length; i++) {
			scaleData[i].userYFactor = spectra.get(i).getUserYFactor();
		  scaleData[i].spectrumYRef = spectra.get(i).getYRef(); // 0 or 100
		}
		resetScaleFactors();
		double minX = Coordinate.getMinX(spectra, this);
		double maxX = Coordinate.getMaxX(spectra, this);
		double minY = Coordinate.getMinYUser(spectra, this);
		double maxY = Coordinate.getMaxYUser(spectra, this);
  	
    if (yPt1 != yPt2) {
      minY = yPt1;
      maxY = yPt2;
      if (minY > maxY) {
        double t = minY;
        minY = maxY;
        maxY = t;
      }
    }
    boolean isInverted = spectra.get(0).isInverted();
		for (int i = 0; i < scaleData.length; i++) {
			scaleData[i].setMinMax(minX, maxX, minY, maxY);
      scaleData[i].setScale(isContinuous, isInverted);
		}
  }

  public void newSpectrum(Lst<Spectrum> spectra) {
		init(spectra, 0, 0, false);
	}

	public void setXRangeForSubSpectrum(Coordinate[] xyCoords) {
  	// forced subspectra only
    setXRange(0, xyCoords, scaleData[0].minX, scaleData[0].maxX, 0, xyCoords.length - 1);
  }

  private int setXRange(int i, Coordinate[] xyCoords, double initX, double finalX, int iStart, int iEnd) {
    int index = 0;
    int ptCount = 0;
    for (index = iStart; index <= iEnd; index++) {
      double x = xyCoords[index].getXVal();
      if (x >= initX) {
        scaleData[i % scaleData.length].startDataPointIndex = index;
        break;
      }
    }

    // determine endDataPointIndex
    for (; index <= iEnd; index++) {
      double x = xyCoords[index].getXVal();
      ptCount++;
      if (x >= finalX) {
        break;
      }
    }
    scaleData[i % scaleData.length].endDataPointIndex = index;// BH was -1
    return ptCount;
  }

  /**
	 * in some cases, there is only one scaleData, but there are more than that number of spectra
	 * this is no problem -- we just use mod to set this to 0
	 * @param i
	 * @return starting point data index
	 */
  public int getStartingPointIndex(int i) {
  	return scaleData[i % scaleData.length].startDataPointIndex;
  }
  
	/**
	 * in some cases, there is only one scaleData, but there are more than that number of spectra
	 * this is no problem -- we just use mod to set this to 0
	 * @param i
	 * @return ending point data index
	 */
  public int getEndingPointIndex(int i) {
  	return scaleData[i % scaleData.length].endDataPointIndex;
  }
  
  public boolean areYScalesSame(int i, int j) {
		i %= scaleData.length;
		j %= scaleData.length;
		return (scaleData[i].minYOnScale == scaleData[j].minYOnScale
		    && scaleData[i].maxYOnScale == scaleData[j].maxYOnScale);
	}

  public void setScale(int i, int xPixels, int yPixels, boolean isInverted) {
		iThisScale = i % scaleData.length;
		thisScale = scaleData[iThisScale];
		thisScale.setXYScale(xPixels, yPixels, isInverted);
	}

  public void resetScaleFactors() {
		for (int i = 0; i < scaleData.length; i++)
  	  scaleData[i].spectrumScaleFactor = 1;
	}
	
  public void scaleSpectrum(int i, double f) {
		if (f <= 0 || i >= nSpectra)
			return;
		if (i == -2) {
			thisScale.scale2D(f);
			return;
		}
		if (i < 0)
			for (i = 0; i < scaleData.length; i++)
				scaleData[i].scaleBy(f);
		else
      scaleData[i % scaleData.length].scaleBy(f);
  }

  public ScaleData[] getNewScales(int iSelected, boolean isXOnly, double y1, double y2) {
		if (isXOnly)
  		return scaleData;
		iSelected %= scaleData.length;
		// y1 and y2 are in terms of only the current scale. 
		// We must precalculate all the min/max Y for the scale of all scaleData.
  	double f1 = (y1 - thisScale.minYOnScale) / (thisScale.maxYOnScale - thisScale.minYOnScale);
  	double f2 = (y2 - thisScale.minYOnScale) / (thisScale.maxYOnScale - thisScale.minYOnScale);
	  ScaleData[] sd = new ScaleData[scaleData.length];
  	for (int i = 0; i < scaleData.length; i++)
	  	sd[i] = (iSelected >= 0 && i != iSelected ? scaleData[i] :  new ScaleData());
	  ScaleData.copyScaleFactors(scaleData, sd);
  	ScaleData.copyYScales(scaleData, sd);
  	for (int i = 0; i < scaleData.length; i++) {
  		if (iSelected >= 0 && i != iSelected)
  			continue;
	  	sd[i].isShiftZoomedY = true;
  		sd[i].minYOnScale = scaleData[i].minYOnScale * (1 - f1) + f1 * scaleData[i].maxYOnScale;
  		sd[i].maxYOnScale = scaleData[i].minYOnScale * (1 - f2) + f2 * scaleData[i].maxYOnScale;
  	}
    return sd;	  
	}

}