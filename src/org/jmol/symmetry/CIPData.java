package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.V3;

import org.jmol.script.T;
import org.jmol.symmetry.CIPChirality.CIPAtom;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.SimpleEdge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class CIPData {


  
  /**
   * measure of planarity in a trigonal system, in Angstroms
   * 
   */
  static final float TRIGONALITY_MIN = 0.2f;


  
  Viewer vwr;
  
  /**
   * also set auxiliary (single-atom only)
   */
  boolean setAuxiliary;

  /**
   * bit set of all biphenyl-like connections
   */
  BS bsAtropisomeric = new BS();

  /**
   * bit set of all atoms to process
   */
  BS bsAtoms;
  /**
   * aromatic atoms at the end of a negative helical turn;
   */
  BS bsHelixM;
  /**
   * aromatic atoms at the end of a positive helical turn;
   */
  BS bsHelixP;
  
  BS bsAromatic;
  
//  /**
//   * 5-membered rings
//   */
//  BS[] lstR5;
//  
//  /**
//   * 5-membered aromatic rings
//   */
//  BS[] lstR5a = new BS[0];
//  
//  /**
//   * 6-membered rings
//   */
//  BS[] lstR6;
//  
//  /**
//   * 6-membered aromatic rings
//   */
//  BS[] lstR6a = new BS[0];
  
  SimpleNode[] atoms;

  /**
   * [r5d3n+0,r5d2o+0]
   */
  BS bsXAromatic = new BS();

  BS bsMolecule;

  /**
   * [c-]
   */
  BS bsCMinus = new BS();

  BS[] lstSmallRings;

  BS bsKekuleAmbiguous = new BS();

  BS bsEnes = new BS();

  Map<Integer, Object> htKekuleBond = new Hashtable<Integer, Object>();

  BS bsAzacyclic;
  
  public CIPData() {
  }

  public CIPData set(Viewer vwr, BS bsAtoms) {
    this.vwr = vwr;
    this.atoms = vwr.ms.at;
    this.bsAtoms = bsAtoms; 
    bsMolecule = vwr.ms.getMoleculeBitSet(bsAtoms);
    init();
    return this;
  }
  
  protected void init() {
    setAuxiliary = vwr.getBoolean(T.testflag1);
    try {
      // four ortho groups required:      bsAtropisomer = match("[!H](.t3:-20,20)a1(.t3).[!H](.t1:-20,20)a(.t1)a1(.t1)(.t2:-20,20)(.t3)(.t4:-20,20)-{a}2(.t1)(.t2)(.t3)(.t4)a(.t2)[!H](.t2).a2(.t4)[!H](.t4)", bsAtoms);
      // three ortho groups required:     bsAtropisomer = match("[!H](.t3:-20,20)a1(.t3).[!H](.t1:-20,20)a(.t1){a}1(.t1)(.t2:-20,20)(.t3)-{a}(.t1)(.t2)(.t3)a(.t2)[!H](.t2)", bsAtoms);
      // one ortho group on each ring required:
      BS lstRing = match("[r]");
      if (lstRing.isEmpty()) {
        lstSmallRings = new BS[0];
      } else {
        lstSmallRings = getList("*1**1||*1***1||*1****1||*1*****1||*1******1");
      }
      bsAromatic = match("a");
      if (!bsAromatic.isEmpty()) {
        bsAtropisomeric = match("[!H](.t1:-20,20)a{a(.t2:-20,20)-a}a[!H]");
        bsHelixM = match("A{a}(.t:-10,-40)a(.t:-10,-40)aaa");
        bsHelixP = match("A{a}(.t:10,40)a(.t:10,40)aaa");
        bsXAromatic = match("[r5v3n+0,r5v2o+0]");
        bsCMinus = match("[a-]");

        
        if (!match("[n+1,o+1]").isEmpty() && !bsXAromatic.isEmpty()) {
          // look for key 5-member ring aromatics.
          bsKekuleAmbiguous.or(match("a1[n+,o+]a[n,o]a1"));
          bsKekuleAmbiguous.or(match("a1[n+,o+][n,o]aa1"));
        }
        if (!bsCMinus.isEmpty())
          bsKekuleAmbiguous.or(match("a1=a[a-]a=a1"));
        
        // pick up five-membered rings with one hetero?
        //bsKekuleAmbiguous.or(match("a1=a[av3,av2]a=a1"));

        // note that Jmol SMILES does the desired check here -- not including caffeine-like OPEN SMILES rings 
        BS[] lstR6a = getList("a1aaaaa1");
        for (int i = lstR6a.length; --i >= 0;) {
          bsKekuleAmbiguous.or(lstR6a[i]);        
        }
      }
      getAzacyclic();
    } catch (Exception e) {
      // ignore
    }
  }
  
  protected BS[] getList(String smarts) throws Exception {
    int level = Logger.getLogLevel();
    Logger.setLogLevel(Math.min(level,  Logger.LEVEL_INFO));
    BS[] list = vwr.getSubstructureSetArray(smarts, bsMolecule, JC.SMILES_TYPE_SMARTS);
    Logger.setLogLevel(level);
    return list;
  }

  protected BS match(String smarts) throws Exception {
    int level = Logger.getLogLevel();
    Logger.setLogLevel(Math.min(level,  Logger.LEVEL_INFO));
    BS bs = vwr.getSmartsMatch(smarts, bsMolecule);
    Logger.setLogLevel(level);
    return bs;
  }

  /**
   * Look for conjugated loops of any size that have atoms not already in aromatic rings
   * 
   */
  public void getEneKekule() {
    if (bsEnes.cardinality() < 8) 
      return;
        
    // check for large ENE loops
    // need at least five alkenes to trigger this.
    
    BS bsAllEnes = (BS) bsEnes.clone();
    BS bsPath = new BS();
    bsEnes.andNot(bsKekuleAmbiguous);
    BS bsEneAtom1 = new BS();
    for (int i = bsEnes.nextSetBit(0); i >= 0; i = bsEnes.nextSetBit(i + 1)) {
      bsPath.clearAll();   
      bsEneAtom1.clearAll();
      checkEne(bsAllEnes, bsPath, -1, i, 2, bsEneAtom1); 
    }
  }

  /**
   * Look for a path that contains this ene in a fully conjugated pattern
   * 
   * @param bsAllEnes all ene carbons
   * @param bsPath current path to loop into
   * @param iLast the last atom
   * @param iAtom this atom
   * @param order expected next order, alternating 2,1,2,1,...
   * @param bsEneAtom1 alternating atoms; first of double bond
   * @return the atom number of the loop or -1 if failed
   */
  private int checkEne(BS bsAllEnes, BS bsPath, int iLast, int iAtom, int order, BS bsEneAtom1) {
    if (bsPath.get(iAtom))
      return (bsEneAtom1.get(iAtom) == (order == 2) ? iAtom : -1);
    bsPath.set(iAtom);
    SimpleNode a = atoms[iAtom];
    int isLoop = -1;
    SimpleEdge[] edges = a.getEdges();
    if (order == 2)
      bsEneAtom1.set(iAtom);
    for (int ib = a.getBondCount(); --ib >= 0;) {
      if (getBondOrder(edges[ib]) != order)
        continue;
      SimpleNode b = edges[ib].getOtherNode(a);
      int iNext = b.getIndex();
      if (iNext != iLast && bsAllEnes.get(iNext)
          && (isLoop = checkEne(bsAllEnes, bsPath, iAtom, iNext, 3 - order, bsEneAtom1)) >= 0) {
      }
    }
    if (isLoop >= 0) {
      bsKekuleAmbiguous.set(iAtom);
      bsEnes.clear(iAtom);
    }
    return isLoop == iAtom ? -1 : isLoop;
  }

  /**
   * Identify bridgehead nitrogens, as these may need to be given chirality
   * designations. See AY-236.203 P-93.5.4.1
   * 
   * Sets a bit set of bridgehead nitrogens
   */
  private void getAzacyclic() {
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      SimpleNode atom = atoms[i];
      if (atom.getElementNumber() != 7 || atom.getCovalentBondCount() != 3
          || bsKekuleAmbiguous.get(i))
        continue;
      // bridgehead N must be in two rings that have at least three atoms in common.
      Lst<BS> nRings = new Lst<BS>();
      for (int j = lstSmallRings.length; --j >= 0;) {
        BS bsRing = lstSmallRings[j];
        if (bsRing.get(i))
          nRings.addLast(bsRing);
      }
      int nr = nRings.size();
      if (nr < 2)
        continue;
      BS bsSubs = new BS();
      SimpleEdge[] bonds = atom.getEdges();
      for (int b = bonds.length; --b >= 0;)
        if (bonds[b].isCovalent())
          bsSubs.set(bonds[b].getOtherNode(atom).getIndex());
      BS bsBoth = new BS();
      BS bsAll = new BS();
      for (int j = 0; j < nr - 1 && bsAll != null; j++) {
        BS bs1 = nRings.get(j);
        for (int k = j + 1; k < nr && bsAll != null; k++) {
          BS bs2 = nRings.get(k);
          BSUtil.copy2(bs1, bsBoth);
          bsBoth.and(bs2);
          if (bsBoth.cardinality() > 2) {
            BSUtil.copy2(bs1, bsAll);
            bsAll.or(bs2);
            bsAll.and(bsSubs);
            if (bsAll.cardinality() == 3) {
              if (bsAzacyclic == null)
                bsAzacyclic = new BS();
              bsAzacyclic.set(i);
              bsAll = null;
            }
          }
        }
      }
    }
  }

  boolean isTracker() {
    return false;
  }
  
  boolean isSmiles() {
    return false;
  }

  /**
   * Determine whether an atom is one we need to consider.
   * 
   * @param a
   * @return true for selected atoms and hybridizations
   * 
   */
  boolean couldBeChiralAtom(SimpleNode a) {
    boolean mustBePlanar = false;
    switch (a.getCovalentBondCount()) {
    default:
      System.out.println("?? too many bonds! " + a);
      return false;
    case 0:
      return false;
    case 1:
      return false;
    case 2:
      return a.getElementNumber() == 7; // could be diazine or imine
    case 3:
      switch (a.getElementNumber()) {
      case 7: // N
        if (bsAzacyclic != null && bsAzacyclic.get(a.getIndex()))
          break;
        return false;
      case 6: // C
        mustBePlanar = true;
        break;
      case 15: // P
      case 16: // S
      case 33: // As
      case 34: // Se
      case 51: // Sb
      case 52: // Te
      case 83: // Bi
      case 84: // Po
        break;
      case 4:
        break;
      default:
        return false;
      }
      break;
    case 4:
      break;
    }
    // check that the atom has at most one 1H atom and whether it must be planar and has a double bond
    SimpleEdge[] edges = a.getEdges();
    int nH = 0;
    boolean haveDouble = false;
    for (int j = edges.length; --j >= 0;) {
      if (mustBePlanar && edges[j].getCovalentOrder() == 2)
        haveDouble = true;
      if (edges[j].getOtherNode(a).getIsotopeNumber() == 1)
        nH++;
    }
    return (nH < 2 && (haveDouble 
        || isSmiles() || mustBePlanar == Math.abs(getTrigonality(a,
        vNorm)) < TRIGONALITY_MIN));
  }

  /**
   * Allow double bonds only if trivalent and first-row atom. (IUPAC
   * 2013.P-93.2.4) Currently: a) first row b) doubly bonded c) doubly bonded
   * atom is also first row
   * 
   * @param a
   * @param edge optional bond
   * @return STEREO_M, STEREO_Z, or UNDETERMINED
   */
  int couldBeChiralAlkene(SimpleNode a, SimpleEdge edge) {
    SimpleNode b = (edge == null ? null : edge.getOtherNode(a));
    switch (a.getCovalentBondCount()) {
    default:
      return CIPChirality.UNDETERMINED;
    case 2:
      // imines and diazines
      if (a.getElementNumber() != 7) // nitrogen
        return CIPChirality.UNDETERMINED;
      break;
    case 3:
      // first-row only (IUPAC 2013.P-93.2.4)
      if (!CIPChirality.isFirstRow(a))
        return CIPChirality.UNDETERMINED;
      break;
    }
    SimpleEdge[] bonds = a.getEdges();
    int n = 0;
    for (int i = bonds.length; --i >= 0;)
      if (getBondOrder(bonds[i]) == 2) {
        if (++n > 1)
          return CIPChirality.STEREO_M; //central allenes
        SimpleNode other = bonds[i].getOtherNode(a);
        if (!CIPChirality.isFirstRow(other))
          return CIPChirality.UNDETERMINED;
        if (b != null && (other != b || b.getCovalentBondCount() == 1)) {
          // could be allene central, but I think this is not necessary
          return CIPChirality.UNDETERMINED;
        }
      }
    return CIPChirality.STEREO_Z;
  }

  /**
   * Determine the trigonality of an atom in order to determine whether it might
   * have a lone pair. The global vector vNorm is returned as well, pointing
   * from the atom to the base plane of its first three substituents.
   * 
   * @param a
   * @param vNorm
   *        a vector returned with the normal from the atom to the base plane
   * @return distance from plane of first three covalently bonded nodes to this
   *         node
   */
  float getTrigonality(SimpleNode a, V3 vNorm) {
    P3[] pts = new P3[4];
    SimpleEdge[] bonds = a.getEdges();
    for (int n = bonds.length, i = n, pt = 0; --i >= 0 && pt < 4;)
      if (bonds[i].isCovalent())
        pts[pt++] = bonds[i].getOtherNode(a).getXYZ();
    P4 plane = Measure.getPlaneThroughPoints(pts[0], pts[1], pts[2], vNorm,
        vTemp, new P4());
    return Measure.distanceToPlane(plane,
        (pts[3] == null ? a.getXYZ() : pts[3]));
  }

  // temporary fields

  protected V3 vNorm = new V3(), vTemp = new V3();

