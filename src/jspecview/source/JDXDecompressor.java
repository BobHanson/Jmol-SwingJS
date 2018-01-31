/* Copyright (c) 2002-2010 The University of the West Indies
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

// CHANGES to 'JDXDecompressor.java' - 
// University of the West Indies, Mona Campus
//
// 23-08-2010 fix for DUP before DIF e.g. at start of line

package jspecview.source;

import java.io.IOException;

import javajs.util.SB;

import org.jmol.util.Logger;

import jspecview.common.Coordinate;

/**
 * JDXDecompressor contains static methods to decompress the data part of
 * JCAMP-DX spectra that have been compressed using DIF, FIX, SQZ or PAC
 * formats. If you wish to parse the data from XY formats see
 * {@link jspecview.common.Coordinate#parseDSV(java.lang.String, double, double)}
 * 
 * @author Christopher Muir
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson - hansonr@stolaf.edu
 */
public class JDXDecompressor {

  /**
   * The x compression factor
   */
  private double xFactor;

  /**
   * The y compression factor
   */
  private double yFactor;

  /**
   * The delta X value
   */
  private double deltaX;


  /**
   * The (nominal) number of points
   */
  private int nPoints;

  /**
   * All Delimiters in a JCAMP-DX compressed file
   */
  private static final String allDelim = "+-%@ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrs? ,\t\n";

  /**
   * The character index on the current line
   */
  private int ich;

  /**
   * The line number of the dataset label in the source file
   */
  private int lineNumber = 0;

  private JDXSourceStreamTokenizer t;
  private double firstX;
  private double dx;
  
  private double maxY = Double.MIN_VALUE;
  private double minY = Double.MAX_VALUE;

  private boolean debugging;
  
  public double getMinY() {
    return minY;
  }
  
  public double getMaxY() {
    return maxY;
  }

  /**
   * Initialises the <code>JDXDecompressor</code> from the compressed data, the
   * x factor, the y factor and the deltaX value
   * @param t
   *        the data to be decompressed
   * @param firstX TODO
   * @param xFactor
   *        the x factor
   * @param yFactor
   *        the y factor
   * @param deltaX
   *        the delta X value
   * @param nPoints
   *        the expected number of points
   */
  public JDXDecompressor(JDXSourceStreamTokenizer t, double firstX, double xFactor,
      double yFactor, double deltaX, int nPoints) {
    this.t = t;
    this.firstX = firstX;
    this.xFactor = xFactor;
    this.yFactor = yFactor;
    this.deltaX = deltaX;
    this.nPoints = nPoints;
    this.lineNumber = t.labelLineNo;
    debugging = Logger.isActiveLevel(Logger.LEVEL_DEBUGHIGH);

    //Logger.checkMemory();
  }

  private Coordinate[] xyCoords;
  private int ipt;
  private String line, lastLine;
  private int lineLen;
  private SB errorLog;

  private void addPoint(Coordinate pt) {
    //System.out.println(pt);
    if (ipt == xyCoords.length) {
      Coordinate[] t = new Coordinate[ipt * 2];
      System.arraycopy(xyCoords, 0, t, 0, ipt);
      xyCoords = t;
    }
    double d = pt.getYVal();
    if (d > maxY)
      maxY = d;
    else if (d < minY)
      minY = d;
    if (debugging)
      logError("Coord: " + ipt + pt);
    xyCoords[ipt++] = pt;
    firstLastX[1] = pt.getXVal();

  }

  //private static final double FMINY = 0.6;
  //private static final double FMAXY = 1.4;

  private int difVal = Integer.MIN_VALUE;
  private int lastDif = Integer.MIN_VALUE;
  private int dupCount;
  private double xval, yval;

  private double[] firstLastX;

