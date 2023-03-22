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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.api.JmolInChI;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.smiles.SmilesAtom;
import org.jmol.smiles.SmilesBond;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;
import net.sf.jniinchi.INCHI_BOND_STEREO;
import net.sf.jniinchi.INCHI_BOND_TYPE;
import net.sf.jniinchi.INCHI_PARITY;
import net.sf.jniinchi.INCHI_STEREOTYPE;
import net.sf.jniinchi.JniInchiAtom;
import net.sf.jniinchi.JniInchiBond;
import net.sf.jniinchi.JniInchiInput;
import net.sf.jniinchi.JniInchiInputInchi;
import net.sf.jniinchi.JniInchiOutput;
import net.sf.jniinchi.JniInchiOutputStructure;
import net.sf.jniinchi.JniInchiStereo0D;
import net.sf.jniinchi.JniInchiStructure;
import net.sf.jniinchi.JniInchiWrapper;

public class InChIJNI implements JmolInChI {

  public InChIJNI() {
    // for dynamic loading
  }

  @Override
  public String getInchi(Viewer vwr, BS atoms, Object molData, String options) {
    try {
      if (atoms == null ? molData == null : atoms.isEmpty())
        return "";
      boolean getKey = false;
      if (options == null)
        options = "";
      String inchi = null;
      String lc = options.toLowerCase();
      boolean getSmiles = (lc.indexOf("smiles") == 0);
      boolean getStructure = (lc.indexOf("structure") >= 0);
      String smilesOptions = (getSmiles ? options : null);
      if (lc.startsWith("structure/")) {
        inchi = options.substring(10);
        options = lc = "";
      }
      if (molData instanceof String && ((String) molData).startsWith("InChI=")) {
        inchi = (String) molData;
        if (!getSmiles) {
          options = lc.replace("structure", "");
          getKey = true;
        }
      } else {
        options = lc;
        getKey = (options.indexOf("key") >= 0);
        if (getKey) {
          options = options.replace("inchikey", "");
          options = options.replace("key", "");
        }

        if (options.indexOf("fixedh?") >= 0) {
          String fxd = getInchi(vwr, atoms, molData, options.replace('?', ' '));
          options = PT.rep(options, "fixedh?", "");
          String std = getInchi(vwr, atoms, molData, options);
          inchi = (fxd != null && fxd.length() <= std.length() ? std : fxd);
        } else {
          JniInchiInput in = new JniInchiInput(getSmiles ? "fixedh" : options);
          JniInchiStructure struc;
          if (atoms == null) {
            in.setStructure(struc = newJniInchiStructure(vwr, molData));
          } else {
            in.setStructure(struc = newJniInchiStructureBS(vwr, atoms));
          }
          if (getStructure) {
            return toString(struc);
          }
          JniInchiOutput out = JniInchiWrapper.getInchi(in);
          String msg = out.getMessage();
          if (msg != null)
            System.err.println(msg);
          inchi = out.getInchi();
        }
      }
      if (getSmiles || getStructure) {
        JniInchiInputInchi in = new JniInchiInputInchi(inchi);
        JniInchiOutputStructure struc = JniInchiWrapper
            .getStructureFromInchi(in);
        return (getSmiles ? getSmiles(vwr, struc, smilesOptions)
            : getStructure(struc));
      }
      return (getKey ? JniInchiWrapper.getInchiKey(inchi).getKey() : inchi);
    } catch (Throwable e) {

      System.out.println(e);

      if (e.getMessage().indexOf("ption") >= 0)
        System.out.println(e.getMessage() + ": " + options.toLowerCase()
            + "\n See https://www.inchi-trust.org/download/104/inchi-faq.pdf for valid options");
      else
        e.printStackTrace();
      return "";
    }
  }

  private String getStructure(JniInchiStructure mol) {
    return toString(mol);
  }

