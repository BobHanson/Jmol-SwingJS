package org.jmol.inchi;

import org.iupac.InChIStructureProvider;
import org.jmol.api.JmolInChI;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.PT;

public abstract class InchiJmol implements JmolInChI, InChIStructureProvider {

  protected boolean inputInChI;
  protected String inchi;
  protected String smilesOptions;
  protected boolean doGetSmiles;
  protected boolean getInchiModel;
  protected boolean getKey;
  protected boolean optionalFixedH;

  protected String setParameters(String options, Object molData, BS atoms,
                                 Viewer vwr) {
    if (atoms == null ? molData == null : atoms.isEmpty())
      return null;
    if (options == null)
      options = "";
    String inchi = null;
    String lc = options.toLowerCase().trim();
    boolean getSmiles = (lc.indexOf("smiles") == 0);
    boolean getInchiModel = (lc.indexOf("model") == 0);
    boolean getKey = (lc.indexOf("key") >= 0);
    String smilesOptions = (getSmiles ? options : null);
    if (lc.startsWith("model/")) {
      inchi = options.substring(10);
      options = lc = "";
    } else if (getInchiModel) {
      lc = lc.substring(5);
    }
    boolean optionalFixedH = (options.indexOf("fixedh?") >= 0);
    boolean inputInChI = (molData instanceof String
        && ((String) molData).startsWith("InChI="));
    if (inputInChI) {
      inchi = (String) molData;
    } else {
      options = lc;
      if (getKey) {
        options = options.replace("inchikey", "");
        options = options.replace("key", "");
      }

      if (optionalFixedH) {
        String fxd = getInchi(vwr, atoms, molData, options.replace('?', ' '));
        options = PT.rep(options, "fixedh?", "");
        String std = getInchi(vwr, atoms, molData, options);
        inchi = (fxd != null && fxd.length() <= std.length() ? std : fxd);
      }
    }
    this.optionalFixedH = optionalFixedH;
    this.inputInChI = inputInChI;
    this.doGetSmiles = getSmiles;
    this.inchi = inchi;
    this.getKey = getKey;
    this.smilesOptions = smilesOptions;
    this.getInchiModel = getInchiModel;
    return options;
  }

  protected String getSmiles(Viewer vwr, String smilesOptions) {
    return new InchiToSmilesConverter(this).getSmiles(vwr, smilesOptions);
}

}
