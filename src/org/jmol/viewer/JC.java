/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-01-25 01:10:13 -0600 (Thu, 25 Jan 2018) $
 * $Revision: 21811 $

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.Elements;
import org.jmol.util.Logger;

import javajs.util.PT;
import javajs.util.V3d;

public final class JC {

  public final static int AXIS_A = 6;
  public final static String[] axisLabels = { "+X", "+Y", "+Z", null, null, null, 
      "a", "b", "c", 
      "X", "Y", "Z", null, null, null,
      "X", null, "Z", null, "(Y)", null};

  public final static String[] axesTypes = {"a", "b", "c", "x", "y", "z"};

  public static final String NBO_TYPES = ";" + "AO;;;;" // 31
      + "PNAO;;" // 32
      + "NAO;;;" // 33
      + "PNHO;;" // 34
      + "NHO;;;" // 35
      + "PNBO;;" // 36
      + "NBO;;;" // 37
      + "PNLMO;" // 38
      + "NLMO;;" // 39
      + "MO;;;;" // 40
      + "NO;;;;" // 41
      + ";;;;;;" // 42
      + "PRNBO;" // 43
      + "RNBO;;" // 44
      + ";;;;;;" // 45
      + "";

  public static int getNBOTypeFromName(String nboType) {
    int pt = NBO_TYPES.indexOf(";" + nboType + ";");
    return (pt < 0 ? pt : pt / 6 + 31);
  }

  // requires 8 bits for rule and type:        rrrba*SR
  public final static int CIP_CHIRALITY_UNKNOWN = 0;
  public final static int CIP_CHIRALITY_R_FLAG = 1;
  public final static int CIP_CHIRALITY_S_FLAG = 2;
  public final static int CIP_CHIRALITY_CANTDETERMINE = 7;

  public final static int CIP_CHIRALITY_NONE = CIP_CHIRALITY_R_FLAG
      | CIP_CHIRALITY_S_FLAG;
  public final static int CIP_CHIRALITY_EZ_FLAG = 1 << 2; //  100
  public final static int CIP_CHIRALITY_PSEUDO_FLAG = 1 << 3; //  1000
  public final static int CIP_CHIRALITY_AXIAL_FLAG = 1 << 4; //  10000
  public final static int CIP_CHIRALITY_NAME_MASK = 7 << 5; //11100000
  public final static int CIP_CHIRALITY_NAME_OFFSET = 5;

  public final static int CIP_CHIRALITY_seqCis_FLAG = CIP_CHIRALITY_R_FLAG
      | CIP_CHIRALITY_EZ_FLAG;
  public final static int CIP_CHIRALITY_seqTrans_FLAG = CIP_CHIRALITY_S_FLAG
      | CIP_CHIRALITY_EZ_FLAG;

  public final static int CIP_CHIRALITY_seqcis_FLAG = CIP_CHIRALITY_seqCis_FLAG
      | CIP_CHIRALITY_PSEUDO_FLAG;
  public final static int CIP_CHIRALITY_seqtrans_FLAG = CIP_CHIRALITY_seqTrans_FLAG
      | CIP_CHIRALITY_PSEUDO_FLAG;

  public final static int CIP_CHIRALITY_M_FLAG = CIP_CHIRALITY_R_FLAG
      | CIP_CHIRALITY_AXIAL_FLAG;
  public final static int CIP_CHIRALITY_P_FLAG = CIP_CHIRALITY_S_FLAG
      | CIP_CHIRALITY_AXIAL_FLAG;

  public final static int CIP_CHIRALITY_r_FLAG = CIP_CHIRALITY_R_FLAG
      | CIP_CHIRALITY_PSEUDO_FLAG;
  public final static int CIP_CHIRALITY_s_FLAG = CIP_CHIRALITY_S_FLAG
      | CIP_CHIRALITY_PSEUDO_FLAG;
  public final static int CIP_CHIRALITY_m_FLAG = CIP_CHIRALITY_M_FLAG
      | CIP_CHIRALITY_PSEUDO_FLAG;
  public final static int CIP_CHIRALITY_p_FLAG = CIP_CHIRALITY_P_FLAG
      | CIP_CHIRALITY_PSEUDO_FLAG;

  public static String getCIPChiralityName(int flags) {
    switch (flags) {
    // going with ACD Labs idea E/Z and e/z, where e/z is the special case -- NOT pseudochiral 
    case CIP_CHIRALITY_seqcis_FLAG:
      return "Z";
    case CIP_CHIRALITY_seqCis_FLAG:
      return "z";
    case CIP_CHIRALITY_seqtrans_FLAG:
      return "E";
    case CIP_CHIRALITY_seqTrans_FLAG:
      return "e";
    case CIP_CHIRALITY_M_FLAG:
      return "M";
    case CIP_CHIRALITY_P_FLAG:
      return "P";
    case CIP_CHIRALITY_R_FLAG:
      return "R";
    case CIP_CHIRALITY_S_FLAG:
      return "S";
    case CIP_CHIRALITY_r_FLAG:
      return "r";
    case CIP_CHIRALITY_s_FLAG:
      return "s";
    case CIP_CHIRALITY_m_FLAG:
      return "m";
    case CIP_CHIRALITY_p_FLAG:
      return "p";
    case CIP_CHIRALITY_CANTDETERMINE:
      return "?";
    case CIP_CHIRALITY_NONE:
    case CIP_CHIRALITY_UNKNOWN:
    default:
      return "";
    }
  }

  private static final String[] ruleNames = { "", "1a", "1b", "2", "3", "4a",
      "4b", "4c", "5", "6" };

  public static String getCIPRuleName(int i) {
    return ruleNames[i];
  }

  public static int getCIPChiralityCode(char c) {
    switch (c) {
    case 'Z':
      return CIP_CHIRALITY_seqcis_FLAG;
    case 'z':
      return CIP_CHIRALITY_seqCis_FLAG;
    case 'E':
      return CIP_CHIRALITY_seqtrans_FLAG;
    case 'e':
      return CIP_CHIRALITY_seqTrans_FLAG;
    case 'R':
      return CIP_CHIRALITY_R_FLAG;
    case 'S':
      return CIP_CHIRALITY_S_FLAG;
    case 'r':
      return CIP_CHIRALITY_r_FLAG;
    case 's':
      return CIP_CHIRALITY_s_FLAG;
    case '?':
      return CIP_CHIRALITY_CANTDETERMINE;
    default:
      return CIP_CHIRALITY_UNKNOWN;
    }
  }

  // axes mode constants --> org.jmol.constant.EnumAxesMode
  // callback constants --> org.jmol.constant.EnumCallback
  // draw constants --> org.jmol.shapespecial.draw.EnumCallback

  public static final String PDB_ANNOTATIONS = ";dssr;rna3d;dom;val;";

  public static final String CACTUS_FILE_TYPES = ";alc;cdxml;cerius;charmm;cif;cml;ctx;gjf;gromacs;hyperchem;jme;maestro;mol;mol2;sybyl2;mrv;pdb;sdf;sdf3000;sln;smiles;xyz";

  // note list of RCSB access points: http://www.rcsb.org/pdb/static.do?p=download/http/index.html

  public static final String defaultMacroDirectory = "https://chemapps.stolaf.edu/jmol/macros";

  public static final String SPACE_GROUP_LIST_BCS = "https://www.cryst.ehu.es/cgi-bin/cryst/programs/nph-getgen";