  /**
   * Determines the type of compression, decompresses the data and stores
   * coordinates in an array to be returned
   * 
   * @param errorLog
   * @param firstLastX 
   * @return the array of <code>Coordinate</code>s
   */
  public Coordinate[] decompressData(SB errorLog, double[] firstLastX) {

    this.errorLog = errorLog;
    this.firstLastX = firstLastX;
    if (debugging)
      logError("firstX=" + firstX 
          + " xFactor=" + xFactor + " yFactor=" + yFactor + " deltaX=" + deltaX + " nPoints=" + nPoints);

          testAlgorithm();

    xyCoords = new Coordinate[nPoints];

    double difMax = Math.abs(0.35 * deltaX);
    double dif14 = Math.abs(1.4 * deltaX);
    double dif06 = Math.abs(0.6 * deltaX);

    try {
      while ((line = t.readLineTrimmed()) != null && line.indexOf("##") < 0) {
        lineNumber++;
        if (debugging)
          logError(lineNumber + "\t" + line);
        if ((lineLen = line.length()) == 0)
          continue;
        ich = 0;
        boolean isCheckPoint = (lastDif != Integer.MIN_VALUE);
        xval = getValueDelim() * xFactor;
        if (ipt == 0) {
          firstLastX[0] = xval;
          dx = firstX - xval;
        }
        xval += dx;
        Coordinate point = new Coordinate().set(xval, (yval = getYValue()) * yFactor);
        if (ipt == 0) {
          addPoint(point); // first data line only
        } else {
          Coordinate lastPoint = xyCoords[ipt - 1];
          double xdif = Math.abs(lastPoint.getXVal() - point.getXVal());
          // DIF Y checkpoint means X value does not advance at start
          // of new line. Remove last values and put in latest ones
          if (isCheckPoint && xdif < difMax) {
            xyCoords[ipt - 1] = point;
            // Check for Y checkpoint error - Y values should correspond
            double y = lastPoint.getYVal();
            double y1 = point.getYVal();
            if (y1 != y)
              logError(lastLine + "\n" + line
                  + "\nY-value Checkpoint Error! Line " + lineNumber
                  + " for y1=" + y1 + " y0=" + y);
          } else {
            addPoint(point);
            // Check for X checkpoint error
            // first point of new line should be deltaX away
            // ACD/Labs seem to have large rounding error so using between 0.6 and 1.4
            if (xdif < dif06 || xdif > dif14)
              logError(lastLine + "\n" + line
                  + "\nX-sequence Checkpoint Error! Line " + lineNumber
                  + " |x1-x0|=" + xdif + " instead of " + Math.abs(deltaX)
                  + " for x1=" + point.getXVal() + " x0=" + lastPoint.getXVal());
          }
        }
        while (ich < lineLen || difVal != Integer.MIN_VALUE || dupCount > 0) {
          xval += deltaX;
          if (!Double.isNaN(yval = getYValue()))
            addPoint(new Coordinate().set(xval, yval * yFactor));
        }
        lastLine = line;
      }
    } catch (IOException ioe) {
    }
    if (nPoints != ipt) {
      logError("Decompressor did not find " + nPoints
          + " points -- instead " + ipt);
      Coordinate[] temp = new Coordinate[ipt];
      System.arraycopy(xyCoords, 0, temp, 0, ipt);
      xyCoords = temp;
    }
    return (deltaX > 0 ? xyCoords : Coordinate.reverse(xyCoords));
  }

  private void logError(String s) {
    if (debugging)
      Logger.debug(s);
    errorLog.append(s).appendC('\n');  
  }

