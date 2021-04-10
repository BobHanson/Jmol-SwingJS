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

import org.jmol.api.JmolInChI;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.PT;

/**
 * This class adapts Richard Apodaca's 2020 molfile-to-inchi LLVM-derived Web Assembly 
 * implementation of IUPAC InChI v. 1.05. 
 * 
 * see https://depth-first.com/articles/2020/03/02/compiling-inchi-to-webassembly-part-2-from-molfile-to-inchi/
 * 
 * Note that this initialiation is asynchronous. One has to either use 
 * 
 * sync inchi
 * 
 * or invoke a call to generate an InChI, such as:
 * 
 * x = {none}.find("inchi")
 * 
 * 
 * 
 */
public class InChIJS implements JmolInChI {

  static {
    @SuppressWarnings("unused")
    String wasmPath = "/_WASM";
    @SuppressWarnings("unused")
    String es6Path = "/_ES6";
    try {
      /**
       * We pass into molfile-to-inchi.js app.inchiPath for the fetch of molfile-to-inchi.wasm
       * but for some reason, the import() path is one directory off from the fetch() pathin J2S
       * 
       * @j2sNative 
       *            var j2sPath = Jmol._applets.master._j2sFullPath;
       *            //
       *            Jmol.inchiPath = j2sPath + wasmPath;
       *            //
       *            var importPath = j2sPath + es6Path;
       *            //
       *            import(importPath + "/molfile-to-inchi.js");
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
  public String getInchi(Viewer vwr, BS atoms, String molData, String options) {
    if (atoms == null ? molData == null : atoms.cardinality() == 0)
      return "";
    String ret = "";
    try {
      if (options == null)
        options = "";
      options = PT.rep(PT.rep(options.replace('-',' '), "  ", " ").trim(), " ", " -").toLowerCase();
      if (options.length() > 0)
        options = "-" + options;
      if (molData == null)
        molData = vwr.getModelExtract(atoms,  false,  false, "MOL");
      if (molData.startsWith("InChI=")) {
        /**
         * @j2sNative
         *  ret = (Jmol.inchiToInchiKey ? Jmol.inchiToInchiKey(molData) : "");
         */{}

      } else {
        boolean haveKey = (options.indexOf("key") >= 0);
        if (haveKey) {
          options = options.replace("inchikey", "key");
        }
        /**
         * @j2sNative
         *  ret = (Jmol.molfileToInChI ? Jmol.molfileToInChI(molData, options) : "");
         */{}
      }
    } catch (Throwable e) {
      // oddly, e will be a string, not an error
      /**
       * @j2sNative
       * 
       *   e = (e.getMessage$ ? e.getMessage$() : e);
       */
      {}
        System.err.println("InChIJS exception: " + e);
    }
    return ret;
  }


}