  private final static String[] databaseArray = {
      // still http:
      "itatable", 
      "https://www.cryst.ehu.es/cgi-bin/cryst/programs/nph-getgen?gnum=%FILE&what=gp&list=Standard%2FDefault+Setting",
      "itadiagramita",
      "https://onlinelibrary.wiley.com/iucr/itc/Ac/ch2o3v0001/sgtable2o3o%FILE",
      "itadiagram",
      "http://img.chem.ucl.ac.uk/sgp/large/%FILEaz1.htm",
      "aflowbin",
      "http://aflowlib.mems.duke.edu/users/jmolers/binary_new/%FILE.aflow_binary",
      "aflowlib", "https://aflow.org/p/%FILE.cif", // updated 2024.04.22
      "aflowpro","$aflowlib",
      // _#DOCACHE_ flag indicates that the loaded file should be saved in any state in full
      // ' at start indicates a Jmol script evaluation
      "ams",
      "'https://rruff.geo.arizona.edu/AMS/viewJmol.php?'+(0+'%file'==0? 'mineral':('%file'.length==7? 'amcsd':'id'))+'=%file&action=showcif#_DOCACHE_'",
      "dssr", "http://dssr-jmol.x3dna.org/report.php?id=%FILE&opts=--json=ebi", //for debugging: -blocks",
      "dssrModel",
      "http://dssr-jmol.x3dna.org/report.php?POST?opts=--json=ebi&model=", // called in DSSR1.java
      "iucr", "http://scripts.iucr.org/cgi-bin/sendcif_yard?%FILE", // e.g. wf5113sup1
      "cod",
      "http://www.crystallography.net/cod/cif/%c1/%c2%c3/%c4%c5/%FILE.cif",
      "nmr", "https://www.nmrdb.org/new_predictor?POST?molfile=", "nmrdb",
      "https://www.nmrdb.org/service/predictor?POST?molfile=", "nmrdb13",
      "https://www.nmrdb.org/service/jsmol13c?POST?molfile=",
      //"pdb", "http://ftp.wwpdb.org/pub/pdb/data/structures/divided/pdb/%c2%c3/pdb%file.ent.gz", // new Jmol 14.5.0 10/28/2015
      "magndata", "http://webbdcrista1.ehu.es/magndata/mcif/%FILE.mcif",
      "rna3d", "http://rna.bgsu.edu/rna3dhub/%TYPE/download/%FILE",
      // now https:
      "mmtf", "https://mmtf.rcsb.org/v1.0/full/%FILE", // new Jmol 14.5.4 4/2016 MMTF phase out in 2024.06
      "bcif", "https://models.rcsb.org/%file.bcif", // new Jmol 16.1.52 2024.01.25
      "chebi",
      "https://www.ebi.ac.uk/chebi/saveStructure.do?defaultImage=true&chebiId=%file%2D%",
      "ligand", "https://files.rcsb.org/ligands/download/%FILE.cif", 
//      "mp",
//      "https://www.materialsproject.org/materials/mp-%FILE/cif#_DOCACHE_", // e.g. https://materialsproject.org/rest/v1/materials/mp-24972/cif 
      "nci", "https://cactus.nci.nih.gov/chemical/structure/", 
      "pdb",
      "https://files.rcsb.org/download/%FILE.pdb", // new Jmol 14.4.4 3/2016
      "pdb0", "https://files.rcsb.org/download/%FILE.pdb", // used in JSmol
      "pdbe", "https://www.ebi.ac.uk/pdbe/entry-files/download/%FILE.cif",
      "pdbe2", "https://www.ebi.ac.uk/pdbe/static/entry/%FILE_updated.cif",
      "pubchem",
      "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/%FILE/SDF?record_type=3d",
      "map",
      "https://www.ebi.ac.uk/pdbe/api/%TYPE/%FILE?pretty=false&metadata=true",
      "pdbemap", "https://www.ebi.ac.uk/pdbe/coordinates/files/%file.ccp4",
      "pdbemapdiff",
      "https://www.ebi.ac.uk/pdbe/coordinates/files/%file_diff.ccp4",
      "pdbemapserver",
      "https://www.ebi.ac.uk/pdbe/volume-server/x-ray/%file/box/0,0,0/0,0,0?detail=6&space=cartesian&encoding=bcif",
      "pdbemapdiffserver",
      "https://www.ebi.ac.uk/pdbe/volume-server/x-ray/%file/box/0,0,0/0,0,0?detail=6&space=cartesian&encoding=bcif&diff=1", // last bit is just mine
      //"emdbmap", "https://ftp.ebi.ac.uk/pub/databases/emdb/structures/EMD-%file/map/emd_%file.map.gz", // https did not work in Java due to certificate issues
      // was considerably slower
      "emdbmap",
      "https://www.ebi.ac.uk/pdbe/densities/emd/emd-%file/cell?detail=6&space=cartesian&encoding=bcif",
      "emdbquery",
      "https://www.ebi.ac.uk/emdb/api/search/fitted_pdbs:%file?fl=emdb_id,map_contour_level_value&wt=csv", // to get the EMDB id from the PDB id
      "emdbmapserver",
      "https://www.ebi.ac.uk/pdbe/densities/emd/emd-%file/box/0,0,0/0,0,0?detail=6&space=cartesian&encoding=bcif",
      "xxxxresolverResolver", "https://chemapps.stolaf.edu/resolver",
      "smiles2d", "https://cirx.chemicalcreatures.com/chemical/structure/%FILE/file?format=sdf&get3d=false",
      "smiles3d", "https://cirx.chemicalcreatures.com/chemical/structure/%FILE/file?format=sdf&get3d=true",
      };

  private final static String defaultOptimadeFieldsStr = 
        ",chemical_formula_descriptive"
      + ",dimension_types"
      + ",lattice_vectors"
      + ",cartesian_site_positions"
      + ",species_at_sites"
      + ",species,"; 
    
  /**
   * Get all necessary response fields if an optimade call.
   * Note that the only conditions we have here is that this is http
   * and "optimade" is in the URL. Obviously not exactly correct.
   * 
   * @param url
   * @return optimade query
   */
  static String fixOptimadeCall(String url) {
    int pt = url.indexOf("response_fields=") + 16;
    String a = defaultOptimadeFieldsStr; 
    if (pt < 16) {
      int ptQ = url.indexOf("?");
      url += (ptQ < 0 ? "?" : "&") + "response_fields="
          + a.substring(1, a.length() - 1);
    } else {
      String fields = ","
          + url.substring(pt, (url + "&").indexOf('&', pt)) + ",";
      int flen = fields.length();
      for (int i = 0, b = 0; i >= 0; i = b) {
        b = a.indexOf(',', i + 1);
        if (b < 0)
          break;
        String k = a.substring(i, b + 1);
        if (fields.indexOf(k) < 0) {
          fields += k.substring(1);
        }
      }
      url = url.substring(0, pt) + fields.substring(1, fields.length() - 1)
      + url.substring(pt + flen - 2);
    }
    return url;
  }

//  static {
//System.out.println(fixOptimadeCall("test?filter&response_fields=lkadfs"));
//System.out.println(fixOptimadeCall("test?response_fields=species"));
//System.out.println(fixOptimadeCall("test?response_fields=lkadfs,species"));
//System.out.println(fixOptimadeCall("test?response_fields=lkadfs,species_at_sites,species&more"));
//System.out.println(fixOptimadeCall("test?filter&response_fields=species,lkadfs&more"));
//System.out.println(fixOptimadeCall("test?response_fields=species,lkadfs&more"));
//    
//    
//  }

  final static String legacyResolver = "cactus.nci.nih.gov/chemical/structure";

  final static Map<String, String> databases = new Hashtable<String, String>();

  static {
    for (int i = 0; i < databaseArray.length; i += 2) {
      String target = databaseArray[i + 1];
      if (target.charAt(0) == '$') {
        // alias
        target = databases.get(target.substring(1)); 
      }
      databases.put(databaseArray[i].toLowerCase(), target);
    }
  }

  public static String resolveDataBase(String database, String id, String format) {
    if (format == null) {
      if ((format = databases.get(database.toLowerCase())) == null)
        return null;
      int pt = id.indexOf("/");
      if (pt < 0) {
        if (database.equals("pubchem"))
          id = "name/" + id;
        else if (database.equals("nci"))
          id += "/file?format=sdf&get3d=true";
      }
      if (format.startsWith("'")) {
        // needs evaluation
        // xxxx.n means "the nth item"
        pt = id.indexOf(".");
        int n = (pt > 0 ? PT.parseInt(id.substring(pt + 1)) : 0);
        if (pt > 0)
          id = id.substring(0, pt);
        format = PT.rep(format, "%n", "" + n);
      }
    } else if (id.indexOf(".") >= 0 && format.indexOf("%FILE.") >= 0) {
      // replace RCSB format extension when a file extension is made explicit 
      format = format.substring(0, format.indexOf("%FILE"));
    }
    if (format.indexOf("%c") >= 0)
      for (int i = 1, n = id.length(); i <= n; i++)
        if (format.indexOf("%c" + i) >= 0)
          format = PT.rep(format, "%c" + i,
              id.substring(i - 1, i).toLowerCase());
    return (format.indexOf("%FILE") >= 0 ? PT.rep(format, "%FILE", id)
        : format.indexOf("%file") >= 0
            ? PT.rep(format, "%file", id.toLowerCase())
            : format + id);
  }

  /**
   * Check for databases that have changed from http:// to https:// over time.
   * We substitute https here in case this is from an old reference.
   * 
   * @param name
   * @return https protocol if necessary
   */
  static String fixProtocol(String name) {
    boolean isHttp = (name != null && name.indexOf("http") >= 0);
    String newname = (name == null || !isHttp ? name
        : name.indexOf("http://www.rcsb.org/pdb/files/") == 0
            ? resolveDataBase(name.indexOf("/ligand/") >= 0 ? "ligand" : "pdb",
                name.substring(name.lastIndexOf("/") + 1), null)
            : name.indexOf("http://www.ebi") == 0
                || name.indexOf("http://rruff") == 0
                || name.indexOf("http://pubchem") == 0
                || name.indexOf("http://cactus") == 0
                || name.indexOf("http://www.materialsproject") == 0
                    ? "https://" + name.substring(7)
             : name.indexOf("optimade") > 0 ? JC.fixOptimadeCall(name)
             : name);
    if (newname != name)
      Logger.info("JC.fixProtocol " + name + " --> " + newname);
    return newname;
  }

  // unused

  //  public static String[] macros = {
  //    "aflow",       "https://chemapps.stolaf.edu/jmol/macros/AFLOW.spt", "AFLOW macros",
  //    "bz",          "https://chemapps.stolaf.edu/jmol/macros/bz.spt", "Brillouin Zone/Wigner-Seitz macros",
  //    "topology",    "https://chemapps.stolaf.edu/jmol/macros/topology.spt", "Topology CIF macros",
  //    "topond",      "https://chemapps.stolaf.edu/jmol/macros/topond.spt", "CRYSTAL/TOPOND macros",
  //    "crystal",     "https://chemapps.stolaf.edu/jmol/macros/crystal.spt", "CRYSTAL macros"
  //  };
  // 
  //  public static String getMacroList() {
  //    SB s = new SB();
  //    for (int i = 0; i < macros.length; i += 3)
  //      s.append(macros[i]).append("\t").append(macros[i + 1]).append("\t").append(macros[i + 1]).append("\n");
  //    return s.toString();
  //  }
  //
  //
  //  public static String getMacro(String key) {
  //    for (int i = 0; i < macros.length; i += 3)
  //      if (macros[i].equals(key))
  //        return macros[i + 1];
  //    return null;
  //  }

  public final static String copyright = "(C) 2005-2025 Jmol Development";

  public final static String version;
  public static String majorVersion;
  public final static String date;
  public final static int versionInt;

