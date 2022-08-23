package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import javajs.util.BS;
import org.jmol.script.T;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.SB;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.V3d;
import javajs.util.T3d;

public class MeshSlicer {

  public MeshSlicer() {
    // for reflection
  }
  
  protected MeshSurface m;
  MeshSlicer set(MeshSurface meshSurface) {
    m = meshSurface;
    values = new double[2];
    fracs = new double[2];
    sources = new int[3];
    return this;
  }

  private boolean doCap;
  private boolean doClear;
  private boolean doGhost;
  private int iD, iE;
  private int[] sources;
  private P3d[] pts;
  V3d norm;
  private double dPlane;  
  double[] values;
  double[] fracs;
  private MeshCapper capper;
  private double wPlane;
  
  
  /**
   * 
   * @param slabObject 
   *    [0] Integer type, 
   *    [1] object, 
   *    [2] andCap,
   *    [3] colorData 
   * @param allowCap
   * @return true if successful
   */
  public boolean slabPolygons(Object[] slabObject, boolean allowCap) {
    if (m.polygonCount0 < 0)
      return false; // disabled for some surface types
    MeshSurface m = this.m;
    int slabType = ((Integer) slabObject[0]).intValue();
    if (slabType == T.none || slabType == T.brillouin) {
      if (m.bsSlabDisplay != null && (m.polygonCount0 != 0 || m.vertexCount0 != 0)) {
        m.pc = m.polygonCount0;
        m.vc = m.vertexCount0;
        m.polygonCount0 = m.vertexCount0 = 0;
        m.normixCount = (m.isDrawPolygon ? m.pc : m.vc);
        m.bsSlabDisplay.setBits(0, (m.pc == 0 ? m.vc : m.pc));
        m.slabOptions = new SB().append(m.meshType + " slab none");
        m.bsSlabGhost = null;
        m.slabMeshType = T.none;
      }
      if (slabType == T.none) {
        
        return false;
      }
    }
    Object slabbingObject = slabObject[1];
    boolean andCap = ((Boolean) slabObject[2]).booleanValue()
        && !(slabType == T.brillouin);
    if (andCap && !allowCap)
      return false;
    Object[] colorData = (Object[]) slabObject[3];
    boolean isGhost = (colorData != null);
    if (m.bsSlabDisplay == null || m.polygonCount0 == 0 && m.vertexCount0 == 0) {
      m.polygonCount0 = m.pc;
      m.vertexCount0 = m.vc;
      m.bsSlabDisplay = BSUtil.setAll(m.pc == 0 ? m.vc : m.pc);
      m.bsSlabGhost = null;
      if (m.pc == 0 && m.vc == 0)
        return false;
    } else if (m.isMerged) {
      if (m.pc == 0)
        m.bsSlabDisplay.setBits(m.mergeVertexCount0, m.vc);
      else
        m.bsSlabDisplay.setBits(m.mergePolygonCount0, m.pc);
    }

    if (isGhost) {
      if (m.bsSlabGhost == null)
        m.bsSlabGhost = new BS();
      m.slabMeshType = ((Integer) colorData[0]).intValue();
      m.slabColix = ((Short) colorData[1]).shortValue();
      //if (C.isColixColorInherited(slabColix))
      //slabColix = C.copyColixTranslucency(slabColix, colix);
      andCap = false;
      m.colix = C.getColixTranslucent3(m.colix, false, 0);
    }

    SB sb = new SB();
    sb.append(andCap ? " cap " : " slab ");
    if (isGhost) {
      sb.append(C.getColixTranslucencyLabel(m.slabColix)).append(" ");
      String s = C.getHexCode(m.slabColix);
      if (s != null)
        sb.append(s).append(" ");
      if (m.slabMeshType == T.mesh)
        sb.append("mesh ");
    }
    switch (slabType) {
    case T.brillouin:
      sb.append("brillouin");
      slabBrillouin((P3d[]) slabbingObject);
      break;
    case T.decimal:
      getIntersection(0, null, null, null, null, (BS) slabbingObject, null,
          andCap, false, T.decimal, isGhost);
      break;
    case T.plane:
      P4d plane = (P4d) slabbingObject;
      sb.append(Escape.eP4(plane));
      getIntersection(0, plane, null, null, null, null, null, andCap, false,
          T.plane, isGhost);
      break;
    case T.unitcell:
    case T.boundbox:
      P3d[] box = (P3d[]) slabbingObject;
      sb.append("within ").append(Escape.eAP(box));
      P4d[] faces = getBoxFacesFromOABC(box);
      for (int i = 0; i < faces.length; i++) {
        getIntersection(0, faces[i], null, null, null, null, null, andCap,
            false, T.plane, isGhost);
      }
      break;
    case T.data:
      getIntersection(0, null, null, null, (double[]) slabbingObject, null,
          null, false, false, T.min, isGhost);
      break;
    case T.within:
    case T.range:
    case T.mesh:
      Object[] o = (Object[]) slabbingObject;
      double distance = ((Number) o[0]).doubleValue();
      switch (slabType) {
      case T.within:
        P3d[] points = (P3d[]) o[1];
        BS bs = (BS) o[2];
        sb.append("within ").appendD(distance)
            .append(bs == null ? Escape.e(points) : Escape.e(bs));
        getIntersection(distance, null, points, null, null, null, null, andCap,
            false, T.distance, isGhost);
        break;
      case T.range:
        // isosurface slab within range x.x y.y
        // if y.y < x.x then this effectively means "NOT within range y.y x.x"
        if (m.vvs == null)
          return false;
        double distanceMax = ((Number) o[1]).doubleValue();
        sb.append("within range ").appendD(distance).append(" ")
            .appendD(distanceMax);
        bs = (distanceMax < distance ? BSUtil.copy(m.bsSlabDisplay) : null);
        getIntersection(distance, null, null, null, null, null, null, andCap,
            false, T.min, isGhost);
        BS bsA = (bs == null ? null : BSUtil.copy(m.bsSlabDisplay));
        BSUtil.copy2(bs, m.bsSlabDisplay);
        getIntersection(distanceMax, null, null, null, null, null, null,
            andCap, false, T.max, isGhost);
        if (bsA != null)
          m.bsSlabDisplay.or(bsA);
        break;
      case T.mesh:
        //NOT IMPLEMENTED
        MeshSurface mesh = (MeshSurface) o[1];
        //distance = -1;
        getIntersection(0, null, null, null, null, null, mesh, andCap, false,
            distance < 0 ? T.min : T.max, isGhost);
        //TODO: unresolved how exactly to store this in the state
        // -- must indicate exact set of triangles to slab and how!
        break;
      }
      break;
    }
    String newOptions = sb.toString();
    if (m.slabOptions == null)
      m.slabOptions = new SB();
    if (m.slabOptions.indexOf(newOptions) < 0)
      m.slabOptions.append(m.slabOptions.length() > 0 ? "; " : "").append(m.meshType)
          .append(newOptions);
    return true;
  }

