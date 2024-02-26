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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.util;

import java.util.Hashtable;

import org.jmol.bspt.PointIterator;
import org.jmol.script.T;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.V3d;

/**
 * A class to represent and produce Brillouin zone polyhedra and Wigner-Seitz
 * cells.
 * 
 * initially implemented in JavaScript
 * https://chemapps.stolaf.edu/jmol/jsmol/spt/ext/xtal/bz.spt
 * 
 * based on the algorithm worked out by Jake LaNasa and Bob Hanson in 2015 and
 * described in /_documents/brillouin-zones-laNasa.pdf or
 * https://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol/_documents/brillouin-zones-laNasa.pdf
 * 
 * Created by
 * 
 * POLYHEDRON BRILLOUIN
 * 
 * or 
 * 
 * POLYHEDRON BRILLOUIN n    // where 1 <= n <= 8
 * 
 * or 
 * 
 * POLYHEDRON offset x.x BRILLOUIN n    // where x.x is an "explosive" offset of subzones from the center
 *
 * For example:
 * 
 * zap;
 * modelkit spacegroup;
 * polyhedron offset 0.8 brillouin 4;
 * 
 * Contains to static subclasses, BZ and BZPoint. 
 * 
 */
public class BZone {

  public BZone() {
    // for reflection
  }

  /**
   * Create a Brillouin zone.
   * 
   * The single public method in this class.
   * 
   * @param vwr
   * @param n
   * @param array
   * @param isK
   * @param id
   * @param scale
   * @param foffset
   * @param offset 
   * 
   */
  public void createBZ(Viewer vwr, int n, Object[] array, boolean isK,
                       String id, double scale, double foffset, P3d offset) {
    BZ bz = new BZ(vwr, id, n);
    bz.createAllZones(scale, foffset, offset);
  }

  /**
   * A class to represent a Brillouin zone k-value that corresponds to one or more faces
   * of the polyhedron associated with a Brillouin zone.
   * 
   * Extends P3d in order to have a Cartesian equivalence
   * 
   */
  private static class BZPoint extends P3d {
    int h;
    int k;
    int l;
    
    BZPoint(int h, int k, int l) {
      this.h = h;
      this.k = k;
      this.l = l;
    }
    
    @Override
    public String toString() {
      return "[" + h + " " + k + " " + l + "] " + super.toString();
    }

    String hkl() {
      return "{" + h + " " + k + " " + l + "}";
    }
  }

  /**
   * An overall class for managing the set of Brillouin zones leading to the
   * desired nth zone, preserving global features of the reciprocal lattice and
   * the set of Brillouin zones.
   * 
   */
  private static class BZ {

    public P3d offset;

    Viewer vwr;

    /**
     * A box for limiting the extent of the temporary PMesh objects created as starting points for faces.
     * 
     */
    P3d[] pmeshBox;
    
    /*private*/ P3d b1, b2, b3;
    P4d[] wedgePlanes;
    final P3d bzGamma = new P3d();
    Lst<BZPoint> bzLatticePts;
    Lst<P3d> bzFaceCenters;
    Lst<P3d> bzPlanePts;
    double explodeOffset;
    double scale;
    String id;
    int n;

    boolean isWignerSeitz;

    private P3d centerOffset;


    /**
     * 
     * @param vwr
     * @param id  
     * @param n   nth Brillouin or -1 for Wigner-Seitz
     */
    BZ(Viewer vwr, String id, int n) {
      this.vwr = vwr;
      if (n == -2) {
        if (id == null)
          id = "hkl";
        /// hkl planes visualization
      } else if (n == -1) {
        isWignerSeitz = true;
        n = 1;
        if (id == null)
          id = "pws"; // "polyhedron Wigner-Seitz"
      } else {
        if (id == null)
          id = "pbz"; // "polyhedron Brillouin zone"
      }
      this.n = n;
      this.id = id;

    }

