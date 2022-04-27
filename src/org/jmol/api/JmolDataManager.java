package org.jmol.api;

import javajs.util.SB;

import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.viewer.JmolStateCreator;
import org.jmol.viewer.Viewer;

public interface JmolDataManager {

  // data type flags
  
  public static final int DATA_TYPE_LAST = -2;
  public final static int DATA_TYPE_UNKNOWN = -1;
  public final static int DATA_TYPE_STRING = 0;
  public final static int DATA_TYPE_AD = 1;
  public final static int DATA_TYPE_ADD = 2;
  public final static int DATA_TYPE_ADDD = 3;

  // indexes into Object[] data
  
  public final static int DATA_LABEL = 0;
  public final static int DATA_VALUE = 1;
  public final static int DATA_SELECTION = 2;
  public final static int DATA_TYPE = 3;
  /**
   * optional; defaults to TRUE
   */
  public final static int DATA_SAVE_IN_STATE = 4; 
  /**
   * optional; from ModelLoader; provides the ModelSet atomProperties Map.Entry for this
   * item in case the array is ever expanded.
   */
  public final static int DATA_ATOM_PROP_ENTRY = 5;

  JmolDataManager set(Viewer vwr);

  boolean getDataState(JmolStateCreator stateCreator, SB commands);

  void clear();

  void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bsDeleted);

  Object getData(String label, BS bsSelected, int dataType);

  String getDefaultVdwNameOrData(VDW type, BS bs);

  void setData(String type, Object[] data, int dataType, int ac,
               int matchField, int matchFieldColumnCount, int dataField,
               int dataFieldColumnCount);

  Object[] createFileData(String strModel);

}
