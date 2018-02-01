package org.jmol.api;


import java.util.Map;

import javajs.util.BS;

import javajs.util.Lst;
import javajs.util.OC;

import org.jmol.viewer.Viewer;

public interface JmolPropertyManager {

  void setViewer(Viewer vwr);

  Object getProperty(String returnType, String infoType, Object paramInfo);

  String getDefaultPropertyParam(int propertyID);

  int getPropertyNumber(String name);

  boolean checkPropertyParameter(String name);

  Object extractProperty(Object property, Object args, int pt, Lst<Object> v2, boolean isCompiled);

  Map<String, Object> getModelInfo(Object atomExpression);

  Map<String, Object> getLigandInfo(Object atomExpression);

  String getModelFileInfo(BS visibleFramesBitSet);

  String getChimeInfo(int tok, BS selectionSet);

  String getModelExtract(BS atomBitSet, boolean doTransform, boolean isModelKit,
                         String type, boolean allTrajectories);

  String getPdbAtomData(BS bs, OC out, boolean asPQR, boolean doTransform, boolean allTrajectories);

  String getPdbData(int modelIndex, String type, BS bsA, Object[] parameters,
                    OC oc, boolean addStructure);

  String getModelCml(BS bs, int nAtomsMax, boolean addBonds, boolean doTransform, boolean allTrajectories);

  String getAtomData(String atomExpression, String type, boolean allTrajectories);

  String fixJMEFormalCharges(BS bsAtoms, String s);


}
