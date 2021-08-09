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
import java.util.Iterator;

import org.jmol.util.Logger;

import javajs.util.SB;
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
public class JDXDecompressor implements Iterator<Double> {

  /**
   * The x compression factor
   */
  private double xFactor;

  /**
   * The y compression factor
   */
  private double yFactor;

  /**
   * The (nominal) number of points
   */
  private int nPoints;

//  /**
//   * All ASCII Squeezed Difference Form (ASDF) digit tokens
//   */
//  private static final String pseudoDigits = "%@ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrs";

  /**
   * number delimiters - whitespace and comma
   */
  private static final String delimiters = " ,\t\n";

  private static final int[] actions = new int[255];

  private static final int ACTION_INVALID   = -1;
  private static final int ACTION_UNKNOWN   = 0;
  private static final int ACTION_DIF       = 1;
  private static final int ACTION_DUP       = 2;
  private static final int ACTION_SQZ       = 3;
  private static final int ACTION_NUMERIC   = 4;
  
  /**
   * if '?' is encountered, we will assign 0 to this value and continue
   */
  private static final double INVALID_Y = Double.MAX_VALUE;
  
  static {
    // from '%' to 's'; all others will remain 0, ACTION_UNKNOWN
    for (int i = 0x25; i <= 0x73; i++) {
      char c = (char) i;
      switch(c) {      
      case '%':
      case 'J':
      case 'K':
      case 'L':
      case 'M':
      case 'N':
      case 'O':
      case 'P':
      case 'Q':
      case 'R':
      case 'j':
      case 'k':
      case 'l':
      case 'm':
      case 'n':
      case 'o':
      case 'p':
      case 'q':
      case 'r':
        actions[i] = ACTION_DIF;
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
        actions[i] = ACTION_NUMERIC;
        break;
      case '?':
        actions[i] = ACTION_INVALID;
        break;
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
        actions[i] = ACTION_SQZ;
        break;
      case 'S':
      case 'T':
      case 'U':
      case 'V':
      case 'W':
      case 'X':
      case 'Y':
      case 'Z':
      case 's':
        actions[i] = ACTION_DUP;
        break;
      }
    }
  }

  /**
   * The character index on the current line
   */
  int ich;

  private JDXSourceStreamTokenizer t;

  private double firstX;
  
  /**
   * initially assigned file value, but in the end assigned the value at the end of the last line
   */
  double lastX;
  
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
   * @param firstX first x listed in file
   * @param lastX
   *        the last X listed in file
   * @param xFactor
   *        the x factor
   * @param yFactor
   *        the y factor
   * @param nPoints
   *        the expected number of points
   */
  public JDXDecompressor(JDXSourceStreamTokenizer t, double firstX, double lastX,
      double xFactor, double yFactor, int nPoints) {
    this.t = t;
    // not used this.isNTUPLE = isNTUPLE;
    this.firstX = firstX;
    this.lastX = lastX;
    this.xFactor = xFactor;
    this.yFactor = yFactor;
    this.nPoints = nPoints;
    debugging = Logger.isActiveLevel(Logger.LEVEL_DEBUGHIGH);

    //Logger.checkMemory();
  }

  public JDXDecompressor(String line, int lastY) {
    this.line = line.trim();
    this.lineLen = line.length();
    this.lastY = lastY;
  }

  private Coordinate[] xyCoords;
  private String line;
  private int lineLen;
  private SB errorLog;

  //private static final double FMINY = 0.6;
  //private static final double FMAXY = 1.4;

  /**
   * the last difference, during duplication
   */
  private int lastDif = Integer.MIN_VALUE;
  
  /**
   * the duplication counter
   */
  private int dupCount;

  /**
   * the number of points actually found
   */
  private int nptsFound;

  double lastY;

