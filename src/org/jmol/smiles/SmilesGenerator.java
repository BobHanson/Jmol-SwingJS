/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-06-05 21:50:17 -0500 (Sat, 05 Jun 2010) $
 * $Revision: 13295 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.SB;

import javajs.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;

/**
 * Double bond, allene, square planar and tetrahedral stereochemistry only
 * not octahedral or trigonal bipyramidal.
 * 
 * No attempt at canonicalization -- unnecessary for model searching.
 * 
 * see SmilesMatcher and package.html for details
 *
 * Bob Hanson, Jmol 12.0.RC17 2010.06.5
 *
 */
public class SmilesGenerator {

  // inputs:
  private Node[] atoms;
  private int ac;
  private BS bsSelected;
  private BS bsAromatic;
  private int flags;
  
  private boolean explicitH;
  
  private Lst<BS> ringSets;

  // data

  private VTemp vTemp = new VTemp();
  private int nPairs, nPairsMax;
  private BS bsBondsUp = new BS();
  private BS bsBondsDn = new BS();
  private BS bsToDo;
  private SimpleNode prevAtom;
  private SimpleNode[] prevSp2Atoms;
  private SimpleNode[] alleneStereo;
  
  // outputs

  private Map<String, Object[]> htRingsSequence = new Hashtable<String, Object[]>();
  private Map<String, Object[]> htRings = new Hashtable<String, Object[]>();
  private BS bsRingKeys = new BS();
  private BS bsIncludingH;
  private boolean topologyOnly;
  boolean getAromatic = true;
  private boolean addAtomComment;
  private boolean noBioComment;
  private boolean aromaticDouble;
  private boolean noStereo;
  private boolean openSMILES;
  public P3 polySmilesCenter;
  private SmilesStereo smilesStereo;
  private boolean isPolyhedral;
  private Lst<BS> aromaticRings;
  private SmilesMatcher sm;
  private int iHypervalent;

  // generation of SMILES strings

  String getSmiles(SmilesMatcher sm, Node[] atoms, int ac, BS bsSelected, String comment, int flags)
      throws InvalidSmilesException {
    int ipt = bsSelected.nextSetBit(0);
    if (ipt < 0)
      return "";
    this.sm = sm;
    this.flags = flags;
    this.atoms = atoms;
    this.ac = ac;
    bsSelected = BSUtil.copy(bsSelected);    

    // note -- some of these are 2-bit flags, so we need to use (flags & X) == X 
    
    this.bsSelected = bsSelected;
    this.flags = flags = SmilesSearch.addFlags(flags,  comment == null ? "" : comment.toUpperCase());
    if ((flags & JC.SMILES_GEN_BIO) == JC.SMILES_GEN_BIO)
      return getBioSmiles(bsSelected, comment, flags);
    openSMILES = ((flags & JC.SMILES_TYPE_OPENSMILES) == JC.SMILES_TYPE_OPENSMILES);
    addAtomComment = ((flags & JC.SMILES_GEN_ATOM_COMMENT) == JC.SMILES_GEN_ATOM_COMMENT);
    aromaticDouble =  ((flags & JC.SMILES_AROMATIC_DOUBLE) == JC.SMILES_AROMATIC_DOUBLE);

    explicitH = ((flags & JC.SMILES_GEN_EXPLICIT_H) == JC.SMILES_GEN_EXPLICIT_H);
    topologyOnly = ((flags & JC.SMILES_GEN_TOPOLOGY) == JC.SMILES_GEN_TOPOLOGY);
    getAromatic = !((flags & JC.SMILES_NO_AROMATIC) == JC.SMILES_NO_AROMATIC);
    noStereo = ((flags & JC.SMILES_IGNORE_STEREOCHEMISTRY) ==  JC.SMILES_IGNORE_STEREOCHEMISTRY);
    isPolyhedral = ((flags & JC.SMILES_GEN_POLYHEDRAL) == JC.SMILES_GEN_POLYHEDRAL);
    return getSmilesComponent(atoms[ipt], bsSelected, true, false, false);
  }

