package org.jmol.adapter.smarter;

import java.util.Map;

import org.jmol.api.SymmetryInterface;

import javajs.util.Lst;
import javajs.util.Matrix;
import javajs.util.P3;


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

  void setModulation(boolean isPost, SymmetryInterface symmetry) throws Exception;

  SymmetryInterface getAtomSymmetry(Atom a, SymmetryInterface symmetry);

  void setMinMax0(P3 minXYZ0, P3 maxXYZ0);

  SymmetryInterface getSymmetryFromCode(String spaceGroupOperationCode);

  boolean addLatticeVector(Lst<float[]> lattvecs, String substring) throws Exception;

  Map<String, double[]> getModulationMap();

  char getModType(String key);

  double[] getQCoefs(String key);


}
