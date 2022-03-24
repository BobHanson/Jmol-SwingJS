/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.util;

import java.util.Hashtable;

import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.V3;

import org.jmol.api.JmolScriptEvaluator;
import org.jmol.bspt.PointIterator;
import org.jmol.script.T;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * class to represent and produce Brillouin zone polyhedra and Wigner-Seitz
 * cells
 * 
 * based on https://chemapps.stolaf.edu/jmol/jsmol/spt/ext/xtal/bz.spt
 */
public class BZone {
  //global options: 

  static String[] bzColors = new String[] { "red", "green", "skyblue",
      "orange", "yellow", "indigo", "violet" };
  boolean bzDrawPointsAndEdges = false;
  boolean bzSavePmeshes = false;

  //global variables:

  private Lst<BZone> bzones = null; // Brillouin zones
  private P3 bzGamma = new P3();
  private Lst<P3> bzFaceCenters = null;
  private Lst<P3> bzLatticePts = null;
  private P3[] bzLatticePtsAll = null;
//  private Lst<P3> bzPlanePtsAll = null;
  private Lst<P3> bzPlanePts = null;
  private Lst<BZone> subzones = null;

  private boolean isWignerSeitz;
  private Viewer vwr;
  private JmolScriptEvaluator eval;

  private String id;
  private int index;
  private String color;
  private Lst<P3> latticePts, newLatticePts, newPlanePts;
  private Lst<P4> planes, newPlanes;
  float volume = 0;
  int zoneIndex;
  P3 offset, center;
  private Lst<P4> planesUnused;
  private Lst<P3> ptsUnused;
  private Lst<Object> pmeshes;
  private Lst<Double> areas;
  private Lst<P3[]> faces;
  private Lst<int[]> faceIndices;
  private Lst<P3> faceCenters;
  private double totalArea;

  public BZone() {
    // for reflection
  }

  public BZone setViewer(Viewer vwr) {
    this.vwr = vwr;
    eval = vwr.eval;
    return this;
  }

  /**
   * Create a Brillouin zone.
   * 
   * /// createBZ or createBZ(1, null, false) just the 1st Brillouin zone ///
   * createBZ(n, null, false) just the nth Brillouin zone /// createbZ(0, [a b c
   * alpha beta gamma], false) create a BZ for a given primitive lattice unit
   * cell /// createbZ(0, [a b c alpha beta gamma], true) create a BZ for a
   * given reciprocal lattice unit cell
   * @param zone 
   * @param array 
   * @param isK 
   * @param id 
   * @param scale 
   * 
   */
  public void createBZ(int zone, Object[] array, boolean isK, String id, float scale) {
    if (vwr == null)
      return;
    if (array != null)
      demoBZ(array, isK);
    else
      createAllBZs(zone, true, id, scale);
    //  if (bzSavePmeshes) {
    //    polyhedra * off;
    //    pmesh * on;
    //  }

  }

  /**
   * Create a Wigner-Seitz unitcell centered on {0 0 0}.
   * 
   * /// primitive cell is assumed -- user is responsible for unitcell PRIMITIVE
   * /// executed first (unitcell CONVENTIONAL later if desired) ///
   * createWS("p1") for example.
   * @param id 
   * 
   * 
   */
  public void createWS(String id) {
    if (vwr == null)
      return;
    createAllBZs(-1, false, id, 1);
  }

