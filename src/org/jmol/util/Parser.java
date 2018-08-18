/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import javajs.util.PT;

import javajs.util.BS;

public class Parser {

  /**
   * parses a "dirty" string for floats. If there are non-float tokens, 
   * they are ignored. A bitset is used to assign values only to specific 
   * atoms in the set, not changing the values of the data array for other atoms.
   * thus, a data set can be incrementally added to in this way.
   * 
   *  @param str     the string to parse
   *  @param bs      the atom positions to assign
   *  @param data    the (sparce) array to fill
   * @return  number of floats
   */
  public static int parseStringInfestedFloatArray(String str, BS bs, float[] data) {
    return Parser.parseFloatArrayBsData(PT.getTokens(str), bs, data);
  }

  public static int parseFloatArrayBsData(String[] tokens, BS bs, float[] data) {
    int len = data.length;
    int nTokens = tokens.length;
    int n = 0;
    int max = 0;
    boolean haveBitSet = (bs != null);
    for (int i = (haveBitSet ? bs.nextSetBit(0) : 0); i >= 0 && i < len && n < nTokens; i = (haveBitSet ? bs.nextSetBit(i + 1) : i + 1)) {
      float f;
      while (Float.isNaN(f = PT.parseFloat(tokens[n++])) 
          && n < nTokens) {
      }
      if (!Float.isNaN(f))
        data[(max = i)] = f;
      if (n == nTokens)
        break;
    }
    return max + 1;
  }

  /**
   * the major lifter here.
   * 
   * @param str         string containing the data 
   * @param bs          selects specific rows of the data 
   * @param fieldMatch  a free-format field pointer, or a column pointer
   * @param fieldMatchColumnCount specifies a column count -- not free-format
   * @param matchData   an array of data to match (atom numbers)
   * @param field       a free-format field pointer, or a column pointer
   * @param fieldColumnCount specifies a column count -- not free-format
   * @param data        float array to modify or null if size unknown
   * @param firstLine   first line to parse (1 indicates all)
   * @return            data
   */
  public static float[] parseFloatArrayFromMatchAndField(
                                                         String str,
                                                         BS bs,
                                                         int fieldMatch,
                                                         int fieldMatchColumnCount,
                                                         int[] matchData,
                                                         int field,
                                                         int fieldColumnCount,
                                                         float[] data, int firstLine) {
    float f;
    int i = -1;
    boolean isMatch = (matchData != null);
    int[] lines = markLines(str, (str.indexOf('\n') >= 0 ? '\n' : ';'));
    int iLine = (firstLine <= 1 || firstLine >= lines.length ? 0 : firstLine - 1);
    int pt = (iLine == 0 ? 0 : lines[iLine - 1]);
    int nLines = lines.length;
    if (data == null)
      data = new float[nLines - iLine];
    int len = data.length;
    int minLen = (fieldColumnCount <= 0 ? Math.max(field, fieldMatch) : Math
        .max(field + fieldColumnCount, fieldMatch + fieldMatchColumnCount) - 1);
    boolean haveBitSet = (bs != null);
    for (; iLine < nLines; iLine++) {
      String line = str.substring(pt, lines[iLine]).trim();
      pt = lines[iLine];
      String[] tokens = (fieldColumnCount <= 0 ? PT.getTokens(line) : null);
      // check for inappropriate data -- line too short or too few tokens or NaN for data
      // and parse data
      if (fieldColumnCount <= 0) {
        if (tokens.length < minLen
            || Float.isNaN(f = PT.parseFloat(tokens[field - 1])))
          continue;
      } else {
        if (line.length() < minLen
            || Float.isNaN(f = PT.parseFloat(line.substring(field - 1, field
                + fieldColumnCount - 1))))
          continue;
      }
      int iData;
      if (isMatch) {
        iData = PT.parseInt(tokens == null ? line.substring(fieldMatch - 1,
            fieldMatch + fieldMatchColumnCount - 1) : tokens[fieldMatch - 1]);
        // in the fieldMatch column we have an integer pointing into matchData
        // we replace that number then with the corresponding number in matchData
        if (iData == Integer.MIN_VALUE || iData < 0 || iData >= len
            || (iData = matchData[iData]) < 0)
          continue;
        // and we set bs to indicate we are updating that value
        if (haveBitSet)
          bs.set(iData);
      } else {
        // no match data
        // bs here indicates the specific data elements that need filling
        if (haveBitSet) 
          i = bs.nextSetBit(i + 1);
        else
          i++;
        if (i < 0 || i >= len)
          return data;
        iData = i;
      }
      data[iData] = f;
      //System.out.println("data[" + iData + "] = " + data[iData]);
    }
    return data;
  }

  public static String fixDataString(String str) {
    str = str.replace(';', str.indexOf('\n') < 0 ? '\n' : ' ');
    str = PT.trim(str, "\n \t");
    str = PT.rep(str, "\n ", "\n");
    str = PT.rep(str, "\n\n", "\n");
    return str;    
  }
  
  public static int[] markLines(String data, char eol) {
    int nLines = 0;
    for (int i = data.length(); --i >=0;)
      if (data.charAt(i) == eol)
        nLines++;
    int[] lines = new int[nLines + 1];
    nLines = 0;
    int pt = 0;
    while ((pt = data.indexOf(eol, pt)) >= 0)
      lines[nLines++] = ++pt;
    lines[nLines] = data.length();
    return lines;
  }


}
