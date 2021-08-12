/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-18 01:25:52 -0500 (Wed, 18 Apr 2007) $
 * $Revision: 7435 $
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

package org.jmol.shape;

import org.jmol.viewer.StateManager;
import javajs.util.BS;

import org.jmol.jvxl.data.JvxlData;
import org.jmol.script.SV;
import org.jmol.script.T;


import java.util.Hashtable;

import java.util.Map;


import org.jmol.util.C;
import org.jmol.util.Escape;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.M4;
import javajs.util.T3;

import org.jmol.util.Logger;
import javajs.util.P3;

public abstract class MeshCollection extends Shape {

  // CGO, Draw, Isosurface(Contact LcaoCartoon MolecularOrbital Pmesh)
    
  protected JvxlData jvxlData;
  public int meshCount;
  public Mesh[] meshes = new Mesh[4];
  public Mesh currentMesh;
  public boolean isFixed;  
  public int nUnnamed;
  public short colix;
  public boolean explicitID;
  protected String previousMeshID;
  protected Mesh linkedMesh;
  protected int modelIndex;

  protected float displayWithinDistance2;
  protected boolean isDisplayWithinNot;
  protected Lst<P3> displayWithinPoints;
  protected BS bsDisplay;

  public String[] title;
  
  protected Mesh pickedMesh;
  protected int pickedModel;
  protected int pickedVertex;
  protected T3 pickedPt;
  
  protected int[] connections;

  private Mesh setMesh(String thisID) {
    linkedMesh = null;
    if (thisID == null || PT.isWild(thisID)) {
      if (thisID != null)
        previousMeshID = thisID;
      currentMesh = null;
      return null;
    }
    currentMesh = getMesh(thisID);
    if (currentMesh == null) {
      allocMesh(thisID, null);
    } else if (thisID.equals(MeshCollection.PREVIOUS_MESH_ID)) {
      linkedMesh = currentMesh.linkedMesh;
    }
    if (currentMesh.thisID == null) {
      if (nUnnamed == 0 || getMesh(myType + nUnnamed) != null)
        nUnnamed++;
      currentMesh.thisID = myType + nUnnamed;
      if (htObjects != null)
        htObjects.put(currentMesh.thisID.toUpperCase(), currentMesh);
    }
    previousMeshID = currentMesh.thisID;
    return currentMesh;
  }

  protected Map<String, Mesh> htObjects;
  protected int color;
  public final static String PREVIOUS_MESH_ID = "+PREVIOUS_MESH+";
  
  public void allocMesh(String thisID, Mesh m) {
    // this particular version is only run from privately;
    // isosurface and draw both have overriding methods
    int index = meshCount++;
    meshes = (Mesh[])AU.ensureLength(meshes, meshCount * 2);
    currentMesh = meshes[index] = (m == null ? new Mesh().mesh1(vwr, thisID, colix, index) : m);
    currentMesh.color = color;
    currentMesh.index = index;
    if (thisID != null && htObjects != null)
      htObjects.put(thisID.toUpperCase(), currentMesh);
    previousMeshID = null;
  }

  /**
   * called by ParallelProcessor at completion
   * 
   * @param mc 
   * 
   */
  public void merge(MeshCollection mc) {
    for (int i = 0; i < mc.meshCount; i++) {
      if (mc.meshes[i] != null) {
        Mesh m = mc.meshes[i];
        Mesh m0 = getMesh(m.thisID);
        if (m0 == null) {
          allocMesh(m.thisID, m);
        } else {
          meshes[m0.index] = m;
          m.index = m0.index;
        }
      }      
    }
    previousMeshID = null;
    currentMesh = null;
  }

  @Override
  public void initShape() {
    colix = C.ORANGE;
    color = 0xFFFFFFFF;
  }
  