    /**
     * Starting with 1, build the Brillouin zones as polyhedra, discarding the
     * previous as we go if not "exploding".
     * 
     * @param scale
     *        scaling; Brillouin only
     * @param explodeOffset
     *        separation of Brillouin zone sections; Brillouin and n > 1 only
     * @param offset
     *        Cartesian offset of polyhedron; Wigner-Seitz only
     * 
     */
    public void createAllZones(double scale, double explodeOffset, P3d offset) {

      if (isWignerSeitz) {
        isWignerSeitz = true;
        this.offset = offset;
      } else {
        if (n > 1)
          this.explodeOffset = explodeOffset;
        this.scale = scale;
      }

      boolean wasPrecise = vwr.getBoolean(T.doubleprecision);
      vwr.setBooleanPropertyTok("doublePrecision", T.doubleprecision, true);
      initializeBZ(n);

      // loop through starting with zone 1  

      String cmd = "";
      double volume1 = 0;
      BZone zone = null;
      for (int i = 1; i <= n; i++) {
        BZone prev = zone;
        zone = new BZone();
        zone.bz = this;
        zone.index = i;
        zone.id = id + "_" + i + "_";
        zone.color = bzColors[(i - 1) % bzColors.length];
        zone.create(prev);
        if (i == 1) {
          volume1 = zone.volume;
          //        //calculate symmetry polyhedra @{zone.subzones[1].polyid};
          //        //info = getProperty("shapeInfo.Polyhedra");  
          //        //bzones[1].pointGroup = info.select("(pointGroup) where id='"+`polyid+"'")[1];      
        }
        if (!isWignerSeitz)
          vwr.showString("Brillouin Zone " + zone.index + " volume = "
              + Math.round(zone.volume / volume1 * 1000) / 1000d + " subzones:"
              + zone.subzones.size() + " new k-points:"
              + zone.newLatticePts.size(), false);

        if (i > 1 && explodeOffset == 0)
          cmd += "polyhedra id " + id + (i - 1) + "_* delete;";
      }
      if (!isWignerSeitz) {
        //cmd += "axes unitcell; axes on; axes scale 2.0;axes 0.01;axes labels  'h' 'k' 'l' \"\";";
      }
      cmd(cmd + ";restore unitcell _bz;");
      vwr.setBooleanPropertyTok("doublePrecision", T.doubleprecision,
          wasPrecise);
    }

