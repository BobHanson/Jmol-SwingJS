package org.jmol.api;

import javajs.util.SB;

import org.jmol.c.VDW;
import javajs.util.BS;
import org.jmol.viewer.JmolStateCreator;
import org.jmol.viewer.Viewer;

public interface JmolDataManager {

  public static final int DATA_TYPE_LAST = -2;
  public final static int DATA_TYPE_UNKNOWN = -1;
  public final static int DATA_TYPE_STRING = 0;
  public final static int DATA_TYPE_AF = 1;
  public final static int DATA_TYPE_AFF = 2;
  public final static int DATA_TYPE_AFFF = 3;
  // indexes into Object[] data
  public final static int DATA_LABEL = 0;
  public final static int DATA_VALUE = 1;
  public final static int DATA_SELECTION = 2;
  public final static int DATA_TYPE = 3;
  public final static int DATA_SAVE_IN_STATE = 4; // optional; defaults to TRUE

  JmolDataManager set(Viewer vwr);

  boolean getDataState(JmolStateCreator stateCreator, SB commands);

  void clear();

  void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bsDeleted);

  Object getData(String label, BS bsSelected, int dataType);

  //float getDataFloatAt(String label, int atomIndex);

  String getDefaultVdwNameOrData(VDW type, BS bs);

  void setData(String type, Object[] data, int dataType, int ac,
               int matchField, int matchFieldColumnCount, int dataField,
               int dataFieldColumnCount);

  Object[] createFileData(String strModel);

}
