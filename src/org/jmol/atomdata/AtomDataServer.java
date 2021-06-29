package org.jmol.atomdata;

import java.io.BufferedInputStream;
import java.util.Map;


import org.jmol.api.AtomIndexIterator;
import javajs.util.BS;

import javajs.util.T3;



public interface AtomDataServer {
  public AtomIndexIterator getSelectedAtomIterator(BS bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased, boolean isMultiModel);

  public void setIteratorForAtom(AtomIndexIterator iterator, int atomIndex, float distance);

  public void setIteratorForPoint(AtomIndexIterator iter, int modelIndex, T3 pt,
                                  float maxDistance);

  public void fillAtomData(AtomData atomData, int mode);
  
  public BufferedInputStream getBufferedInputStream(String fullPathName);

  public void log(String msg);
  
  public float evalFunctionFloat(Object func, Object params, float[] values);

  Map<String, Object> readCifData(String fileName, Object rdrOrStringData, String type);

}