  /**
   * Starting with 1, build the Brillouin zones as polyhedra, optionally
   * discarding the previous as we go.
   * 
   * bNote that even for the 4th Brillouin zone, this is time consuming.
   * 
   * If n = -1, then this is a Wigner-Seitz cell
   * @param n 
   * @param discardPrev 
   * @param id 
   * @param scale 
   * 
   */
  private void createAllBZs(int n, boolean discardPrev, String id, float scale) {

    // set up the unit cell as a reciprocal lattice
    // scaling it by 2 pi just to make it somewhat larger
    // and draw the axes

    cmd("unitcell reset");
    //print "conventional: " + show("unitcell/a").trim()
    cmd("unitcell primitive");
    //print "primitive: " + show("unitcell/a").trim()

    if (n < 0) {
      n = -n;
      isWignerSeitz = true;
    } else {
      if (n == 0)
        n = 1;      
      if (Float.isNaN(scale))
        scale = 2;
      //id = "";
      cmd("unitcell 'reciprocal' " + scale);
      //print "reciprocal: " + show("unitcell/a").trim()
    }

    cmd("polyhedra pbz* delete");
    cmd("pmesh fbz* delete");

    if (!isWignerSeitz) {
      cmd("axes unitcell; axes on; axes scale 2.0;axes 0.01;axes labels \"b1\" \"b2\" \"b3\" \"\"");
    }

    bzones = new Lst<BZone>();
    bzLatticePts = new Lst<P3>();
    bzPlanePts = new Lst<P3>();
    //bzPlanePtsAll = new Lst<P3>();
    bzFaceCenters = new Lst<P3>();

    boolean wasPrecise = vwr.getBoolean(T.legacyjavafloat);
    vwr.setBooleanProperty("legacyJavaFloat", true);
    //set legacyJavaFloat // ensures high precision point positions

    getLatticePoints(n);

    // loop through starting with zone 1  

    bzones.addLast(null);
    for (int i = 1; i <= n; i++) {
      bzones.add(i, newBZ(i));
      createNextBZ(bzones.get(i), bzones.get(i - 1), id);
      if (discardPrev && i > 1)
        cmd("polyhedra id \"pbz" + (i - 1) + "_*\" delete");
    }

    vwr.setBooleanProperty("legacyJavaFloat", wasPrecise);

    if (bzSavePmeshes) {
      cmd("polyhedra * off;pmesh * on;");
    }
  }

  private void createNextBZ(BZone zone, BZone zonePrev, String id) {
    getNewLatticePoints(zone);
    if (bzDrawPointsAndEdges)
      drawZoneCenters(zone);
    getSubzones(zone, zonePrev);
    for (int i = 0; i < zone.subzones.size(); i++) {
      BZone subzone = zone.subzones.get(i);

      // first we create a set of pmeshes, each a set of triangles
      // by slabbing subzone planes
      // then we get the faces associated with each of those pmeshes

      if (!getSubzonePmeshes(subzone))
        continue;

      // now, for testing, we draw those polygon faces

      if (bzDrawPointsAndEdges)
        drawSubzonePolygons(subzone);

      // finally, we create the subzone's polyhedron

      createSubzonePolyhedron(subzone, id);
    }
    finalizeZone(zone);
  }

  /*
   * initialize a new Brillouin subzone
   */
  private BZone newSubZone(BZone zone, String id, int index) {
    BZone subzone = new BZone();
    subzone.index = index;
    subzone.id = zone.id + id + index + "_";
    subzone.zoneIndex = zone.index;
    subzone.newLatticePts = zone.newLatticePts;
    subzone.planes = new Lst<P4>();
    subzone.latticePts = new Lst<P3>();
    subzone.planesUnused = new Lst<P4>();
    subzone.ptsUnused = new Lst<P3>();
    subzone.pmeshes = new Lst<Object>();
    subzone.areas = new Lst<Double>();
    subzone.faces = new Lst<P3[]>();
    subzone.faceIndices = new Lst<int[]>();
    subzone.faceCenters = new Lst<P3>();
    subzone.volume = 0;
    subzone.color = zone.color;
    subzone.offset = new P3();
    subzone.center = new P3();
    zone.subzones.addLast(subzone);
    return subzone;
  }
  private void getSubzones(BZone zone, BZone zonePrev) {

    if (zone.index == 1) {

      // for BZ1, just use the zone planes

      BZone subzone = newSubZone(zone, "", 1);
      subzone.latticePts = zone.newLatticePts;
      subzone.planes = zone.newPlanes;
      return;
    }

    // for all others, go through all previous subzones...

    for (int i = 0; i < zonePrev.subzones.size(); i++) {

      // ...each subzone of the previous zone has a set of planes. 

      Lst<P4> planesNew = zone.newPlanes;
      Lst<P3> ptsNew = zone.newLatticePts;

      BZone prev = zonePrev.subzones.get(i);
      Lst<P4> planesPrev = prev.planes;
      Lst<P3> ptsPrev = prev.latticePts;

      Lst<P4> planesUnusedPrev = prev.planesUnused;
      Lst<P3> ptsUnusedPrev = prev.ptsUnused;
      
      Lst<P3> centersPrev = prev.faceCenters;

      String id = prev.id.substring(4);

      // ...use all planes if the previous is BZ1       
      // otherwise, always skip the first plane, which originated two zones back       

      for (int j0 = (zonePrev.index == 1 ? 0 : 1), j = j0; j < planesPrev.size(); j++) {
        if (j0 == 1 && within(0.01f, centersPrev.get(j), bzFaceCenters).size() > 1)
          continue;

        // each of these planes is a starting point for a new subzone

        BZone subzone = newSubZone(zone, id, j + 1);

        // the new subzone's initial plane (j) will be negative of the first plane
        // ...now add all the other previous planes, without inversion

        addBZ(subzone.planes, subzone.latticePts, planesPrev, ptsPrev, j);

        // ...now add all the previously unused planes

        addBZ(subzone.planes, subzone.latticePts, planesUnusedPrev, ptsUnusedPrev, -1); 

        // ...now add all the new planes

        addBZ(subzone.planes, subzone.latticePts, planesNew, ptsNew, -1);
      }  
    }
  }

