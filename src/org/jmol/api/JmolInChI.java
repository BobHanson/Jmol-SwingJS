package org.jmol.api;

import org.jmol.viewer.Viewer;

import javajs.util.BS;

public interface JmolInChI {

  /**
   * 
   * @param vwr
   * @param atoms
   * @param molData can be String or InputStream
   * @param options
   * @return InChI or SMILES or InChIKey or internal InChI structure details
   */
  String getInchi(Viewer vwr, BS atoms, Object molData, String options);

}
