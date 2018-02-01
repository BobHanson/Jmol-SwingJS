package org.jmol.api;



import org.jmol.atomdata.RadiusData;
import javajs.util.BS;
import org.jmol.modelset.ModelSet;

import javajs.util.P3;
import javajs.util.T3;


/**
 * note: YOU MUST RELEASE THE ITERATOR
 */
public interface AtomIndexIterator {
  /**
   * @param modelSet 
   * @param modelIndex
   * @param zeroBase    an offset used in the AtomIteratorWithinSet only
   * @param atomIndex
   * @param center
   * @param distance
   * @param rd 
   */
  public void setModel(ModelSet modelSet, int modelIndex, int zeroBase, int atomIndex, T3 center, float distance, RadiusData rd);
  public void setCenter(T3 center, float distance);
  public void addAtoms(BS bsResult);
  public boolean hasNext();
  public int next();
  public float foundDistance2();
  public P3 getPosition();
  public void release();
}
