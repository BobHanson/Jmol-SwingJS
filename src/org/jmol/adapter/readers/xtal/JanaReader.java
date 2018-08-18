/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
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
package org.jmol.adapter.readers.xtal;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.MSInterface;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import javajs.util.BS;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.Lst;
import javajs.util.Matrix;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.util.Logger;
import org.jmol.util.Modulation;

/**
 * A reader for Jana M50+M40 file pairs.
 *  
 * TODO: rigid-body rotation TLS, local symmetry, and local axes
 * 
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/7/2013  
 */

public class JanaReader extends AtomSetCollectionReader {

  private Lst<float[]> lattvecs;
  private int thisSub;
  private String modAxes;
  private boolean haveM40Data;
  
  @Override
  public void initializeReader() throws Exception {
    modAxes = getFilter("MODAXES=");
    setFractionalCoordinates(true);
    asc.newAtomSet();
    asc.setCurrentModelInfo("autoBondUsingOccupation", Boolean.TRUE);
  }
  
  /////////////////////////////// M50 file reading ///////////////////////////////////
  
  // see below for M40 file reading
  
  
  final static String records = "tit  cell ndim qi   lat  sym  spg  end  wma";
  //                             0    5    10   15   20   25   30   35   40
  final static int TITLE = 0;
  final static int CELL  = 5;
  final static int NDIM  = 10;
  final static int QI    = 15;
  final static int LATT  = 20;
  final static int SYM   = 25;
  final static int SPG   = 30;
  final static int END   = 35;
  final static int WMATRIX = 40;
  
//  Version Jana2006
//  title
//  cell 8.987 15.503 12.258 90 90 90
//  esdcell 0.002 0.002 0.002 0 0 0
//  ndim 4 ncomp 1
//  qi 0 0 0.413
//  qr 0 0 0
//  spgroup Pmcn(00g)s00 62 3
//  lattice P
//  symmetry x1 x2 x3 x4
//  symmetry -x1+1/2 -x2+1/2 x3+1/2 x4+1/2
//  symmetry -x1 x2+1/2 -x3+1/2 -x4
//  symmetry x1+1/2 -x2 -x3 -x4+1/2
//  symmetry -x1 -x2 -x3 -x4
//  symmetry x1+1/2 x2+1/2 -x3+1/2 -x4+1/2
//  symmetry x1 -x2+1/2 x3+1/2 x4
//  symmetry -x1+1/2 x2 x3 x4+1/2

  @Override
  protected boolean checkLine() throws Exception {
    if (line.length() < 3)
      return true;
    Logger.info(line);
    parseTokenStr(line);
    switch (records.indexOf(line.substring(0, 3))) {
      case TITLE:
        asc.setAtomSetName(line.substring(5).trim());
        break;
      case CELL:
        cell();
        setSymmetryOperator("x,y,z");
        break;
      case NDIM:
        ndim();
        break;
      case LATT:
        if (lattvecs == null)
          lattvecs = new Lst<float[]>();
        if (!ms.addLatticeVector(lattvecs, line.substring(8)))
          appendLoadNote(line + " not supported");
        break;
      case SPG:
        setSpaceGroupName(getTokens()[1]);
        break;
      case SYM:
        symmetry();        
        break;
      case QI:
        qi();
        break;
      case END:
        // look for appended M40 data from load x.M50 + x.M40
        while (rd() != null) {
          if (line.startsWith("command") || parseIntStr(line) >= 0) {
            readM40Data(true);
            break;
          }
        }
        continuing = false;
        break;
      case WMATRIX:
        int n = 3 + modDim;
        Matrix m;
        if (thisSub++ == 0) {
          m = Matrix.identity(n, n);
          ms.addSubsystem("" + thisSub++, m);
        }
        m = new Matrix(null, n, n);
        double[][] a = m.getArray();
        float[] data = new float[n * n];
        fillFloatArray(null, 0, data);
        for (int i = 0, pt = 0; i < n; i++)
          for (int j = 0; j < n; j++, pt++)
             a[i][j] = data[pt];
        ms.addSubsystem("" + thisSub, m);
    }
    return true;
  }

  @Override
  public void doPreSymmetry() throws Exception {
    if (ms != null)
      ms.setModulation(false, null);
    // when M40 can store magnetic moments
    // but I do not know if they will be fractional like this.
    // currently vibsFractional is false
    if (vibsFractional)
      asc.getXSymmetry().scaleFractionalVibs();

  }

  @Override
  public void finalizeSubclassReader() throws Exception {
    if (!haveM40Data)
      readM40Data(false);
    if (lattvecs != null && lattvecs.size() > 0)
      asc.getSymmetry().addLatticeVectors(lattvecs);
    applySymmetryAndSetTrajectory();
    finalizeReaderASCR();
  }

