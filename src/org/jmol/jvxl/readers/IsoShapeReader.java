/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.util.Random;


import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;

import javajs.util.Measure;
import javajs.util.SB;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

final class IsoShapeReader extends VolumeDataReader {
  // final because we are initiating static fields using static{}

  private int psi_n = 2;
  private int psi_l = 1;
  private int psi_m = 1;
  private float psi_Znuc = 1; // hydrogen
  private float sphere_radiusAngstroms;
  private int monteCarloCount;
  private Random random;

  IsoShapeReader() {}
  
  @Override
  void init(SurfaceGenerator sg) {
    initVDR(sg);
    Object o = sg.getReaderData();
    if (o instanceof Float) {
      sphere_radiusAngstroms = ((Float) o).floatValue();      
    } else {
      sphere_radiusAngstroms = 0;
      float[] data = (float[]) o;
      psi_n = (int) data[0];
      psi_l = (int) data[1];
      psi_m = (int) data[2];
      psi_Znuc = data[3];//z_eff;
      monteCarloCount = (int) data[4];
    }
  }

  private boolean allowNegative = true;

  private double[] rfactor = new double[10];
  private double[] pfactor = new double[10];

  private final static double A0 = 0.52918; //x10^-10 meters
  private final static double ROOT2 = 1.414214;
  private static final float ATOMIC_ORBITAL_ZERO_CUT_OFF = 1e-7f;

  private float radius;
  private final P3 ptPsi = new P3();

  @Override
  protected void setup(boolean isMapData) {
    volumeData.sr = this; // we will provide point data for mapping
    precalculateVoxelData = false;
    isQuiet = true;
    if (Float.isNaN(center.x))
      center.set(0, 0, 0);
    String type = "sphere";
    switch (dataType) {
    case Parameters.SURFACE_ATOMICORBITAL:
      calcFactors(psi_n, psi_l, psi_m);
      autoScaleOrbital();
      ptsPerAngstrom = 5f;
      maxGrid = 40;
      type = "hydrogen-like orbital";
      if (monteCarloCount > 0) {
        vertexDataOnly = true;
        //params.colorDensity = true;
        random = new Random(params.randomSeed);
      } else {
        isQuiet = false;
      }
      break;
    case Parameters.SURFACE_LONEPAIR:
    case Parameters.SURFACE_RADICAL:
      type = "lp";
      vertexDataOnly = true;
      radius = 0;
      ptsPerAngstrom = 1;
      maxGrid = 1;
      break;
    case Parameters.SURFACE_LOBE:
      allowNegative = false;
      calcFactors(psi_n, psi_l, psi_m);
      psi_normalization = 1;
      radius = 1.1f * eccentricityRatio * eccentricityScale;
      if (eccentricityScale > 0 && eccentricityScale < 1)
        radius /= eccentricityScale;
      ptsPerAngstrom = 10f;
      maxGrid = 21;
      type = "lobe";
      break;
    case Parameters.SURFACE_ELLIPSOID3:
      type = "ellipsoid(thermal)";
      radius = 3.0f * sphere_radiusAngstroms;
      ptsPerAngstrom = 10f;
      maxGrid = 22;
      break;
    case Parameters.SURFACE_GEODESIC:
      if (!isMapData && monteCarloCount == 0)
        break;
      type = "geodesic";
      //$FALL-THROUGH$
    case Parameters.SURFACE_ELLIPSOID2:
      if (type.equals("sphere"))
        type = "ellipsoid";
      //$FALL-THROUGH$
    case Parameters.SURFACE_SPHERE:
    default:
      radius = 1.2f * sphere_radiusAngstroms * eccentricityScale;
      ptsPerAngstrom = 10f;
      maxGrid = 22;
      break;
    }
    if (monteCarloCount == 0)
      setVolumeData();
    setHeader(type + "\n");
  }

  @Override
  protected void setVolumeData() {
    setVoxelRange(0, -radius, radius, ptsPerAngstrom, maxGrid, 0);
    setVoxelRange(1, -radius, radius, ptsPerAngstrom, maxGrid, 0);
    if (allowNegative)
      setVoxelRange(2, -radius, radius, ptsPerAngstrom, maxGrid, 0);
    else
      setVoxelRange(2, 0, radius / eccentricityRatio, ptsPerAngstrom, maxGrid, 0);
  }

  @Override
  public float getValue(int x, int y, int z, int ptyz) {
    volumeData.voxelPtToXYZ(x, y, z, ptPsi);    
    return getValueAtPoint(ptPsi, false);
  }

