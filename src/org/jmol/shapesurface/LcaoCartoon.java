/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:23:28 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5305 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net,jmol-developers@lists.sourceforge.net
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapesurface;


import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.util.C;
import org.jmol.util.Escape;

public class LcaoCartoon extends Isosurface {

  // these are globals, stored here and only passed on when the they are needed. 

  @Override
  public void initShape() {
    super.initShape();
    myType = "lcaoCartoon";
    allowMesh = false;
  }

  //transient
  private String thisType;
  private int myColorPt;
  private String lcaoID;
  private BS thisSet;
  private boolean isMolecular;
  private V3 rotationAxis;

  //persistent
  private Float lcaoScale;
  private boolean lcaoTranslucent;
  private float lcaoTranslucentLevel;
  private Integer lcaoColorPos;
  private Integer lcaoColorNeg;
  boolean isLonePair;
  boolean isRadical;
  private Object cappingObject;
  private Object slabbingObject;
  private String fullCommand;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    // in the case of molecular orbitals, we just cache the information and
    // then send it all at once. 

    boolean setInfo = false;

    if ("init" == propertyName) {
      myColorPt = 0;
      lcaoID = null;
      thisSet = bs;
      isMolecular = isLonePair = isRadical = false;
      thisType = null;
      rotationAxis = null;
      fullCommand = (String) value;
      // overide bitset selection
      setPropI("init", null, null);
      return;
    }

    //setup

    if ("lcaoID" == propertyName) {
      lcaoID = (String) value;
      return;
    }

    if ("thisID" == propertyName) {
      lcaoID = (String) value;
      // pass on?
    }


    if ("selectType" == propertyName) {
      thisType = (String) value;
      return;
    }

    if ("rotationAxis" == propertyName) {
      rotationAxis = (V3) value;
      return;
    }

    if ("scale" == propertyName) {
      lcaoScale = (Float) value;
      //pass through
    }

    if ("colorRGB" == propertyName) {
      lcaoColorPos = (Integer) value;
      if (myColorPt++ == 0)
        lcaoColorNeg = lcaoColorPos;
      //pass through
    }

    if ("select" == propertyName) {
      thisSet = (BS) value;
      //pass through
    }

    if ("translucentLevel" == propertyName) {
      lcaoTranslucentLevel = ((Float) value).floatValue();
      //pass through
    }

    if ("settranslucency" == propertyName) {
      lcaoTranslucent = (((String) value).equals("translucent"));
      return;
    }

    if ("translucency" == propertyName) {
      lcaoTranslucent = (((String) value).equals("translucent"));
      if (lcaoID == null)
        return;
    }

    //final operations
    if ("molecular" == propertyName) {
      isMolecular = true;
      if (value == null)
        return;
      propertyName = "create";
      //continue
    }

    if ("create" == propertyName) {
      myColorPt = 0;
      thisType = (String) value;
      createLcaoCartoon();
      return;
    }

    if ("lonePair" == propertyName) {
      isLonePair = true;
      return;
    }

    if ("lp" == propertyName) {
      isLonePair = setInfo = true;
    }

    if ("radical" == propertyName) {
      isRadical = true;
      return;
    }

    if ("rad" == propertyName) {
      isRadical = setInfo = true;
    }

    if ("delete" == propertyName) {
      deleteLcaoCartoon();
      return;
    }
    
    if ("on" == propertyName) {
      setLcaoOn(true);
      return;
    }
    
    if ("off" == propertyName) {
      setLcaoOn(false);
      return;
    }

    if ("slab" == propertyName) {
      slabbingObject = value;
    }
    if ("cap" == propertyName) {
      cappingObject = value;
    }
    
    //from the state:
    if ("lobe" == propertyName || "sphere" == propertyName) {
      getCapSlabInfo(fullCommand);
    }

    setPropI(propertyName, value, bs);
    
