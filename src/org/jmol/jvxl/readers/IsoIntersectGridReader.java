/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.jvxl.readers;

import org.jmol.util.Logger;

/**
 * A class to read a linear combination of cube file data.
 * 
 * readerData is Object[] { VolumeFileReader[], float[] }
 * 
 */
class IsoIntersectGridReader extends VolumeFileReader {

  private VolumeFileReader[] readers;
  private float[] factors;
  public IsoIntersectGridReader(){
    super();
  }
  
  @Override
  protected void init(SurfaceGenerator sg) {
    initSR(sg);
    Object[] data = (Object[]) sg.getReaderData();
    readers = (VolumeFileReader[]) data[0];
    factors = (float[]) data[1];
  }
  
  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    for (int i = readers.length; -- i >= 0;)
      if (!readers[i].readVolumeParameters(isMapData))
        return false;
    return true;
  }

  @Override
  protected float getNextVoxelValue() throws Exception {
    float f = 0;
    for (int i = readers.length; -- i >= 0;)
      f += factors[i] * readers[i].getNextVoxelValue();
    return f;
  }

  @Override
  protected void closeReader() {
    if (readerClosed)
      return;
    readerClosed = true;
    for (int i = readers.length; -- i >= 0;)
      readers[i].closeReaderSFR();
    if (nData == 0 || dataMax == -Float.MAX_VALUE)
      return;
    dataMean /= nData;
    Logger.info("IsoIntersectFileReader closing file: " + nData
        + " points read \ndata min/max/mean = " + dataMin + "/" + dataMax + "/"
        + dataMean);
  }

  @Override
  protected void readParameters() throws Exception {
    // unused    
  }
  
}
