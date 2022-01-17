package org.jmol.symmetry;

import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.Measure;

import org.jmol.smiles.SmilesAtom;
import org.jmol.smiles.SmilesBond;
import org.jmol.symmetry.CIPChirality.CIPAtom;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Node;
import org.jmol.util.SimpleEdge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * A subclass that allows Jmol processing of SMILES using 
 * 
 * "...smiles...".find("SMILES","chirality")
 * 
 */
public class CIPDataSmiles extends CIPData {

  String smiles = null;

  @Override
  boolean isSmiles() {
    return true;
  }

  public CIPDataSmiles() {
  }

  public CIPDataSmiles setAtomsForSmiles(Viewer vwr, String smiles)
      throws Exception {
    this.vwr = vwr;
    this.smiles = smiles;
    this.atoms = vwr.getSmilesAtoms(smiles);
    bsAtoms = BSUtil.newBitSet2(0, atoms.length);
    bsMolecule = (BS) bsAtoms.clone();
    init();
    return this;
  }

  @Override
  protected BS[] getList(String smarts) throws Exception {
    return vwr.getSubstructureSetArrayForNodes(smarts, (Node[]) atoms,
        JC.SMILES_TYPE_SMARTS);
  }

  @Override
  protected BS match(String smarts) throws Exception {
    return vwr.getSmartsMatchForNodes(smarts, (Node[]) atoms);
  }

  @Override
  int getBondOrder(SimpleEdge bond) {
    return ((SmilesBond) bond).getRealCovalentOrder();
  }

  /**
   * Check cis vs. trans nature of a--b==c--d.
   * 
   * @param a
   * @param b
   * @param c
   * @param d
   * @return true if this is a cis relationship
   */
  @Override
  int isCis(CIPAtom a, CIPAtom b, CIPAtom c, CIPAtom d) {
    int stereo1 = getStereoEdge(b.atom, a.atom);
    int stereo2 = getStereoEdge(c.atom, d.atom);
    return (stereo1 == 0 || stereo2 == 0 ? CIPChirality.NO_CHIRALITY
        : stereo1 != stereo2 ? CIPChirality.STEREO_E : CIPChirality.STEREO_Z);
  }

  private int getStereoEdge(SimpleNode atom, SimpleNode winner) {
    SimpleEdge[] edges = atom.getEdges();
    int order = 0;
    for (int i = edges.length; --i >= 0;) {
      SmilesBond edge = (SmilesBond) edges[i];
      switch (order = edge.getCovalentOrder()) {
      case Edge.BOND_STEREO_NEAR:
        return (edge.getOtherNode(atom) == winner) == (edge.getAtom1() == atom) ? Edge.BOND_STEREO_FAR
            : order;
      case Edge.BOND_STEREO_FAR:
        return (edges[i].getOtherNode(atom) == winner) == (edge.getAtom1() == atom) ? Edge.BOND_STEREO_NEAR
            : order;
      }
    }
    return 0;
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
  @Override
  int isPositiveTorsion(CIPAtom a, CIPAtom b, CIPAtom c, CIPAtom d) {
    SmilesAtom center = findCumulativeCenter(b, c);
    if (center == null)
      return CIPChirality.NO_CHIRALITY;
    // get ordered list based on the SMILES string, 
    // correctly inserting lone pairs where needed
    Node[] jn = center.stereo.getAlleneAtoms(false, null, center, (SmilesAtom) b.atom);
    if (jn == null)
      return CIPChirality.NO_CHIRALITY;
    center.stereo.setTopoCoordinates(center, null, null, jn, false);
    float angle = Measure.computeTorsion(jn[0].getXYZ(), jn[1].getXYZ(),
        jn[2].getXYZ(), jn[3].getXYZ(), true);
    //    System.out.println(a.atomIndex + " " + b.atomIndex + " " + c.atomIndex + " " + d.atomIndex);
    //    System.out.println(jn[0].getIndex() + " " + jn[1].getIndex() + " " + jn[2].getIndex() + " " + jn[3].getIndex());
    //    System.out.println(angle);
    // positive and both on the inside or both on the outside
    return ((angle > 0) == ((a.atom.getIndex() == jn[0].getIndex())
        && (d.atom.getIndex() == jn[3].getIndex()) || (a.atom.getIndex() == jn[1]
        .getIndex()) && (d.atom.getIndex() == jn[2].getIndex())) ? CIPChirality.STEREO_P
        : CIPChirality.STEREO_M);
  }

  private SmilesAtom findCumulativeCenter(CIPAtom a, CIPAtom a2) {
    SimpleNode center = a.atom, c = null, b = null;
    while (center != null && center != a2.atom) {
      SimpleEdge[] edges = center.getEdges();
      for (int i = edges.length; --i >= 0;) {
        if (edges[i].getCovalentOrder() == 2
            && (c = edges[i].getOtherNode(center)) != b) {
          SmilesAtom sa = (SmilesAtom) c;
          if (sa.stereo != null) {
            return sa;
          }
        }
      }
      b = center;
      center = c;
    }
    return null;
  }

  private Node[] nodes = new Node[6];

  //  public boolean canBeChiralBond(SimpleEdge bond) {
  //    return !htKekuleBonds.containsKey(Integer.valueOf(bond.hashCode()));
  //  }

  @Override
  boolean setCoord(SimpleNode atom, CIPAtom[] atoms) {
    SmilesAtom a = (SmilesAtom) atom;
    if (a.stereo == null)
      return false;
    Edge[] edges = a.getEdges();
    for (int i = edges.length; --i >= 0;)
      nodes[i] = (Node) edges[i].getOtherNode(a);
    a.stereo.setTopoCoordinates(a, null, null, nodes, false);
    return true;

  }

  public String[] getSmilesChiralityArray() {
    Lst<String> chirality = new Lst<String>();
    for (int i = 0; i < atoms.length; i++) {
      SmilesAtom a = (SmilesAtom) atoms[i];
      int pt = a.getPatternIndex();
      if (pt >= 0) {
        String c = a.getCIPChirality(false);
        //if (c != "")
        chirality.addLast(c);//(pt+1) + c);
      }
    }
    return chirality.toArray(new String[chirality.size()]);
  }
}