  /*
   * Add the necessary planes from planes0 into the subzone.planes array 
   * and add necessary lattice points from pts0 into the subzone.latticePts array.
   * 
   * Note that subzone.latticepts is for reference only and is not used in the calculation.
   *    
   */
  private void addBZ(Lst<P4> planes, Lst<P3> pts, Lst<P4> planes0, Lst<P3> pts0, int j) {

    // designated j is inverted and introduced first

    if (j >= 0) {
      P4 pt4 = P4.newPt(planes0.get(j));
      pt4.scale4(-1f);
      planes.addLast(pt4);
      pts.addLast(pts0.get(j));
    }
    int n = planes0.size();
    for (int k = 0; k < n; k++) {
      if (k != j) { 
        planes.addLast(planes0.get(k));
        pts.addLast(pts0.get(k));
      }
    }
  }

  /**
   * Loop through all points, looking for non-excluded points using St. Olaf
   * half-distance sphere test.
   * @param zone 
   */
  private void getNewLatticePoints(BZone zone) {

    Lst<P3> unusedPts = new Lst<P3>();
    Lst<P3> unusedLatticePts = new Lst<P3>();
    Lst<P3> centers = zone.newPlanePts;
    Lst<P3> zoneLPs = zone.newLatticePts;
    Lst<P4> planes = zone.newPlanes;
    Lst<P3> ap;
    Lst<P3> al;
    for (int i = 0; i < bzPlanePts.size(); i++) {
      P3 p = bzPlanePts.get(i);
      P3 center = P3.newP(p);
      center.scale(0.5f);

      // just a bit over so that all excluding points are found

      float radius = 0.501f * p.length();
      Lst<P3> inSphere = within(radius, center, bzPlanePts);

      // there is always at least one point within this radius -- point p itself

      if (inSphere.size() == 1) {
        ap = centers;
        al = zoneLPs;

        // plane through point p directed away from Gamma

        planes.addLast(plane(bzGamma, p, 1));

      } else {
        ap = unusedPts;
        al = unusedLatticePts;
      }
      ap.addLast(p);
      al.addLast(bzLatticePts.get(i));
    }

    // replace lattice and plane points with just those that have not been used

    bzPlanePts = unusedPts;
    bzLatticePts = unusedLatticePts;
  }

  private P4 plane(P3 pt1, P3 pt2, float f) {
    // plane(<point1>,<point2>,f)
    V3 norm = V3.newVsub(pt2, pt1);
    P3 pt3 = new P3();
    pt3.scaleAdd2(f, norm, pt1);
    P4 plane = new P4();
    Measure.getPlaneThroughPoint(pt3, norm, plane);
    return plane;
  }

  private Lst<P3> within(float radius, P3 center, Lst<P3> pts) {
    Lst<P3> ret = new Lst<P3>();
    float r2 = radius * radius;
    for (int i = 0, n = pts.size(); i < n; i++) {
      P3 pt = pts.get(i);
      if (center.distanceSquared(pt) < r2)
        ret.addLast(pt);
    }
    return ret;
  }

