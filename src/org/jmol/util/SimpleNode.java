package org.jmol.util;


import javajs.util.P3;

/**
 * Just the bare minimum for CIP, and non-BioSMILES
 */
public interface SimpleNode {

  void setCIPChirality(int rs);

  int getIsotopeNumber();

  String getAtomName();

  /**
   * Get the total number of bonds, including hydrogen bonds.
   * 
   * @return number of bonds
   */
  int getBondCount();

  /**
   * Get the total number of covalent bonds, thus not including hydrogen bonds.
   * 
   * @return number of bonds
   */
  int getCovalentBondCount();

  /**
   * Get the bond array, including hydrogen bonds.
   * 
   * @return number of bonds
   */
  SimpleEdge[] getEdges();

  /**
   * 
   * @return the atomic number for this aotm
   */
  int getElementNumber();

  /**
   * 
   * @return the formal charge for this atom
   */
  int getFormalCharge();

  /**
   * 
   * @return the unique ID number associated with this atom (which in Jmol is its position in the atoms[] array
   */
  int getIndex();

  /**
   *
   * @return the mass or, if specified, the mass number
   */
  float getMass();

  /**
   *
   * @return the sum of the bond orders for this atom
   */
  int getValence();

  /**
   * 
   * @return the position of this atom
   */
  P3 getXYZ();


}
