package org.jmol.api;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.P3d;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.T3d;
import javajs.util.V3d;

public interface SymmetryInterface {

  int addSpaceGroupOperation(String xyz, int opId);

  String addSubSystemOp(String code, Matrix rs, Matrix vs, Matrix sigma);

  void calculateCIPChiralityForAtoms(Viewer vwr, BS bsAtoms);

  String[] calculateCIPChiralityForSmiles(Viewer vwr, String smiles)
      throws Exception;

  int addBioMoleculeOperation(M4d mat, boolean isReverse);

  boolean addLatticeVectors(Lst<float[]> lattvecs);

  boolean checkDistance(P3d f1, P3d f2, double distance, 
                                        double dx, int iRange, int jRange, int kRange, P3d ptOffset);

  boolean createSpaceGroup(int desiredSpaceGroupIndex,
                                           String name,
                                           Object data, int modDim);

  Object findSpaceGroup(Viewer vwr, BS atoms, String op, boolean asString);

  int[] getCellRange();

  T3d[] getConventionalUnitCell(String latticeType, M3d primitiveToCryst);

  boolean getCoordinatesAreFractional();

  void getEquivPointList(Lst<P3> pts, int nIgnore, String flags);

  P3d getFractionalOffset();

  String getIntTableNumber();

  int getLatticeOp();

  char getLatticeType();

  String getMatrixFromString(String xyz, float[] temp, boolean allowScaling, int modDim);

  Lst<String> getMoreInfo();

  Matrix getOperationRsVs(int op);

  String getPointGroupName();

  Quat getQuaternionRotation(String abc);

  int getSiteMultiplicity(P3d a);

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

  M4d getSpaceGroupOperation(int i);
  
  String getSpaceGroupOperationCode(int op);

  int getSpaceGroupOperationCount();
  
//  M4d getSpaceGroupOperationRaw(int i);
  
  String getSpaceGroupXyz(int i, boolean doNormalize);

  int getSpinOp(int op);

  boolean getState(ModelSet ms, int modelIndex, SB commands);

  String getSymmetryInfoStr();

  M4d[] getSymmetryOperations();

  Tensor getTensor(Viewer vwr, double[] anisoBorU);

  M4d getTransform(P3d fracA, P3d fracB, boolean debug);

  SymmetryInterface getUnitCelld(T3d[] points, boolean setRelative, String name);

  SymmetryInterface getUnitCell(T3[] points, boolean setRelative, String name);

  double[] getUnitCellAsArray(boolean vectorsOnly);

  String getUnitCellInfo(boolean scaled);

  Map<String, Object> getUnitCellInfoMap();

  double getUnitCellInfoType(int infoType);

  SymmetryInterface getUnitCellMultiplied();

  double[] getUnitCellParams();

  String getUnitCellState();

  P3d[] getUnitCellVectors();

  P3[] getUnitCellVerticesNoOffset();

  T3d[] getV0abc(Object def);

  boolean haveUnitCell();

  boolean isBio();

  boolean isPolymer();

  boolean isSimple();

  boolean isSlab();

  boolean isSupercell();

  void newSpaceGroupPoint(P3d pt, int i, M4d o,
                                          int transX, int transY, int transZ, P3d retPoint);

  BS notInCentroid(ModelSet modelSet, BS bsAtoms,
                          int[] minmax);

  BS removeDuplicates(ModelSet ms, BS bs);

  V3d[] rotateAxes(int iop, V3d[] axes, P3d ptTemp, M3d mTemp);

  void setFinalOperations(int dim, String name, P3d[] atoms,
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

  void setSpaceGroup(boolean doNormalize);

  void setSpaceGroupName(String name);

  /**
   * 
   * @param spaceGroup ITA number, ITA full name ("48:1")
   */
  void setSpaceGroupTo(Object spaceGroup);

  SymmetryInterface setSymmetryInfo(int modelIndex, Map<String, Object> modelAuxiliaryInfo, double[] notionalCell);

  void setTimeReversal(int op, int val);

  SymmetryInterface setUnitCell(double[] params, boolean setRelative);

  void setUnitCell(SymmetryInterface uc);

  void toCartesian(T3d pt, boolean asAbsolute);

  void toFractional(T3d pt, boolean asAbsolute);
  
  void toFractionalM(M4d m);

  boolean toFromPrimitive(boolean toPrimitive, char type, T3d[] oabc,
                          M3d primitiveToCrystal);

  void toUnitCellD(T3d pt, T3d offset);

  void toUnitCellRnd(T3d pt, T3d offset);

  boolean unitCellEquals(SymmetryInterface uc2);

  void unitize(T3d ptFrac);

  void initializeOrientation(M3d matUnitCellOrientation);

  String fcoord(T3 p);

  // floats
  
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

  void toUnitCell(T3 pt, T3 offset);


  P3 toSupercell(P3 fpt);


  T3 getUnitCellMultiplier();

  void toCartesianF(T3 pt, boolean asAbsolute);

  void toFractionalF(T3 pt, boolean asAbsolute);

  P3 getCartesianOffset();

  P3[] getCanonicalCopy(float scale, boolean withOffset);

  Lst<P3> getLatticeCentering();

  Object getLatticeDesignation();

  Object getPointGroupInfo(int modelIndex, String drawID,
                           boolean asInfo, String type,
                           int index, float scale);

  SymmetryInterface setPointGroup(
                                  SymmetryInterface pointGroupPrevious,
                                  T3 center, T3[] atomset,
                                  BS bsAtoms,
                                  boolean haveVibration,
                                  float distanceTolerance, float linearTolerance, boolean localEnvOnly);

  int[] getInvariantSymops(P3 p3, int[] v0);

  float[] getUnitCellParamsF();

  Lst<P3> getEquivPoints(Lst<P3> pts, P3 pt, String flags);
  
  Lst<P3> generateCrystalClass(P3 pt0);

  P3 getFractionalOrigin();

  AtomIndexIterator getIterator(Viewer vwr, Atom atom, BS bstoms, float radius);


}
