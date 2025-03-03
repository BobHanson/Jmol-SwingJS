package org.iupac;

public interface InChIStructureProvider {
  
  void initializeModelForSmiles();
  
  //InChIStructureProvider Setters
  InChIStructureProvider setAtom(int i);
  InChIStructureProvider setBond(int i);
  InChIStructureProvider setStereo0D(int i);
  
  int getNumAtoms();
  int getNumBonds();
  int getNumStereo0D();
  
  //Atom Methods
  String getElementType();
  double getX();
  double getY();
  double getZ();
  int getCharge();
  int getImplicitH();
  
  /**
   * from inchi_api.h
   * 
   * #define ISOTOPIC_SHIFT_FLAG 10000
   * 
   * add to isotopic mass if isotopic_mass = (isotopic mass - average atomic
   * mass)
   * 
   * AT_NUM isotopic_mass;
   * 
   * 0 => non-isotopic; isotopic mass or ISOTOPIC_SHIFT_FLAG + mass - (average
   * atomic mass)
   * 
   * @return inchi's value of average mass
   */
  int getIsotopicMass();
 
  //Bond Methods
  int getIndexOriginAtom();
  int getIndexTargetAtom();
  String getInchiBondType();
  
  //Stereo Methods
  String getParity();
  String getStereoType();
  int getCenterAtom();
  int[] getNeighbors();

}