    /**
     * Get the needed lattice points for n Brillouin zones and various starting
     * points and global structures.
     * 
     * A calculation is done to ensure that enough points are provided in all
     * directions, which may not be the same number, depending upon lattice parameters.
     * 
     * @param n nth Brillouin;
     * 
     */
    void initializeBZ(int n) {

      // we must consider the offset set by the user in the next command.
      centerOffset = vwr.getCurrentUnitCell().getCartesianOffset();
      if (offset != null)
        offset.add(centerOffset);
      else
        offset = P3d.newP(centerOffset);      
      if (offset.length() == 0)
        offset = null;
      
      bzLatticePts = new Lst<BZPoint>();
      bzPlanePts = new Lst<P3d>();
      bzFaceCenters = new Lst<P3d>();

      String cmd = "save unitcell _bz;";
      if (isWignerSeitz) {
        cmd += "unitcell conventional;unitcell primitive;";
      } else {
        if (n == 0)
          n = 1;
        if (Double.isNaN(scale))
          scale = -1;
        // note - legacy transpiler will just write "-1" not "-1.0" if just ... + scale + ...
        // new Double(scale).toString() is what the SwingJS transpiler writes.
        cmd += "unitcell conventional;unitcell 'reciprocal' " + new Double(scale).toString() + ";";
      }
      cmd += "polyhedra " + id + "* delete;";
      cmd(cmd);


      // Note that "pt.xyz" is interpreted in jmol as "fractional to Cartesian".

      // set the range for pmesh planes to be [-2 2] in each direction.
      
      pmeshBox = new P3d[] { newCartesian(-2, -2, -2, new P3d()),
          newCartesian(2, 2, 2, new P3d()) };
      
      // we need to set the min and max values on each axis. 

      double[] abc = new double[] {
          (b1 = newCartesian(1, 0, 0, new P3d())).length(),
          (b2 = newCartesian(0, 1, 0, new P3d())).length(),
          (b3 = newCartesian(0, 0, 1, new P3d())).length() };
      
  // idea to cut BZ into a quadrant. Probably not that helpful.
//      
//    V3d v1 = new V3d();
//    V3d v2 = new V3d();
//    wedgePlanes = new P4d[] {
//      MeasureD.getPlaneThroughPoints(bzGamma, b2, b1, v1, v2, new P4d()),  
//      MeasureD.getPlaneThroughPoints(bzGamma, b3, b2, v1, v2, new P4d()),  
//      MeasureD.getPlaneThroughPoints(bzGamma, b1, b3, v1, v2, new P4d())  
//             
//    };
    

      // We need to make sure there is plenty of room for higher-order
      // h, k, l than just [-1,0,1]. This is important with monoclinic and
      // triclinic, where angles are small and there is a long side. 
      // e.g. load =aflowlib/1 unitcell [5 17 9 60 70 65], giving
      //
      //    BZ pt[9]=[0 2 1] {0.0, 2.5063, 2.8389}
      //    BZ pt[10]=[1 1 0] {6.1329, -1.6067, -1.7184}
      //    BZ pt[11]=[1 2 0] {6.1329, -0.3536, -2.294}
      //    BZ pt[12]=[1 2 1] {6.1329, -0.3536, 1.696}
      //    BZ pt[13]=[1 3 1] {6.1329, 0.8996, 1.1204}
      // 
      double abcmax = Math.max(abc[0], Math.max(abc[1], abc[2]));
      int[][] minmax = new int[3][3];
      // get the max length of an edge

      for (int i = 0; i < 3; i++) {
        int m = (int) ((n + 1) * abcmax / abc[i]);
        minmax[i] = new int[] { -m, m };
      }
          
      Lst<P3d> pts = new Lst<P3d>();
      for (int h = minmax[0][0]; h <= minmax[0][1]; h++) {
        for (int k = minmax[1][0]; k <= minmax[1][1]; k++) {
          for (int l = minmax[2][0]; l <= minmax[2][1]; l++) {
            // skip Gamma itself
            if (h != 0 || k != 0 || l != 0) {
              BZPoint lppt = new BZPoint(h, k, l);
              newCartesian(h, k, l, lppt);
              pts.addLast(P3d.newP(lppt));
              bzLatticePts.addLast(lppt);
              P3d ppt = P3d.newP(lppt);
              ppt.scale(0.5f);
              bzPlanePts.addLast(ppt);
              //System.out.println("draw ID 'pt"  + i + j + k + "' " + lppt); // for testing
            }
          }
        }
      }
    }

    /**
     * Convert HKL to Cartesian representation of the reciprocal lattice.
     * 
     * Any offset of the current unit cell is ignored.
     * 
     * @param h
     * @param k
     * @param l
     * @param ret  point to be returned
     * @return ret
     */
    private P3d newCartesian(int h, int k, int l, P3d ret) {
      ret.x = h;
      ret.y = k;
      ret.z = l;
      vwr.toCartesian(ret, true);
      return ret;
    }

    /**
     * Send command to Jmol for managing the unit cell and clearing polyhedra.
     * 
     * @param cmd
     */
    void cmd(String cmd) {
      try {
        vwr.eval.runScript(cmd);
      } catch (Exception e) {
        //
      }
    }

    /**
     * Create the PMesh surface that will be slabbed to create the faces of the polyhedron.
     * In the end, these are discarded.
     * 
     * @param pid plane id
     * @param plane  half way from Gamma to a K-point
     */
    void createPMesh(String pid, P4d plane) {
      vwr.shm.setShapeProperties(JC.SHAPE_PMESH, 
          new Object[] { "init", "cmd" },
          new Object[] { "thisID", pid }, 
          new Object[] { "newObject", null },
          new Object[] { "fileType", "Pmesh" }, 
          new Object[] { "silent", null },
          new Object[] { "resolution", Double.valueOf(0.001d) },
          new Object[] { "boundingBox", pmeshBox },
          new Object[] { "plane", plane },
          new Object[] { "nomap", Double.valueOf(0) },
          new Object[] { "hidden", Boolean.TRUE },
          new Object[] { "finalize", "cmd" }, 
          new Object[] { "clear", null }
      );
    }