  private P4d[] getBoxFacesFromOABC(P3d[] oabc) {
      P4d[] faces = new P4d[6];
      V3d vNorm = new V3d();
      V3d vAB = new V3d();
      P3d pta = new P3d();
      P3d ptb = new P3d();
      P3d ptc = new P3d();
      P3d[] vertices = BoxInfo.getVerticesFromOABC(oabc);
      for (int i = 0; i < 6; i++) {
        pta.setT(vertices[BoxInfo.facePoints[i][0]]);
        ptb.setT(vertices[BoxInfo.facePoints[i][1]]);
        ptc.setT(vertices[BoxInfo.facePoints[i][2]]);
        faces[i] = MeasureD.getPlaneThroughPoints(pta, ptb, ptc, vNorm, vAB, new P4d());
      }
      return faces;
    }

  /**
   * @param distance
   *        a distance from a plane or point
   * @param plane
   *        a slabbing plane
   * @param ptCenters
   *        a set of atoms to measure distance from
   * @param vData
   *        when not null, this is a query, not an actual slabbing
   * @param fData
   *        vertex values or other data to overlay
   * @param bsSource
   *        TODO
   * @param meshSurface
   *        second surface; not implemented -- still some problems there
   * @param andCap
   *        to cap this off, crudely only
   * @param doClean
   *        compact set - draw only
   * @param tokType
   *        type of slab
   * @param isGhost
   *        translucent slab, so we mark slabbed triangles
   */
  public void getIntersection(double distance, P4d plane, P3d[] ptCenters,
                              Lst<P3d[]> vData, double[] fData, BS bsSource,
                              MeshSurface meshSurface, boolean andCap,
                              boolean doClean, int tokType, boolean isGhost) {
    MeshSurface m = this.m;
    boolean isSlab = (vData == null);
    P3d[] p = null;
    pts = ptCenters;
    if (plane != null) {
      norm = V3d.newV(plane);
      dPlane = (double) Math.sqrt(norm.dot(norm));
      wPlane = plane.w;
      if (dPlane == 0) {
        norm.z = dPlane = 1;
        wPlane = 0;
      }
    }

    if (fData == null) {
      if (tokType == T.decimal && bsSource != null) {
        if (m.vertexSource == null)
          return;
        fData = new double[m.vc];
        for (int i = 0; i < m.vc; i++) {
          fData[i] = m.vertexSource[i];
          if (fData[i] == -1)
            System.out.println("meshsurface hmm");
        }
      } else {
        fData = m.vvs;
      }
    }
    if (m.pc == 0) {
      for (int i = m.mergeVertexCount0; i < m.vc; i++) {
        if (Double.isNaN(fData[i])
            || checkSlab(tokType, m.vs[i], fData[i], distance, bsSource) > 0)
          m.bsSlabDisplay.clear(i);
      }
      return;
    }
    if (ptCenters != null || isGhost)
      andCap = false; // can only cap faces, and no capping of ghosts
    if (andCap && capper == null)
      capper = ((MeshCapper) Interface.getInterface("org.jmol.util.MeshCapper",
          m.vwr, "script")).set(this);
    if (capper != null)
      capper.clear();
    double absD = Math.abs(distance);
    Map<String, Integer> mapEdge = new Hashtable<String, Integer>();
    BS bsD = BS.newN(m.vc);
    double[] d = new double[m.vc];
    double d1 = 0, d2 = 0, d3 = 0, valA, valB, valC;
    for (int i = m.mergePolygonCount0, iLast = m.pc; i < iLast; i++) {
      int[] face = m.setABC(i);
      if (face == null)
        continue;
      BS bsSlab = (m.bsSlabGhost != null && m.bsSlabGhost.get(i) ? m.bsSlabGhost
          : m.bsSlabDisplay);
      int check1 = face[MeshSurface.P_CHECK];
      int iContour = (m.dataOnly ? 0 : face[MeshSurface.P_CONTOUR]);
      int ia = m.iA;
      int ib = m.iB;
      int ic = m.iC;
      T3d vA = m.vs[ia];
      T3d vB = m.vs[ib];
      T3d vC = m.vs[ic];
      valA = fData[ia];
      valB = fData[ib];
      valC = fData[ic];
      if (m.vertexSource != null) {
        sources[0] = m.vertexSource[ia];
        sources[1] = m.vertexSource[ib];
        sources[2] = m.vertexSource[ic];
      }
      if (!bsD.get(ia)) {
        bsD.set(ia);
        d[ia] = checkSlab(tokType, vA, valA, (bsSource == null ? distance
            : sources[0]), bsSource);
      }
      if (!bsD.get(ib)) {
        bsD.set(ib);
        d[ib] = checkSlab(tokType, vB, valB, (bsSource == null ? distance
            : sources[1]), bsSource);
      }
      if (!bsD.get(ic)) {
        bsD.set(ic);
        d[ic] = checkSlab(tokType, vC, valC, (bsSource == null ? distance
            : sources[2]), bsSource);
      }
      d1 = d[ia];
      d2 = d[ib];
      d3 = d[ic];
      int test1 = (d1 != 0 && d1 < 0 ? 1 : 0) + (d2 != 0 && d2 < 0 ? 2 : 0)
          + (d3 != 0 && d3 < 0 ? 4 : 0);
      int thisSet = (m.vertexSets == null ? 0 : m.vertexSets[ia]);

      /*      
            if (iA == 955 || iB == 955 || iC == 955) {
              System.out.println(i + " " + iA + " " + iB + " " + iC + " "+ d1 + " " + d2 + " " + d3 + " " + test1);
              System.out.println("testing messhsurf ");
            }
      */
      /*      
      if (isMeshIntersect && test1 != 7 && test1 != 0) {
        // NOT IMPLEMENTED
        boolean isOK = (d1 == 0 && d2 * d3 >= 0 || d2 == 0 && (d1 * d3) >= 0 || d3 == 0
            && d1 * d2 >= 0);
        if (isOK)
          continue;
        // We have a potential crossing. Now to find the exact point of crossing
        // the other isosurface.
        if (checkIntersection(vA, vB, vC, meshSurface, pts2, vNorm, vBC, vAC,
            plane, planeTemp, vTemp3)) {
          iD = addIntersectionVertex(pts2[0], 0, sources[0], mapEdge, -1, -1); // have to choose some source
          addPolygon(iA, iB, iD, check1 & 1, iContour, 0, bsSlabDisplay);
          addPolygon(iD, iB, iC, check1 & 2, iContour, 0, bsSlabDisplay);
          addPolygon(iA, iD, iC, check1 & 4, iContour, 0, bsSlabDisplay);
          test1 = 0; // toss original    
          iLast = polygonCount;
        } else {
          // process normally for now  
          // not fully implemented -- need to check other way as well.
        }
      }
      */
      switch (test1) {
      default:
      case 7:
      case 0:
        // all on the same side
        break;
      case 1:
      case 6:
        // BC on same side
        if (ptCenters == null)
          p = new P3d[] { interpolatePoint(vA, vB, -d1, d2, valA, valB, 0),
              interpolatePoint(vA, vC, -d1, d3, valA, valC, 1) };
        else
          p = new P3d[] {
              interpolateSphere(vA, vB, -d1, -d2, absD, valA, valB, 0),
              interpolateSphere(vA, vC, -d1, -d3, absD, valA, valC, 1) };
        break;
      case 2:
      case 5:
        //AC on same side
        if (ptCenters == null)
          p = new P3d[] { interpolatePoint(vB, vA, -d2, d1, valB, valA, 1),
              interpolatePoint(vB, vC, -d2, d3, valB, valC, 0) };
        else
          p = new P3d[] {
              interpolateSphere(vB, vA, -d2, -d1, absD, valB, valA, 1),
              interpolateSphere(vB, vC, -d2, -d3, absD, valB, valC, 0) };
        break;
      case 3:
      case 4:
        //AB on same side need A-C, B-C
        if (ptCenters == null)
          p = new P3d[] { interpolatePoint(vC, vA, -d3, d1, valC, valA, 0),
              interpolatePoint(vC, vB, -d3, d2, valC, valB, 1) };
        else
          p = new P3d[] {
              interpolateSphere(vC, vA, -d3, -d1, absD, valC, valA, 0),
              interpolateSphere(vC, vB, -d3, -d2, absD, valC, valB, 1) };
        break;
      }
      doClear = true;
      doGhost = isGhost;
      doCap = andCap;
      BS bs;
      // adjust for minor discrepencies 
      //for (int j = 0; j < 2; j++) 
      //if (fracs[j] == 0)
      //fracs[1 - j] = (fracs[1 - j] < 0.5 ? 0 : 1);

      if (isSlab) {
        //        iD = iE = -1;
        switch (test1) {
        //             A
        //            / \
        //           B---C
        case 0:
          // all on the same side -- toss
          doCap = false;
          break;
        case 7:
          // all on the same side -- keep
          continue;
        case 1:
        case 6:
          //          0  A  0
          //            / \
          //        0 -------- 1 
          //          /     \
          //       1 B-------C  1
          boolean tossBC = (test1 == 1);
          if (tossBC || isGhost) {
            // 1: BC on side to toss -- +tossBC+isGhost  -tossBC+isGhost
            if (!getDE(fracs, 0, ia, ib, ic, tossBC))
              break;
            if (iD < 0)
              iD = addIntersectionVertex(p[0], values[0], sources[0], thisSet,
                  mapEdge, ia, ib);
            if (iE < 0)
              iE = addIntersectionVertex(p[1], values[1], sources[0], thisSet,
                  mapEdge, ia, ic);
            bs = (tossBC ? bsSlab : m.bsSlabGhost);
            m.addPolygonV3(ia, iD, iE, check1 & 5 | 2, iContour, 0, bs);
            if (!isGhost)
              break;
          }
          // BC on side to keep -- -tossBC+isGhost,  +tossBC+isGhost
          if (!getDE(fracs, 1, ia, ic, ib, tossBC))
            break;
          bs = (tossBC ? m.bsSlabGhost : bsSlab);
          if (iE < 0) {
            iE = addIntersectionVertex(p[0], values[0], sources[1], thisSet,
                mapEdge, ia, ib);
            m.addPolygonV3(iE, ib, ic, check1 & 3, iContour, 0, bs);
          }
          if (iD < 0) {
            iD = addIntersectionVertex(p[1], values[1], sources[2], thisSet,
                mapEdge, ia, ic);
            m.addPolygonV3(iD, iE, ic, check1 & 4 | 1, iContour, 0, bs);
          }
          break;
        case 5:
        case 2:
          //              A
          //            \/ \
          //            /\  \
          //           B--\--C
          //               \
          //
          boolean tossAC = (test1 == 2);
          if (tossAC || isGhost) {
            //AC on side to toss
            if (!getDE(fracs, 0, ib, ic, ia, tossAC))
              break;
            bs = (tossAC ? bsSlab : m.bsSlabGhost);
            if (iE < 0)
              iE = addIntersectionVertex(p[0], values[0], sources[1], thisSet,
                  mapEdge, ib, ia);
            if (iD < 0)
              iD = addIntersectionVertex(p[1], values[1], sources[1], thisSet,
                  mapEdge, ib, ic);
            m.addPolygonV3(iE, ib, iD, check1 & 3 | 4, iContour, 0, bs);
            if (!isGhost)
              break;
          }
          // AC on side to keep
          if (!getDE(fracs, 1, ib, ia, ic, tossAC))
            break;
          bs = (tossAC ? m.bsSlabGhost : bsSlab);
          if (iD < 0) {
            iD = addIntersectionVertex(p[0], values[0], sources[0], thisSet,
                mapEdge, ib, ia);
            m.addPolygonV3(ia, iD, ic, check1 & 5, iContour, 0, bs);
          }
          if (iE < 0) {
            iE = addIntersectionVertex(p[1], values[1], sources[2], thisSet,
                mapEdge, ib, ic);
            m.addPolygonV3(iD, iE, ic, check1 & 2 | 1, iContour, 0, bs);
          }
          break;
        case 4:
        case 3:
          //              A
          //             / \/
          //            /  /\
          //           B--/--C
          //             /
          //
          boolean tossAB = (test1 == 4);
          if (tossAB || isGhost) {
            if (!getDE(fracs, 0, ic, ia, ib, tossAB))
              break;
            if (iD < 0)
              iD = addIntersectionVertex(p[0], values[0], sources[2], thisSet,
                  mapEdge, ia, ic); //CA
            if (iE < 0)
              iE = addIntersectionVertex(p[1], values[1], sources[2], thisSet,
                  mapEdge, ib, ic); //CB
            bs = (tossAB ? bsSlab : m.bsSlabGhost);
            m.addPolygonV3(iD, iE, ic, check1 & 6 | 1, iContour, 0, bs);
            if (!isGhost)
              break;
          }
          //AB on side to keep
          if (!getDE(fracs, 1, ic, ib, ia, tossAB))
            break;
          bs = (tossAB ? m.bsSlabGhost : bsSlab);
          if (iE < 0) {
            iE = addIntersectionVertex(p[0], values[0], sources[0], thisSet,
                mapEdge, ia, ic); //CA
            m.addPolygonV3(ia, ib, iE, check1 & 5, iContour, 0, bs);
          }
          if (iD < 0) {
            iD = addIntersectionVertex(p[1], values[1], sources[1], thisSet,
                mapEdge, ib, ic); //CB
            m.addPolygonV3(iE, ib, iD, check1 & 2 | 4, iContour, 0, bs);
          }
          break;
        }
        if (doClear) {
          bsSlab.clear(i);
          if (doGhost)
            m.bsSlabGhost.set(i);
        }
        // [v0, v1, triangle, edge, direction]
        if (doCap)
          capper.addEdge(iE, iD, thisSet);
      } else if (p != null) {
        vData.addLast(p);
      }
    }
    if (andCap)
      capper.createCap(norm);
    if (!doClean)
      return;
    BS bsv = new BS();
    BS bsp = new BS();
    for (int i = 0; i < m.pc; i++) {
      if (m.pis[i] == null)
        continue;
      bsp.set(i);
      for (int j = 0; j < 3; j++)
        bsv.set(m.pis[i][j]);
    }
    int n = 0;
    int nPoly = bsp.cardinality();
    if (nPoly != m.pc) {
      int[] map = new int[m.vc];
      for (int i = 0; i < m.vc; i++)
        if (bsv.get(i))
          map[i] = n++;
      T3d[] vTemp = new P3d[n];
      n = 0;
      for (int i = 0; i < m.vc; i++)
        if (bsv.get(i))
          vTemp[n++] = m.vs[i];
      int[][] pTemp = AU.newInt2(nPoly);
      nPoly = 0;
      for (int i = 0; i < m.pc; i++)
        if (m.pis[i] != null) {
          for (int j = 0; j < 3; j++)
            m.pis[i][j] = map[m.pis[i][j]];
          pTemp[nPoly++] = m.pis[i];
        }
      m.vs = vTemp;
      m.vc = n;
      m.pis = pTemp;
      m.pc = nPoly;
    }
  }

