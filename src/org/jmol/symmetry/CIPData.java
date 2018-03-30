package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;

import org.jmol.script.T;
import org.jmol.util.SimpleEdge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class CIPData {

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

  /**
   * [c-]
   */
  BS bsCMinus = new BS();

  BS bsMolecule;

  BS[] lstSmallRings;

  BS bsKekuleAmbiguous = new BS();

  BS bsEnes = new BS();

  Map<Integer, Object> htKekuleBond = new Hashtable<Integer, Object>();
  
  public CIPData() {
  }

  public CIPData set(Viewer vwr, BS bsAtoms) {
    this.vwr = vwr;
    this.atoms = vwr.ms.at;
    this.bsAtoms = bsAtoms; 
    this.bsMolecule = vwr.ms.getMoleculeBitSet(bsAtoms);
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
    } catch (Exception e) {
      // ignore
    }
    return this;
  }
  
  private BS[] getList(String smarts) throws Exception {
    return vwr.getSubstructureSetArray(smarts, bsMolecule, JC.SMILES_TYPE_SMARTS);
  }

  private BS match(String smarts) throws Exception {
    return vwr.getSmartsMatch(smarts, bsMolecule);
  }

//  private int[][] map(String smarts) throws Exception {
//    return vwr.getSmartsMap(smarts, bsMolecule);
//  }

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
      if (edges[ib].getCovalentOrder() != order)
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

//  public boolean canBeChiralBond(SimpleEdge bond) {
//    return !htKekuleBonds.containsKey(Integer.valueOf(bond.hashCode()));
//  }
}
