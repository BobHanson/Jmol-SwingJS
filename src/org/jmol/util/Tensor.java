/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-04-26 01:52:03 -0500 (Sat, 26 Apr 2008) $
 * $Revision: 9314 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import javajs.util.Eigen;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.T3d;
import javajs.util.V3d;

/**
 * @author Bob Hanson hansonr@stolaf.edu 6/30/2013
 * @author Simone Sturniolo 
 */
public class Tensor {

  // factors that give reasonable first views of ellipsoids.
  
  private static final double ADP_FACTOR = Math.sqrt(0.5) / Math.PI;
  private static final double MAGNETIC_SUSCEPTIBILITY_FACTOR = 0.01d;
  private static final double INTERACTION_FACTOR = 0.04d;
  private static final double CHEMICAL_SHIFT_ANISOTROPY_FACTOR = 0.01d;
  
  private static EigenSort tSort; // used for sorting eigenvector/values


  // base data:
  
  public String id;
  public String type;
  public int iType = TYPE_OTHER;
  
  // type is an identifier that the reader/creator delivers:
  //
  // adp    -- crystallographic displacement parameters 
  //           - "erature factors"; t.forThermalEllipsoid = true
  //           - either anisotropic (ADP) or isotropic (IDP)
  // iso      -- isotropic displacement parameters; from org.jmol.symmetry.UnitCell 
  //           - changed to "adp" after setting t.isIsotropic = true
  // ms       -- magnetic susceptibility
  // isc      -- NMR interaction tensors
  //           - will have both atomIndex1 and atomIndex2 defined when
  //           - incorporated into a model
  // charge   -- Born Effective Charge tensor
  // TLS-U    -- Translation/Libration/Skew tensor (anisotropic)
  // TLS-R    -- Translation/Libration/Skew tensor (residual)
  // csa      -- Chemical Shift Anisotropy tensor
  
  private static final String KNOWN_TYPES = 
    ";iso........" +
    ";adp........" +
    ";tls-u......" +
    ";tls-r......" +
    ";ms........." +
    ";efg........" +
    ";isc........" +
    ";charge....." +
    ";quadrupole." +
    ";raman......" +
    ";csa........";
  private static int getType(String type) {
    int pt = type.indexOf("_");
    if (pt >= 0)
      type = type.substring(0, pt);
    pt = KNOWN_TYPES.indexOf(";" + type.toLowerCase() + ".");
    return (pt < 0 ? TYPE_OTHER : pt / 11); 
  }

  // these may be augmented, but the order should be kept the same within this list 
  // no types  < -1, because these are used in Ellipsoids.getAtomState() as bs.get(iType + 1)
  
  public static final int TYPE_OTHER      = -1;
  public static final int TYPE_ISO        = 0;
  public static final int TYPE_ADP        = 1;
  public static final int TYPE_TLS_U      = 2;
  public static final int TYPE_TLS_R      = 3;
  public static final int TYPE_MS         = 4;
  public static final int TYPE_EFG        = 5;
  public static final int TYPE_ISC        = 6;
  public static final int TYPE_CHARGE     = 7;
  public static final int TYPE_QUADRUPOLE = 8;
  public static final int TYPE_RAMAN      = 9;
  public static final int TYPE_CSA        = 10;

  public double[][] asymMatrix;
  public double[][] symMatrix;    
  public V3d[] eigenVectors;
  public double[] eigenValues;
  public double[] parBorU;  // unmodulated

  // derived type-based information, Jmol-centric, for rendering:
  
  public String altType; // "0" "1" "2"

  // altType is somewhat of a legacy - just lets you use 
  
  //  ellipsoid SET 1
  //  ellipsoid SET 2
  //   etc...
  
  public boolean isIsotropic; // just rendered as balls, not special features
  public boolean forThermalEllipsoid;
  public int eigenSignMask = 7; // signs of eigenvalues; bits 2,1,0 set to 1 if > 0
  private double typeFactor = 1; // an ellipsoid scaling factor depending upon type
  private boolean sortIso;

  // added only after passing
  // the tensor to ModelLoader:
  
  public int modelIndex;
  public int atomIndex1 = -1;
  public int atomIndex2 = -1;
  
  public boolean isModulated;
  public boolean isUnmodulated;

  private static final String infoList = 
    ";............." + ";eigenvalues.." + ";eigenvectors."
  + ";asymmatrix..." + ";symmatrix...." + ";value........"
  + ";isotropy....." + ";anisotropy..." + ";asymmetry...." 
  + ";eulerzyz....." + ";eulerzxz....." + ";quaternion..." 
  + ";indices......" + ";string......." + ";type........."
  + ";id..........." + ";span........." + ";skew.........";
  