  @Override
  protected void finalizeSubclassSymmetry(boolean haveSymmetry) throws Exception {
    // called by applySymTrajASCR();
    adjustM40Occupancies();
    if (ms != null && haveSymmetry) {
      ms.setModulation(true, asc.getXSymmetry().getBaseSymmetry());
      ms.finalizeModulation();
    }
  }


  private void cell() throws Exception {
    for (int ipt = 0; ipt < 6; ipt++)
      setUnitCellItem(ipt, parseFloat());
  }

  private void ndim() throws Exception {
    ms = (MSInterface) Interface
        .getOption("adapter.readers.cif.MSRdr", vwr, "file");
    modDim = ms.initialize(this, (parseIntStr(getTokens()[1]) - 3));
  }

  private int qicount;

  private void qi() throws Exception {
    double[] pt = new double[modDim];
    pt[qicount] = 1;
    double[] a = new double[] {parseFloat(), parseFloat(), parseFloat()};
    // get qr record as well
    parseTokenStr(rd());
    for (int i = 0; i < 3; i++)
      a[i] += parseFloat();
    ms.addModulation(null, "W_" + (++qicount), a, -1);
    ms.addModulation(null, "F_" + qicount + "_coefs_", pt, -1);
  }

  private void symmetry() throws Exception {
    setSymmetryOperator(PT.rep(line.substring(9).trim()," ", ","));
  }

  /////////////////////////////// M40 file reading ///////////////////////////////////
  
  // terms in JANA Manual98; some of these are mine:
  //
  // model molecule                -- group of atoms listed prior to the pos# record.
  // model atom                    -- an untransformed atom in the model molecule
  //                                  used for all #pos records relating to this model.
  // model parameters              -- parameters prior to the pos# record.
  // model reference point (rho)   -- an M40 model parameter used for all positions and all atoms.
  // model atom position (a)       -- model atom position found in M40 atom record.  
  // atom offset vector (v0)       -- a - rho, calculated

  // molecular parameters          -- parameters found in the pos# record.
  // molecular position            -- a final translated and rotated coordinate set.
  // molecular offset (vTrans)     -- an M40 molecular parameter
  // modulation reference pt (g)   -- the point of reference for modulations 
  //                                  for a given molecular position, calculated as rho + vTrans 
  // unrotated atomic position(aT) -- g + v0 = rho + vTrans + v0
  // molecular rotation (matR)     -- representing a proper or improper rotation.
  // final atom offset from g (vR) -- toFractional(matR(toCartesian(v0))), calculated
  // real (final) position (a')    -- the calculated transformed coordinates of the actual atoms
  //                                  after (possibly improper) rotation and translation:
  //                                  a' = rho + vTrans + vR = g + vR
  // sine/cosine vectors (vcosr,vsinr)M40 molecular rotational displacive modulation parameters
  //                                  that will be crossed with v0 (as Cartesians)
  //                                  and added to translational displacement modulation parameters,
  //                                  then rotated to give standard CIF sin/cos positional modulations.

  // overall process:
  //
  // 1) Read model parameters, including model atom positions and reference point
  // 2) Read pos# molecular parameters.
  // 3) Clone each model atom to form a molecular atom and add that to the atom set
  //    including calculation of v0cart and vR.
  // 4) Copy modulations from the pos# "atom", phase-shifting occupational modulations.
  // 5) Transform rotational modulations, add translational modulations.
  // 6) Phase shift all displacement modulations.

  
  
  
  //  12    0    0    1
  //  1.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000                                          00
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //
  //                                                         SPECIAL   COUNTS
  //                                                             ---  -------
  //                             x        y        z             ODU  O  D  U
  // Zn        5  1     0.500000 0.250000 0.406400 0.244000      000  0  2  0
  //
  // 0         1         2         3         4         5         6         7
  // 01234567890123456789012345678901234567890123456789012345678901234567890
  //
  //  0.048000 0.000000 0.000000 0.000000 0.000000 0.000000      0000000000
  //  0.015300 0.000000 0.000000-0.010100 0.000000 0.000000      000000
  //  0.000000 0.000200-0.000100 0.000000 0.000500-0.000400      000000
  //  0.000000                                                   0
  // ...
  //                   [occ_site]
  // Na1       1  2     0.500000 0.500000 0.000000 0.000000      000  1  1  1
  //  0.034155-0.007468 0.002638 0.000000-0.005723 0.000000      0000111010
  // [occ_0 for Fourier or width for crenel] 
  //  0.848047                                                   1
  // [center for Crenel; ]
  //  0.000000 0.312670                                          01
  //  0.029441 0.000000 0.003581 0.000000 0.000000 0.000000      101000
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  // -0.051170 0.000624-0.008585 0.000000 0.014781 0.000000      111010
  //  0.000000
  //

