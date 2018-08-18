/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

package org.jmol.quantum;



import javajs.util.PT;
import javajs.util.SB;


/**
 * Constants and static methods for quantum shells.
 */
public class QS {

  public QS() {
    //
  }
    
  final public static int S = 0;
  final public static int P = 1;
  final public static int SP = 2;
  final public static int DS = 3;
  final public static int DC = 4;
  final public static int FS = 5;
  final public static int FC = 6;
  final public static int GS = 7;
  final public static int GC = 8;
  final public static int HS = 9;
  final public static int HC = 10;
  final public static int IS = 11;
  final public static int IC = 12;

  public static int MAX_TYPE_SUPPORTED = FC;

  private final static int MAXID = 13;

  final public static int[] idSpherical = { S,   P,   SP,   DS,   DS,  FS,   FS,  GS,   GS,  HS,    HS,  IS,    IS};
  final public static String[] tags =     {"S", "P", "SP", "5D", "D", "7F", "F", "9G", "G", "11H", "H", "13I", "I"};
  final public static String[] tags2 =    {"S", "X", "SP", "5D", "XX", "7F", "XXX", "9G", "XXXX", "11H", "XXXXX", "13I", "XXXXXX"};
  
  final public static String CANONICAL_DC_LIST = "DXX   DYY   DZZ   DXY   DXZ   DYZ";
  final public static String CANONICAL_DS_LIST = "d0    d1+   d1-   d2+   d2-";
  final public static String CANONICAL_FC_LIST = "XXX   YYY   ZZZ   XYY   XXY   XXZ   XZZ   YZZ   YYZ   XYZ";
  final public static String CANONICAL_FS_LIST = "f0    f1+   f1-   f2+   f2-   f3+   f3-";

//  S("S","S",0,0),
//  P("P","X",1,1),
//  SP("SP","SP",2,2),
//  D_SPHERICAL("5D","5D",3,3),
//  D_CARTESIAN("D","XX",4,3),
//  F_SPHERICAL("7F","7F",5,5),
//  F_CARTESIAN("F","XXX",6,5),
//  G_SPHERICAL("9G","9G",7,7),
//  G_CARTESIAN("G","XXXX",8,7),
//  H_SPHERICAL("11H","11H",9,9),
//  H_CARTESIAN("H","XXXXX",10,9),
//  I_SPHERICAL("13I","13I",11,11),
//  I_CARTESIAN("I","XXXXXX",12,11);

  public static boolean isQuantumBasisSupported(char ch) {
    return ("SPLDF".indexOf(Character.toUpperCase(ch)) >= 0);
  }
  

  public static int[][] getNewDfCoefMap() {
    return new int[][] { 
        new int[1],  //0 S 0
        new int[3],  //1 P
        new int[4],  // SP
        new int[5],  //2 D5 3
        new int[6],  //2 D6 
        new int[7],  //3 F7 5
        new int[10], //3 F10
        new int[9],  //4 G9 7
        new int[15], //4 G15
        new int[11], //5 H11 == 2*5 + 1
        new int[21], //5 H21 == (5+1)(5+2)/2 = (n*n+3*n +2)/2 = n(n+3)/2 + 1
        new int[13], //6 I13 == 2*6 + 1
        new int[28]  //6 I28 == (6+1)(6+2)/2
    };
  }

  public static int getItem(int i) {
    return (i >= 0 && i < MAXID ? i : -1);
  }

  public static int getQuantumShellTagID(String tag) {
    return (tag.equals("L") ? SP : getQuantumShell(tag));
  }

  private static int getQuantumShell(String tag) {
    for (int i = 0; i < MAXID; i++)
      if (tags[i].equals(tag) || tags2[i].equals(tag))
        return i;
    return -1;
  }

  final public static int getQuantumShellTagIDSpherical(String tag) {
    if (tag.equals("L"))
      return SP;
    int id = getQuantumShell(tag);
    return (id < 0 ? id : idSpherical[id]);
  }
  
  final public static String getQuantumShellTag(int id) {
    return (id >= 0 && id < MAXID ? tags[id] : "" + id);
  }

  final public static String getMOString(float[] lc) {
    SB sb = new SB();
    if (lc.length == 2)
      return "" + (int)(lc[0] < 0 ? -lc[1] : lc[1]);
    sb.appendC('[');
    for (int i = 0; i < lc.length; i += 2) {
      if (i > 0)
        sb.append(", ");
      sb.appendF(lc[i]).append(" ").appendI((int) lc[i + 1]);
    }
    sb.appendC(']');
    return sb.toString();
  }

  

  
  // Jmol's ordering is based on GAUSSIAN
  
  
  // We don't modify the coefficients at read time, only create a 
  // map to send to MOCalculation. 

  // DS: org.jmol.quantum.MOCalculation expects 
  //   d2z^2-x2-y2, dxz, dyz, dx2-y2, dxy
  
  // DC: org.jmol.quantum.MOCalculation expects 
  //      Dxx Dyy Dzz Dxy Dxz Dyz
  
  // FS: org.jmol.quantum.MOCalculation expects
  //        as 2z3-3x2z-3y2z
  //               4xz2-x3-xy2
  //                   4yz2-x2y-y3
  //                           x2z-y2z
  //                               xyz
  //                                  x3-3xy2
  //                                     3x2y-y3

  // FC: org.jmol.quantum.MOCalculation expects
  //           xxx yyy zzz xyy xxy xxz xzz yzz yyz xyz

  // These strings are the equivalents found in the file in Jmol order.
  // DO NOT CHANGE THESE. They are in the order the MOCalculate expects. 
  // Subclassed readers can make their own to match. 
    
  /* Molden -- same as Gaussian, so no need to map these:
  5D: D 0, D+1, D-1, D+2, D-2
  6D: xx, yy, zz, xy, xz, yz

  7F: F 0, F+1, F-1, F+2, F-2, F+3, F-3
 10F: xxx, yyy, zzz, xyy, xxy, xxz, xzz, yzz, yyz, xyz

  9G: G 0, G+1, G-1, G+2, G-2, G+3, G-3, G+4, G-4
 15G: xxxx yyyy zzzz xxxy xxxz yyyx yyyz zzzx zzzy,
      xxyy xxzz yyzz xxyz yyxz zzxy
  */
  
  /**
   * 
   * @param map map to fill
   * @param fileList reader-dependent list
   * @param jmolList Jmol's reordering of that list
   * @param minLength minimum token length so that "G 0" is not taken as two token -- depends upon reader
   * @return true if successful
   */
  public static boolean createDFMap(int[] map, String fileList,
                                    String jmolList, int minLength) {

    
    // say we had line = "251   252   253   254   255"  i  points here
    // Jmol expects list "255   252   253   254   251"  pt points here
    // we need an array that reads
    //                    [4     0     0     0    -4]
    // meaning add that number to the pointer for this coef.
    String[] tokens = PT.getTokens(fileList);
    boolean isOK = true;
    for (int i = 0; i < map.length; i++) {
      String key = tokens[i];
      if (key.length() >= minLength) {
        int pt = jmolList.indexOf(key);
        if (pt >= 0) {
          pt /= 6;
          map[pt] = i - pt;
          continue;
        }
      }
      isOK = false;
      break;
    }
    if (!isOK)
      map[0] = Integer.MIN_VALUE;
    return isOK;    
  }

}