  static {
    String tmpVersion = null;
    String tmpDate = null;

    //    /**
    //     * definitions are incorporated into j2s/java/core.z.js by buildtojs.xml
    //     * 
    //     * @j2sNative
    //     * 
    //     *            tmpVersion = Jmol.___JmolVersion; tmpDate = Jmol.___JmolDate;
    //     */
    //    {
    BufferedInputStream bis = null;
    InputStream is = null;
    try {
      // Reading version from resource   inside jar
      is = JC.class.getClassLoader().getResourceAsStream(
          /** @j2sNative "core/Jmol.properties" || */
          "org/jmol/viewer/Jmol.properties");
      bis = new BufferedInputStream(is);
      Properties props = new Properties();
      props.load(bis);
      String s = props.getProperty("Jmol.___JmolVersion", tmpVersion);
      if (s != null && s.lastIndexOf("\"") > 0)
        s = s.substring(0, s.lastIndexOf("\"") + 1);
      tmpVersion = PT.trimQuotes(s);
      tmpDate = PT.trimQuotes(props.getProperty("Jmol.___JmolDate", tmpDate));
    } catch (Exception e) {
      // Nothing to do
    } finally {
      if (bis != null) {
        try {
          bis.close();
        } catch (Exception e) {
          // Nothing to do
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // Nothing to do
        }
      }
    }
    //    }
    if (tmpDate != null) {
      tmpDate = tmpDate.substring(7, 23);
      // NOTE : date is updated in the properties by SVN, and is in the format
      // "$Date: 2018-01-25 01:10:13 -0600 (Thu, 25 Jan 2018) $"
      //  0         1         2
      //  012345678901234567890123456789
    }
    version = (tmpVersion != null ? tmpVersion : "(Unknown_version)");
    majorVersion = (tmpVersion != null ? tmpVersion : "(Unknown_version)");
    date = (tmpDate != null ? tmpDate : "");
    // 11.9.999 --> 1109999
    int v = -1;
    if (tmpVersion != null)
      try {
        String s = version;
        String major = "";
        // Major number
        int i = s.indexOf(".");
        if (i < 0) {
          v = 100000 * Integer.parseInt(s);
          s = null;
        }
        if (s != null) {
          v = 100000 * Integer.parseInt(major = s.substring(0, i));

          // Minor number
          s = s.substring(i + 1);
          i = s.indexOf(".");
          if (i < 0) {
            v += 1000 * Integer.parseInt(s);
            s = null;
          }
          if (s != null) {
            String m = s.substring(0, i);
            major += "." + m;
            majorVersion = major;
            v += 1000 * Integer.parseInt(m);

            // Revision number
            s = s.substring(i + 1);
            i = s.indexOf("_");
            if (i >= 0)
              s = s.substring(0, i);
            i = s.indexOf(" ");
            if (i >= 0)
              s = s.substring(0, i);
            v += Integer.parseInt(s);
          }
        }
      } catch (NumberFormatException e) {
        // We simply keep the version currently found
      }
    versionInt = v;
  }

  public final static boolean officialRelease = false;

  public final static String DEFAULT_HELP_PATH = "https://chemapps.stolaf.edu/jmol/docs/index.htm";

  public final static String STATE_VERSION_STAMP = "# Jmol state version ";

  public final static String EMBEDDED_SCRIPT_TAG = "**** Jmol Embedded Script ****";

  public static String embedScript(String s) {
    return "\n/**" + EMBEDDED_SCRIPT_TAG + " \n" + s + "\n**/";
  }

  public final static String NOTE_SCRIPT_FILE = "NOTE: file recognized as a script file: ";

  public static final String SCRIPT_EXT = "\1##";
  public final static String SCRIPT_GUI = "; ## GUI ##";
  public static final String SCRIPT_QUIET = "#quiet";
  public static final String SCRIPT_STEP = SCRIPT_EXT + "SCRIPT_STEP";
  public static final String SCRIPT_START = SCRIPT_EXT + "SCRIPT_START";
  public static final String SCRIPT_NOENDCHECK = SCRIPT_EXT + "NOENDCHECK";
  public final static String SCRIPT_ISEDITOR = SCRIPT_EXT + " ISEDITOR";
  public final static String SCRIPT_EDITOR_IGNORE = SCRIPT_EXT + " EDITOR_IGNORE ##";
  public final static String SCRIPT_CONSOLE = SCRIPT_GUI + SCRIPT_EDITOR_IGNORE + SCRIPT_GUI;
  public final static String REPAINT_IGNORE = SCRIPT_EXT + " REPAINT_IGNORE ##";

  public static final int SG_AS_STRING = 1;
  public static final int SG_IS_ASSIGN = 2;
  public static final int SG_FROM_SCRATCH = 4;
  public static final int SG_CHECK_SUPERCELL = 8;
  public static final int SG_CALC_ONLY = 16;
  
  
  public final static String LOAD_ATOM_DATA_TYPES = ";xyz;vxyz;vibration;temperature;occupancy;partialcharge;";

  public final static double radiansPerDegree = Math.PI / 180;

  public final static String allowedQuaternionFrames = "RC;RP;a;b;c;n;p;q;x;";

  //note: Eval.write() processing requires drivers to be first-letter-capitalized.
  //do not capitalize any other letter in the word. Separate by semicolon.
  public final static String EXPORT_DRIVER_LIST = "Idtf;Maya;Povray;Vrml;X3d;Stl;Tachyon;Obj";

  public final static V3d center = V3d.new3(0, 0, 0);
  public final static V3d axisX = V3d.new3(1, 0, 0);
  public final static V3d axisY = V3d.new3(0, 1, 0);
  public final static V3d axisZ = V3d.new3(0, 0, 1);
  public final static V3d axisNX = V3d.new3(-1, 0, 0);
  public final static V3d axisNY = V3d.new3(0, -1, 0);
  public final static V3d axisNZ = V3d.new3(0, 0, -1);
  public final static V3d[] unitAxisVectors = { axisX, axisY, axisZ, axisNX,
      axisNY, axisNZ };

  public final static int XY_ZTOP = 100; // Z value for [x y] positioned echos and axis origin
  public final static int DEFAULT_PERCENT_VDW_ATOM = 23; // matches C sizes of AUTO with 20 for Jmol set
  public final static double DEFAULT_BOND_RADIUS = 0.15d;
  public final static short DEFAULT_BOND_MILLIANGSTROM_RADIUS = (short) (DEFAULT_BOND_RADIUS
      * 1000);
  public final static double DEFAULT_STRUT_RADIUS = 0.3d;
  //angstroms of slop ... from OpenBabel ... mth 2003 05 26
  public final static double DEFAULT_BOND_TOLERANCE = 0.45d;
  //minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  public final static double DEFAULT_MIN_BOND_DISTANCE = 0.4d;
  public final static double DEFAULT_MAX_CONNECT_DISTANCE = 100000000d;
  public final static double DEFAULT_MIN_CONNECT_DISTANCE = 0.1d;
  public final static double MINIMIZE_FIXED_RANGE = 5.0d;

  public final static double ENC_CALC_MAX_DIST = 3.0d;
  public final static int ENV_CALC_MAX_LEVEL = 3;//Geodesic.standardLevel;

  public final static int MOUSE_NONE = -1;

  public final static byte MULTIBOND_NEVER = 0;
  public final static byte MULTIBOND_WIREFRAME = 1;
  public final static byte MULTIBOND_NOTSMALL = 2;
  public final static byte MULTIBOND_ALWAYS = 3;

  // maximum number of bonds that an atom can have when
  // autoBonding
  // All bonding is done by distances
  // this is only here for truly pathological cases
  public final static int MAXIMUM_AUTO_BOND_COUNT = 20;

  public final static short madMultipleBondSmallMaximum = 500;

  /* .cube files need this */
  public final static double ANGSTROMS_PER_BOHR = 0.5291772d;

  
  public static final int COLOR_CONTRAST = 0xFFfedcba;


  public final static int[] altArgbsCpk = { 0xFFFF1493, // Xx 0
      0xFFBFA6A6, // Al 13
      0xFFFFFF30, // S  16
      0xFF57178F, // Cs 55
      0xFFFFFFC0, // D 2H
      0xFFFFFFA0, // T 3H
      0xFFD8D8D8, // 11C  6 - lighter
      0xFF505050, // 13C  6 - darker
      0xFF404040, // 14C  6 - darker still
      0xFF105050, // 15N  7 - darker
  };

  public final static int FORMAL_CHARGE_COLIX_RED = Elements.elementSymbols.length
      + altArgbsCpk.length;
  
  // hmmm ... what is shapely backbone? seems interesting
  //public final static int argbShapelyBackbone = 0xFFB8B8B8;
  //public final static int argbShapelySpecial =  0xFF5E005E;
  //public final static int argbShapelyDefault =  0xFFFF00FF;

  public final static int[] argbsFormalCharge = { 0xFFFF0000, // -4
      0xFFFF4040, // -3
      0xFFFF8080, // -2
      0xFFFFC0C0, // -1
      0xFFFFFFFF, // 0
      0xFFD8D8FF, // 1
      0xFFB4B4FF, // 2
      0xFF9090FF, // 3
      0xFF6C6CFF, // 4
      0xFF4848FF, // 5
      0xFF2424FF, // 6
      0xFF0000FF, // 7
  };

  public final static int PARTIAL_CHARGE_COLIX_RED = FORMAL_CHARGE_COLIX_RED
      + argbsFormalCharge.length;

  public final static int[] argbsRwbScale = { 0xFFFF0000, // red
      0xFFFF1010, //
      0xFFFF2020, //
      0xFFFF3030, //
      0xFFFF4040, //
      0xFFFF5050, //
      0xFFFF6060, //
      0xFFFF7070, //
      0xFFFF8080, //
      0xFFFF9090, //
      0xFFFFA0A0, //
      0xFFFFB0B0, //
      0xFFFFC0C0, //
      0xFFFFD0D0, //
      0xFFFFE0E0, //
      0xFFFFFFFF, // white
      0xFFE0E0FF, //
      0xFFD0D0FF, //
      0xFFC0C0FF, //
      0xFFB0B0FF, //
      0xFFA0A0FF, //
      0xFF9090FF, //
      0xFF8080FF, //
      0xFF7070FF, //
      0xFF6060FF, //
      0xFF5050FF, //
      0xFF4040FF, //
      0xFF3030FF, //
      0xFF2020FF, //
      0xFF1010FF, //
      0xFF0000FF, // blue
  };

