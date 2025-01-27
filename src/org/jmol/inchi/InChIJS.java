/*
 * Copyright 2006-2011 Sam Adams <sea36 at users.sourceforge.net>
 *
 * This file is part of JNI-InChI.
 *
 * JNI-InChI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JNI-InChI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JNI-InChI.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jmol.inchi;

import java.util.List;
import java.util.Map;

import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.PT;

/**
 * This class implements inchi-web.wasm from InChI-SwingJS, which is adapted
 * from https://github.com/IUPAC-InChI/InChI-Web-Demo by Bob Hanson and Josh
 * Charlton 2025.01.23-2025.01.24.
 * 
 * In the case of a Jmol model for mdel to InChI, we first generate MOL file
 * data.
 * 
 * For InChI to SMILES, we use the inchi-web.c method model_from_inchi() that we
 * developed with the assistance of Frank Lange.
 * 
 * The class originally adapted Richard Apodaca's 2020 molfile-to-inchi
 * LLVM-derived Web Assembly implementation of IUPAC InChI v. 1.05. see
 * https://depth-first.com/articles/2020/03/02/compiling-inchi-to-webassembly-part-2-from-molfile-to-inchi/
 * 
 * Note that this initialiation is asynchronous. One has to either use
 * 
 * sync inchi
 * 
 * or invoke a call to generate an InChI, such as:
 * 
 * x = {none}.find("inchi")
 * 
 */
public class InChIJS extends InchiJmol implements InChIStructureProvider {

  static {
    try {
      /**
       * Import inchi-web-SwingJS.js
       * 
       * @j2sNative var j2sPath = Jmol._applets.master._j2sFullPath;
       *            Jmol.inchiPath = Jmol._applets.master._j2sFullPath +
       *            "/_ES6"; import(importPath + "/inchi-web-SwingJS.js");
       */
      {
      }
    } catch (Throwable t) {
      // 
    }

  }

  public InChIJS() {
    // for dynamic loading
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, Object molData, String options) {
    String ret = "";
    try {
      options = setParameters(options, molData, atoms, vwr);
      if (options == null)
        return "";
      options = PT
          .rep(PT.rep(options.replace('-', ' '), "  ", " ").trim(), " ", " -")
          .toLowerCase();
      if (options.length() > 0)
        options = "-" + options;
      if (molData == null)
        molData = vwr.getModelExtract(atoms, false, false, "MOL");
      if (inputInChI) {
        if (doGetSmiles || getInchiModel) {
          String json = null;
          /**
           * @j2sNative json = (Jmol.modelFromInchi ?
           *            Jmol.modelFromInchi(molData).model : ""); if (json &&
           *            !this.getInchiModel) { json = JSON.parse(json); }
           */
          {
          }
          // "getInchiModel" is just a debugging method 
          // to see the exposed InChI structure in string form
          return (doGetSmiles ? getSmiles(vwr, json, smilesOptions) : json);
        }
        // could be inchikey from inchi
        /**
         * @j2sNative ret = (Jmol.inchikeyFromInchi ?
         *            Jmol.inchikeyFromInchi(molData).inchikey : "");
         */
        {
        }
      } else {
        boolean haveKey = (options.indexOf("key") >= 0);
        if (haveKey) {
          options = options.replace("inchikey", "key");
        }
        /**
         * @j2sNative ret = (Jmol.inchiFromMolfile ?
         *            Jmol.inchiFromMolfile(molData, options).inchi : "");
         */
        {
        }
      }
    } catch (Throwable e) {
      // oddly, e will be a string, not an error
      /**
       * @j2sNative
       * 
       *            e = (e.getMessage$ ? e.getMessage$() : e);
       */
      {
      }
      System.err.println("InChIJS exception: " + e);
    }
    return ret;
  }

  @SuppressWarnings("unused")
  private Object json;

  //all javascript maps and arrays, only accessible through j2sNative.
  List<Map<String, Object>> atoms, bonds, stereo0d;
  private Map<String, Object> thisAtom;
  private Map<String, Object> thisBond;
  private Map<String, Object> thisStereo;