  private boolean getDE(double[] fracs, int fD, int i1, int i2, int i3,
                        boolean toss23) {

    //          0 (1) 0
    //            / \
    //     iD 0 -fracs- 1 iE 
    //          /     \
    //      1 (2)-------(3) 1
    iD = setPoint(fracs, fD, i1, i2);
    iE = setPoint(fracs, 1 - fD, i1, i3);

    // initially: doClear=true, doCap = andCap, doGhost = isGhost

    if (iD == i1 && iE == i1) {
      // toss all if tossing 23, otherwise ignore
      doClear = toss23;
      doCap = false;
      return false;
    }
    if (iD == i2 && iE == i3) {
      // cap but don't toss if tossing 23
      doClear = !toss23;
      return !doClear; // was FALSE, but we need to recreate the edge
    }
    if (iD == i1 || iE == i1) {
      // other is i2 or i3 -- along an edge
      // cap but toss all if tossing 23
      doClear = toss23;
      if (iD < 0) {
        iD = (toss23 ? i2 : i3);
      } else if (iE < 0) {
        iE = (toss23 ? i3 : i2);
      }
      return doCap;
    }

    doGhost = false;
    return true;
  }

  private static int setPoint(double[] fracs, int i, int i0, int i1) {
    return (fracs[i] == 0 ? i0 : fracs[i] == 1 ? i1 : -1);
  }

