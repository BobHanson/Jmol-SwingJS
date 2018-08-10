/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-12-23 02:23:37 -0600 (Wed, 23 Dec 2015) $
 * $Revision: 20903 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapebio;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;

import org.jmol.atomdata.RadiusData;
import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.modelsetbio.BioModel;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.Monomer;
import org.jmol.shape.Shape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.viewer.JC;
/****************************************************************
 * Mps stands for Model-Polymer-Shape
 * 
 * When a Cartoon is instantiated with a call to setSize(),
 * it creates an MpsShape for each BioPolymer in the model set.
 * 
 * It is these shapes that are the real "shapes". Unlike other
 * shapes, which are indexed by atom and throughout the entire
 * model set, these shapes are indexed by residue and are 
 * restricted to a given BioPolymer within a given Model.
 * 
 * Model 
 * 
 ****************************************************************/
public abstract class BioShapeCollection extends Shape {

  Atom[] atoms;
  
  short madOn = -2;
  short madHelixSheet = 3000;
  short madTurnRandom = 800;
  short madDnaRna = 5000;
  boolean isActive = false;
  
  public BioShape[] bioShapes;
  
  @Override
  public final void initModelSet() {
    isBioShape = true;
    atoms = ms.at;
    initialize();
  }

  @Override
  public void initShape() {
    // nothing to do
  }

  @Override
  public int getSizeG(Group group) {
    Monomer m = (Monomer) group;
    int groupIndex = m.groupIndex;
    int leadAtomIndex = m.getLeadAtom().i;
    for (int i = bioShapes.length; --i >= 0;) {
      BioShape bioShape = bioShapes[i];
      for (int j = 0; j < bioShape.monomerCount; j++) {
        if (bioShape.monomers[j].groupIndex == groupIndex 
          && bioShape.monomers[j].getLeadAtom().i == leadAtomIndex)
            return bioShape.mads[j];
      }
    }
    return 0;
  }
  
  @Override
  public void replaceGroup(Group g0, Group g1) {
    //BioShapeCollection only; after MUTATE
    for (int i = bioShapes.length; --i >= 0;) {
      BioShape bioShape = bioShapes[i];
      for (int j = 0; j < bioShape.monomerCount; j++)
        if (bioShape.monomers[j] == g0) {
          bioShape.monomers[j] = (Monomer) g1;
          break;
        }
    }
  }


  @Override
  public void setShapeSizeRD(int size, RadiusData rd, BS bsSelected) {
    short mad = (short) size;
    initialize();
    for (int i = bioShapes.length; --i >= 0;) {
      BioShape bioShape = bioShapes[i];
      if (bioShape.monomerCount > 0)
        bioShape.setMad(mad, bsSelected, (rd == null ? null : rd.values));
    }
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {
    setPropBSC(propertyName, value, bsSelected);
  }

  protected void setPropBSC(String propertyName, Object value, BS bsSelected) {

    if (propertyName == "refreshTrajectories") {
      int modelIndex = ((Integer) ((Object[]) value)[0]).intValue();
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape b = bioShapes[i];
        if (b.modelIndex == modelIndex)
          b.falsifyMesh();
      }
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      atoms = (Atom[]) ((Object[]) value)[1];
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape b = bioShapes[i];
        if (b.modelIndex > modelIndex) {
          b.modelIndex--;
          b.leadAtomIndices = b.bioPolymer.getLeadAtomIndices();
        } else if (b.modelIndex == modelIndex) {
          bioShapes = (BioShape[]) AU.deleteElements(bioShapes, i, 1);
        }
      }
      return;
    }

    initialize();
    if ("color" == propertyName) {
      byte pid = PAL.pidOf(value);
      short colix = C.getColixO(value);
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape bioShape = bioShapes[i];
        if (bioShape.monomerCount > 0)
          bioShape.setColixBS(colix, pid, bsSelected);
      }
      return;
    }
    if ("params" == propertyName) {
      int n = bsSelected.length();
      int[] atomMap = new int[n];
      for (int pt = 0, i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected.nextSetBit(i + 1), pt++)
        atomMap[i] = pt;
      for (int i = bioShapes.length; --i >= 0;)
        bioShapes[i].setParams((Object[]) value, atomMap, bsSelected);
      return;
    }
    
    if ("colorPhase" == propertyName) {
      // cartoons and ribbons only
      Object[] twoColors = (Object[]) value;
      short colixBack = C.getColixO(twoColors[0]);
      short colix = C.getColixO(twoColors[1]);
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape bioShape = bioShapes[i];
        if (bioShape.monomerCount > 0) {
          bioShape.setColixBS(colix, PAL.PALETTE_VOLATILE, bsSelected);
          bioShape.setColixBack(colixBack, bsSelected);
        }
      }
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent".equals(value));
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape bioShape = bioShapes[i];
        if (bioShape.monomerCount > 0)
          bioShape.setTranslucent(isTranslucent, bsSelected, translucentLevel);
      }
      return;
    }

    setPropS(propertyName, value, bsSelected);
  }

  @Override
  public String getShapeState() {
    // leaving this here because it is so specialized
    Map<String, BS> temp = new Hashtable<String, BS>();
    Map<String, BS> temp2 = new Hashtable<String, BS>();
    String type = JC.shapeClassBases[shapeID];
    for (int iShape = bioShapes.length; --iShape >= 0;)
      bioShapes[iShape].getBioShapeState(type, translucentAllowed, temp, temp2);
    String s = "\n"
        + vwr.getCommands(temp, temp2,
            shapeID == JC.SHAPE_BACKBONE ? "Backbone" : "select");
    return s;
  }


  void initialize() {
    int modelCount = ms.mc;
    Model[] models = ms.am;
    int n = ms.getBioPolymerCountInModel(-1);
    BioShape[] shapes = new BioShape[n--];
    for (int i = modelCount; --i >= 0;)
      for (int j = ms.getBioPolymerCountInModel(i); --j >= 0; n--) {
        BioPolymer bp = ((BioModel) models[i]).bioPolymers[j];
        shapes[n] = (bioShapes == null || bioShapes.length <= n
            || bioShapes[n] == null || bioShapes[n].bioPolymer != bp ? new BioShape(
            this, i, bp)
            : bioShapes[n]);
      }
    bioShapes = shapes;
  }

  @Override
  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest, BS bsNot) {
    for (int i = bioShapes.length; --i >= 0; )
      bioShapes[i].findNearestAtomIndex(xMouse, yMouse, closest, bsNot);
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    if (bioShapes == null)
      return;
    bsModels = BSUtil.copy(bsModels);
    if (ms.trajectory != null)
      ms.trajectory.setBaseModels(bsModels);
    for (int i = bioShapes.length; --i >= 0;) {
      BioShape b = bioShapes[i];
      b.modelVisibilityFlags = (bsModels.get(b.modelIndex) ? vf : 0);
    }
  }

  @Override
  public void setAtomClickability() {
    if (bioShapes == null)
      return;
    for (int i = bioShapes.length; --i >= 0; )
      bioShapes[i].setAtomClickability();
  }

  int getMpsShapeCount() {
    return bioShapes.length;
  }

  public BioShape getBioShape(int i) {
    return bioShapes[i];
  }  
}