  /**
   * initialize a new Brillouin zone
   * @param i 
   * @return new BZone
   */
  private BZone newBZ(int i) {
    BZone bzone = new BZone();
    bzone.id = "bz" + i + "_";
    bzone.index = i;
    bzone.color = bzColor(i);

    // key here is that Brillouin zones are going to have multiple parts 
    // each subzone will ultimately be a single polyhedron

    bzone.subzones = new Lst<BZone>();
    bzone.newLatticePts = new Lst<P3>();
    bzone.newPlanePts = new Lst<P3>();
    bzone.newPlanes = new Lst<P4>();
    bzone.volume = 0;
    return bzone;
  }

  /**
   * give each Brillouin zone a new color
   * @param i 
   * @return color as string 
   */
  private String bzColor(int i) {
    return bzColors[(i - 1) % bzColors.length];
  }

  /**
   * Get the needed lattice points for n Brillouin zones.
   * 
   * A calculation is done to ensure that enough points are provided in all
   * directions, which may not be the same number.
   * @param n 
   * 
   */
  private void getLatticePoints(int n) {

    // Note that "pt.xyz" is interpreted in jmol as "fractional to Cartesian".

    // we need to set the min and max values on each axis. 

    int[][] minmax = new int[3][3];
    // get the max length of an edge
    P3 pt = new P3();
    float[] abc = new float[] { newPoint(1, 0, 0, pt).length(),
        newPoint(0, 1, 0, pt).length(), newPoint(0, 0, 1, pt).length() };
    float abcmax = Math.max(abc[0], Math.max(abc[1], abc[2]));

    for (int i = 0; i < 3; i++) {
      int m = (int) (n * abcmax / abc[i]);
      minmax[i] = new int[] { -m, m };
    }
    //print "setting lattice ranges to " + minmax.format("JSON")
    Lst<P3> pts = new Lst<P3>();
    for (int i = minmax[0][0]; i <= minmax[0][1]; i++) {
      for (int j = minmax[1][0]; j <= minmax[1][1]; j++) {
        for (int k = minmax[2][0]; k <= minmax[2][1]; k++) {
          // skip Gamma itself
          if (i != 0 || j != 0 || k != 0) {
            P3 lppt = newPoint(i, j, k, new P3());
            pts.addLast(P3.newP(lppt));
            bzLatticePts.addLast(lppt);
            P3 ppt = P3.newP(lppt);
            ppt.scale(0.5f);
            bzPlanePts.addLast(ppt);
            System.out.println("draw ID 'pt"  + i + j + k + "' " + lppt); // for testing
          }
        }
      }
    }
    bzLatticePtsAll = pts.toArray(new P3[pts.size()]);
  }

  private P3 newPoint(int i, int j, int k, P3 pt) {
    pt.x = i;
    pt.y = j;
    pt.z = k;
    vwr.toCartesian(pt, false);
    return pt;
  }

  private void cmd(String cmd) {
    System.out.println(cmd);
    try  {  
      eval.runScript(cmd);
    } catch (Exception e) {
      //
    }
  }

  /**
   * @param array  
   * @param isK 
   */
  private void demoBZ(Object[] array, boolean isK) {
    // TODO

  }

