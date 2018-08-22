package org.jmol.adapter.readers.quantum;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

public class NBOParser {

  private Viewer vwr;
  private boolean haveBeta;

  public NBOParser() {
    // for reflection
  }
  
  public NBOParser set(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  public Lst<Object> getAllStructures(String output, Lst<Object> list) {
    if (output == null)
      return null;
    if (list == null)
      list = new Lst<Object>();
    // $CHOOSE record in the .nbo file
    output = PT.rep(output,  "the $CHOOSE", "");
    getStructures(getBlock(output, "$CHOOSE"), "CHOOSE", list);
    
    
    // NRTSTR, NTRSTRA, NTRSTRB in the .nbo file
    getStructures(getBlock(output, "$NRTSTR"), "NRTSTR", list);
    getStructures(getBlock(output, "$NRTSTRA"), "NRTSTRA", list);
    getStructures(getBlock(output, "$NRTSTRB"), "NRTSTRB", list);
    
    
    // TOPO section in the alpha or beta sections of the .nbo file
    getStructuresTOPO(getData(output, "TOPO matrix", "* Total *", 1), "RSA", list);
    getStructuresTOPO(getData(output, "TOPO matrix", "* Total *", 2), "RSB", list);
    return list;
  }
  
  private String getBlock(String output, String key) {
    int pt = output.indexOf(key);
    int pt1 = output.indexOf("$END", pt + 1);
    return (pt < 0 || pt1 < 0 ? null : output.substring(pt + key.length(), pt1));
  }
  

  //NBO ALPHA  55
//C 1(cr)   C 2(cr)   C 3(cr)   C 3(lp)   C 1- C 2  C 1- C 2  C 1- H 4 
//C 1- H 5  C 2- C 3  C 2- H 6  C 3- H 7  C 3- H 8  C 1- C 2* C 1- C 2*
//C 1- H 4* C 1- H 5* C 2- C 3* C 2- H 6* C 3- H 7* C 3- H 8* C 1(ry)  
//C 1(ry)   C 1(ry)   C 1(ry)   C 1(ry)   C 1(ry)   C 1(ry)   C 1(ry)  
//C 1(ry)   C 1(ry)   C 2(ry)   C 2(ry)   C 2(ry)   C 2(ry)   C 2(ry)  
//C 2(ry)   C 2(ry)   C 2(ry)   C 2(ry)   C 2(ry)   C 3(ry)   C 3(ry)  
//C 3(ry)   C 3(ry)   C 3(ry)   C 3(ry)   C 3(ry)   C 3(ry)   C 3(ry)  
//C 3(ry)   H 4(ry)   H 5(ry)   H 6(ry)   H 7(ry)   H 8(ry)  

  /**
   * Use the .46 file NBO alpha/beta labels to identify bonds, lone pairs, and lone valences.
   * 
   * @param tokens
   * @param type
   * @param structures
   * @param nAtoms 
   */
  public static void getStructures46(String[] tokens, String type,
                                     Lst<Object> structures, int nAtoms) {
    if (tokens == null)
      return;
    Map<String, Object> htData = new Hashtable<String, Object>();
    structures.addLast(htData);
    int[][] matrix = new int[nAtoms][nAtoms];
    htData.put("matrix", matrix);
    htData.put("type", type); // alpha or beta
    htData.put("spin", type);
    htData.put("index", Integer.valueOf(0));
    for (int n = tokens.length, i = 0; i < n; i++) {
      String org = tokens[i];
      if (org.contains("(ry)"))
        break;
      if (org.contains("*") || org.contains("(cr)"))
        continue;
      // lone pair or lone valence
      boolean isLP = org.endsWith("(lp)");
      
      if (isLP || org.endsWith("(lv)")) {
        int ia = getAtomIndex(org.substring(0, org.length() - 4));
        matrix[ia][ia]+= (isLP ? 1 : 10);
        continue;
      }
      // bond
      String[] names = PT.split(org, "-");
      if (names.length == 3) {
        // three-center bond -- ignored?
        System.out.println("NBOParser 3-center bonnd " + org + " ignored for Kekule structure");
        continue;
      }
      int ia = getAtomIndex(names[0]);
      int ib = getAtomIndex(names[1]);
      matrix[ia][ib]++;
      
    }
    dumpMatrix(type, 0, matrix);
  }

  private static int getAtomIndex(String xx99) {
    for (int n = xx99.length(), i = n, val = 0, pow = 1, ch = 0; --i >= 0;) {
      if ((ch = xx99.charAt(i)) < 48 || ch > 57)
        return val - 1;
      val += (ch - 48) * pow;
      pow *= 10;
    }
    return 0;
  }


  
  //  TOPO matrix for the leading resonance structure:
  //
  //    Atom  1   2   3
  //    ---- --- --- ---
  //  1.  O   2   2   0
  //  2.  C   2   0   2
  //  3.  O   0   2   1
  //
  //        Resonance
  //   RS   Weight(%)                  Added(Removed)
  //---------------------------------------------------------------------------
  //   1*(2)  24.76
  //   2*(2)  24.72   ( O  1),  O  3
  //   3*(2)  24.69    O  1- C  2, ( C  2- O  3), ( O  1),  O  3
  //   4*     24.61   ( O  1- C  2),  C  2- O  3
  //   5       0.23   ( O  1- C  2),  O  1- O  3, ( O  1),  C  2
  //   6       0.20    O  1- C  2,  O  1- C  2, ( C  2- O  3), ( C  2- O  3),
  //                  ( O  1), ( O  1),  O  3,  O  3
  //   7       0.17    O  1- O  3, ( C  2- O  3), ( O  1), ( O  1),  C  2,
  //                   O  3
  //   8       0.16   ( O  1- C  2), ( O  1- C  2),  C  2- O  3,  C  2- O  3,
  //                   O  1, ( O  3)
  //   9       0.12    O  1- O  3, ( C  2- O  3), ( O  1),  C  2
  //  10       0.12   ( O  1- C  2),  C  2- O  3,  O  1, ( O  3)
  //  11-20    0.22
  //---------------------------------------------------------------------------
  //         100.00   * Total *                [* = reference structure]
  //

  private void getStructuresTOPO(String data, String nrtType, Lst<Object> list) {
    if (data == null || data.length() == 0)
      return;
    String[] parts = PT.split(data, "Resonance");
    if (parts.length < 2)
      return;
    int pt = parts[0].lastIndexOf(".");
    int nAtoms = PT.parseInt(parts[0].substring(pt - 3, pt));
    if (nAtoms < 0)
      return;
    // decode the top table
    String[] tokens = PT.getTokens(PT.rep(PT.rep(parts[0], ".", ".1"), "Atom",
        "-1"));
    float[] raw = new float[tokens.length];
    int n = PT.parseFloatArrayInfested(tokens, raw);
    int[][] table = new int[nAtoms][nAtoms];
    int atom1 = -1, atom2 = 0, atom0 = 0;
    for (int i = 0; i < n; i++) {
      float f = raw[i];
      if (f < 0) {
        // start of table
        atom1 = -1;
        continue;
      }
      if (f % 1 == 0) {
        if (atom1 == -1) {
          // first atom in header
          atom0 = (int) (f);
          atom1 = -2;
        }
        // value or header
        if (atom1 < 0)
          continue;
        table[atom1][atom2++] = (int) f;
      } else {
        // new row
        atom1 = (int) (f - 1);
        atom2 = atom0 - 1;
      }
    }
    //    Resonance
    //    RS   Weight(%)                  Added(Removed)
    //---------------------------------------------------------------------------
    //    1*     16.80
    //    2*     16.80   ( C  2- C  3),  C  2- C 10,  C  3- C  4, ( C  4- C  7),
    //                    C  7- C  8, ( C  8- C 10), ( C 12- C 13),  C 12- C 14,
    //                    C 13- C 15, ( C 14- C 16), ( C 15- C 21),  C 16- C 21

    int[][] matrix = null;
    // turn this listing into a numeric array. decimal points indicate new atoms
    tokens = parts[1].split("\n");
    String s = "";
    for (int i = 3; i < tokens.length; i++)
      if (tokens[i].indexOf("--") < 0)
        s += tokens[i].substring(10) + "\n";    
    s = s.replace('-', ' ');
    s = PT.rep(s, ".", ".1");
    s = PT.rep(s, "(", " -1 ");
    s = PT.rep(s, ")", " -2 ");
    s = PT.rep(s, ",", " -3 ");
    tokens = PT.getTokens(s);
    raw = new float[tokens.length];
    n = PT.parseFloatArrayInfested(tokens, raw);
    Map<String, Object> htData = null;
    int dir = 1;
    atom1 = atom2 = -1;
    for (int i = 0, index = 0; i < n; i++) {
      float f = raw[i];
      float remain = f % 1;
      if (remain == 0) {
        int v = (int) f;
        switch (v) {
        case -1: // (
          dir = -1;
          atom1 = atom2 = -1;
          continue;
        case -2: // )
          break;
        case -3: // ,
          if (atom1 < 0)
            continue;
          break;
        default:
          if (atom1 < 0) {
            atom1 = atom2 = v - 1;
          } else {
            atom2 = v - 1;
          }
          continue;
        }
        matrix[atom1][atom2] += dir;
        atom1 = atom2 = -1;
        dir = 1;
      } else {
        if (htData == null)
          matrix = table;
        dumpMatrix(nrtType, index, matrix);
        
        if (raw[i + 2] == 0) 
          break;
        list.addLast(htData = new Hashtable<String, Object>());
        s = "" + ((int) f * 100 + (int) ((remain - 0.0999999) * 1000));
        int len = s.length();
        s = (len == 2 ? "0" : "") + s.substring(0, len - 2) + "."
            + s.substring(len - 2);
        htData.put("weight", s);
        htData.put("index", Integer.valueOf(index++));
        htData.put("type", nrtType.toLowerCase());
        htData.put("spin", nrtType.indexOf("B") >= 0 ? "beta" : "alpha");
        matrix = new int[nAtoms][nAtoms];
        htData.put("matrix", matrix);
        for (int j = 0; j < nAtoms; j++)
          for (int k = 0; k < nAtoms; k++)
            matrix[j][k] = table[j][k];
      }
    }
  }


  private static void dumpMatrix(String nrtType, int index, int[][] matrix) {
    System.out.println("NBOParser matrix " + nrtType + " " + index);
    for (int j = 0, nAtoms = matrix.length; j < nAtoms; j++)
      System.out.println(PT.toJSON(null, matrix[j]));
    System.out.println("-------------------");
  }


  private String getData(String output, String start, String end, int n) {
    int pt = 0, pt1 = 0;
    for (int i = 0; i < n; i++) {
      pt = output.indexOf(start, pt1 + 1);
      pt1 = output.indexOf(end, pt + 1);
    }
    return (pt < 0 || pt1 < 0 ? null : output.substring(pt, pt1));
  }


  /**
   * Reads the $NRTSTR $NRTSTRA, $NRTSTRB, and $CHOOSE blocks. Creates a Lst of
   * Hashtables
   * 
   * @param data
   *        NBO output block not including $END
   * 
   * @param nrtType
   *        "CHOOSE", "NRTSTRA", "NRTSTRB"
   * @param list
   *        to fill
   * @return number of structures found or -1 for an error
   * 
   */
  public int getStructures(String data, String nrtType, Lst<Object> list) {

    //    $NRTSTRA
    //    STR        ! Wgt = 49.51%
    //      LONE 1 2 3 2 END
    //      BOND D 1 2 D 2 3 END
    //    END
    //    STR        ! Wgt = 25.63%
    //      LONE 1 3 3 1 END
    //      BOND S 1 2 T 2 3 END
    //    END
    //    STR        ! Wgt = 24.45%
    //      LONE 1 1 3 3 END
    //      BOND T 1 2 S 2 3 END
    //    END
    //  $END

    //    $CHOOSE
    //    ALPHA
    //     LONE 1 1 3 3 END
    //     BOND T 1 2 S 2 3 END
    //    END
    //    BETA
    //     LONE 1 1 3 2 END
    //     BOND D 1 2 S 2 3 END
    //       3C S 1 2 3 END
    //    END
    //   $END

    //  $CHOOSE
    //    BOND D 1 2 S 1 6 S 1 7 S 2 3 S 2 8 D 3 4 S 3 9 S 4 5 S 4 10 D 5 6 S 5 11
    //         S 6 12 END
    //  $END

    if (data == null || data.length() == 0)
      return 0;
    int n = 0;
    try {
      boolean ignoreSTR = (data.indexOf("ALPHA") >= 0);
      if (!ignoreSTR && !data.contains("STR"))
        data = "STR " + data + " END";
      nrtType = nrtType.toLowerCase();
      String spin = (nrtType.equals("nrtstrb") ? "beta" : "alpha");
      if (nrtType.equals("choose"))
        nrtType = null;
      Map<String, Object> htData = null;
      String[] tokens = PT.getTokens(data.replace('\r', ' ').replace('\n', ' ').replace('\t', ' '));
      String lastType = "";
      int index = 0;
      for (int i = 0, nt = tokens.length; i < nt; i++) {
        String tok = tokens[i];
        //       0         1         2         3         4
        //       01234567890123456789012345678901234567890
        switch ("STR  =    ALPHABETA LONE BOND 3C".indexOf(tok)) {
        case 0:
          if (ignoreSTR)
            continue;
          tok = spin;
          //$FALL-THROUGH$
        case 10:
        case 15:
          list.addLast(htData = new Hashtable<String, Object>());
          if (!lastType.equals(tok)) {
            lastType = tok;
            index = 0;
          }
          htData.put("index", Integer.valueOf(index++));
          htData.put("spin", spin = tok.toLowerCase());
          if (spin.equals("beta"))
            haveBeta = true;
          htData.put("type", nrtType == null ? "choose" + spin.substring(0, 1) : nrtType);
          n++;
          break;
        case 5:
          htData.put("weight", tokens[++i]);
          break;
        case 20: // LONE
          Lst<int[]> lone = new Lst<int[]>();
          htData.put("lone", lone);
          while (!(tok = tokens[++i]).equals("END")) {
            int at1 = Integer.parseInt(tok);
            int nlp = Integer.parseInt(tokens[++i]);
            lone.addLast(new int[] { nlp, at1 });
          }
          break;
        case 25: // BOND
          Lst<int[]> bonds = new Lst<int[]>();
          htData.put("bond", bonds);
          while (!(tok = tokens[++i]).equals("END")) {
            int order = "DTQ".indexOf(tok.charAt(0)) + 2;
            int at1 = Integer.parseInt(tokens[++i]);
            int at2 = Integer.parseInt(tokens[++i]);
            bonds.addLast(new int[] { order, at1, at2 });
          }
          break;
        case 30: // 3C
          Lst<int[]> threeCenter = new Lst<int[]>();
          htData.put("3c", threeCenter);
          while (!(tok = tokens[++i]).equals("END")) {
            int order = "DTQ".indexOf(tok.charAt(0)) + 2;
            int at1 = Integer.parseInt(tokens[++i]);
            int at2 = Integer.parseInt(tokens[++i]);
            int at3 = Integer.parseInt(tokens[++i]);
            threeCenter.addLast(new int[] { order, at1, at2, at3 });
          }
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      list.clear();
      return -1;
    }
    return n;
  }



  public boolean isOpenShell() {
    return haveBeta;
  }

  /**
   * 
   * Find the map for a specified structure, producing a structure that can be used to generate lone pairs and bonds for a Lewis structure 
   * 
   * @param structureList  a list of structural information from this class created from an NBO file
   * @param type  topoa, topob, nrtstra, nrtstrb, alpha, beta  -- last two are from CHOOSE
   * @param index  0-based index for this type
   * @return Hashtable or null
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getStructureMap(Lst<Object> structureList,
                                                    String type, int index) {
    if (type == null || structureList == null)
      return null;
    type = type.toLowerCase();
    String spin = (type.indexOf("b") < 0 ? "alpha" : "beta");
    for (int i = 0; i < structureList.size(); i++) {
      Map<String, Object> map = (Map<String, Object>) structureList.get(i);
      if (spin.equals(map.get("spin")) && type.equals(map.get("type"))
          && (index < 0 || index == ((Integer) map.get("index")).intValue())) {      
        return map;
      }
    }
    return null;
  }

  /**
   * 
   * @param modelIndex
   * @param type
   *        one of alpha|beta|choosea|chooseb|nrtstr_n|nrtstra_n|topo_n|topoa_n|
   *        topob_n
   * @return true if successful
   */
  @SuppressWarnings("unchecked")
  public boolean connectNBO(int modelIndex, String type) {
    try {
      if (type == null)
        type = "alpha";
      type = type.toLowerCase();
      if (type.length() == 0 || type.equals("46"))
        type = "alpha";
      Map<String, Object> map = vwr.ms.getModelAuxiliaryInfo(modelIndex);
      haveBeta = map.containsKey("isOpenShell");
      Lst<Object> list = (Lst<Object>) map.get("nboStructures");
      if (list == null || list.size() == 0)
        return false;
      type = type.toLowerCase();
      int index = type.indexOf("_");
      if (index > 0) {
        if (list.size() <= 2) {
          String fname = (String) map.get("fileName");
          if (fname != null && !fname.endsWith(".nbo")) {
            fname = fname.substring(0, fname.lastIndexOf(".")) + ".nbo";
            getAllStructures(vwr.getAsciiFileOrNull(fname), list);
          }
        }
        String[] tokens = PT.split(type, "_");
        index = PT.parseInt(tokens[1]) - 1;
        type = tokens[0];
        
      } else {
        index = 0;
      }
      Map<String, Object> structureMap = getStructureMap(list, type, index);
      if (structureMap == null
          || !setJmolLewisStructure(structureMap, modelIndex, index + 1)) {
//        map.remove("nboStructure");
        return false;
      }
      map.put("nboStructure", structureMap);
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  /**
   * Starting with a structure map, do what needs to be done to change the
   * current Jmol structure to that in terms of bonding and formal charge.
   * 
   * Three map configurations are supported:
   * 
   * bond/lone/3c data from $CHOOSE or $NRTSTR, $NRTSTRA, or $NRTSTRB matrix
   * from TOPO map of atom names from the .46 file NBO alpha/beta lists
   * 
   * @param structureMap
   * 
   * @param modelIndex
   * @param resNo 
   * 
   * @return true if successful
   */
  @SuppressWarnings("unchecked")
  private boolean setJmolLewisStructure(Map<String, Object> structureMap,
                                       int modelIndex, int resNo) {
    if (structureMap == null || modelIndex < 0)
      return false;
    String type = (String) structureMap.get("type");
    System.out.println("creating structure " + modelIndex + " " + type);
    Lst<Object> bonds = (Lst<Object>) structureMap.get("bond");
    Lst<Object> lonePairs = (Lst<Object>) structureMap.get("lone");
    int[][] matrix = (int[][]) structureMap.get("matrix");
    int[] lplv = (int[]) structureMap.get("lplv");
    int[] bondCounts = (int[]) structureMap.get("bondCounts");
    boolean needLP = (lplv == null);
    BS bsAtoms = vwr.ms.getModelAtomBitSetIncludingDeleted(modelIndex, false);
    int atomCount = bsAtoms.cardinality();
    int iatom0 = bsAtoms.nextSetBit(0);
    if (matrix != null && atomCount != matrix.length)
      return false;
    if (matrix != null)
      dumpMatrix(type, resNo, matrix);
    if (needLP) {
      structureMap.put("lplv", lplv = new int[atomCount]);
      structureMap.put("bondCounts", bondCounts = new int[atomCount]);
    }
    if (needLP) {
      if (lonePairs != null) {
        for (int i = lonePairs.size(); --i >= 0;) {
          int[] na = (int[]) lonePairs.get(i);
          int nlp = na[0];
          int a1 = na[1] - 1;
          lplv[a1] = nlp;
        }
      } else if (matrix != null) {
        for (int i = atomCount; --i >= 0;) {
          lplv[i] = matrix[i][i];
        }
      }
    }

    // create bonds
    vwr.ms.deleteModelBonds(modelIndex);
    int mad = vwr.ms.getDefaultMadFromOrder(1);
    if (bonds != null) {
      for (int i = bonds.size(); --i >= 0;) {
        int[] oab = (int[]) bonds.get(i);
        int a1 = iatom0 + oab[1] - 1;
        int a2 = iatom0 + oab[2] - 1;
        int order = oab[0];
        if (needLP) {
          bondCounts[a1] += order;
          bondCounts[a2] += order;
        }
        vwr.ms.bondAtoms(vwr.ms.at[a1], vwr.ms.at[a2], order, (short) mad,
            bsAtoms, 0, true, true);
      }
    } else if (matrix != null) {
      for (int i = 0; i < atomCount - 1; i++) {
        int[] m = matrix[i];
        for (int j = i + 1; j < atomCount; j++) {
          int order = m[j];
          if (order == 0)
            continue;
          System.out.println("adding bond " + vwr.ms.at[i + iatom0] + " " + vwr.ms.at[j + iatom0]);
          vwr.ms.bondAtoms(vwr.ms.at[i + iatom0], vwr.ms.at[j + iatom0], order, (short) mad,
              null, 0, false, true);
          if (needLP) {
            bondCounts[i] += order;
            bondCounts[j] += order;
          }
        }
      }
    }
    for (int i = 0, ia = bsAtoms.nextSetBit(0); ia >= 0; ia = bsAtoms
        .nextSetBit(ia + 1), i++) {
      // It is not entirely possible to determine charge just by how many
      // bonds there are to an atom. But we can come close for most standard
      // structures - NOT CO2(+), though.
      Atom a = vwr.ms.at[ia];
      a.setValence(bondCounts[i]);
      a.setFormalCharge(0);
      int nH = vwr.ms.getMissingHydrogenCount(a, true);
      if (a.getElementNumber() == 6 && nH == 1) {
        // for carbon, we need to adjust for lone pairs.
        // sp2 C+ will be "missing one H", but effectively we want to consider it 
        // "one H too many", referencing to carbene (CH2) instead of methane (CH4).
        // thus setting its charge to 1+, not 1-
        if (bondCounts[i] == 3 && lplv[i] % 10 == 0 || bondCounts[i] == 2)
          nH -= 2;
      }
      a.setFormalCharge(-nH);
    }
    return true;
  }


  /**
   * get the 
   * @param a
   * @return label including (lp), (lv), and (if not open-spin) formal charge
   */
  @SuppressWarnings("unchecked")
  public String getNBOAtomLabel(Atom a) {
    String name = a.getAtomName();
    int modelIndex = a.getModelIndex();
    Map<String, Object> structureMap = (Map<String, Object>) vwr.ms.getModelAuxiliaryInfo(modelIndex).get("nboStructure");
    if (vwr == null || structureMap == null)
      return name;
    int[] lplv = (int[]) structureMap.get("lplv");
    int i = a.i - vwr.ms.am[modelIndex].firstAtomIndex;
    boolean addFormalCharge = vwr.getBoolean(T.nbocharges);
    int charge = (addFormalCharge ? vwr.ms.at[i].getFormalCharge() : 0);
    if (lplv[i] == 0 && charge == 0)
      return name;
    if (lplv[i] % 10 > 0)
      name = "<sup>(" + (lplv[i] % 10) + ")</sup>" + name;
    if (lplv[i] >= 10)
      name = "*" + name;//"<sup>*</sup>" + name;
    if (addFormalCharge) {
      if (charge != 0)
        name += "<sup>" + Math.abs(charge)
            + (charge > 0 ? "+" : charge < 0 ? "-" : "") + "</sup>";
    }
    return name;
  }
  
  
}