  private String molName;
  private Lst<Atom> molAtoms;
  private Lst<Integer> molTtypes;
  private Lst<P3> modelMolecule;
  private boolean molHasTLS;
  private M3 matR;
  private P3 rho;
  private boolean firstPosition;
  private V3 vR, v0Cart;
  private boolean isLegendre;


  /**
   * read the M40 file, possibly as the extension of M50+M40
   * 
   * @param haveReader
   * 
   * @throws Exception
   */
  private void readM40Data(boolean haveReader) throws Exception {
    if (haveReader) {
      // already have the line
      parseM40Floats();
    } else {
      // must retrieve separate file
      String m40File = filePath;
      int ipt = m40File.lastIndexOf(".");
      if (ipt < 0)
        return;
      m40File = m40File.substring(0, ipt + 2) + "40";
      String id = m40File.substring(0, ipt);
      reader.close();
      reader = Rdr.getBR((String) vwr.getLigandModel(id, m40File, "_file", "----"));
      if (out != null)
        out.append("******************************* M40 DATA *******************************\n");
      readM40Floats();
    }
    haveM40Data = true;
    if (line.startsWith("command"))
      readM40WaveVectors();

    // ref: manual98.pdf
    // Jana98: The Crystallographic Computing System
    // Vaclav Petricek and Michal Dusek,Dec. 2000
    //    p. 98
    //    Header numbers (This is part of table in page 98)
    //    Nat1 Nmol1 Nat21 Nmol2 Nat32 Nmol3 Itemp Irot
    //    Natm1 Npos1
    //    Natm2 Npos2 Nmol1 lines for the 1st composite subsystem
    //    ......
    //    The header of m40 contains number of atoms in atomic and molecular parts, number
    //    of molecules and molecular positions. In the case of a composite these numbers are
    //    listed repeatedly for each composite part.
    //    The number of composite parts is given in m50 (see the key ncomp, Table 9, page 80)
    //    and can be defined with PRELIM user interface (see § 2.2.2, page 68). The numbers
    //    for non-existing composite parts are omitted.
    //    Meaning of parameters
    //    Nat1 Number of atoms in the 1st composite part.
    //    Nmol1 Number of molecules3 in the 1st composite part
    //    Nat2 Number of atoms in the 2nd composite part.
    //    Nmol2 Number of molecules in the 2nd composite part
    //    Nat3 Number of atoms in the 3rd composite part.
    //    Nmol3 Number of molecules in the 3rd composite part
    //    Itemp Type of temperature parameters (0 for U, 1 for beta)
    //    Irot Key of molecular rotation (0 for Eulerian, 1 for axial). See page 143
    //    for more information.
    //    Natm1 Number of atoms in the 1st molecule of the 1st composite part
    //    Npos1 Number of positions of the 1st molecule of the 1st composite part
    //    Natm2 Number of atoms in the 2nd molecule of the 1st composite part
    //    Npos2 Number of positions of the 2nd molecule of the 1st composite part

    // except Jana2006 may have changed this:

    int nFree = 0, nGroups = 0;
    boolean isAxial = false;
    BS newSub = (thisSub == 0 ? null : new BS());
    int iSub = (thisSub == 0 ? 1 : thisSub);
    for (int i = 0, n = 0, pt = 0; i < iSub; i++, pt += 10) {
      nFree = getInt(pt, pt + 5);
      nGroups = getInt(pt + 5, pt + 10);
      isAxial = (getInt(pt + 15, pt + 20) == 1);
      if (nGroups != 0 && i > 0) {
        throw new Exception(
            "Jmol cannot read rigid body M40 files for composites");
      }
      if (newSub != null)
        newSub.set(n = n + nFree);
    }
    iSub = (newSub == null ? 0 : 1);
    int nAtoms = -1;
    String refAtomName = null;
    rho = null;
    if (nGroups > 0) {
      Logger.info("JanaReader found " + nFree + " free atoms and " + nGroups
          + " groups");
      molName = null;
      molAtoms = new Lst<Atom>();
      molTtypes = new Lst<Integer>();
    }

    // note that we are skipping scale, overall isotropic temperature factor, and extinction parameters

    while (skipToNextAtom() != null) {
      nAtoms++;
      Atom atom = new Atom();
      Logger.info(line);
      String name = line.substring(0, 9).trim();
      atom.atomName = name;
      boolean isRefAtom = name.equals(refAtomName);
      atom.foccupancy = floats[2];
      boolean isJanaMolecule = Float.isNaN(atom.foccupancy);
      if (isJanaMolecule) {
        // new "molecule" group
        //refType = getInt(10, 11);
        // IR The type of the reference point 
        // (0=explicit, 1=gravity centre, 2=geometry centre)
        String pointGroup = getStr(12, 18);

        // see http://en.wikipedia.org/wiki/Crystallographic_point_group
        if (pointGroup.length() > 0 && !pointGroup.equals("1")) {
          throw new Exception(
              "Jmol cannot process M40 files with molecule positions based on point-group symmetry.");
        }
        refAtomName = null;
        if (Float.isNaN(floats[4]))
          refAtomName = getStr(28, 37);
        else
          rho = P3.new3(floats[3], floats[4], floats[5]);
        molName = name;
        molAtoms.clear();
        molTtypes.clear();
        molHasTLS = false;
        firstPosition = true;
        modelMolecule = new Lst<P3>();
        continue;
      }
      boolean isExcluded = false;
      String posName = (name.startsWith("pos#") ? name : null);
      if (posName == null) {
        if (!filterAtom(atom, 0)) {
          if (!isRefAtom)
            continue;
          isExcluded = true;
        }
        setAtomCoordXYZ(atom, floats[3], floats[4], floats[5]);
        if (isRefAtom) {
          rho = P3.newP(atom);
          if (isExcluded)
            continue;
        }
        asc.addAtom(atom);
        if (iSub > 0) {
          if (newSub.get(nAtoms))
            iSub++;
          atom.altLoc = ("" + iSub).charAt(0);
        }
        readAtomRecord(atom, null, null, false);
        if (molAtoms != null)
          molAtoms.addLast(atom);
      } else {
        if (molAtoms.size() == 0)
          continue;
        processPosition(posName, atom, isAxial);
      }
    }
  }