  public final static int PARTIAL_CHARGE_RANGE_SIZE = argbsRwbScale.length;

//  $ print  color("red","blue", 33,true)
  //  [xff0000][xff2000][xff4000]
  //[xff6000][xff8000][xff9f00]

  //[xffbf00][xffdf00] -------  
  //[xffff00] ------- [xdfff00]

  //[xbfff00][x9fff00][x7fff00]
  //[x60ff00][x40ff00][x20ff00]
  //[x00ff00][x00ff20][x00ff40]
  //[x00ff60][x00ff7f][x00ff9f]
  //[x00ffbf][x00ffdf][x00ffff]
  //[x00dfff][x00bfff][x009fff]
  //[x0080ff][x0060ff][x0040ff]
  //[x0020ff][x0000ff]

  public final static int[] argbsRoygbScale = {
      // 35 in all //why this comment?: must be multiple of THREE for high/low
      0xFFFF0000, 0xFFFF2000, 0xFFFF4000, 0xFFFF6000, 0xFFFF8000, 0xFFFFA000,

      // yellow gets compressed, so give it an extra boost

      0xFFFFC000, 0xFFFFE000, 0xFFFFF000, 0xFFFFFF00, 0xFFF0F000, 0xFFE0FF00,

      0xFFC0FF00, 0xFFA0FF00, 0xFF80FF00, 0xFF60FF00, 0xFF40FF00, 0xFF20FF00,
      0xFF00FF00, 0xFF00FF20, 0xFF00FF40, 0xFF00FF60, 0xFF00FF80, 0xFF00FFA0,
      0xFF00FFC0, 0xFF00FFE0, 0xFF00FFFF, 0xFF00E0FF, 0xFF00C0FF, 0xFF00A0FF,
      0xFF0080FF, 0xFF0060FF, 0xFF0040FF, 0xFF0020FF, 0xFF0000FF, };

  // positive and negative default colors used for
  // isosurface rendering of .cube files
  // multiple colors removed -- RMH 3/2008 11.1.28

  public final static int argbsIsosurfacePositive = 0xFF5020A0;
  public final static int argbsIsosurfaceNegative = 0xFFA02050;

  ////////////////////////////////////////////////////////////////
  // currently, ATOMIDs must be >= 0 && <= 127
  // if we need more then we can go to 255 by:
  //  1. applying 0xFF mask ... as in atom.specialAtomID & 0xFF;
  //  2. change the interesting atoms table to be shorts
  //     so that we can store negative numbers
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // keep this table in order to make it easier to maintain
  ////////////////////////////////////////////////////////////////

  // the following refer to jmol.biomodelset.Resolver.specialAtomNames

  // atomID 0 => nothing special, just an ordinary atom
  public final static byte ATOMID_AMINO_NITROGEN = 1;
  public final static byte ATOMID_ALPHA_CARBON = 2;
  public final static byte ATOMID_CARBONYL_CARBON = 3;
  public final static byte ATOMID_CARBONYL_OXYGEN = 4;
  public final static byte ATOMID_O1 = 5;

  // this is for groups that only contain an alpha carbon
  public final static int ATOMID_ALPHA_ONLY_MASK = 1 << ATOMID_ALPHA_CARBON;

  //this is entries 1 through 3 ... 3 bits ... N, CA, C
  public final static int ATOMID_PROTEIN_MASK = 0x7 << ATOMID_AMINO_NITROGEN;

  public final static byte ATOMID_O5_PRIME = 6;
  public final static byte ATOMID_C5_PRIME = 7;
  public final static byte ATOMID_C4_PRIME = 8;
  public final static byte ATOMID_C3_PRIME = 9;
  public final static byte ATOMID_O3_PRIME = 10;
  public final static byte ATOMID_C2_PRIME = 11;
  public final static byte ATOMID_C1_PRIME = 12;
  public final static byte ATOMID_O4_PRIME = 78;

  // this is entries 6 through through 12 ... 7 bits
  public final static int ATOMID_NUCLEIC_MASK = 0x7F << ATOMID_O5_PRIME;

  public final static byte ATOMID_NUCLEIC_PHOSPHORUS = 13;

  // this is for nucleic groups that only contain a phosphorus
  public final static int ATOMID_PHOSPHORUS_ONLY_MASK = 1 << ATOMID_NUCLEIC_PHOSPHORUS;

  // this can be increased as far as 32, but not higher.
  public final static int ATOMID_DISTINGUISHING_ATOM_MAX = 14;

  public final static byte ATOMID_CARBONYL_OD1 = 14;
  public final static byte ATOMID_CARBONYL_OD2 = 15;
  public final static byte ATOMID_CARBONYL_OE1 = 16;
  public final static byte ATOMID_CARBONYL_OE2 = 17;
  //public final static byte ATOMID_SG = 18;

  public final static byte ATOMID_N1 = 32;
  public final static byte ATOMID_C2 = 33;
  public final static byte ATOMID_N3 = 34;
  public final static byte ATOMID_C4 = 35;
  public final static byte ATOMID_C5 = 36;
  public final static byte ATOMID_C6 = 37; // wing
  public final static byte ATOMID_O2 = 38;
  public final static byte ATOMID_N7 = 39;
  public final static byte ATOMID_C8 = 40;
  public final static byte ATOMID_N9 = 41;
  public final static byte ATOMID_N4 = 42;
  public final static byte ATOMID_N2 = 43;
  public final static byte ATOMID_N6 = 44;
  public final static byte ATOMID_C5M = 45;
  public final static byte ATOMID_O6 = 46;
  public final static byte ATOMID_O4 = 47;
  public final static byte ATOMID_S4 = 48;
  public final static byte ATOMID_C7 = 49;

  public final static byte ATOMID_TERMINATING_OXT = 64;

  public final static byte ATOMID_H5T_TERMINUS = 72;
  public final static byte ATOMID_O5T_TERMINUS = 73;
  public final static byte ATOMID_O1P = 74;
  public final static byte ATOMID_OP1 = 75;
  public final static byte ATOMID_O2P = 76;
  public final static byte ATOMID_OP2 = 77;
  public final static byte ATOMID_O2_PRIME = 79;
  public final static byte ATOMID_H3T_TERMINUS = 88;
  public final static byte ATOMID_HO3_PRIME = 89;
  public final static byte ATOMID_HO5_PRIME = 90;

  // These masks are only used for P-only and N-only polymers
  // or cases where there are so few atoms that a monomer's type
  // cannot be determined by checking actual atoms and connections.
  // They are not used for NucleicMonomer or AminoMonomer classes.
  //
  //             I  A G        
  //   purine:   100101 = 0x25
  //
  //              UT C
  // pyrimidine: 011010 = 0x1A
  //
  //            +IUTACGDIUTACG IUTACG
  //        dna: 001111 111111 001000 = 0x0FFC8
  //  
  //            +IUTACGDIUTACG IUTACG
  //        rna: 110??? 000000 110111 = 0x30037

  public static final int PURINE_MASK = 0x25 | (0x25 << 6) | (0x25 << 12);
  public static final int PYRIMIDINE_MASK = 0x1A | (0x1A << 6) | (0x1A << 12);
  public static final int DNA_MASK = 0x0FFC8;
  public static final int RNA_MASK = 0x30037;

  ////////////////////////////////////////////////////////////////
  // GROUP_ID related stuff for special groupIDs
  ////////////////////////////////////////////////////////////////

  public final static int GROUPID_ARGININE = 2;
  public final static int GROUPID_ASPARAGINE = 3;
  public final static int GROUPID_ASPARTATE = 4;
  public final static int GROUPID_CYSTEINE = 5;
  public final static int GROUPID_GLUTAMINE = 6;
  public final static int GROUPID_GLUTAMATE = 7;
  public final static int GROUPID_HISTIDINE = 9;
  public final static int GROUPID_LYSINE = 12;
  public final static int GROUPID_PROLINE = 15;
  public final static int GROUPID_TRYPTOPHAN = 19;
  public final static int GROUPID_AMINO_MAX = 24;
  public final static int GROUPID_NUCLEIC_MAX = 42;
  public final static int GROUPID_WATER = 42;
  public final static int GROUPID_SOLVENT_MIN = 45; // urea only
  private final static int GROUPID_ION_MIN = 46;
  private final static int GROUPID_ION_MAX = 48;

  ////////////////////////////////////////////////////////////////
  // predefined sets
  ////////////////////////////////////////////////////////////////

  // these must be removed after various script commands so that they stay current

  public static String[] predefinedVariable = {
      //  
      // main isotope (variable because we can do {xxx}.element = n;
      //
      "@_1H _H & !(_2H,_3H)", "@_12C _C & !(_13C,_14C)", "@_14N _N & !(_15N)",

      //
      // solvent
      //
      // @water is specially defined, avoiding the CONNECTED() function
      //"@water _g>=" + GROUPID_WATER + " & _g<" + GROUPID_SOLVENT_MIN
      //+ ", oxygen & connected(2) & connected(2, hydrogen), (hydrogen) & connected(oxygen & connected(2) & connected(2, hydrogen))",

      "@solvent water, (_g>=" + GROUPID_SOLVENT_MIN + " & _g<" + GROUPID_ION_MAX
          + ")", // water, other solvent or ions
      "@ligand _g=0|!(_g<" + GROUPID_ION_MIN + ",protein,nucleic,water)", // includes UNL

      // protein structure
      "@turn structure=1", "@sheet structure=2", "@helix structure=3",
      "@helix310 substructure=7", "@helixalpha substructure=8",
      "@helixpi substructure=9",

      // nucleic acid structures
      "@bulges within(dssr,'bulges')", "@coaxStacks within(dssr,'coaxStacks')",
      "@hairpins within(dssr,'hairpins')", "@hbonds within(dssr,'hbonds')",
      "@helices within(dssr,'helices')", "@iloops within(dssr,'iloops')",
      "@isoCanonPairs within(dssr,'isoCanonPairs')",
      "@junctions within(dssr,'junctions')",
      "@kissingLoops within(dssr,'kissingLoops')",
      "@multiplets within(dssr,'multiplets')",
      "@nonStack within(dssr,'nonStack')", "@nts within(dssr,'nts')",
      "@pairs within(dssr,'pairs')", "@ssSegments within(dssr,'ssSegments')",
      "@stacks within(dssr,'stacks')", "@stems within(dssr,'stems')",

  };