  /**
   * Determines the type of compression, decompresses the data and stores
   * coordinates in an array to be returned
   * 
   * @param errorLog
   * @return the array of <code>Coordinate</code>s
   */
  public Coordinate[] decompressData(SB errorLog) {

    this.errorLog = errorLog;
    double deltaXcalc = Coordinate.deltaX(lastX, firstX, nPoints);

    //debugging = true;

    if (debugging)
      logError("firstX=" + firstX + " lastX=" + lastX + " xFactor=" + xFactor
          + " yFactor=" + yFactor + " deltaX=" + deltaXcalc + " nPoints="
          + nPoints);

    //testAlgorithm();

    xyCoords = new Coordinate[nPoints];

    //double difMax = Double.MAX_VALUE;//Math.abs(0.35 * deltaXcalc);
    //double dif06 = Math.abs(0.6 * deltaXcalc);
    //double dif14 = 1.4;//Math.abs(1.4 * deltaXcalc);

    double difFracMax = 0.5;
    double prevXcheck = 0;
    int prevIpt = 0;
    double lastXExpected = lastX;
    double x = lastX = firstX;
    String lastLine = null;
    int ipt = 0;
    double yval = 0;
    boolean haveWarned = false;
    int lineNumber = t.labelLineNo;
    try {
      while ((line = t.readLineTrimmed()) != null && line.indexOf("##") < 0) {
        lineNumber++;
        if ((lineLen = line.length()) == 0)
          continue;
        ich = 0;
        boolean isCheckPoint = isDIF;
        double xcheck = readSignedFloat() * xFactor;
        yval = nextValue(yval);
        // only advance x if this is not a checkpoint and not the first point
        if (!isCheckPoint && ipt > 0)
          x += deltaXcalc;
        if (debugging)
          logError("Line: " + lineNumber +  " isCP=" + isCheckPoint + "\t>>" + line + "<<\n x, xcheck " + x + " " + x/xFactor + " " + xcheck/xFactor + " " + deltaXcalc/xFactor);
        double y = yval * yFactor;
        Coordinate point = new Coordinate().set(x, y);
        if (ipt == 0 || !isCheckPoint) {
          addPoint(point, ipt++); // first data line only or not a checkpoint
        } else if (ipt < nPoints) {
          // do check
          // DIF Y checkpoint means X value does not advance at start
          // of new line. Remove last values and put in latest ones
          // note that missing or out-of-order lines will result in a Y-value error and two X-check failures
          // Check for Y checkpoint error - Y values should correspond
          double lastY = xyCoords[ipt - 1].getYVal();
          if (y != lastY) {
            xyCoords[ipt - 1] = point;
            logError(
                lastLine + "\n" + line + "\nY-value Checkpoint Error! Line "
                    + lineNumber + " for y=" + y + " yLast=" + lastY);
          }
          if (xcheck == prevXcheck
              || (xcheck < prevXcheck) != (deltaXcalc < 0)) {
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
            System.err.println(
                "JDXD fracDif = " + xcheck + "\t" + prevXcheck + "\txcheckDif="
                    + xcheckDif + "\txiptDif=" + xiptDif + "\tf=" + fracDif);
          if (fracDif > difFracMax) {
            logError(
                lastLine + "\n" + line + "\nX-value Checkpoint Error! Line "
                    + lineNumber + " expected " + xiptDif
                    + " but X-Sequence Check difference reads " + xcheckDif);
          }
        }
        prevIpt = (ipt == 1 ? 0 : ipt);
        prevXcheck = xcheck;
        int nX = 0;
        while (hasNext()) {
          int ich0 = ich;
          if (debugging)
            logError("line " + lineNumber + " char " + ich0 + ":" + line.substring(0, ich0) + ">>>>" + line.substring(ich));
          if (Double.isNaN(yval = nextValue(yval))) {
            logError("There was an error reading line " + lineNumber + " char " + ich0 + ":" + line.substring(0, ich0) + ">>>>" + line.substring(ich0));
          } else {
            x += deltaXcalc;
            if (yval == INVALID_Y) {
              yval = 0;
              logError("Point marked invalid '?' for line " + lineNumber + " char " + ich0 + ":" + line.substring(0, ich0) + ">>>>" + line.substring(ich0));              
            }
            addPoint(new Coordinate().set(x, yval * yFactor), ipt++);
            if (debugging)
              logError("nx=" + ++nX + " " + x + " " + x/xFactor + " yval=" + yval);
          }
        }
        lastX = x;
        if (!haveWarned && ipt > nPoints) {
          logError("! points overflow nPoints!");
          haveWarned = true;
        }
        lastLine = line;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    checkZeroFill(ipt, lastXExpected);
    return xyCoords;
  }

  private void checkZeroFill(int ipt, double lastXExpected) {
    nptsFound = ipt;
    if (nPoints == nptsFound) {
      if (Math.abs(lastXExpected - lastX) > 0.00001)
        logError("Something went wrong! The last X value was " + lastX + " but expected " + lastXExpected);
    } else {
      logError("Decompressor did not find " + nPoints + " points -- instead "
          + nptsFound + " xyCoords.length set to " + nPoints);
      for (int i = nptsFound; i < nPoints; i++)
        addPoint(new Coordinate().set(0, Double.NaN), i);
    }
  }

  /**
   * Add a point at the given array index
   * @param pt
   * @param ipt
   */
  private void addPoint(Coordinate pt, int ipt) {
    if (ipt >= nPoints)
      return;
    xyCoords[ipt] = pt;
    double y = pt.getYVal();
    if (y > maxY)
      maxY = y;
    else if (y < minY)
      minY = y;
    if (debugging)
      logError("Coord: " + ipt + pt);
  }



  private void logError(String s) {
    if (debugging)
      Logger.debug(s);
    System.err.println(s);
    errorLog.append(s).appendC('\n');  
  }

  /**
   * Process the number or pseudo-digit on the current trimmed line.
   * 
   * Derive the yval from:
   * 
   * a) duplication, if dupCount > 0 or next char is [S-Zs],
   * 
   * b) difference, if the next char is [%J-Rj-r],
   * 
   * c) the next Squeezed number, if the next char is [@A-Ia-i], or
   * 
   * d) the next floating point number, if the next char is [+-.0-9]
   * 
   * 
   * 
   * @param yval
   * @return the double value, or NaN if any other character is found
   */
  private double nextValue(double yval) {
    if (dupCount > 0)
      return getDuplicate(yval);
    char ch = skipUnknown();
    switch (actions[ch]) {
    case ACTION_DIF:
      isDIF = true;
      return yval + (lastDif = readNextInteger(
          ch == '%' ? 0 : ch <= 'R' ? ch - 'I' : 'i' - ch));
    case ACTION_DUP:
      dupCount = readNextInteger((ch == 's' ? 9 : ch - 'R')) - 1;
      return getDuplicate(yval);
    case ACTION_SQZ:
      yval = readNextSqueezedNumber(ch);
      break;
    case ACTION_NUMERIC:
      ich--;
      yval = readSignedFloat();
      break;
    case ACTION_INVALID:
      yval = INVALID_Y;
      break;
    default:
      yval = Double.NaN;
      break;
    }
    isDIF = false;
    return yval;
  }
  
  private boolean isDIF = true;

  /**
   * Skip all unknown characters, setting the ich field to the first known character or to 0 if the end of line has been reached
   * 
   * @return the next known character, or 0 if end of line
   */
  private char skipUnknown() {
    char ch = '\0';
    while (ich < lineLen && actions[(int) (ch = line.charAt(ich++))] == ACTION_UNKNOWN) {
    }
    return ch;
  }

  /**
   * Read x or y as a number, possibly signed and possibly exponential of the form
   * xxxE[+-]nn or xxxE[+-]nnn.
   * 
   * @return the parsed number
   * @throws NumberFormatException
   */
  private double readSignedFloat() throws NumberFormatException {
    int ich0 = ich;
    char ch = '\0';
    while (ich < lineLen && delimiters.indexOf(ch = line.charAt(ich)) >= 0)
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
    if (scanToNonnumeric() == 'E' && ich + 3 < lineLen) {
      // looking at >E+xxx
      // Scan E..., specifically assuming that "E+" or "E-" will be followed by two
      // or three digits exactly. No check is made that the two characters following
      // the sign are actually numeric, only that we have all the possible numbers. 
      switch (line.charAt(ich + 1)) {
      case '-':
      case '+':
        ich += 4;
        // looking at E+xx>x
        if (ich < lineLen && (ch = line.charAt(ich)) >= '0' && ch <= '9')
          ich++;
        break;
      }
    }
    return factor * Double.parseDouble(line.substring(ich0, ich));
  }

  private double getDuplicate(double yval) {
      dupCount--;
      return (isDIF ? yval + lastDif : yval);
  }

  /**
   * Starting with the given digit, read the next digits, constructing an integer.
   * @param n
   * @return the integer
   */
  private int readNextInteger(int n) {
    char c = 0;
    while (ich < lineLen && (c = line.charAt(ich)) >= '0' && c <= '9') {
      n = n * 10 + (n < 0 ? '0' - c : c - '0');
      ich++;
    }
    return n;
  }

  /**
   * Get a squeezed number, negative if the SQZ digit was [a-i], false if [@A-I].
   * 
   * @param ch the character [a-i@A-I] substituting for the delimiter, sign, and first digit
   * 
   * @return the parsed number or Double.NaN if end-of-line is reached
   *         prematurely
   */
  private double readNextSqueezedNumber(char ch) {
    int ich0 = ich;
    scanToNonnumeric();
    return Double.parseDouble((ch > 0x60 ? 0x60 - ch : ch - 0x40) + line.substring(ich0, ich));
  }
  
  /**
   * Skip through anything not [0-9] or '.'
   * 
   * @return next character or \0 if end of line
   */
  private char scanToNonnumeric() {
    char ch = 0;
    while (ich < lineLen && ((ch = line.charAt(ich)) == '.' || ch >= '0' && ch <= '9'))
      ich++;
    return (ich < lineLen ? ch : '\0');
  }

//  private void testAlgorithm() {
//     line = "4265A8431K85L83L71K55P5j05k35k84k51j63n5K4M1j2j10j97k28j88j01j7K4or4k04k89";
//     lineLen = line.length();
//     System.out.println(getValue(allDelim));
//     while (ich < lineLen)
//       System.out.println(line.substring(0, ich) + "\n" + ipt++ + " " + (yval = getYValue()));
//     ipt= 0;
//  }

  /**
   * Report out the number of points found (for error reporting)
   * 
   * @return the number of points found
   */
  public int getNPointsFound() {
    return nptsFound;
  }
  
  @Override
  public boolean hasNext() {
    return (ich < lineLen || dupCount > 0);
  }

  @Override
  public Double next() {
    return (hasNext() ? Double.valueOf(lastY = nextValue(lastY)) : null);
  }

  @Override
  public void remove() {
  }

}