  /**
   * safe int parsing of line.substring(col1, col2);
   * 
   * @param col1
   * @param col2
   * @return value or 0
   */
  private int getInt(int col1, int col2) {
    int n = line.length();
    return (n > col1 ? parseIntStr(getStr(col1, col2)) : 0);
  }
  
  /**
   * safe string parsing of line.substring(col1, col2);
   * 
   * @param col1
   * @param col2
   * @return value or ""
   */
  private String getStr(int col1, int col2) {
    int n = line.length();
    return (n > col1 ? line.substring(col1, Math.min(n, col2)).trim(): "");
  }

  /**
   * safely get a one-character 0 or 1 as a boolean
   * 
   * @param i
   * @return   true if it was a 1
   */
  private boolean getFlag(int i) {
    return (getInt(i, i + 1) > 0);
  }
  private String skipToNextAtom() throws Exception {
    while (readM40Floats() != null
        && (line.length() == 0 || line.charAt(0) == ' ' || line.charAt(0) == '-')) {
    }
    return line;
  }

  public final static String U_LIST = "U11U22U33U12U13U23UISO";
  
  
  private void readM40WaveVectors() throws Exception {
    while (!readM40Floats().contains("end"))
      if (line.startsWith("wave")) {
        String[] tokens = getTokens();
        double[] pt = new double[modDim];
        for (int i = 0; i < modDim; i++)
          pt[i] = parseFloatStr(tokens[i + 2]);
        ms.addModulation(null, "F_" + parseIntStr(tokens[1]) + "_coefs_", pt, -1);
      }
    readM40Floats();
  }

  //////////////// JANA "molecule" business //////////////
  
