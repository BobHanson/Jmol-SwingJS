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

import java.io.BufferedReader;


import org.jmol.util.Logger;

import javajs.util.CU;
import javajs.util.P3;
import javajs.util.PT;

/*
 * 
 * a Kinemage contact dot reader
 * 
 * Visualizing and Quantifying Molecular Goodness-of-Fit:
 * Small-probe Contact Dots with Explicit Hydrogen Atoms
 * J. Michael Word, Simon C. Lovell, Thomas H. LaBean, Hope C. Taylor
 * Michael E. Zalis, Brent K. Presley, Jane S. Richardson
 * and David C. Richardson, J. Mol. Biol. (1999) 285, 1711-1733
 * 
 * see http://kinemage.biochem.duke.edu/software/probe.php
 * 
 */


class KinemageReader extends PmeshReader {

  private final static int POINTS_ALL = 0;
  private final static int POINTS_MCMC = 1;
  private final static int POINTS_SCSC = 2;
  private final static int POINTS_MCSC = 3;
  private final static int POINTS_HETS = 4;
  
  private int nDots = 0;
  private float vMin = -Float.MAX_VALUE;
  private float vMax = Float.MAX_VALUE;
  private int pointType;
  private String findString;

  KinemageReader(){}
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2PR(sg, br);
    type = "kinemage";
    setHeader();
  }

  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    if (params.parameters != null && params.parameters.length >= 2) {
      vMin = params.parameters[1];
      vMax = (params.parameters.length >= 3 ? params.parameters[2] : vMin);
      pointType = (params.parameters.length >= 4 ? (int) params.parameters[3] : POINTS_ALL);
      findString = params.calculationType;
    }
    return true;
  }

  @Override
  protected boolean readVertices() throws Exception {
    // also reads polygons
    rd();
    int n0;
    while (line != null) {
      if (line.length() != 0 && line.charAt(0) == '@') {
        Logger.info(line);
        if (line.indexOf("contact}") >= 0 
            || line.indexOf("overlap}") >= 0
            || line.indexOf("H-bonds}") >= 0) {
          if (line.indexOf("@dotlist") == 0) {
            n0 = nDots;
            readDots();
            if (nDots > n0)
              Logger.info("dots: " + (nDots - n0) + "/" + nDots);
            continue;
          } else if (line.indexOf("@vectorlist") == 0) {
            n0 = nPolygons;
            readVectors();
            if (nPolygons > n0)
              Logger.info("lines: " + (nPolygons - n0) + "/" + nPolygons);
            continue;
          }
        }
      }
      rd();
    }
    return true;
  }

  /*
@dotlist {x} color=white master={vdw contact} master={dots}
{ CB  SER   5  A}blue  'S' -16.373,-12.761,26.781
{"}sky  'S' -16.277,-13.003,26.770
   */

  private void readDots() throws Exception {
    int[] color = new int[1];
    while (rd() != null && line.indexOf('@') < 0) {
      int i = getPoint(line, 2, color, true);
      if (i < 0)
        continue;
      nDots++;
      nTriangles = addTriangleCheck(i, i, i, 7, 0, false, color[0]);
    }
  }
  
  /*
@vectorlist {x} color=sky master={small overlap} master={dots}
{ H   VAL   7  A}yellowtint P  'P' -17.239,-10.431,22.660 {"}yellowtint   'P' -17.219,-10.460,22.693
   */
  private void readVectors() throws Exception {
    int[] color = new int[1];
    while (rd() != null && line.indexOf('@') < 0) {
      int ia = getPoint(line, 3, color, true);
      int ib = getPoint(line.substring(line.lastIndexOf('{')), 2, color, false);
      if (ia < 0 || ib < 0)
        continue;
      nPolygons++;
      nTriangles = addTriangleCheck(ia, ib, ib, 7, 0, false, color[0]);
    }
  }

  private String lastAtom = "";
  
  private int getPoint(String line, int i, int[] retColor, boolean checkType) {
    if (findString != null) {
    String atom = line.substring(0, line.indexOf("}") + 1);
    if (atom.length() < 4)
      atom = lastAtom;
    else
      lastAtom = atom;
    if (atom.indexOf(findString) < 0)
      return -1;
    }
    String[] tokens = PT.getTokens(line.substring(line.indexOf("}") + 1));
    float value = assignValueFromGapColorForKin(tokens[0]);
    if (Float.isNaN(value))
      return -1;
    if (checkType && pointType != POINTS_ALL) {      
      switch (tokens[i - 1].charAt(1)) {
      case 'M':
        if (pointType != POINTS_MCMC)
          return -1;
        break;
      case 'S':
        if (pointType != POINTS_SCSC)
          return -1;
        break;
      case 'P':
        if (pointType != POINTS_MCSC)
          return -1;
        break;
      case 'O':
        if (pointType != POINTS_HETS)
          return -1;
        break;
      default:
        return -1;
      }
    }
    retColor[0] = getColor(tokens[0]);
    tokens = PT.getTokens(tokens[i].replace(',', ' '));
    P3 pt = P3.new3(PT.parseFloat(tokens[0]), PT
        .parseFloat(tokens[1]), PT.parseFloat(tokens[2]));
    if (isAnisotropic)
      setVertexAnisotropy(pt);
    return addVertexCopy(pt, value, nVertices++, false);
  }

  private int getColor(String color) {
    if (color.equals("sky"))
      color = "skyblue";
    else if (color.equals("sea"))
      color = "seagreen";
    return CU.getArgbFromString(color); 
  }
  
  /*
  char* assignGapColorForKin(float gap, int class) 
  {
     char *colorValue = "";
     if (class == 4)     { colorValue = "greentint "; } hbond
     else if (gap > 0.35){ colorValue = "blue ";      }
     else if (gap > 0.25){ colorValue = "sky ";       }
     else if (gap > 0.15){ colorValue = "sea ";       }
     else if (gap > 0.0) { colorValue = "green ";     }
     else if (gap >-0.1) { colorValue = "yellowtint ";}
     else if (gap >-0.2) { colorValue = "yellow ";    }
     else if (gap >-0.3) { colorValue = "orange ";    }
     else if (gap >-0.4) { colorValue = "red ";       }
     else                { colorValue = "hotpink ";   }
     return colorValue;
  }
     */

    /**
     * C++ code gives these as " value > x.x ? "xxxxx", etc. 
     * so technically we are off by a smidgeon. But they are the
     * reference numbers, so we will use them inclusively instead.
     * 
     * @param color
     * @return value or NaN if outsided desired range
     */
    private float assignValueFromGapColorForKin(String color) {
       float value = (
         color.equals("greentint") ? 4f
           : color.equals("blue") ? 0.35f
           : color.equals("sky") ? 0.25f
           : color.equals("sea") ? 0.15f
           : color.equals("green") ? 0.0f
           : color.equals("yellowtint") ? -0.1f
           : color.equals("yellow") ? -0.2f
           : color.equals("orange") ? -0.3f
           : color.equals("red") ? -0.4f
           : -0.5f);
       return (value >= vMin && value <= vMax ? value : Float.NaN);
    }

  
  @Override
  protected boolean readPolygons() {
    return true;
  }
}