 @SuppressWarnings("unchecked")
 protected void setPropMC(String propertyName, Object value, BS bs) {

//   if (propertyName == "setXml") {
//     if (currentMesh != null)
//       currentMesh.xmlProperties = xmlProperties;
//   }
   
    if ("init" == propertyName) {
      title = null;
      return;
    }

    if ("link" == propertyName) {
      if (meshCount >= 2 && currentMesh != null)
        currentMesh.linkedMesh = meshes[meshCount - 2];
      return;
    }

    if ("lattice" == propertyName) {
      if (currentMesh != null)
        currentMesh.lattice = (P3) value;
      return;
    }

    if ("symops" == propertyName) {
      if (currentMesh != null) {
        currentMesh.symops = (M4[]) value;
        if (currentMesh.symops == null)
          return;
        int n = currentMesh.symops.length;
        currentMesh.symopColixes = new short[n];
        for (int i = n; --i >= 0;)
        currentMesh.symopColixes[i] = C.getColix(vwr.cm.ce.getArgbMinMax(i + 1, 1, n));        
      }
      return;
    }

    if ("variables" == propertyName) {
      if (currentMesh != null && currentMesh.scriptCommand != null && !currentMesh.scriptCommand.startsWith("{"))
        currentMesh.scriptCommand = "{\n" 
          + StateManager.getVariableList((Map<String, SV>) value, 0, false, false) + "\n" + currentMesh.scriptCommand;
      return;
    }

    if ("thisID" == propertyName) {
      String id = (String) value;
      setMesh(id);
      checkExplicit(id);
      return;
    }

    if ("title" == propertyName) {
      if (value == null) {
        title = null;
      } else if (value instanceof String){
        int nLine = 1;
        String lines = (String) value;
        for (int i = lines.length(); --i >= 0;)
          if (lines.charAt(i) == '|')
            nLine++;
        title = new String[nLine];
        nLine = 0;
        int i0 = -1;
        for (int i = 0; i < lines.length(); i++)
          if (lines.charAt(i) == '|') {
            title[nLine++] = lines.substring(i0 + 1, i);
            i0 = i;
          }
        title[nLine] = lines.substring(i0 + 1);
      } else {
        title = (String[]) value;
      }
      return;
    }

    if ("delete" == propertyName) {
      deleteMesh();
      return;
    }

    if ("reset" == propertyName) {
      String thisID = (String) value;
      if (setMesh(thisID) == null)
        return;
//      deleteMesh();
      setMesh(thisID);
      return;
    }

    if ("color" == propertyName) {
      if (value == null)
        return;
      colix = C.getColixO(value);
      color = ((Integer) value).intValue();
      if (currentMesh != null) {
        currentMesh.color = color;
      }
      setTokenProperty(T.color, false, false);
      return;
    }

    if ("translucency" == propertyName) {
      setTokenProperty(T.translucent, (((String) value).equals("translucent")), false);
      return;
    }

    if ("hidden" == propertyName) {
      value = Integer.valueOf(((Boolean)value).booleanValue() ? T.off: T.on);
      propertyName = "token";
      //continue
    }

    if ("token" == propertyName) {
      int tok = ((Integer) value).intValue();
      int tok2 = 0;
      boolean test = true;
      switch (tok) {
      case T.display:
      case T.on:
      case T.frontlit:
      case T.backlit:
      case T.fullylit:
      case T.dots:
      case T.fill:
      case T.backshell:
      case T.triangles:
      case T.frontonly:
        break;
      case T.off:
        test = false;
        tok = T.on;
        break;
      case T.contourlines:
        tok2 = T.mesh;
        break;
      case T.nocontourlines:
        test = false;
        // TODO leave this as is for now; probably correct...
        tok = T.contourlines;//(allowContourLines ? Token.contourlines : Token.mesh);
        tok2 = T.mesh;
        break;
      case T.mesh:
        tok2 = T.contourlines;
        break;
      case T.nomesh:
        test = false;
        tok = T.mesh;
        tok2 = T.contourlines;
        break;
      case T.nodots:
        test = false;
        tok = T.dots;
        break;
      case T.nofill:
        test = false;
        tok = T.fill;
        break;
      case T.nobackshell:
        test = false;
        tok = T.backshell;
        break;
      case T.notriangles:
        test = false;
        tok = T.triangles;
        break;
      case T.notfrontonly:
        test = false;
        tok = T.frontonly;
        break;
      default:
        Logger.error("PROBLEM IN MESHCOLLECTION: token? " + T.nameOf(tok));
      }
      setTokenProperty(tok, test, false);
      if (tok2 != 0)
          setTokenProperty(tok2, test, true);
      return;
    }
    setPropS(propertyName, value, bs);
  }