  /**
   * We process the Pos#n record here. This involves cloning the free atoms,
   * translating and rotating them to the proper locations, and copying the
   * modulations. Jmol uses the alternative location PDB option (%1, %2,...) to
   * specify the group, enabling the Jmol command DISPLAY configuration=1, for
   * example. We also set a flag to prevent autobonding between alt-loc sets.
   * This is not perfect, as in some cases "pos#2" would be better than "pos#1"
   * in terms of bonding.
   * 
   * At this point we only support systType=1 (basic coordinates)
   * 
   * @param posName
   * @param pos
   * @param isAxial
   * @throws Exception
   */
  private void processPosition( String posName, Atom pos,
                               boolean isAxial) throws Exception {

    // read the first pos# line.

    pos.atomName = molName + "_" + posName;
    
    boolean isImproper = (getInt(9, 11) == -1); // "sign" of rotation
    int systType = getInt(13, 14);
    P3 rm = (systType == 0 ? null : new P3());
    P3 rp = (systType == 0 ? null : new P3());

    // Type of the local coordinate system. 
    // 0 if the basic crystallographic setting is used. 
    // 1 if the local system for the model molecule is defined 
    //   explicitly
    // 2 if an explicit choice is used also for the actual position.  
    if (systType != 0) {
      throw new Exception(
          "Jmol can only read rigid body groups with basic crystallographic settings.");
    }

    // read the modulation --  phi, chi, psi, and vTrans will be stored in atom.anisoBorU
    
    float[][] rotData = readAtomRecord(pos, rm, rp, true);

    String name = pos.atomName;
    int n = molAtoms.size();
    Logger.info(name + " Molecular group " + molName + " has " + n + " atoms");
    String ext = "_" + posName.substring(4);

    // set the molecular modulation reference point offset from the model reference point
    
    V3 vTrans = V3.new3(pos.anisoBorU[3], pos.anisoBorU[4], pos.anisoBorU[5]);
    
    // calculate the rotation, either Euler ZYZ or Cartesian XYZ
    
    //  isAxial: X Y Z (X first)
    // notAxial: Z X Z
    Quat phi = Quat.newAA(A4.newVA(V3.new3(0, 0, 1),
        (float) (pos.anisoBorU[0] / 180 * Math.PI)));
    Quat chi = Quat.newAA(A4.newVA(
        isAxial ? V3.new3(0, 1, 0) : V3.new3(1, 0, 0),
        (float) (pos.anisoBorU[1] / 180 * Math.PI)));
    Quat psi = Quat.newAA(A4.newVA(
        isAxial ? V3.new3(1, 0, 0) : V3.new3(0, 0, 1),
        (float) (pos.anisoBorU[2] / 180 * Math.PI)));
    matR = phi.mulQ(chi).mulQ(psi).getMatrix();
    if (isImproper)
      matR.scale(-1);

    // We will generate a script that will define the model name as an atom selection.

    String script = "";
      
    // process atoms in model molecule:

    for (int i = 0; i < n; i++) {
      Atom a = molAtoms.get(i);
      String newName = a.atomName;
      script += ", " + newName;
      if (firstPosition) {
        newName += ext;
        modelMolecule.addLast(P3.newP(a));
      } else {
        a = asc.newCloneAtom(a);
        newName = newName.substring(0, newName.lastIndexOf("_")) + ext;
      }
      a.atomName = newName;
      
      // The molecular atom position is calculated as
      // 
      //  a' = g + vR
      //
      // where g is the local modulation reference point
      // (for which the pos# record's modulations are given in the M40 file): 
      //
      //  g = rho + vTrans
      // 
      // and vR is the offset of the atom from this point,
      // which is calculated by rotating the Cartesian offset of
      // the model atom from the model reference point:
      //
      //  vR = toFractional[R(toCartesian[v0])]
      //
      //  where v0 = a - rho
      // 
      // (vR will be used only in setRigidBodyPhase) 
      
      V3 v0 = V3.newVsub(modelMolecule.get(i), rho);
      getSymmetry().toCartesian(v0Cart = V3.newV(v0), true);
      vR = V3.newV(v0); 
      cartesianProduct(vR, null);
      
      a.setT(rho);
      a.add(vTrans);
      a.add(vR);
      
      // copy and process all modulations
      
      copyModulations(";" + pos.atomName, ";" + newName);
      if (rotData != null)
        setRigidBodyRotations(";" + newName, rotData);
    }
    firstPosition = false;
    script = "@" + molName + ext + script.substring(1);
    addJmolScript(script);
    appendLoadNote(script);
  }

  /**
   * dual-purpose function for cross products,
   * proper rotations, and improper rotations
   * 
   * @param vA
   * @param vB
   */
  private void cartesianProduct(T3 vA, T3 vB) {
    symmetry.toCartesian(vA, true);
    if (vB == null) // proper or improper rotation
      matR.rotate2(vA, vA);
    else  //cross these two
      vA.cross(vA, vB);
    symmetry.toFractional(vA, true);
  }

  private static String[] XYZ = {"x", "y", "z"};
 
