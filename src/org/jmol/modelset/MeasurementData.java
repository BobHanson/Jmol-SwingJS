/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-01-11 09:57:12 -0600 (Mon, 11 Jan 2010) $
 * $Revision: 12093 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.modelset;

import java.util.Map;

import javajs.util.Lst;


import org.jmol.api.JmolMeasurementClient;
import org.jmol.atomdata.RadiusData;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Point3fi;
import org.jmol.viewer.Viewer;

public class MeasurementData implements JmolMeasurementClient {

  /*
   * a class used to pass measurement parameters to Measures and
   * to generate a list of measurements from ScriptMathProcessor
   * 
   */
  
  private JmolMeasurementClient client;
  private Lst<String> measurementStrings;
  private Lst<Float> measurements;

  public Lst<Object> points;
  public boolean mustBeConnected;
  public boolean mustNotBeConnected;
  public TickInfo tickInfo;
  public int tokAction = T.define;
  public RadiusData radiusData;
  public String strFormat;
  public String note;
  public boolean isAll;
  public short colix;
  public Boolean intramolecular;
  public int mad;
  public String thisID;
  public Text text;
  
  private Atom[] atoms;
  private String units;
  private float[] minArray;
  private ModelSet ms;
  
  
  
  public MeasurementData() {
    // by reflection
  }
  public MeasurementData init(String id, Viewer vwr, Lst<Object> points) {
    this.vwr = vwr;
    this.points = points;
    thisID = id;
    return this;
  }
  
  public MeasurementData setModelSet(ModelSet m) {
    ms = m;
    return this;
  }
  
  /*
   * the general constructor. tokAction is not used here
   */
  public MeasurementData set(int tokAction, Map<String, Integer> htMin, 
                             RadiusData radiusData, String strFormat, String units,
                 TickInfo tickInfo,
                 boolean mustBeConnected, boolean mustNotBeConnected,
                 Boolean intramolecular, boolean isAll, 
                 int mad, short colix, Text text) {
    this.ms = vwr.ms;
    this.tokAction = tokAction;
    if (points.size() >= 2 && points.get(0) instanceof BS && points.get(1) instanceof BS) {
      justOneModel = BSUtil.haveCommon(
          vwr.ms.getModelBS((BS) points.get(0), false),
          vwr.ms.getModelBS((BS) points.get(1), false)); 
    }
    //this.rangeMinMax = rangeMinMax;
    this.htMin = htMin;
    this.radiusData = radiusData;
    this.strFormat = strFormat;
    this.units = units;
    this.tickInfo = tickInfo;
    this.mustBeConnected = mustBeConnected;
    this.mustNotBeConnected = mustNotBeConnected;
    this.intramolecular = intramolecular;    
    this.isAll = isAll;
    this.mad = mad;
    this.colix = colix;
    this.text = text;
    return this;
  }
  
  /**
   * if this is the client, then this method is 
   * called by MeasurementData when a measurement is ready
   * 
   * @param m 
   * 
   */
  @Override
  public void processNextMeasure(Measurement m) {
    float value = m.getMeasurement(null);
    // here's where we check vdw
    if (htMin != null && !m.isMin(htMin) || radiusData != null && !m.isInRange(radiusData, value))
      return;
    if (measurementStrings == null && measurements == null) {
      float f = minArray[iFirstAtom];
      m.value = value;
      value = m.fixValue(units, false);
      minArray[iFirstAtom] = (1/f == Float.NEGATIVE_INFINITY ? value : Math.min(f, value));
      return;
    }
    
    if (measurementStrings != null)
      measurementStrings.addLast(m.getStringUsing(vwr, strFormat, units));
    else
      measurements.addLast(Float.valueOf(m.getMeasurement(null)));   
  }

