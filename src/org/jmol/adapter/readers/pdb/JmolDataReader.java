/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.pdb;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.P3;

import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

/**
 * JmolData file reader, for a modified PDB format
 *
 */

public class JmolDataReader extends PdbReader {

  
  private Map<String, float[]> props;
  private String[] residueNames;
  private String[] atomNames;
  
  //  REMARK   6 Jmol PDB-encoded data: property atomno temperature;
  //  REMARK   6 Jmol atom names ... ... ...;
  //  REMARK   6 Jmol residue names ... ... ...;
  //  REMARK   6 Jmol data min = {1.0 -41.87 0.0} max = {26.0 66.53 0.0} unScaledXyz = xyz * {1.0 1.0 0.0} + {0.0 12.33 0.0} plotscale = {100 100 100};
  //  REMARK   6 Jmol property atomno [1.0, 2.0, ...];
  //  REMARK   6 Jmol property temperature [0.0, 23.0, 21.0, ...];

  @Override
  protected void checkRemark() {
    // REMARK 6 Jmol
    while (true) {
      if (line.length() < 30 || line.indexOf("Jmol") != 11)
        break;
      switch ("Ppard".indexOf(line.substring(16, 17))) {
      case 0: //Jmol PDB-encoded data
        props = new Hashtable<String, float[]>();
        asc.setInfo("jmolData", line);
        if (!line.endsWith("#noautobond"))
          line += "#noautobond";
        break;
      case 1: // Jmol property 
        int pt1 = line.indexOf("[");
        int pt2 = line.indexOf("]");
        if (pt1 < 25 || pt2 <= pt1)
          return;
        String name = line.substring(25, pt1).trim();
        line = line.substring(pt1 + 1, pt2).replace(',', ' ');
        String[] tokens = getTokens();
        Logger.info("reading " + name + " " + tokens.length);
        float[] prop = new float[tokens.length];
        for (int i = prop.length; --i >= 0;)
          prop[i] = parseFloatStr(tokens[i]);
        props.put(name, prop);
        break;
      case 2: // Jmol atom names
        //  REMARK   6 Jmol atom names ... ... ...;
        line = line.substring(27);
        atomNames = getTokens();
        Logger.info("reading atom names " + atomNames.length);
        break;
      case 3: // Jmol residue names
        //  REMARK   6 Jmol residue names ... ... ...;
        line = line.substring(30);
        residueNames = getTokens();
        Logger.info("reading residue names " + residueNames.length);
        break;
      case 4: //Jmol data min
        Logger.info(line);
        // The idea here is to use a line such as the following:
        //
        // REMARK   6 Jmol data min = {-1 -1 -1} max = {1 1 1} 
        //                      unScaledXyz = xyz / {10 10 10} + {0 0 0} 
        //                      plotScale = {100 100 100}
        //
        // to pass on to Jmol how to graph non-molecular data. 
        // The format allows for the actual data to be linearly transformed
        // so that it fits into the PDB format for x, y, and z coordinates.
        // This adapter will then unscale the data and also pass on to
        // Jmol the unit cell equivalent that takes the actual data (which
        // will be considered the fractional coordinates) to Jmol coordinates,
        // which will be a cube centered at {0 0 0} and ranging from {-100 -100 -100}
        // to {100 100 100}.
        //
        // Jmol 12.0.RC23 uses this to pass through the adapter a quaternion,
        // ramachandran, or other sort of plot.

        float[] data = new float[15];
        Parser.parseStringInfestedFloatArray(
            line.substring(10).replace('=', ' ').replace('{', ' ')
                .replace('}', ' '), null, data);
        P3 minXYZ = P3.new3(data[0], data[1], data[2]);
        P3 maxXYZ = P3.new3(data[3], data[4], data[5]);
        fileScaling = P3.new3(data[6], data[7], data[8]);
        fileOffset = P3.new3(data[9], data[10], data[11]);
        P3 plotScale = P3.new3(data[12], data[13], data[14]);
        if (plotScale.x <= 0)
          plotScale.x = 100;
        if (plotScale.y <= 0)
          plotScale.y = 100;
        if (plotScale.z <= 0)
          plotScale.z = 100;
        if (fileScaling.y == 0)
          fileScaling.y = 1;
        if (fileScaling.z == 0)
          fileScaling.z = 1;
        setFractionalCoordinates(true);
        latticeCells = new int[4];
        asc.xtalSymmetry = null;
        setUnitCell(plotScale.x * 2 / (maxXYZ.x - minXYZ.x), plotScale.y * 2
            / (maxXYZ.y - minXYZ.y), plotScale.z * 2
            / (maxXYZ.z == minXYZ.z ? 1 : maxXYZ.z - minXYZ.z), 90, 90, 90);
        unitCellOffset = P3.newP(plotScale);
        unitCellOffset.scale(-1);
        getSymmetry();
        symmetry.toFractional(unitCellOffset, false);
        unitCellOffset.scaleAdd2(-1f, minXYZ, unitCellOffset);
        symmetry.setOffsetPt(unitCellOffset);
        asc.setInfo("jmolDataScaling", new P3[] { minXYZ, maxXYZ, plotScale });
        doApplySymmetry = true;
        break;
      }
      break;
    }
    checkCurrentLineForScript();
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    if (residueNames != null && atom.index < residueNames.length)
      atom.group3 = residueNames[atom.index];
    if (atomNames != null && atom.index < atomNames.length)
      atom.atomName = atomNames[atom.index];
  }
  
  @Override
  protected void finalizeSubclassReader() throws Exception {
    asc.setCurrentModelInfo("jmolDataProperties", props);
    finalizeReaderPDB();
  }

}