  /**
   * Read the atom or pos# record, including occupancy, various flags, and,
   * especially, modulations.
   * 
   * Not implemented: TLS, space groups, and local position rotation axes.
   * 
   * @param atom
   * @param rm
   *        // rotation vector/point not implemented
   * @param rp
   *        // rotation point not implemented
   * @param isPos
   * @return pos# record's rotational displacement data
   * 
   * @throws Exception
   */
  private float[][] readAtomRecord(Atom atom, P3 rm, P3 rp, boolean isPos)
      throws Exception {
    String label = ";" + atom.atomName;
    int tType = (isPos ? -1 : getInt(13, 14));
    if (!isPos && molTtypes != null)
      molTtypes.addLast(Integer.valueOf(tType));
    boolean haveSpecialOcc = getFlag(60);
    boolean haveSpecialDisp = getFlag(61);
    boolean haveSpecialUij = getFlag(62);
    int nOcc = getInt(65, 68); // could be -1
    int nDisp = getInt(68, 71);
    int nUij = getInt(71, 74);
    if (rm != null) {
      readM40Floats();
      rm.set(floats[0], floats[1], floats[2]);
      rp.set(floats[3], floats[4], floats[5]);
    }
    if (tType > 2)
      readM40Floats();
    // read anisotropies (or Pos#n rotation/translations)
    readM40Floats();
    switch (tType) {
    case 6:
    case 5:
    case 4:
    case 3:
      readLines(tType - 1);
      appendLoadNote("Skipping temperature factors with order > 2");
      //$FALL-THROUGH$
    case 2:
    case -1:
      for (int j = 0; j < 6; j++)
        asc.setU(atom, j, floats[j]);
      break;
    case 1:
      if (floats[0] != 0)
        asc.setU(atom, 7, floats[0]);
      break;
    case 0:
      molHasTLS = true;
      appendLoadNote("Jmol cannot process molecular TLS parameters");
      break;
    }
    if (modDim == 0)
      return null; // return for unmodulated Pos#n

    if (isPos && molHasTLS)
      readLines(4);

    // read occupancy modulation

    double[] pt;
    float o_0 = (nOcc > 0 && !haveSpecialOcc ? parseFloatStr(rd()) : 1);

    // We add a J_O record that saves the original (unadjusted) o_0 and o_site
    //
    //  O = o_site (o_0 + SUM)
    //
    // However, first we need to adjust o_0 because the value given in m40 is 
    // divided by the number of operators giving this site.

    if (o_0 != 1)
      ms.addModulation(null, "J_O#0" + label, new double[] { atom.foccupancy,
          o_0, 0 }, -1);
    atom.foccupancy *= o_0;
    int wv = 0;
    float a1, a2;
    isLegendre = false;
    for (int j = 0; j < nOcc; j++) {
      if (haveSpecialOcc) {
        float[][] data = readM40FloatLines(2, 1);
        a2 = data[0][0]; // width (first line)
        a1 = data[1][0]; // center (second line)
      } else {
        wv = j + 1;
        readM40Floats();
        a1 = floats[0]; // sin (first on line)
        a2 = floats[1]; // cos (second on line)
      }
      pt = new double[] { a1, a2, 0 };
      if (a1 != 0 || a2 != 0)
        ms.addModulation(null, "O_" + wv + "#0" + label, pt, -1);
    }

    // read displacement modulation

    for (int j = 0; j < nDisp; j++) {
      if (haveSpecialDisp) {
        readM40Floats();
        float c = floats[3]; // center
        float w = floats[4]; // width
        for (int k = 0; k < 3; k++)
          if (floats[k] != 0)
            ms.addModulation(null, "D_S#" + XYZ[k] + label, new double[] { c,
                w, floats[k] }, -1);
      } else {
        // Fourier or Legendre displacements
        addSinCos(j, "D_", label, isPos);
      }
    }

    // collect rotational displacive parameters

    float[][] rotData = (isPos && nDisp > 0 ? readM40FloatLines(nDisp, 6)
        : null);

    // finally read Uij sines and cosines

    if (!isPos) { // No TLS here
      if (isLegendre)
        nUij *= 2;
      for (int j = 0; j < nUij; j++) {
        if (tType == 1) {
          // Fourier displacements
          addSinCos(j, "U_", label, false);
        } else {
          if (haveSpecialUij) {
            //TODO
            Logger.error("JanaReader -- not interpreting SpecialUij flag: "
                + line);
          } else if (isLegendre) {
            float[][] data = readM40FloatLines(1, 6);
            int order = j + 1;
            double coeff = 0;
            for (int k = 0, p = 0; k < 6; k++, p += 3) {
              if ((coeff = data[0][k]) != 0)
                ms.addModulation(null,
                    "U_L" + order + "#" + U_LIST.substring(p, p + 3) + label,
                    new double[] { coeff, order, 0 }, -1);
            }
          } else {
            float[][] data = readM40FloatLines(2, 6);
            for (int k = 0, p = 0; k < 6; k++, p += 3) {
              double csin = data[1][k];
              double ccos = data[0][k];
              ms.addModulation(null,
                  "U_" + (j + 1) + "#" + U_LIST.substring(p, p + 3) + label,
                  new double[] { csin, ccos, 0 }, -1);
            }
          }
        }
      }
    }
    // higher order temperature factor modulation ignored

    // phason ignored
    return rotData;
  }

