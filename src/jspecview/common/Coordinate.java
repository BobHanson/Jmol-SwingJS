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

import java.util.Arrays;
import java.util.Comparator;

import java.util.StringTokenizer;

import javajs.util.DF;
import javajs.util.Lst;





/**
 * The <code>Coordinate</code> class stores the x and y values of a coordinate.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class Coordinate {
  /** the x value */
  private double xVal = 0;
  /** the y value */
  private double yVal = 0;

  /**
   * Constructor
   */
  public Coordinate() {
  }

  /**
   * Constructor
   * 
   * @param x
   *        the x value
   * @param y
   *        the y value
   * @return this
   */
  public Coordinate set(double x, double y) {
    xVal = x;
    yVal = y;
    return this;
  }

  /**
   * Returns the x value of the coordinate
   * 
   * @return the x value of the coordinate
   */
  public double getXVal() {
    return xVal;
  }

  /**
   * Returns the y value of the coordinate
   * 
   * @return the y value of the coordinate
   */
  public double getYVal() {
    return yVal;
  }

  /**
   * Returns the x value of the coordinate formatted to a maximum of eight
   * decimal places
   * 
   * @return Returns the x value of the coordinate formatted to a maximum of
   *         eight decimal places
   */
  public String getXString() {
    return DF.formatDecimalTrimmed(xVal, 8);
  }

  /**
   * Returns the y value of the coordinate formatted to a maximum of eight
   * decimal places
   * 
   * @return Returns the y value of the coordinate formatted to a maximum of
   *         eight decimal places
   */
  public String getYString() {
    return DF.formatDecimalTrimmed(yVal, 8);
  }

  /**
   * Sets the x value of the coordinate
   * 
   * @param val
   *        the x value
   */
  public void setXVal(double val) {
    xVal = val;
  }

  /**
   * Sets the y value of the coordinate
   * 
   * @param val
   *        the y value
   */
  public void setYVal(double val) {
    yVal = val;
  }

  /**
   * Returns a new coordinate that has the same x and y values of this
   * coordinate
   * 
   * @return Returns a new coordinate that has the same x and y values of this
   *         coordinate
   */
  public Coordinate copy() {
    return new Coordinate().set(xVal, yVal);
  }

  /**
   * Indicates whether some other Coordinate is equal to this one
   * 
   * @param coord
   *        the reference coordinate
   * @return true if the coordinates are equal, false otherwise
   */
  public boolean equals(Coordinate coord) {
    return (coord.xVal == xVal && coord.yVal == yVal);
  }

  /**
   * Overides Objects toString() method
   * 
   * @return the String representation of this coordinate
   */
  @Override
  public String toString() {
    return "[" + xVal + ", " + yVal + "]";
  }
  
  /**
   * Determines if the y values of a spectrum are in a certain range
   * 
   * @param xyCoords
   * @param min
   * @param max
  * @return true is in range, otherwise false
   */
  public static boolean isYInRange(Coordinate[] xyCoords, double min,
                                    double max) {
    return (getMinY(xyCoords, 0, xyCoords.length - 1) >= min 
        && getMaxY(xyCoords, 0, xyCoords.length - 1) >= max);
  }

  /**
   * Normalises the y values of a spectrum to a certain range
   * 
   * @param xyCoords
   * @param min
   * @param max
   * @return array of normalised coordinates
   */
  public static Coordinate[] normalise(Coordinate[] xyCoords, double min,
                                        double max) {
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    double minY = getMinY(xyCoords, 0, xyCoords.length - 1);
    double maxY = getMaxY(xyCoords, 0, xyCoords.length - 1);
    double factor = (maxY - minY) / (max - min); // range = 0-5
    for (int i = 0; i < xyCoords.length; i++)
      newXYCoords[i] = new Coordinate().set(xyCoords[i].getXVal(), 
          ((xyCoords[i].getYVal() - minY) / factor) - min);
    return newXYCoords;
  }

  public static Coordinate[] reverse(Coordinate[] x) {
    int n = x.length;
    for (int i = 0; i < n; i++) {
      Coordinate v = x[i];
      x[i] = x[--n];
      x[n] = v;
    }
    return x;
  }

  /**
   * Parses data stored in x, y format
   * 
   * @param dataPoints
   *        the data as string
   * @param xFactor
   *        the factor to apply to x values
   * @param yFactor
   *        the factor to apply to y values
   * @return an array of <code>Coordinate</code>s
   */
  public static Coordinate[] parseDSV(String dataPoints, double xFactor,
                                      double yFactor) {
  
    //int linenumber = 0;
    Coordinate point;
    double xval = 0;
    double yval = 0;
    Lst<Coordinate> xyCoords = new Lst<Coordinate>();
  
    String delim = " \t\n\r\f,;";
    StringTokenizer st = new StringTokenizer(dataPoints, delim);
    String tmp1, tmp2;
  
    while (st.hasMoreTokens()) {
      tmp1 = st.nextToken().trim();
      tmp2 = st.nextToken().trim();
  
      xval = Double.parseDouble(tmp1);
      yval = Double.parseDouble(tmp2);
      point = new Coordinate().set(xval * xFactor, yval * yFactor);
      xyCoords.addLast(point);
    }
    
    Coordinate[] coord = new Coordinate[xyCoords.size()];
    return xyCoords.toArray(coord);
  }

  /**
   * Returns the Delta X value
   * 
   * @param last
   *        the last x value
   * @param first
   *        the first x value
   * @param numPoints
   *        the number of data points
   * @return the Delta X value
   */
  public static double deltaX(double last, double first, int numPoints) {
    double test = (last - first) / (numPoints - 1);
    return test;
  }

  /**
   * Removes the scale factor from the coordinates
   * 
   * @param xyCoords
   *        the array of coordinates
   * @param xScale
   *        the scale for the x values
   * @param yScale
   *        the scale for the y values
   */
  public static void removeScale(Coordinate[] xyCoords, double xScale,
                                 double yScale) {
    applyScale(xyCoords, (1 / xScale), (1 / yScale));
  }

  /**
   * Apply the scale factor to the coordinates
   * 
   * @param xyCoords
   *        the array of coordinates
   * @param xScale
   *        the scale for the x values
   * @param yScale
   *        the scale for the y values
   */
  public static void applyScale(Coordinate[] xyCoords, double xScale,
                                double yScale) {
    if (xScale != 1 || yScale != 1) {
      for (int i = 0; i < xyCoords.length; i++) {
        xyCoords[i].setXVal(xyCoords[i].getXVal() * xScale);
        xyCoords[i].setYVal(xyCoords[i].getYVal() * yScale);
      }
    }
  }

  /**
   * Applies the shift reference to all coordinates
   * 
   * @param xyCoords
   *        an array of coordinates
   * @param dataPointNum
   *        the number of the data point in the the spectrum, indexed from 1
   * @param firstX
   *        the first X value
   * @param lastX
   *        the last X value
   * @param offset
   *        the offset value
   * @param observedFreq
   *        the observed frequency
   * @param shiftRefType
   *        the type of shift
   * @throws IndexOutOfBoundsException
   */
  public static void applyShiftReference(Coordinate[] xyCoords,
                                         int dataPointNum, double firstX,
                                         double lastX, double offset,
                                         double observedFreq, int shiftRefType)
      throws IndexOutOfBoundsException {
  
    if (dataPointNum > xyCoords.length || dataPointNum < 0)
      //throw new IndexOutOfBoundsException();
      return;
  
    Coordinate coord;
    switch (shiftRefType) {
    case 0:
      //double deltaX = JSpecViewUtils.deltaX(xyCoords[xyCoords.length - 1].getXVal(), xyCoords[0].getXVal(), xyCoords.length);     
      offset = xyCoords[xyCoords.length - dataPointNum].getXVal() - offset * observedFreq;
      break;
    case 1:
      offset = firstX - offset * observedFreq;
      break;
    case 2:
      offset = lastX + offset;
      break;
    }
  
    for (int index = 0; index < xyCoords.length; index++) {
      coord = xyCoords[index];
      coord.setXVal(coord.getXVal() - offset);
      xyCoords[index] = coord;
    }
  
    firstX -= offset;
    lastX -= offset;
  }

  /**
   * Returns the minimum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the maximum x value of an array of <code>Coordinate</code>s
   */
  public static double getMinX(Coordinate[] coords, int start, int end) {   
    double min = Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getXVal();
      if (tmp < min)
      min = tmp;
    }
    return min;
  }

  /**
   * Returns the minimum x value value from an array of arrays of
   * <code>Coordinate</code>s.
   * @param spectra 
   * @param vd 
   * 
   * @return the minimum x value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMinX(Lst<Spectrum> spectra, ViewData vd) {
    double min = Double.MAX_VALUE;
    for (int i = 0; i < spectra.size(); i++) {
      Coordinate[] xyCoords = spectra.get(i).getXYCoords();
      double tmp = getMinX(xyCoords, vd.getStartingPointIndex(i), vd.getEndingPointIndex(i));
      if (tmp < min)
        min = tmp;
    }
    return min;
  }

  /**
   * Returns the minimum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the minimum x value of an array of <code>Coordinate</code>s
   */
  public static double getMaxX(Coordinate[] coords, int start, int end) {
    double max = -Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getXVal();
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  /**
   * Returns the maximum x value value from an array of arrays of
   * <code>Coordinate</code>s.
   * @param spectra 
   * @param vd 
   * @return the maximum x value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMaxX(Lst<Spectrum> spectra, ViewData vd) {
    double max = -Double.MAX_VALUE;
    for (int i = 0; i < spectra.size(); i++) {
      Coordinate[] xyCoords = spectra.get(i).getXYCoords();
      double tmp = getMaxX(xyCoords, vd.getStartingPointIndex(i), vd.getEndingPointIndex(i));
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  /**
   * Returns the minimum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the minimum y value of an array of <code>Coordinate</code>s
   */
  public static double getMinY(Coordinate[] coords, int start, int end) {
    double min = Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getYVal();
      if (tmp < min)
      min = tmp;
    }
    return min;
  }

  
  /**
   * Returns the minimum y value value from an array of arrays of
   * <code>Coordinate</code>s.
   * @param spectra 
   * @param vd 
   * @return the minimum y value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMinYUser(Lst<Spectrum> spectra, ViewData vd) {
    double min = Double.MAX_VALUE;
    for (int i = 0; i < spectra.size(); i++) {
      double u = spectra.get(i).getUserYFactor();
      double yref = spectra.get(i).getYRef();
      Coordinate[] xyCoords = spectra.get(i).getXYCoords();
      double tmp = (getMinY(xyCoords, vd.getStartingPointIndex(i), vd.getEndingPointIndex(i)) - yref) * u + yref;
      if (tmp < min)
        min = tmp;
    }  
    return min;
  }

  /**
   * Returns the maximum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the maximum y value of an array of <code>Coordinate</code>s
   */
  public static double getMaxY(Coordinate[] coords, int start, int end) {
    double max = -Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getYVal();
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  /**
   * Returns the maximum y value value from an array of arrays of
   * <code>Coordinate</code>s.
   * @param spectra 
   * @param vd 
   * @return the maximum y value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMaxYUser(Lst<Spectrum> spectra, ViewData vd) {
    double max = -Double.MAX_VALUE;
    for (int i = 0; i < spectra.size(); i++) {
      double u = spectra.get(i).getUserYFactor();
      double yref = spectra.get(i).getYRef();
      Coordinate[] xyCoords = spectra.get(i).getXYCoords();
      double tmp = (getMaxY(xyCoords, vd.getStartingPointIndex(i), vd.getEndingPointIndex(i)) - yref) * u + yref;
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  private final static Comparator<Coordinate> c = new CoordComparator();
  
  public static double getYValueAt(Coordinate[] xyCoords, double xPt) {
    int i = getNearestIndexForX(xyCoords, xPt);
    if (i == 0 || i == xyCoords.length)
      return Double.NaN;
    double x1 = xyCoords[i].getXVal();
    double x0 = xyCoords[i - 1].getXVal();
    double y1 = xyCoords[i].getYVal();
    double y0 = xyCoords[i - 1].getYVal();
    if (x1 == x0)
      return y1; 
    return y0 + (y1 - y0) / (x1 - x0) * (xPt - x0);
  }

  static int intoRange(int i, int i0, int i1) {
    return Math.max(Math.min(i, i1), i0);
  }

  public static int getNearestIndexForX(Coordinate[] xyCoords, double xPt) {
    Coordinate x = new Coordinate().set(xPt, 0);
    int i = Arrays.binarySearch(xyCoords, x, c);
    if (i < 0) i = -1 - i;
    if (i < 0)
      return 0;
    if (i > xyCoords.length - 1)
      return xyCoords.length - 1;
    return i;
  }
  
  public static double findXForPeakNearest(Coordinate[] xyCoords, double x, 
      boolean isMin) {
    int pt = getNearestIndexForX(xyCoords, x);
    double f = (isMin ? -1 : 1);
    while (pt < xyCoords.length - 1 && f * (xyCoords[pt + 1].yVal - xyCoords[pt].yVal) > 0)
        pt++;
    while (pt >= 1 && f * (xyCoords[pt - 1].yVal - xyCoords[pt].yVal) > 0)
      pt--;
    // now at local max
    // could leave it there? 
    // see https://ccrma.stanford.edu/~jos/sasp/Quadratic_Interpolation_Spectral_Peaks.html
    if (pt == 0 || pt == xyCoords.length - 1)
      return xyCoords[pt].xVal;
    return parabolicInterpolation(xyCoords, pt);
  }

  /**
   *    see https://ccrma.stanford.edu/~jos/sasp/Quadratic_Interpolation_Spectral_Peaks.html
   *
   * @param xyCoords
   * @param pt
   * @return center
   */
  public static double parabolicInterpolation(Coordinate[] xyCoords, int pt) {
    double alpha = xyCoords[pt - 1].yVal;
    double beta = xyCoords[pt].yVal;
    double gamma = xyCoords[pt + 1].yVal;
    double p = (alpha - gamma) / 2 / (alpha - 2 * beta + gamma);
    return xyCoords[pt].xVal + p * (xyCoords[pt + 1].xVal - xyCoords[pt].xVal);
  }

  static boolean getPickedCoordinates(Coordinate[] coordsClicked,
      Coordinate coordClicked, Coordinate coord, Coordinate actualCoord) {
    if (coordClicked == null)
      return false;
    double x = coordClicked.getXVal();
    coord.setXVal(x);
    coord.setYVal(coordClicked.getYVal());
    if (actualCoord == null)
      return true;
    int pt = getNearestIndexForX(coordsClicked, x);
    actualCoord.setXVal(coordsClicked[pt].getXVal());
    actualCoord.setYVal(coordsClicked[pt].getYVal());
    return true;
  }

  public static void shiftX(Coordinate[] xyCoords, double dx) {
    for (int i = xyCoords.length; --i >= 0;)
      xyCoords[i].xVal += dx;
  }

  /**
   * discovers nearest peak left or right of x that is above threshold y
   *  
   * @param xyCoords
   * @param x
   * @param y
   * @param inverted
   * @param andGreaterThanX
   * @return   interpolated x value or NaN
   */
  public static double getNearestXWithYAbove(Coordinate[] xyCoords, double x,
      double y, boolean inverted, boolean andGreaterThanX) {
    int pt = getNearestIndexForX(xyCoords, x);
    double f = (inverted ? -1 : 1);
    if (andGreaterThanX)
      while (pt < xyCoords.length && f * (xyCoords[pt].yVal - y) < 0)
        pt++;
    else
      while (pt >= 0 && f * (xyCoords[pt].yVal - y) < 0)
        pt--;
    if (pt == -1 || pt == xyCoords.length)
      return Double.NaN;
    return findXForPeakNearest(xyCoords, xyCoords[pt].getXVal(), inverted);
  }
}
