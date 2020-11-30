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

/**
 * JavaScript path for J2S or Jmol
 * 
 * Allows for (Jmol||J2S)._inchiPath (must include trailing "/")
 */
public class InChIJS implements JmolInChI {

  static boolean isSwingJS = Viewer.isSwingJS;
  static {
    boolean j2sloaded = false;
    /**
     * @j2sNative
     * 
     *           self.InChI || (InChI = {}); j2sloaded = InChI.j2sloaded;
     *            
     *            InChI.j2sloaded = true;
     * 
     */
    {
    }
    if (!j2sloaded) {
      @SuppressWarnings("unused")
      String prefix = "";
      @SuppressWarnings("unused")
      Object app = null;
      /**
       * @j2sNative
       *            var path = '/org/jmol/inchi/js/';
       *            app = (self.J2S || Jmol)._inchiPath || (self.J2S ||
       *            Jmol)._applets.master; prefix = app._j2sPath + path; 
       *            if (prefix.indexOf("./") == 0) {prefix = prefix.substring(2);}
       *            InChI.memoryInitializerPrefixURL =
       *            prefix;
       */
      {
      }
      try {
      if (isSwingJS) {
        /**
         * @j2sNative
         * 
         *            swingjs.JSUtil.loadStaticResource$S(path + "inchi.js");
         */
        {
        }
      } else {
        /**
         * @j2sNative
         *
         *            eval(Jmol._getFileData(prefix + "inchi.js"));
         * 
         */
        {
        }
      }
      } catch (Throwable t) {
        // 
      }
      
      /**
       * @j2sNative
       *
       * 
       *
       *              InChI.fromMolfile = (InChI.cwrap && InChI.cwrap('get_inchi', 'string', ['string']));
       * 
       */
      {
      }

    }
  }
  public InChIJS() {
    // for dynamic loading
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, String options) {
    if (atoms == null || atoms.cardinality() == 0)
      return "";
    String s = "";
    try {
      if (options == null)
        options = "";
      @SuppressWarnings("unused")
      String molData = vwr.getModelExtract(atoms,  false,  false, "MOL");
      options = options.toLowerCase();
      if (options.length() > 0)
        System.err.println("JavaScript inchi.js options not implemented");
      boolean haveKey = (options.indexOf("key") >= 0);
      if (haveKey) {
        // NOT IMPLEMENTED
        options = options.replace("inchikey", "");
        options = options.replace("key", "");
        return "";
      }

      /**
       * @j2sNative
       *  if (!InChI.fromMolfile)return "";
       *  s = InChI.fromMolfile(molData);
       *  if (haveKey) {
       *   // what here?
       *  }
       */{}
//      
//      JniInchiInput in = new JniInchiInput(options);
//      in.setStructure(newJniInchiStructure(vwr, atoms));
//      String s = JniInchiWrapper.getInchi(in).getInchi();
//      return (haveKey ? JniInchiWrapper.getInchiKey(s).getKey() : s);
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
    return s;
  }


}
