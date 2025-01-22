package org.jmol.inchi;

import org.jmol.smiles.SmilesAtom;

import net.sf.jniinchi.JniInchiAtom;

interface InChIStructureProvider {
  //JniInchiAtom Methods
  String getElementType();
  int getNumAtoms();
  int getNumBonds();
  int getImplicitH();
  int getCharge();
  double getX();
  double getY();
  double getZ();
  
  //InChIStructureProvider Setters
  InChIStructureProvider setAtom(int i);
  InChIStructureProvider setBond(int i);
  InChIStructureProvider setStereo0D(int i);
  
  //JniInchiBond Methods
  Integer getOriginAtom();
  Integer getTargetAtom();
  int getJmolBondType();
  
  //JniInchiStereo Methods
  String getParity();
  String getStereoType();
  int getNumStereo0D();
  
  //Others
  void setAn();
  void setCa();
  int get_an_length();
  int get_an_map_index(int i);
  boolean caIsNull();
  

}