  protected void checkExplicit(String id) {
    if (explicitID) // not twice
      return;
    explicitID = (id != null && !id.equals(MeshCollection.PREVIOUS_MESH_ID));
    if (explicitID)
      previousMeshID = id;
  } 
  
  protected void setTokenProperty(int tokProp, boolean bProp, boolean testD) {
    if (currentMesh == null) {
      String key = (explicitID && PT.isWild(previousMeshID) ? previousMeshID : null);
      Lst<Mesh> list = getMeshList(key, false);
      for (int i = list.size(); --i >= 0;)
        setMeshTokenProperty(list.get(i), tokProp, bProp, testD);
      if (list.size() == 1)
        currentMesh = list.get(0);
    } else {
      setMeshTokenProperty(currentMesh, tokProp, bProp, testD);
      if (linkedMesh != null)
        setMeshTokenProperty(linkedMesh, tokProp, bProp, testD);
    }
  }
 
  private void setMeshTokenProperty(Mesh m, int tokProp, boolean bProp, boolean testD) {
    if (testD && (!m.havePlanarContours || m.drawTriangles == m.showContourLines))
      return;
    switch (tokProp) {
    case T.display:
      m.bsDisplay = bsDisplay;
      if (bsDisplay == null && displayWithinPoints != null) 
        m.setShowWithin(displayWithinPoints, displayWithinDistance2, isDisplayWithinNot);
      return;
    case T.on:
      m.visible = bProp;
      return;
    case T.color:
      m.colix = colix;
      return;
    case T.translucent:
      m.setTranslucent(bProp, translucentLevel);
      // color isosurface translucent clears the slab
      if (bProp && m.bsSlabGhost != null)
        m.resetSlab();
      // color isosurface translucent or opaque clears all special translucent polygons
      //if (m.bsTransPolygons != null)
        //m.resetTransPolygons();
      return;
    default:
      m.setTokenProperty(tokProp, bProp);
    }
  }

  @SuppressWarnings("unchecked")
  protected boolean getPropDataMC(String property, Object[] data) {
    if (property == "keys") {
      Lst<String> keys = (data[1] instanceof Lst<?> ? (Lst<String>) data[1] : new Lst<String>());
      data[1] = keys;
      keys.addLast("count");
      keys.addLast("getCenter");
      // will continue on to getPropertyIndex
    }
    if (property == "getNames") {
      Map<String, T> map = (Map<String, T>) data[0];
      boolean withDollar = ((Boolean) data[1]).booleanValue();
      for (int i = meshCount; --i >= 0;)
        if (meshes[i] != null && meshes[i].vc != 0)
          map.put((withDollar ? "$" : "") + meshes[i].thisID, T.tokenOr); // just a placeholder
      return true;
    }
    if (property == "getVertices") {
      Mesh m = getMesh((String) data[0]);
      if (m == null)
        return false;
      data[1] = m.vs;
      data[2] = m.getVisibleVertexBitSet();
      return true;

    }
    if (property == "checkID") {
      String key = (String) data[0];
      Lst<Mesh> list = getMeshList(key, true);
      if (list.size() == 0)
        return false;
      data[1] = list.get(0).thisID;
      return true;
    }
    if (property == "index") {
      Mesh m = getMesh((String) data[0]);
      data[1] = Integer.valueOf(m == null ? -1 : m.index);
      return true;
    }    
    if (property == "getCenter") {
      String id = (String) data[0];
      int index = ((Integer) data[1]).intValue();
      Mesh m;
      if ((m = getMesh(id)) == null || m.vs == null)
        return false;
      if (index == Integer.MAX_VALUE)
        data[2] = P3.new3(m.index + 1, meshCount, m.vc);
      else
        data[2] = m.vs[m.getVertexIndexFromNumber(index)];
      return true;
    }
    return getPropShape(property, data);
  }