  @Override
  public float getValueAtPoint(T3 pt, boolean getSource) {
    ptTemp.sub2(pt, center);
    if (isEccentric)
      eccentricityMatrixInverse.rotate(ptTemp);
    if (isAnisotropic) {
      ptTemp.x /= anisotropy[0];
      ptTemp.y /= anisotropy[1];
      ptTemp.z /= anisotropy[2];
    }
    if (sphere_radiusAngstroms > 0) {
      if (params.anisoB != null) {

        return sphere_radiusAngstroms
            -

            (float) Math.sqrt(ptTemp.x * ptTemp.x + ptTemp.y * ptTemp.y
                + ptTemp.z * ptTemp.z)
            / (float) (Math.sqrt(params.anisoB[0] * ptTemp.x * ptTemp.x
                + params.anisoB[1] * ptTemp.y * ptTemp.y + params.anisoB[2]
                * ptTemp.z * ptTemp.z + params.anisoB[3] * ptTemp.x * ptTemp.y
                + params.anisoB[4] * ptTemp.x * ptTemp.z + params.anisoB[5]
                * ptTemp.y * ptTemp.z));
      }
      return sphere_radiusAngstroms
          - (float) Math.sqrt(ptTemp.x * ptTemp.x + ptTemp.y * ptTemp.y
              + ptTemp.z * ptTemp.z);
    }
    float value = (float) hydrogenAtomPsi(ptTemp);
    if (Math.abs(value) < ATOMIC_ORBITAL_ZERO_CUT_OFF)
      value = 0;
    return (allowNegative || value >= 0 ? value : 0);
  }

  private void setHeader(String line1) {
    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append(line1);
    if (sphere_radiusAngstroms > 0) {
      jvxlFileHeaderBuffer.append(" rad=").appendF(sphere_radiusAngstroms);
    } else {
      jvxlFileHeaderBuffer.append(" n=").appendI(psi_n).append(", l=").appendI(
          psi_l).append(", m=").appendI(psi_m).append(" Znuc=").appendF(psi_Znuc)
          .append(" res=").appendF(ptsPerAngstrom).append(" rad=")
          .appendF(radius);
    }
    jvxlFileHeaderBuffer.append(isAnisotropic ? " anisotropy=(" + anisotropy[0]
        + "," + anisotropy[1] + "," + anisotropy[2] + ")\n" : "\n");
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
  }

  private final static float[] fact = new float[20];
  static {
    fact[0] = 1;
    for (int i = 1; i < 20; i++)
      fact[i] = fact[i - 1] * i;
  }

  private double psi_normalization = 1 / (2 * Math.sqrt(Math.PI));

  private void calcFactors(int n, int el, int m) {
    int abm = Math.abs(m);
    double NnlLnl = Math.pow(2 * psi_Znuc / n / A0, 1.5)
        * Math.sqrt(fact[n - el - 1] * fact[n + el] / 2 / n);
    //double Lnl = fact[n + el] * fact[n + el];
    double Plm = Math.pow(2, -el) * fact[el] * fact[el + abm]
        * Math.sqrt((2 * el + 1) * fact[el - abm] / 2 / fact[el + abm]);

    for (int p = 0; p <= n - el - 1; p++)
      rfactor[p] = NnlLnl / fact[p] / fact[n - el - p - 1]
          / fact[2 * el + p + 1];
    for (int p = abm; p <= el; p++)
      pfactor[p] = Math.pow(-1, el - p) * Plm / fact[p] / fact[el + abm - p]
          / fact[el - p] / fact[p - abm];
  }

  private double aoMax;
  private double aoMax2;
  private double angMax2;
  private V3 planeU;
  private V3 planeV;
  private P3 planeCenter;
  private float planeRadius;
  