//  public boolean canBeChiralBond(SimpleEdge bond) {
//    return !htKekuleBonds.containsKey(Integer.valueOf(bond.hashCode()));
//  }
  
  /**
   * Check cis vs. trans nature of a--b==c--d.
   * 
   * @param a
   * @param b
   * @param c
   * @param d
   * @return true if this is a cis relationship
   */
  int isCis(CIPAtom a, CIPAtom b, CIPAtom c, CIPAtom d) {
    Measure.getNormalThroughPoints(a.atom.getXYZ(), b.atom.getXYZ(),
        c.atom.getXYZ(), vNorm, vTemp);
    V3 vNorm2 = new V3();
    Measure.getNormalThroughPoints(b.atom.getXYZ(), c.atom.getXYZ(),
        d.atom.getXYZ(), vNorm2, vTemp);
    return (vNorm.dot(vNorm2) > 0 ? CIPChirality.STEREO_Z : CIPChirality.STEREO_E);
  }

  /**
   * Checks the torsion angle and returns true if it is positive
   * 
   * @param a
   * @param b
   * @param c
   * @param d
   * @return true if torsion angle is
   */
  int isPositiveTorsion(CIPAtom a, CIPAtom b, CIPAtom c, CIPAtom d) {
    float angle = Measure.computeTorsion(a.atom.getXYZ(), b.atom.getXYZ(),
        c.atom.getXYZ(), d.atom.getXYZ(), true);
    return (angle > 0 ? CIPChirality.STEREO_P : CIPChirality.STEREO_M);
  }

  int getBondOrder(SimpleEdge bond) {
    return bond.getCovalentOrder();
  }

  /**
   * set the coordinate -- SMILES only
   * 
   * @param atom1
   * @param atoms
   * @return true if coordinate is able to be set
   */
  boolean setCoord(SimpleNode atom1, CIPAtom[] atoms) {
    // 
    return true;    
  }

  /**
   * Determine the ordered CIP winding of this atom. For this, we just take
   * the directed normal through the plane containing the top three
   * substituent atoms and dot that with the vector from any one of them to
   * the fourth ligand (or the root atom if trigonal pyramidal). If this is
   * positive, we have R.
   * 
   * @param a 
   * 
   * @return 1 for "R", 2 for "S"
   */
  int checkHandedness(CIPAtom a) {
    CIPAtom[] atoms = a.atoms; 
    if (!setCoord(a.atom, atoms))
      return CIPChirality.NO_CHIRALITY;
    P3 p0 = (atoms[3].atom == null ? a.atom : atoms[3].atom).getXYZ();
    P3 p1 = atoms[0].atom.getXYZ(), p2 = atoms[1].atom.getXYZ(), p3 = atoms[2].atom
        .getXYZ();
    Measure.getNormalThroughPoints(p1, p2, p3, vNorm, vTemp);
    vTemp.setT(p0);
    vTemp.sub(p1);
    return (vTemp.dot(vNorm) > 0 ? CIPChirality.STEREO_R : CIPChirality.STEREO_S);
  }

  /**
   * track this decision - CIPDataTracker only
   * 
   * @param cip
   * @param a
   * @param b
   * @param sphere
   * @param finalScore
   * @param mode
   */
  void track(CIPChirality cip, CIPAtom a, CIPAtom b, int sphere,
             int finalScore, int mode) {
    // CIPDataTracker only
  }

  /**
   * CIPDataTracker only
   * 
   * @param root
   * @return string expression of decision path
   */
  String getRootTrackerResult(CIPAtom root) {    
    // CIPDataTracker only
    return null;
  }
}
