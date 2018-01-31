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

  SimpleNode getOtherNode(SimpleNode a);

  boolean isCovalent();

}