  /**
   * Get matching list of meshes, order reversed
   * 
   * @param key
   * @param justOne
   * @return list in reverse order, highest index first
   */
  protected Lst<Mesh> getMeshList(String key, boolean justOne) {
    Lst<Mesh> list = new Lst<Mesh>();
    if (key != null)
      key = (key.length() == 0 ? null : key.toUpperCase());
    boolean isWild = PT.isWild(key);
    String id;
    // important that this counts down because sometimes
    // we want just the MOST RECENT mesh.
    for (int i = meshCount; --i >= 0;)
      if (key == null
          || (id = meshes[i].thisID.toUpperCase()).equals(key) 
          || isWild && PT.isMatch(id, key, true, true)) {
        list.addLast(meshes[i]);
        if (justOne)
          break;
      }
    return list;
  }

  protected Object getPropMC(String property, int index) {
    Mesh m = currentMesh;
    if (index >= 0 && (index >= meshCount || (m = meshes[index]) == null))
      return null;
    if (property == "count") {
      int n = 0;
      for (int i = 0; i < meshCount; i++)
        if ((m = meshes[i]) != null && m.vc > 0)
          n++;
      return Integer.valueOf(n);
    }
    if (property == "bsVertices") {
      if (m == null)
        return null;
      Lst<Object> lst = new Lst<Object>();
      lst.addLast(m.vs);
      lst.addLast(m.getVisibleVBS());
      return lst;
    }
    if (property == "ID")
      return (m == null ? null : m.thisID);
    if (property.startsWith("list")) {
      clean();
      SB sb = new SB();
      int k = 0;
      boolean isNamed = property.length() > 5;
      String id = (property.equals("list") ? null
          : isNamed ? property.substring(5) : m == null ? null : m.thisID);
      for (int i = 0; i < meshCount; i++) {
        m = meshes[i];
        if (id != null && !id.equalsIgnoreCase(m.thisID))
          continue;
        sb.appendI((++k)).append(" id:" + m.thisID)
            .append("; model:" + vwr.getModelNumberDotted(m.modelIndex))
            .append("; vertices:" + m.vc).append("; polygons:" + m.pc)
            .append("; visible:" + m.visible);
        float[] range = (float[]) getProperty("dataRange", 0);
        if (range != null)
          sb.append("; dataRange:").append(Escape.eAF(range));
        if (m.title != null) {
          String s = "";
          for (int j = 0; j < m.title.length; j++)
            s += (j == 0 ? "; title:" : " | ") + m.title[j];
          if (s.length() > 10000)
            s = s.substring(0, 10000) + "...";
          sb.append(s);
        }
        sb.appendC('\n');
        if (isNamed) {
          Object info = getProperty("jvxlFileInfo", 0);
          if (info != null)
            sb.append((String) info).appendC('\n');
        }
      }
      return sb.toString();
    }
    if (property == "values")
      return getValues(m);
    if (property == "vertices")
      return getVertices(m);
    if (property == "info") {
      if (m == null)
        return null;
      @SuppressWarnings("unchecked")
      Map<String, Object> info = (Map<String, Object>) m.getInfo(false);
      if (info != null && jvxlData != null) {
        String ss = jvxlData.jvxlFileTitle;
        if (ss != null)
          info.put("jvxlFileTitle", ss.trim());
        ss = jvxlData.jvxlFileSource;
        if (ss != null)
          info.put("jvxlFileSource", ss);
        ss = jvxlData.jvxlFileMessage;
        if (ss != null)
          info.put("jvxlFileMessage", ss.trim());
      }
      return info;
    }
    if (property == "data")
      return (m == null ? null : m.getInfo(true));

    return null;
  }

  protected Object getValues(Mesh mesh) {
    return (mesh == null ? null : mesh.vvs);
  }
 
  protected Object getVertices(Mesh mesh) {
    return (mesh == null ? null : mesh.vs);
  }
 
