package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.PT;
import javajs.util.SB;

/**
 * 
 * adjusted for AFLOW options -  adding element names, environment radius on atom line 
 * 
 * http://cms.mpi.univie.ac.at/vasp/
 * 
 * @author Pieremanuele Canepa, Wake Forest University, Department of Physics
 *         Winston Salem, NC 27106, canepap@wfu.edu (pcanepa@mit.edu)
 * 
 * @author Bob Hanson
 * 
 * @version 1.0
 */

public class VaspPoscarReader extends AtomSetCollectionReader {

  protected Lst<String> atomLabels;
  private boolean haveAtomLabels = true;
  private boolean atomsLabeledInline;
  private float scaleFac;
  protected int ac;
  protected String title;
  protected boolean quiet;
  protected String[] defaultLabels;  

  @Override
  protected void initializeReader() throws Exception {
    isPrimitive = true;
    readStructure(null);
    continuing = false;
  }
  
  protected void readStructure(String titleMsg) throws Exception {
    title = rd().trim();
    int pt = title.indexOf("--params");
    if ((pt = title.indexOf("& ", pt + 1)) >= 0) {
      //AB3_hR8_155_c_de & a,c/a,x1,y2,y3 --params=4.91608,2.53341483458,0.237,0.43,0.07 & R32
      latticeType = title.substring(pt + 2, pt + 3);
      Logger.info("AFLOW lattice:" + latticeType + " title=" + title);
    }
    readUnitCellVectors();
    readMolecularFormula();
    readCoordinates();
    asc.setAtomSetName(title + (titleMsg == null ? "" : "[" + titleMsg + "]"));
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (!haveAtomLabels && !atomsLabeledInline)     
      appendLoadNote("VASP POSCAR reader using pseudo atoms Al B C Db...");
    finalizeReaderASCR();
  }

  protected void readUnitCellVectors() throws Exception {
    // Read Unit Cell
    setSpaceGroupName("P1");
    setFractionalCoordinates(true);
    scaleFac = parseFloatStr(rdline().trim());
    boolean isVolume = (scaleFac < 0);
    if (isVolume)
      scaleFac = (float) Math.pow(-scaleFac, 1./3.);      
    float[] unitCellData = new float[9];
    String s = rdline() + " " + rdline() + " " + rdline();
    Parser.parseStringInfestedFloatArray(s, null, unitCellData);
    if (isVolume) {
      M3 m = M3.newA9(unitCellData);
      scaleFac /= m.determinant3();
 //     System.out.println("scalecheck: " + scaleFac + " " + Math.pow(m.determinant3(), 1/3.));
    }
    if (scaleFac != 1)
      for (int i = 0; i < unitCellData.length; i++)
        unitCellData[i] *= scaleFac;
    addExplicitLatticeVector(0, unitCellData, 0);
    addExplicitLatticeVector(1, unitCellData, 3);
    addExplicitLatticeVector(2, unitCellData, 6);
  }

  protected String[] elementLabel;
  
  /**
   * try various ways to read the optional atom labels. There is no convention
   * here.
   * 
   * @throws Exception
   */
  protected void readMolecularFormula() throws Exception {
    //   H    C    O    Be   C    H
    if (elementLabel == null)
      elementLabel = PT.getTokens(discardLinesUntilNonBlank());
    String[] elementCounts;
    if (PT.parseInt(elementLabel[0]) == Integer.MIN_VALUE) {
      atomsLabeledInline = false;
      elementCounts = PT.getTokens(rdline());
      while (line != null && (elementCounts.length == 0 || parseIntStr(elementCounts[0]) == Integer.MIN_VALUE))
        elementCounts = PT.getTokens(rdline());      
      //   6    24    18     6     6    24
    } else {
      elementCounts = elementLabel;
      elementLabel = PT.split(title, " ");
      if (elementLabel.length != elementCounts.length
          || elementLabel[0].length() > 2) {
        elementLabel = PT.split(
            "Al B C Db Eu F Ga Hf I K Li Mn N O P Ru S Te U V W Xe Yb Zn", " ");
        haveAtomLabels = false;
      }
    }
    String[] labels = elementLabel;
    SB mf = new SB();
    atomLabels = new Lst<String>();
    ac = 0;
    for (int i = 0; i < elementCounts.length; i++) {
      int n = Integer.parseInt(elementCounts[i]);
      ac += n;
      String label = labels[i];
      mf.append(" ").append(label).appendI(n);
      for (int j = n; --j >= 0;)
        atomLabels.addLast(label);
    }
    String s = mf.toString();
    if (!quiet)
      appendLoadNote(ac + " atoms identified for" + s);
    asc.newAtomSet();
    asc.setAtomSetName(s);
  }

  int radiusPt = Integer.MIN_VALUE;
  int elementPt = Integer.MIN_VALUE;
  
  protected void readCoordinates() throws Exception {
    // If Selective is there, then skip a line 
    boolean isSelective = discardLinesUntilNonBlank().toLowerCase().contains("selective");
    if (isSelective)    
      rd();
    boolean isCartesian = (line.toLowerCase().contains("cartesian")); 
    if (isCartesian)
      setFractionalCoordinates(false);
    for (int i = 0; i < ac; i++) {
      float radius = Float.NaN;
      String[] tokens = PT.getTokens(rdline());
      if (radiusPt == Integer.MIN_VALUE) {
        for (int j = tokens.length; --j > 2;)
          if (tokens[j].equals("radius")) {
            radiusPt = j + 1;
          } else if (getElement(tokens[j]) != null) {
            elementPt = j;            
            atomsLabeledInline = true;
          }
      }
      if (radiusPt >= 0)
        radius = parseFloatStr(tokens[radiusPt]);
      String label = (atomsLabeledInline ? tokens[elementPt] : atomLabels.get(i));
      if (isCartesian)
        for (int j = 0; j < 3; j++)
          tokens[j] = "" + parseFloatStr(tokens[j]) * scaleFac;
      Atom atom = addAtomXYZSymName(tokens, 0, null, label);
      if (!Float.isNaN(radius))
        atom.radius = radius * scaleFac;
      if (asc.bsAtoms != null)
        asc.bsAtoms.set(atom.index);
    }
  }

  /**
   * allow for any number of characters, for which the first
   * one or two are an element symbol.
   * 
   * @param token
   * @return element symbol
   */
  protected String getElement(String token) {
    String s = null;
    switch (token.length()) {
    default:
      s = (token.length() > 2 ? token.substring(0, 2) : null);
      if (s != null && JmolAdapter.getElementNumber(s) >= 0)
        return s;
      //$FALL-THROUGH$
    case 1:
      if (JmolAdapter.getElementNumber(s = token.substring(0)) >= 0)
        return s;
      //$FALL-THROUGH$
    case 0:
      return null;
    }
  }

  protected String rdline() throws Exception  {
    rd();
    if (line != null && line.startsWith("["))
      line = line.substring(line.indexOf("]") + 1).trim();
    return line;
  }
}