  /**
   * Add x, y, and z modulations as [ csin, ccos, 0 ] or, possibly Legendre [
   * coef, order, 0 ]
   * 
   * @param j
   * @param key
   * @param label
   * @param isPos
   * @throws Exception
   */
  private void addSinCos(int j, String key, String label, boolean isPos)
      throws Exception {
    readM40Floats();
    if (isLegendre) {
      for (int i = 0; i < 2; i++) {
        int order = (j * 2 + i + 1);
        for (int k = 0; k < 3; ++k) {
          float coeff = floats[3 * i + k];
          if (coeff == 0) {
            continue;
          }
          String axis = XYZ[k % 3];
          if (modAxes != null && modAxes.indexOf(axis.toUpperCase()) < 0)
            continue;
          String id = key + "L#" + axis + order + label;
          ms.addModulation(null, id, new double[] { coeff, order, 0 }, -1);
        }
      }
      return;
    }
    ensureFourier(j);
    for (int k = 0; k < 3; ++k) {
      float csin = floats[k];
      float ccos = floats[k + 3];
      if (csin == 0 && ccos == 0) {
        if (!isPos)
          continue;
        csin = 1e-10f;
      }
      String axis = XYZ[k % 3];
      if (modAxes != null && modAxes.indexOf(axis.toUpperCase()) < 0)
        continue;
      String id = key + (j + 1) + "#" + axis + label;
      ms.addModulation(null, id, new double[] { csin, ccos, 0 }, -1);
    }
  }

  /**
   * Make sure that F_n record is present.
   * 
   * @param j
   */
  private void ensureFourier(int j) {
    double[] pt;
    if (j > 0 && ms.getMod("F_" + (++j) + "_coefs_") == null && (pt = ms.getMod("F_1_coefs_")) != null) {
      double[] p = new double[modDim];
      for (int i = modDim; --i >= 0;)
        p[i] = pt[i] * j;
      ms.addModulation(null, "F_" + j + "_coefs_", p, -1);
    }
  }

  private float[] floats = new float[6];
  
  private String readM40Floats() throws Exception {
    if ((line = rd()) == null || line.indexOf("-------") >= 0) 
      return (line = null);
    if (debugging)
      Logger.debug(line);
    parseM40Floats();
    return line;
  }

  private void parseM40Floats() {
    int ptLast = line.length() - 9;
    for (int i = 0, pt = 0; i < 6; i++, pt += 9) {
      floats[i] = (pt <= ptLast ? parseFloatStr(line.substring(pt, pt + 9)) : Float.NaN);
    }
  }

  private float[][] readM40FloatLines(int nLines, int nFloats) throws Exception {
    float[][] data = new float[nLines][nFloats];
    for (int i = 0; i < nLines; i++) {
      readM40Floats();
      if (line.indexOf("Legendre") == 19)
        isLegendre = true;
      for (int j = 0; j < nFloats; j++)
        data[i][j] = floats[j];
    }
    return data;
  }

  /**
   * M40 occupancies are divided by the site multiplicity; 
   * here we factor that back in.
   * 
   */
  private void adjustM40Occupancies() {
    Map<String, Integer> htSiteMult = new Hashtable<String, Integer>();    
    Atom[] atoms = asc.atoms;
    SymmetryInterface symmetry = asc.getSymmetry();
    for (int i = asc.ac; --i >= 0;) {
      Atom a = atoms[i];
      Integer ii = htSiteMult.get(a.atomName);
      if (ii == null)
        htSiteMult.put(a.atomName, ii = Integer.valueOf(symmetry.getSiteMultiplicity(a)));
      a.foccupancy *= ii.intValue();
    }
  }

  /**
   * Create a new catalog entry for an atom's modulation components.
   * Just occupation and displacement here.
   * 
   * @param label
   * @param newLabel
   */
  private void copyModulations(String label, String newLabel) {
    Map<String, double[]> mapTemp = new Hashtable<String, double[]>();
    for (Entry<String, double[]> e : ms.getModulationMap().entrySet()) {
      String key = e.getKey();
      if (!key.contains(label))
        continue;
      key = PT.rep(key, label, newLabel);
      double[] val = e.getValue();
      switch (key.charAt(0)) {
      case 'O':
        setRigidBodyPhase(key, val = new double[] {val[0], val[1], 0});
        break;
      case 'D':
        // we will phase these at the time of rotation, in setModRot
        break;
      case 'U':
        // not implemented
        continue;
      }
      mapTemp.put(key, val);
    }

    for (Entry<String, double[]> e : mapTemp.entrySet())
      ms.addModulation(null, e.getKey(), e.getValue(), -1);
  }

