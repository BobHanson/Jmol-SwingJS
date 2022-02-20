package org.jmol.viewer;

import java.util.Map;

import javajs.util.SB;

import javajs.util.BS;
import org.jmol.shape.AtomShape;

public abstract class JmolStateCreator {

  abstract void setViewer(Viewer vwr);

  abstract String getStateScript(String type, int width, int height);

  abstract String getSpinState(boolean b);
  
  abstract String getLightingState(boolean isAll);
  
  abstract String getModelState(SB sfunc, boolean isAll,
                               boolean withProteinStructure);

  abstract String getCommands(Map<String, BS> htDefine, Map<String, BS> htMore,
                     String select);

  abstract String getAllSettings(String prefix);

  abstract String getFunctionCalls(String selectedFunction);

  abstract String getAtomicPropertyState(int taintCoord, BS bsSelected);

  abstract void getAtomicPropertyStateBuffer(SB commands, int type,
                                    BS bs, String name, float[] data);

  abstract int undoMoveAction(int action, int n);

  abstract void undoMoveActionClear(int taintedAtom, int type, boolean clearRedo);

  abstract void getInlineData(SB loadScript, String strModel, boolean isAppend,
                     Integer appendToModelIndex, String defaultLoadFilter);

  public abstract String getAtomShapeState(AtomShape shape); // called by Polyhedra
}