  private String getSmiles(Viewer vwr, Object json, String smilesOptions) {
    this.json = json;
    return new InchiToSmilesConverter(this).getSmiles(vwr, smilesOptions);
  }

  @Override
  public void initializeModelForSmiles() {
    /**
     * @j2sNative this.atoms = this.json.atoms; this.bonds = this.json.bonds;
     *            this.stereo0d = this.json.stereo0d;
     */
    {
    }
  }

  /// Atoms ///
  
  @Override
  public int getNumAtoms() {
    /**
     * @j2sNative return this.atoms.length;
     */
    {
      return 0;
    }
  }

  @Override
  public InChIStructureProvider setAtom(int i) {
    /**
     * @j2sNative this.thisAtom = this.atoms[i];
     */
    {
    }
    return this;
  }

  @Override
  public String getElementType() {
    return getString(thisAtom, "elname", "");
  }

  @Override
  public double getX() {
    return getDouble(thisAtom, "x", 0);
  }

  @Override
  public double getY() {
    return getDouble(thisAtom, "y", 0);
  }

  @Override
  public double getZ() {
    return getDouble(thisAtom, "z", 0);
  }

  @Override
  public int getCharge() {
    return getInt(thisAtom, "charge", 0);
  }

  @Override
  public int getImplicitH() {
    return getInt(thisAtom, "implicitH", 0);
  }

  @Override
  public int getIsotopicMass() {
    String sym = getElementType();
    int mass = 0;
    /**
     * @j2sNative mass = this.thisAtom["isotopicMass"] || 0;
     */
    {
    }
    return getActualMass(sym, mass);
  }

  /// Bonds ///
  
  @Override
  public int getNumBonds() {
    /**
     * @j2sNative return this.bonds.length;
     */
    {
      return 0;
    }
  }

  @Override
  public InChIStructureProvider setBond(int i) {
    /**
     * @j2sNative this.thisBond = this.bonds[i];
     */
    {
    }
    return this;
  }

  @Override
  public int getIndexOriginAtom() {
    return getInt(thisBond, "originAtom", 0);
  }

  @Override
  public int getIndexTargetAtom() {
    return getInt(thisBond, "targetAtom", 0);
  }

  @Override
  public String getInchiBondType() {
    return getString(thisBond, "type", "");
  }

  
  /// Stereo ///
  
  @Override
  public int getNumStereo0D() {
    /**
     * @j2sNative return this.stereo0d.length;
     */
    {
      return 0;
    }
  }

  @Override
  public InChIStructureProvider setStereo0D(int i) {
    /**
     * @j2sNative this.thisStereo = this.stereo0d[i];
     */
    {
    }
    return this;
  }
  
  @Override
  public String getParity() {
    return getString(thisStereo, "parity", "");
  }

  @Override
  public String getStereoType() {
    return getString(thisStereo, "stereoType", "");
  }

  @Override
  public int getCenterAtom() {
    return getInt(thisStereo, "centralAtom", -1);
  }

  @Override
  public int[] getNeighbors() {
    /**
     * @j2sNative return this.thisStereo.neighbors;
     */
    {
      return null;
    }
  }

  @SuppressWarnings("unused")
  private int getInt(Map<String, Object> map, String name, int defaultValue) {
    /**
     * @j2sNative var val = map[name]; if (val || val == 0) return val;
     */
    {
    }
    return defaultValue;
  }

  @SuppressWarnings("unused")
  private double getDouble(Map<String, Object> map, String name,
                           double defaultValue) {
    /**
     * @j2sNative var val = map[name]; if (val || val == 0) return val;
     */
    {
    }
    return defaultValue;
  }

  @SuppressWarnings("unused")
  private String getString(Map<String, Object> map, String name,
                           String defaultValue) {
    /**
     * @j2sNative var val = map[name]; if (val || val == "") return val;
     */
    {
    }
    return defaultValue;
  }  

}