  /**
   * 
   * Adjust phases to match the difference between the atom's position and the
   * rigid molecular fragment's reference point. We have:
   * 
   *   a' = g + vR
   * 
   * Here we want just the local rotational part, vR
   * 
   * 
   * @param key 
   * @param v 
   * @return phase-adjusted parameters
   * 
   */
  private double[] setRigidBodyPhase(String key, double[] v) {
    boolean isCenter = false;
    
    // really only OCC and DISP processed here.
    
    switch (ms.getModType(key)) {
    case Modulation.TYPE_OCC_FOURIER:
    case Modulation.TYPE_DISP_FOURIER:
    case Modulation.TYPE_U_FOURIER:
      break;
    case Modulation.TYPE_OCC_CRENEL:
    case Modulation.TYPE_DISP_SAWTOOTH:
      isCenter = true;
      break;
    }
    
    // calculate the overall sum of all wave vector products
    // including here the Fourier number "n" (untested)
    
    double nqDotD = 0;
    double n = -1;
    double[] qcoefs = ms.getQCoefs(key);
    for (int i = modDim; --i >= 0;) {
      if (qcoefs[i] != 0) {
        n = qcoefs[i];
        // n in sin(2 pi n q.vR),
        double[] q = ms.getMod("W_" + (i + 1));
        nqDotD = n * (q[0] * vR.x + q[1] * vR.y + q[2] * vR.z);
        break;
      }
    }
    if (isCenter) {
      // untested -- shift center
      v[0] += nqDotD; // move center of sawtooth or crenel to match this atom
    } else {
      // apply sin(a + x) = sin(a)cos(x) + cos(a)sin(x)
      //       cos(a + x) = cos(a)cos(x) - sin(a)sin(x) 
      double sA = v[0]; // A sin...
      double cA = v[1]; // B cos....
      double sX = Math.sin(2 * Math.PI * nqDotD);
      double cX = Math.cos(2 * Math.PI * nqDotD);
      v[0] = sA * cX + cA * sX;   // A sin
      v[1] = -sA * sX + cA * cX;  // B cos
    }
    return v;
  }

  /**
   * Transform unphased Fourier x,y,z cos/sin coefficients in a rigid body
   * system based on distance from center. We have:
   * 
   * a' = g + vR = g + R(v0)
   * 
   * Here we need just the original atom offset from the reference point, v0, 
   * as a Cartesian vector.
   * 
   * @param label
   *        ";atomName"
   * @param params
   *        block of [nDisp][6] rotational parameters
   * 
   */
  private void setRigidBodyRotations(String label, float[][] params) {

    // process each contribution as a separate set of x y z modulations

    int n = params.length;
    for (int i = 0; i < n; i++) {
      ensureFourier(i);
      String key = "D_" + (i + 1);
      float[] data = params[i];
      V3 vsin = V3.new3(data[0], data[1], data[2]);
      V3 vcos = V3.new3(data[3], data[4], data[5]);

      // sines and cosines vectors into cartesians, 
      // cross with rotation vector, and back to fractional

      cartesianProduct(vcos, v0Cart);
      cartesianProduct(vsin, v0Cart);

      // add in already-cataloged raw displacement component

      String keyx = key + "#x" + label;
      String keyy = key + "#y" + label;
      String keyz = key + "#z" + label;
      double[] vx = combineModulation(keyx, vsin.x, vcos.x);
      double[] vy = combineModulation(keyy, vsin.y, vcos.y);
      double[] vz = combineModulation(keyz, vsin.z, vcos.z);

      // set back into T3 vector mode for processing

      vsin.set((float) vx[0], (float) vy[0], (float) vz[0]);
      vcos.set((float) vx[1], (float) vy[1], (float) vz[1]);

      // now take combined translation and rotation
      // to Cartesian, rotate by matR, then back to fractional

      cartesianProduct(vsin, null);
      cartesianProduct(vcos, null);

      // save the displacement modulation sines and cosines again

      setMolecularModulation(keyx, vsin.x, vcos.x);
      setMolecularModulation(keyy, vsin.y, vcos.y);
      setMolecularModulation(keyz, vsin.z, vcos.z);

    }
  }

  /**
   * Retrieve cataloged displacement and add in this component,
   * returning a new double[3].
   * 
   * @param key
   * @param csin
   * @param ccos
   * @return new array
   */
  private double[] combineModulation(String key, float csin, float ccos) {
    double[] v = ms.getMod(key);
    return new double[] {v[0] + csin,  v[1] + ccos, 0};
  }

  /**
   * Add the modulation after applying rigid-body phase correction
   * 
   * @param key
   * @param csin
   * @param ccos
   */
  private void setMolecularModulation(String key, float csin, float ccos) {
    ms.addModulation(null, key, setRigidBodyPhase(key, new double[] {csin, ccos, 0}), -1);
  }

}
