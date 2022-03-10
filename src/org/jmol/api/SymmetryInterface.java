package org.jmol.api;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.Tensor;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

public interface SymmetryInterface {

  int addBioMoleculeOperation(M4 mat, boolean isReverse);

  boolean addLatticeVectors(Lst<float[]> lattvecs);

  int addSpaceGroupOperation(String xyz, int opId);

  String addSubSystemOp(String code, Matrix rs, Matrix vs, Matrix sigma);

  void calculateCIPChiralityForAtoms(Viewer vwr, BS bsAtoms);

  String[] calculateCIPChiralityForSmiles(Viewer vwr, String smiles)
      throws Exception;

  boolean checkDistance(P3 f1, P3 f2, float distance, 
                                        float dx, int iRange, int jRange, int kRange, P3 ptOffset);

  boolean createSpaceGroup(int desiredSpaceGroupIndex,
                                           String name,
                                           Object data, int modDim);

  String fcoord(T3 p);

  Object findSpaceGroup(Viewer vwr, BS atoms, String op, boolean asString);

  Lst<P3> generateCrystalClass(P3 pt0);

  P3[] getCanonicalCopy(float scale, boolean withOffset);

  P3 getCartesianOffset();

  int[] getCellRange();

  T3[] getConventionalUnitCell(String latticeType, M3 primitiveToCryst);

  boolean getCoordinatesAreFractional();

  void getEquivPointList(Lst<P3> pts, int nIgnore, String flags);

  Lst<P3> getEquivPoints(Lst<P3> pts, P3 pt, String flags);
  
  P3 getFractionalOffset();

  T3 getFractionalOrigin();

  String getIntTableNumber();

  int[] getInvariantSymops(P3 p3, int[] v0);

  AtomIndexIterator getIterator(Viewer vwr, Atom atom, BS bstoms, float radius);

  Lst<P3> getLatticeCentering();

  Object getLatticeDesignation();

  int getLatticeOp();

  char getLatticeType();

  String getMatrixFromString(String xyz, float[] temp, boolean allowScaling, int modDim);

  Lst<String> getMoreInfo();

  Matrix getOperationRsVs(int op);

  Object getPointGroupInfo(int modelIndex, String drawID,
                                           boolean asInfo, String type,
                                           int index, float scale);

  String getPointGroupName();

  Quat getQuaternionRotation(String abc);

  int getSiteMultiplicity(P3 a);

  Object getSpaceGroup();

  Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String spaceGroup, int modelIndex, boolean isFull, float[] cellParams);

  Object getSpaceGroupInfoObj(String name, SymmetryInterface cellInfo,
                              boolean isFull, boolean addNonstandard);

  String getSpaceGroupName();

  /**
   * 
   * @param type "Hall" or "HM" or "ITA"
   * @return type or null
   */
  String getSpaceGroupNameType(String type);

  M4 getSpaceGroupOperation(int i);
  
  String getSpaceGroupOperationCode(int op);

  int getSpaceGroupOperationCount();
  
  M4 getSpaceGroupOperationRaw(int i);
  
  String getSpaceGroupXyz(int i, boolean doNormalize);

  float getSpinOp(int op);

  boolean getState(SB commands);

  /**
   * 
   * @param ms
   * @param iatom
   * @param xyz
   * @param op
   * @param translation TODO
   * @param pt
   * @param pt2 a second point or an offset
   * @param id
   * @param type  T.point, T.lattice, or T.draw, T.matrix4f, T.label, T.list, T.info, T.translation, T.axis, T.plane, T.angle, T.center
   * @param scaleFactor
   * @param nth TODO
   * @param options could be T.offset
   * @return a variety of object types
   */
  Object getSymmetryInfoAtom(ModelSet ms, int iatom, String xyz, int op,
                                    P3 translation, P3 pt, P3 pt2, String id, int type, float scaleFactor, int nth, int options);

  String getSymmetryInfoStr();

  M4[] getSymmetryOperations();

  Tensor getTensor(Viewer vwr, float[] anisoBorU);

  M4 getTransform(P3 fracA, P3 fracB, boolean debug);

  SymmetryInterface getUnitCell(T3[] points, boolean setRelative, String name);

  float[] getUnitCellAsArray(boolean vectorsOnly);

  String getUnitCellInfo(boolean scaled);

  Map<String, Object> getUnitCellInfoMap();

  float getUnitCellInfoType(int infoType);

  SymmetryInterface getUnitCellMultiplied();

  T3 getUnitCellMultiplier();

  float[] getUnitCellParams();

  String getUnitCellState();

  P3[] getUnitCellVectors();

  P3[] getUnitCellVerticesNoOffset();

  T3[] getV0abc(Object def);

  boolean haveUnitCell();

  void initializeOrientation(M3 matUnitCellOrientation);

  boolean isBio();

  boolean isPolymer();

  boolean isSimple();

  boolean isSlab();

  boolean isSupercell();

  void newSpaceGroupPoint(int i, P3 atom1, P3 atom2,
                                          int transX, int transY, int transZ, M4 o);

  BS notInCentroid(ModelSet modelSet, BS bsAtoms,
                          int[] minmax);

  BS removeDuplicates(ModelSet ms, BS bs);

  V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, M3 mTemp);

  void setFinalOperations(String name, P3[] atoms,
                                          int iAtomFirst,
                                          int noSymmetryCount, boolean doNormalize, String filterSymop);

  /**
   * set symmetry lattice type using Hall rotations
   * 
   * @param latt SHELX index or character lattice character P I R F A B C S T or \0
   * 
   */
  void setLattice(int latt);

  void setOffset(int nnn);

  void setOffsetPt(T3 pt);

  SymmetryInterface setPointGroup(
                                     SymmetryInterface pointGroupPrevious,
                                     T3 center, T3[] atomset,
                                     BS bsAtoms,
                                     boolean haveVibration,
                                     float distanceTolerance, float linearTolerance, boolean localEnvOnly);

  void setSpaceGroup(boolean doNormalize);

  void setSpaceGroupName(String name);

  /**
   * 
   * @param spaceGroup ITA number, ITA full name ("48:1")
   */
  void setSpaceGroupTo(Object spaceGroup);

  SymmetryInterface setSymmetryInfo(int modelIndex, Map<String, Object> modelAuxiliaryInfo, float[] notionalCell);

  void setTimeReversal(int op, int val);

  SymmetryInterface setUnitCell(float[] params, boolean setRelative);

  void setUnitCell(SymmetryInterface uc);

  void toCartesian(T3 pt, boolean asAbsolute);

  void toFractional(T3 pt, boolean asAbsolute);

  void toFractionalM(M4 m);

  boolean toFromPrimitive(boolean toPrimitive, char type, T3[] oabc,
                          M3 primitiveToCrystal);

  P3 toSupercell(P3 fpt);

  void toUnitCell(T3 pt, T3 offset);

  boolean unitCellEquals(SymmetryInterface uc2);

  void unitize(T3 ptFrac);

}
