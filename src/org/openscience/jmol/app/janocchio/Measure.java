/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

import java.text.DecimalFormat;


public class Measure implements Comparable<Measure> {
  private String expValue = "";
  private double calcValue;
  protected double diff;
  private int type;
  
  final static int TYPE_DISTANCE = 0; //00
  final static int TYPE_J        = 1; //0
  final static int TYPE_NOE      = 2; //0000
  final static int TYPE_EXP_NOE  = 0; //00

  static final DecimalFormat[] df = new DecimalFormat[] { 
    new DecimalFormat("0.00"), new DecimalFormat("0.0"), new DecimalFormat("0.0000") 
  };


  Measure(String expValue, double calcValue, int type) {
    this.expValue = expValue;
    this.calcValue = calcValue;
    this.type = type;
  }

  public String getExpValue() {
    return expValue;
  }

  public double getCalcValue() {
    return calcValue;
  }

  public double getDiff() {
    return diff;
  }

  // This method allows the values to be sorted in the data tables
  @Override
  public int compareTo(Measure m) {
    double dcomp = calcValue - m.getCalcValue();
    if (dcomp > 0) {
      return 1;
    } else if (dcomp < 0) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    return "" + calcValue;
  }

  public String round() {
    return df[type].format(calcValue);
  }

  public static String formatDistance(double d) {
    return df[TYPE_DISTANCE].format(d);
  }

  public static String formatJ(double d) {
    return df[TYPE_J].format(d);
  }

  public static String formatNOE(double d) {
    return df[TYPE_NOE].format(d);
  }

  public static String formatExpNOE(double d) {
    return df[TYPE_EXP_NOE].format(d);
  }

}