  /**
   * Loop through all planes, creating a pmesh for each face.
   * We use resolution 0.001 to indicate we only want the 
   * minimum number of triangles (that is, starting with two giant 
   * triangles, not a grid of small triangles). 
   * Also slab each plane by all other planes to form a face.
   * @param subzone 
   * @return true if total area gt 0 
   */
  private boolean getSubzonePmeshes(BZone subzone) {
     planes = subzone.planes;
     latticePts = subzone.latticePts;
     planesUnused = subzone.planesUnused;
     ptsUnused = subzone.ptsUnused;
     faces = subzone.faces;
     faceCenters = subzone.faceCenters;
     int nPlanes = planes.size();

      // It is important to include all planes, 
      // as they may be used in later BZs. 

      // subzone.planes will be replaced by planesUsed 
      // subzone.latticePts will be replaced by ptsUsed

      Lst<P4> planesUsed = new Lst<P4>();
      Lst<P3> ptsUsed = new Lst<P3>();

      double totalArea = 0; 
      for (int i = 0; i < nPlanes; i++) {
        String pid = "f" + subzone.id + i;
        //print "creating " + pid;
        cmd ("pmesh ID " + pid + " silent resolution 0.001 boundingbox {-2/1 -2/1 -2/1} {2/1 2/1 2/1} plane   " + toScript(planes.get(i))+" off");
        double area = 0;
        
        for (int j = 0; j < nPlanes; j++) {
          if (j == i)
            continue; // don't slab by plane being slabbed
          cmd("pmesh slab plane " + toScript(planes.get(j)));
          double[] a = (double[])getProperty(pid, "area");
          area = (a == null ? 0 : a[0]);
          //print "area is " + area;
          if (area == 0) {
            // this i-plane has been totally excluded -- we are done here
            break;
          }
          totalArea += area;
        }
        P3 a = null;
        P3[] face = null;
        if (area > 0) {

          // The new Jmol feature pmesh.getProperty("face") allows us to extract an 
          // array of points that are only at the intersections of planes. 
          // They are in order, right-hand rule CCW cycle  

          // Here we are seeing if there are already two faces at this center,
          // indicating that we are re-entrant this time.

          face = (P3[]) getProperty(pid, "face");
                        
          // this can be [] if a very tiny triangle was found. 
          a = average(face);
          if (i == 0 && within(0.01f, a, bzFaceCenters).size() >= 2) {
            area = 0;
            totalArea = 0;
            i = nPlanes;
          }
        }
        if (area > 0) {
          faces.addLast(cleanFace(face));
          faceCenters.addLast(a);
          bzFaceCenters.addLast(a);
          if (bzSavePmeshes) {
            subzone.pmeshes.addLast(pid);
          } else {
            cmd("pmesh ID " + pid + " delete");
          }
          planesUsed.addLast(planes.get(i));
          ptsUsed.addLast(latticePts.get(i));
          subzone.areas.addLast(Double.valueOf(area));
        } else {
          cmd("pmesh ID " + pid + " delete");
          planesUnused.addLast(planes.get(i));
          ptsUnused.addLast(latticePts.get(i));
        }
        subzone.planes = planesUsed;
        subzone.latticePts = ptsUsed;
      }
      subzone.totalArea = totalArea;

      //prompt pid + " " + area + " " + nplanes

      return (totalArea > 0);
    }
    
  private String toScript(P4 p4) {
    return "{" + p4.x + " " + p4.y + " " + p4.z + " " + p4.w + "}";
  }

  private static P3 ptInner = P3.new3(Float.NaN,  0,  0);
  private Object[] ret = new Object[1];
  String polyid;
  P3[] pts;
  
  private Object getProperty(String name, String key) {
    Object[] data = new Object[3];
    int shapeID;
    shapeID = vwr.shm.getShapeIdFromObjectName(name);
    if (shapeID >= 0) {
      data[0] = name;
      vwr.shm.getShapePropertyData(shapeID, "index", data);
      if (data[1] != null && !key.equals("index")) {
        int index = ((Integer) data[1]).intValue();
        data[1] = vwr.shm.getShapePropertyIndex(shapeID, key.intern(), index);
      }
    }
    return data[1];
  }

