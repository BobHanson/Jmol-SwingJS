/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.more;

import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.Logger;
import javajs.util.P3;


/**
 * Amber Coordinate File Reader
 * 
 * not a stand-alone reader -- must be after COORD keyword in LOAD command
 * 
 */

public class MdCrdReader extends AtomSetCollectionReader {

  @Override 
  protected void setup(String fullPath, Map<String, Object> htParams, Object readerOrDocument) {
    requiresBSFilter = true;
    setupASCR(fullPath, htParams, readerOrDocument);
  }
  
  @Override
  protected void initializeReader() {
    initializeTrajectoryFile();
  }

  @Override
  protected boolean checkLine() throws Exception {
    readCoordinates();
    Logger.info("Total number of trajectory steps=" + trajectorySteps.size());
    continuing = false;
    return false;
  }

  private void readCoordinates() throws Exception {
    line = null;
    int ac = (bsFilter == null ? templateAtomCount : ((Integer) htParams
        .get("filteredAtomCount")).intValue());
    boolean isPeriodic = htParams.containsKey("isPeriodic");
    int floatCount = templateAtomCount * 3 + (isPeriodic ? 3 : 0);
    while (true)
      if (doGetModel(++modelNumber, null)) {
        P3[] trajectoryStep = new P3[ac];
        if (!getTrajectoryStep(trajectoryStep, isPeriodic))
          return;
        trajectorySteps.addLast(trajectoryStep);
        if (isLastModel(modelNumber))
          return;
      } else {
        if (!skipFloats(floatCount))
          return;
      }
  }

  private int ptFloat = 0;
  private int lenLine = 0;

  private float getFloat() throws Exception {
    while (line == null || ptFloat >= lenLine) {
      if (rd() == null)
        return Float.NaN;
      ptFloat = 0;
      lenLine = line.length();
    }
    ptFloat += 8;
    return parseFloatRange(line, ptFloat - 8, ptFloat);
  }

  private P3 getPoint() throws Exception {
    float x = getFloat();
    float y = getFloat();
    float z = getFloat();
    return (Float.isNaN(z) ? null : P3.new3(x, y, z));
  }

  private boolean getTrajectoryStep(P3[] trajectoryStep, boolean isPeriodic)
      throws Exception {
    int ac = trajectoryStep.length;
    int n = -1;
    for (int i = 0; i < templateAtomCount; i++) {
      P3 pt = getPoint();
      if (pt == null)
        return false;
      if (bsFilter == null || bsFilter.get(i)) {
        if (++n == ac)
          return false;
        trajectoryStep[n] = pt;
      }
    }
    if (isPeriodic)
      getPoint(); // why? not in specs?
    return (line != null);
  }

  private boolean skipFloats(int n) throws Exception {
    int i = 0;
    // presumes float sets are separated by new line
    while (i < n && rd() != null)
      i += getTokens().length;
    return (line != null);
  }
}