  static private int getInfoIndex(String infoType) {
    if (infoType.charAt(0) != ';')
      infoType = ";" + infoType + ".";
    return infoList.indexOf(infoType) / 14;
  }

  public static boolean isFloatInfo(String infoType) {
    switch (getInfoIndex(infoType)) {
    default:
      return false;
    case 5: // value
    case 6: // isotropy
    case 7: // anisotropy
    case 8: // asymmetry
    case 16: // span
    case 17: // skew
      return true;
    }
  }

  /**
   * returns an object of the specified type, including "eigenvalues",
   * "eigenvectors", "asymmetric", "symmetric", "trace", "indices", and "type"
   * 
   * @param infoType
   * @return Object or null
   */
  public Object getInfo(String infoType) {
    switch (getInfoIndex(infoType)) {
    default:
      // dump all key/value pairs
      Map<String, Object> info = new Hashtable<String, Object>();
      String[] s = PT.getTokens(PT.replaceWithCharacter(infoList, ";.", ' ').trim());
      Arrays.sort(s);
      for (int i = 0; i < s.length; i++) {
        Object o = getInfo(s[i]);
        if (o != null)
          info.put(s[i], o);
      }
      return info;
      
    case 1:
      return eigenValues;
    case 2:
      P3d[] list = new P3d[3];
      for (int i = 0; i < 3; i++)
        list[i] = P3d.newP(eigenVectors[i]);
      return list;
      
      
    case 3:
      if (asymMatrix == null)
        return null;
      double[] a = new double[9];
      int pt = 0;
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
          a[pt++] = asymMatrix[i][j];
      return M3d.newA9(a);
    case 4: 
      if (symMatrix == null)
        return null;
      double[] b = new double[9];
      int p2 = 0;
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
          b[p2++] = symMatrix[i][j];
      return M3d.newA9(b);
    case 5: // value
      return Double.valueOf(eigenValues[2]);
    case 6: // isotropy
      return Double.valueOf(isotropy());
    case 7: // anisotropy
      // Anisotropy, defined as Vzz-(Vxx+Vyy)/2
      return Double.valueOf(anisotropy()); 
    case 8: // asymmetry
      // Asymmetry, defined as (Vyy-Vxx)/(Vzz - Viso)
      return Double.valueOf(asymmetry());
 
      
    case 9: // eulerzyz
      return ((Qd) getInfo("quaternion")).getEulerZYZ();
    case 10: // eulerzxz
      return ((Qd) getInfo("quaternion")).getEulerZXZ();
    case 11: // quaternion
      return Qd.getQuaternionFrame(null, eigenVectors[0],
          eigenVectors[1]);
      
      
    case 12: 
      return new int[] { modelIndex, atomIndex1, atomIndex2 };
    case 13:
      return this.toString();
    case 14:
      return type;
      
    case 15:
      return id;
    
    case 16:
      return Double.valueOf(span());
    case 17:
      return Double.valueOf(skew());
    
    }
  }

  //  isotropy = (e2 + e1 + e0)/3
  //
  //                |                  |        |
  //                |                  |        |
  //               e2                 e1       e0
  //                               |
  //                              iso

  /**
   * isotropy = average of eigenvalues
   * 
   * @return isotropy
   */
  public double isotropy() {
    return (eigenValues[0] + eigenValues[1] + eigenValues[2]) / 3;
  }

  // span = |e2 - e0|
  //
  //                |                  |        |
  //                |                  |        |
  //                e2                 e1       e0
  //                |---------------------------|
  //                            span      
  //
    
  /**
   * width of the signal; |e2 - e0|
   * 
   * @return unitless; >= 0
   */
  public double span() {
    return Math.abs(eigenValues[2] - eigenValues[0]);  
  }

  // skew = 3 (e1 - iso) / span
  //
  //                |                  |        |
  //                |              iso |        |
  //                e2              |  e1       e0
  //                      e1 - iso  |->          
  //                                     
  //                |---------------------------|
  //                            span      
  //
  //  or 0 if 0/0
    
  /**
   * a measure of asymmetry.
   * 
   * @return range [-1, 1]
   */
  public double skew() {
    return (span() == 0 ? 0 : 3 * (eigenValues[1] - isotropy()) / span());
  }


  // anistropy = e2 - (e1 + e0)/2
  //
  //                |                  |        |
  //                |                  |        |
  //               e2                 e1       e0
  //                <----------------------|              
  //                        anisotropy
    