  private double checkSlab(int tokType, T3d v, double val, double distance,
                                 BS bs) {
    double d;
    switch (tokType) {
    case T.decimal:
      return (val >= 0 && bs.get((int) val) ? 1 : -1);
    case T.min:
      d = distance - val;
      break;
    case T.max:
      d = val - distance;
      break;
    case T.plane:
      d = (v.dot(norm) + wPlane) / dPlane;
      break;
    //case T.sphere:
    //case T.distance:
    default:
      double dmin = Integer.MAX_VALUE;
      for (int i = pts.length; --i >= 0;) {
        d = pts[i].distance(v);
        if (d < dmin)
          dmin = d;
      }
      d = (distance > 0 ? dmin - distance : -distance - dmin);
      break;
    }
    return (Math.abs(d) < 0.0001f ? 0 : d);
  }

  /*
    private static boolean checkIntersection(Point3f vA, Point3f vB, Point3f vC,
                                      MeshSurface meshSurface, Point3f[] pts, 
                                      Vector3f vNorm, Vector3f vAB, Vector3f vAC, Point4f plane, Point4f pTemp, Vector3f vTemp3) {
      
      Measure.getPlaneThroughPoints(vA, vB, vC, vNorm, vAB, vAC, plane);
      for (int i = 0; i < meshSurface.polygonCount; i++) {
        Point3f pt = meshSurface.getTriangleIntersection(i, vA, vB, vC, plane, vNorm, vAB, pts[1], pts[2], vAC, pTemp, vTemp3);
        if (pt != null) {
          pts[0] = Point3f.new3(pt);
          return true; 
        }
      }
      return false;
    }
    
    private Point3f getTriangleIntersection(int i, Point3f vA, Point3f vB, Point3f vC, Point4f plane, Vector3f vNorm, Vector3f vTemp, 
                                            Point3f ptRet, Point3f ptTemp, Vector3f vTemp2, Point4f pTemp, Vector3f vTemp3) {
      return (setABC(i) ? Measure.getTriangleIntersection(vA, vB, vC, plane, vertices[iA], vertices[iB], vertices[iC], vNorm, vTemp, ptRet, ptTemp, vTemp2, pTemp, vTemp3) : null);
    }
  */