  private String getBioSmiles(BS bsSelected, String comment, int flags)
      throws InvalidSmilesException {
    addAtomComment = ((flags & JC.SMILES_GEN_ATOM_COMMENT) == JC.SMILES_GEN_ATOM_COMMENT);
    boolean allowUnmatchedRings = ((flags & 
        JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS) == JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS);
    boolean noBioComments = ((flags & JC.SMILES_GEN_BIO_NOCOMMENTS) == JC.SMILES_GEN_BIO_NOCOMMENTS);
    boolean crosslinkCovalent = ((flags & JC.SMILES_GEN_BIO_COV_CROSSLINK) == JC.SMILES_GEN_BIO_COV_CROSSLINK);
    boolean crosslinkHBonds = ((flags & JC.SMILES_GEN_BIO_HH_CROSSLINK) == JC.SMILES_GEN_BIO_HH_CROSSLINK);
    boolean addCrosslinks = (crosslinkCovalent || crosslinkHBonds);
    SB sb = new SB();
    BS bs = bsSelected;
    if (comment != null && !noBioComment)
      sb.append("//* Jmol bioSMILES ").append(comment.replace('*', '_'))
          .append(" *//");
    String end = (noBioComment ? "" : "\n");
    BS bsIgnore = new BS();
    String lastComponent = null;
    String groupString = "";
    String s;
    Lst<Integer> vLinks = new Lst<Integer>();
    try {
      int len = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Node a = atoms[i];
        String ch = a.getGroup1('?');
        String bioStructureName = a.getBioStructureTypeName();
        boolean unknown = (ch == ch.toLowerCase());
        if (end != null) {
          if (sb.length() > 0)
            sb.append(end);
          end = null;
          len = 0;
          if (bioStructureName.length() > 0) {
            int id = a.getChainID();
            if (id != 0 && !noBioComments) {
              s = "//* chain " + a.getChainIDStr() + " " + bioStructureName
                  + " " + a.getResno() + " *// ";
              len = s.length();
              sb.append(s);
            }
            len++;
            sb.append("~").appendC(bioStructureName.toLowerCase().charAt(0))
                .append("~");
          } else {
            s = getSmilesComponent(a, bs, false, true, true);
            if (s.equals(lastComponent)) {
              end = "";
              continue;
            }
            lastComponent = s;
            String groupName = a.getGroup3(true);
            String key;
            if (noBioComments) {
              key = "/" + s + "/";
            } else {
              if (groupName != null) {
                s = "//* " + groupName + " *//" + s;
              }
              key = s + "//";
            }
            if (groupString.indexOf(key) >= 0) {
              end = "";
              continue;
            }
            groupString += key;
            sb.append(s);
            end = (noBioComments ? "." : ".\n");
            continue;
          }
        }
        if (len >= 75 && !noBioComments) {
          sb.append("\n  ");
          len = 2;
        }
        if (addAtomComment)
          sb.append("\n//* [" + a.getGroup3(false) + "#" + a.getResno()
              + "] *//\t");
        if (unknown) {
          addBracketedBioName(sb, a, bioStructureName.length() > 0 ? ".0"
              : null, false);
        } else {
          sb.append(ch);
        }
        len++;
        //int i0 = a.getOffsetResidueAtom("\0", 0);
        if (addCrosslinks) {
          a.getCrossLinkVector(vLinks, crosslinkCovalent, crosslinkHBonds);
          for (int j = 0; j < vLinks.size(); j += 3) {
            sb.append(":");
            s = getRingCache(vLinks.get(j).intValue(), vLinks.get(j + 1)
                .intValue(), htRingsSequence);
            sb.append(s);
            len += 1 + s.length();
          }
          vLinks.clear();
        }
        a.getGroupBits(bsIgnore);
        bs.andNot(bsIgnore);
        int i2 = a.getOffsetResidueAtom("\0", 1);
        if (i2 < 0 || !bs.get(i2)) {
          if (!noBioComments)
            sb.append(" //* ").appendI(a.getResno()).append(" *//");
          if (i2 < 0 && (i2 = bs.nextSetBit(i + 1)) < 0)
            break;
          if (len > 0)
            end = (noBioComments ? "." : ".\n");
        }
        i = i2 - 1;
      }
    } catch (Exception e) {
      throw new InvalidSmilesException("//* error: " + e.getMessage() + " *//");
    }
    if (!allowUnmatchedRings && !htRingsSequence.isEmpty()) {
      dumpRingKeys(sb, htRingsSequence);
      throw new InvalidSmilesException("//* ?ring error? *//");
    }
    s = sb.toString();
    if (s.endsWith(".\n"))
      s = s.substring(0, s.length() - 2);
    else if (noBioComments && s.endsWith("."))
      s = s.substring(0, s.length() - 1);
    return s;
  }

  private void addBracketedBioName(SB sb, Node atom, String atomName,
                                   boolean addComment) {
    sb.append("[");
    if (atomName != null) {
      String chain = atom.getChainIDStr();
      sb.append(atom.getGroup3(false));
      if (!atomName.equals(".0"))
        sb.append(atomName).append("#").appendI(atom.getElementNumber());
      if (addComment) {
        sb.append("//* ").appendI(atom.getResno());
        if (chain.length() > 0)
          sb.append(":").append(chain);
        sb.append(" *//");
      }
    } else {
      sb.append(Elements.elementNameFromNumber(atom.getElementNumber()));
    }
    sb.append("]");
  }

  /**
   * 
   * creates a valid SMILES string from a model. TODO: stereochemistry other
   * than square planar and tetrahedral
   * 
   * @param atom
   * @param bs
   * @param allowBioResidues
   * @param allowConnectionsToOutsideWorld
   * @param forceBrackets
   * @return SMILES
   * @throws InvalidSmilesException
   */
  private String getSmilesComponent(Node atom, BS bs, boolean allowBioResidues,
                                    boolean allowConnectionsToOutsideWorld,
                                    boolean forceBrackets)
      throws InvalidSmilesException {

    if (!explicitH && atom.getAtomicAndIsotopeNumber() == 1
        && atom.getEdges().length > 0)
      atom = atoms[atom.getBondedAtomIndex(0)]; // don't start with H
    
    bsSelected = JmolMolecule.getBranchBitSet(atoms, atom.getIndex(),
        BSUtil.copy(bs), null, -1, true, allowBioResidues);
    bs.andNot(bsSelected);
    iHypervalent = -1; // this needs to be a bitset
    
    for (int i = bsSelected.nextSetBit(0); i >= 0 && iHypervalent < 0; i = bsSelected.nextSetBit(i + 1))
      if (atoms[i].getCovalentBondCount() > 4 || isPolyhedral)
        iHypervalent = i;
//    if (iHypervalent >= 0)
  //    explicitH = true;
    bsIncludingH = BSUtil.copy(bsSelected);
    if (!explicitH)
      for (int j = bsSelected.nextSetBit(0); j >= 0; j = bsSelected
          .nextSetBit(j + 1)) {
        Node a = atoms[j];
        if (a.getAtomicAndIsotopeNumber() == 1 
            && a.getBondCount() > 0 && a.getBondedAtomIndex(0) != iHypervalent)
          bsSelected.clear(j);
      }
    bsAromatic = new BS();
    if (!topologyOnly && bsSelected.cardinality() > 2) {
      generateRingData();
      setBondDirections();
    }
    bsToDo = BSUtil.copy(bsSelected);
    SB sb = new SB();

    // The idea hear is to allow a hypervalent atom to be listed first
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1))
      if (atoms[i].getCovalentBondCount() > 4 || isPolyhedral) {
        if (atom == null)
          sb.append(".");
        getSmilesAt(sb, atoms[i], allowConnectionsToOutsideWorld, false, forceBrackets);
        atom = null;
      }
    if (atom != null)
      while ((atom = getSmilesAt(sb, atom, allowConnectionsToOutsideWorld,
          true, forceBrackets)) != null) {
      }
    while (bsToDo.cardinality() > 0 || !htRings.isEmpty()) {
      Iterator<Object[]> e = htRings.values().iterator();
      if (e.hasNext()) {
        atom = atoms[((Integer) e.next()[1]).intValue()];
        if (!bsToDo.get(atom.getIndex()))
          break;
      } else {
        atom = atoms[bsToDo.nextSetBit(0)];
      }
      sb.append(".");
      prevSp2Atoms = alleneStereo = null;
      prevAtom = null;
      while ((atom = getSmilesAt(sb, atom, allowConnectionsToOutsideWorld,
          true, forceBrackets)) != null) {
      }
    }
    if (!htRings.isEmpty()) {
      dumpRingKeys(sb, htRings);
      throw new InvalidSmilesException("//* ?ring error? *//\n" + sb);
    }
    String s = sb.toString();
    if (s.indexOf("^-") >= 0) {
      String s0 = s;
      try {
      String keys = sm.getAtropisomerKeys(s, atoms, ac, bsSelected, bsAromatic, flags);
      for (int i = 1; i < keys.length();) {
        int pt = s.indexOf("^-");
        if (pt < 0)
          break;
        s = s.substring(0, pt + 1) + keys.substring(i, i + 2) + s.substring(pt + 1);
        i += 3;
      }
      } catch (Exception e) {
        System.out.println("???");
        s = s0;
      }
    }
    return s;
  }

  /**
   * 
   * get aromaticity, ringSets, and aromaticRings fields so that we can
   * assign / and \ and also provide inter-aromatic single bond
   * 
   * @throws InvalidSmilesException
   */
  private void generateRingData() throws InvalidSmilesException {
    // we are not actually running this search, just getting preliminary data    
    SmilesSearch search = SmilesParser.newSearch("[r500]", true, true);
    search.targetAtoms = atoms;
    search.setSelected(bsSelected);
    search.setFlags(flags);
    search.targetAtomCount = ac;
    search.ringDataMax = 7;
    search.flags = flags;
    Lst<BS>[] vRings = AU.createArrayOfArrayList(4);
    search.setRingData(null, vRings, true);
    bsAromatic = search.bsAromatic;
    ringSets = search.ringSets;
    aromaticRings = vRings[3];
  }

  /**
   * Retrieves the saved character based on the index of the bond.
   * bsBondsUp and bsBondsDown are global fields.
   * 
   * @param bond
   * @param atomFrom
   * @return   the correct character '/', '\\', '\0' (meaning "no stereochemistry")
   */
  private char getBondStereochemistry(Edge bond, SimpleNode atomFrom) {
    if (bond == null)
      return '\0';
    int i = bond.index;
    boolean isFirst = (atomFrom == null || bond.getAtomIndex1() == atomFrom
        .getIndex());
    return (bsBondsUp.get(i) ? (isFirst ? '/' : '\\')
        : bsBondsDn.get(i) ? (isFirst ? '\\' : '/') : '\0');
  }

  /**
   * Creates global BitSets bsBondsUp and bsBondsDown. Noniterative. 
   *
   */
  private void setBondDirections() {
    BS bsDone = new BS();
    Edge[][] edges = new Edge[2][3];
    
    // We don't assume a bond list, just an atom list, so we
    // loop through all the bonds of all the atoms, flagging them
    // as having been done already so as not to do twice. 
    // The bonds we are marking will be bits in bsBondsUp or bsBondsDn
    
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1)) {
      Node atom1 = atoms[i];
      Edge[] bonds = atom1.getEdges();
      for (int k = 0; k < bonds.length; k++) {
        Edge bond = bonds[k];
        int index = bond.index;
        SimpleNode atom2;
        if (bsDone.get(index) || bond.getCovalentOrder() != 2
            || SmilesSearch.isRingBond(ringSets, null, i, (atom2 = bond.getOtherNode(atom1)).getIndex()))
          continue;
        bsDone.set(index);
        int nCumulene = 0;
        SimpleNode a10 = atom1;
        while (atom2.getCovalentBondCount() == 2 && atom2.getValence() == 4) {
          Edge[] e2 = (Edge[]) atom2.getEdges();
          Edge e = e2[e2[0].getOtherNode(atom2) == a10 ? 1 : 0];
          bsDone.set(e.index);
          a10 = atom2;
          atom2 = e.getOtherNode(atom2);
          nCumulene++;
        }
        if (nCumulene % 2 == 1)
          continue;
        Edge b0 = null;
        SimpleNode a0 = null;
        int i0 = 0;
        SimpleNode[] atom12 = new SimpleNode[] { atom1, atom2 };
        int edgeCount = 1;
        
        // OK, so we have a double bond. Only looking at single bonds around it.
        
        // First pass: just see if there is an already-assigned bond direction
        // and collect the edges in an array. 
        
        for (int j = 0; j < 2 && edgeCount > 0 && edgeCount < 3; j++) {
          edgeCount = 0;
          SimpleNode atomA = atom12[j];
          Edge[] bb = ((Node) atomA).getEdges();
          for (int b = 0; b < bb.length; b++) {
            SimpleNode other;
            if (bb[b].getCovalentOrder() != 1 || !explicitH && (other = bb[b].getOtherNode(atomA)).getElementNumber() == 1
                && other.getIsotopeNumber() == 0)
              continue;
            edges[j][edgeCount++] = bb[b];
            if (getBondStereochemistry(bb[b], atomA) != '\0') {
              b0 = bb[b];
              i0 = j;
            }
          }
        }
        if (edgeCount == 3 || edgeCount == 0)
          continue;
        
        // If no bond around this double bond is already marked, we assign it UP.
        
        if (b0 == null) {
          i0 = 0;
          b0 = edges[i0][0];
          bsBondsUp.set(b0.index);
        }
        
        // The character '/' or '\\' is assigned based on a
        // geometric reference to the reference bond. Initially
        // this comes in in reference to the double bond, but
        // when we save the bond, we are saving the correct 
        // character for the bond itself -- based on its 
        // "direction" from atom 1 to atom 2. Then, when 
        // creating the SMILES string, we use the atom on the 
        // left as the reference to get the correct character
        // for the string itself. The only tricky part, I think.
        // SmilesSearch.isDiaxial is just a simple method that
        // does the dot products to determine direction. In this
        // case we are looking simply for vA.vB < 0,meaning 
        // "more than 90 degrees apart" (ab, and cd)
        // Parity errors would be caught here, but I doubt you
        // could ever get that with a real molecule. 
        
        char c0 = getBondStereochemistry(b0, atom12[i0]);
        a0 = b0.getOtherNode(atom12[i0]);
        if (a0 == null)
          continue;
        for (int j = 0; j < 2; j++)
          for (int jj = 0; jj < 2; jj++) {
            Edge b1 = edges[j][jj];
            if (b1 == null || b1 == b0)
              continue;
            int bi = b1.index;
            SimpleNode a1 = b1.getOtherNode(atom12[j]);
            if (a1 == null)
              continue;
            char c1 = getBondStereochemistry(b1, atom12[j]);

            //   c1 is FROM the double bond:
            //    
            //     a0    a1
            //      \   /
            //    [i0]=[j]       /a /b  \c \d
            //   
            boolean isOpposite = SmilesStereo.isDiaxial(atom12[i0], atom12[j],
                a0, a1, vTemp, 0);
            if (c1 == '\0' || (c1 != c0) == isOpposite) {
              boolean isUp = (c0 == '\\' && isOpposite || c0 == '/'
                  && !isOpposite);
              if (isUp == (b1.getAtomIndex1() != a1.getIndex()))
                bsBondsUp.set(bi);
              else
                bsBondsDn.set(bi);
            } else {
              Logger.error("BOND STEREOCHEMISTRY ERROR");
            }
            if (Logger.debugging)
              Logger.debug(getBondStereochemistry(b0, atom12[0]) + " "
                  + a0.getIndex() + " " + a1.getIndex() + " "
                  + getBondStereochemistry(b1, atom12[j]));
          }
      }
    }
  }

  private int ptAtom, ptSp2Atom0;
  
  private Node getSmilesAt(SB sb, SimpleNode atom,
                           boolean allowConnectionsToOutsideWorld,
                           boolean allowBranches, boolean forceBrackets) {
    int atomIndex = atom.getIndex();
    if (!bsToDo.get(atomIndex))
      return null;
    ptAtom++;
    bsToDo.clear(atomIndex);
    boolean includeHs = (atomIndex == iHypervalent || explicitH);
    boolean isExtension = (!bsSelected.get(atomIndex));
    int prevIndex = (prevAtom == null ? -1 : prevAtom.getIndex());
    boolean isAromatic = bsAromatic.get(atomIndex);
    // prevSp2Atoms is for allene ABC=C=CDE
    SimpleNode[] sp2Atoms = prevSp2Atoms;
    boolean havePreviousSp2Atoms = (sp2Atoms != null);
    int atomicNumber = atom.getElementNumber();
    int nH = 0;
    SimpleNode[] prevStereo = alleneStereo;
    alleneStereo = null;
    Lst<Edge> v = new Lst<Edge>();
    Edge bondNext = null;
    Edge bondPrev = null;
    Edge[] bonds = (Edge[]) atom.getEdges();
    if (polySmilesCenter != null) {
      allowBranches = false;
      sortBonds(atom, prevAtom, polySmilesCenter);
    }
    SimpleNode aH = null;
    int stereoFlag = (isAromatic ? 10 : 0);
    if (Logger.debugging)
      Logger.debug(sb.toString());

    // first look through the bonds for the best 
    // continuation -- bond0 -- and count hydrogens
    // and create a list of bonds to process.
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;) {
        Edge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        SimpleNode atom1 = bonds[i].getOtherNode(atom);
        int index1 = atom1.getIndex();
        if (index1 == prevIndex) {
          bondPrev = bonds[i];
          continue;
        }
        boolean isH = !includeHs
            && (atom1.getElementNumber() == 1 && atom1.getIsotopeNumber() == 0);
        if (!bsIncludingH.get(index1)) {
          if (!isH && allowConnectionsToOutsideWorld
              && bsSelected.get(atomIndex))
            bsToDo.set(index1);
          else
            continue;
        }
        if (isH) {
          aH = atom1;
          nH++;
          if (nH > 1)
            stereoFlag = 10;
        } else {
          v.addLast(bonds[i]);
        }
      }

    // order of listing is critical for stereochemistry:
    //
    // 1) previous atom
    // 2) bond from previous atom
    // 3) atom symbol (possibly followed with H or Hn if in brackets
    // 4) connections
    // 5) branches

    // add the bond from the previous atom and carry over the prev atom in case it is sp2
    if (nH > 1)
      sp2Atoms = null;
    int nSp2Atoms = (sp2Atoms != null ? 2 : 0);
    if (sp2Atoms == null && !isAromatic && nH <= 1)
      sp2Atoms = new Node[5];
    String strPrev = null;
    if (bondPrev != null) {
      strPrev = getBondOrder(bondPrev, atomIndex, prevIndex, isAromatic);
      if (sp2Atoms != null && !havePreviousSp2Atoms) {
        sp2Atoms[nSp2Atoms++] = prevAtom;
      }
    }
    if (sp2Atoms != null && !havePreviousSp2Atoms) {
      ptSp2Atom0 = ptAtom;
    }

    //    if (isAromatic || nH > 1){
    // not sure why this next was here:
    //|| atomNext != null && SmilesSearch.isRingBond(ringSets, null, atomIndex,
    //  atomNext.getIndex())) {
    //    sp2Atoms = null;
    //  }

    if (sp2Atoms != null && nH == 1)
      sp2Atoms[nSp2Atoms++] = aH;

    // determine which connected atom should carry on the chain
    // atomNext will not be in parentheses or marked as a connection number
    int nMax = 0;
    BS bsBranches = new BS();
    int nBonds = v.size();
    if (allowBranches) {
      for (int i = 0; i < nBonds; i++) {
        Edge bond = v.get(i);
        SimpleNode a = bond.getOtherNode(atom);
        int n = a.getCovalentBondCount()
            - (includeHs ? 0 : ((Node) a).getCovalentHydrogenCount());
        int order = bond.getCovalentOrder();
        if (n == 1 && (bondNext != null || i < nBonds - 1)) {
          bsBranches.set(bond.index);
        } else if ((order > 1 || n > nMax)
            && !htRings.containsKey(getRingKey(a.getIndex(), atomIndex))) {
          nMax = (order > 1 ? 1000 + order : n);
          bondNext = bond;
        }
      }
    }
    Node atomNext = (bondNext == null ? null : (Node) bondNext
        .getOtherNode(atom));
    int orderNext = (bondNext == null ? 0 : bondNext.getCovalentOrder());

    // initialize stereo[] for stereochemistry 
    SimpleNode[] stereo = new Node[7];

    if (stereoFlag < 7 && bondPrev != null) {
      if (havePreviousSp2Atoms && bondPrev.getCovalentOrder() == 2
          && orderNext == 2 && sp2Atoms[1] != null) {
        // allene continuation
        stereo[stereoFlag++] = sp2Atoms[0];
        stereo[stereoFlag++] = sp2Atoms[1];
      } else {
        stereo[stereoFlag++] = prevAtom;
      }
    }

    if (stereoFlag < 7 && nH == 1)
      stereo[stereoFlag++] = aH;

    boolean deferStereo = (orderNext == 1 && sp2Atoms == null);
    char chBond = getBondStereochemistry(bondPrev, prevAtom);

    if (strPrev != null || chBond != '\0') {
      if (chBond != '\0')
        strPrev = "" + chBond;
      sb.append(strPrev);
    }

    // We have to output branches after rings, but
    // we need the branches to determine the rings! 
    // No problem, except we are tracking stereochemistry

    // from here on, globals such as prevBondAtoms and prevAtom must not be used,
    // as we are about to re-enter this method for a branch.

    int stereoFlag0 = stereoFlag;
    int nSp2Atoms0 = nSp2Atoms;
    SB sbBranches = new SB();
    Lst<String> vBranches = new Lst<String>();
    for (int i = 0; i < v.size(); i++) {
      Edge bond = v.get(i);
      if (!bsBranches.get(bond.index))
        continue;
      SimpleNode a = bond.getOtherNode(atom);
      SB s2 = new SB();
      prevAtom = atom;
      prevSp2Atoms = alleneStereo = null;
      Edge bond0t = bondNext;
      int ptSp2Atom0t = ptSp2Atom0;
      int ptAtomt = ptAtom;
      // next call re-enters this method.
      getSmilesAt(s2, a, allowConnectionsToOutsideWorld, allowBranches,
          forceBrackets);
      bondNext = bond0t;
      ptAtom = ptAtomt;
      ptSp2Atom0 = ptSp2Atom0t;
      String branch = s2.toString();
      // catch is that if there are 
      v.removeItemAt(i--);
      if (bondNext == null)
        vBranches.addLast(branch);
      else
        sbBranches.append("(").append(branch).append(")");
      if (stereoFlag < 7)
        stereo[stereoFlag++] = a;
      if (sp2Atoms != null && nSp2Atoms < 5)
        sp2Atoms[nSp2Atoms++] = a;
    }

    // now process any connections

    SB sbRings = new SB();

    int stereoFlag1 = stereoFlag;
    int nSp2Atoms1 = nSp2Atoms;

    String atat = null;
    if (!allowBranches && !noStereo && polySmilesCenter == null
        && (v.size() == 5 || v.size() == 6)) {
      // only for first hypervalent atom; we are not allowing any branches here
      atat = sortInorganic(atom, v, vTemp);
    }
    for (int i = 0; i < v.size(); i++) {
      Edge bond = v.get(i);
      if (bond == bondNext)
        continue;
      SimpleNode a = bond.getOtherNode(atom);
      strPrev = getBondOrder(bond, atomIndex, a.getIndex(), isAromatic);
      if (!deferStereo) {
        chBond = getBondStereochemistry(bond, atom);
        if (chBond != '\0')
          strPrev = "" + chBond;
      }
      sbRings.append(strPrev);
      sbRings.append(getRingCache(atomIndex, a.getIndex(), htRings));
      if (stereoFlag < 7)
        stereo[stereoFlag++] = a;
      if (sp2Atoms != null && nSp2Atoms < 5)
        sp2Atoms[nSp2Atoms++] = a;
    }

    // Reorder the stereo[] and sp2Atoms[] arrays because 
    // they may be out of order with respect to bonds and rings.

    if (stereoFlag0 != stereoFlag1 && stereoFlag1 != stereoFlag)
      swapArray(stereo, stereoFlag0, stereoFlag1, stereoFlag);
    if (nSp2Atoms0 != nSp2Atoms1 && nSp2Atoms1 != nSp2Atoms)
      swapArray(sp2Atoms, nSp2Atoms0, nSp2Atoms1, nSp2Atoms);

    // now the atom symbol or bracketed expression
    // we allow for charge, hydrogen count, isotope number,
    // and stereochemistry 

    if (havePreviousSp2Atoms && stereoFlag == 2 && orderNext == 2) {
      int nc = (ptAtom - ptSp2Atom0);
      int nb = atomNext.getCovalentBondCount();
      boolean lastIsN = (atomNext.getElementNumber() == 7);
      if (nc % 2 == 0) {
        stereoFlag = 8; // no @/@@ for even cumulenes
      } else {
        if (nb == 3 || nb == 2 && lastIsN) {
          // this is for allenes only, not general odd cumulenes
          bonds = atomNext.getEdges();
          for (int k = 0; k < bonds.length; k++) {
            int index = atomNext.getBondedAtomIndex(k);
            if (bonds[k].isCovalent() && index != atomIndex)
              stereo[stereoFlag++] = atoms[index];
          }
          if (nb == 2) // X=C=NR
            stereo[stereoFlag++] = atomNext;
          if (stereoFlag == 4) {
            alleneStereo = stereo;
            if (((Node) stereo[3]).getAtomicAndIsotopeNumber() == 1) {
              // if last atom is 1H, we swap.
              SimpleNode n = stereo[3];
              stereo[3] = stereo[2];
              stereo[2] = n;
            }
          }
        }
      }
      nSp2Atoms = 0;
    } else if (atomNext != null && stereoFlag < 7) {
      stereo[stereoFlag++] = atomNext;
    }
    if (prevStereo != null) {
      if (prevStereo[3] != stereo[2]) {
        // must switch allene stereochem because there has been a switch in substituents
        int ptat = sb.lastIndexOf("@]=");
        if (ptat > 0) {
          String trail = sb.substring(ptat);
          sb.setLength(sb.charAt(ptat - 1) == '@' ? ptat - 1 : ptat + 1);
          sb.append(trail);
        }
      }
      prevStereo = null;
    }
    int charge = atom.getFormalCharge();
    int isotope = atom.getIsotopeNumber();
    int valence = atom.getValence();
    float osclass = (openSMILES ? ((Node) atom)
        .getFloatProperty("property_atomclass") : Float.NaN);
    String atomName = atom.getAtomName();
    String groupType = ((Node) atom).getBioStructureTypeName();
    // for bioSMARTS we provide the connecting atom if 
    // present. For example, in 1BLU we have 
    // .[CYS.SG#16] could match either the atom number or the element number 
    if (addAtomComment)
      sb.append("\n//* " + atom.toString() + " *//\t");
    if (topologyOnly)
      sb.append("*");
    else if (isExtension && groupType.length() != 0 && atomName.length() != 0)
      addBracketedBioName(sb, (Node) atom, "." + atomName, false);
    else
      sb.append(SmilesAtom.getAtomLabel(
          atomicNumber,
          isotope,
          (forceBrackets ? -1 : valence),
          charge,
          osclass,
          nH,
          isAromatic,
          atat != null ? atat : noStereo ? null : checkStereoPairs(atom,
              alleneStereo == null ? atomIndex : -1, stereo, stereoFlag)));

    // add the rings...

    sb.appendSB(sbRings);

    // ...then add the branches

    if (bondNext != null) {
      // not the end - all branches get parentheses
      sb.appendSB(sbBranches);
    } else {
      // the last branch has no parentheses
      int n = vBranches.size() - 1;
      if (n >= 0) {
        for (int i = 0; i < n; i++)
          sb.append("(").append(vBranches.get(i)).append(")");
        sb.append(vBranches.get(n));
      }
      return null;
    }

    if (sp2Atoms != null && orderNext == 2
        && (nSp2Atoms == 1 || nSp2Atoms == 2)) {
      if (sp2Atoms[0] == null)
        sp2Atoms[0] = atom; // CN=C= , for example. close enough!
      if (sp2Atoms[1] == null)
        sp2Atoms[1] = atom; // .C3=C=
    } else {
      sp2Atoms = null;
      nSp2Atoms = 0;
    }

    // prevSp2Atoms is only so that we can track
    // ABC=C=CDE  systems

    prevSp2Atoms = sp2Atoms;
    prevAtom = atom;
    return atomNext;
  }

  private SimpleNode[] atemp;
  /**
   * swap slices of an array [i0 i1) with [i1 i2)
   * @param a
   * @param i0
   * @param i1
   * @param i2
   */
  private void swapArray(SimpleNode[] a, int i0, int i1, int i2) {
    
    int n = i1 - i0;
    if (atemp == null || atemp.length < n)
      atemp = new Node[n];
    for (int p = n, i = i1; p > 0;)
      atemp[--p] = a[--i];
    for (int i = i1; i < i2; i++)
      a[i - n] = a[i];
    for (int p = n, i = i2; p > 0;)
      a[--i] = atemp[--p];
  }

  /**
   * 
   * @param bondPrev
   * @param atomIndex
   * @param prevIndex
   * @param isAromatic
   * @return "-", "=", "#", "$", or ""
   */
  private String getBondOrder(Edge bondPrev, int atomIndex, int prevIndex,
                              boolean isAromatic) {
    // look for Jmol having LOAD $biphenyl; CONNECT @{@1|@2} @{@7|@8} atropisomer
    
    if(topologyOnly)
      return "";
    if ((bondPrev.order & Edge.TYPE_ATROPISOMER) == Edge.TYPE_ATROPISOMER) {
      return "^-";
    }
    int border = bondPrev.getCovalentOrder();
    return (!isAromatic || !bsAromatic.get(prevIndex) ? SmilesBond
        .getBondOrderString(border) : border == 1
        && !isSameAromaticRing(atomIndex, prevIndex) ? 
            "-" 
            : aromaticDouble
        && (border == 2 || border == Edge.BOND_AROMATIC_DOUBLE) ? "=" : "");
  }

  private boolean isSameAromaticRing(int a1, int a2) {
    BS bs;
    for (int i = aromaticRings.size(); --i >= 0;)
      if ((bs = aromaticRings.get(i)).get(a1) && bs.get(a2))
        return true;
    return false;
  }

  void sortBonds(SimpleNode atom, SimpleNode refAtom, P3 center) {
    if (smilesStereo == null)
      try {
        smilesStereo = SmilesStereo.newStereo(null);
      } catch (InvalidSmilesException e) {
        // not possible
      }
    smilesStereo.sortBondsByStereo(atom, refAtom, center, atom.getEdges(), vTemp.vA);
  }

  /**
   * We must sort the bond vector such that a diaxial pair is
   * first and last. Then we assign stereochemistry based on what
   * is left. The assignment is not made if there are no diaxial groups
   * or with octahedral if there are fewer than three or trigonal bipyramidal
   * with no axial ligands.
   * 
   * @param atom
   * @param v
   * @param vTemp 
   * @return  "@" or "@@" or ""
   */
  private String sortInorganic(SimpleNode atom, Lst<Edge> v, VTemp vTemp) {
    int atomIndex = atom.getIndex();
    int n = v.size();
    Lst<Edge[]> axialPairs = new  Lst<Edge[]>();
    Lst<Edge> bonds = new  Lst<Edge>();
    SimpleNode a1, a2, a01 = null, a02 = null;
    Edge bond1, bond2;
    BS bsDone = new BS();
    Edge[] pair0 = null;
    SimpleNode[] stereo = new Node[6];
    boolean isOK = true; // AX6 or AX5
    String s = "";
    int naxial = 0;
    for (int i = 0; i < n; i++) {
      bond1 = v.get(i);
      stereo[0] = a1 = bond1.getOtherNode(atom);
      // just looking for the opposite atom not being identical
      if (i == 0)
        s = addStereoCheck(0, atomIndex, a1, "", null);
      else if (isOK && addStereoCheck(0, atomIndex, a1, s, null) != null)
        isOK = false;
      if (bsDone.get(i))
        continue;
      bsDone.set(i);
      boolean isAxial = false;
      for (int j = i + 1; j < n; j++) {
        if (bsDone.get(j))
          continue;
        bond2 = v.get(j);
        a2 = bond2.getOtherNode(atom);
        if (SmilesStereo.isDiaxial(atom, atom, a1, a2, vTemp, -0.95f)) {
          switch (++naxial) {
          case 1:
            a01 = a1;
            break;
          case 2:
            a02 = a1;
            break;
          case 3:
            // we must check to see if we have the proper winding for the
            // two "equatorial" pairs
            if (SmilesStereo.getHandedness(a02, a01, a1, atom, vTemp) == 2) {
              Edge b = bond1;
              bond1 = bond2;
              bond2 = b;
            }
            break;  
          }
          axialPairs.addLast(new Edge[] { bond1, bond2 });
          isAxial = true;
          bsDone.set(j);
          break;
        }
      }
      if (!isAxial)
        bonds.addLast(bond1);
    }
    int npAxial = axialPairs.size();

    // AX6 or AX5 are fine as is
    // can't proceed if octahedral and not all axial pairs
    // or trigonal bipyramidal and no axial pair.
    
    if (isOK || n == 6 && npAxial != 3 || n == 5 && npAxial == 0)
      return "";
    pair0 = axialPairs.get(0);
    bond1 = pair0[0];
    stereo[0] = bond1.getOtherNode(atom);
    
    // now sort them into the ligand vector in the proper order
    v.clear();
    v.addLast(bond1);
    if (npAxial > 1)
      bonds.addLast(axialPairs.get(1)[0]);
    if (npAxial == 3)
      bonds.addLast(axialPairs.get(2)[0]);
    if (npAxial > 1)
      bonds.addLast(axialPairs.get(1)[1]);
    if (npAxial == 3)
      bonds.addLast(axialPairs.get(2)[1]);
    for (int i = 0; i < bonds.size(); i++) {
      bond1 = bonds.get(i);
      v.addLast(bond1);
      stereo[i + 1] = bond1.getOtherNode(atom);
    }
    v.addLast(pair0[1]);
    stereo[n - 1] = pair0[1].getOtherNode(atom);
    
    // now deterimine the stereochemistry
    
    return SmilesStereo.getStereoFlag(atom, stereo, n, vTemp);
  }

  private String checkStereoPairs(SimpleNode atom, int atomIndex,
                                  SimpleNode[] stereo, int stereoFlag) {
    if (stereoFlag < 4)
      return "";
    if (atomIndex >= 0 && stereoFlag == 4 && (atom.getElementNumber()) == 6) {
      // do a quick check for two of the same group for tetrahedral carbon only
      String s = "";
      for (int i = 0; i < 4; i++) {
        if ((s = addStereoCheck(0, atomIndex, stereo[i], s, BSUtil.newAndSetBit(atomIndex))) == null) {
          stereoFlag = 10;
          break;
        }
      }
    }
    return (stereoFlag > 6 ? "" : SmilesStereo.getStereoFlag(atom, stereo,
        stereoFlag, vTemp));
  }
  
  private int chainCheck;

  /**
   * checks a group and either adds a new group to the growing check string or
   * returns null
   * @param level 
   * 
   * @param atomIndex
   * @param atom
   * @param s
   * @param bsDone
   * @return null if duplicate
   */
  private String addStereoCheck(int level, int atomIndex, SimpleNode atom, String s,
                                BS bsDone) {
    if (bsDone != null)
      bsDone.set(atomIndex);
    //System.out.println("level=" + level + " atomIndex=" + atomIndex + " atom=" + atom + " s=" + s);
    int n = ((Node)atom).getAtomicAndIsotopeNumber();
    int nx = atom.getCovalentBondCount();
    int nh = (n == 6 && !explicitH ? ((Node) atom).getCovalentHydrogenCount() : 0);
    // only carbon or singly-connected atoms are checked
    // for C we use nh -- CH3, for example.
    // for other atoms, we use number of bonds.
    // just checking for tetrahedral CH3)
    if (n == 6 ? nx != 4 : n == 1 || nx > 1)
      return s + (++chainCheck);
    String sa = ";" + level + "/" + n + "/" + nh + "/" + nx + (level == 0 ? "," : "_");
    if (n == 6) {
      switch (nh) {
      case 1:
        return s + sa + (++chainCheck);
      case 0:
      case 2:
        if (bsDone == null)
          return s;
        Edge[] edges = ((Node)atom).getEdges();
        String s2 = "";
        String sa2 = "";
        int nunique = (nh == 2 ? 0 : 3);
        for (int j = atom.getBondCount(); --j >= 0;) {
          SimpleNode a2 = edges[j].getOtherNode(atom);
          int i2 = a2.getIndex();
          if (bsDone.get(i2) || !edges[j].isCovalent() || a2.getElementNumber() == 1)
            continue;
          bsDone.set(i2);
          sa2 = addStereoCheck(level + 1, atom.getIndex(), a2, "", (BS) bsDone.clone());
          if (s2.indexOf(sa2) >= 0)
            nunique--;
          s2 += sa2;
        }
        if (nunique == 3)
          return s + sa + (++chainCheck);
        sa = (sa + s2).replace(',','_');
        if (level > 0)
          return s + sa;
              break;
      case 3:
        break;
      }
    }
    if (s.indexOf(sa) >= 0) {
      if (nh == 3) {
        // must check isotopes for CH3
        int ndt = 0;
        for (int j = 0; j < nx && ndt < 3; j++) {
          int ia = ((Node)atom).getBondedAtomIndex(j);
          if (ia == atomIndex)
            continue;
          ndt += atoms[ia].getAtomicAndIsotopeNumber();
        }
        if (ndt > 3)
          return s;
      }
      return null;
    }
    return s + sa;
  }

  private String getRingCache(int i0, int i1, Map<String, Object[]> ht) {
    String key = getRingKey(i0, i1);
    Object[] o = ht.get(key);
    String s = (o == null ? null : (String) o[0]);
    if (s == null) {
      bsRingKeys.set(++nPairs);
      nPairsMax = Math.max(nPairs, nPairsMax);
      ht.put(key,
          new Object[] { s = getRingPointer(nPairs), Integer.valueOf(i1),
              Integer.valueOf(nPairs) });
      if (Logger.debugging)
        Logger.debug("adding for " + i0 + " ring key " + nPairs + ": " + key);
    } else {
      ht.remove(key);
      // let the ring count go up to 9 before resetting if all rings are closed;
      // if it runs over 99 ever, then never reset it; 
      // otherwise If it runs over 9 ever, then just reset it to 10
      // otherwise if it hits 9, then reset it to 0
      int nPair = ((Integer) o[2]).intValue();
      bsRingKeys.clear(nPair);
      if (bsRingKeys.nextSetBit(0) < 0 && (nPairsMax == 2 || nPairsMax == 99)) {
        nPairsMax = nPairs = (nPairsMax == 99 ? 10 : 0);
      }
      if (Logger.debugging)
        Logger.debug("using ring key " + key);
    }
    return s;
  }

  private String getRingPointer(int i) {
    return (i < 10 ? "" + i : i < 100 ? "%" + i : "%(" + i + ")");
  }

  private void dumpRingKeys(SB sb, Map<String, Object[]> ht) {
    Logger.info(sb.toString() + "\n\n");
    for (String key: ht.keySet())
      Logger.info("unmatched connection: " + key);
  }

  protected static String getRingKey(int i0, int i1) {
    return Math.min(i0, i1) + "_" + Math.max(i0, i1);
  }

//static {
//  T3 atom = P3.new3(0,  0.0001f,  -1);
//  T3 atomPrev = P3.new3(0,  0 , 1);
//  T3 ref = P3.new3(0,  0 , 0);
//  T3 a = P3.new3(1,  0,  0);
// System.out.println(Measure.computeTorsion((T3) atom, (T3)atomPrev, ref, (T3) a, true));  
//}

}