    /**
     * Reduce the extent of the plane using ISOSURFACE SLAB methods.
     * 
     * @param pid
     * @param plane
     * @return true if slabbing leaves any area; false if this slab discards all
     *         of the remaining triangles composing the face.
     */
    boolean slab(String pid, P4d plane) {
      if (plane == null) {
        if (wedgePlanes != null) {
          for (int n = wedgePlanes.length, w = 0; w < n; w++) {
            if (!slab(pid, wedgePlanes[w]))
              return false;
          }
        }
        return true;
      }
      vwr.shm.setShapePropertyBs(JC.SHAPE_PMESH, "slab",
          MeshSurface.getSlabObjectType(T.plane, plane, false, null), null);
      double[] a = (double[]) getProperty(pid, "area");
      //System.out.println(pid + " " + plane + " " + a[0]);
      return (a != null && a[0] != 0);
    }

    /**
     * Discard all relevant PMesh objects.
     * 
     * @param pid
     */
    void clearPMesh(String pid) {
      vwr.setShapeProperty(JC.SHAPE_PMESH, "clear", null);
      vwr.setShapeProperty(JC.SHAPE_PMESH, "delete", pid);
    }

    /**
     * Get properties "face", "index", and "area"
     * 
     * @param name
     * @param key
     * @return the data from this query
     * 
     */
    Object getProperty(String name, String key) {
      Object[] data = new Object[3];
      data[0] = name;
      vwr.shm.getShapePropertyData(JC.SHAPE_PMESH, "index", data);
      if (data[1] != null && !key.equals("index")) {
        int index = ((Integer) data[1]).intValue();
        data[1] = vwr.shm.getShapePropertyIndex(JC.SHAPE_PMESH, key.intern(), index);
      }
      return data[1];
    }

