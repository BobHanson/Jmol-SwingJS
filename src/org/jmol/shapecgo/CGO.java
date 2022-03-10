/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 17:19:14 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4529 $
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

package org.jmol.shapecgo;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import javajs.util.BS;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;

public class CGO extends MeshCollection {
  
  CGOMesh[] cmeshes = new CGOMesh[4];
  private CGOMesh cgoMesh;
  private boolean useColix; // not implemented?
  private float newScale; // not implemented
  private int indicatedModelIndex = Integer.MIN_VALUE;

  
  public CGO() {
    // for reflection
    myType = "CGO";
    htObjects = new Hashtable<String, Mesh>();
  }

  private void initCGO() {
    indicatedModelIndex = Integer.MIN_VALUE;
  }

  @Override
  public void allocMesh(String thisID, Mesh m) {
    int index = meshCount++;
    meshes = cmeshes = (CGOMesh[]) AU.ensureLength(cmeshes,
        meshCount * 2);
    currentMesh = cgoMesh = cmeshes[index] = (m == null ? new CGOMesh(vwr, thisID,
        colix, index) : (CGOMesh) m);
    currentMesh.color = color;
    currentMesh.index = index;
    currentMesh.useColix = useColix;
    currentMesh.modelIndex = indicatedModelIndex;
    if (thisID != null && thisID != MeshCollection.PREVIOUS_MESH_ID
        && htObjects != null)
      htObjects.put(thisID.toUpperCase(), currentMesh);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      initCGO();
      setPropertySuper("init", value, bs);
      return;
    }
    
    if ("setCGO" == propertyName) {
      Lst<Object> list = (Lst<Object>) value;
      setProperty("init", null, null);
      int n = list.size() - 1;
      setProperty("thisID", list.get(n), null);
      propertyName = "set";
      setProperty("set", value, null);
      return;
    }
    
    if ("modelIndex" == propertyName) {
      indicatedModelIndex = Math.max(((Integer) value).intValue(), -1);
      return;
    }

    if ("set" == propertyName) {
      if (cgoMesh == null) {
        allocMesh(null, null);
        cgoMesh.colix = colix;
        cgoMesh.color = color;
        cgoMesh.useColix = useColix;
      }
      cgoMesh.modelIndex = (indicatedModelIndex == Integer.MIN_VALUE ? vwr.am.cmi : indicatedModelIndex);
      cgoMesh.isValid = setCGO((Lst<Object>) value);
      if (cgoMesh.isValid) {
        scale(cgoMesh, newScale );
        cgoMesh.initialize(T.fullylit, null, null);
        cgoMesh.title = title;
        cgoMesh.visible = true;
      }
      clean();
      return;
    }
    
    if (propertyName == "deleteModelAtoms") {
      deleteModels(((int[]) ((Object[]) value)[2])[0]);
      return;
    }

    setPropertySuper(propertyName, value, bs);
  }
  
  protected void deleteModels(int modelIndex) {
    //int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
    //int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
    for (int i = meshCount; --i >= 0;) {
      CGOMesh m = (CGOMesh) meshes[i];
      if (m == null)
        continue;
      boolean deleteMesh = (m.modelIndex == modelIndex);
      if (deleteMesh) {
        meshCount--;
        deleteMeshElement(i);
      } else if (meshes[i].modelIndex > modelIndex) {
        meshes[i].modelIndex--;
      }
    }
    resetObjects(); 
   }


  @Override
  public Object getProperty(String property, int index) {
    if (property == "command")
      return getCommand(cgoMesh);
    return getPropMC(property, index);
  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "data")
      return CGOMesh.getData(data);
    return getPropDataMC(property, data);
  }

  private void deleteMeshElement(int i) {
    if (meshes[i] == currentMesh)
      currentMesh = cgoMesh = null;
    meshes = cmeshes = (CGOMesh[]) AU
        .deleteElements(meshes, i, 1);
  }

  private void setPropertySuper(String propertyName, Object value, BS bs) {
    currentMesh = cgoMesh;
    setPropMC(propertyName, value, bs);
    cgoMesh = (CGOMesh)currentMesh;  
  }

  @Override
  protected void clean() {
    for (int i = meshCount; --i >= 0;)
      if (meshes[i] == null || cmeshes[i].cmds == null || cmeshes[i].cmds.size() == 0)
        deleteMeshI(i);
  }

  private boolean setCGO(Lst<Object> data) {
    if (cgoMesh == null)
      allocMesh(null, null);
    cgoMesh.clear("cgo");
    return cgoMesh.set(data);
  }

  private void scale(Mesh mesh, float newScale) {
    // TODO
    
  }
  
  @Override
  public Object getShapeDetail() {
    Lst<Map<String, Object>> V = new Lst<Map<String, Object>>();
    for (int i = 0; i < meshCount; i++) {
      CGOMesh mesh = cmeshes[i];
      Map<String, Object> info = new Hashtable<String, Object>();
      info.put("visible", mesh.visible ? Boolean.TRUE : Boolean.FALSE);
      info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
      info.put("command", getCommand(mesh));
      V.addLast(info);
    }
    return V;
  }

  @Override
  public String getShapeState() {
    SB sb = new SB();
    int modelCount = ms.mc;
    for (int i = 0; i < meshCount; i++) {
      CGOMesh mesh = cmeshes[i];
      if (mesh == null || mesh.cmds == null || mesh.modelIndex >= modelCount)
        continue;
      if (sb.length() == 0) {
        sb.append("\n");
        appendCmd(sb, myType + " delete");
      }
      sb.append(getCommand2(mesh, modelCount));
      if (!mesh.visible)
        sb.append(" " + myType + " ID " + PT.esc(mesh.thisID) + " off;\n");
    }
    return sb.toString();
  }
  
  private String getCommand(Mesh mesh) {
    if (mesh != null)
      return getCommand2(mesh, mesh.modelIndex);

    SB sb = new SB();
    String key = (explicitID && previousMeshID != null
        && PT.isWild(previousMeshID) ? previousMeshID : null);
    Lst<Mesh> list = getMeshList(key, false);
    // counting down on list will be up in order
    for (int i = list.size(); --i >= 0;) {
      Mesh m = list.get(i);
      sb.append(getCommand2(m, m.modelIndex));
    }
    return sb.toString();
  }

  private String getCommand2(Mesh mesh, int modelCount) {
    CGOMesh cmesh = (CGOMesh) mesh;
    SB str = new SB();
    int iModel = mesh.modelIndex;
    str.append("  CGO ID ").append(PT.esc(mesh.thisID));
    if (iModel >= -1 && modelCount > 1)
      str.append(" modelIndex " + iModel);
    str.append(" [");
    int n = cmesh.cmds.size();
    for (int i = 0; i < n; i++)
      str.append(" " + cmesh.cmds.get(i));
    str.append(" ];\n");
    appendCmd(str, cmesh.getState("cgo"));
    if (cmesh.useColix)
      appendCmd(str, getColorCommandUnk("cgo", cmesh.colix, translucentAllowed));
    return str.toString();
  }
  
  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    for (int i = 0; i < meshCount; i++) {
      CGOMesh m = cmeshes[i];
      if (m != null)
        m.visibilityFlags = (m.isValid && m.visible && (m.modelIndex < 0 || bsModels.get(m.modelIndex)) ? vf : 0);
    }
  }
 
}