  /**
   * Generate the polyhedra.
   * @param subzone 
   * @param id 
   *  
   */
  private void createSubzonePolyhedron(BZone subzone, String id) {

      if (id == null)
        id = "p" + subzone.id;
      //print "id is " + id;
      //print "creating " + id;
      subzone.polyid = id;

      // Variable pts is an array of arrays, with duplicate points. 
      // We want a flat array using .join() and a new Jmol feature that
      // uses within(distance, array) to remove the duplicate points.

      P3[] apts = join(subzone.faces);
      //print "var pts =" + pts
      P3[] pts = cleanFace(apts);
      //print "pts =" + pts
      subzone.pts = pts;
      subzone.center = average(pts);
      subzone.offset = closest(subzone.center, bzLatticePtsAll); // closest
      subzone.faceIndices = new Lst<int[]>();
      Lst<int[]> ifaces = subzone.faceIndices;
      Lst<P3[]> faces = subzone.faces;
      for (int i = 0, n = faces.size(); i < n; i++) {
        ifaces.addLast(faceIndices(faces.get(i), pts)); 
      }
      for (int i = ifaces.size(); --i >= 0;) {
        if (ifaces.get(i).length < 3) {
          subzone.faces.removeItemAt(i);
          subzone.faceIndices.removeItemAt(i);
          subzone.faceCenters.removeItemAt(i);
          subzone.planes.removeItemAt(i);
        }
      } 
      // We now create the polyhedron at Gamma that goes to all these points
      // using a new Jmol feature that allows named polyhedra at specific points
      Hashtable<String, Object> p = new Hashtable<String, Object>();
      p.put("id", id);
      p.put("center", subzone.center);
      Lst<P3> lst = new Lst<P3>();
      for (int i = 0, n = pts.length; i < n; i++)
        lst.addLast(pts[i]);
      p.put("vertices", lst);
      p.put("faces", ifaces);
      p.put("color", subzone.color);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.TRUE);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "info", p);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "generate", null);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.FALSE);

      if (bzDrawPointsAndEdges) {
        // for testing purposes:
        cmd("color $" + id + " translucent");
        cmd("draw pts points " + pts +" dots nofill nomesh");
      }
  }

  private int[] faceIndices(P3[] p3s, P3[] pts) {
    PointIterator.withinDistPoints(0, null, pts, p3s, null, ret);
    return (int[]) ret[0];
  }

  private P3 closest(P3 center, P3[] ap3) {
    PointIterator.withinDistPoints(0, center, ap3, null, null, ret);
    return (P3) ret[0];
  }

  private P3[] cleanFace(P3[] face) {
    PointIterator.withinDistPoints(0.01f, ptInner, face, null, null, ret);
    @SuppressWarnings("unchecked")
    Lst<P3> l = (Lst<P3>) ret[0];
    return l.toArray(new P3[l.size()]);
  }

  private P3 average(P3[] face) {
    P3 a = new P3();
    for (int i = face.length; --i >= 0;)
      a.add(face[i]);
    a.scale(1f/face.length);
    return a;
  }

  private P3[] join(Lst<P3[]> faces) {
    int n = 0;
    for (int i = faces.size(); --i >= 0;)
      n += faces.get(i).length;
    P3[] pts = new P3[n];
    n = 0;
    for (int i = faces.size(); --i >= 0;) {
      P3[] face = faces.get(i);
      for (int j = face.length; --j >= 0;)
        pts[n++] = face[j];
    }
    return pts;
  }

  /**
   * @param zone  
   */
  private void drawZoneCenters(BZone zone) {
    // TODO - debugging only

  }

  /**
   * @param subzone  
   */
  private void drawSubzonePolygons(BZone subzone) {
    // TODO - debugging only

  }

  /**
   *  Finalize a Brillouin zone. 
   * @param zone 
   */
  private void finalizeZone(BZone zone) {

      // remove 0-volume subzones
      
      for (int i = zone.subzones.size(); --i >= 0;) 
        if (zone.subzones.get(i).totalArea == 0)
          zone.subzones.removeItemAt(i);
      
//      if (zone.index == 1) {
//        //calculate symmetry polyhedra @{zone.subzones[1].polyid};
//        //info = getProperty("shapeInfo.Polyhedra");  
//        //bzones[1].pointGroup = info.select("(pointGroup) where id='"+subzone.polyid+"'")[1];
//      }
//        
//      // calculate total volume

//      zone.volume = 0;
//      info = getProperty("shapeInfo.Polyhedra");  
//      for (var subzone in zone.subzones) {
//        var v = info.select("(volume) where id='"+subzone.polyid+"'")[1];
//        subzone.volume = v;
//        zone.volume += v;
//      }
//      
//      // list again all volumes, for checking
//      
//      for (var i = 1; i <= zone.index; i++) {
//        print "BZ" + i + " volume=" + bzones[i].volume%7 + " " + bzones[i].pointGroup;
//      }
  }


}