  private P3d interpolateSphere(T3d v1, T3d v2, double d1, double d2, double absD,
                               double val1, double val2, int i) {
    return interpolateFraction(v1, v2,
        MeshSurface.getSphericalInterpolationFraction(absD, d1, d2, v1.distance(v2)), val1,
        val2, i);
  }

  private P3d interpolatePoint(T3d v1, T3d v2, double d1, double d2,
                                     double val1, double val2, int i) {
    return interpolateFraction(v1, v2, d1 / (d1 + d2), val1, val2, i);
  }

  private P3d interpolateFraction(T3d v1, T3d v2, double f, double val1,
                                        double val2, int i) {
    if (f < 0.0001)
      f = 0;
    else if (f > 0.9999)
      f = 1;
    fracs[i] = f;
    values[i] = (val2 - val1) * f + val1;
    return P3d.new3(v1.x + (v2.x - v1.x) * f, v1.y + (v2.y - v1.y) * f, v1.z
        + (v2.z - v1.z) * f);
  }

  /**
   * "slabs" an isosurface into the first Brillouin zone moving points as
   * necessary.
   * 
   * @param unitCellPoints
   * 
   */
  protected void slabBrillouin(P3d[] unitCellPoints) {
    MeshSurface m = this.m;
    T3d[] vectors = (unitCellPoints == null ? m.oabc : unitCellPoints);
    if (vectors == null)
      return;

    // define 26 k-points around the origin

    P3d[] pts = new P3d[27];
    pts[0] = P3d.newP(vectors[0]);
    int pt = 0;
    for (int i = -1; i <= 1; i++)
      for (int j = -1; j <= 1; j++)
        for (int k = -1; k <= 1; k++)
          if (i != 0 || j != 0 || k != 0) {
            pts[++pt] = P3d.newP(pts[0]);
            pts[pt].scaleAdd2(i, vectors[1], pts[pt]);
            pts[pt].scaleAdd2(j, vectors[2], pts[pt]);
            pts[pt].scaleAdd2(k, vectors[3], pts[pt]);
          }

//System.out.println("draw line1 {0 0 0} color red"
//        + Escape.eP(m.spanningVectors[1]));
//System.out.println("draw line2 {0 0 0} color green"
//        + Escape.eP(m.spanningVectors[2]));
//System.out.println("draw line3 {0 0 0} color blue"
//        + Escape.eP(m.spanningVectors[3]));

    P3d ptTemp = new P3d();
    P4d planeGammaK = new P4d();
    V3d vGammaToKPoint = new V3d();
    V3d vTemp = new V3d();
    BS bsMoved = new BS();
    Map<String, Integer> mapEdge = new Hashtable<String, Integer>();
    m.bsSlabGhost = new BS();

    // iterate over the 26 k-points using getIntersection() to
    // clip cleanly on the bisecting plane and identify "ghost" triangles
    // which we will simply copy. We have to be careful here never to 
    // move a point twice for each k-point. The iteration is restarted
    // if any points are moved.

    for (int i = 1; i < 27; i++) {
      vGammaToKPoint.setT(pts[i]);
      MeasureD.getBisectingPlane(pts[0], vGammaToKPoint, ptTemp, vTemp,
          planeGammaK);
      getIntersection(1, planeGammaK, null, null, null, null, null, false,
          false, T.plane, true);

      //System.out.println("#slab " + i + " " + bsSlabGhost.cardinality());
      //System.out.println("isosurface s" + i + " plane " + Escape.escape(plane)
      //  + "#" + vGamma);
      bsMoved.clearAll();
      mapEdge.clear();
      for (int j = m.bsSlabGhost.nextSetBit(0); j >= 0; j = m.bsSlabGhost
          .nextSetBit(j + 1)) {
        if (m.setABC(j) == null)
          continue;

        // copy points because at least some will be needed by both sides,
        // and in some cases triangles will be split multiple times

        int[] p = AU.arrayCopyRangeI(m.pis[j], 0, -1);
        for (int k = 0; k < 3; k++) {
          int pk = p[k];
          p[k] = addIntersectionVertex(m.vs[pk], m.vvs[pk],
              m.vertexSource == null ? 0 : m.vertexSource[pk],
              m.vertexSets == null ? 0 : m.vertexSets[pk], mapEdge, 0, pk);
          // we have to be careful, because some points have already been
          // moved 
          if (pk != p[k] && bsMoved.get(pk))
            bsMoved.set(p[k]);
        }
        m.addPolygon(p, m.bsSlabDisplay);

        // now move the (copied) points

        for (int k = 0; k < 3; k++)
          if (!bsMoved.get(p[k])) {
            bsMoved.set(p[k]);
            m.vs[p[k]].sub(vGammaToKPoint);
          }
      }

      if (m.bsSlabGhost.nextSetBit(0) >= 0) {

        // append these points to the display set again
        // and clear the ghost set

        //bsSlabDisplay.or(bsSlabGhost);
        m.bsSlabGhost.clearAll();

        // restart iteration if any points are moved, because 
        // some triangles need to be moved and/or split multiple 
        // times, and the order is not predictable (I don't think).

        i = 0;
      }
    }

    // all done -- clear ghost slabbing and reset the bounding box

    m.bsSlabGhost = null;
    // reset BoundingBox
    BoxInfo bi = new BoxInfo();
    if (m.pc == 0) {
      for (int i = m.vc; --i >= 0;)
        bi.addBoundBoxPoint(m.vs[i]);
    } else {
      BS bsDone = new BS();
      for (int i = m.pc; --i >= 0;) {
        int[] f = m.setABC(i);
        if (f != null)
          for (int j = 3; --j >= 0;)
            if (!bsDone.get(f[j])) {
              bi.addBoundBoxPoint(m.vs[f[j]]);
              bsDone.set(f[j]);
            }
      }
    }
    m.setBoundingBox(bi.getBoundBoxPoints(false));
  }
  