  private void autoScaleOrbital() {
    aoMax = 0;
    float rmax = 0;
    aoMax2 = 0;
    float rmax2 = 0;
    double d;
    if (params.distance == 0) {
      for (int ir = 0; ir < 1000; ir++) {
        float r = ir / 10f;
        d = Math.abs(radialPart(r));
        if (Logger.debugging)
          Logger.debug("R\t" + r + "\t" + d);
        if (d >= aoMax) {
          rmax = r;
          aoMax = d;
        }
        d *= d * r * r;
        if (d >= aoMax2) {
          rmax2 = r;
          aoMax2 = d;
        }
      }
    } else {
      aoMax = Math.abs(radialPart(params.distance));
      aoMax2 = aoMax * aoMax * params.distance * params.distance;
      rmax = rmax2 = params.distance;
    }
    Logger.info("Atomic Orbital radial max = " + aoMax + " at " + rmax);
    Logger.info("Atomic Orbital r2R2 max = " + aoMax2 + " at " + rmax2);

    if (monteCarloCount >= 0) {
      angMax2 = 0;
      for (float ang = 0; ang < 180; ang += 1) {
        double th = ang / (2 * Math.PI);
        d = Math.abs(angularPart(th, 0, 0));
        if (Logger.debugging)
          Logger.debug("A\t" + ang + "\t" + d);
        if (d > angMax2) {
          angMax2 = d;
        }
      }
      angMax2 *= angMax2;
      if (psi_m != 0) {
        // if psi_m not 0, we include sqrt2 from creating real counterpart
        // of the imaginary solution: 1/sqrt2(psi_a +/- psi_b)
        // you get, for example, 1/sqrt2(2 cos phi) = sqrt2 * cos phi
        // which has a max of sqrt2.
        angMax2 *= 2; 
      }
      Logger.info("Atomic Orbital phi^2theta^2 max = " + angMax2);
      // (we don't apply the final 4pi here because it is just a constant)
    }
    double min;
    if (params.cutoff == 0) {
      min = (monteCarloCount > 0 ? aoMax * 0.01f : 0.01f);
    } else if (monteCarloCount > 0) {
      aoMax = Math.abs(params.cutoff);
      min = aoMax * 0.01f;
    } else {
      min = Math.abs(params.cutoff / 2);
      // ISSQUARED means cutoff is in terms of psi*2, not psi
      if (params.isSquared)
        min = Math.sqrt(min / 2);
    }
    float r0 = 0;
    for (int ir = 1000; --ir >= 0; ir -= 1) {
      float r = ir / 10f;
      d = Math.abs(radialPart(r));
      if (d >= min) {
        r0 = r;
        break;
      }
    }
    radius = r0 + (monteCarloCount == 0 ? 1 : 0);
    if (isAnisotropic) {
      float aMax = 0;
      for (int i = 3; --i >= 0;)
        if (anisotropy[i] > aMax)
          aMax = anisotropy[i];
      radius *= aMax;
    }
    Logger.info("Atomic Orbital radial extent set to " + radius
        + " for cutoff " + params.cutoff);
    if (params.thePlane != null && monteCarloCount > 0) {
      // get two perpendicular unit vectors in the plane.
      planeCenter = new P3();
      planeU = new V3();
      Measure.getPlaneProjection(center, params.thePlane, planeCenter, planeU);
      planeU.set(params.thePlane.x, params.thePlane.y, params.thePlane.z);
      planeU.normalize();
      planeV = V3.new3(1, 0, 0);
      if (Math.abs(planeU.dot(planeV)) > 0.5f)
        planeV.set(0, 1, 0);
      planeV.cross(planeU, planeV);
      planeU.cross(planeU, planeV);
      aoMax2 = 0;
      d = center.distance(planeCenter);
      if (d < radius) {
        planeRadius = (float) Math.sqrt(radius * radius - d * d);
        int ir = (int) (planeRadius * 10);
        for (int ix = -ir; ix <= ir; ix++)
          for (int iy = -ir; iy <= ir; iy++) {
            ptPsi.setT(planeU);
            ptPsi.scale(ix / 10f);
            ptPsi.scaleAdd2(iy / 10f, planeV, ptPsi);
            d = hydrogenAtomPsi(ptPsi);
            // we need an approximation  of the max value here
            d = Math.abs(hydrogenAtomPsi(ptPsi));
            if (d > aoMax2)
              aoMax2 = d;
          }
        if (aoMax2 < 0.001f) // must be a node
          aoMax2 = 0;
        else
          aoMax2 *= aoMax2;
      }
    }
  }

  private double radialPart(double r) {
    double rho = 2d * psi_Znuc * r / psi_n / A0;
    double sum = 0;
    for (int p = 0; p <= psi_n - psi_l - 1; p++)
      sum += Math.pow(-rho, p) * rfactor[p];
    return Math.exp(-rho / 2) * Math.pow(rho, psi_l) * sum;
  }

  private double rnl;

  private double hydrogenAtomPsi(P3 pt) {
    // ref: http://www.stolaf.edu/people/hansonr/imt/concept/schroed.pdf
    double x2y2 = pt.x * pt.x + pt.y * pt.y;
    rnl = radialPart(Math.sqrt(x2y2 + pt.z * pt.z));
    double ph = Math.atan2(pt.y, pt.x);
    double th = Math.atan2(Math.sqrt(x2y2), pt.z);
    double theta_lm_phi_m = angularPart(th, ph, psi_m);
    return rnl * theta_lm_phi_m;
  }