  /**
   * Jmol addition to create a JniInchiStructure from Jmol atoms. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param bsAtoms
   * @return a structure for JniInput
   */
  private static JniInchiStructure newJniInchiStructureBS(Viewer vwr,
                                                        BS bsAtoms) {
    JniInchiStructure mol = new JniInchiStructure();
    JniInchiAtom[] atoms = new JniInchiAtom[bsAtoms.cardinality()];
    int[] map = new int[bsAtoms.length()];
    BS bsBonds = vwr.ms.getBondsForSelectedAtoms(bsAtoms, false);
    for (int pt = 0, i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms
        .nextSetBit(i + 1)) {
      Atom a = vwr.ms.at[i];
      String sym = a.getElementSymbol();
      int iso = a.getIsotopeNumber();
      if (a.getElementNumber() == 1) {
        sym = "H"; // in case this is D
      }
      mol.addAtom(atoms[pt] = new JniInchiAtom(a.x, a.y, a.z, sym));
      atoms[pt].setCharge(a.getFormalCharge());
      if (iso > 0)
        atoms[pt].setIsotopicMass(iso);
      map[i] = pt++;
    }
    Bond[] bonds = vwr.ms.bo;
    for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
      Bond bond = bonds[i];
      // partial bonds (as in ferrocene) become at least 1
      // but oddly still do not get generated, probably because the carbons are then too strange?
      INCHI_BOND_TYPE order = getOrder(Math.max(bond.isPartial() ? 1 : 0, bond.getCovalentOrder()));
      if (order != null) {
        mol.addBond(new JniInchiBond(atoms[map[bond.getAtomIndex1()]],
            atoms[map[bond.getAtomIndex2()]], order, getStereo(bond.getBondType())));
      }
    }
    return mol;
  }

  /**
   * Jmol addition to create a JniInchiStructure from MOL data. Currently only
   * supports single, double, aromatic_single and aromatic_double.
   * 
   * @param vwr
   * @param molData
   *        String or InputStream
   * @return a structure for JniInput
   */
  private static JniInchiStructure newJniInchiStructure(Viewer vwr,
                                                        Object molData) {
    JniInchiStructure mol = new JniInchiStructure();
    Object r = null;
    try {
      if (molData instanceof String) {
        r = new BufferedReader(new StringReader((String) molData));
      } else if (molData instanceof InputStream) {
        r = molData;
      }
      Map<String, Object> htParams = new Hashtable<String, Object>();
      JmolAdapter adapter = vwr.getModelAdapter();
      Object atomSetReader = adapter.getAtomSetCollectionReader("String", null,
          r, htParams);
      if (atomSetReader instanceof String) {
        System.err.println("InChIJNI could not read molData");
        return null;
      }
      AtomSetCollection asc = (AtomSetCollection) adapter
          .getAtomSetCollection(atomSetReader);
      JmolAdapterAtomIterator ai = adapter.getAtomIterator(asc);
      JmolAdapterBondIterator bi = adapter.getBondIterator(asc);
      JniInchiAtom[] atoms = new JniInchiAtom[asc.getAtomSetAtomCount(0)];
      int n = 0;
      Map<Object, Integer> atomMap = new Hashtable<Object, Integer>();
      while (ai.hasNext() && n < atoms.length) {
        P3d p = ai.getXYZ();
        int atno = ai.getElementNumber();
        if (atno <= 0) {
          System.err.println("InChIJNI atom " + p + " index " + ai.getUniqueID()
              + " is not a valid element");
          return null;
        }
        String sym = Elements.elementSymbolFromNumber(atno);
        JniInchiAtom a = new JniInchiAtom(p.x, p.y, p.z, sym);
        a.setCharge(ai.getFormalCharge());
        mol.addAtom(a);
        atomMap.put(ai.getUniqueID(), Integer.valueOf(n));
        atoms[n++] = a;
      }
      int nb = 0;
      while (bi.hasNext()) {
        int jmolOrder = bi.getEncodedOrder();
        INCHI_BOND_TYPE order = getOrder(jmolOrder);
        if (order != null) {
          Integer id1 = atomMap.get(bi.getAtomUniqueID1());
          Integer id2 = atomMap.get(bi.getAtomUniqueID2());
          if (id1 == null || id2 == null) {
            System.err.println("InChIJNI bond " + nb + "has null atom "
                + (id1 == null ? bi.getAtomUniqueID1() : "") + " "
                + (id2 == null ? bi.getAtomUniqueID2() : ""));
            return null;
          }
          JniInchiAtom a = atoms[id1.intValue()];
          JniInchiAtom b = atoms[id2.intValue()];
          if (a == null || b == null) {
            System.err
                .println("InChIJNI bond " + nb + "has null atom: " + a + "/" + b
                    + " for ids " + id1 + " " + id2 + " and " + n + " atoms");
            return null;
          }
          mol.addBond(new JniInchiBond(a, b, order, getStereo(jmolOrder)));
          nb++;
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
      System.err.println(t.toString());
    } finally {
      try {
        if (r instanceof BufferedReader) {
          ((BufferedReader) r).close();
        } else {
          ((InputStream) r).close();
        }
      } catch (IOException e) {
      }
    }
    return mol;
  }

  private static INCHI_BOND_STEREO getStereo(int jmolOrder) {
    switch (jmolOrder) {
    case Edge.BOND_STEREO_FAR:
      return INCHI_BOND_STEREO.SINGLE_1DOWN;
    case Edge.BOND_STEREO_NEAR:
      return INCHI_BOND_STEREO.SINGLE_1UP;
    case Edge.BOND_STEREO_EITHER:
      // this will generate a stereo ?
      return INCHI_BOND_STEREO.SINGLE_1EITHER;
    default:
      return INCHI_BOND_STEREO.NONE;
    }
  }

  private static INCHI_BOND_TYPE getOrder(int jmolOrder) {
    switch (jmolOrder) {
    case Edge.BOND_STEREO_EITHER:
    case Edge.BOND_STEREO_FAR:
    case Edge.BOND_STEREO_NEAR:
    case Edge.BOND_COVALENT_SINGLE:
      return INCHI_BOND_TYPE.SINGLE;
    case Edge.BOND_COVALENT_DOUBLE:
      return INCHI_BOND_TYPE.DOUBLE;
    case Edge.BOND_COVALENT_TRIPLE:
      return INCHI_BOND_TYPE.TRIPLE;
    default:
      return null;
    }
  }

  private static String toString(JniInchiStructure mol) {
    int na = mol.getNumAtoms();
    int nb = mol.getNumBonds();
    String s = "";
    for (int i = 0; i < na; i++) {
      s += mol.getAtom(i).getDebugString() + "\n";
    }
    for (int i = 0; i < nb; i++) {
      s += mol.getBond(i).getDebugString() + "\n";
    }
    return s;
  }

  private Map<BS, int[]> mapTet;
  private Map<Integer, Boolean> mapPlanar;

  private String getSmiles(Viewer vwr, JniInchiOutputStructure struc,
                           String smilesOptions) {
    boolean hackImine = (smilesOptions.indexOf("imine") >= 0);
    int nAtoms = struc.getNumAtoms();
    int nBonds = struc.getNumBonds();
    int nh = 0;
    for (int i = 0; i < nAtoms; i++) {
      JniInchiAtom a = struc.getAtom(i);
      nh += a.getImplicitH();
    }
    Lst<SmilesAtom> atoms = new Lst<SmilesAtom>();
    Map<JniInchiAtom, SmilesAtom> map = new Hashtable<JniInchiAtom, SmilesAtom>();
    mapTet = new Hashtable<BS, int[]>();
    mapPlanar = new Hashtable<Integer, Boolean>();
    int nb = 0;
    int na = 0;
    for (int i = 0; i < nAtoms; i++) {
      JniInchiAtom a = struc.getAtom(i);
      SmilesAtom n = new SmilesAtom() {
        @Override
        public boolean definesStereo() {
          return true;
        }

        @Override
        public String getStereoAtAt(SimpleNode[] nodes) {
          return decodeInchiStereo(nodes);
        }

        @Override
        public Boolean isStereoOpposite(int i2, int iA, int iB) {
          return isInchiOpposite(getIndex(), i2, iA, iB);
        }

      };
      atoms.addLast(n);
      n.set(a.getX(), a.getY(), a.getZ());
      n.setIndex(na++);
      n.setCharge(a.getCharge());
      n.setSymbol(a.getElementType());
      nh = a.getImplicitH();
      for (int j = 0; j < nh; j++) {
        addH(atoms, n, nb++);
        na++;
      }
      map.put(a, n);
    }
    for (int i = 0; i < nBonds; i++) {
      JniInchiBond b = struc.getBond(i);
      JniInchiAtom a1 = b.getOriginAtom();
      JniInchiAtom a2 = b.getTargetAtom();
      int bt = getJmolBondType(b);
      SmilesAtom sa1 = map.get(a1);
      SmilesAtom sa2 = map.get(a2);
      SmilesBond sb = new SmilesBond(sa1, sa2, bt, false);
      sb.index = nb++;
    }
    nb = checkFormalCharges(atoms, nb, hackImine);
    na = atoms.size();
    SmilesAtom[] aatoms = new SmilesAtom[na];
    atoms.toArray(aatoms);
    for (int i = 0; i < na; i++) {
      aatoms[i].setBondArray();
    }

    int iA = -1, iB = -1;
    for (int i = struc.getNumStereo0D(); --i >= 0;) {
      JniInchiStereo0D sd = struc.getStereo0D(i);
      JniInchiAtom[] an = sd.getNeighbors();
      if (an.length != 4)
        continue;
      JniInchiAtom ca = sd.getCentralAtom();
      int i0 = map.get(an[0]).getIndex();
      int i1 = map.get(an[1]).getIndex();
      int i2 = map.get(an[2]).getIndex();
      int i3 = map.get(an[3]).getIndex();
//System.out.println(aatoms[i0] + "\n" +  aatoms[i1] + "\n" +  aatoms[i2] + "\n" +  aatoms[i3]);
      boolean isEven = (sd.getParity() == INCHI_PARITY.EVEN);
      INCHI_STEREOTYPE type = sd.getStereoType();
      switch (type) {
      case ALLENE:
      case DOUBLEBOND:
        // alkene or 2N-cummulene
        iA = i1;
        iB = i2;
        i1 = getOtherEneAtom(aatoms, i1, i0);
        i2 = getOtherEneAtom(aatoms, i2, i3);
        break;
      case NONE:
        continue;
      case TETRAHEDRAL:
        break;
      }
      if (ca == null) {
//        addStereoMap(aatoms, i0, i1, i2, i3, isEven);
        // i1 and i2 are now substituents on the original A=B, possibly -1, but we don't care
        setPlanarKey(i0, i3, iA, iB, Boolean.valueOf(isEven));
        setPlanarKey(i0, i2, iA, iB, Boolean.valueOf(!isEven));
        setPlanarKey(i1, i2, iA, iB, Boolean.valueOf(isEven));
        setPlanarKey(i1, i3, iA, iB, Boolean.valueOf(!isEven));
        setPlanarKey(i0, i1, iA, iB, Boolean.TRUE);
        setPlanarKey(i2, i3, iA, iB, Boolean.TRUE);
     } else {
        int[] list = new int[] { isEven ? i0 : i1, isEven ? i1 : i0, i2, i3 };
        mapTet.put(orderList(list), list);
      }
    }
    try {
      SmilesMatcherInterface m = vwr.getSmilesMatcher();
      return m.getSmiles(aatoms, na, BSUtil.newBitSet2(0, na), smilesOptions,
          JC.SMILES_TYPE_SMILES);
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  private void setPlanarKey(int i0, int i3, int iA, int iB, Boolean v) {
    mapPlanar.put(getIntKey(i0, iA, i3), v);    
    mapPlanar.put(getIntKey(i0, iB, i3), v);    
  }

  private SmilesAtom addH(Lst<SmilesAtom> atoms, SmilesAtom n, int nb) {
    SmilesAtom h = new SmilesAtom();
    h.setIndex(atoms.size());
    h.setSymbol("H");
    atoms.addLast(h);
    SmilesBond sb = new SmilesBond(n, h, Edge.BOND_COVALENT_SINGLE, false);
    sb.index = nb;
    return h;
  }

  private int checkFormalCharges(Lst<SmilesAtom> atoms, int nb, boolean hackImine) {
    for (int i = atoms.size(); --i >= 0;) {
      SmilesAtom a = atoms.get(i);
      int val = a.getValence();
      int nbonds = a.getCovalentBondCount();
      int nbtot = a.getBondCount();
      int ano = a.getElementNumber();
      int formalCharge = a.getCharge();
      //System.out.println("InChIJNI " + ano + " " + val + " " + nbonds);
      SmilesBond b1 = null, b2 = null;
      switch (val * 10 + nbonds) {
      case 32:
        // X-N=Y
        if (ano == 7 && hackImine) {
          // change N to C17 and add H5
          // the MOL reader will fix these
          a.setSymbol("C");
          a.setAtomicMass(17);
          SmilesAtom h = addH(atoms, a, nb++);
          h.setAtomicMass(5);
        }
        break;
      case 53:
        if (ano == 7 && formalCharge == 0) {
          // X=N(R)=X -->  (-)X-N(+)(R)=X
          for (int j = 0; j < nbtot; j++) {
            SmilesBond b = a.getBond(j);
            if (b.getCovalentOrder() == 2) {
              if (b1 == null) {
                b1 = b;
              } else {
                b2 = b;
                break;
              }
            }
          }
        }
        break;
      case 54:
//        if (ano == 15) {
//          // X=P(R)=X -->  (-)X-N(+)(R)=X
//          for (int j = 0; j < nbtot; j++) {
//            SmilesBond b = a.getBond(j);
//            if (b.getCovalentOrder() == 2) {
//              if (b1 == null) {
//                b1 = b;
//              } else {
//                b2 = b;
//                break;
//              }
//            }
//          }
//        }
        break;

      }
      if (b2 != null) {
        SmilesAtom a2 = b2.getOtherAtom(a);
        a2.setCharge(-1);
        a.setCharge(1);
        b2.set2(Edge.BOND_COVALENT_SINGLE, false);
      }
    }
    return nb;
  }

  protected Boolean isInchiOpposite(int i1, int i2, int iA, int iB) {
    return mapPlanar.get(getIntKey(i1, Math.max(iA, iB), i2));
  }

  protected String decodeInchiStereo(SimpleNode[] nodes) {
    int[] list = new int[] { getNodeIndex(nodes[0]),
        getNodeIndex(nodes[1]), getNodeIndex(nodes[2]), getNodeIndex(nodes[3]) };
    int[] list2 = mapTet.get(orderList(list));
    return (list2 == null ? null : isPermutation(list, list2) ? "@@" : "@");
  }

  private static int getNodeIndex(SimpleNode node) {
    return (node == null ? -1 : node.getIndex());
  }
  private static Integer getIntKey(int i, int iA, int j) {
    Integer v =  Integer.valueOf((Math.min(i, j) << 24) +
        (iA << 12) +  Math.max(i, j));
//System.out.println("getIntKey " + i + " " + iA + " "+ j + " " + 
//        (v == null ? null : Integer.toHexString(v.intValue())));
    return v;
  }

  private static BS orderList(int[] list) {
    BS bs = new BS();
    for (int i = 0; i < list.length; i++)
      bs.set(list[i]);
    return bs;
  }

  private static boolean isPermutation(int[] list, int[] list2) {
    boolean ok = true;
    for (int i = 0; i < 3; i++) {
      int l1 = list[i];
      for (int j = i + 1; j < 4; j++) {
        int l2 = list2[j];
        if (l2 == l1) {
          if (j != i) {
            list2[j] = list2[i];
            list2[i] = l2;
            ok = !ok;
          }
        }
      }
    }
    return ok;
  }

  private static int getOtherEneAtom(SmilesAtom[] atoms, int i0, int i1) {
    SmilesAtom a = atoms[i0];
    for (int i = a.getBondCount(); --i >= 0;) {
      if (a.getBond(i).getBondType() == Edge.BOND_COVALENT_SINGLE) {
        int i2 = a.getBondedAtomIndex(i);
        if (i2 != i1) {
          return i2;
        }
      }
    }
    // could be imine
    return -1;
  }

  private static int getJmolBondType(JniInchiBond b) {
    INCHI_BOND_TYPE type = b.getBondType();
    switch (type) {
    case NONE:
      return 0;
    case ALTERN:
      return Edge.BOND_AROMATIC;
    case DOUBLE:
      return Edge.BOND_COVALENT_DOUBLE;
    case TRIPLE:
      return Edge.BOND_COVALENT_TRIPLE;
    case SINGLE:
    default:
      return Edge.BOND_COVALENT_SINGLE;
    }
  }

}