  // these are only updated once per file load or file append

  public static String[] predefinedStatic = {
      //
      // protein related
      //
      // protein is hardwired
      "@amino _g>0 & _g<=23", "@acidic asp,glu", "@basic arg,his,lys",
      "@charged acidic,basic", "@negative acidic", "@positive basic",
      "@neutral amino&!(acidic,basic)", "@polar amino&!hydrophobic",
      "@peptide protein&within(chain,monomer>1)&!within(chain,monomer>12)", // Jmol 14.29.1
      "@cyclic his,phe,pro,trp,tyr", "@acyclic amino&!cyclic",
      "@aliphatic ala,gly,ile,leu,val", "@aromatic his,phe,trp,tyr",
      "@cystine within(group,(cys,cyx)&atomname=sg&connected((cys,cyx)&atomname=sg))",

      "@buried ala,cys,ile,leu,met,phe,trp,val", "@surface amino&!buried",

      // doc on hydrophobic is inconsistent
      // text description of hydrophobic says this
      //    "@hydrophobic ala,leu,val,ile,pro,phe,met,trp",
      // table says this
      "@hydrophobic ala,gly,ile,leu,met,phe,pro,trp,tyr,val",
      "@mainchain backbone", "@small ala,gly,ser",
      "@medium asn,asp,cys,pro,thr,val",
      "@large arg,glu,gln,his,ile,leu,lys,met,phe,trp,tyr",

      //
      // nucleic acid related

      // nucleic, dna, rna, purine, pyrimidine are hard-wired
      //
      "@c nucleic & ([C] or [DC] or within(group,_a=" + ATOMID_N4 + "))",
      "@g nucleic & ([G] or [DG] or within(group,_a=" + ATOMID_N2 + "))",
      "@cg c,g",
      "@a nucleic & ([A] or [DA] or within(group,_a=" + ATOMID_N6 + "))",
      "@t nucleic & ([T] or [DT] or within(group,_a=" + ATOMID_C5M + " | _a="
          + ATOMID_C7 + "))",
      "@at a,t",
      "@i nucleic & ([I] or [DI] or within(group,_a=" + ATOMID_O6 + ") & !g)",
      "@u nucleic & ([U] or [DU] or within(group,_a=" + ATOMID_O4 + ") & !t)",
      "@tu nucleic & within(group,_a=" + ATOMID_S4 + ")",

      //
      // ions
      //
      "@ions _g>=" + GROUPID_ION_MIN + "&_g<" + GROUPID_ION_MAX,

      //
      // structure related
      //
      "@alpha _a=2", // rasmol doc says "approximately *.CA" - whatever?
      "@_bb protein&(_a>=1&_a<6|_a=64) | nucleic&(_a>=6&_a<14|_a>=73&&_a<=79||_a==99||_a=100)", // no H atoms    
      "@backbone _bb | _H && connected(single, _bb)",
      "@spine protein&_a>=1&_a<4|nucleic&(_a>=6&_a<11|_a=13)",
      "@sidechain (protein,nucleic) & !backbone", "@base nucleic & !backbone",
      "@dynamic_flatring search('[a]')",

      //periodic table
      "@nonmetal _H,_He,_B,_C,_N,_O,_F,_Ne,_Si,_P,_S,_Cl,_Ar,_As,_Se,_Br,_Kr,_Te,_I,_Xe,_At,_Rn",
      "@metal !nonmetal && !_Xx", 
      "@alkaliMetal _Li,_Na,_K,_Rb,_Cs,_Fr",
      "@alkalineEarth _Be,_Mg,_Ca,_Sr,_Ba,_Ra",
      "@nobleGas _He,_Ne,_Ar,_Kr,_Xe,_Rn", "@metalloid _B,_Si,_Ge,_As,_Sb,_Te",
      // added La, Ac as per Frank Weinhold - these two are not f-block
      "@transitionMetal elemno>=21&elemno<=30|elemno=57|elemno=89|elemno>=39&elemno<=48|elemno>=72&elemno<=80|elemno>=104&elemno<=112",
      // removed La
      "@lanthanide elemno>57&elemno<=71",
      // removed Ac 
      "@actinide elemno>89&elemno<=103",

      //    "@hetero", handled specially

  };

  /**
   * specifically for ECHO and DRAW to have these specific for a given model
   * and only appearing when there is only one model showing (see MODELKIT SET KEY ON)
   */
  public static final String THIS_MODEL_ONLY = "_!_";
  public static final String MODELKIT_ELEMENT_KEY_ID = THIS_MODEL_ONLY + "elkey_";


  public static final String MODELKIT_SET_LABEL_KEY = "setlabelkey";
  public static final String MODELKIT_SET_ELEMENT_KEY = "setelementkey";
  public static final String MODELKIT_KEY = "key";
  public static final String MODELKIT_MODEL_KEY = "key_";
  public static final String MODELKIT_ELEMENT_KEY = "elementkey";
  public static final String MODELKIT_LABEL_KEY = "labelkey";
  public static final String MODELKIT_NEW_MODEL_ATOM_KEYS = "newmodelatomkeys";
  public static final String MODELKIT_FRAME_RESIZED = "frameresized";
  public static final String MODELKIT_UDPATE_KEY_STATE = "updatekeysfromstate";
  public static final String MODELKIT_UPDATE_MODEL_KEYS = "updatemodelkeys";
  public static final String MODELKIT_UPDATE_ATOM_KEYS = "updateatomkeys";
  public static final String MODELKIT_ASSIGN_BOND = "assignbond";
  public static final String MODELKIT_ROTATE_BOND_ATOM_INDEX = "rotatebloadond";
  public static final String MODELKIT_BRANCH_ATOM_PICKED = "branchatomclicked";
  public static final String MODELKIT_BRANCH_ATOM_DRAGGED = "branchatomdragged";
  
  public static final String MODELKIT_CENTER = "center";
  public static final String MODELKIT_HIDEMENU = "hidemenu";
  public static final String MODELKIT_CONSTRAINT = "constraint";
  public static final String MODELKIT_EXISTS = "exists";
  public static final String MODELKIT_ISMOLECULAR = "ismolecular";
  public static final String MODELKIT_ALLOPERATORS = "alloperators";
  public static final String MODELKIT_DATA = "data";
  public static final String MODELKIT_MINIMIZING = "minimizing";
  public static final String MODELKIT_RESET = "reset";
  public static final String MODELKIT_ATOMPICKINGMODE = "atompickingmode";
  public static final String MODELKIT_BONDPICKINGMODE = "bondpickingmode";
  public static final String MODELKIT_HIGHLIGHT = "highlight";
  public static final String MODELKIT_MODE = "mode";
  public static final String MODELKIT_APPLYLOCAL = "applylocal";
  public static final String MODELKIT_RETAINLOCAL = "retainlocal";
  public static final String MODELKIT_APPLYFULL = "applyfull";
  public static final String MODELKIT_UNITCELL = "unitcell";
  public static final String MODELKIT_ADDHYDROGEN = "addhydrogen";
  public static final String MODELKIT_ADDHYDROGENS = "addhydrogens";
  public static final String MODELKIT_AUTOBOND = "autobond";
  public static final String MODELKIT_CLICKTOSETELEMENT = "clicktosetelement";
  public static final String MODELKIT_HIDDEN = "hidden";
  public static final String MODELKIT_SHOWSYMOPINFO = "showsymopinfo";
  public static final String MODELKIT_SYMOP = "symop";
  public static final String MODELKIT_SYMMETRY = "symmetry";
  public static final String MODELKIT_HOVERLABEL = "hoverlabel";
  public static final String MODELKIT_ATOMTYPE = "atomtype";
  public static final String MODELKIT_BONDTYPE = "bondtype";
  public static final String MODELKIT_BONDINDEX = "bondindex";
  public static final String MODELKIT_ROTATEBONDINDEX = "rotatebondindex";
  public static final String MODELKIT_OFFSET = "offset";
  public static final String MODELKIT_SCREENXY = "screenxy";
  public static final String MODELKIT_INVARIANT = "invariant";
  public static final String MODELKIT_DISTANCE = "distance";
  public static final String MODELKIT_ADDCONSTRAINT = "addconstraint";
  public static final String MODELKIT_REMOVECONSTRAINT = "removeconstraint";
  public static final String MODELKIT_REMOVEALLCONSTRAINTS = "removeallconstraints";
  public static final String MODELKIT_VIBRATION = "vibration";
  public static final String MODELKIT_INITIALIZE_MODEL = "initializemodel";

  public static final String MODELKIT_DRAGATOM = "dragatom";
  public static final String MODELKIT_DELETE_BOND = "deletebond";
  public final static String MODELKIT_ZAP_STRING = "5\n\nC 0 0 0\nH .63 .63 .63\nH -.63 -.63 .63\nH -.63 .63 -.63\nH .63 -.63 -.63";
  public final static String MODELKIT_ZAP_TITLE = "Jmol Model Kit";//do not ever change this -- it is in the state
  public final static String ZAP_TITLE = "zapped";//do not ever change this -- it is in the state
  public final static String ADD_HYDROGEN_TITLE = "Viewer.AddHydrogens"; //do not ever change this -- it is in the state
  public static final String JMOL_MODEL_KIT = "Jmol Model Kit";