    /**
     * Create the polyhedron for the subzone and return its volume
     * @param subzone 
     * @param pts 
     * 
     * @return volume
     */
    double addPolyhedron(Subzone subzone, P3d[] pts) {
      
      if (offset != null) {
        subzone.center.add(offset);
        for (int i = pts.length; --i >= 0;)
          pts[i].add(offset);
      }

      // We now create the polyhedron at Gamma that goes to all these points
      // using a new Jmol feature that allows named polyhedra at specific points
      Hashtable<String, Object> info = new Hashtable<String, Object>();
      //System.out.println("BZONE " + id);
      info.put("id", subzone.id);
      info.put("center", subzone.center);
      Lst<P3d> lst = new Lst<P3d>();
      for (int i = 0, n = pts.length; i < n; i++)
        lst.addLast(pts[i]);
      info.put("vertices", lst);
      info.put("faces", subzone.faceIndices);
      if (subzone.index > 1 &&  explodeOffset != 0)
        info.put("explodeOffset", Double.valueOf(explodeOffset * (subzone.index - 1)));
      info.put("color", subzone.color);
      info.put("volume", Double.valueOf(0));
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.TRUE);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "info", info);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "generate", null);
      vwr.setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.FALSE);
      return ((Number) info.get("volume")).doubleValue();
    }

    public void drawMillerPlanes(P4d plane, P3d[] pts) {
      cmd("draw id " + id + "* delete");
      double d = Math.abs(plane.w);
      if (d == 0)
        return;
      P4d p = P4d.newPt(plane);
      p.w = d * 10;
      int n = 0;
      for (int i = 1;;i++) {
        p.w -= d;
        System.out.println(p);
        Lst<Object> list = vwr.getTriangulator().intersectPlane(p, pts, 0);
        if (list == null) {
          if (n == 0 && i < 20)
            continue;
          break;
        }
        createHKL(id + "_" + i, list);
       }
    }

    void createHKL(String id, Lst<Object> list) {
      //draw init draw id d1 intersection unitcell hkl {2 1 1};
      //draw thisID +PREVIOUS_MESH+
      //draw thisID d1
      //draw points 0
      //draw polygon [[Ljavajs.util.P3d;@24d841c5, [[I@2a1fbb2e]
      //draw set null
      vwr.shm.setShapeProperties(JC.SHAPE_DRAW, 
          new Object[] { "init", "hkl" },
          new Object[] { "thisID", id }, 
          new Object[] { "points", Integer.valueOf(0) },
          new Object[] { "polygon", list },
          new Object[] { "set", null }
      );
    }

  }

  /**
   *  temporary array
   * 
   */
  Object[] ret = new Object[1];  

  static P3d ptInner = P3d.new3(Double.NaN, 0, 0);


  /**
   * A convex polygon subsection of a Brillouin zone.
   * 
   */
  private static class Subzone extends BZone {

    BZone zone;

    /**
     * indicate if a subzone has points still
     */
    protected boolean isValid;

    protected Lst<P3d[]> faces;
    protected Lst<int[]> faceIndices;
    protected Lst<P3d> faceCenters;

    
    protected Lst<BZPoint> latticePts;
    protected Lst<P4d> planes;

    protected Lst<BZPoint> ptsUnused;
    protected Lst<P4d> planesUnused;


    public Subzone(BZone zone, String id, int index) {
      this.zone = zone;
      this.bz = zone.bz;
      this.id = zone.id + id + index + "_";
      this.index = index;
      this.newLatticePts = zone.newLatticePts;
      this.planes = (zone.index == 1 ? zone.newPlanes : new Lst<P4d>());
      this.latticePts = (zone.index == 1 ? zone.newLatticePts : new Lst<BZPoint>());
      this.planesUnused = new Lst<P4d>();
      this.ptsUnused = new Lst<BZPoint>();
      this.faces = new Lst<P3d[]>();
      this.faceIndices = new Lst<int[]>();
      this.faceCenters = new Lst<P3d>();
      this.volume = 0;
      this.color = zone.color;
      this.center = new P3d();
      zone.subzones.addLast(this);    
    }

    /*
     * Add the necessary planes from planes0 into the subzone.planes array 
     * and add necessary lattice points from pts0 into the subzone.latticePts array.
     * 
     * Note that subzone.latticepts is for reference only and is not used in the calculation.
     *    
     */
    void addPlanes(Lst<P4d> planes0, Lst<BZPoint> pts0, int j) {

      // designated j is inverted and introduced first

      if (j >= 0) {
        P4d pt4 = P4d.newPt(planes0.get(j));
        pt4.scale4(-1d);
        planes.addLast(pt4);
        latticePts.addLast(pts0.get(j));
      }
      int n = planes0.size();
      for (int k = 0; k < n; k++) {
        if (k != j) {
          planes.addLast(planes0.get(k));
          latticePts.addLast(pts0.get(k));
        }
      }
    }

    /**
     * Generate the polyhedra.
     * 
     * @param id
     * 
     */
    void createSubzonePolyhedron(String id) {

      id += this.id;
      // Variable pts is an array of arrays, with duplicate points. 
      // We want a flat array using .join() and a new Jmol feature that
      // uses within(distance, array) to remove the duplicate points.

      P3d[] apts = join(this.faces);
      P3d[] pts = cleanFace(apts);
      if (pts.length == 0)
        return;
      this.center = average(pts);
      this.faceIndices = new Lst<int[]>();
      for (int i = 0, n = this.faces.size(); i < n; i++) {
        //P3d[] face = this.faces.get(i);
        //this.ensureCW(this.faceCenters.get(i), face);
        this.faceIndices
            .addLast(cleanFaceIndices(this.faces.get(i), pts));
      }
      for (int i = this.faceIndices.size(); --i >= 0;) {
        if (this.faceIndices.get(i).length < 3) {
          this.faces.removeItemAt(i);
          this.faceIndices.removeItemAt(i);
          this.faceCenters.removeItemAt(i);
          this.planes.removeItemAt(i);
        }
      }
      
      // We now create the polyhedron at Gamma that goes to all these points
      // using a new Jmol feature that allows named polyhedra at specific points
      this.volume = bz.addPolyhedron(this, pts);
    }

    /**
     * Remove duplicates from face indices array.
     * 
     * @param P3ds
     * @param pts
     * @return temporary cleaned array
     */
    private int[] cleanFaceIndices(P3d[] P3ds, P3d[] pts) {
      PointIterator.withinDistPoints(0, null, pts, P3ds, null, ret);
      return (int[]) ret[0];
    }

    /**
     * Use a point iterator to exclude points that are too close together to be
     * considered to be two distinct edge points.
     * 
     * @param face
     * @return cleaned face
     */
    P3d[] cleanFace(P3d[] face) {
      PointIterator.withinDistPoints(slop, ptInner, face, null, null, ret);
      @SuppressWarnings("unchecked")
      Lst<P3d> l = (Lst<P3d>) ret[0];
      return l.toArray(new P3d[l.size()]);
    }

    static P3d average(P3d[] face) {
      P3d a = new P3d();
      for (int i = face.length; --i >= 0;)
        a.add(face[i]);
      a.scale(1d / face.length);
      return a;
    }

    private static P3d[] join(Lst<P3d[]> faces) {
      int n = 0;
      for (int i = faces.size(); --i >= 0;)
        n += faces.get(i).length;
      P3d[] pts = new P3d[n];
      n = 0;
      for (int i = faces.size(); --i >= 0;) {
        P3d[] face = faces.get(i);
        for (int j = face.length; --j >= 0;)
          pts[n++] = face[j];
      }
      return pts;
    }

      /**
       * Loop through all planes, creating a pmesh for each face. We use resolution
       * 0.001 to indicate we only want the minimum number of triangles (that is,
       * starting with two giant triangles, not a grid of small triangles). Also
       * slab each plane by all other planes to form a face.
       * 
       * @return true if total area gt 0
       */
      protected boolean getPmeshes() {
        int nPlanes = planes.size();

        // It is important to include all planes, 
        // as they may be used in later BZs. 

        // this.planes will be replaced by planesUsed 
        // this.latticePts will be replaced by ptsUsed

        Lst<P4d> planesUsed = new Lst<P4d>();
        Lst<BZPoint> ptsUsed = new Lst<BZPoint>();

        boolean haveValidPlane = false;

        for (int i = 0; i < nPlanes; i++) {
          String pid = "f" + id + i;
          boolean isValid = true;
          bz.createPMesh(pid, planes.get(i));
          for (int j = 0; j < nPlanes; j++) {
            if (j == i) {
              // don't slab by plane being slabbed
              continue;
            }
            isValid = bz.slab(pid, planes.get(j));
            if (isValid) {
              haveValidPlane = true;
            } else {
              // this i-plane has been totally excluded -- we are done here
              break;
            }
          }

          if (isValid)
            isValid = bz.slab(pid, null);

          P3d a = null;
          P3d[] face = null;
          if (isValid) {

            // The new Jmol feature pmesh.getProperty("face") allows us to extract an 
            // array of points that are only at the intersections of planes. 
            // They are in order, right-hand rule CCW cycle  

            // Here we are seeing if there are already two faces at this center,
            // indicating that we are re-entrant this time.

            face = (P3d[]) bz.getProperty(pid, "face");

            // this can be [] if a very tiny triangle was found. 
            a = average(face);
            if (i == 0 && within(slop, a, bz.bzFaceCenters).size() >= 2) {
              isValid = false;
              i = nPlanes;
            }
          }
          if (isValid) {
            this.isValid = true;
            face = cleanFace(face);
            faces.addLast(face);
            faceCenters.addLast(a);
            bz.bzFaceCenters.addLast(a);
            //System.out.println("subzone " + pid + " " + Arrays.toString(face));
            planesUsed.addLast(planes.get(i));
            ptsUsed.addLast(latticePts.get(i));
          } else if (i < nPlanes) {
            planesUnused.addLast(planes.get(i));
            ptsUnused.addLast(latticePts.get(i));
          }
          bz.clearPMesh(pid);
        }
        planes = planesUsed;
        latticePts = ptsUsed;
        if (zone.index == 1) {
          for (int i = 0; i < ptsUsed.size(); i++) {
            BZPoint bp = ptsUsed.get(i);
            System.out.println("#BZ pt[" + i + "]=" + ptsUsed.get(i));
            System.out.println("draw id d" + i + " intersection unitcell hkl " + bp.hkl() +" all;");
          }
        }
        return haveValidPlane;
      }
  }

  /**
   * overall object
   */
  protected BZ bz;

  final static double slop = 0.0001d;   
 
  final static String[] bzColors = new String[] { "red", "green", "skyblue",
      "orange", "yellow", "blue", "violet" };

  protected String id;
  protected int index;
  protected String color;
  protected P3d center;

  protected Lst<Subzone> subzones;

  protected Lst<BZPoint> newLatticePts;
  protected Lst<P4d> newPlanes;  

  protected double volume;
  
  protected void create(BZone zonePrev) {

    // Set the lattice points of interest.
    getNewLatticePoints();
    
    // Produce all of the individual sections of the Brillouin zone.
    
    getSubzones(zonePrev);
        
    // Now create the single polyhedron for this zone, consisting
    // of vertices and faces. 
    //
    // Each subzone is a set of pmeshes.
    // Each pmesh is a square surface, a pair of large triangles. 
    // Each pmesh is then slabbed by the "excluding" planes 
    // to form subzone planes.
    // We then generate the faces associated with each of those pmeshes.
    
    for (int i = 0; i < subzones.size(); i++) {
      Subzone subzone = subzones.get(i);
      if (subzone.getPmeshes())
        subzone.createSubzonePolyhedron(id);
    }
    finalizeZone();
  }

  private void getSubzones(BZone zonePrev) {

    subzones = new Lst<Subzone>();
    
    if (index == 1) {

      // for BZ1, just use the zone planes

      new Subzone(this, "", 1);
      return;
    }

    // for all others, go through all previous subzones...

    int len = zonePrev.id.length();
    for (int i = 0; i < zonePrev.subzones.size(); i++) {
      Subzone prev = zonePrev.subzones.get(i);
      String id = prev.id.substring(len);

      // Each subzone of the previous zone has a set of planes. 
      // Use all planes if this is zone 2 (the previous is BZ1).
      // Otherwise, skip the first plane, which originated two zones back

      boolean isZone2 = (zonePrev.index == 1);
      for (int j = (isZone2 ? 0 : 1); j < prev.planes.size(); j++) {
        if (!isZone2
            && within(slop, prev.faceCenters.get(j), bz.bzFaceCenters).size() > 1)
          continue;

        // each of these planes is a starting point for a new subzone

        Subzone subzone = new Subzone(this, id, isZone2 ? j + 1 : j);

        // the new subzone's initial plane (j) will be negative of the first plane
        // ...now add all the other previous planes, without inversion

        subzone.addPlanes(prev.planes, prev.latticePts, j);

        // ...now add all the previously unused planes

        subzone.addPlanes(prev.planesUnused, prev.ptsUnused, -1);

        // ...now add all the new planes

        subzone.addPlanes(newPlanes, newLatticePts, -1);
      }
    }
  }

  /**
   * Loop through all points, looking for non-excluded points using St. Olaf
   * half-distance sphere test.
   * 
   * This is the key method of the algorithm. The idea is that the points we are
   * interested in (q) that define the planes that form the faces of the nth
   * Brillouin zone are all points for which there exist no "excluding" points x
   * within the sphere that spans the line between Gamma and q.
   * 
   * <pre>
   *   \
   *     \
   *      .p..
   *    ..- \ .../
   *  .. -    \ /.
   * ..-       /\ ..         
   * G-----o--/-- q          
   * ..--    /   .. \         
   *  .. -- x    ..   \
   *    .../ ...
   *      /...
   *     /
   * 
   * </pre>
   * 
   * 
   * This sphere has a radius that is half the distance from Gamma to point q.
   * (Its center is the actual Brillouin zone face center.)
   * 
   * The right triangle G-p-q shows that for all points p ON this sphere, the
   * plane(\\\) normal to the line connecting them to G (---) passes directly
   * though q.
   * 
   * Any point such as q WITHIN this sphere have normal planes that will exclude
   * X from the Brillouin zone. (Or, more precisely, point "o" from being the
   * center of a face of the Nth Brillouin zone.
   * 
   * Thus, the points associated with the faces of the Brillouin zone will be
   * all points such that there exist no such excluding points q.
   * 
   */
  private void getNewLatticePoints() {

    newLatticePts = new Lst<BZPoint>();
    newPlanes = new Lst<P4d>();


    Lst<P3d> unusedPts = new Lst<P3d>();
    Lst<BZPoint> unusedLatticePts = new Lst<BZPoint>();
    for (int i = 0; i < bz.bzPlanePts.size(); i++) {
      P3d p = bz.bzPlanePts.get(i);
      P3d center = P3d.newP(p);
      center.scale(0.5f);

      // just a bit over so that all excluding points are found

      double radius = 0.501d * p.length();
      Lst<P3d> inSphere = within(radius, center, bz.bzPlanePts);

      // there is always at least one point within this radius -- point q itself

      //Lst<P3d> ap;
      Lst<BZPoint> al;
      if (inSphere.size() == 1) {
        al = newLatticePts;
        // plane through point p directed away from Gamma
        newPlanes.addLast(newLatticePlane(p, 1, bz.bzGamma));
      } else {
        unusedPts.addLast(p);
        al = unusedLatticePts;
      }
      al.addLast(bz.bzLatticePts.get(i));
    }

    // replace lattice and plane points with just those that have not been used

    bz.bzPlanePts = unusedPts;
    bz.bzLatticePts = unusedLatticePts;
  }

  private static P4d newLatticePlane(P3d pt2, double f, P3d bzGamma) {
    // plane(<point1>,<point2>,f)
    V3d norm = V3d.newVsub(pt2, bzGamma);
    P3d pt3 = new P3d();
    pt3.scaleAdd2(f, norm, bzGamma);
    norm.normalize();
    P4d plane = new P4d();
    MeasureD.getPlaneThroughPoint(pt3, norm, plane);
    return plane;
  }

  /**
   * 
   * @param radius
   * @param center
   * @param pts
   * @return list of points within radius of center
   */
  protected static Lst<P3d> within(double radius, P3d center, Lst<P3d> pts) {
    Lst<P3d> ret = new Lst<P3d>();
    double r2 = radius * radius;
    for (int i = 0, n = pts.size(); i < n; i++) {
      P3d pt = pts.get(i);
      if (center.distanceSquared(pt) < r2)
        ret.addLast(pt);
    }
    return ret;
  }