  protected void clean() {
    for (int i = meshCount; --i >= 0;)
      if (meshes[i] == null || meshes[i].vc == 0)
        deleteMeshI(i);
  }

  private void deleteMesh() {
    if (explicitID && currentMesh != null)
      deleteMeshI(currentMesh.index);
    else
      deleteMeshKey(explicitID && previousMeshID != null
          && PT.isWild(previousMeshID) ?  
              previousMeshID : null);
    currentMesh = null;
  }

  protected void deleteMeshKey(String key) {
    if (key == null || key.length() == 0) {
      for (int i = meshCount; --i >= 0; )
        meshes[i] = null;
      meshCount = 0;
      nUnnamed = 0;
      if (htObjects != null)
        htObjects.clear();
    } else {
      Lst<Mesh> list = getMeshList(key, false);
      int n = list.size();
      // this will count DOWN since list is reverse order
      for (int i = 0; i < n; i++) 
        deleteMeshI(list.get(i).index);
    }
  }

  public void deleteMeshI(int i) {
    if (htObjects != null)
      htObjects.remove(meshes[i].thisID.toUpperCase());
    for (int j = i + 1; j < meshCount; ++j)
      meshes[--meshes[j].index] = meshes[j];
    meshes[--meshCount] = null;
  }
  
  protected void resetObjects() {
    htObjects.clear();
    for (int i = 0; i < meshCount; i++) {
      Mesh m = meshes[i];
      m.index = i;
      htObjects.put(m.thisID.toUpperCase(), m);
    }    
  }

  public Mesh getMesh(String thisID) {
    int i = getIndexFromName(thisID);
    return (i < 0 ? null : meshes[i]);
  }
  
  @Override
  public int getIndexFromName(String id) {
    if (MeshCollection.PREVIOUS_MESH_ID.equals(id))
      return (previousMeshID == null ? meshCount - 1
          : getIndexFromName(previousMeshID));
    if (PT.isWild(id)) {
      Lst<Mesh> list = getMeshList(id, true);
      return (list.size() == 0 ? -1 : list.get(0).index);
    }
    if (htObjects != null) {
      Mesh m = htObjects.get(id.toUpperCase());
      return (m == null ? -1 : m.index);
    }
    for (int i = meshCount; --i >= 0;) {
      if (meshes[i] != null && meshes[i].vc != 0
          && id.equalsIgnoreCase(meshes[i].thisID))
        return i;
    }
    return -1;
  }
  
  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * 
     */
    BS bsDeleted = vwr.slm.bsDeleted;
    for (int i = meshCount; --i >= 0;) {
      Mesh mesh = meshes[i];
      mesh.visibilityFlags = (mesh.visible
          && mesh.isValid
          && (mesh.modelIndex < 0 || bsModels.get(mesh.modelIndex)
              && (mesh.atomIndex < 0 || !ms.isAtomHidden(mesh.atomIndex)
                  && !(bsDeleted != null && bsDeleted.get(mesh.atomIndex)))) ? vf
          : 0);
    }
  }
 
  protected void setStatusPicked(int flag, T3 v, Map<String, Object> map) {
    // for draw and isosurface
    vwr.setStatusAtomPicked(flag, "[\"" + myType + "\"," + PT.esc(pickedMesh.thisID) + "," +
        + pickedModel + "," + pickedVertex + "," + v.x + "," + v.y + "," + v.z + "," 
        + (pickedMesh.title == null ? "\"\"" 
               : PT.esc(pickedMesh.title[0]))+"]", map, false);
  }

  protected Map<String, Object> getPickedPoint(T3 v, int modelIndex) {
    Map<String, Object> map = new Hashtable<String, Object>();
    if (v != null) {
      map.put("pt", v);
      map.put("modelIndex", Integer.valueOf(modelIndex));
      map.put("model", vwr.getModelNumberDotted(modelIndex));
      map.put("id", pickedMesh.thisID);
      map.put("vertex", Integer.valueOf(pickedVertex + 1));
      map.put("type", myType);
    }
    return map;
  }

}

 