package org.jmol.api;

import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.Point3fi;
import org.jmol.viewer.Viewer;

import javajs.util.A4d;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.M34d;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.Matrix;
import javajs.util.P3d;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;

public interface SymmetryInterface {

  int addBioMoleculeOperation(M4d mat, boolean isReverse);

  int addSpaceGroupOperation(String xyz, int opId);

  void calculateCIPChiralityForAtoms(BS bsAtoms);

  String[] calculateCIPChiralityForSmiles(String smiles)
      throws Exception;

  boolean checkPeriodic(P3d pt, double packing, double packing2);

  Object convertTransform(String transform, M4d trm);

  Object findSpaceGroup(BS atoms, String xyzList, double[] unitCellParams, T3d origin, 
                        T3d[] oabc, int flags);

  boolean fixUnitCell(double[] unitCellParams);
  
  String geCIFWriterValue(String type);

  Lst<P3d> generateCrystalClass(P3d pt0);

  M4d[] getAdditionalOperations();

  int getAdditionalOperationsCount();

  P3d[] getCanonicalCopy(double scale, boolean allow2D);

  P3d getCartesianOffset();

  int[] getCellRange();

  double getCellWeight(P3d pt);

  Atom getConstrainableEquivAtom(Atom a);

  boolean getCoordinatesAreFractional();

  int getDimensionality();

  void getEquivPointList(int nIgnore, String flags, M4d[] opsCtr, double packing, Lst<Point3fi> pts);

  Lst<Point3fi> getEquivPoints(Point3fi pt, String flags, double packing);

  int getFinalOperationCount();

  P3d getFractionalOffset(boolean onlyIfFractional);

  P3d getFractionalOrigin();

  int getGroupType();
  
  String getIntTableIndex();
  
  String getIntTableNumber();

  String getIntTableTransform();

  int[] getInvariantSymops(P3d p3, int[] v0);

  AtomIndexIterator getIterator(Atom atom, BS bstoms, double radius);

  Lst<P3d> getLatticeCentering();

  Object getLatticeDesignation();

  int getLatticeOp();

  char getLatticeType();

  Lst<String> getMoreInfo();

  Matrix getOperationRsVs(int op);

  int getPeriodicity();

  Object getPointGroupInfo(int modelIndex, String drawID,
                           boolean asInfo, String type,
                           int index, double scale);

  String getPointGroupName();

  double getPrecision();

  Qd getQuaternionRotation(String abc);

  int getSiteMultiplicity(P3d a);

  Object getSpaceGroup();

  String getSpaceGroupClegId();

  Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String spaceGroup, int modelIndex, boolean isFull, double[] cellParams);

  Object getSpaceGroupInfoObj(String name, Object params,
                              boolean isFull, boolean addNonstandard);

  String getSpaceGroupJmolId();

  Object getSpaceGroupJSON(String name, String data, int index);

  String getSpaceGroupName();

  M4d getSpaceGroupOperation(int i);

  int getSpaceGroupOperationCount();

  String getSpaceGroupXyzOriginal(int i, boolean doNormalize);

  int getSpinIndex(int op);

  int getSpinOp(int op);

  SymmetryInterface getSpinSym();

  boolean getState(ModelSet ms, int modelIndex, SB commands);

  Object getSubgroupJSON(String nameFrom, String nameTo, int index1, int index2, int flags, Map<String, Object> retMap, Lst<Object> retLst);

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
   * @param oplist 
   * @return a variety of object types
   */
  Object getSymmetryInfoAtom(ModelSet ms, int iatom, String xyz, int op,
                                    P3d translation, P3d pt, P3d pt2, String id, int type, double scaleFactor, int nth, int options, int[] oplist);

  String getSymmetryInfoStr();

  M4d[] getSymmetryOperations();

  String[] getSymopList(boolean normalize);

  M4d getTransform(P3d fracA, P3d fracB, boolean debug);

  SymmetryInterface getUnitCell(T3d[] points, boolean setRelative, String name);
  
   double[] getUnitCellAsArray(boolean vectorsOnly);

  P3d getUnitCellCenter();

  String getUnitCellDisplayName();

  String getUnitCellInfo(boolean scaled);

  Map<String, Object> getUnitCellInfoMap();

  double getUnitCellInfoStr(String type);

  double getUnitCellInfoType(int infoType);


  SymmetryInterface getUnitCellMultiplied();

  T3d getUnitCellMultiplier();

  double[] getUnitCellParams();

  String getUnitCellState();

  P3d[] getUnitCellVectors();

  P3d[] getUnitCellVerticesNoOffset();

  T3d[] getV0abc(Object def, M4d m);

  Object getWyckoffPosition(P3d pt, String letter);

  boolean haveUnitCell();

  void initializeOrientation(M3d matUnitCellOrientation);

  boolean isBio();

  boolean isPolymer();

  boolean isSimple();

  boolean isSlab();

  boolean isSupercell();

  boolean isSymmetryCell(SymmetryInterface sym);

  boolean isWithinUnitCell(P3d pt, double x, double y, double z, double packing);

  void newSpaceGroupPoint(P3d pt, int i, M4d o,
                                          int transX, int transY, int transZ, P3d retPoint);

  BS notInCentroid(ModelSet modelSet, BS bsAtoms,
                          int[] minmax);

  BS removeDuplicates(ModelSet ms, BS bs, boolean highPrec);

  M4d saveOrRetrieveTransformMatrix(M4d trmat);

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

  void setOffsetPt(T3d pt);

  SymmetryInterface setPointGroup(
                                  SymmetryInterface pointGroupPrevious,
                                  T3d center, T3d[] atomset,
                                  BS bsAtoms,
                                  boolean haveVibration,
                                  double distanceTolerance, double linearTolerance, int maxAtoms, boolean localEnvOnly);

  void setSpaceGroup(boolean doNormalize);

  void setSpaceGroupName(String name);

  /**
   * 
   * @param spaceGroup ITA number, ITA full name ("48:1")
   */
  void setSpaceGroupTo(Object spaceGroup);

  void setSpinAxisAngle(A4d aa);

  void setUnitCell(SymmetryInterface uc);

  SymmetryInterface setUnitCellFromParams(double[] params, boolean setRelative, double slop);

  /**
   * for Viewer.getSymStatic only
   * @param vwr
   * @param id TODO
   * @return this
   */
  SymmetryInterface setViewer(Viewer vwr, String id);
  
  String staticCleanTransform(String trm);

  Object staticConvertOperation(String xyz, M34d matrix, String labels);

  M4d staticGetMatrixTransform(String cleg, Object retLstOrMap);

  String staticGetTransformABC(M4d transform, boolean normalize);

  String staticToRationalXYZ(P3d fPt, String sep);

  String staticTransformSpaceGroup(BS bs, String cleg, Object paramsOrUC,
                                   SB sb);

  void toCartesian(T3d pt, boolean ignoreOffset);

  void toFractional(T3d pt, boolean ignoreOffset);

  boolean toFromPrimitive(boolean toPrimitive, char type, T3d[] oabc,
                          M3d primitiveToCrystal);

  P3d toSupercell(P3d fpt);

  void toUnitCell(T3d pt, T3d offset);

  boolean unitCellEquals(SymmetryInterface uc2);

  void unitize(T3d ptFrac);

}
