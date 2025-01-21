package org.jmol.inchi;

interface InChIStructureProvider {

  int getNumAtoms();
  int getNumBonds();
  int getImplicitH();
  InChIStructureProvider setAtom(int i);
  double getX();

}