  private double getYValue() {
    if (dupCount > 0) {
      --dupCount;
      yval = (lastDif == Integer.MIN_VALUE ? yval : yval + lastDif);
      return yval;
    }
    if (difVal != Integer.MIN_VALUE) {
      yval += difVal;
      lastDif  = difVal;
      difVal = Integer.MIN_VALUE;
      return yval;
    }
    if (ich == lineLen)
      return Double.NaN;
    char ch = line.charAt(ich);
    if (debugging)
      Logger.info("" + ch);
    switch (ch) {
    case '%':
      difVal = 0;
      break;
    case 'J':
    case 'K':
    case 'L':
    case 'M':
    case 'N':
    case 'O':
    case 'P':
    case 'Q':
    case 'R':
      difVal = ch - 'I';
      break;
    case 'j':
    case 'k':
    case 'l':
    case 'm':
    case 'n':
    case 'o':
    case 'p':
    case 'q':
    case 'r':
      difVal = 'i' - ch;
      break;
    case 'S':
    case 'T':
    case 'U':
    case 'V':
    case 'W':
    case 'X':
    case 'Y':
    case 'Z':
      dupCount = ch - 'R';
      break;
    case 's':
      dupCount = 9;
      break;
    case '+':
    case '-':
    case '.':
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
    case '@':
    case 'A':
    case 'B':
    case 'C':
    case 'D':
    case 'E':
    case 'F':
    case 'G':
    case 'H':
    case 'I':
    case 'a':
    case 'b':
    case 'c':
    case 'd':
    case 'e':
    case 'f':
    case 'g':
    case 'h':
    case 'i':
      lastDif = Integer.MIN_VALUE;
      return getValue();
    case '?':
      lastDif = Integer.MIN_VALUE;
      return Double.NaN;
    default:
      // ignore
      ich++;
      lastDif = Integer.MIN_VALUE;
      return getYValue();
    }
    ich++;
    if (difVal != Integer.MIN_VALUE)
      difVal = getDifDup(difVal);
    else
      dupCount = getDifDup(dupCount) - 1;
    return getYValue();
  }
  
  private int getDifDup(int i) {
    int ich0 = ich;
    next();
    String s = i + line.substring(ich0, ich);
    //System.out.println("skip " + ich0 + " " + this.ich + " " + s);
    return (ich0 == ich ? i : Integer.valueOf(s).intValue());
  }

  private double getValue() {
    int ich0 = ich;
    if (ich == lineLen)
      return Double.NaN;
    char ch = line.charAt(ich);
    int leader = 0;
    switch (ch) {
    case '+':
    case '-':
    case '.':
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
      return getValueDelim();
    case '@':
    case 'A':
    case 'B':
    case 'C':
    case 'D':
    case 'E':
    case 'F':
    case 'G':
    case 'H':
    case 'I':
      leader = ch - '@';
      ich0 = ++ich;
      break;
    case 'a':
    case 'b':
    case 'c':
    case 'd':
    case 'e':
    case 'f':
    case 'g':
    case 'h':
    case 'i':
      leader =  '`' - ch;
      ich0 = ++ich;
      break;
    default:
      // skip
      ich++;
      return getValue();
    }
    next();
    return Double.valueOf(leader + line.substring(ich0, ich)).doubleValue();
  }
  
  private final static String WHITE_SPACE = " ,\t\n";

  private double getValueDelim() {
    int ich0 = ich;
    char ch = '\0';
    while (ich < lineLen && WHITE_SPACE.indexOf(ch = line.charAt(ich)) >= 0)
      ich++;
    double factor = 1;
    switch (ch) {
    case '-':
      factor = -1;
      //$FALL-THROUGH$
    case '+':
      ich0 = ++ich;
      break;
    }
    ch = next();
    if (ch == 'E' && ich + 3 < lineLen)
      switch (line.charAt(ich + 1)) {
      case '-':
      case '+':
        ich += 4;
        if (ich < lineLen && (ch = line.charAt(ich)) >= '0' && ch <= '9')
          ich++;
        break;
      }
    return factor * ((Double.valueOf(line.substring(ich0, ich))).doubleValue());
  }

	private char next() {
		while (ich < lineLen && allDelim.indexOf(line.charAt(ich)) < 0)
			ich++;
		return (ich == lineLen ? '\0' : line.charAt(ich));
	}

  private void testAlgorithm() {
/*     line = "4265A8431K85L83L71K55P5j05k35k84k51j63n5K4M1j2j10j97k28j88j01j7K4or4k04k89";
     lineLen = line.length();
     System.out.println(getValue(allDelim));
     while (ich < lineLen)
       System.out.println(line.substring(0, ich) + "\n" + ipt++ + " " + (yval = getYValue()));
     ipt= 0;
*/  }

}