  int addIntersectionVertex(T3d vertex, double value, int source,
                                    int set, Map<String, Integer> mapEdge,
                                    int i1, int i2) {
    String key = getKey(i1, i2);
    if (key.length() > 0) {
      Integer v = mapEdge.get(key);
      if (v != null)
        return v.intValue();
    }
    if (m.vertexSource != null) {
      if (m.vc >= m.vertexSource.length)
        m.vertexSource = AU.doubleLengthI(m.vertexSource);
      m.vertexSource[m.vc] = source;
    }
    if (m.vertexSets != null) {
      if (m.vc >= m.vertexSets.length)
        m.vertexSets = AU.doubleLengthI(m.vertexSets);
      m.vertexSets[m.vc] = set;
    }
    int i = m.addVCVal(vertex, value, true);
    if (key.length() > 0)
      mapEdge.put(key, Integer.valueOf(i));
    return i;
  }

  String getKey(int i1, int i2) {
    return (i1 < 0 ? "" : i1 > i2 ? i2 + "_" + i1 : i1 + "_" + i2);
  }

  /**
   * from MeshCapper
   * 
   * @param ipt1
   * @param ipt2
   * @param ipt3
   */
  void addTriangle(int ipt1, int ipt2, int ipt3) {
    m.addPolygonV3(ipt1, ipt2, ipt3, 0, 0, 0, m.bsSlabDisplay);  
  }

}
