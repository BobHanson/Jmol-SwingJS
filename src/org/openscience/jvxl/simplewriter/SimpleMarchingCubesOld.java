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
package org.openscience.jvxl.simplewriter;

//import javax.util.BitSet;
//
//import javax.vecmath.Point3i;
//
//import org.jmol.jvxl.data.JvxlCoder;
//import org.jmol.jvxl.data.VolumeData;
//import org.jmol.util.Logger;

public class SimpleMarchingCubesOld {

//  /*
//   * SimpleMarchingCubesOld implements the original method of generating
//   * data, using an array that is size [12] to hold each cube's edge vertex data.
//   * and an array of size [nCubesY*nCubesZ][12] to hold a slice of cubes in memory.
//   * Turns out it is slower by about 10% and consumes far more memory than the 
//   * new algorithm I wrote Feb 10, 2008. Saved here for posterity -- Bob Hanson 
//   * 
//   * timing: SimpleMarchingCubes with 100,100,100:
//   * 
//   * getEdgeData: 641 ms
//   * getEdgeData: 1625 ms
//   * 
//   * old getEdgeData: 688 ms
//   * old getEdgeData: 1672 ms
//   * 
//   * An adaptation of Marching Cubes to include data slicing and the option
//   * for progressive reading of the data. Associated SurfaceReader and VoxelData
//   * structures are required to store the sequential values in the case of a plane
//   * and to deliver the sequential vertex numbers in any case.
//   * 
//   * Author: Bob Hanson, hansonr@stolaf.edu
//   * 
//   * The "Simple" version does not create triangle data, 
//   * just the JVXL fractionData string
//   *  
//   */
//
//  private VolumeData volumeData;
//  private float cutoff;
//  private boolean isCutoffAbsolute;
//  private boolean isXLowToHigh;
//  private StringXBuilder fractionData = new StringXBuilder();
//
//  private int cubeCountX, cubeCountY, cubeCountZ;
//  private int nY, nZ;
//
//  private BitSet bsVoxels = new BitSet();
//
//  public BitSet getBsVoxels() {
//    return bsVoxels;
//  }
//  
//  private int mode;
//  private final static int MODE_CUBE = 1;
//  private final static int MODE_BITSET = 2;
//  private final static int MODE_GETXYZ = 3;
//
//  private VoxelDataCreator vdc;
//  
//  public SimpleMarchingCubesOld(VoxelDataCreator vdc, VolumeData volumeData, float cutoff,
//      boolean isCutoffAbsolute ,   boolean isXLowToHigh) {
//    
//    // when just creating a JVXL file all you really need are:
//    //
//    // volumeData.voxelData[x][y][z]
//    // cutoff
//    //
//    
//    this.vdc = vdc;
//    this.volumeData = volumeData;
//    this.cutoff = cutoff;
//    this.isCutoffAbsolute = isCutoffAbsolute;
//    this.isXLowToHigh = isXLowToHigh;
//    
//    if (vdc == null) {
//      mode = MODE_CUBE;
//    } else {
//      mode = MODE_GETXYZ;
//    }
//
//    cubeCountX = volumeData.voxelCounts[0] - 1;
//    cubeCountY = (nY = volumeData.voxelCounts[1]) - 1;
//    cubeCountZ = (nZ = volumeData.voxelCounts[2]) - 1;
//    yzCount = nY * nZ;
//    setLinearOffsets();
//  }
//
//  private final float[] vertexValues = new float[8];
//  private final Point3i[] vertexPoints = new Point3i[8];
//  {
//    for (int i = 8; --i >= 0;)
//      vertexPoints[i] = new Point3i();
//  }
//
//  int edgeCount;
//
//  /* Note to Jason from Bob:
//   * 
//   * To just create a JVXL file, you need these five methods.
//   * Their output is the fractionData string buffer and the
//   * number of surface points
//   * 
//   * inputs required: 
//   * 
//   *  1) volumeData.voxelData[x][y][z]
//   *  2) cutoff
//   *  3) values created in MarchingCubes constructor
//   *  
//   * The first four methods are in org.jmol.jvxl.calc.MarchingCubes.java
//   * 
//   *  generateSurfaceData  -- isXLowToHigh false; isContoured false
//   *    -- triangle stuff at end not needed
//   *  propagateNeighborPointIndexes -- EXACTLY as is, no changes allowed
//   *  isInside -- EXACTLY as is -- defines what "inside" means
//   *  processOneCubical -- EXACTLY as is, no changes at all
//   *  SurfaceReader.getSurfacePointIndex -- your job
//   *    -- receives the point value data and positions
//   *    -- responsible for creating the fractionData character buffer
//   *    -- just return 0 since you are not creating triangles
//   *  
//   */
//  
//  private static int[] xyPlanePts = new int[] { 0, 1, 1, 0, 0, 1, 1, 0 };
//
//  public String getEdgeData() {
//
//    Logger.startTimer();
//    // set up the set of edge points in the YZ plane
//    // isoPointIndixes are indices into an array of Point3f values
//    // They will be initialized as -1 whenever a vertex is needed.
//    // But if just creating a JVXL file, all you need to do
//    // is set them to 0, not an index into any actual array.
//    
//    int[][] isoPointIndexes = new int[cubeCountY * cubeCountZ][12];
//
//    float[][] xyPlanes = (mode == MODE_GETXYZ ? new float[2][yzCount] : null);
//
//    int x0, x1, xStep, ptStep, pt, ptX;
//    if (isXLowToHigh) {
//      x0 = 0;
//      x1 = cubeCountX;
//      xStep = 1;
//      ptStep = yzCount;
//      pt = ptX = (yzCount - 1) - nZ - 1;
//      // we are starting at the top corner, in the next to last
//      // cell on the next to last row of the first plane
//    } else {
//      x0 = cubeCountX - 1;
//      x1 = -1;
//      xStep = -1;
//      ptStep = -yzCount;
//      pt = ptX = (cubeCountX * yzCount - 1) - nZ - 1;
//      // we are starting at the top corner, in the next to last
//      // cell on the next to last row of the next to last plane(!)
//    }
//    int cellIndex0 = cubeCountY * cubeCountZ - 1;
//    int cellIndex = cellIndex0;
//    float[][][] voxelData = (mode == MODE_CUBE ? volumeData.getVoxelData() : null);
//    for (int x = x0; x != x1; x += xStep, ptX += ptStep, pt = ptX, cellIndex = cellIndex0) {
//      if (mode == MODE_GETXYZ) {
//        float[] plane = xyPlanes[0];
//        xyPlanes[0] = xyPlanes[1];
//        xyPlanes[1] = plane;
//      }
//      for (int y = cubeCountY; --y >= 0; pt--) {
//        for (int z = cubeCountZ; --z >= 0; pt--, cellIndex--) {
//
//          
//          // set up the list of indices that need checking
//          
//          int[] voxelPointIndexes = propagateNeighborPointIndexes(x, y, z, pt,
//              isoPointIndexes, cellIndex);
//          
//          // create the bitset mask indicating which vertices are inside.
//          // 0xFF here means "all inside"; 0x00 means "all outside"
//          
//          int insideMask = 0;
//          for (int i = 8; --i >= 0;) {
//            
//            // cubeVertexOffsets just gets us the specific grid point relative
//            // to our base x,y,z cube position
//            
//            boolean isInside;
//            Point3i offset = cubeVertexOffsets[i];
//            int pti = pt + linearOffsets[i];
//            switch (mode) {
//            case MODE_GETXYZ:
//              vertexValues[i] = getValue(i, x + offset.x, y + offset.y, z
//                  + offset.z, pti, xyPlanes[xyPlanePts[i]]);
//              isInside = bsVoxels.get(pti);
//              break;
//            case MODE_BITSET:
//              isInside = bsVoxels.get(pti);
//              vertexValues[i] = (isInside ? 1 : 0);
//              break;
//            default:
//            case MODE_CUBE:
//              vertexValues[i] = voxelData[x + offset.x][y + offset.y][z
//                  + offset.z];
//              isInside = isInside(vertexValues[i], cutoff, isCutoffAbsolute);
//              if (isInside)
//                bsVoxels.set(pti);
//            }
//            if (isInside) {
//              insideMask |= 1 << i;
//            }
//          }
//
//          if (insideMask == 0) {
//            continue;
//          }
//          if (insideMask == 0xFF) {
//            continue;
//          }
//          // This cube is straddling the cutoff. We must check all edges 
//          
//          processOneCubical(insideMask, voxelPointIndexes, x, y, z, pt);
//        }
//      }
//    }
//    Logger.checkTimer("old getEdgeData");
//    return fractionData.toString();
//  }
//  
//  public static boolean isInside(float voxelValue, float max, boolean isAbsolute) {
//    return ((max > 0 && (isAbsolute ? Math.abs(voxelValue) : voxelValue) >= max) || (max <= 0 && voxelValue <= max));
//  }
//
//  BitSet bsValues = new BitSet();
//
//  private float getValue(@SuppressWarnings("unused") int i,
//                         int x, int y, int z,
//                         int pt, float[] tempValues) {
//    //if (bsValues.get(pt))
//      //return tempValues[pt % yzCount];
//    bsValues.set(pt);
//    float value = vdc.getValue(x, y, z);
//    tempValues[pt % yzCount] = value;
//    //System.out.println("xyz " + x + " " + y + " " + z + " v=" + value);
//    if (isInside(value, cutoff, isCutoffAbsolute))
//      bsVoxels.set(pt);
//    return value;
//  }
//
//  private final int[] nullNeighbor = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
//      -1, -1 };
//
//  private int[] propagateNeighborPointIndexes(int x, int y, int z,
//                                              @SuppressWarnings("unused") int pt,
//                                              int[][] isoPointIndexes,
//                                              int cellIndex) {
//    /*
//     * 
//     * We are running through the grid points in yz planes from high x --> low x
//     * and within those planes along strips from high y to low y
//     * and within those strips, from high z to low z. The "leading vertex" is 0, 
//     * and the "leading edges" are {0,3,8}. 
//     * 
//     * For each such cube, edges are traversed from high to low (11-->0)
//     * 
//     * Each edge has the potential to be "critical" and cross the surface.
//     * Setting -1 in voxelPointIndexes indicates that this edge needs checking.
//     * Otherwise, the crossing point for this edge is taken from the value
//     * already determined, because it has already been determined to be critical. 
//     *
//     * The above model, because it starts at HIGH x, requires that all x,y,z points 
//     * be in memory from the beginning. We could have instead used a progressive 
//     * streaming model, where we only pull in the slice of data that we need. In 
//     * that case, each edge corresponds to a specific pair of indices in our slice.
//     * 
//     * Say we have a 51 x 11 x 21 block of data. This represents a 50 x 10 x 20 set
//     * of cubes. If, instead of reading all the data, we pull in just the first two
//     * "slices" x=0(10x20), x=1(10x20), that is just 400 points. Once a slice of
//     * data is used, we can flush it -- it is never used again. 
//     * 
//     * When color mapping, we can do the same thing; we just have to put the verticies
//     * into bins based on which pair of slices will be relevant, and then make sure we
//     * process the verticies based on these bins. 
//     * 
//     * The JVXL format depends on a specific order of reading of the edge data. The
//     * progressive model completely messes this up. The vertices will be read in the 
//     * same order around the cube, but the "leading edges" will be {0,1,9}, not {0,3,8}. 
//     * We do know which edge is which, so we could construct a progressive model from
//     * a nonprogressive one, if necessary. 
//     * 
//     * All we are really talking about is the JVXL reader, because we can certainly
//     * switch to progressive mode in all the other readers.  
//     *  
//     *                      Y 
//     *                      4 --------4--------- 5  
//     *                     /|                   /|
//     *                    / |                  / |
//     *                   /  |                 /  |
//     *                  7   8                5   |
//     *                 /    |               /    9
//     *                /     |              /     |
//     *               7 --------6--------- 6      |
//     *               |      |             |      |
//     *               |      0 ---------0--|----- 1    X
//     *               |     /              |     /
//     *              11    /               10   /
//     *               |   3                |   1
//     *               |  /                 |  /
//     *               | /                  | /
//     *               3 ---------2-------- 2
//     *              Z 
//     * 
//     *  
//     */
//
//    /* DO NOT EVER CHANGE THIS */
//
//    
//    int[] voxelPointIndexes = isoPointIndexes[cellIndex];
//
//    boolean noYNeighbor = (y == cubeCountY - 1);
//    int[] yNeighbor = noYNeighbor ? nullNeighbor 
//        : isoPointIndexes[cellIndex + cubeCountZ];
//    boolean noZNeighbor = (z == cubeCountZ - 1);
//    int[] zNeighbor = noZNeighbor ? nullNeighbor
//        : isoPointIndexes[cellIndex + 1];
//    voxelPointIndexes[0] = -1;
//    voxelPointIndexes[2] = zNeighbor[0];
//    voxelPointIndexes[4] = yNeighbor[0];
//    voxelPointIndexes[6] = (noYNeighbor ? zNeighbor[4] : yNeighbor[2]);
//
//    if (isXLowToHigh) {
//      // reading x from low to high
//      if (x == 0) {
//        voxelPointIndexes[3] = -1;
//        voxelPointIndexes[8] = -1;
//        voxelPointIndexes[7] = yNeighbor[3];
//        voxelPointIndexes[11] = zNeighbor[8];
//      } else {
//        voxelPointIndexes[3] = voxelPointIndexes[1];
//        voxelPointIndexes[7] = voxelPointIndexes[5];
//        voxelPointIndexes[8] = voxelPointIndexes[9];
//        voxelPointIndexes[11] = voxelPointIndexes[10];
//      }
//      voxelPointIndexes[1] = -1;
//      voxelPointIndexes[5] = yNeighbor[1];
//      voxelPointIndexes[9] = -1;
//      voxelPointIndexes[10] = zNeighbor[9];
//    } else {
//      // reading x from high to low
//      if (x == cubeCountX - 1) {
//        voxelPointIndexes[1] = -1;
//        voxelPointIndexes[5] = yNeighbor[1];
//        voxelPointIndexes[9] = -1;
//        voxelPointIndexes[10] = zNeighbor[9];
//      } else {
//        voxelPointIndexes[1] = voxelPointIndexes[3];
//        voxelPointIndexes[5] = voxelPointIndexes[7];
//        voxelPointIndexes[9] = voxelPointIndexes[8];
//        voxelPointIndexes[10] = voxelPointIndexes[11];
//      }
//      voxelPointIndexes[3] = -1;
//      voxelPointIndexes[7] = yNeighbor[3];
//      voxelPointIndexes[8] = -1;
//      voxelPointIndexes[11] = zNeighbor[8];
//    }
//
//    return voxelPointIndexes;
//  }
//  
//  private static final int[] Pwr2 = new int[] { 1, 2, 4, 8, 16, 32, 64, 128,
//    256, 512, 1024, 2048 };
//
//  private boolean processOneCubical(int insideMask, int[] voxelPointIndexes,
//                                    @SuppressWarnings("unused") int x,
//                                    @SuppressWarnings("unused") int y,
//                                    @SuppressWarnings("unused") int z,
//                                    @SuppressWarnings("unused") int pt) {
//    
//    // the key to the algorithm is that we have a catalog that
//    // maps the inside-vertex mask to an edge mask. 
//    
//    int edgeMask = insideMaskTable[insideMask];
//    //for (int i =0; i < 8; i++) System.out.print("\nvpi for cell  " + pt + ": vertex " + i + ": " + voxelPointIndexes[i] + " " + Integer.toBinaryString(edgeMask));
//    boolean isNaN = false;
//    for (int iEdge = 12; --iEdge >= 0;) {
//      
//      // bit set to one means it's a relevant edge
//      
//      if ((edgeMask & Pwr2[iEdge]) == 0)
//        continue;
//      
//      // if we have a point already, we don't need to check this edge.
//      // for triangles, this will be an index into an array;
//      // for just creating JVXL files, this can just be 0
//      
//      if (voxelPointIndexes[iEdge] >= 0)
//        continue; // propagated from neighbor
//      
//      // here's an edge that has to be checked.
//      
//      // get the vertex numbers 0 - 7
//      
//      int vertexA = edgeVertexes[iEdge << 1];
//      int vertexB = edgeVertexes[(iEdge << 1) + 1];
//      
//      // pick up the actual value at each vertex
//      // this array of 8 values is updated as we go.
//      
//      float valueA = vertexValues[vertexA];
//      float valueB = vertexValues[vertexB];
//      
//      // we allow for NaN values -- missing triangles
//      
//      if (Float.isNaN(valueA) || Float.isNaN(valueB))
//        isNaN = true;
//      
//      // the exact point position -- not important for just
//      // creating the JVXL file. In that case, all you 
//      // need are the two values valueA and valueB and the cutoff.
//      // from those you can define the fractional offset
//      
//      // here is where we get the value and assign the point for that edge
//      // it is where the JVXL surface data line is appended
//      
//      voxelPointIndexes[iEdge] = edgeCount++;
//      //System.out.println(" pt=" + pt + " edge" + iEdge + " xyz " + x + " " + y + " " + z + " vertexAB=" + vertexA + " " + vertexB + " valueAB=" + valueA + " " + valueB + " f= " + (cutoff - valueA) / (valueB - valueA));
//      fractionData.append(JvxlCoder.jvxlFractionAsCharacter((cutoff - valueA) / (valueB - valueA)));
//    }
//    return !isNaN;
//  }
//
//  final static Point3i[] cubeVertexOffsets = { Point3i.new3(0, 0, 0), //0 pt
//    Point3i.new3(1, 0, 0), //1 pt + yz
//    Point3i.new3(1, 0, 1), //2 pt + yz + 1
//    Point3i.new3(0, 0, 1), //3 pt + 1
//    Point3i.new3(0, 1, 0), //4 pt + z
//    Point3i.new3(1, 1, 0), //5 pt + yz + z
//    Point3i.new3(1, 1, 1), //6 pt + yz + z + 1
//    Point3i.new3(0, 1, 1) //7 pt + z + 1 
//};
//
//private final int[] linearOffsets = new int[8];
//int yzCount;
//
///* set the linear offsets for unique cell ID
// * and for pointing into the inside/outside BitSet. 
// * Add offset to 0: x * (nY * nZ) + y * nZ + z 
// */
//void setLinearOffsets() {
//  linearOffsets[0] = 0;
//  linearOffsets[1] = yzCount;
//  linearOffsets[2] = yzCount + 1;
//  linearOffsets[3] = 1;
//  linearOffsets[4] = nZ;
//  linearOffsets[5] = yzCount + nZ;
//  linearOffsets[6] = yzCount + nZ + 1;
//  linearOffsets[7] = nZ + 1;
//}
//
//public int getLinearOffset(int x, int y, int z, int offset) {
//  return x * yzCount + y * nZ + z + linearOffsets[offset];
//}
//
//
//  /*                     Y 
//   *                      4 --------4--------- 5                     +z --------4--------- +yz+z                  
//   *                     /|                   /|                     /|                   /|
//   *                    / |                  / |                    / |                  / |
//   *                   /  |                 /  |                   /  |                 /  |
//   *                  7   8                5   |                  7   8                5   |
//   *                 /    |               /    9                 /    |               /    9
//   *                /     |              /     |                /     |              /     |
//   *               7 --------6--------- 6      |            +z+1 --------6--------- +yz+z+1|
//   *               |      |             |      |               |      |             |      |
//   *               |      0 ---------0--|----- 1    X          |      0 ---------0--|----- +yz    X(outer)    
//   *               |     /              |     /                |     /              |     /
//   *              11    /               10   /                11    /               10   /
//   *               |   3                |   1                  |   3                |   1
//   *               |  /                 |  /                   |  /                 |  /
//   *               | /                  | /                    | /                  | /
//   *               3 ---------2-------- 2                     +1 ---------2-------- +yz+1
//   *              Z                                           Z (inner)
//   * 
//   *                                                              streaming data offsets
//   * type 0: x-edges: 0 2 4 6
//   * type 1: y-edges: 8 9 10 11
//   * type 2: z-edges: 1 3 5 7
//   * 
//   * Data stream offsets for vertices, relative to point 0, based on reading 
//   * loops {for x {for y {for z}}} 0-->n-1
//   * y and z are numbers of grid points in those directions:
//   * 
//   *            0    1      2      3      4      5      6        7
//   *            0   +yz   +yz+1   +1     +z    +yz+z  +yz+z+1  +z+1     
//   * 
//   * These are just looked up in a table. After the first set of cubes, 
//   * we are only adding points 1, 2, 5 or 6. This means that initially
//   * we need two data slices, but after that only one (slice 1):
//   * 
//   *            base
//   *           offset 0    1      2      3      4      5      6     7
//   *  slice[0]        0                 +1     +z                 +z+1     
//   *  slice[1]  +yz        0     +1                   +z    +z+1      
//   * 
//   *  slice:          0    1      1      0      0      1      1     0
//   *  
//   *  We can request reading of two slices (2*nY*nZ data points) first, then
//   *  from then on, just nY*nZ points. "Reading" is really just being handed a 
//   *  pointer into an array. Perhaps that array is already filled completely;
//   *  perhaps it is being read incrementally. 
//   *  
//   *  As it is now, the JVXL data are just read into an [nX][nY][nZ] array anyway, 
//   *  so we can continue to do that with NON progressive files. 
//   */
//
//  private final static byte edgeVertexes[] = { 
//    0, 1, 1, 2, 2, 3, 3, 0, 4, 5,
//  /*0     1     2     3     4  */
//    5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };
//  /*5     6     7     8     9     10    11 */
//
//  private final static short insideMaskTable[] = { 0x0000, 0x0109, 0x0203,
//      0x030A, 0x0406, 0x050F, 0x0605, 0x070C, 0x080C, 0x0905, 0x0A0F, 0x0B06,
//      0x0C0A, 0x0D03, 0x0E09, 0x0F00, 0x0190, 0x0099, 0x0393, 0x029A, 0x0596,
//      0x049F, 0x0795, 0x069C, 0x099C, 0x0895, 0x0B9F, 0x0A96, 0x0D9A, 0x0C93,
//      0x0F99, 0x0E90, 0x0230, 0x0339, 0x0033, 0x013A, 0x0636, 0x073F, 0x0435,
//      0x053C, 0x0A3C, 0x0B35, 0x083F, 0x0936, 0x0E3A, 0x0F33, 0x0C39, 0x0D30,
//      0x03A0, 0x02A9, 0x01A3, 0x00AA, 0x07A6, 0x06AF, 0x05A5, 0x04AC, 0x0BAC,
//      0x0AA5, 0x09AF, 0x08A6, 0x0FAA, 0x0EA3, 0x0DA9, 0x0CA0, 0x0460, 0x0569,
//      0x0663, 0x076A, 0x0066, 0x016F, 0x0265, 0x036C, 0x0C6C, 0x0D65, 0x0E6F,
//      0x0F66, 0x086A, 0x0963, 0x0A69, 0x0B60, 0x05F0, 0x04F9, 0x07F3, 0x06FA,
//      0x01F6, 0x00FF, 0x03F5, 0x02FC, 0x0DFC, 0x0CF5, 0x0FFF, 0x0EF6, 0x09FA,
//      0x08F3, 0x0BF9, 0x0AF0, 0x0650, 0x0759, 0x0453, 0x055A, 0x0256, 0x035F,
//      0x0055, 0x015C, 0x0E5C, 0x0F55, 0x0C5F, 0x0D56, 0x0A5A, 0x0B53, 0x0859,
//      0x0950, 0x07C0, 0x06C9, 0x05C3, 0x04CA, 0x03C6, 0x02CF, 0x01C5, 0x00CC,
//      0x0FCC, 0x0EC5, 0x0DCF, 0x0CC6, 0x0BCA, 0x0AC3, 0x09C9, 0x08C0, 0x08C0,
//      0x09C9, 0x0AC3, 0x0BCA, 0x0CC6, 0x0DCF, 0x0EC5, 0x0FCC, 0x00CC, 0x01C5,
//      0x02CF, 0x03C6, 0x04CA, 0x05C3, 0x06C9, 0x07C0, 0x0950, 0x0859, 0x0B53,
//      0x0A5A, 0x0D56, 0x0C5F, 0x0F55, 0x0E5C, 0x015C, 0x0055, 0x035F, 0x0256,
//      0x055A, 0x0453, 0x0759, 0x0650, 0x0AF0, 0x0BF9, 0x08F3, 0x09FA, 0x0EF6,
//      0x0FFF, 0x0CF5, 0x0DFC, 0x02FC, 0x03F5, 0x00FF, 0x01F6, 0x06FA, 0x07F3,
//      0x04F9, 0x05F0, 0x0B60, 0x0A69, 0x0963, 0x086A, 0x0F66, 0x0E6F, 0x0D65,
//      0x0C6C, 0x036C, 0x0265, 0x016F, 0x0066, 0x076A, 0x0663, 0x0569, 0x0460,
//      0x0CA0, 0x0DA9, 0x0EA3, 0x0FAA, 0x08A6, 0x09AF, 0x0AA5, 0x0BAC, 0x04AC,
//      0x05A5, 0x06AF, 0x07A6, 0x00AA, 0x01A3, 0x02A9, 0x03A0, 0x0D30, 0x0C39,
//      0x0F33, 0x0E3A, 0x0936, 0x083F, 0x0B35, 0x0A3C, 0x053C, 0x0435, 0x073F,
//      0x0636, 0x013A, 0x0033, 0x0339, 0x0230, 0x0E90, 0x0F99, 0x0C93, 0x0D9A,
//      0x0A96, 0x0B9F, 0x0895, 0x099C, 0x069C, 0x0795, 0x049F, 0x0596, 0x029A,
//      0x0393, 0x0099, 0x0190, 0x0F00, 0x0E09, 0x0D03, 0x0C0A, 0x0B06, 0x0A0F,
//      0x0905, 0x080C, 0x070C, 0x0605, 0x050F, 0x0406, 0x030A, 0x0203, 0x0109,
//      0x0000 };

}