  private double angularPart(double th, double ph, int m) {
    // note: we are factoring in 1 / 2 sqrt PI starting with Jmol 12.1.52
    double cth = Math.cos(th);
    double sth = Math.sin(th);
    boolean isS = (m == 0 && psi_l == 0);
    int abm = Math.abs(m);
    double sum = 0;
    if (isS)
      sum = pfactor[0];
    else
      for (int p = abm; p <= psi_l; p++)
        sum += (p == abm ? 1 : Math.pow(1 + cth, p - abm))
            * (p == psi_l ? 1 : Math.pow(1 - cth, psi_l - p)) * pfactor[p];
    double theta_lm = (abm == 0 ? sum : Math.abs(Math.pow(sth, abm)) * sum);
    double phi_m;
    if (m == 0)
      phi_m = 1;
    else if (m > 0)
      phi_m = Math.cos(m * ph) * ROOT2;
    else
      phi_m = Math.sin(m * ph) * ROOT2;
    return (Math.abs(phi_m) < 0.0000000001 ? 0 : theta_lm * phi_m * psi_normalization);
  }

  private boolean surfaceDone;
  private int nTries;

  private void createMonteCarloOrbital() {
    if (surfaceDone || aoMax2 == 0 || params.distance > radius)
      return;
    boolean isS = (psi_m == 0 && psi_l == 0);
    surfaceDone = true;
    float value;
    float rave = 0;
    nTries = 0;
    for (int i = 0; i < monteCarloCount; nTries++) {
      // we do Pshemak's idea here -- force P(r2R2), then pick a random
      // point on the sphere for that radius
      if (params.thePlane == null) {
        double r;
        if (params.distance == 0) {
          r = random.nextDouble() * radius;
          double rp = r * radialPart(r);
          if (rp * rp <= aoMax2 * random.nextDouble())
            continue;
        } else {
          r = params.distance;
        }
        double u = random.nextDouble();
        double v = random.nextDouble();
        double theta = 2 * Math.PI * u;
        double cosPhi = 2 * v - 1;
        if (!isS) {
          double phi = Math.acos(cosPhi);
          double ap = angularPart(phi, theta, psi_m);
          if (ap * ap <= angMax2 * random.nextDouble())
            continue;
        }
        //http://mathworld.wolfram.com/SpherePointPicking.html
        double sinPhi = Math.sin(Math.acos(cosPhi));
        double x = r * Math.cos(theta) * sinPhi;
        double y = r * Math.sin(theta) * sinPhi;
        double z = r * cosPhi;
        //x = r; y = r2R2/aoMax2 * 10; z = 0;
        ptPsi.set((float) x, (float) y, (float) z);
        ptPsi.add(center);
        value = getValueAtPoint(ptPsi, false);
      } else {
        ptPsi.setT(planeU);
        ptPsi.scale(random.nextFloat() * planeRadius * 2 - planeRadius);
        ptPsi.scaleAdd2(random.nextFloat() * planeRadius * 2 - planeRadius,
            planeV, ptPsi);
        ptPsi.add(planeCenter);
        value = getValueAtPoint(ptPsi, false);
        if (value * value <= aoMax2 * random.nextFloat())
          continue;
      }
      rave += ptPsi.distance(center);
      addVC(ptPsi, value, 0, true);
      i++;
    }
    if (params.distance == 0)
      Logger.info("Atomic Orbital mean radius = " + rave / monteCarloCount
          + " for " + monteCarloCount + " points (" + nTries + " tries)");
  }

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    switch (params.dataType) {
    case Parameters.SURFACE_ATOMICORBITAL:
      if (monteCarloCount <= 0)
        break;
      createMonteCarloOrbital();
      return;
    case Parameters.SURFACE_LONEPAIR:
    case Parameters.SURFACE_RADICAL:
      ptPsi.set(0, 0, eccentricityScale / 2);
      eccentricityMatrixInverse.rotate(ptPsi);
      ptPsi.add(center);
      addVC(center, 0, 0, true);
      addVC(ptPsi, 0, 0, true);
      addTriangleCheck(0, 0, 0, 0, 0, false, 0);
      return;
    case Parameters.SURFACE_GEODESIC:
      if (!isMapData) {
        createGeodesic();
        return;
      }
    }
    readSurfaceDataVDR(isMapData);
  }

  private void createGeodesic() {
    MeshSurface ms = MeshSurface.getSphereData(4);
    T3[] pts = ms.altVertices;
    for (int i = 0; i < pts.length; i++) {
      P3 pt = P3.newP(pts[i]);
      pt.scale(params.distance);
      pt.add(center);
      addVC(pt, 0, i, false);
    }
    int[][] faces = ms.pis;
    for (int i = 0; i < faces.length; i++) {
      int[] face = faces[i];
      addTriangleCheck(face[0], face[1], face[2], 7, 7, false, 0);
    }    
  }

}
