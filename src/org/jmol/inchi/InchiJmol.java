package org.jmol.inchi;

import org.jmol.api.JmolInChI;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.PT;

public abstract class InchiJmol implements JmolInChI {

  protected boolean inputInChI;
  protected String inchi;
  protected String smilesOptions;
  protected boolean getSmiles;
  protected boolean getInchiModel;
  protected boolean getKey;
  protected boolean fixedH;

  protected String setParameters(String options, Object molData, BS atoms, Viewer vwr) {
    String inchi = null;
    String lc = options.toLowerCase();
    boolean getSmiles = (lc.indexOf("smiles") == 0);
    boolean getInchiModel = (lc.indexOf("structure") >= 0);
    boolean getKey = (lc.indexOf("key") >= 0);
    String smilesOptions = (getSmiles ? options : null);
    if (lc.startsWith("structure/")) {
      inchi = options.substring(10);
      options = lc = "";
    }
    boolean fixedH = (options.indexOf("fixedh?") >= 0);
    boolean inputInChI = (molData instanceof String && ((String) molData).startsWith("InChI="));
    if (inputInChI) {
      inchi = (String) molData;
      if (getSmiles) {
        options = lc.replace("structure", "");
      }
    } else {
      options = lc;
      if (getKey) {
        options = options.replace("inchikey", "");
        options = options.replace("key", "");
      }

      if (fixedH) {
        String fxd = getInchi(vwr, atoms, molData, options.replace('?', ' '));
        options = PT.rep(options, "fixedh?", "");
        String std = getInchi(vwr, atoms, molData, options);
        inchi = (fxd != null && fxd.length() <= std.length() ? std : fxd);
      }
    }
    this.fixedH = fixedH;
    this.inputInChI = inputInChI;
    this.getSmiles = getSmiles;
    this.inchi = inchi;
    this.getKey = getKey;
    this.smilesOptions = smilesOptions;
    this.getInchiModel = getInchiModel;
    
    return options;
  }
}