  ////////////////////////////////////////////////////////////////
  // font-related
  ////////////////////////////////////////////////////////////////

  public final static String DEFAULT_FONTFACE = "SansSerif";
  public final static String DEFAULT_FONTSTYLE = "Plain";

  public final static int MEASURE_DEFAULT_FONTSIZE = 18;
  public final static int AXES_DEFAULT_FONTSIZE = 16;
  public static final double DRAW_DEFAULT_FONTSIZE = 16;

  ////////////////////////////////////////////////////////////////
  // do not rearrange/modify these shapes without
  // updating the String[] shapeBaseClasses below &&
  // also creating a token for this shape in Token.java &&
  // also updating shapeToks to confirm consistent
  // conversion from tokens to shapes
  ////////////////////////////////////////////////////////////////

  public final static int SHAPE_BALLS = 0;
  public final static int SHAPE_STICKS = 1;
  public final static int SHAPE_HSTICKS = 2; //placeholder only; handled by SHAPE_STICKS
  public final static int SHAPE_SSSTICKS = 3; //placeholder only; handled by SHAPE_STICKS
  public final static int SHAPE_STRUTS = 4; //placeholder only; handled by SHAPE_STICKS
  public final static int SHAPE_LABELS = 5;
  public final static int SHAPE_MEASURES = 6;
  public final static int SHAPE_STARS = 7;

  public final static int SHAPE_MIN_HAS_SETVIS = 8;

  public final static int SHAPE_HALOS = 8;

  public final static int SHAPE_MIN_SECONDARY = 9; //////////

  public final static int SHAPE_BACKBONE = 9;
  public final static int SHAPE_TRACE = 10;
  public final static int SHAPE_CARTOON = 11;
  public final static int SHAPE_STRANDS = 12;
  public final static int SHAPE_MESHRIBBON = 13;
  public final static int SHAPE_RIBBONS = 14;
  public final static int SHAPE_ROCKETS = 15;

  public final static int SHAPE_MAX_SECONDARY = 16; //////////
  public final static int SHAPE_MIN_SPECIAL = 16; //////////

  public final static int SHAPE_DOTS = 16;
  public final static int SHAPE_DIPOLES = 17;
  public final static int SHAPE_VECTORS = 18;
  public final static int SHAPE_GEOSURFACE = 19;
  public final static int SHAPE_ELLIPSOIDS = 20;

  public final static int SHAPE_MAX_SIZE_ZERO_ON_RESTRICT = 21; //////////

  public final static int SHAPE_MIN_HAS_ID = 21; //////////

  public final static int SHAPE_POLYHEDRA = 21; // for restrict, uses setProperty(), not setSize()

  public final static int SHAPE_DRAW = 22;

  public final static int SHAPE_MAX_SPECIAL = 23; //////////

  public final static int SHAPE_CGO = 23;

  public final static int SHAPE_MIN_SURFACE = 24; //////////

  public final static int SHAPE_ISOSURFACE = 24;
  public final static int SHAPE_CONTACT = 25;

  public final static int SHAPE_LCAOCARTOON = 26;

  private final static int SHAPE_LAST_ATOM_VIS_FLAG = 26; // LCAO 
  // no setting of atom.shapeVisibilityFlags after this point

  public final static int SHAPE_MO = 27; //but no ID for MO
  public final static int SHAPE_NBO = 28; //but no ID for MO

  public final static int SHAPE_PMESH = 29;
  public final static int SHAPE_PLOT3D = 30;

  public final static int SHAPE_MAX_SURFACE = 30; //////////
  public final static int SHAPE_MAX_MESH_COLLECTION = 30; //////////

  public final static int SHAPE_ECHO = 31;

  public final static int SHAPE_MAX_HAS_ID = 32;

  public final static int SHAPE_BBCAGE = 32;

  public final static int SHAPE_MAX_HAS_SETVIS = 33;

  public final static int SHAPE_UCCAGE = 33;
  public final static int SHAPE_AXES = 34;
  public final static int SHAPE_HOVER = 35;
  public final static int SHAPE_FRANK = 36;
  public final static int SHAPE_MAX = SHAPE_FRANK + 1;

  public final static int getShapeVisibilityFlag(int shapeID) {
    return 16 << Math.min(shapeID, SHAPE_LAST_ATOM_VIS_FLAG);
  }

  public static final int VIS_BOND_FLAG = 16 << SHAPE_STICKS;
  public static final int VIS_BALLS_FLAG = 16 << SHAPE_BALLS;
  public static final int VIS_LABEL_FLAG = 16 << SHAPE_LABELS;
  public static final int VIS_BACKBONE_FLAG = 16 << SHAPE_BACKBONE;
  public final static int VIS_CARTOON_FLAG = 16 << SHAPE_CARTOON;

  public final static int ALPHA_CARBON_VISIBILITY_FLAG = (16 << SHAPE_ROCKETS)
      | (16 << SHAPE_TRACE) | (16 << SHAPE_STRANDS) | (16 << SHAPE_MESHRIBBON)
      | (16 << SHAPE_RIBBONS) | VIS_CARTOON_FLAG | VIS_BACKBONE_FLAG;

  // note that these next two arrays *MUST* be in the same sequence 
  // given in SHAPE_* and they must be capitalized exactly as in their class name 

  public final static String[] shapeClassBases = { "Balls", "Sticks", "Hsticks",
      "Sssticks", "Struts",
      //Hsticks, Sssticks, and Struts classes do not exist, but this returns Token for them
      "Labels", "Measures", "Stars", "Halos", "Backbone", "Trace", "Cartoon",
      "Strands", "MeshRibbon", "Ribbons", "Rockets", "Dots", "Dipoles",
      "Vectors", "GeoSurface", "Ellipsoids", "Polyhedra", "Draw", "CGO",
      "Isosurface", "Contact", "LcaoCartoon", "MolecularOrbital", "NBO",
      "Pmesh", "Plot3D", "Echo", "Bbcage", "Uccage", "Axes", "Hover", "Frank" };
  // .hbond and .ssbonds will return a class,
  // but the class is never loaded, so it is skipped in each case.
  // coloring and sizing of hydrogen bonds and S-S bonds is now
  // done by Sticks.

  public final static int shapeTokenIndex(int tok) {
    switch (tok) {
    case T.atoms:
    case T.balls:
      return SHAPE_BALLS;
    case T.bonds:
    case T.wireframe:
      return SHAPE_STICKS;
    case T.hbond:
      return SHAPE_HSTICKS;
    case T.ssbond:
      return SHAPE_SSSTICKS;
    case T.struts:
      return SHAPE_STRUTS;
    case T.label:
      return SHAPE_LABELS;
    case T.measure:
    case T.measurements:
      return SHAPE_MEASURES;
    case T.star:
      return SHAPE_STARS;
    case T.halo:
      return SHAPE_HALOS;
    case T.backbone:
      return SHAPE_BACKBONE;
    case T.trace:
      return SHAPE_TRACE;
    case T.cartoon:
      return SHAPE_CARTOON;
    case T.strands:
      return SHAPE_STRANDS;
    case T.meshRibbon:
      return SHAPE_MESHRIBBON;
    case T.ribbon:
      return SHAPE_RIBBONS;
    case T.rocket:
      return SHAPE_ROCKETS;
    case T.dots:
      return SHAPE_DOTS;
    case T.dipole:
      return SHAPE_DIPOLES;
    case T.vector:
      return SHAPE_VECTORS;
    case T.geosurface:
      return SHAPE_GEOSURFACE;
    case T.ellipsoid:
      return SHAPE_ELLIPSOIDS;
    case T.polyhedra:
      return SHAPE_POLYHEDRA;
    case T.cgo:
      return SHAPE_CGO;
    case T.draw:
      return SHAPE_DRAW;
    case T.isosurface:
      return SHAPE_ISOSURFACE;
    case T.contact:
      return SHAPE_CONTACT;
    case T.lcaocartoon:
      return SHAPE_LCAOCARTOON;
    case T.mo:
      return SHAPE_MO;
    case T.nbo:
      return SHAPE_NBO;
    case T.pmesh:
      return SHAPE_PMESH;
    case T.plot3d:
      return SHAPE_PLOT3D;
    case T.echo:
      return SHAPE_ECHO;
    case T.axes:
      return SHAPE_AXES;
    case T.boundbox:
      return SHAPE_BBCAGE;
    case T.unitcell:
      return SHAPE_UCCAGE;
    case T.hover:
      return SHAPE_HOVER;
    case T.frank:
      return SHAPE_FRANK;
    }
    return -1;
  }

  public final static String getShapeClassName(int shapeID,
                                               boolean isRenderer) {
    if (shapeID < 0)
      return shapeClassBases[~shapeID];
    return "org.jmol." + (isRenderer ? "render" : "shape")
        + (shapeID >= SHAPE_MIN_SECONDARY && shapeID < SHAPE_MAX_SECONDARY
            ? "bio."
            : shapeID >= SHAPE_MIN_SPECIAL && shapeID < SHAPE_MAX_SPECIAL
                ? "special."
                : shapeID >= SHAPE_MIN_SURFACE && shapeID < SHAPE_MAX_SURFACE
                    ? "surface."
                    : shapeID == SHAPE_CGO ? "cgo." : ".")
        + shapeClassBases[shapeID];
  }

  //  public final static String binaryExtensions = ";pse=PyMOL;";// PyMOL

