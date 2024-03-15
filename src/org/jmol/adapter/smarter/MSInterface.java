package org.jmol.adapter.smarter;

import java.util.Map;

import org.jmol.adapter.smarter.XtalSymmetry.FileSymmetry;

import javajs.util.Lst;
import javajs.util.Matrix;
import javajs.util.P3d;


/**
 * Modulated Structure Reader Interface
 * 
 */
public interface MSInterface {

  // methods called from org.jmol.adapters.readers.xtal.JanaReader
  
  void addModulation(Map<String, double[]> map, String id, double[] pt, int iModel);

  void addSubsystem(String code, Matrix w);

  void finalizeModulation();

  double[] getMod(String key);

  int initialize(AtomSetCollectionReader r, int modDim) throws Exception;

  void setModulation(boolean isPost, XtalSymmetry.FileSymmetry symmetry) throws Exception;

  XtalSymmetry.FileSymmetry getAtomSymmetry(Atom a, XtalSymmetry.FileSymmetry symmetry);

  void setMinMax0(P3d minXYZ0, P3d maxXYZ0);

  XtalSymmetry.FileSymmetry getSymmetryFromCode(String spaceGroupOperationCode);

  boolean addLatticeVector(Lst<double[]> lattvecs, String substring) throws Exception;

  Map<String, double[]> getModulationMap();

  char getModType(String key);

  double[] getQCoefs(String key);


}