  /**
   * anisotropy = directed distance from (center of two closest) to (the furthest)
   * @return unitless number
   */
  public double anisotropy() {
    return eigenValues[2] - (eigenValues[0] + eigenValues[1]) / 2;
  }

  //  reduced anisotropy = e2 - iso = anisotropy * 2/3
  //
  //                |                  |        |
  //                |                  |        |
  //                e2            iso  e1       e0
  //                <----------------------|              
  //                        anisotropy
  //                <--------------|
  //                  reduced anisotropy
    
  /**
   * reduced anisotropy = largest difference from isotropy
   * (may be negative)
   * 
   * @return unitless number
   * 
   */
  public double reducedAnisotropy() {
    return anisotropy() * 2 / 3;  // = eigenValues[2]-iso();
  }

  // asymmetry = (e1 - e0)/(e2 - iso)
  //
  //                |                  |        |
  //                |                  |        |
  //                e2            iso  e1       e0
  //                <--------------|   <--------|
  //                   (e2 - iso)       (e1 - e0)
  //  or 0 when 0/0

  /**
   * asymmetry = deviation from a symmetric tensor
   * 
   * @return range [0,1]
   */
  public double asymmetry() {
    return span() == 0 ? 0 : (eigenValues[1] - eigenValues[0]) / reducedAnisotropy();
  }

  public Tensor copyTensor() {
    Tensor t = new Tensor();
    t.setType(type);
    t.eigenValues = eigenValues;
    t.eigenVectors = eigenVectors;
    t.asymMatrix = asymMatrix;
    t.symMatrix = symMatrix;
    t.eigenSignMask = eigenSignMask;
    t.modelIndex = modelIndex;
    t.atomIndex1 = atomIndex1;
    t.atomIndex2 = atomIndex2;
    t.parBorU = parBorU;
    t.id = id;
    return t;
  }

  /**
   * Although this constructor is public, to be a valid tensor, one must invoke one of the
   * "setFrom" methods. These had been static, but it turns out when that is the case, then
   * JavaScript versions cannot be modularized to omit this class along with Eigen. So the
   * general full constructor would look something like:
   * 
   *   new Tensor().setFrom...(....)
   *   
   *   
   * 
   */
  public Tensor() {}
  