  public static final String SCRIPT_COMPLETED = "Script completed";
  public static final String JPEG_EXTENSIONS = ";jpg;jpeg;jpg64;jpeg64;";
  public final static String IMAGE_TYPES = JPEG_EXTENSIONS
      + "gif;gift;pdf;ppm;png;pngj;pngt;";
  public static final String IMAGE_OR_SCENE = IMAGE_TYPES + "scene;";

  static {
    /**
     * @j2sNative
     */
    {
      if (argbsFormalCharge.length != Elements.FORMAL_CHARGE_MAX
          - Elements.FORMAL_CHARGE_MIN + 1) {
        Logger.error("formal charge color table length");
        throw new NullPointerException();
      }
      if (shapeClassBases.length != SHAPE_MAX) {
        Logger.error("shapeClassBases wrong length");
        throw new NullPointerException();
      }
      if (shapeClassBases.length != SHAPE_MAX) {
        Logger.error("the shapeClassBases array has the wrong length");
        throw new NullPointerException();
      }
    }
  }

  ///////////////// LABEL and ECHO ///////////////////////

  // note that the y offset is positive upward

  //  3         2         1        
  // 10987654321098765432109876543210
  //  -x-offset--y-offset-___cafgaabp
  //                      |||||||| ||_pointer on
  //                      |||||||| |_background pointer color
  //                      ||||||||_text alignment 0xC 
  //                      |||||||_labels group 0x10
  //                      ||||||_labels front  0x20
  //                      |||||_absolute
  //                      ||||_centered
  //                      |||_reserved
  //                      ||_reserved
  //                      |_reserved

  public final static int LABEL_MINIMUM_FONTSIZE = 6;
  public final static int LABEL_MAXIMUM_FONTSIZE = 63;
  public final static int LABEL_DEFAULT_FONTSIZE = 13;
  public final static int LABEL_DEFAULT_X_OFFSET = 4;
  public final static int LABEL_DEFAULT_Y_OFFSET = 4;
  public final static int LABEL_OFFSET_MAX = 500; // 0x1F4; 

  private final static int LABEL_OFFSET_MASK = 0x3FF; // 10 bits for each offset (-500 to 500)
  private final static int LABEL_FLAGY_OFFSET_SHIFT = 11; // 11-20 is Y offset
  private final static int LABEL_FLAGX_OFFSET_SHIFT = 21; // 21-30 is X offset

  public final static int LABEL_FLAGS = 0x03F; // does not include absolute or centered
  private final static int LABEL_POINTER_FLAGS = 0x003;
  public final static int LABEL_POINTER_NONE = 0x000;
  public final static int LABEL_POINTER_ON = 0x001; // add label pointer
  public final static int LABEL_POINTER_BACKGROUND = 0x002; // add label pointer to background

  private final static int TEXT_ALIGN_SHIFT = 0x002;
  private final static int TEXT_ALIGN_FLAGS = 0x00C;
  public final static int TEXT_ALIGN_NONE = 0x000;
  public final static int TEXT_ALIGN_LEFT = 0x004;
  public final static int TEXT_ALIGN_CENTER = 0x008;
  public final static int TEXT_ALIGN_RIGHT = 0x00C;

  private final static int LABEL_ZPOS_FLAGS = 0x030;
  public final static int LABEL_ZPOS_GROUP = 0x010;
  public final static int LABEL_ZPOS_FRONT = 0x020;

  public final static int LABEL_EXPLICIT = 0x040;

  private final static int LABEL_CENTERED = 0x100;

  public static int LABEL_DEFAULT_OFFSET = (LABEL_DEFAULT_X_OFFSET << LABEL_FLAGX_OFFSET_SHIFT)
      | (LABEL_DEFAULT_Y_OFFSET << LABEL_FLAGY_OFFSET_SHIFT);

  public final static int ECHO_TOP = 0;
  public final static int ECHO_BOTTOM = 1;
  public final static int ECHO_MIDDLE = 2;
  public final static int ECHO_XY = 3;
  public final static int ECHO_XYZ = 4;

  public final static String scaleName = "%SCALE";
  
  private final static String[] echoNames = { "top", "bottom", "middle", "xy",
      "xyz" };

  public static String getEchoName(int type) {
    return echoNames[type];
  }

  public static int setZPosition(int offset, int pos) {
    return (offset & ~LABEL_ZPOS_FLAGS) | pos;
  }

  public static int setPointer(int offset, int pointer) {
    return (offset & ~LABEL_POINTER_FLAGS) | pointer;
  }

  public static int getPointer(int offset) {
    return offset & LABEL_POINTER_FLAGS;
  }

  public static String getPointerName(int pointer) {
    return ((pointer & LABEL_POINTER_ON) == 0 ? ""
        : (pointer & LABEL_POINTER_BACKGROUND) > 0 ? "background" : "on");
  }

  public static boolean isOffsetAbsolute(int offset) {
    return ((offset & LABEL_EXPLICIT) != 0);
  }

  /**
   * Construct an 32-bit integer packed with 10-byte x and y offsets (-500 to
   * 500) along with flags to indicate if exact and, if not, a flag to indicate
   * that the 0 in x or y indicates "centered". The non-exact default offset of
   * [4,4] is represented as 0 so that new array elements do not have to be
   * initialized.
   * 
   * @param xOffset
   * @param yOffset
   * @param isAbsolute
   * @return packed offset x and y with positioning flags
   */
  public static int getOffset(int xOffset, int yOffset, boolean isAbsolute) {
    xOffset = Math.min(Math.max(xOffset, -LABEL_OFFSET_MAX), LABEL_OFFSET_MAX);
    yOffset = (Math.min(Math.max(yOffset, -LABEL_OFFSET_MAX),
        LABEL_OFFSET_MAX));
    int offset = ((xOffset & LABEL_OFFSET_MASK) << LABEL_FLAGX_OFFSET_SHIFT)
        | ((yOffset & LABEL_OFFSET_MASK) << LABEL_FLAGY_OFFSET_SHIFT)
        | (isAbsolute ? LABEL_EXPLICIT : 0);
    if (offset == LABEL_DEFAULT_OFFSET)
      offset = 0;
    else if (!isAbsolute && (xOffset == 0 || yOffset == 0))
      offset |= LABEL_CENTERED;
    return offset;
  }

  /**
   * X offset in pixels.
   * 
   * negative of this is the actual screen offset
   * 
   * @param offset
   *        0 for an offset indicates "not set" and delivers the default offset
   * @return screen offset from left
   */
  public static int getXOffset(int offset) {
    if (offset == 0)
      return LABEL_DEFAULT_X_OFFSET;
    int x = (offset >> LABEL_FLAGX_OFFSET_SHIFT) & LABEL_OFFSET_MASK;
    x = (x > LABEL_OFFSET_MAX ? x - LABEL_OFFSET_MASK - 1 : x);
    return x;
  }

  /**
   * Y offset in pixels; negative of this is the actual screen offset
   * 
   * @param offset
   *        0 for an offset indicates "not set" and delivers the default offset
   * @return screen offset from bottom
   */
  public static int getYOffset(int offset) {
    if (offset == 0)
      return LABEL_DEFAULT_Y_OFFSET;
    int y = (offset >> LABEL_FLAGY_OFFSET_SHIFT) & LABEL_OFFSET_MASK;
    return (y > LABEL_OFFSET_MAX ? y - LABEL_OFFSET_MASK - 1 : y);
  }

  public static int getAlignment(int offset) {
    return (offset & TEXT_ALIGN_FLAGS);
  }

  public static int setHorizAlignment(int offset, int hAlign) {
    return (offset & ~TEXT_ALIGN_FLAGS) | hAlign;
  }

  private final static String[] hAlignNames = { "", "left", "center", "right" };

  public static String getHorizAlignmentName(int align) {
    return hAlignNames[(align >> TEXT_ALIGN_SHIFT) & 3];
  }

  public static boolean isSmilesCanonical(String options) {
    return (options != null && PT.isOneOf(options.toLowerCase(),
        ";/cactvs///;/cactus///;/nci///;/canonical///;"));
  }

  public static final int SMILES_TYPE_SMILES = 0x1; // placeholder -- DO NOT TEST FOR THIS as it is also in openSMARTS
  public static final int SMILES_TYPE_SMARTS = 0x2; // CmdExt -> matcher
  public static final int SMILES_TYPE_OPENSMILES = 0x5; // includes aromatic normalization of pattern; tests true when openSMARTS as well
  public static final int SMILES_TYPE_OPENSMARTS = 0x7; // 

  public static final int SMILES_FIRST_MATCH_ONLY = 0x8; // 0xFF0 reserved for SmilesMatcher mflag

  public final static int SMILES_NO_AROMATIC = 0x010; //SmilesParser -> SmilesSearch

  public final static int SMILES_IGNORE_STEREOCHEMISTRY = 0x020; //SmilesParser -> SmilesSearch

  public final static int SMILES_INVERT_STEREOCHEMISTRY = 0x040; //SmilesParser -> SmilesSearch

  public static final int SMILES_MAP_UNIQUE = 0x080; //SmilesMatcher return only unique mappings 

  /**
   * AROMATIC_DEFINED draws all aromatic bonds from connection definitions It is
   * deprecated, because a=a will set it by itself.
   */
  public final static int SMILES_AROMATIC_DEFINED = 0x080; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_STRICT enforces Hueckel 4+2 rule, not allowing acyclic double
   * bonds
   * 
   */
  public final static int SMILES_AROMATIC_STRICT = 0x100; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_DOUBLE allows a distinction between single and double, as for
   * example is necessary to distinguish between n=cNH2 and ncNH2 (necessary for
   * MMFF94 atom typing)
   */
  public final static int SMILES_AROMATIC_DOUBLE = 0x200; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_MMFF94 also raises the strictness level to force all 6- and
   * 7-membered rings to have exactly three double bonds.
   */
  public static final int SMILES_AROMATIC_MMFF94 = 0x300; // includes AROMATIC_STRICT and AROMATIC_DOUBLE;

