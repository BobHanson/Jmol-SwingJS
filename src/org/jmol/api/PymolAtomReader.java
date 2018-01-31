package org.jmol.api;

public interface PymolAtomReader {

  int getUniqueID(int iAtom);

  float getVDW(int iAtom);

  int getCartoonType(int iAtom);

  int getSequenceNumber(int i);

  boolean compareAtoms(int iPrev, int i);
  

}