  /**
   * if this is the client, then this method
   * can be called to get the result vector, either as a string
   * or as an array.
   * 
   * @param asArray 
   * @param asMinArray array of minimum of a given atom type 
   * @return Vector of formatted Strings or array of minimum-distance values
   * 
   */
  public Object getMeasurements(boolean asArray, boolean asMinArray) {
    if (asMinArray) {
      minArray = new float[((BS) points.get(0)).cardinality()];
      for (int i = 0; i < minArray.length; i++)
        minArray[i] = -0f;
      define(null, ms);
      return minArray;      
    }
    if (asArray) {
      measurements = new Lst<Float>();
      define(null, ms);
      return measurements;
    }
    measurementStrings = new  Lst<String>();
    define(null, ms);
    return measurementStrings;
  }
  
  private Viewer vwr;
  private int iFirstAtom;
  private boolean justOneModel = true;
  public Map<String, Integer> htMin;
  
  /**
   * called by the client to generate a set of measurements
   * 
   * @param client    or null to specify this to be our own client
   * @param modelSet
   */
  public void define(JmolMeasurementClient client, ModelSet modelSet) {
    this.client = (client == null ? this : client);
    atoms = modelSet.at;
    /*
     * sets up measures based on an array of atom selection expressions -RMH 3/06
     * 
     *(1) run through first expression, choosing model
     *(2) for each item of next bs, iterate over next bitset, etc.
     *(3) for each last bitset, trigger toggle(int[])
     *
     *simple!
     *
     */
    int nPoints = points.size();
    if (nPoints < 2)
      return;
    int modelIndex = -1;
    Point3fi[] pts = new Point3fi[4];
    int[] indices = new int[5];
    Measurement m = new Measurement().setPoints(modelSet, indices, pts, null);
    m.setCount(nPoints);
    int ptLastAtom = -1;
    for (int i = 0; i < nPoints; i++) {
      Object obj = points.get(i);
      if (obj instanceof BS) {
        BS bs = (BS) obj;
        int nAtoms = bs.cardinality(); 
        if (nAtoms == 0)
          return;
        if (nAtoms > 1)
          modelIndex = 0;
        ptLastAtom = i;
        if (i == 0)
          iFirstAtom = 0;
        indices[i + 1] = bs.nextSetBit(0);
      } else {
        pts[i] = (Point3fi)obj;
        indices[i + 1] = -2 - i; 
      }
    }
    nextMeasure(0, ptLastAtom, m, modelIndex);
  }

  /**
   * iterator for measurements
   * 
   * @param thispt
   * @param ptLastAtom
   * @param m
   * @param thisModel
   */
  private void nextMeasure(int thispt, int ptLastAtom, Measurement m, int thisModel ) {
    if (thispt > ptLastAtom) {
      if (m.isValid() 
          && (!mustBeConnected || m.isConnected(atoms, thispt))
          && (!mustNotBeConnected || !m.isConnected(atoms, thispt))
          && (intramolecular == null || m.isIntramolecular(atoms, thispt) == intramolecular.booleanValue())
          )
        client.processNextMeasure(m);
      return;
    }
    BS bs = (BS) points.get(thispt);
    int[] indices = m.countPlusIndices;
    int thisAtomIndex = (thispt == 0 ? Integer.MAX_VALUE : indices[thispt]);
    if (thisAtomIndex < 0) {
      nextMeasure(thispt + 1, ptLastAtom, m, thisModel);
      return;
    }
    boolean haveNext = false;
    for (int i = bs.nextSetBit(0), pt = 0; i >= 0; i = bs.nextSetBit(i + 1), pt++) {
      if (i == thisAtomIndex)
        continue;
      int modelIndex = atoms[i].mi;
      if (thisModel >= 0 && justOneModel) {
        if (thispt == 0)
          thisModel = modelIndex;
        else if (thisModel != modelIndex)
          continue;
      }
      indices[thispt + 1] = i;
      if (thispt == 0)
        iFirstAtom = pt;
      haveNext = true;
      nextMeasure(thispt + 1, ptLastAtom, m, thisModel);
    }
    if (!haveNext)
      nextMeasure(thispt + 1, ptLastAtom, m, thisModel);
  }
    
}

