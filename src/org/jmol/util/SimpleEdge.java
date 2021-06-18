package org.jmol.util;


/**
 * minimal bond interface for SMILES and CIP
 */
public interface SimpleEdge {

  /**
   * 
   * @return the bond order 1-4 if it is covalent, or 0 if not.
   */
  int getCovalentOrder();

  /**
   * Get the order of the bond. This could be covalent order, but also could be
   * NEAR, FAR, etc.
   * 
   * @return order
   */
  int getBondType();
  
  SimpleNode getOtherNode(SimpleNode a);
  
  /**
   * Get the respective atom, 0 or 1
   * @param i
   * @return SimpleNode
   */
  SimpleNode getAtom(int i);

  boolean isCovalent();

}
