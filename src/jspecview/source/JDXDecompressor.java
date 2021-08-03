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
   * The delta X value calculated as (lastX - firstX) / (nPoints - 1)
   */
  private double deltaXcalc;


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
  private double lastX;
  
  private double maxY = Double.MIN_VALUE;
  private double minY = Double.MAX_VALUE;

  private boolean debugging;

  private boolean isNTUPLE;

  
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
   * @param firstX first x listed in file
   * @param xFactor
   *        the x factor
   * @param yFactor
   *        the y factor
   * @param lastX
   *        the last X listed in file
   * @param nPoints
   *        the expected number of points
   * @param isNTUPLE 
   */
  public JDXDecompressor(JDXSourceStreamTokenizer t, double firstX, double xFactor,
      double yFactor, double lastX, int nPoints, boolean isNTUPLE) {
    this.t = t;
    this.isNTUPLE = isNTUPLE;
    this.firstX = firstX;
    this.xFactor = xFactor;
    this.yFactor = yFactor;
    this.lastX = lastX;
    this.deltaXcalc = Coordinate.deltaX(lastX, firstX, nPoints);
    this.nPoints = nPoints;
    this.lineNumber = t.labelLineNo;
    debugging = Logger.isActiveLevel(Logger.LEVEL_DEBUGHIGH);

    //Logger.checkMemory();
  }

  private Coordinate[] xyCoords;
  private String line;
  private int lineLen;
  private SB errorLog;

  private void addPoint(Coordinate pt, int ipt) {
    //System.out.println(pt);
    if (ipt == xyCoords.length) {
      Coordinate[] t = new Coordinate[ipt * 2];
      System.arraycopy(xyCoords, 0, t, 0, ipt);
      xyCoords = t;
    }
    xyCoords[ipt] = pt;
    firstLastX[1] = pt.getXVal();
    double y = pt.getYVal();
    if (y > maxY)
      maxY = y;
    else if (y < minY)
      minY = y;
    if (debugging)
      logError("Coord: " + ipt + pt);
  }

  //private static final double FMINY = 0.6;
  //private static final double FMAXY = 1.4;

  private int difVal = Integer.MIN_VALUE;
  private int lastDif = Integer.MIN_VALUE;
  private int dupCount;
  private double yval;

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
      logError("firstX=" + firstX + " xFactor=" + xFactor + " yFactor="
          + yFactor + " deltaX=" + deltaXcalc + " nPoints=" + nPoints);

    //testAlgorithm();

    xyCoords = new Coordinate[nPoints];

    //double difMax = Double.MAX_VALUE;//Math.abs(0.35 * deltaXcalc);
    //double dif06 = Math.abs(0.6 * deltaXcalc);
    //double dif14 = 1.4;//Math.abs(1.4 * deltaXcalc);

    double difFracMax = 0.5;
    double prevXcheck = 0;
    int prevIpt = 0;
    double x = firstX;
    String lastLine = null;
    int ipt = 0;
    try {
      while ((line = t.readLineTrimmed()) != null && line.indexOf("##") < 0) {
        lineNumber++;
        if (debugging)
          logError(lineNumber + "\t" + line);
        if ((lineLen = line.length()) == 0)
          continue;
        ich = 0;
        boolean isCheckPoint = (lastDif != Integer.MIN_VALUE);
        double xcheck = getValueDelim() * xFactor;
        yval = getYValue();
        double y = yval * yFactor;
        Coordinate point = new Coordinate().set(x, y);
        if (ipt == 0) {
          firstLastX[0] = xcheck;
          addPoint(point, ipt++); // first data line only
        } else {
          // do check
          Coordinate lastPoint = xyCoords[ipt - 1];
          //double xdif = Math.abs(lastPoint.getXVal() - point.getXVal());
          //System.out.println(ipt + " " + xdif + " " + point + " " + dx + " " + lastPoint);
          // DIF Y checkpoint means X value does not advance at start
          // of new line. Remove last values and put in latest ones
          if (isCheckPoint) {
            // note that missing or out-of-order lines will result in a Y-value error and two X-check failures
            xyCoords[ipt - 1] = point;
            // Check for Y checkpoint error - Y values should correspond
            double lastY = lastPoint.getYVal();
            if (y != lastY)
              logError(
                  lastLine + "\n" + line + "\nY-value Checkpoint Error! Line "
                      + lineNumber + " for y=" + y + " yLast=" + lastY);
            if (xcheck == prevXcheck || (xcheck < prevXcheck) != (deltaXcalc < 0)) {
              // duplicated or out of order lines
              logError(
                  lastLine + "\n" + line + "\nX-sequence Checkpoint Error! Line "
                      + lineNumber + " order for xCheck=" + xcheck
                      + " after prevXCheck=" + prevXcheck);
            }
            // |--------|.....by ipt
            // |---|..........by xcheck duplicated or out
            // |------------|.by xcheck  missing a line
            double xcheckDif = Math.abs(xcheck - prevXcheck);
            double xiptDif = Math.abs((ipt - prevIpt) * deltaXcalc);
            double fracDif = Math.abs((xcheckDif - xiptDif)) / xcheckDif;
            if (debugging)
              System.out.println("JDXD fracDif = " + xcheck + "\t" + prevXcheck + "\txcheckDif=" + xcheckDif + "\txiptDif=" + xiptDif + "\tf=" + fracDif);
            if (fracDif > difFracMax) {
              logError(lastLine + "\n" + line
                  + "\nX-value Checkpoint Error! Line " + lineNumber
                  + " expected " + xiptDif + " but X-Sequence Check difference reads " + xcheckDif);
            }
            //          } else {
            //            addPoint(point);
            //            // Check for X checkpoint error
            //            // first point of new line should be deltaX away
            //            // ACD/Labs seem to have large rounding error so using between 0.6 and 1.4
            //            if (xdif < dif06 || xdif > dif14)
            //              logError(lastLine + "\n" + line
            //                  + "\nX-sequence Checkpoint Error! Line " + lineNumber
            //                  + " |x1-x0|=" + xdif + " instead of " + Math.abs(deltaX)
            //                  + " for x1=" + point.getXVal() + " x0=" + lastPoint.getXVal());
          }
        }
        prevIpt = (ipt == 1 ? 0 : ipt);
        prevXcheck = xcheck;
        while (ich < lineLen || difVal != Integer.MIN_VALUE || dupCount > 0) {
          x += deltaXcalc;
          if (!Double.isNaN(yval = getYValue())) {
            addPoint(new Coordinate().set(x, yval * yFactor), ipt++);
          }
        }
        lastLine = line;
      }
    } catch (IOException ioe) {
    }
    if (nPoints != ipt) {
      int n = nPoints;//(isNTUPLE ? nPoints : ipt);
      logError("Decompressor did not find " + nPoints + " points -- instead "
          + ipt + " xyCoords.length set to " + n);
      Coordinate[] temp = new Coordinate[n];
      System.arraycopy(xyCoords, 0, temp, 0, Math.min(ipt, n));
      xyCoords = temp;
    }
    return (deltaXcalc > 0 ? xyCoords : Coordinate.reverse(xyCoords));
  }

  private void logError(String s) {
    if (debugging)
      Logger.debug(s);
    System.err.println(s);
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
