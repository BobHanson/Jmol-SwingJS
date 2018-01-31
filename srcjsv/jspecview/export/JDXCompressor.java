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

package jspecview.export;

import javajs.util.DF;
import javajs.util.SB;

import org.jmol.util.Logger;

import jspecview.common.Coordinate;

/**
 * <code>JDXCompressor</code> takes an array of <code>Coordinates<code> and
 * compresses them into one of the JCAMP-DX compression formats: DIF, FIX, PAC
 * and SQZ.
 * 
 * @author Christopher Muir
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 * @see jspecview.common.Coordinate
 * @see jspecview.source.JDXDecompressor
 */

class JDXCompressor {

  /**
   * Compresses the <code>Coordinate<code>s into DIF format
   * 
   * @param xyCoords
   *        the array of <code>Coordinate</code>s
   * @param startIndex
   *        the start index of the array of Coordinates to be compressed
   * @param endIndex
   *        the end index of the array of Coordinates to be compressed
   * @param step 
   * @param xFactor
   *        x factor for compression
   * @param yFactor
   *        y factor for compression
   * @param isDIFDUP
   * @return A String representing the compressed data
   */
  static String compressDIF(Coordinate[] xyCoords, int startIndex,
                            int endIndex, int step, double xFactor,
                            double yFactor, boolean isDIFDUP) {
    SB yStr = new SB();
    SB buffer = new SB();
    for (int i = startIndex; i != endIndex;) {
      buffer.append(fixIntNoExponent(xyCoords[i].getXVal() / xFactor));
      yStr.setLength(0);
      if (Logger.debugging)
        Logger.info("" + i + '\t' + xyCoords[i].getXVal() + '\t' + xyCoords[i].getYVal());
      long y1 = Math.round(xyCoords[i].getYVal() / yFactor);
      yStr.append(makeSQZ(y1));
      String lastDif = "";
      int nDif = 0;
      i += step;
      if (i == endIndex) {
        // we're done
        i -= step;
      } else {
        while (i + step != endIndex && yStr.length() < 50) {
          // Print remaining Y values on a line
          long y2 = Math.round(xyCoords[i].getYVal() / yFactor);
          // Calculate DIF value here
          String temp = makeDIF(y2 - y1);
          if (isDIFDUP && temp.equals(lastDif)) {
            nDif++;
          } else {
            lastDif = temp;
            if (nDif > 0) {
              yStr.append(makeDUP(nDif + 1));
              nDif = 0;
            }
            yStr.append(temp);
          }
          if (Logger.debugging)
            Logger.info("" + i + '\t' + xyCoords[i].getXVal() + '\t' + xyCoords[i].getYVal() + '\t' + y2 + '\t' + nDif + '\t' + yStr);
          y1 = y2;
          i += step;
        }
        if (nDif > 0)
          yStr.append(makeDUP(nDif + 1));
        // convert last digit of string to SQZ
        yStr.append(makeSQZ(xyCoords[i], yFactor));
        if (Logger.debugging)
          Logger.info("" + i + '\t' + xyCoords[i].getXVal() + '\t' + xyCoords[i].getYVal() + '\t' + nDif + '\t' + yStr);
      }
      buffer.append(yStr.toString()).append(Exporter.newLine);
      i += step;
    }
    // Get checksum line -- for an X-sequence check only
    buffer.append(
        fixIntNoExponent(xyCoords[endIndex].getXVal() / xFactor))
        .append(makeSQZ(xyCoords[endIndex], yFactor));
    buffer.append("  $$checkpoint").append(Exporter.newLine);
    return buffer.toString();
  }

  final static String spaces = "                    ";

	/**
	 * Compresses the <code>Coordinate<code>s into FIX format
	 * 
	 * @param xyCoords
	 *          the array of <code>Coordinate</code>s
	 * @param startIndex
	 *          startIndex the start index of the array of Coordinates to be
	 *          compressed
	 * @param step
	 * @param endIndex
	 *          endIndex the end index of the array of Coordinates to be
	 *          compressed
	 * @param xFactor
	 *          x factor for compression
	 * @param yFactor
	 *          y factor for compression
	 * @return A String representing the compressed data
	 */
	static String compressFIX(Coordinate[] xyCoords, int startIndex,
			int endIndex, int step, double xFactor, double yFactor) {
		endIndex += step;
		SB buffer = new SB();
		for (int i = startIndex; i != endIndex;) {
			leftJustify(buffer, "              ", 
					fixIntNoExponent(xyCoords[i].getXVal() / xFactor)); // 14 spaces
			for (int j = 0; j < 6 && i != endIndex; j++) {
			  rightJustify(buffer, "          ", ""
						+ Math.round(xyCoords[i].getYVal() / yFactor));
				buffer.append(" ");
				i += step;
			}
			buffer.append(Exporter.newLine);
		}

		return buffer.toString();
	}

  public static void leftJustify(SB s, String s1, String s2) {
    s.append(s2);
    int n = s1.length() - s2.length();
    if (n > 0)
      s.append(s1.substring(0, n));
  }
  
  public static void rightJustify(SB s, String s1, String s2) {
    int n = s1.length() - s2.length();
    if (n > 0)
      s.append(s1.substring(0, n));
    s.append(s2);
  }
  
