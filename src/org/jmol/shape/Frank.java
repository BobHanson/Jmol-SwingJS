/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-05-08 12:29:47 -0500 (Sun, 08 May 2016) $
 * $Revision: 21082 $
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

package org.jmol.shape;

import javajs.awt.Font;

import org.jmol.i18n.GT;
import org.jmol.java.BS;


public class Frank extends Shape {

  // Axes, Bbcage, Frank, Uccage

  final static String defaultFontName = "SansSerif";
  final static String defaultFontStyle = "Plain";
  final static int defaultFontSize = 16;
  public final static int frankMargin = 4;

  public String frankString = "Jmol";
  Font currentMetricsFont3d;
  public Font baseFont3d;
  public int frankWidth;
  public int frankAscent;
  public int frankDescent;
  int x, y, dx, dy;
  private float scaling;
  public Font font3d;



  @Override
  public void initShape() {
    myType = "frank";
    baseFont3d = font3d = vwr.gdata.getFont3DFSS(defaultFontName, defaultFontStyle, defaultFontSize);
    calcMetrics();
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if ("font" == propertyName) {
      Font f = (Font) value;
      if (f.fontSize >= 10) {
        baseFont3d = f;
        scaling = 0;
      }
    }
    // no other aspects
    return;
  }
  
  @Override
  public boolean wasClicked(int x, int y) {
    int width = vwr.getScreenWidth();
    int height = vwr.getScreenHeight();
    return (width > 0 && height > 0 
        && x > width - frankWidth - frankMargin
        && y > height - frankAscent - frankMargin);
  }

  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    if (!vwr.getShowFrank() || !wasClicked(x, y) || !vwr.menuEnabled())
      return false;
    if (vwr.gdata.antialiasEnabled && !vwr.isSingleThreaded) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    vwr.hoverOnPt(x, y, GT._("Click for menu..."), null, null);
    return true;
  }
  
  void calcMetrics() {
    if (vwr.isJS)
      frankString = "JSmol";
    else if (vwr.isSignedApplet)
      frankString = "Jmol_S";
    if (font3d == currentMetricsFont3d) 
      return;
    currentMetricsFont3d = font3d;
    frankWidth = font3d.stringWidth(frankString);
    frankDescent = font3d.getDescent();
    frankAscent = font3d.getAscent();
  }

  public void getFont(float imageFontScaling) {
    if (imageFontScaling != scaling) {
      scaling = imageFontScaling;
      font3d = vwr.gdata.getFont3DScaled(baseFont3d, imageFontScaling);
      calcMetrics();
    }
  }
  
  @Override
  public String getShapeState() {
    // see StateCreator
    return null;//vwr.getFontState(myType, baseFont3d);
  }
  
}
