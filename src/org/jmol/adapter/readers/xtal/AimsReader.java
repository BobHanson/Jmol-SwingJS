/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Joerg Meyer, FHI Berlin
 *
 * Contact: meyer@fhi-berlin.mpg.de
 *
 * Copyright (C) 2011  Joerg Meyer, TU Muenchen 
 *
 * Contact: joerg.meyer@ch.tum.de
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
 *
 */

package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.Logger;

/**
 * FHI-aims (http://www.fhi-berlin.mpg.de/aims) geometry.in file format
 *
 * samples of relevant lines in geometry.in file are included as comments below
 * 
 * modified (May 1, 2011, hansonr@stolaf.edu) to account for atom/atom_frac lines
 * and to bring it into compliance with other load options (such as overriding
 * file-based symmetry or unit cell parameters).
 *
 * @author Joerg Meyer, TU Muenchen 2011 (joerg.meyer@ch.tum.de)
 * @version 1.3
 * 
 */

public class AimsReader extends AtomSetCollectionReader {

  private boolean globalDoApplySymmetry;
  private boolean isFractional;
  
  @Override
  protected void initializeReader() {
    globalDoApplySymmetry = doApplySymmetry;
    doApplySymmetry = true;
    isFractional = true;
  }
  
  @Override
  protected boolean checkLine() {
    String[] tokens = getTokens();
    if (tokens.length == 0)
      return true;    
    if (tokens[0].equals("lattice_vector")) {
      readLatticeVector(tokens);
      return true;
    }    
    if (tokens[0].equals("atom")) {
      readAtom(tokens, false);
      return true;
    }
    if (tokens[0].equals("atom_frac")) {
      readAtom(tokens, true);
      return true;
    }
    if (tokens[0].equals("multipole")) {
      readMultipole(tokens);
      return true;
    }
    return true;
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    doApplySymmetry = globalDoApplySymmetry;
    if (nLatticeVectors == 1 || nLatticeVectors == 2) {
      Logger.warn("ignoring translation symmetry for more or less than 3 dimensions"
                + "(which is currently neither supported by FHI-aims");
      // note: Jmol DOES support both polymer and slab symmetry.
    }
    finalizeReaderASCR();
  }

  /*
   * lattice_vector 16.503872273 0.000000000 0.000000000
   */
  
  private int nLatticeVectors;
  
  private void readLatticeVector(String[] tokens) {
    if (tokens.length < 4) {
      Logger.warn("cannot read line with FHI-aims lattice vector: " + line);
    } else if (nLatticeVectors == 3) {
      Logger.warn("more than 3 FHI-aims lattice vectors found with line: "
          + line);
    } else {
      addExplicitLatticeVector(nLatticeVectors++, new float[] {parseFloatStr(tokens[1]), parseFloatStr(tokens[2]), parseFloatStr(tokens[3])}, 0);
      setFractionalCoordinates(nLatticeVectors == 3);
    }
  }

  /*
   * absolute and fractional coordinates are "treated on equal footing" in FHI-aims,
   * i.e. "atom" and "atom_frac" lines are allowed to appear together in the same geometry.in file
   * 
   * atom 2.750645380 2.750645380 25.000000000 Pd
   * atom_frac 0.25 0.25 0.5 Pd
   */
  
  private void readAtom(String[] tokens, boolean isFractional) {
    if (tokens.length < 5) {
      Logger.warn("cannot read line with FHI-aims line: " + line);
      return;
    }
    if (this.isFractional != isFractional)
      setFractionalCoordinates(this.isFractional = isFractional);
    addAtomXYZSymName(tokens, 1, tokens[4], null); 
  }

  /*
   * multipole 2.750645380 2.750645380 25.000000000 0 46.0
   */
  private void readMultipole(String[] tokens) {
    if (tokens.length < 6) {
      Logger.warn("cannot read line with FHI-aims atom data: " + line);
      return;
    }
    int order = parseIntStr(tokens[4]);
    if (order > 0) {
      Logger
          .warn("multipole line ignored since only monopoles are currently supported: "
              + line);
      return;
    }
    if (this.isFractional)
      setFractionalCoordinates(this.isFractional = false);
    addAtomXYZSymName(tokens, 1, null, null).partialCharge = parseFloatStr(tokens[5]);
    // we generally do not do this: atom.formalCharge = Math.round(atom.partialCharge);
  }

}

