/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 11:08:02 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7492 $
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

/*
 * miguel 2005 07 17
 *
 *  System and method for the display of surface structures
 *  contained within the interior region of a solid body
 * United States Patent Number 4,710,876
 * Granted: Dec 1, 1987
 * Inventors:  Cline; Harvey E. (Schenectady, NY);
 *             Lorensen; William E. (Ballston Lake, NY)
 * Assignee: General Electric Company (Schenectady, NY)
 * Appl. No.: 741390
 * Filed: June 5, 1985
 *
 *
 * Patents issuing prior to June 8, 1995 can last up to 17
 * years from the date of issuance.
 *
 * Dec 1 1987 + 17 yrs = Dec 1 2004
 */

/*
 * Bob Hanson May 22, 2006
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 *  
 * inventing "Jmol Voxel File" format, *.jvxl
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters represting the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 */

package org.jmol.shapesurface;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javajs.api.GenericBinaryDocument;
import javajs.util.A4;
import javajs.util.AU;
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import javajs.util.BS;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.jvxl.readers.SurfaceGenerator;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;
import org.jmol.util.TempArray;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public class Isosurface extends MeshCollection implements MeshDataServer {

  protected IsosurfaceMesh[] isomeshes = new IsosurfaceMesh[4];
  protected IsosurfaceMesh thisMesh;

  @Override
  public void allocMesh(String thisID, Mesh m) {
    int index = meshCount++;
    meshes = isomeshes = (IsosurfaceMesh[]) AU.ensureLength(isomeshes,
        meshCount * 2);
    currentMesh = thisMesh = isomeshes[index] = (m == null ? new IsosurfaceMesh(vwr,
        thisID, colix, index) : (IsosurfaceMesh) m);
    currentMesh.index = index;
    if (sg != null)
      sg.setJvxlData(jvxlData = thisMesh.jvxlData);
  }

  @Override
  public void initShape() {
    super.initShape();
    myType = "isosurface";
    newSg();
  }

  protected void newSg() {
    sg = new SurfaceGenerator(vwr, this, null, jvxlData = new JvxlData());
    sg.params.showTiming = vwr.getBoolean(T.showtiming);
    sg.version = "Jmol " + Viewer.getJmolVersion();
  }
  
  protected void clearSg() {
    sg = null; // not Molecular Orbitals
  }
  //private boolean logMessages;
  private String actualID;
  protected boolean iHaveBitSets;
  private boolean explicitContours;
  private int atomIndex;
  private int moNumber;
  private float[] moLinearCombination;
  private int colorType;
  private short defaultColix;
  private short meshColix;
  private P3 center;
  private float scale3d;
  private boolean isPhaseColored;
  private boolean isColorExplicit;
  private String scriptAppendix = "";

  protected SurfaceGenerator sg;
  protected JvxlData jvxlData;

  private float withinDistance2;
  private boolean isWithinNot;
  private Lst<P3> withinPoints;
  private float[] cutoffRange;

  //private boolean allowContourLines;
  boolean allowMesh = true;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    setPropI(propertyName, value, bs);
  }  

  @SuppressWarnings("unchecked")
  protected void setPropI(String propertyName, Object value, BS bs) {

    //System.out.println("isosurface testing " + propertyName + " " + value + (propertyName == "token" ? " " + T.nameOf(((Integer)value).intValue()) : ""));

    //isosurface-only (no calculation required; no calc parameters to set)

    //    if ("navigate" == propertyName) {
    //      navigate(((Integer) value).intValue());
    //      return;
    //    }
    if ("cache" == propertyName) {
      if (currentMesh == null)
        return;
      String id = currentMesh.thisID;
      int imodel = currentMesh.modelIndex;
      vwr.cachePut("cache://isosurface_" + id, ((String)getPropI("jvxlDataXml", -1)).getBytes());
      deleteMeshI(currentMesh.index);
      setPropI("init", null, null);
      setPropI("thisID", id, null);
      setPropI("modelIndex", Integer.valueOf(imodel), null);
      setPropI("fileName", "cache://isosurface_" + id, null);
      setPropI("readFile", null, null);
      setPropI(
          "finalize",
          "isosurface ID " + PT.esc(id)
              + (imodel >= 0 ? " modelIndex " + imodel : "") + " /*file*/"
              + PT.esc("cache://isosurface_" + id), null);
      setPropI("clear", null, null);
      return;
    }
    if ("delete" == propertyName) {
      setPropertySuper(propertyName, value, bs);
      if (!explicitID)
        nLCAO = nUnnamed = 0;
      currentMesh = thisMesh = null;
      return;
    }

    if ("remapInherited" == propertyName) {
      for (int i = meshCount; --i >= 0;) {
        if (isomeshes[i] != null
            && "#inherit;".equals(isomeshes[i].colorCommand))
          isomeshes[i].remapColors(vwr, null, Float.NaN);
      }
      return;
    }

    if ("remapColor" == propertyName) {
      if (thisMesh != null)
        thisMesh.remapColors(vwr, (ColorEncoder) value, translucentLevel);
      return;
    }

    if ("thisID" == propertyName) {
      if (actualID != null)
        value = actualID;
      setPropertySuper("thisID", value, null);
      return;
    }

    if ("params" == propertyName) {
      if (thisMesh != null) {
        ensureMeshSource();
        thisMesh.checkAllocColixes();
        Object[] data = (Object[]) value;
        short[] colixes = (short[]) data[0];
        int[] atomMap = null;
        //float[] atrans = (float[]) data[1];
        if (colixes != null) {
          for (int i = 0; i < colixes.length; i++) {
            short colix = colixes[i];
            float f = 0;//(atrans == null ? 0 : atrans[pt]);
            if (f > 0.01f)
              colix = C.getColixTranslucent3(colix, true, f);
            colixes[i] = colix;
          }
          atomMap = new int[bs.length()];
          for (int pt = 0, i = bs.nextSetBit(0); i >= 0; i = bs
              .nextSetBit(i + 1), pt++)
            atomMap[i] = pt;
        }
        thisMesh.setVertexColixesForAtoms(vwr, colixes, atomMap, bs);
        thisMesh.setVertexColorMap();
      }
      return;
    }
    if ("atomcolor" == propertyName) {
      // color $id red ({0:30 ....})  (atoms)
      // color $id red [{0:30 ....}]  (vertices)
      if (thisMesh != null) {
        ensureMeshSource();
        thisMesh.colorVertices(C.getColixO(value), bs, true);
      }
      return;
    }

    if ("pointSize" == propertyName) {
      if (thisMesh != null) {
        thisMesh.volumeRenderPointSize = ((Float) value).floatValue();
      }
      return;
    }

    if ("vertexcolor" == propertyName) {
      if (thisMesh != null) {
        thisMesh.colorVertices(C.getColixO(value), bs, false);
      }
      return;
    }

    if ("colorPhase" == propertyName) {
      // from color isosurface phase color1 color2  Jmol 12.3.5
      Object[] colors = (Object[]) value;
      short colix0 = C.getColix(((Integer) colors[0]).intValue());
      short colix1 = C.getColix(((Integer) colors[1]).intValue());
      String id = (thisMesh != null ? thisMesh.thisID : PT
          .isWild(previousMeshID) ? previousMeshID : null);
      Lst<Mesh> list = getMeshList(id, false);
      for (int i = list.size(); --i >= 0;)
        setColorPhase((IsosurfaceMesh) list.get(i), colix0, colix1);
      return;
    }
    if ("color" == propertyName) {
      String color = C.getHexCode(C.getColixO(value));
      if (thisMesh != null) {
        setIsoMeshColor(thisMesh, color);
      } else {
        Lst<Mesh> list = getMeshList(PT.isWild(previousMeshID) ? previousMeshID
            : null, false);
        for (int i = list.size(); --i >= 0;)
          setIsoMeshColor((IsosurfaceMesh) list.get(i), color);
      }
      setPropertySuper(propertyName, value, bs);
      return;
    }

    if ("nocontour" == propertyName) {
      // recontouring
      if (thisMesh != null) {
        thisMesh.deleteContours();
      }
      return;
    }
    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      setMeshI();
      return;
    }

    if ("newObject" == propertyName) {
      if (thisMesh != null)
        thisMesh.clearType(thisMesh.meshType, false);
      return;
    }

    if ("moveIsosurface" == propertyName) {
      if (thisMesh != null && !thisMesh.isModelConnected) {
        thisMesh.updateCoordinates((M4) value, null);
        thisMesh.altVertices = null;
      }
      return;
    }

    if ("refreshTrajectories" == propertyName) {
      int m = ((Integer) ((Object[]) value)[0]).intValue();
      for (int i = meshCount; --i >= 0;)
        if (meshes[i].modelIndex == m
            && (meshes[i].connectedAtoms != null || meshes[i].isModelConnected))
          ((IsosurfaceMesh) meshes[i]).updateCoordinates(
              (M4) ((Object[]) value)[2], (BS) ((Object[]) value)[1]);
      return;
    }

    if ("modelIndex" == propertyName) {
      if (!iHaveModelIndex) {
        modelIndex = ((Integer) value).intValue();
        isFixed = (modelIndex < 0);
        sg.params.modelIndex = Math.abs(modelIndex);
      }
      return;
    }

    if ("lcaoCartoon" == propertyName || "lonePair" == propertyName
        || "radical" == propertyName) {
      // z x center rotationAxis (only one of x, y, or z is nonzero; in radians)
      V3[] info = (V3[]) value;
      if (!explicitID) {
        setPropertySuper("thisID", null, null);
      }
      // center (info[2]) is set in SurfaceGenerator
      if (!sg.setProp("lcaoCartoonCenter", info[2], null))
        drawLcaoCartoon(
            info[0],
            info[1],
            info[3],
            ("lonePair" == propertyName ? 2 : "radical" == propertyName ? 1 : 0));
      return;
    }

    if ("select" == propertyName) {
      if (iHaveBitSets)
        return;
    }

    if ("ignore" == propertyName) {
      if (iHaveBitSets)
        return;
    }

    if ("meshcolor" == propertyName) {
      int rgb = ((Integer) value).intValue();
      meshColix = C.getColix(rgb);
      if (thisMesh != null)
        thisMesh.meshColix = meshColix;
      return;
    }

    if ("offset" == propertyName) {
      P3 offset = P3.newP((P3) value);
      if (offset.equals(JC.center))
        offset = null;
      if (thisMesh != null) {
        thisMesh.rotateTranslate(null, offset, true);
        thisMesh.altVertices = null;
      }
      return;
    }

    if ("rotate" == propertyName) {
      P4 pt4 = (P4) value;
      if (thisMesh != null) {
        thisMesh.rotateTranslate(Quat.newP4(pt4), null, true);
        thisMesh.altVertices = null;
      }
      return;
    }

    if ("bsDisplay" == propertyName) {
      bsDisplay = (BS) value;
      return;
    }
    if ("displayWithin" == propertyName) {
      Object[] o = (Object[]) value;
      displayWithinDistance2 = ((Float) o[0]).floatValue();
      isDisplayWithinNot = (displayWithinDistance2 < 0);
      displayWithinDistance2 *= displayWithinDistance2;
      displayWithinPoints = (Lst<P3>) o[3];
      if (displayWithinPoints.size() == 0)
        displayWithinPoints = vwr.ms.getAtomPointVector((BS) o[2]);
      return;
    }

    if ("finalize" == propertyName) {
      if (thisMesh != null) {
        String cmd = (String) value;
        if (cmd != null && !cmd.startsWith("; isosurface map")) {
          thisMesh.setDiscreteColixes(sg.params.contoursDiscrete,
              sg.params.contourColixes);
          setJvxlInfo();
        }
        setScriptInfo(cmd);
      }
      clearSg();
      return;
    }

    if ("connections" == propertyName) {
      if (currentMesh != null) {
        connections = (int[]) value;
        if (connections[0] >= 0 && connections[0] < vwr.ms.ac)
          currentMesh.connectedAtoms = connections;
        else
          connections = currentMesh.connectedAtoms = null;
      }
      return;
    }

    if ("cutoffRange" == propertyName) {
      cutoffRange = (float[]) value;
      return;
    }

    if ("fixLattice" == propertyName) {
      if (thisMesh != null)
        thisMesh.fixLattice();
      return;
    }

    // Isosurface / SurfaceGenerator both interested

    if ("slab" == propertyName) {
      if (value instanceof Integer) {
        if (thisMesh != null)
          thisMesh.jvxlData.slabValue = ((Integer) value).intValue();
        return;
      }
      if (thisMesh != null) {
        Object[] slabInfo = (Object[]) value;
        int tok = ((Integer) slabInfo[0]).intValue();
        switch (tok) {
        case T.mesh:
          Object[] data = (Object[]) slabInfo[1];
          Mesh m = getMesh((String) data[1]);
          if (m == null)
            return;
          data[1] = m;
          break;
        }
        slabPolygons(slabInfo);
        return;
      }
    }

    if ("cap" == propertyName) {
      if (thisMesh != null && thisMesh.pc != 0) {
        thisMesh.getMeshSlicer().slabPolygons((Object[]) value, true);
        thisMesh.initialize(thisMesh.lighting, null, null);
        return;
      }
    }
    if ("map" == propertyName) {
      if (sg != null)
        sg.params.isMapped = true;
      setProperty("squareData", Boolean.FALSE, null);
      if (thisMesh == null || thisMesh.vc == 0)
        return;
    }

    if ("deleteVdw" == propertyName) {
      for (int i = meshCount; --i >= 0;)
        if (isomeshes[i].bsVdw != null
            && (bs == null || bs.intersects(isomeshes[i].bsVdw)))
          deleteMeshI(i);
      currentMesh = thisMesh = null;
      return;
    }
    if ("mapColor" == propertyName || "readFile" == propertyName) {
      if (value == null) {
        // ScriptEvaluator has passed the filename to us as the value of the
        // "fileName" property. We retrieve that from the surfaceGenerator
        // and open a BufferedReader for it. Or not. But that would be
        // unlikely since we have just checked it in ScriptEvaluator
        
        if (sg.params.filesData == null) {
          value = getFileReader(sg.params.fileName);
        } else {
          value = sg.params.filesData;
          String[] a = (String[]) sg.params.filesData[0];
          Object[] b = new Object[a.length];
          for (int i = b.length; --i >= 0 && value != null;)
            if ((b[i] = getFileReader(a[i])) == null)
              value = null;
          if (value != null)
            sg.params.filesData[0] = b;
        }
        if (value == null)
          return;
      }
    } else if ("atomIndex" == propertyName) {
      atomIndex = ((Integer) value).intValue();
      if (thisMesh != null)
        thisMesh.atomIndex = atomIndex;
    } else if ("center" == propertyName) {
      center.setT((P3) value);
    } else if ("colorRGB" == propertyName) {
      int rgb = ((Integer) value).intValue();
      if (rgb == T.symop) {
        colorType = rgb;
      } else {
        colorType = 0;
        defaultColix = C.getColix(rgb);
      }
    } else if ("contour" == propertyName) {
      explicitContours = true;
    } else if ("functionXY" == propertyName) {
      //allowContourLines = false;
      if (sg.params.state == Parameters.STATE_DATA_READ)
        setScriptInfo(null); // for script DATA1
    } else if ("init" == propertyName) {
      newSg();
    } else if ("getSurfaceSets" == propertyName) {
      if (thisMesh != null) {
        thisMesh.jvxlData.thisSet = ((Integer) value).intValue();
        thisMesh.calculatedVolume = null;
        thisMesh.calculatedArea = null;
      }
    } else if ("localName" == propertyName) {
      value = vwr.getOutputChannel((String) value, null);
      propertyName = "outputChannel";
    } else if ("molecularOrbital" == propertyName) {
      isFixed = false;
      setMeshI();
      if (value instanceof Integer) {
        moNumber = ((Integer) value).intValue();
        moLinearCombination = null;
      } else {
        moLinearCombination = (float[]) value;
        moNumber = 0;
      }
      if (!isColorExplicit)
        isPhaseColored = true;
      if (sg == null || !sg.params.isMapped) {
        M4 mat4 =  ms.am[currentMesh.modelIndex].mat4;
        if (mat4 != null) {
          M4 minv = M4.newM4(mat4);
          minv.invert();
          setPropI("modelInvRotation", minv, null);
        }
      }
    } else if ("phase" == propertyName) {
      isPhaseColored = true;
    } else if ("plane" == propertyName) {
      //allowContourLines = false;
    } else if ("pocket" == propertyName) {
      // Boolean pocket = (Boolean) value;
      // lighting = (pocket.booleanValue() ? JmolConstants.FULLYLIT
      //     : JmolConstants.FRONTLIT);
    } else if ("scale3d" == propertyName) {
      scale3d = ((Float) value).floatValue();
      if (thisMesh != null) {
        thisMesh.scale3d = thisMesh.jvxlData.scale3d = scale3d;
        thisMesh.altVertices = null;
      }
    } else if ("title" == propertyName) {
      if (value instanceof String && "-".equals(value))
        value = null;
      setPropertySuper(propertyName, value, bs);
      value = title;
    } else if ("withinPoints" == propertyName) {
      Object[] o = (Object[]) value;
      withinDistance2 = ((Float) o[0]).floatValue();
      isWithinNot = (withinDistance2 < 0);
      withinDistance2 *= withinDistance2;
      withinPoints = (Lst<P3>) o[3];
      if (withinPoints.size() == 0)
        withinPoints = vwr.ms.getAtomPointVector((BS) o[2]);
    } else if (("nci" == propertyName || "orbital" == propertyName)
        && sg != null) {
      sg.params.testFlags = (vwr.getBoolean(T.testflag2) ? 2 : 0);
    }

    // surface Export3D only (return TRUE) or shared (return FALSE)

    if (sg != null && sg.setProp(propertyName, value, bs)) {
      if (sg.isValid) {
        if ("molecularOrbital" == propertyName) {
          currentMesh.isModelConnected = true;
          currentMesh.mat4 = ms.am[currentMesh.modelIndex].mat4;          
        }
        return;
      }
      propertyName = "delete";
    }

    // ///////////// isosurface LAST, shared

    if ("init" == propertyName) {
      explicitID = false;
      scriptAppendix = "";
      String script = (value instanceof String ? (String) value : null);
      int pt = (script == null ? -1 : script.indexOf("# ID="));
      actualID = (pt >= 0 ? PT.getQuotedStringAt(script, pt) : null);
      setPropertySuper("thisID", MeshCollection.PREVIOUS_MESH_ID, null);
      if (script != null && !(iHaveBitSets = getScriptBitSets(script, null)))
        sg.setProp("select", bs, null);
      initializeIsosurface();
      sg.params.modelIndex = (isFixed ? -1 : modelIndex);
      return;
    }

    if ("clear" == propertyName) {
      discardTempData(true);
      return;
    }

    if ("colorDensity" == propertyName) {
      if (value != null && currentMesh != null)
        currentMesh.volumeRenderPointSize = ((Float) value).floatValue();
      return;
    }
    /*
     * if ("background" == propertyName) { boolean doHide = !((Boolean)
     * value).booleanValue(); if (thisMesh != null) thisMesh.hideBackground =
     * doHide; else { for (int i = meshCount; --i >= 0;)
     * meshes[i].hideBackground = doHide; } return; }
     */

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      for (int i = meshCount; --i >= 0;) {
        Mesh m = meshes[i];
        if (m == null)
          continue;
        if (m.connectedAtoms != null) {
          int iAtom = m.connectedAtoms[0];
          if (iAtom >= firstAtomDeleted + nAtomsDeleted)
            m.connectedAtoms[0] = iAtom - nAtomsDeleted;
          else if (iAtom >= firstAtomDeleted)
            m.connectedAtoms = null;
        }
        m.connectedAtoms = null; // just no way to 
        if (m.modelIndex == modelIndex) {
          meshCount--;
          if (m == currentMesh)
            currentMesh = thisMesh = null;
          meshes = isomeshes = (IsosurfaceMesh[]) AU.deleteElements(meshes, i,
              1);
        } else if (m.modelIndex > modelIndex) {
          m.modelIndex--;
          if (m.atomIndex >= firstAtomDeleted)
            m.atomIndex -= nAtomsDeleted;
        }
      }
      return;
    }

    // processing by meshCollection:
    setPropertySuper(propertyName, value, bs);
  }

  private Object getFileReader(String fileName) {
    Object value = vwr.fm.getBufferedReaderOrErrorMessageFromName(
        fileName, null, true, true);
    if (value instanceof String) {
      Logger.error("Isosurface: could not open file " + fileName
          + " -- " + value);
      return null;
    }
    if (!(value instanceof BufferedReader))
      try {
        value = Rdr.getBufferedReader((BufferedInputStream) value,
            "ISO-8859-1");
      } catch (IOException e) {
        // ignore
      }
    return value;
  }

  private void setIsoMeshColor(IsosurfaceMesh m, String color) {
    // thisMesh.vertexColixes = null;
    m.jvxlData.baseColor = color;
    m.isColorSolid = true;
    m.pcs = null;
    m.colorsExplicit = false;
    m.colorEncoder = null;
    m.vertexColorMap = null;
  }

  private void setColorPhase(IsosurfaceMesh m, short colix0,
                             short colix1) {
    m.colorPhased = true;
    m.colix = m.jvxlData.minColorIndex = colix0;
    m.jvxlData.maxColorIndex = colix1;
    m.jvxlData.isBicolorMap = true;
    m.jvxlData.colorDensity = false;
    m.isColorSolid = false;
    m.remapColors(vwr, null, translucentLevel);
  }

  private void ensureMeshSource() {
    boolean haveColors = (thisMesh.vertexSource != null);
    if (haveColors)
      for (int i = thisMesh.vc; --i >= 0;)
        if (thisMesh.vertexSource[i] < 0) {
          haveColors = false;
          break;
        }
    if (!haveColors) {
      int[] source = thisMesh.vertexSource;
      short[] vertexColixes = thisMesh.vcs;
      short colix = (thisMesh.isColorSolid ? thisMesh.colix : 0);
      setProperty("init", null, null);
      setProperty("map", Boolean.FALSE, null);
      setProperty("property", new float[vwr.ms.ac], null);
      if (colix != 0) {
        thisMesh.colorCommand = "color isosurface "
            + C.getHexCode(colix);
        setProperty("color", Integer.valueOf(C.getArgb(colix)), null);
      }                      
      if (source != null) {
        for (int i = thisMesh.vc; --i >= 0;)
          if (source[i] < 0)
            source[i] = thisMesh.vertexSource[i];
        thisMesh.vertexSource = source;
        thisMesh.vcs = vertexColixes;
      }
    }
  }

  protected void slabPolygons(Object[] slabInfo) {
    thisMesh.calculatedVolume = null;
    thisMesh.calculatedArea = null;
    thisMesh.getMeshSlicer().slabPolygons(slabInfo, false);
    thisMesh.reinitializeLightingAndColor(vwr);
  }

  private void setPropertySuper(String propertyName, Object value, BS bs) {
    if (propertyName == "thisID" && currentMesh != null 
        && currentMesh.thisID != null && currentMesh.thisID.equals(value)) {
      checkExplicit((String) value);
      return;
    }
    currentMesh = thisMesh;
    setPropMC(propertyName, value, bs);
    thisMesh = (IsosurfaceMesh) currentMesh;
    jvxlData = (thisMesh == null ? null : thisMesh.jvxlData);
    if (sg != null)
      sg.setJvxlData(jvxlData);
  }

  
  @SuppressWarnings("unchecked")
  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "keys") {
      Lst<String> keys = (data[1] instanceof Lst<?> ? (Lst<String>) data[1] : new Lst<String>());
      data[1] = keys;
      keys.addLast("info");
      keys.addLast("data");
      keys.addLast("atoms");
      // will continue on to super
    }
    if (property == "colorEncoder") {
      IsosurfaceMesh mesh = (IsosurfaceMesh) getMesh((String) data[0]);
      return (mesh != null && (data[1] = mesh.colorEncoder) != null);
    }
    if (property == "intersectPlane") {
      IsosurfaceMesh mesh = (IsosurfaceMesh) getMesh((String) data[0]);
      if (mesh == null || data.length < 4)
        return false;
      data[3] = Integer.valueOf(mesh.modelIndex);
      mesh.getMeshSlicer().getIntersection(0, (P4) data[1], null, (Lst<P3[]>) data[2], null, null, null, false, false, T.plane, false);
      return true;
    }
    if (property == "getBoundingBox") {
      String id = (String) data[0];
      IsosurfaceMesh m = (IsosurfaceMesh) getMesh(id);
      if (m == null || m.vs == null)
        return false;
      data[2] = m.jvxlData.boundingBox;
      if (m.mat4 != null) {
        P3[] d = new P3[2];
        d[0] = P3.newP(m.jvxlData.boundingBox[0]);
        d[1] = P3.newP(m.jvxlData.boundingBox[1]);
        V3 v = new V3();
        m.mat4.getTranslation(v);
        d[0].add(v);
        d[1].add(v);
        data[2] = d;
      }
      return true;
    }
    if (property == "unitCell") {
      IsosurfaceMesh m = (IsosurfaceMesh) getMesh((String) data[0]);
      return (m != null && (data[1] = m.getUnitCell()) != null);
    }
    if (property == "getCenter") {
      int index = ((Integer)data[1]).intValue();
      if (index == Integer.MIN_VALUE) {
        String id = (String) data[0];
        IsosurfaceMesh m = (IsosurfaceMesh) getMesh(id);
        if (m == null || m.vs == null)
          return false;
        P3 p = P3.newP(m.jvxlData.boundingBox[0]);
        p.add(m.jvxlData.boundingBox[1]);
        p.scale(0.5f);
        if (m.mat4 != null) {
          V3 v = new V3();
          m.mat4.getTranslation(v);
          p.add(v);
        }
        data[2] = p;
        return true;
      }
      // continue to super
    }

    return getPropDataMC(property, data);
  }

  @Override
  public Object getProperty(String property, int index) {
    return getPropI(property, index);
  }

  protected Object getPropI(String property, int index) {
    IsosurfaceMesh thisMesh = this.thisMesh;
    if (index >= 0 && (index >= meshCount || (thisMesh = isomeshes[index]) == null))
      return null;
    Object ret = getPropMC(property, index);
    if (ret != null)
      return ret;
    if (property == "message") {
      String s = "";
      if (!jvxlData.isValid)
        return "invalid! (no atoms selected?)";
      if (!Float.isNaN(jvxlData.integration))
        s += "integration " + jvxlData.integration;
      if (shapeID == JC.SHAPE_ISOSURFACE || shapeID == JC.SHAPE_MO  || shapeID == JC.SHAPE_NBO)
        s += " with cutoff=" + jvxlData.cutoff;
      if (shapeID == JC.SHAPE_MO || shapeID == JC.SHAPE_NBO)
        return s;
      if (jvxlData.dataMin != Float.MAX_VALUE)
        s += " min=" + jvxlData.dataMin + " max=" + jvxlData.dataMax;
      
      s += "; " + JC.shapeClassBases[shapeID].toLowerCase() + " count: "
          + getPropMC("count", index);
      return s + getPropI("dataRangeStr", index) + jvxlData.msg;
    }
    if (property == "dataRange")
      return getDataRange(thisMesh);
    if (property == "dataRangeStr") {
      float[] dataRange = getDataRange(thisMesh);
      return (dataRange != null && dataRange[0] != Float.MAX_VALUE
          && dataRange[0] != dataRange[1] ? "\nisosurface"
          + " full data range " + dataRange[0] + " to " + dataRange[1]
          + " with color scheme spanning " + dataRange[2] + " to " + dataRange[3]
          : "");
    }
    if (property == "moNumber")
      return Integer.valueOf(moNumber);
    if (property == "moLinearCombination")
      return moLinearCombination;
    if (property == "nSets")
      return Integer.valueOf(thisMesh == null ? 0 : thisMesh.nSets);
    if (property == "area") // could be Float or double[]
      return (thisMesh == null ? Float.valueOf(Float.NaN) : calculateVolumeOrArea(thisMesh, true));
    if (property == "volume") // could be Float or double[]
      return (thisMesh == null ? Float.valueOf(Float.NaN) : calculateVolumeOrArea(thisMesh, false));
    if (thisMesh == null)
      return null;//"no current isosurface";
    if (property == "cutoff")
      return Float.valueOf(jvxlData.cutoff);
    if (property == "minMaxInfo")
      return new float[] { jvxlData.dataMin, jvxlData.dataMax };
    if (property == "plane")
      return jvxlData.jvxlPlane;
    if (property == "contours")
      return thisMesh.getContours();
    if (property == "pmesh" || property == "pmeshbin")
      return thisMesh.getPmeshData(property == "pmeshbin"); 
    if (property == "jvxlDataXml" || property == "jvxlMeshXml") {
      MeshData meshData = null;
      jvxlData.slabInfo = null;
      if (property == "jvxlMeshXml" || jvxlData.vertexDataOnly || thisMesh.bsSlabDisplay != null && thisMesh.bsSlabGhost == null) {
        meshData = new MeshData();
        fillMeshData(meshData, MeshData.MODE_GET_VERTICES, thisMesh);
        meshData.polygonColorData = getPolygonColorData(meshData.pc, meshData.pcs, (meshData.colorsExplicit ? meshData.pis : null), meshData.bsSlabDisplay);
      } else if (thisMesh.bsSlabGhost != null) {
        jvxlData.slabInfo = thisMesh.slabOptions.toString();
      }
      SB sb = new SB();
      getMeshCommand(sb, thisMesh.index);
      thisMesh.setJvxlColorMap(true);
      return JvxlCoder.jvxlGetFile(jvxlData, meshData, title, "", true, 1, sb.toString(), null);
    }
    if (property == "jvxlFileInfo") {
      return JvxlCoder.jvxlGetInfo(jvxlData);
    }
    if (property == "command") {
      SB sb = new SB();
      Lst<Mesh> list = getMeshList((index < 0 ? previousMeshID : thisMesh.thisID), false);
      for (int i = list.size(); --i >= 0;)
         getMeshCommand(sb, i);
      return sb.toString();
    }
    if (property == "atoms") {
      return thisMesh.surfaceAtoms;
    }    
    if (property == "colorEncoder")
      return thisMesh.colorEncoder;
    return null;
  }

  private float[] getDataRange(IsosurfaceMesh mesh) {
    return (mesh == null ? null : mesh.getDataRange());
  }

  private Object calculateVolumeOrArea(IsosurfaceMesh mesh, boolean isArea) {
    if (isArea) {
      if (mesh.calculatedArea != null)
        return mesh.calculatedArea;
    } else {
      if (mesh.calculatedVolume != null)
        return mesh.calculatedVolume;
    }
    MeshData meshData = new MeshData();
    fillMeshData(meshData, MeshData.MODE_GET_VERTICES, mesh);
    meshData.nSets = mesh.nSets;
    meshData.vertexSets = mesh.vertexSets;
    if (!isArea && mesh.jvxlData.colorDensity) {
      float f = mesh.jvxlData.voxelVolume;
      f *= (mesh.bsSlabDisplay == null ? mesh.vc : mesh.bsSlabDisplay.cardinality());
      return  mesh.calculatedVolume = Float.valueOf(f); 
    }
    Object ret = MeshData.calculateVolumeOrArea(meshData, mesh.jvxlData.thisSet, isArea, false);
    if (mesh.nSets <= 0)
      mesh.nSets = -meshData.nSets;
    if (isArea)
      mesh.calculatedArea = ret;
    else
      mesh.calculatedVolume = ret;
    return ret;
  }

  public static String getPolygonColorData(int ccount, short[] colixes, int[][] polygons, BS bsSlabDisplay) {
    boolean isExplicit = (polygons != null);
    if (colixes == null && polygons == null)
      return null;
    SB list1 = new SB();
    int count = 0;
    short colix = 0;
    int color = 0, colorNext = 0;
    boolean done = false;
    for (int i = 0; i < ccount || (done = true) == true; i++) {
      if (!done && bsSlabDisplay != null && !bsSlabDisplay.get(i))
        continue;
      if (done || (isExplicit ? (colorNext = polygons[i][MeshSurface.P_EXPLICIT_COLOR]) != color : colixes[i] != colix)) {
        if (count != 0)
          list1.append(" ").appendI(count).append(" ").appendI(
              (isExplicit ? color : colix == 0 ? 0 : C.getArgb(colix)));
        if (done)
          break;
        if (isExplicit)
          color = colorNext;
        else
          colix = colixes[i];
        count = 1;
      } else {
        count++;
      }
    }
    list1.append("\n");
    return list1.toString();
  }

  @Override
  public String getShapeState() {
    clean();
    SB sb = new SB();
    sb.append("\n");
    for (int i = 0; i < meshCount; i++)
      getMeshCommand(sb, i);
    return sb.toString();
  }

  private void getMeshCommand(SB sb, int i) {
    IsosurfaceMesh imesh = (IsosurfaceMesh) meshes[i];
    if (imesh == null || imesh.scriptCommand == null)
      return;
    String cmd = imesh.scriptCommand;
    int modelCount = vwr.ms.mc;
    if (modelCount > 1)
      appendCmd(sb, "frame " + vwr.getModelNumberDotted(imesh.modelIndex));
    cmd = PT.rep(cmd, ";; isosurface map"," map");
    cmd = PT.rep(cmd, "; isosurface map", " map");
    cmd = cmd.replace('\t', ' ');
    cmd = PT.rep(cmd, ";#", "; #");
    int pt = cmd.indexOf("; #");
    if (pt >= 0)
      cmd = cmd.substring(0, pt);
    if (imesh.connectedAtoms != null)
      cmd += " connect " + Escape.eAI(imesh.connectedAtoms);
    cmd = PT.trim(cmd, ";");
    if (imesh.linkedMesh != null)
      cmd += " LINK"; // for lcaoCartoon state
    if (myType == "lcaoCartoon" && imesh.atomIndex >= 0)
      cmd += " ATOMINDEX " + imesh.atomIndex;
    appendCmd(sb, cmd);
    String id = myType + " ID " + PT.esc(imesh.thisID);
    if (imesh.jvxlData.thisSet >= 0)
      appendCmd(sb, id + " set " + (imesh.jvxlData.thisSet + 1));
    if (imesh.mat4 != null && !imesh.isModelConnected)
      appendCmd(sb, id + " move " + Escape.matrixToScript(imesh.mat4));
    if (imesh.scale3d != 0)
      appendCmd(sb, id + " scale3d " + imesh.scale3d);
    if (imesh.jvxlData.slabValue != Integer.MIN_VALUE)
      appendCmd(sb, id + " slab " + imesh.jvxlData.slabValue);
    if (imesh.slabOptions != null)
      appendCmd(sb, imesh.slabOptions.toString());
    if (cmd.charAt(0) != '#') {
      if (allowMesh)
        appendCmd(sb, imesh.getState(myType));
      if (!imesh.isColorSolid && imesh.colorType == 0 && C.isColixTranslucent(imesh.colix))
        appendCmd(sb, "color " + myType + " " + getTranslucentLabel(imesh.colix));
      if (imesh.colorCommand != null && imesh.colorType == 0 && !imesh.colorCommand.equals("#inherit;")) {
        appendCmd(sb, imesh.colorCommand);
      }
      boolean colorArrayed = (imesh.isColorSolid && imesh.pcs != null);
      if (imesh.isColorSolid && imesh.colorType == 0 && !imesh.colorsExplicit && !colorArrayed) {
        appendCmd(sb, getColorCommandUnk(myType, imesh.colix, translucentAllowed));
      } else if (imesh.jvxlData.isBicolorMap && imesh.colorPhased) {
        appendCmd(sb, "color isosurface phase "
            + encodeColor(imesh.jvxlData.minColorIndex) + " "
            + encodeColor(imesh.jvxlData.maxColorIndex));
      }
      if (imesh.vertexColorMap != null)
        for (Map.Entry<String, BS> entry : imesh.vertexColorMap.entrySet()) {
          BS bs = entry.getValue();
          if (!bs.isEmpty())
            appendCmd(sb, "color " + myType + " " + Escape.eBS(bs)
                + " " + entry.getKey());
        }
    }
  }

  
  private String script;

  private boolean getScriptBitSets(String script, BS[] bsCmd) {
    this.script = script;
    int i;
    iHaveModelIndex = false;
    modelIndex = -1;
    if (script != null && (i = script.indexOf("MODEL({")) >= 0) {
      int j = script.indexOf("})", i);
      if (j > 0) {
        BS bs = BS.unescape(script.substring(i + 3, j + 1));
        modelIndex = (bs == null ? -1 : bs.nextSetBit(0));
        iHaveModelIndex = (modelIndex >= 0);
      }
    }    
    if (script == null)
      return false;
    getCapSlabInfo(script);
    i = script.indexOf("# ({");
    if (i < 0)
      return false;
    int j = script.indexOf("})", i);
    if (j < 0)
      return false;
    BS bs = BS.unescape(script.substring(i + 2, j + 2));
    if (bsCmd == null)
      sg.setProp("select", bs, null);
    else
      bsCmd[0] = bs;
    if ((i = script.indexOf("({", j)) < 0)
      return true;
    j = script.indexOf("})", i);
    if (j < 0) 
      return false;
      bs = BS.unescape(script.substring(i + 1, j + 1));
      if (bsCmd == null)
        sg.setProp("ignore", bs, null);
      else
        bsCmd[1] = bs;
    if ((i = script.indexOf("/({", j)) == j + 2) {
      if ((j = script.indexOf("})", i)) < 0)
        return false;
      bs = BS.unescape(script.substring(i + 3, j + 1));
      if (bsCmd == null)
        vwr.ms.setTrajectoryBs(bs);
      else
        bsCmd[2] = bs;
    }
    return true;
  }

  protected void getCapSlabInfo(String script) {
    int i = script.indexOf("# SLAB=");
    if (i >= 0)
      sg.setProp("slab", getCapSlabObject(PT.getQuotedStringAt(script, i), false), null);
    i = script.indexOf("# CAP=");
    if (i >= 0)
      sg.setProp("slab", getCapSlabObject(PT.getQuotedStringAt(script, i), true), null);
  }

  /**
   * legacy -- for some scripts with early isosurface slabbing
   * 
   * @param s
   * @param isCap
   * @return slabInfo object
   */
  private Object[] getCapSlabObject(String s, boolean isCap) {
    try {
      if (s.indexOf("array") == 0) {
        String[] pts = PT.split(s.substring(6, s.length() - 1), ",");
        return TempArray.getSlabObjectType(T.boundbox,
            new P3[] { (P3) Escape.uP(pts[0]), (P3) Escape.uP(pts[1]),
                (P3) Escape.uP(pts[2]), (P3) Escape.uP(pts[3]) }, isCap, null);
      }
      Object plane = Escape.uP(s);
      if (plane instanceof P4)
        return TempArray.getSlabObjectType(T.plane, plane, isCap, null);
    } catch (Exception e) {
      //
    }
    return null;
  }


  private boolean iHaveModelIndex;

  private void initializeIsosurface() {
    //System.out.println("isosurface initializing " + thisMesh);
    if (!iHaveModelIndex)
      modelIndex = vwr.am.cmi;
    atomIndex = -1;
    //allowContourLines = true; //but not for f(x,y) or plane, which use mesh
    bsDisplay = null;
    center = P3.new3(Float.NaN, 0, 0);
    colix = C.ORANGE;
    connections = null;
    cutoffRange = null;
    colorType = defaultColix = meshColix = 0;
    displayWithinPoints = null;
    explicitContours = false;
    isFixed = (modelIndex < 0);
    isPhaseColored = isColorExplicit = false;
    linkedMesh = null;
    if (modelIndex < 0)
      modelIndex = 0; 
    // but note that modelIndex = -1
    // is critical for surfaceGenerator. Setting this equal to 
    // 0 indicates only surfaces for model 0.
    scale3d = 0;
    title = null;
    translucentLevel = 0;
    withinPoints = null;
    initState();
  }

  private void initState() {
    associateNormals = true;
    sg.initState();
    //TODO   need to pass assocCutoff to sg
  }

  private void setMeshI() {
    thisMesh.visible = true;
    if ((thisMesh.atomIndex = atomIndex) >= 0)
      thisMesh.modelIndex = vwr.ms.at[atomIndex].mi;
    else if (isFixed)
      thisMesh.modelIndex = -1;
    else if (modelIndex >= 0)
      thisMesh.modelIndex = modelIndex;
    else
      thisMesh.modelIndex = vwr.am.cmi;
    thisMesh.scriptCommand = script;
    thisMesh.ptCenter.setT(center);
    thisMesh.scale3d = (thisMesh.jvxlData.jvxlPlane == null ? 0 : scale3d);
//    if (thisMesh.bsSlabDisplay != null)
//      thisMesh.jvxlData.vertexDataOnly = true;
//      thisMesh.bsSlabDisplay = thisMesh.jvxlData.bsSlabDisplay;
  }

  /*
   void checkFlags() {
   if (vwr.getTestFlag2())
   associateNormals = false;
   if (!logMessages)
   return;
   Logger.info("Isosurface using testflag2: no associative grouping = "
   + !associateNormals);
   Logger.info("IsosurfaceRenderer using testflag4: show vertex normals = "
   + vwr.getTestFlag4());
   Logger
   .info("For grid points, use: isosurface delete myiso gridpoints \"\"");
   }
   */

  protected void discardTempData(boolean discardAll) {
    if (!discardAll)
      return;
    title = null;
    if (thisMesh == null)
      return;
    thisMesh.surfaceSet = null;
  }

  ////////////////////////////////////////////////////////////////
  // default color stuff (deprecated in 11.2)
  ////////////////////////////////////////////////////////////////

  private short getDefaultColix() {
    if (defaultColix != 0)
      return defaultColix;
    if (!sg.jvxlData.wasCubic)
      return colix; // orange
    int argb = (sg.params.cutoff >= 0 ? JC.argbsIsosurfacePositive
        : JC.argbsIsosurfaceNegative);
    return C.getColix(argb);
  }

  ///////////////////////////////////////////////////
  ////  LCAO Cartoons  are sets of lobes ////

  private int nLCAO = 0;

  private void drawLcaoCartoon(V3 z, V3 x, V3 rotAxis, int nElectrons) {
    String lcaoCartoon = sg.setLcao();
    //really rotRadians is just one of these -- x, y, or z -- not all
    float rotRadians = rotAxis.x + rotAxis.y + rotAxis.z;
    defaultColix = C.getColix(sg.params.colorPos);
    short colixNeg = C.getColix(sg.params.colorNeg);
    V3 y = new V3();
    boolean isReverse = (lcaoCartoon.length() > 0 && lcaoCartoon.charAt(0) == '-');
    if (isReverse)
      lcaoCartoon = lcaoCartoon.substring(1);
    int sense = (isReverse ? -1 : 1);
    y.cross(z, x);
    if (rotRadians != 0) {
      A4 a = new A4();
      if (rotAxis.x != 0)
        a.setVA(x, rotRadians);
      else if (rotAxis.y != 0)
        a.setVA(y, rotRadians);
      else
        a.setVA(z, rotRadians);
      M3 m = new M3().setAA(a);
      m.rotate(x);
      m.rotate(y);
      m.rotate(z);
    }
    if (thisMesh == null && nLCAO == 0)
      nLCAO = meshCount;
    String id = (thisMesh == null ? (nElectrons > 0 ? "lp" : "lcao") + (++nLCAO) + "_" + lcaoCartoon
        : thisMesh.thisID);
    if (thisMesh == null)
      allocMesh(id, null);
    if (lcaoCartoon.equals("px")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(x, sense, nElectrons);
      if (nElectrons > 0) 
        return;
      setProperty("thisID", id + "b", null);
      createLcaoLobe(x, -sense, nElectrons);
      thisMesh.colix = colixNeg;
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("py")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(y, sense, nElectrons);
      if (nElectrons > 0) 
        return;
      setProperty("thisID", id + "b", null);
      createLcaoLobe(y, -sense, nElectrons);
      thisMesh.colix = colixNeg;
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("pz")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(z, sense, nElectrons);
      if (nElectrons > 0) 
        return;
      setProperty("thisID", id + "b", null);
      createLcaoLobe(z, -sense, nElectrons);
      thisMesh.colix = colixNeg;
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("pza") 
        || lcaoCartoon.indexOf("sp") == 0 
        || lcaoCartoon.indexOf("d") == 0 
        || lcaoCartoon.indexOf("lp") == 0) {
      createLcaoLobe(z, sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pzb")) {
      createLcaoLobe(z, -sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pxa")) {
      createLcaoLobe(x, sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pxb")) {
      createLcaoLobe(x, -sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pya")) {
      createLcaoLobe(y, sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pyb")) {
      createLcaoLobe(y, -sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("spacefill") || lcaoCartoon.equals("cpk")) {
      createLcaoLobe(null, 2 * vwr.ms.at[atomIndex].getRadius(), nElectrons);
      return;      
    }

    // assume s
    createLcaoLobe(null, 1, nElectrons);
    return;
  }

  private P4 lcaoDir = new P4();

  private void createLcaoLobe(V3 lobeAxis, float factor, int nElectrons) {
    initState();
    if (Logger.debugging) {
      Logger.debug("creating isosurface ID " + thisMesh.thisID);
    }
    if (lobeAxis == null) {
      setProperty("sphere", Float.valueOf(factor / 2f), null);
    } else {
      lcaoDir.x = lobeAxis.x * factor;
      lcaoDir.y = lobeAxis.y * factor;
      lcaoDir.z = lobeAxis.z * factor;
      lcaoDir.w = 0.7f;
      setProperty(nElectrons == 2 ? "lp" : nElectrons == 1 ? "rad" : "lobe", 
          lcaoDir, null);
    }
    thisMesh.colix = defaultColix;
    setScriptInfo(null);
  }

  /////////////// meshDataServer interface /////////////////

  @Override
  public void invalidateTriangles() {
    thisMesh.invalidatePolygons();
  }

  @Override
  public void setOutputChannel(GenericBinaryDocument binaryDoc, OC out) {
    binaryDoc.setOutputChannel(out);
  }

  @Override
  public void fillMeshData(MeshData meshData, int mode, IsosurfaceMesh mesh) {
    if (meshData == null) {
      if (thisMesh == null)
        allocMesh(null, null);
      if (!thisMesh.isMerged)
        thisMesh.clearType(myType, sg.params.iAddGridPoints);
      thisMesh.connectedAtoms = connections;
      thisMesh.colix = getDefaultColix();
      thisMesh.colorType = colorType;
      thisMesh.meshColix = meshColix;
      if (isPhaseColored || thisMesh.jvxlData.isBicolorMap)
        thisMesh.isColorSolid = false;
      return;
    }
    if (mesh == null)
      mesh = thisMesh;
    if (mesh == null)
      return;
    //System.out.println("isosurface _get " + mode + " " + MeshData.MODE_GET_VERTICES + " " + MeshData.MODE_PUT_VERTICES + " vc=" + mesh.vertexCount + " pc=" + mesh.polygonCount + " " + mesh +" " 
      //  + (mesh.bsSlabDisplay == null ? "" :      
        //" bscard=" + mesh.bsSlabDisplay.cardinality() +
        //" " + mesh.bsSlabDisplay.hashCode() + "  " + mesh.bsSlabDisplay));
    switch (mode) {
    case MeshData.MODE_GET_VERTICES:
      meshData.mergeVertexCount0 = mesh.mergeVertexCount0;
      meshData.vs = mesh.vs;
      meshData.vertexSource = mesh.vertexSource;
      meshData.vvs = mesh.vvs;
      meshData.vc = mesh.vc;
      meshData.vertexIncrement = mesh.vertexIncrement;
      meshData.pc = mesh.pc;
      meshData.pis = mesh.pis;
      meshData.pcs = mesh.pcs;
      meshData.bsSlabDisplay = mesh.bsSlabDisplay;
      meshData.bsSlabGhost = mesh.bsSlabGhost;
      meshData.slabColix = mesh.slabColix;
      meshData.slabMeshType = mesh.slabMeshType;
      meshData.polygonCount0 = mesh.polygonCount0;
      meshData.vertexCount0 = mesh.vertexCount0;
      meshData.slabOptions = mesh.slabOptions;
      meshData.colorsExplicit = mesh.colorsExplicit;
      return;
    case MeshData.MODE_GET_COLOR_INDEXES:
      if (mesh.vcs == null
          || mesh.vc > mesh.vcs.length)
        mesh.vcs = new short[mesh.vc];
      meshData.vcs = mesh.vcs;
      //meshData.polygonIndexes = null;
      return;
    case MeshData.MODE_PUT_SETS:
      mesh.surfaceSet = meshData.surfaceSet;
      mesh.vertexSets = meshData.vertexSets;
      mesh.nSets = meshData.nSets;
      return;
    case MeshData.MODE_PUT_VERTICES:
      mesh.vs = meshData.vs;
      mesh.vvs = meshData.vvs;
      mesh.vc = meshData.vc;
      mesh.vertexIncrement = meshData.vertexIncrement;
      mesh.vertexSource = meshData.vertexSource;
      mesh.pc = meshData.pc;
      mesh.pis = meshData.pis;
      mesh.pcs = meshData.pcs;
      mesh.bsSlabDisplay = meshData.bsSlabDisplay;
      mesh.bsSlabGhost = meshData.bsSlabGhost;
      mesh.slabColix = meshData.slabColix;
      mesh.slabMeshType = meshData.slabMeshType;
      mesh.polygonCount0 = meshData.polygonCount0;
      mesh.vertexCount0 = meshData.vertexCount0;
      mesh.mergeVertexCount0 = meshData.mergeVertexCount0;
      mesh.slabOptions = meshData.slabOptions;
      mesh.colorsExplicit = meshData.colorsExplicit;
      return;
    }
  }

  @Override
  public boolean notifySurfaceGenerationCompleted() {
    setMeshI();
    setBsVdw();
    thisMesh.surfaceAtoms = sg.params.bsSelected;
    thisMesh.insideOut = sg.params.isInsideOut();
    thisMesh.isModelConnected = sg.params.isModelConnected;
    thisMesh.vertexSource = sg.params.vertexSource;
    thisMesh.oabc = sg.getOriginVaVbVc();
    thisMesh.calculatedArea = null;
    thisMesh.calculatedVolume = null;
    // from JVXL file:
    if (!thisMesh.isMerged) {
      thisMesh.initialize(sg.params.isFullyLit() ? T.fullylit
        : T.frontlit, null, sg.params.thePlane);
      if (jvxlData.fixedLattice != null) {
        thisMesh.lattice = jvxlData.fixedLattice;
        thisMesh.fixLattice();
      }
      return thisMesh.setColorsFromJvxlData(sg.params.colorRgb);
    }
    if (!sg.params.allowVolumeRender)
      thisMesh.jvxlData.allowVolumeRender = false;
    thisMesh.setColorsFromJvxlData(sg.params.colorRgb);
    if (thisMesh.jvxlData.slabInfo != null)
      vwr.runScriptCautiously("isosurface " + thisMesh.jvxlData.slabInfo);
      
    if (sg.params.psi_monteCarloCount > 0)
      thisMesh.diameter = -1; // use set DOTSCALE
    return false;
    
  }

  @Override
  public void notifySurfaceMappingCompleted() {
    if (!thisMesh.isMerged)
      thisMesh.initialize(sg.params.isFullyLit() ? T.fullylit : T.frontlit, null,
          sg.params.thePlane);
    setBsVdw();
    thisMesh.isColorSolid = false;
    thisMesh.colorDensity = jvxlData.colorDensity;
    thisMesh.volumeRenderPointSize = jvxlData.pointSize;
    thisMesh.colorEncoder = sg.params.colorEncoder;
    thisMesh.getContours();
    if (thisMesh.jvxlData.nContours != 0 && thisMesh.jvxlData.nContours != -1)
      explicitContours = true;
    if (explicitContours && thisMesh.jvxlData.jvxlPlane != null)
      thisMesh.havePlanarContours = true;
    setPropertySuper("token",
        Integer.valueOf(explicitContours ? T.nofill : T.fill), null);
    setPropertySuper("token",
        Integer.valueOf(explicitContours ? T.contourlines : T.nocontourlines),
        null);
    if (!thisMesh.isMerged)
      thisMesh.setJvxlDataRendering();
    if (sg.params.slabInfo != null) {
      thisMesh.slabPolygonsList(sg.params.slabInfo, false);
      thisMesh.reinitializeLightingAndColor(vwr);
    }
    // may not be the final color scheme, though.
    thisMesh.setColorCommand();
  }

  private void setBsVdw() {
    if (sg.bsVdw == null)
      return;
    if (thisMesh.bsVdw == null)
      thisMesh.bsVdw = new BS();
    thisMesh.bsVdw.or(sg.bsVdw);
  }

  @Override
  public P3[] calculateGeodesicSurface(BS bsSelected,
                                            float envelopeRadius) {
    return vwr.calculateSurface(bsSelected, envelopeRadius);
  }

  /////////////  VertexDataServer interface methods ////////////////

  @Override
  public int getSurfacePointIndexAndFraction(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, P3i offset, int vA,
                                  int vB, float valueA, float valueB,
                                  T3 pointA, V3 edgeVector,
                                  boolean isContourType, float[] fReturn) {
    return 0;
  }

  private boolean associateNormals;
  private String oldFileName;
  private String newFileName;

  @Override
  public int addVertexCopy(T3 vertexXYZ, float value, int assocVertex, boolean asCopy) {
    if (cutoffRange != null && (value < cutoffRange[0] || value > cutoffRange[1]))
      return -1;
    return (withinPoints != null && !Mesh.checkWithin(vertexXYZ, withinPoints, withinDistance2, isWithinNot) ? -1
        : thisMesh.addVertexCopy(vertexXYZ, value, assocVertex,
        associateNormals, asCopy));
  }

  @Override
  public int addTriangleCheck(int iA, int iB, int iC, int check,
                              int iContour, boolean isAbsolute, int color) {
   return (iA < 0 || iB < 0 || iC < 0 
       || isAbsolute && !MeshData.checkCutoff(iA, iB, iC, thisMesh.vvs)
       ? -1 : thisMesh.addTriangleCheck(iA, iB, iC, check, iContour, color));
  }

  protected void setScriptInfo(String strCommand) {
    // also from lcaoCartoon
    String script = (strCommand == null ? sg.params.script : strCommand);
    int pt = (script == null ? -1 : script.indexOf("; isosurface map"));
    if (pt == 0) {
      // remapping surface
      if (thisMesh.scriptCommand == null)
        return;
      pt = thisMesh.scriptCommand.indexOf("; isosurface map"); 
      if (pt >= 0)
        thisMesh.scriptCommand = thisMesh.scriptCommand.substring(0, pt);
      thisMesh.scriptCommand += script;
      return;
    }
    thisMesh.title = sg.params.title;
    thisMesh.dataType = sg.params.dataType;
    thisMesh.scale3d = sg.params.scale3d;
    if (script != null) {
      if (oldFileName != null) {
        script = script.replace(oldFileName, newFileName);
      }
      if (script.charAt(0) == ' ') {
        script = myType + " ID " + PT.esc(thisMesh.thisID) + script;
        pt = script.indexOf("; isosurface map");
      }
    }    
    if (pt > 0 && scriptAppendix.length() > 0)
      thisMesh.scriptCommand = script.substring(0, pt) + scriptAppendix + script.substring(pt);
    else
      thisMesh.scriptCommand = script + scriptAppendix;
    if (!explicitID && script != null && (pt = script.indexOf("# ID=")) >= 0)
      thisMesh.thisID = PT.getQuotedStringAt(script, pt);
  }

  @Override
  public void addRequiredFile(String fileName) {
    fileName = " # /*file*/\"" + fileName + "\"";
    if (scriptAppendix.indexOf(fileName) < 0)
    scriptAppendix += fileName;
  }

  @Override
  public void setRequiredFile(String oldName, String fileName) {
    oldFileName = oldName;
    newFileName = fileName;
  }

  private void setJvxlInfo() {
    if (sg.jvxlData != jvxlData || sg.jvxlData != thisMesh.jvxlData)
      jvxlData = thisMesh.jvxlData = sg.jvxlData;
  }

  @Override
  public Object getShapeDetail() {
    Lst<Map<String, Object>> V = new  Lst<Map<String, Object>>();
    for (int i = 0; i < meshCount; i++) {
      Map<String, Object> info = new Hashtable<String, Object>();
      IsosurfaceMesh mesh = isomeshes[i];
      if (mesh == null || mesh.vs == null 
          || mesh.vc == 0 && mesh.pc == 0)
        continue;
      addMeshInfo(mesh, info);
      V.addLast(info);
    }
    return V;
  }

  protected void addMeshInfo(IsosurfaceMesh mesh, Map<String, Object> info) {
    info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
    info.put("visible", Boolean.valueOf(mesh.visible));
    info.put("vertexCount", Integer.valueOf(mesh.vc));
    if (mesh.calculatedVolume != null)
      info.put("volume", mesh.calculatedVolume);
    if (mesh.calculatedArea != null)
      info.put("area", mesh.calculatedArea);
    if (!Float.isNaN(mesh.ptCenter.x))
      info.put("center", mesh.ptCenter);
    if (mesh.mat4 != null)
      info.put("mat4", mesh.mat4);
    if (mesh.scale3d != 0)
      info.put("scale3d", Float.valueOf(mesh.scale3d));
    info.put("xyzMin", mesh.jvxlData.boundingBox[0]);
    info.put("xyzMax", mesh.jvxlData.boundingBox[1]);
    String s = JvxlCoder.jvxlGetInfo(mesh.jvxlData);
    if (s != null)
      info.put("jvxlInfo", s.replace('\n', ' '));
    info.put("modelIndex", Integer.valueOf(mesh.modelIndex));
    info.put("color", CU.colorPtFromInt(C
        .getArgb(mesh.colix), null));
    if (mesh.colorEncoder != null)
      info.put("colorKey", mesh.colorEncoder.getColorKey());
    if (mesh.title != null)
      info.put("title", mesh.title);
    if (mesh.jvxlData.contourValues != null
        || mesh.jvxlData.contourValuesUsed != null)
      info.put("contours", mesh.getContourList(vwr));
  }

  @Override
  public float[] getPlane(int x) {
    // only for surface readers
    return null;
  }
  
  @Override
  public float getValue(int x, int y, int z, int ptyz) {
    return 0;
  }
  
  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    if (keyXy != null && x >= keyXy[0] && y >= keyXy[1] && x < keyXy[2] && y < keyXy[3]) {
      hoverKey(x, y);
      return true;
    }
    if (!vwr.getDrawHover())
      return false;
    String s = findValue(x, y, false, bsVisible);
    if (s == null)
      return false;
    if (vwr.gdata.antialiasEnabled) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    vwr.hoverOnPt(x, y, s, pickedMesh.thisID, pickedPt);
    return true;
  }

  private void hoverKey(int x, int y) {
    try {
      String s;
      float f = 1 - 1.0f * (y - keyXy[1]) / (keyXy[3] - keyXy[1]);
      if (thisMesh.showContourLines) {
        Lst<Object>[] vContours = thisMesh.getContours();
        if (vContours == null) {
          if (thisMesh.jvxlData.contourValues == null)
            return;
          int i = (int) Math.floor(f * thisMesh.jvxlData.contourValues.length);
          if (i < 0 || i > thisMesh.jvxlData.contourValues.length)
            return;
          s = "" + thisMesh.jvxlData.contourValues[i];
        } else {
          int i = (int) Math.floor(f * vContours.length);
          if (i < 0 || i > vContours.length)
            return;
          s = ""
              + ((Float) vContours[i].get(JvxlCoder.CONTOUR_VALUE))
                  .floatValue();
        }
      } else {
        float g = thisMesh.colorEncoder.quantize(f, true);
        f = thisMesh.colorEncoder.quantize(f, false);
        s = "" + g + " - " + f;
      }
      if (vwr.gdata.isAntialiased()) {
        x <<= 1;
        y <<= 1;
      }
      vwr.hoverOnPt(x, y, s, null, null);
    } catch (Exception e) {
      // never mind!
    }
  }
  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;
  private final P3i ptXY = new P3i();
  public int[] keyXy;

  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int action, BS bsVisible, boolean drawPicking) {
    if (!drawPicking)// || vwr.getNavigationMode() && vwr.getNavigateSurface())) 
       return null;
    if (!vwr.isBound(action, ActionManager.ACTION_pickIsosurface))
      return null;
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (vwr.gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    int imesh = -1;
    int jmaxz = -1;
    int jminz = -1;
    int maxz = Integer.MIN_VALUE;
    int minz = Integer.MAX_VALUE;
    boolean pickFront = true;
    for (int i = 0; i < meshCount; i++) {
      IsosurfaceMesh m = isomeshes[i];
      if (!isPickable(m, bsVisible))
        continue;
      T3[] centers = (pickFront ? m.vs : m.getCenters());
      if (centers == null)
        continue;
      for (int j = centers.length; --j >= 0; ) {
          T3 v = centers[j];
          if (v == null)
            continue;
          int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
          if (d2 >= 0) {
            if (ptXY.z < minz) {
              if (pickFront)
                imesh = i;
              minz = ptXY.z;
              jminz = j;
            }
            if (ptXY.z > maxz) {
              if (!pickFront)
                imesh = i;
              maxz = ptXY.z;
              jmaxz = j;
            }
          }
      }
    }
    if (imesh < 0)
      return null;
    pickedMesh = isomeshes[imesh];
    setPropertySuper("thisID", pickedMesh.thisID, null);
    int iFace = pickedVertex = (pickFront ? jminz : jmaxz);
    P3 ptRet = new P3();
    ptRet.setT((pickFront ? pickedMesh.vs[pickedVertex] : ((IsosurfaceMesh)pickedMesh).centers[iFace]));
    pickedModel = (short) pickedMesh.modelIndex;
    Map<String, Object> map = getPickedPoint(ptRet, pickedModel);
//    if (pickFront) {
      setStatusPicked(-4, ptRet, map);
//    } else {
//      Vector3f vNorm = new Vector3f();
//      ((IsosurfaceMesh)pickedMesh).getFacePlane(iFace, vNorm);
//      // get normal to surface
//      vNorm.scale(-1);
//     // setHeading(ptRet, vNorm, 2);
//    }
    return map;
  }

  private boolean isPickable(IsosurfaceMesh m, BS bsVisible) {
    return m.visibilityFlags != 0 && (m.modelIndex < 0
        || bsVisible.get(m.modelIndex)) && !C
        .isColixTranslucent(m.colix);
  }

  //  private void navigate(int dz) {
  //    if (thisMesh == null)
  //      return;
  //    Point3f navPt = Point3f.newP(vwr.getNavigationOffset());
  //    Point3f toPt = new Point3f();
  //    vwr.unTransformPoint(navPt, toPt);
  //    navPt.z += dz;
  //    vwr.unTransformPoint(navPt, toPt);
  //    Point3f ptRet = new Point3f();
  //    Vector3f vNorm = new Vector3f();
  //    if (!getClosestNormal(thisMesh, toPt, ptRet, vNorm))
  //      return;
  //    Point3f pt2 = Point3f.newP(ptRet);
  //    pt2.add(vNorm);
  //    Point3f pt2s = new Point3f();
  //    vwr.tm.transformPt3f(pt2, pt2s);
  //    if (pt2s.y > navPt.y)
  //      vNorm.scale(-1);
  //    setHeading(ptRet, vNorm, 0);     
  //  }

  //  private void setHeading(Point3f pt, Vector3f vNorm, int nSeconds) {
  //    // general trick here is to save the original orientation, 
  //    // then do all the changes and save the new orientation.
  //    // Then just do a timed restore.
  //
  //    Orientation o1 = vwr.getOrientation();
  //    
  //    // move to point
  //    vwr.navigatePt(pt);
  //    
  //    Point3f toPts = new Point3f();
  //    
  //    // get screen point along normal
  //    Point3f toPt = Point3f.newP(vNorm);
  //    //vwr.script("draw test2 vector " + Escape.escape(pt) + " " + Escape.escape(toPt));
  //    toPt.add(pt);
  //    vwr.tm.transformPt3f(toPt, toPts);
  //    
  //    // subtract the navigation point to get a relative point
  //    // that we can project into the xy plane by setting z = 0
  //    Point3f navPt = Point3f.newP(vwr.getNavigationOffset());
  //    toPts.sub(navPt);
  //    toPts.z = 0;
  //    
  //    // set the directed angle and rotate normal into yz plane,
  //    // less 20 degrees for the normal upward sloping view
  //    float angle = Measure.computeTorsion(JmolConstants.axisNY, 
  //        JmolConstants.center, JmolConstants.axisZ, toPts, true);
  //    vwr.navigateAxis(JmolConstants.axisZ, angle);        
  //    toPt.setT(vNorm);
  //    toPt.add(pt);
  //    vwr.tm.transformPt3f(toPt, toPts);
  //    toPts.sub(navPt);
  //    angle = Measure.computeTorsion(JmolConstants.axisNY,
  //        JmolConstants.center, JmolConstants.axisX, toPts, true);
  //    vwr.navigateAxis(JmolConstants.axisX, 20 - angle);
  //    
  //    // save this orientation, restore the first, and then
  //    // use TransformManager.moveto to smoothly transition to it
  //    // a script is necessary here because otherwise the application
  //    // would hang.
  //    
  //    navPt = Point3f.newP(vwr.getNavigationOffset());
  //    if (nSeconds <= 0)
  //      return;
  //    vwr.saveOrientation("_navsurf");
  //    o1.restore(0, true);
  //    vwr.script("restore orientation _navsurf " + nSeconds);
  //  }

  //  private boolean getClosestNormal(IsosurfaceMesh m, Point3f toPt, Point3f ptRet, Vector3f normalRet) {
  //    Point3f[] centers = m.getCenters();
  //    float d;
  //    float dmin = Float.MAX_VALUE;
  //    int imin = -1;
  //    for (int i = centers.length; --i >= 0; ) {
  //      if ((d = centers[i].distance(toPt)) >= dmin)
  //        continue;
  //      dmin = d;
  //      imin = i;
  //    }
  //    if (imin < 0)
  //      return false;
  //    getClosestPoint(m, imin, toPt, ptRet, normalRet);
  //    return true;
  //  }

  //  private void getClosestPoint(IsosurfaceMesh m, int imin, Point3f toPt, Point3f ptRet,
  //                               Vector3f normalRet) {
  //    Point4f plane = m.getFacePlane(imin, normalRet);
  //    float dist = Measure.distanceToPlane(plane, toPt);
  //    normalRet.scale(-dist);
  //    ptRet.setT(toPt);
  //    ptRet.add(normalRet);
  //    dist = Measure.distanceToPlane(plane, ptRet);
  //    if (m.centers[imin].distance(toPt) < ptRet.distance(toPt))
  //      ptRet.setT(m.centers[imin]);
  //  }

  /**
   * 
   * @param x
   * @param y
   * @param isPicking
   *        IGNORED
   * @param bsVisible
   * @return value found
   */
  private String findValue(int x, int y, boolean isPicking, BS bsVisible) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (vwr.gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    int pickedVertex = -1;
    Lst<Object> pickedContour = null;
    IsosurfaceMesh m = null;
    for (int i = 0; i < meshCount; i++) {
      m = isomeshes[i];
      if (!isPickable(m, bsVisible))
        continue;
      Lst<Object>[] vs = m.jvxlData.vContours;
      int ilast = (m.firstRealVertex < 0 ? 0 : m.firstRealVertex);
      int pickedJ = 0;
      if (vs != null && vs.length > 0) {
        for (int j = 0; j < vs.length; j++) {
          Lst<Object> vc = vs[j];
          int n = vc.size() - 1;
          for (int k = JvxlCoder.CONTOUR_POINTS; k < n; k++) {
            T3 v = (T3) vc.get(k);
            int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
            if (d2 >= 0) {
              dmin2 = d2;
              pickedContour = vc;
              pickedJ = j;
              pickedMesh = m;
              pickedPt = v;
            }
          }
        }
        if (pickedContour != null)
          return pickedContour.get(JvxlCoder.CONTOUR_VALUE).toString()
              + (Logger.debugging ? " " + pickedJ : "");
      } else if (m.jvxlData.jvxlPlane != null && m.vvs != null) {
        T3[] vertices = (m.mat4 == null && m.scale3d == 0 ? m.vs : m
            .getOffsetVertices(m.jvxlData.jvxlPlane));
        for (int k = m.vc; --k >= ilast;) {
          T3 v = vertices[k];
          int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
          if (d2 >= 0) {
            dmin2 = d2;
            pickedVertex = k;
            pickedMesh = m;
            pickedPt = v;
          }
        }
        if (pickedVertex != -1)
          break;
      } else if (m.vvs != null) {
        if (m.bsSlabDisplay != null) {
          for (int k = m.bsSlabDisplay.nextSetBit(0); k >= 0; k = m.bsSlabDisplay
              .nextSetBit(k + 1)) {
            int[] p = m.pis[k];
            if (p != null)
              for (int l = 0; l < 3; l++) {
                T3 v = m.vs[p[l]];
                int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
                if (d2 >= 0) {
                  dmin2 = d2;
                  pickedVertex = p[l];
                  pickedMesh = m;
                  pickedPt = v;
                }
              }
          }
        } else {
          for (int k = m.vc; --k >= ilast;) {
            T3 v = m.vs[k];
            int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
            if (d2 >= 0) {
              dmin2 = d2;
              pickedVertex = k;
              pickedMesh = m;
              pickedPt = v;
            }
          }
        }
        if (pickedVertex != -1)
          break;
      }
    }
    return (pickedVertex == -1 ? null : (Logger.debugging ? "$" + m.thisID
        + "[" + (pickedVertex + 1) + "] " + m.vs[pickedVertex] + ": "
        : m.thisID + ": ")
        + m.vvs[pickedVertex]);
  }

  public String getCmd(int index){
    SB sb = new SB().append("\n");
//    result = this.isomeshes[index].scriptCommand;
    getMeshCommand(sb, index);
    return (sb.toString());
  }
}
