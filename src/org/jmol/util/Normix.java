/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-04-03 02:22:41 -0500 (Thu, 03 Apr 2014) $
 * $Revision: 19589 $
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

package org.jmol.util;

import javajs.J2SRequireImport;
import javajs.util.V3;

import org.jmol.java.BS;


/**
 * Provides quantization of normalized vectors so that shading for
 * lighting calculations can be handled by a simple index lookup
 *<p>
 * A 'normix' is a normal index, represented as a short
 *
 * @author Miguel, miguel@jmol.org
 */
@J2SRequireImport({org.jmol.util.Geodesic.class})
public class Normix {

  private final static int NORMIX_GEODESIC_LEVEL = Geodesic.standardLevel;

  private static short normixCount;
  public static short getNormixCount() {
    // Grahics3D, Normix.setInverseNormixes
    return (normixCount > 1 ? normixCount : 
      (normixCount = Geodesic.getVertexCount(NORMIX_GEODESIC_LEVEL)));
  }
  
  public static BS newVertexBitSet() {
    return BS.newN(getNormixCount());
  }

  private static V3[] vertexVectors;
  public static V3[] getVertexVectors() {
    // Graphics3D, isosurfaceRenderer normals, below
    if (vertexVectors == null)
      vertexVectors = Geodesic.getVertexVectors();
    return vertexVectors;
  }

  private static short[] inverseNormixes;
  
  public static void setInverseNormixes() {
    // Mesh.invertNormixes
    if (inverseNormixes != null)
      return;
    getNormixCount();
    getVertexVectors();
    inverseNormixes = new short[normixCount];    // level 0 1 2 3
    // vertices 12, 42, 162, 642
    BS bsTemp = new BS();
    for (int n = normixCount; --n >= 0;) {
      V3 v = vertexVectors[n];
      inverseNormixes[n] = getNormix(-v.x, -v.y, -v.z, NORMIX_GEODESIC_LEVEL,
          bsTemp);
    }
  }
  public static short getInverseNormix(short normix) {
    // from Mesh
    return inverseNormixes[normix];
  }

  private static short[][] neighborVertexesArrays;
  private static short[][] getNeighborVertexArrays() {
    if (neighborVertexesArrays == null) {
       neighborVertexesArrays = Geodesic.getNeighborVertexesArrays();
    }
    return neighborVertexesArrays;
  }

  public static final short NORMIX_NULL = 9999;
     // graphics3D, Mesh
  
  public static short getNormixV(V3 v, BS bsTemp) {
    // envelope, mesh, polyhedra only
    return getNormix(v.x, v.y, v.z, NORMIX_GEODESIC_LEVEL, bsTemp);
  }

  public static short get2SidedNormix(V3 v, BS bsTemp) {
    // ellipsoid arc and CGO and polyhedra only
    return (short) ~getNormixV(v, bsTemp);
  }

  private static short getNormix(double x, double y, double z, int geodesicLevel, BS bsConsidered) {
    // envelope, mesh, polyhedra, ellipsoid
    short champion;
    double t;
    if (z >= 0) {
      champion = 0;
      t = z - 1;
    } else {
      champion = 11;
      t = z - (-1);
    }
    bsConsidered.clearAll();
    bsConsidered.set(champion);
    getVertexVectors();
    getNeighborVertexArrays();
    double championDist2 = x*x + y*y + t*t;
    for (int lvl = 0; lvl <= geodesicLevel; ++lvl) {
      short[] neighborVertexes = neighborVertexesArrays[lvl];
      for (int offsetNeighbors = 6 * champion,
             i = offsetNeighbors + (champion < 12 ? 5 : 6);
           --i >= offsetNeighbors; ) {
        short challenger = neighborVertexes[i];
        if (bsConsidered.get(challenger))
            continue;
        bsConsidered.set(challenger);
        V3 v = vertexVectors[challenger];
        double d;
        d = v.x - x;
        double d2 = d * d;
        if (d2 >= championDist2)
          continue;
        d = v.y - y;
        d2 += d * d;
        if (d2 >= championDist2)
          continue;
        d = v.z - z;
        d2 += d * d;
        if (d2 >= championDist2)
          continue;
        champion = challenger;
        championDist2 = d2;
      }
    }
    return champion;
  }
  
}
