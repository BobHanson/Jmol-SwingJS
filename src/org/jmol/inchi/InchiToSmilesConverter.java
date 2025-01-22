package org.jmol.inchi;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.smiles.SmilesAtom;
import org.jmol.smiles.SmilesBond;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.Lst;
//import net.sf.jniinchi.JniInchiAtom;

class InchiToSmilesConverter {
  
  private Map<BS, int[]> mapTet;
  private Map<Integer, Boolean> mapPlanar;
  private Lst<SmilesAtom> listSmiles = new Lst<>();
  private InChIStructureProvider provider;

  public InchiToSmilesConverter(InChIStructureProvider provider) {
    this.provider = provider;
  }

  String getSmiles(Viewer vwr, String smilesOptions) {
    boolean hackImine = (smilesOptions.indexOf("imine") >= 0);
    int nAtoms = provider.getNumAtoms();
    int nBonds = provider.getNumBonds();
    int nh = 0;
    for (int i = 0; i < nAtoms; i++) {
      nh += provider.setAtom(i).getImplicitH();
    }
    Lst<SmilesAtom> atoms = new Lst<SmilesAtom>();
    mapTet = new Hashtable<BS, int[]>();
    mapPlanar = new Hashtable<Integer, Boolean>();
    int nb = 0;
    int na = 0;
    for (int i = 0; i < nAtoms; i++) {
      provider.setAtom(i);
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
      n.set(provider.getX(), provider.getY(), provider.getZ());
      n.setIndex(na++);
      n.setCharge(provider.getCharge());
      n.setSymbol(provider.getElementType());
      nh = provider.getImplicitH();
      for (int j = 0; j < nh; j++) {
        addH(atoms, n, nb++);
        na++;
      }
      listSmiles.add(n);
    }
    for (int i = 0; i < nBonds; i++) {
      provider.setBond(i);
      int bt = provider.getJmolBondType();
      SmilesAtom sa1 = listSmiles.get(provider.getOriginAtom()); //getOriginAtom produces the index of the origin atom of the bond
      SmilesAtom sa2 = listSmiles.get(provider.getTargetAtom()); //getTargetAtom produces the index of the target atom of the bond
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
    for (int i = provider.getNumStereo0D(); --i >= 0;) {
      provider.setStereo0D(i);
      provider.setAn(); //an is an array of JniInchiAtoms that gets the neighbors of the current stereo0D
      if (provider.get_an_length() != 4)
        continue;
      
      provider.setCa(); //create central atom
      int i0 = provider.get_an_map_index(0);
      int i1 = provider.get_an_map_index(1);
      int i2 = provider.get_an_map_index(2);
      int i3 = provider.get_an_map_index(3);
//System.out.println(aatoms[i0] + "\n" +  aatoms[i1] + "\n" +  aatoms[i2] + "\n" +  aatoms[i3]);
      boolean isEven = (provider.getParity() == "INCHI_PARITY.EVEN"); //converted to String from INCHI_STEREOTYPE
      String type = provider.getStereoType();
      switch (type) {
      case "ALLENE":
      case "DOUBLEBOND":
        // alkene or 2N-cummulene
        iA = i1;
        iB = i2;
        i1 = getOtherEneAtom(aatoms, i1, i0);
        i2 = getOtherEneAtom(aatoms, i2, i3);
        break;
      case "NONE":
        continue;
      case "TETRAHEDRAL":
        break;
      }
      if (provider.caIsNull()) {
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
    Boolean b = mapPlanar.get(getIntKey(i1, Math.max(iA, iB), i2));
    return b;
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


}