  /**
   * Compresses the <code>Coordinate<code>s into SQZ format
   * 
   * @param xyCoords
   *        the array of <code>Coordinate</code>s
   * @param startIndex
   *        startIndex the start index of the array of Coordinates to be
   *        compressed
   * @param endIndex
   *        endIndex the end index of the array of Coordinates to be compressed
   * @param step 
   * @param xFactor
   *        x factor for compression
   * @param yFactor
   *        y factor for compression
   * @return A String representing the compressed data
   */
  static String compressSQZ(Coordinate[] xyCoords, int startIndex,
                            int endIndex, int step, double xFactor,
                            double yFactor) {
    SB yStr = new SB();
    endIndex += step;
    SB buffer = new SB();
    for (int i = startIndex; i == startIndex || i != endIndex;) {
      buffer.append(fixIntNoExponent(xyCoords[i].getXVal() / xFactor));
      yStr.setLength(0);
      yStr.append(makeSQZ(xyCoords[i], yFactor));
      i += step;
      while ((yStr.length() < 60) && i != endIndex) {
        yStr.append(makeSQZ(xyCoords[i], yFactor));
        i += step;
      }
      buffer.append(yStr.toString()).append(Exporter.newLine);
    }
    return buffer.toString();
  }

  /**
   * Compresses the <code>Coordinate<code>s into PAC format
   * 
   * @param xyCoords
   *        the array of <code>Coordinate</code>s
   * @param startIndex
   *        startIndex the start index of the array of Coordinates to be
   *        compressed
   * @param endIndex
   *        endIndex the end index of the array of Coordinates to be compressed
   * @param step 
   * @param xFactor
   *        x factor for compression
   * @param yFactor
   *        y factor for compression
   * @return A String representing the compressed data
   */
  static String compressPAC(Coordinate[] xyCoords, int startIndex,
                            int endIndex, int step, double xFactor,
                            double yFactor) {
    SB buffer = new SB();
    endIndex += step;
    for (int i = startIndex; i != endIndex;) {
      buffer.append(
          fixIntNoExponent(xyCoords[i].getXVal() / xFactor)).append(
          fixPacY(xyCoords[i].getYVal() / yFactor));
      i += step;
      for (int j = 0; j < 4 && i != endIndex; j++) {
        // Print remaining Y values on a line
        buffer.append(fixPacY(xyCoords[i].getYVal() / yFactor));
        i += step;
      }
      buffer.append(Exporter.newLine);
    }
    return buffer.toString();
  }

  private static String fixPacY(double y) {
    return (y < 0 ? "" : " ") + fixIntNoExponent(y);
  }

  /**
   * Makes a SQZ Character for a y value
   * 
   * @param pt
   *        the input point
   * @param yFactor
   * @return the SQZ character
   */
  private static String makeSQZ(Coordinate pt, double yFactor) {
    return makeSQZ(Math.round(pt.getYVal() / yFactor));
  }

  /**
   * Makes a SQZ Character
   * 
   * @param y
   *        the input number
   * @return the SQZ character
   */
  private static String makeSQZ(long y) {
    return compress(y, "@ABCDEFGHI", "abcdefghi");
  }

  /**
   * Makes a DIF Character
   * @param dy 
   * 
   * @return the DIF Character
   */
  private static String makeDIF(long dy) {
    return compress(dy, "%JKLMNOPQR", "jklmnopqr");
  }

  /**
   * Makes a DUP Character
   * @param y 
   * 
   * @return the DUP character
   */
  private static String makeDUP(long y) {
    return compress(y, "0STUVWXYZs", "");
  }

  /**
   * replace first character and "-" sign with a letter
   * 
   * @param y
   * @param strPos
   * @param strNeg
   * @return compressed string
   */
  private static String compress(long y, String strPos, String strNeg) {
    boolean negative = false;
    String yStr = String.valueOf(y);
    char ch = yStr.charAt(0);
    if (ch == '-') {
      negative = true;
      yStr = yStr.substring(1);
      ch = yStr.charAt(0);
    }
    char[] yStrArray = yStr.toCharArray();
    yStrArray[0] = (negative ? strNeg.charAt(ch - '1') : strPos
        .charAt(ch - '0'));
    return new String(yStrArray);
  }

  /**
   * Converts and returns the list of Coordinates as a string
   * 
   * @param xyCoords
   *        the array of coordinates
   * @param startIndex
   *        that start index
   * @param endIndex
   *        the end index
   * @param step 
   * @return returns the list of Coordinates as a string
   */
  static String getXYList(Coordinate[] xyCoords, int startIndex, int endIndex,
                          int step) {
    endIndex += step;
    SB buffer = new SB();
    for (int i = startIndex; i != endIndex; i += step) {
      Coordinate point = xyCoords[i];
      buffer.append(fixIntNoExponent(point.getXVal())).append(", ")
          .append(fixIntNoExponent(point.getYVal())).append(
              Exporter.newLine);
    }
    return buffer.toString();
  }

	private static String fixIntNoExponent(double x) {
	  return (x == Math.floor(x) ? String.valueOf((int) x) : DF.formatDecimalTrimmed(x, 10));
	}


}
