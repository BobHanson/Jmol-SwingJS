/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-05 09:07:28 -0500 (Thu, 05 Apr 2007) $
 * $Revision: 7326 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.util;


import org.jmol.c.STR;
import org.jmol.script.T;

import javajs.util.P3;
import javajs.util.P3i;

public class TempArray {

  public TempArray() {
  }

  
  public void clear() {
    clearTempPoints();
    clearTempScreens();
    //clearTempBooleans();
  }
  
  private static int findBestFit(int size, int[] lengths) {
    int iFit = -1;
    int fitLength = Integer.MAX_VALUE;

    for (int i = lengths.length; --i >= 0;) {
      int freeLength = lengths[i];
      if (freeLength >= size && freeLength < fitLength) {
        fitLength = freeLength;
        iFit = i;
      }
    }
    if (iFit >= 0)
      lengths[iFit] = 0;
    return iFit;
  }

  private static int findShorter(int size, int [] lengths) {
    for (int i = lengths.length; --i >= 0;)
      if (lengths[i] == 0) {
        lengths[i] = size;
        return i;
      }
    int iShortest = 0;
    int shortest = lengths[0];
    for (int i = lengths.length; --i > 0;)
      if (lengths[i] < shortest) {
        shortest = lengths[i];
        iShortest = i;
      }
    if (shortest < size) {
      lengths[iShortest] = size;
      return iShortest;
    }
    return -1;
  }

  ////////////////////////////////////////////////////////////////
  // temp Points
  ////////////////////////////////////////////////////////////////
  private final static int freePointsSize = 6;
  private final int[] lengthsFreePoints = new int[freePointsSize];
  private final P3[][] freePoints = new P3[freePointsSize][];

  private void clearTempPoints() {
    for (int i = 0; i < freePointsSize; i++) {
      lengthsFreePoints[i] = 0;
      freePoints[i] = null;
    }
  }
  
  public P3[] allocTempPoints(int size) {
    P3[] tempPoints;
    int iFit = findBestFit(size, lengthsFreePoints);
    if (iFit > 0) {
      tempPoints = freePoints[iFit];
    } else {
      tempPoints = new P3[size];
      for (int i = size; --i >= 0;)
        tempPoints[i] = new P3();
    }
    return tempPoints;
  }

  public void freeTempPoints(P3[] tempPoints) {
    for (int i = 0; i < freePoints.length; i++)
      if (freePoints[i] == tempPoints) {
        lengthsFreePoints[i] = tempPoints.length;
        return;
      }
    int iFree = findShorter(tempPoints.length, lengthsFreePoints);
    if (iFree >= 0)
      freePoints[iFree] = tempPoints;
  }

  ////////////////////////////////////////////////////////////////
  // temp Screens
  ////////////////////////////////////////////////////////////////
  private final static int freeScreensSize = 6;
  private final int[] lengthsFreeScreens = new int[freeScreensSize];
  private final P3i[][] freeScreens = new P3i[freeScreensSize][];

  private void clearTempScreens() {
    for (int i = 0; i < freeScreensSize; i++) {
      lengthsFreeScreens[i] = 0;
      freeScreens[i] = null;
    }
  }
  
  public P3i[] allocTempScreens(int size) {
    P3i[] tempScreens;
    int iFit = findBestFit(size, lengthsFreeScreens);
    if (iFit > 0) {
      tempScreens = freeScreens[iFit];
    } else {
      tempScreens = new P3i[size];
      for (int i = size; --i >= 0;)
        tempScreens[i] = new P3i();
    }
    return tempScreens;
  }

  public void freeTempScreens(P3i[] tempScreens) {
    for (int i = 0; i < freeScreens.length; i++)
      if (freeScreens[i] == tempScreens) {
        lengthsFreeScreens[i] = tempScreens.length;
        return;
      }
    int iFree = findShorter(tempScreens.length, lengthsFreeScreens);
    if (iFree >= 0)
      freeScreens[iFree] = tempScreens;
  }

  ////////////////////////////////////////////////////////////////
  // temp EnumProteinStructure
  ////////////////////////////////////////////////////////////////
  private final static int freeEnumSize = 2;
  private final int[] lengthsFreeEnum = new int[freeEnumSize];
  private final STR[][] freeEnum = new STR[freeEnumSize][];

  public STR[] allocTempEnum(int size) {
    STR[] tempEnum;
    int iFit = findBestFit(size, lengthsFreeEnum);
    if (iFit > 0) {
      tempEnum = freeEnum[iFit];
    } else {
      tempEnum = new STR[size];
    }
    return tempEnum;
  }

  public void freeTempEnum(STR[] tempEnum) {
    for (int i = 0; i < freeEnum.length; i++)
      if (freeEnum[i] == tempEnum) {
        lengthsFreeEnum[i] = tempEnum.length;
        return;
      }
    int iFree = findShorter(tempEnum.length, lengthsFreeEnum);
    if (iFree >= 0)
      freeEnum[iFree] = tempEnum;
  }


  // admittedly an odd place for these two; just avoidng making a new class just for them.
  
  public static Object[] getSlabWithinRange(float min, float max) {
    return new Object[] { Integer.valueOf(T.range), 
        new Float[] {Float.valueOf(min), Float.valueOf(max)}, Boolean.FALSE, null };
  }

  public static Object[] getSlabObjectType(int tok, Object data, boolean isCap, Object colorData) {
    return new Object[] { Integer.valueOf(tok), data, Boolean.valueOf(isCap), colorData };
  }
}