    //from the state:
    if (setInfo || "lobe" == propertyName || "sphere" == propertyName) {
      setScriptInfo(null);
    }
  }

  private void setLcaoOn(boolean TF) {
    if (PT.isWild(lcaoID)) {
      Lst<Mesh> list = getMeshList(lcaoID, false);
      for (int i = list.size(); --i >= 0;)
        list.get(i).visible = TF;
      return;
    }
    // older method

    int ac = vwr.ms.ac;
    for (int i = ac; --i >= 0;)
      if (lcaoID != null || thisSet.get(i))
        setLcaoOn(i, TF);
  }

  private void setLcaoOn(int iAtom, boolean TF) {
    String id = getID(lcaoID, iAtom);
    for (int i = meshCount; --i >= 0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        meshes[i].visible = TF;
  }

  private void deleteLcaoCartoon() {
    if (PT.isWild(lcaoID)) {
      deleteMeshKey(lcaoID);
      return;
    }
    // older method does not use * but still deletes multiple lobes
    int ac = vwr.ms.ac;
    for (int i = ac; --i >= 0;)
      if (lcaoID != null || thisSet.get(i))
        deleteLcaoCartoon(i);
  }

  private void deleteLcaoCartoon(int iAtom) {
    String id = getID(lcaoID, iAtom);
    for (int i = meshCount; --i >= 0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        deleteMeshI(i);
  }

  private void createLcaoCartoon() {
    isMolecular = (isMolecular && (thisType.indexOf("px") >= 0
        || thisType.indexOf("py") >= 0 || thisType.indexOf("pz") >= 0));
    String lcaoID0 = lcaoID;
    for (int i = thisSet.nextSetBit(0); i >= 0; i = thisSet.nextSetBit(i + 1)) {
      createLcaoCartoon(i);
      lcaoID = lcaoID0;
    }
  }

  private void createLcaoCartoon(int iAtom) {
    String id = getID(lcaoID, iAtom);
    boolean isCpk = (thisType.equals("cpk"));
    for (int i = meshCount; --i >= 0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        deleteMeshI(i);
    setPropI("init", null, null);
    translucentLevel = lcaoTranslucentLevel;
    setPropI("thisID", id, null);
    //System.out.println("lcaocartoon: " + id);
    if (lcaoScale != null)
      setPropI("scale", lcaoScale, null);
    if (isCpk) {
      setPropI("colorRGB", Integer.valueOf(vwr.gdata.getColorArgbOrGray(ms.at[iAtom].colixAtom)), null);
    } else if (lcaoColorNeg != null) {
      setPropI("colorRGB", lcaoColorNeg, null);
      setPropI("colorRGB", lcaoColorPos, null);
    }
    if (slabbingObject != null)
      setPropI("slab", slabbingObject, null);
    if (cappingObject != null)
      setPropI("cap", cappingObject, null);
    setPropI("lcaoType", thisType, null);
    setPropI("atomIndex", Integer.valueOf(iAtom), null);
    V3[] axes = { new V3(), new V3(),
        V3.newV(ms.at[iAtom]), new V3() };
    if (rotationAxis != null)
      axes[3].setT(rotationAxis);
    if (isMolecular) {
      if (thisType.indexOf("px") >= 0) {
        axes[0].set(0, -1, 0);
        axes[1].set(1, 0, 0);
      } else if (thisType.indexOf("py") >= 0) {
        axes[0].set(-1, 0, 0);
        axes[1].set(0, 0, 1);
      } else if (thisType.indexOf("pz") >= 0) {
        axes[0].set(0, 0, 1);
        axes[1].set(1, 0, 0);
      }
      if (thisType.indexOf("-") == 0)
        axes[0].scale(-1);
    }
    if (isMolecular || isCpk
        || thisType.equalsIgnoreCase("s")
        || vwr.getHybridizationAndAxes(iAtom, axes[0], axes[1], thisType) != null) {
      setPropI((isRadical ? "radical" : isLonePair ? "lonePair" : "lcaoCartoon"), axes, null);
    }
    if (isCpk) {
      short colix = vwr.ms.at[iAtom].colixAtom;
      if (C.isColixTranslucent(colix)) {
        setPropI("translucentLevel", Float.valueOf(C.getColixTranslucencyLevel(colix)), null);
        setPropI("translucency", "translucent", null);
      }
    } else if (lcaoTranslucent)
      for (int i = meshCount; --i >= 0;)
        if (meshes[i].thisID.indexOf(id) == 0)
          meshes[i].setTranslucent(true, translucentLevel);
  }

  private String getID(String id, int i) {
    // remove "-" from "-px" "-py" "-pz" because we never want to have
    // both "pz" and "-pz" on the same atom
    // but we can have "-sp3a" and "sp3a"
    return (id != null ? id : (isLonePair || isRadical ? "lp_" : "lcao_") + (i + 1) + "_")
        + (thisType == null ? "" : PT.rep(thisType, "-",
            (thisType.indexOf("-p") == 0 ? "" : "_")));
  }

  @Override
  public String getShapeState() {
    SB sb = new SB();
    if (lcaoScale != null)
      appendCmd(sb, "lcaoCartoon scale " + lcaoScale.floatValue());
    if (lcaoColorNeg != null)
      appendCmd(sb, "lcaoCartoon color "
          + Escape.escapeColor(lcaoColorNeg.intValue()) + " "
          + Escape.escapeColor(lcaoColorPos.intValue()));
    if (lcaoTranslucent)
      appendCmd(sb, "lcaoCartoon translucent " + translucentLevel);
    for (int i = meshCount; --i >= 0;)
      if (!meshes[i].visible)
        appendCmd(sb, "lcaoCartoon ID " + meshes[i].thisID + " off");
    return super.getShapeState() + sb.toString();
  }
  
  @Override
  public void merge(MeshCollection shape) {
    LcaoCartoon lc = (LcaoCartoon) shape;
    lcaoScale = lc.lcaoScale;
    lcaoColorNeg = lc.lcaoColorNeg;
    lcaoTranslucent = lc.lcaoTranslucent;
    lcaoTranslucentLevel = lc.lcaoTranslucentLevel;
    super.merge(shape);
  }
  

}