//  /**
//   * Testing only -- there was no problem here. All faces are 
//   * property wound.
//   * 
//   * @param a
//   * @param face
//   */
//  private void ensureCW(P3d a, P3d[] face) {
//    V3d v0 = V3d.newVsub(a, center);
//    V3d vc = new V3d();
//    boolean isOK = true;
//    for (int i = 1; i < face.length - 1; i++) {
//      V3d v1 = V3d.newVsub(face[i], face[i - 1]);
//      V3d v2 = V3d.newVsub(face[i + 1], face[i]);
//      vc.cross(v1, v2);
//      if (vc.dot(v0) > 0) {
//        continue;
//      }
//      isOK = false;
//      break;
//    }
//    if (!isOK) {
//      System.out.println("reversing");
//      for (int i = 0, n = face.length; i <= n / 2; i++) {
//        P3d p = face[i];
//        face[i] = face[n - 1 - i];
//        face[n - 1 - i] = p;
//      }
//    }
//  }

  /**
   * Finalize this Brillouin zone.
   * 
   */
  private void finalizeZone() {
    // remove 0-area subzones

    volume = 0;
    for (int i = subzones.size(); --i >= 0;) {
      Subzone subzone = subzones.get(i);
      if (subzone.isValid) {
        volume += subzone.volume;
        if (subzone.volume < 0.05) {
          System.out.println("draw id "+ "d" + subzone.id+" points " +esc(subzone.faceCenters) 
              + ";draw id "+ "dc" + subzone.id+" width 0.1 color red " + subzone.center);
        }
      } else {
       subzones.removeItemAt(i);
      }
    }

  }

  static String esc(Lst<P3d> pts) {
    String s = "[";
    String sep = "";
    for (int i = pts.size(); --i >= 0;) {
      s += sep + pts.get(i).toString();
      sep = " ";
    }
    return s + "]";
  }

  public void drawHKL(Viewer vwr, String id, P4d plane, P3d[] pts) {
    // this will be HKL plane 
    bz = new BZ(vwr, id , -2);
    bz.drawMillerPlanes(plane, pts);
  }

}
