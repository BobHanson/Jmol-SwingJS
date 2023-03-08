package org.jmol.api;



import org.jmol.modelset.Atom;
import org.jmol.util.Node;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;

public interface SmilesMatcherInterface {

  // Truly public
  
  public int areEqual(String smiles1, String smiles2) throws Exception;

  public abstract int[][] find(String pattern,/* ...in... */String smiles,
                                int flags) throws Exception;

  public abstract String getLastException();

  public abstract String getMolecularFormula(String pattern, boolean isSearch, boolean isEmpirical) throws Exception;

  public abstract String getRelationship(String smiles1, String smiles2) throws Exception;

  public abstract String reverseChirality(String smiles) throws Exception;

  public abstract String polyhedronToSmiles(Node center, int[][] faces, int atomCount, P3d[] points, int flags, String details) throws Exception;

  
  // Internal -- Jmol use only -- 
  
  public abstract BS getSubstructureSet(Object pattern, Object target,
                                            int ac, BS bsSelected,
                                            int flags) throws Exception;

  public abstract BS[] getSubstructureSetArray(String pattern,
                                                   Node[] atoms,
                                                   int ac,
                                                   BS bsSelected,
                                                   BS bsAromatic,
                                                   int flags) throws Exception;

  public abstract int[][] getCorrelationMaps(String pattern, Node[] atoms,
                                             int ac, BS bsSelected,
                                             int flags) throws Exception;

  public abstract void getMMFF94AtomTypes(String[] smarts, Node[] atoms, int ac,
                                           BS bsSelected, Lst<BS> bitSets, Lst<BS>[] vRings) throws Exception;

  public abstract String getSmiles(Node[] atoms, int ac, BS bsSelected,
                                      String bioComment, int flags) throws Exception;

  public abstract String cleanSmiles(String smiles);

  public int[][] getMapForJME(String jme, Atom[] at, BS bsAtoms);

  Node[] getAtoms(String target) throws Exception;

  String getSmilesFromJME(String jmeFile);

  int[] hasStructure(String smarts, String[] smilesSet, int flags)
      throws Exception;

  Object compileSmartsPattern(String pattern) throws Exception;

  Object compileSearchTarget(Node[] atoms, int atomCount, BS bitSet);

}