  //  /**
  //   * AROMATIC_JSME_NONCANONICAL matches the JSME noncanonical option.
  //  * 
  //   */
  //  final static int AROMATIC_JSME_NONCANONICAL = 0x800; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_PLANAR only invokes planarity (Jmol default through 14.5)
   * 
   */
  public final static int SMILES_AROMATIC_PLANAR = 0x400; //SmilesParser -> SmilesSearch

  public static final int SMILES_IGNORE_ATOM_CLASS = 0x800;

  public static final int SMILES_GEN_EXPLICIT_H_ALL = 0x00001000; // SmilesExt -> generator
  public static final int SMILES_GEN_EXPLICIT_H2_ONLY = 0x00002000; // SmilesExt -> generator
  public static final int SMILES_GEN_TOPOLOGY = 0x00004000; // SmilesExt -> generator
  public static final int SMILES_GEN_ALL_COMPONENTS = 0x00008000; // SmilesExt -> generator
  public static final int SMILES_GEN_POLYHEDRAL = 0x00010000; // polyhedron -> generator
  public static final int SMILES_GEN_ATOM_COMMENT = 0x00020000; // polyhedron,Viewer -> generator
  public static final int SMILES_GEN_NO_BRANCHES = 0x00040000; // MathExt -> Generator

  public static final int SMILES_GEN_BIO = 0x00100000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS = 0x00300000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_COV_CROSSLINK = 0x00500000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_HH_CROSSLINK = 0x00900000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_COMMENT = 0x01100000; // MathExt -> Viewer
  public static final int SMILES_GEN_BIO_NOCOMMENTS = 0x02100000; // MathExt -> Generator

  public static final int SMILES_GROUP_BY_MODEL = 0x04000000; // MathExt -> search
  public static final int SMILES_2D = 0x08000000; // Viewer -> generator

  // SYNC types

  public static final int JSV_NOT = -1;
  public static final int JSV_SEND_JDXMOL = 0;
  public static final int JSV_SETPEAKS = 7;
  public static final int JSV_SELECT = 14;
  public static final int JSV_STRUCTURE = 21;
  public static final int JSV_SEND_H1SIMULATE = 28;
  public static final int JSV_SEND_C13SIMULATE = 35;
  public static final int NBO_MODEL = 42;
  public static final int NBO_RUN = 49;
  public static final int NBO_VIEW = 56;
  public static final int NBO_SEARCH = 63;
  public static final int NBO_CONFIG = 70;
  public static final int JSV_CLOSE = 77;

  public static int getServiceCommand(String script) {
    return (script.length() < 7 ? -1
        : ("" + "JSPECVI" + "PEAKS: " + "SELECT:" + "JSVSTR:" + "H1SIMUL"
            + "C13SIMU" + "NBO:MOD" + "NBO:RUN" + "NBO:VIE" + "NBO:SEA"
            + "NBO:CON" + "NONESIM")
                .indexOf(script.substring(0, 7).toUpperCase()));
  }

  public static String READER_NOT_FOUND = "File reader was not found:";
  public static String BASE64_TAG = ";base64,";

  public final static int UNITID_MODEL = 1;
  public final static int UNITID_RESIDUE = 2;
  public final static int UNITID_ATOM = 4;
  public final static int UNITID_INSCODE = 8;
  public final static int UNITID_TRIM = 16;

  public static final String DEFAULT_DRAG_DROP_SCRIPT = "zap; load SYNC \"%FILE\";if (%ALLOWCARTOONS && _loadScript == '' && defaultLoadScript == '' && _filetype == 'Pdb') {if ({(protein or nucleic)&*/1.1} && {*/1.1}[1].groupindex != {*/1.1}[0].groupindex){select protein or nucleic;cartoons only;}if ({visible && cartoons > 0}){color structure}else{wireframe -0.1};if (!{visible}){spacefill 23%};select *}";
  /**
   * used to compare two atom *fractional* positions in order to see if they are at the same position.
   * 
   * Q: should change this to unit cell slop? 
   */
  public static final double UC_TOLERANCE2 = 0.0014 * 0.0014; // always use check for LT this, not LTE

  /**
   * Get a unitID type
   * 
   * @param type
   *        -mra (model name, residue, atom, and ins code), -mr (model and
   *        residue; no atom) -ra default - or -r just residue -t right-trim
   * 
   * @return coded type
   */
  public static int getUnitIDFlags(String type) {
    int i = UNITID_RESIDUE | UNITID_ATOM | UNITID_INSCODE;
    if (type.indexOf("-") == 0) {
      if (type.indexOf("m") > 0)
        i |= UNITID_MODEL;
      if (type.indexOf("a") < 0)
        i ^= UNITID_ATOM;
      if (type.indexOf("t") > 0)
        i |= UNITID_TRIM;
    }
    return i;
  }

  public final static String[] globalBooleans = {
      "someModelsHaveFractionalCoordinates", "someModelsHaveSymmetry",
      "someModelsHaveUnitcells", "someModelsHaveCONECT", "isPDB",
      "someModelsHaveDomains", "someModelsHaveValidations", "isSupercell",
      "someModelsHaveAromaticBonds", "someModelsAreModulated" };

  public final static int GLOBAL_FRACTCOORD = 0;
  public final static int GLOBAL_SYMMETRY = 1;
  public final static int GLOBAL_UNITCELLS = 2;
  public final static int GLOBAL_CONECT = 3;
  public final static int GLOBAL_ISPDB = 4;
  public final static int GLOBAL_DOMAINS = 5;
  public final static int GLOBAL_VALIDATIONS = 6;
  public final static int GLOBAL_SUPERCELL = 7;
  public final static int GLOBAL_AROMATICBONDS = 8;
  public static final int GLOBAL_MODULATED = 9;

  public static String getBoolName(int g) {
    return globalBooleans[g];
  }

  public final static double FLOAT_MIN_SAFE = Double.MIN_VALUE; // was 2E-45f; 

  // these literals are used various places. Track them down with their INFO_ name.
  
  public static final String INFO_HM = "HermannMauguinSymbol";
  public static final String INFO_HALL = "HallSymbol";
  public static final String INFO_ITA = "ita";

  public static final String INFO_SPACE_GROUP = "spaceGroup";
  public static final String INFO_SPACE_GROUP_F2C_TITLE = "f2cTitle";
  public static final String INFO_SPACE_GROUP_ASSIGNED = "spaceGroupAssigned";
  public static final String INFO_SPACE_GROUP_INFO = "spaceGroupInfo";
  public static final String INFO_SPACE_GROUP_INDEX = "spaceGroupIndex";
  public static final String INFO_SPACE_GROUP_TITLE = "spaceGroupTitle";
  public static final String INFO_SPACE_GROUP_NAME = "spaceGroupName";
  public static final String INFO_SPACE_GROUP_NOTE = "spaceGroupNote";
  public static final String INFO_UNIT_CELL_PARAMS = "unitCellParams";
  public static final String INFO_UNIT_CELL_OFFSET = "unitCellOffset";
  public static final String INFO_UNIT_CELL_CONVENTIONAL = "unitcell_conventional";
  public static final String INFO_SYMMETRY_OPERATIONS = "symmetryOperations";
  public static final String INFO_SYMOPS_TEMP = "symOpsTemp";
  
  public static final String PROP_DELETE_MODEL_ATOMS = "deleteModelAtoms";
  public static final String PROP_ATOMS_DELETED = "atomsDeleted";
  public static final String PROP_ATOMS_MOVED = "atomsMoved";
  public static final String PROP_ATOMS_LABELED = "atomslabeled";
  
  public static final String INFO_TRAJECTORY_STEPS = "trajectorySteps";
  public static final String INFO_VIBRATION_STEPS = "vibrationSteps";

  /** 
   * used to set atom symmetry 555 556 etc. 
   */
  public static final String INFO_UNIT_CELL_RANGE = "ML_unitCellRange";
//  public static final String INFO_UNIT_CELL_TRANSLATIONS = "unitCellTranslations";
  public static final String INFO_UNIT_CELLS = "unitCells";
  public static final String INFO_UNIT_CELL = "unitcell";

  public static final String FILE_DATA = "fileData";
  public static final String LOAD_OPTION_FILL_RANGE = "fillRange";
  
  /**
   * When UNITCELL NONE is given, clear out all space group and unit cell keys from model info.
   * 
   * @param key
   * @return true to delete
   */
  public static boolean isSpaceGroupInfoKey(String key) {
    return (key.indexOf("nitCell") >= 0
        || key.equals("coordinatesAreFractional")
        || key.startsWith("spaceGroup")
        || key.indexOf("ymmet") >= 0
        || key.startsWith("f2c")              
        || key.startsWith("lattice")
        || key.startsWith("intlTable"));
  }

  /**
   * was a minimum for float as double, but now just Double.MIN_VALUE
   * @param type 
   * @return script for opening file menu for PDB and MOL
   */
  public static String getMenuScript(String type) {
    if (type == "openPDB") {
      return "var x__id__ = _modelTitle; if (x__id__.length != 4) { x__id__ = '1crn'};x__id__ = prompt('"
          + GT.$("Enter a four-digit PDB model ID or \"=\" and a three-digit ligand ID")
          + "',x__id__);if (!x__id__) { quit }; load @{'=' + x__id__}";

    }
    if (type == "openMOL") {
      return "var x__id__ = _smilesString; if (!x__id__) { x__id__ = 'tylenol'};x__id__ = prompt('"
          + GT.$("Enter the name or identifier (SMILES, InChI, CAS) of a compound. Preface with \":\" to load from PubChem; otherwise Jmol will use the NCI/NIH Resolver.")
          + "',x__id__);if (!x__id__) { quit }; load @{(x__id__[1]==':' ? x__id__ : '$' + x__id__)}";

    }
    return null;
  }

}
