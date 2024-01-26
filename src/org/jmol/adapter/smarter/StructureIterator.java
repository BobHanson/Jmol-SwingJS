/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-08-09 08:31:30 -0500 (Thu, 09 Aug 2012) $
 * $Revision: 17434 $
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

package org.jmol.adapter.smarter;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterStructureIterator;
import org.jmol.c.STR;
import javajs.util.BS;

public class StructureIterator extends JmolAdapterStructureIterator {
  private int structureCount;
  private Structure[] structures;
  private Structure structure;
  private int istructure;
  private BS bsModelsDefined;
  
  /**
   * @param asc 
   */
  StructureIterator(AtomSetCollection asc) {
    structureCount = asc.structureCount;
    structures = asc.structures;
    istructure = 0;
    bsModelsDefined = asc.bsStructuredModels;
  }

  @Override
  public boolean hasNext() {
    if (istructure == structureCount)
      return false;
    structure = structures[istructure++];
    return true;
  }

  @Override
  public STR getStructureType() {
    return structure.structureType;
  }

  @Override
  public STR getSubstructureType() {
    return structure.substructureType;
  }

  @Override
  public String getStructureID() {
    return structure.structureID;
  }

  @Override
  public String getStrandID() {
    return structure.strandID;
  }

  @Override
  public int getStartChainID() {
    return structure.startChainID;
  }
  
  @Override
  public int getStartSequenceNumber() {
    return structure.startSequenceNumber;
  }
  
  @Override
  public char getStartInsertionCode() {
    return JmolAdapter.canonizeInsertionCode(structure.startInsertionCode);
  }
  
  @Override
  public int getEndChainID() {
    return structure.endChainID;
  }
  
  @Override
  public int getEndSequenceNumber() {
    return structure.endSequenceNumber;
  }
    
  @Override
  public char getEndInsertionCode() {
    return structure.endInsertionCode;
  }

  @Override
  public int getStrandCount() {
    return structure.strandCount;
  }

  @Override
  public BS getStructuredModels() {
    return bsModelsDefined;
  }
  
  @Override
  public int[] getAtomIndices() {
    return structure.atomStartEnd;
  }
    
  @Override
  public int[] getModelIndices() {
    return structure.modelStartEnd;
  }

  @Override
  public BS[] getBSAll() {
    return structure.bsAll;
  }

}