  /**
   * Standard constructor for QM tensors
   * 
   * @param asymmetricTensor
   * @param type
   * @param id 
   * @return this
   */
  public Tensor setFromAsymmetricTensor(double[][] asymmetricTensor, String type, String id) {
    double[][] a = new double[3][3];    
    for (int i = 3; --i >= 0;)
      for (int j = 3; --j >= 0;)
        a[i][j] = asymmetricTensor[i][j];
    
    // symmetrize matrix
    if (a [0][1] != a[1][0]) {
      a[0][1] = a[1][0] = (a[0][1] + a[1][0])/2;
    }
    if (a[1][2] != a[2][1]) {
      a[1][2] = a[2][1] = (a[1][2] + a[2][1])/2;
    }
    if (a[0][2] != a[2][0]) {
      a[0][2] = a[2][0] = (a[0][2] + a[2][0])/2;
    }
    M3d m = new M3d();
    double[] mm = new double[9];
    for (int i = 0, p = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        mm[p++] = a[i][j];
    m.setA(mm);

    V3d[] vectors = new V3d[3];
    double[] values = new double[3];
    new Eigen().setM(a).fillDoubleArrays(vectors, values);

// this code was used for testing only
//  Eigen e = new Eigen().setM(a);  
//  V3d[] evec = eigen.getEigenVectors3();
//  V3 n = new V3();
//  V3 cross = new V3();
//  for (int i = 0; i < 3; i++) {
//    n.setT(evec[i]);
//    m.rotate(n);
//    cross.cross(n, evec[i]);
//    //Logger.info("v[i], n, n x v[i]"+ evec[i] + " " + n + " "  + cross);
//    n.setT(evec[i]);
//    n.normalize();
//    cross.cross(evec[i], evec[(i + 1) % 3]);
//    //Logger.info("draw id eigv" + i + " " + Escape.eP(evec[i]) + " color " + (i ==  0 ? "red": i == 1 ? "green" : "blue") + " # " + n + " " + cross);
//  }
//  Logger.info("eigVal+vec (" + eigen.d[0] + " + " + eigen.e[0]
//      + ")\n             (" + eigen.d[1] + " + " + eigen.e[1]
//      + ")\n             (" + eigen.d[2] + " + " + eigen.e[2] + ")");

    newTensorType(vectors, values, type, id);
    asymMatrix = asymmetricTensor;
    symMatrix = a;
    this.id = id;
    return this;
  }

  /**
   * Standard constructor for charge and iso.
   * 
   * @param eigenVectors
   * @param eigenValues
   * @param type
   * @param id 
   * @param t 
   * @return this
   */
  public Tensor setFromEigenVectors(T3d[] eigenVectors,
                                            double[] eigenValues, String type, String id, Tensor t) {
    double[] values = new double[3];
    V3d[] vectors = new V3d[3];
    for (int i = 0; i < 3; i++) {
      vectors[i] = V3d.newV(eigenVectors[i]);
      values[i] = eigenValues[i];
    }    
    newTensorType(vectors, values, type, id);
    if (t != null) {
      isModulated = t.isModulated;
      isUnmodulated = t.isUnmodulated;
      parBorU = t.parBorU;
    }
    return this;
  }

  /**
   * Standard constructor for ellipsoids based on axes 
   * 
   * @param axes
   * @return Tensor
   */
  public Tensor setFromAxes(V3d[] axes) {
    eigenValues = new double[3];
    eigenVectors = new V3d[3];
    for (int i = 0; i < 3; i++) {
      eigenVectors[i] = V3d.newV(axes[i]);
      eigenValues[i] = axes[i].length();
      if (eigenValues[i] == 0)
        return null;
      eigenVectors[i].normalize();
    }
    if (Math.abs(eigenVectors[0].dot(eigenVectors[1])) > 0.0001f
        || Math.abs(eigenVectors[1].dot(eigenVectors[2])) > 0.0001f 
        || Math.abs(eigenVectors[2].dot(eigenVectors[0])) > 0.0001f)
      return null;
    setType("other");
    sortAndNormalize();
    return this;
  }

  /**
   * standard constructor for thermal ellipsoids convention beta
   * (see http://www.iucr.org/iucr-top/comm/cnom/adp/finrepone/finrepone.html)
   * 
   * @param coefs
   * @param id  
   * @return this
   */
  public Tensor setFromThermalEquation(double[] coefs, String id) {
    eigenValues = new double[3];
    eigenVectors = new V3d[3];
    this.id = (id == null ? "coefs=" + Escape.eAD(coefs) : id);
    // assumes an ellipsoid centered on 0,0,0
    // called by UnitCell for the initial creation from PDB/CIF ADP data    
    double[][] mat = new double[3][3];
    mat[0][0] = coefs[0]; //XX
    mat[1][1] = coefs[1]; //YY
    mat[2][2] = coefs[2]; //ZZ
    mat[0][1] = mat[1][0] = coefs[3] / 2; //XY
    mat[0][2] = mat[2][0] = coefs[4] / 2; //XZ
    mat[1][2] = mat[2][1] = coefs[5] / 2; //YZ
    new Eigen().setM(mat).fillDoubleArrays(eigenVectors, eigenValues);
    setType("adp");
    sortAndNormalize();
    return this;
  }

  /**
   * Note that type may be null here to skip type initialization
   * and allow later setting of type; this should be used with care.
   * 
   * @param type
   * @return "this" for convenience only
   */
  public Tensor setType(String type) {
    if (this.type == null || type == null)
      this.type = type;
    if (type != null)
      processType();
    return this;
  }

  /**
   * Returns a factored eigenvalue; thermal ellipsoids use sqrt(abs(eigenvalue)) for
   * ellipsoid axes; others use just use abs(eigenvalue); all cases get factored by
   * typeFactor
   * 
   * @param i
   * @return factored eigenvalue
   */
  public double getFactoredValue(int i) {
    double f = Math.abs(eigenValues[i]);
    return (forThermalEllipsoid ? Math.sqrt(f) : f) * typeFactor;
  }

  public void setAtomIndexes(int index1, int index2) {
    atomIndex1 = index1;
    atomIndex2 = index2;
  }

  public boolean isSelected(BS bsSelected, int iAtom) {
    return (iAtom >= 0 ? (atomIndex1 == iAtom || atomIndex2 == iAtom)
        : bsSelected.get(atomIndex1)
            && (atomIndex2 < 0 || bsSelected.get(atomIndex2)));
  }

  /**
   * common processing of eigenvectors.
   * 
   * @param vectors
   * @param values
   * @param type
   * @param id 
   */
  private void newTensorType(V3d[] vectors, double[] values, String type, String id) {
    eigenValues = values;
    eigenVectors = vectors;
    for (int i = 0; i < 3; i++)
      eigenVectors[i].normalize();
    setType(type);
    this.id = id;
    sortAndNormalize();
    eigenSignMask = (eigenValues[0] >= 0 ? 1 : 0)
        + (eigenValues[1] >= 0 ? 2 : 0) + (eigenValues[2] >= 0 ? 4 : 0);
  }

  /**
   * Sets typeFactor, altType, isIsotropic, forThermalEllipsoid;
   * type "iso" changed to "" here.
   * 
   */
  private void processType() {
    
    forThermalEllipsoid = false;
    isIsotropic = false;
    altType = null;
    typeFactor = 1;
    sortIso = false;
    
    switch (iType = getType(type)) {
    case TYPE_ISO:
      forThermalEllipsoid = true;
      isIsotropic = true;
      altType = "1";
      type = "adp";
      break;
    case TYPE_ADP:
      forThermalEllipsoid = true;
      typeFactor = ADP_FACTOR;
      altType = "1";
      break;
    case TYPE_CSA:
      sortIso = true;
      typeFactor = CHEMICAL_SHIFT_ANISOTROPY_FACTOR;
      break;
    case TYPE_MS:
      sortIso = true;
      typeFactor = MAGNETIC_SUSCEPTIBILITY_FACTOR;
      break;
    case TYPE_EFG:
      sortIso = true;
      break;
    case TYPE_ISC:
      sortIso = true;
      typeFactor = INTERACTION_FACTOR;
      break;
    case TYPE_TLS_R:
      altType = "2";
      break;
    case TYPE_TLS_U:
      altType = "3";
      break;
    case TYPE_CHARGE:
    case TYPE_QUADRUPOLE:
      break;
    }
  }

  /**
   * The expression:
   * 
   * |sigma_3 - sigma_iso| >= |sigma_1 - sigma_iso| >= |sigma_2 - sigma_iso|
   * 
   * simply sorts the values from largest to smallest or smallest to largest,
   * depending upon the direction of the asymmetry, always setting the last
   * value to be the farthest from the mean. We use a simpler form here:
   * 
   * |sigma_3 - sigma_1| >= |sigma_3 - sigma_2| >= |sigma_2 - sigma_1|
   * 
   * which amounts to the same thing and is prettier. (Think about it!)
   * 
   */
  private void sortAndNormalize() {
    // first sorted 3 2 1, then check for iso-sorting
    Object[] o = new Object[] {
        new Object[] { V3d.newV(eigenVectors[0]), Double.valueOf(eigenValues[0]) },
        new Object[] { V3d.newV(eigenVectors[1]), Double.valueOf(eigenValues[1]) },
        new Object[] { V3d.newV(eigenVectors[2]), Double.valueOf(eigenValues[2]) } };
    Arrays.sort(o, getEigenSort());
    for (int i = 0; i < 3; i++) {
      int pt = i;
      eigenVectors[i] = (V3d) ((Object[]) o[pt])[0];
      eigenValues[i] = ((Double) ((Object[]) o[pt])[1]).doubleValue();
    }
    if (sortIso
        && eigenValues[2] - eigenValues[1] < eigenValues[1] - eigenValues[0]) {
      V3d vTemp = eigenVectors[0];
      eigenVectors[0] = eigenVectors[2];
      eigenVectors[2] = vTemp;
      double f = eigenValues[0];
      eigenValues[0] = eigenValues[2];
      eigenValues[2] = f;
    }
    for (int i = 0; i < 3; i++)
      eigenVectors[i].normalize();
  }

  public boolean isEquiv(Tensor t) {
    if (t.iType != iType) 
      return false;
    double f = Math.abs(eigenValues[0] + eigenValues[1] + eigenValues[2]);
    for (int i = 0; i < 3; i++)
      if (Math.abs(t.eigenValues[i] - eigenValues[i]) / f > 0.0003f)
        return false;
    return true;
  }
  private static Comparator<? super Object> getEigenSort() {
    return (tSort == null ? (tSort = new EigenSort()) : tSort);
  }

  @Override
  public String toString() {
    return (type + " " + modelIndex + " " + atomIndex1 + " " + atomIndex2 + "\n"
  + (eigenVectors == null ? ""  + eigenValues[0]
      : eigenVectors[0] + "\t" + eigenValues[0] + "\t"  + "\n" 
      + eigenVectors[1] + "\t" + eigenValues[1] + "\t" + "\n"
        + eigenVectors[2] + "\t" + eigenValues[2] + "\t" + "\n"));
  }


}
