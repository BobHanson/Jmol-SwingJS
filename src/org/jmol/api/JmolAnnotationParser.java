package org.jmol.api;

import java.util.Map;

import javajs.util.Lst;
import javajs.util.P3;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.viewer.Viewer;

public interface JmolAnnotationParser {

  String fixDSSRJSONMap(Map<String, Object> map);
  
  BS getAtomBits(Viewer vwr, String key, Object dssr, Map<String, Object> cache, int type, int modelIndex, BS bsModel);

  void getBasePairs(Viewer vwr, int modelIndex);

  String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds, boolean doReport);

  void getAtomicDSSRData(ModelSet ms, int modelIndex, float[] dssrData,
                         String dataType);

  String calculateDSSRStructure(Viewer vwr, BS bsAtoms);

  String getAnnotationInfo(Viewer vwr, SV a, String match, int type, int modelIndex);

  Lst<Object> catalogValidations(Viewer vwr, SV validation, int[] modelAtomIndices,
                            Map<String, int[]> valResMap,
                            Map<String, Integer> map, Map<String, Integer> modelMap);

  Lst<SV> initializeAnnotation(SV objAnn, int type, int modelIndex);

  Lst<Float> getAtomValidation(Viewer vwr, String type, Atom atom);

  void fixAtoms(int modelIndex, SV v, BS bsAddedMask, int type, int margin);

  String catalogStructureUnits(Viewer vwr, SV svMap, int[] modelAtomIndices,
                               Map<String, int[]> resMap, Object object,
                               Map<String, Integer> modelMap);

  void setGroup1(ModelSet ms, int modelIndex);

  P3[] getDSSRFrame(Map<String, Object> dssrNT);

}
