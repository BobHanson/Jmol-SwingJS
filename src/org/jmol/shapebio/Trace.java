/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-16 01:43:57 -0600 (Thu, 16 Nov 2006) $
 * $Revision: 6225 $
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

package org.jmol.shapebio;

import org.jmol.atomdata.RadiusData;
import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.script.T;

public class Trace extends BioShapeCollection {

  @Override
  public void initShape() {
    madOn = 600;
    madHelixSheet = 1500;
    madTurnRandom = 500;
    madDnaRna = 1500;
  }

  private static final int PUTTY_NormalizedNonlinear = 0;
  private static final int PUTTY_RelativeNonlinear   = 1;
  private static final int PUTTY_ScaledNonlinear     = 2;
  private static final int PUTTY_AbsoluteNonlinear   = 3;
  private static final int PUTTY_NormalizedLinear    = 4;
  private static final int PUTTY_RelativeLinear      = 5;
  private static final int PUTTY_ScaledLinear        = 6;
  private static final int PUTTY_AbsoluteLinear      = 7;
  private static final int PUTTY_ImpliedRMS          = 8;
 
  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {
    if (propertyName == "putty") {
      setPutty((float[]) value, bsSelected);
      return;
    }
    setPropBSC(propertyName, value, bsSelected);
  }

  /**
   * PyMOL-based "putty"
   * 
   * @param info [quality,radius,range,scale_min,scale_max,scale_power,transform]
   * @param bsAtoms
   */
  private void setPutty(float[] info, BS bsAtoms) {
    int n = bsAtoms.cardinality();
    if (n == 0)
      return;
    float[] data = new float[bsAtoms.length()];
    double sum = 0.0, sumsq = 0.0;
    float min = Float.MAX_VALUE;
    float max = 0;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      float value = atoms[i].atomPropertyFloat(null, T.temperature, null);
      sum += value;
      sumsq += (value * value);
      if (value < min)
        min = value;
      if (value > max)
        max = value;
    }
    float mean = (float) (sum / n);
    float stdev = (float) Math.sqrt((sumsq - (sum * sum / n)) / n);

    float rad = info[1];
    float range = info[2];
    float scale_min = info[3];
    float scale_max = info[4];
    float power = info[5];

    int transform = (int) info[6];
    float data_range = max - min;
    boolean nonlinear = false;
    switch (transform) {
    case PUTTY_NormalizedNonlinear:
    case PUTTY_RelativeNonlinear:
    case PUTTY_ScaledNonlinear:
    case PUTTY_AbsoluteNonlinear:
      nonlinear = true;
      break;
    }
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      float scale = atoms[i].atomPropertyFloat(null, T.temperature, null);
      switch (transform) {
      case PUTTY_AbsoluteNonlinear:
      case PUTTY_AbsoluteLinear:
      default:
        break;
      case PUTTY_NormalizedNonlinear:
      case PUTTY_NormalizedLinear:
        /* normalized by Z-score, with the range affecting the distribution width */
        scale = 1 + (scale - mean) / range / stdev;
        break;
      case PUTTY_RelativeNonlinear:
      case PUTTY_RelativeLinear:
        scale = (scale - min) / data_range / range;
        break;
      case PUTTY_ScaledNonlinear:
      case PUTTY_ScaledLinear:
        scale /= range;
        break;
      case PUTTY_ImpliedRMS:
        if (scale < 0.0F)
          scale = 0.0F;
        scale = (float) (Math.sqrt(scale / 8.0) / Math.PI);
        break;
      }
      if (scale < 0.0F)
        scale = 0.0F;
      if (nonlinear)
        scale = (float) Math.pow(scale, power);
      if ((scale < scale_min) && (scale_min >= 0.0))
        scale = scale_min;
      if ((scale > scale_max) && (scale_max >= 0.0))
        scale = scale_max;
      data[i] = scale * rad;
    }
    RadiusData rd = new RadiusData(data, 0, RadiusData.EnumType.ABSOLUTE,
        VDW.AUTO);
    setShapeSizeRD(0, rd, bsAtoms);
  }

}