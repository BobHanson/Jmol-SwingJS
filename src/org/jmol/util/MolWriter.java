package org.jmol.util;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.viewer.PropertyManager;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

public class MolWriter {

  private Viewer vwr;

  private P3 ptTemp;
  private T3 vNorm;
  private T3 vTemp;

  private int[] connections;


  public MolWriter() {
    
  }
  
  public MolWriter setViewer(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  public boolean addMolFile(int iModel, SB mol, BS bsAtoms, BS bsBonds,
                            boolean asV3000, boolean asJSON,
                            boolean noAromatic, Quat q) {
   int nAtoms = bsAtoms.cardinality();
   int nBonds = bsBonds.cardinality();
   if (!asV3000 && !asJSON && (nAtoms > 999 || nBonds > 999))
     return false;
   boolean asSDF = (iModel >= 0);
   @SuppressWarnings("unchecked")
   Map<String, Object> molData = (asSDF
       ? (Map<String, Object>) vwr.ms.getInfo(iModel, "molData")
       : null);
   @SuppressWarnings("unchecked")
   Lst<String> _keyList = (asSDF ? (Lst<String>) vwr.ms.getInfo(iModel, "molDataKeys") : null);
   ModelSet ms = vwr.ms;
   int[] atomMap = new int[ms.ac];
   P3 pTemp = new P3();
   if (asV3000) {
     mol.append("  0  0  0  0  0  0            999 V3000");
   } else if (asJSON) {
     mol.append("{\"mol\":{\"createdBy\":\"Jmol " + Viewer.getJmolVersion()
         + "\",\"a\":[");
   } else {
     PT.rightJustify(mol, "   ", "" + nAtoms);
     PT.rightJustify(mol, "   ", "" + nBonds);
     mol.append("  0  0  0  0            999 V2000");
   }
   if (!asJSON)
     mol.append("\n");
   if (asV3000) {
     mol.append("M  V30 BEGIN CTAB\nM  V30 COUNTS ").appendI(nAtoms)
         .append(" ").appendI(nBonds).append(" 0 0 0\n")
         .append("M  V30 BEGIN ATOM\n");
   }
   Object o = (molData == null ? null : molData.get("atom_value_name"));
   if (o instanceof SV)
     o = ((SV) o).asString();
   int valueType = (o == null ? T.nada : T.getTokFromName("" + o));
   SB atomValues = (valueType == T.nada && !asSDF ? null : new SB());
   for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
       .nextSetBit(i + 1)) {
     getAtomRecordMOL(iModel, ms, mol, atomMap[i] = ++n, ms.at[i], q, pTemp,
         asV3000, asJSON, atomValues, valueType, asSDF);
   }
   if (asV3000) {
     mol.append("M  V30 END ATOM\nM  V30 BEGIN BOND\n");
   } else if (asJSON) {
     mol.append("],\"b\":[");
   }
   for (int i = bsBonds.nextSetBit(0), n = 0; i >= 0; i = bsBonds
       .nextSetBit(i + 1))
     getBondRecordMOL(mol, ++n, ms.bo[i], atomMap, asV3000, asJSON,
         noAromatic);
   if (asV3000) {
     mol.append("M  V30 END BOND\nM  V30 END CTAB\n");
   }
   if (asJSON)
     mol.append("]}}");
   else {
     if (atomValues != null && atomValues.length() > 0)
       mol.append(atomValues.toString());
     mol.append("M  END\n");
   }
   if (asSDF) {
     try {
       float[] pc = ms.getPartialCharges();
       if (molData == null)
         molData = new Hashtable<String, Object>();
       SB sb = new SB();
       if (pc != null) {
         sb.appendI(nAtoms).appendC('\n');
         for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
             .nextSetBit(i + 1))
           sb.appendI(++n).append(" ").appendF(pc[i]).appendC('\n');
         molData.put("jmol_partial_charges", sb.toString());
       }
       sb.setLength(0);
       sb.appendI(nAtoms).appendC('\n');
       for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
           .nextSetBit(i + 1)) {
         String name = ms.at[i].getAtomName().trim();
         if (name.length() == 0)
           name = ".";
         sb.appendI(++n).append(" ").append(name.replace(' ', '_'))
             .appendC('\n');
       }
       molData.put("jmol_atom_names", sb.toString());
       if (_keyList == null)
         _keyList = new Lst<String>();
       for (String key : molData.keySet())
         if (!_keyList.contains(key))
           _keyList.addLast(key);
       for (int i = 0, n = _keyList.size(); i < n; i++) {
         String key = _keyList.get(i);
         if (key.startsWith(">"))
           continue;
         o = molData.get(key);
         if (o instanceof SV)
           o = ((SV) o).asString();
         mol.append("> <" + key.toUpperCase() + ">\n");
         output80CharWrap(mol, o.toString(), 80);
         mol.append("\n\n");
       }
     } catch (Throwable e) {
       // ignore
     }
     mol.append("$$$$\n");
   }
   return true;
 }

  
  /*
  L-Alanine
  GSMACCS-II07189510252D 1 0.00366 0.00000 0
  Figure 1, J. Chem. Inf. Comput. Sci., Vol 32, No. 3., 1992
  0 0 0 0 0 999 V3000
  M  V30 BEGIN CTAB
  M  V30 COUNTS 6 5 0 0 1
  M  V30 BEGIN ATOM
  M  V30 1 C -0.6622 0.5342 0 0 CFG=2
  M  V30 2 C 0.6622 -0.3 0 0
  M  V30 3 C -0.7207 2.0817 0 0 MASS=13
  M  V30 4 N -1.8622 -0.3695 0 0 CHG=1
  M  V30 5 O 0.622 -1.8037 0 0
  M  V30 6 O 1.9464 0.4244 0 0 CHG=-1
  M  V30 END ATOM
  M  V30 BEGIN BOND
  M  V30 1 1 1 2
  M  V30 2 1 1 3 CFG=1
  M  V30 3 1 1 4
  M  V30 4 2 2 5
  M  V30 5 1 2 6
  M  V30 END BOND
  M  V30 END CTAB
  M  END
   */

  private void getAtomRecordMOL(int iModel, ModelSet ms, SB mol, int n, Atom a, Quat q,
                                P3 pTemp, boolean asV3000, boolean asJSON, SB atomValues, 
                                int tokValue, boolean asSDF) {
    //https://cactus.nci.nih.gov/chemical/structure/caffeine/file?format=sdf&get3d=true
    //__Jmol-14_06161707413D 1   1.00000     0.00000     0
    //Jmol version 14.19.1  2017-06-12 00:33 EXTRACT: ({0:23})
    // 24 25  0  0  0  0              1 V2000
    //    1.3120   -1.0479    0.0025 N   0  0  0  0  0  0
    //    2.2465   -2.1762    0.0031 C   0  0  0  0  0  0
    //    1.7906    0.2081    0.0011 C   0  0  0  0  0  0
    //xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee
    //012345678901234567890123456789012
    PropertyManager.getPointTransf(iModel, ms, a, q, pTemp);
    int elemNo = a.getElementNumber();
    String sym = (a.isDeleted() ? "Xx" : Elements
        .elementSymbolFromNumber(elemNo));
    int isotope = a.getIsotopeNumber();
    int charge = a.getFormalCharge();
    Object [] o = new Object[] { pTemp };
    if (asV3000) {
      mol.append("M  V30 ").appendI(n).append(" ").append(sym)
            .append(PT.sprintf(" %12.5p %12.5p %12.5p 0", "p", o));
      if (charge != 0)
        mol.append(" CHG=").appendI(charge);
      if (isotope != 0)
        mol.append(" MASS=").appendI(isotope);
      mol.append("\n");
    } else if (asJSON) {
      if (n != 1)
        mol.append(",");
      mol.append("{");
      if (a.getElementNumber() != 6)
        mol.append("\"l\":\"").append(a.getElementSymbol()).append("\",");
      if (charge != 0)
        mol.append("\"c\":").appendI(charge).append(",");
      if (isotope != 0)
        mol.append("\"m\":").appendI(isotope).append(",");
      mol.append("\"x\":").appendF(a.x).append(",\"y\":").appendF(a.y)
          .append(",\"z\":").appendF(a.z).append("}");
    } else {
      mol.append(PT.sprintf("%10.4p%10.4p%10.4p", "p", o));
      mol.append(" ").append(sym);
      if (sym.length() == 1)
        mol.append(" ");
      PT.rightJustify(mol, "   ", "" + (isotope > 0 ? isotope - Elements.getNaturalIsotope(a.getElementNumber()) : 0));
      if (asSDF && isotope > 0) {
          atomValues.append("M  ISO  1");
          PT.rightJustify(atomValues, "    ", "" + n);
          PT.rightJustify(atomValues, "    ", "" + isotope);
          atomValues.append("\n");
      }

      PT.rightJustify(mol, "   ", "" + (charge == 0 ? 0 : 4 - charge));
      mol.append("  ").append(getAtomParity(a));
      mol.append("  0  0  0\n");
      String label = (tokValue == T.nada || asV3000 ? null : 
        getAtomPropertyAsString(a, tokValue));
      if (label != null && (label = label.trim()).length() > 0) {
        String sn = "   " + n + " ";
        atomValues.append("V  ").append(sn.substring(sn.length() - 4));
        output80CharWrap(atomValues, label, 73);
      }
    }
  }

  private String getAtomParity(Atom a) {
    if (a.getCovalentBondCount() == 4) {
      if (connections == null) {
        connections = new int[4];
        vTemp = new V3();
        vNorm = new V3();
      }
      Bond[] bonds = a.bonds;
      int nH = 0;
      for (int pt = 0, i = bonds.length; --i >= 0;) {
        if (bonds[i].isCovalent()) {
          Atom b = bonds[i].getOtherAtom(a);
          if (b.getAtomicAndIsotopeNumber() == 1)
            nH++;
          connections[pt++] = b.i;
        }
      }
      if (nH < 3) {
        Arrays.sort(connections);
        Atom[] atoms = vwr.ms.at;
        Measure.getNormalThroughPoints(atoms[connections[0]],
            atoms[connections[1]], atoms[connections[2]], vNorm, vTemp);
        vTemp.sub2(atoms[connections[3]], atoms[connections[0]]);
        return (vTemp.dot(vNorm) > 0 ? "1" : "2");
      }
    }
    return "0";
  }
  

  private String getAtomPropertyAsString(Atom a, int tok) {
    switch (tok & T.PROPERTYFLAGS) {
    case T.intproperty:
      int i = a.atomPropertyInt(tok);
      return (tok == T.color ? PT.trim(Escape.escapeColor(i),"[x]").toUpperCase() : "" + i);
    case T.strproperty:
      return a.atomPropertyString(vwr, tok);
    case T.floatproperty:
      float f = a.atomPropertyFloat(vwr, tok, null);
      return (Float.isNaN(f) ? null : "" + f);
    default: // point property
      if (ptTemp == null)
        ptTemp = new P3();
      a.atomPropertyTuple(vwr, tok, ptTemp);
      return  (ptTemp == null ? null : ptTemp.toString());
    }
  }

  private void getBondRecordMOL(SB mol, int n, Bond b, int[] atomMap,
                                boolean asV3000, boolean asJSON, boolean noAromatic) {
    //  1  2  1  0
    int a1 = atomMap[b.atom1.i];
    int a2 = atomMap[b.atom2.i];
    int order = b.getValence();
    if (order > 3)
      order = 1;
    switch (b.order & ~Edge.BOND_NEW) {
    case Edge.BOND_AROMATIC:
      order = (asJSON ? -3 : 4);
      break;
    case Edge.BOND_PARTIAL12:
      order = (asJSON ? -3 : 5);
      break;
    case Edge.BOND_AROMATIC_SINGLE:
      order = (asJSON || noAromatic ? 1: 6);
      break;
    case Edge.BOND_AROMATIC_DOUBLE:
      order = (asJSON || noAromatic  ? 2: 7);
      break;
    case Edge.BOND_PARTIAL01:
      order = (asJSON ? -1: 8);
      break;
    }
    if (asV3000) {
      mol.append("M  V30 ").appendI(n).append(" ").appendI(order).append(" ")
          .appendI(a1).append(" ").appendI(a2).appendC('\n');
    } else if (asJSON) {
      if (n != 1)
        mol.append(",");
      mol.append("{\"b\":").appendI(a1 - 1).append(",\"e\":").appendI(a2 - 1);
      if (order != 1) {
        mol.append(",\"o\":");
        if (order < 0) {
          mol.appendF(-order / 2f);
        } else {
          mol.appendI(order);   
        }
      }
      mol.append("}");
    } else {
      PT.rightJustify(mol, "   ", "" + a1);
      PT.rightJustify(mol, "   ", "" + a2);
      mol.append("  ").appendI(order).append("  0  0  0\n");
    }
  }

  
  
  
 /**
  * 
  * @param mol
  * @param data
  * @param maxN 80 for multi-line wrap; something smaller for single line output
  */
 private void output80CharWrap(SB mol, String data, int maxN) {
   if (maxN < 80)
     data = PT.rep(data, "\n", "|");
   String[] lines = PT.split(PT.trim(PT.rep(data, "\n\n", "\n"), "\n"), "\n");
   for (int i = 0; i < lines.length; i++)
     outputLines(mol, lines[i], maxN);
 }

 private void outputLines(SB mol, String data, int maxN) {
   boolean done = false;    
   for (int  i = 0, n = data.length(); i < n && !done; i += 80) {
     mol.append(data.substring(i, Math.min(i + maxN, n)));
     if (!(done = (maxN != 80)) && i + 80 < n)
       mol.append("+");
     mol.append("\n");
   }

   // TODO
   
 }



}
