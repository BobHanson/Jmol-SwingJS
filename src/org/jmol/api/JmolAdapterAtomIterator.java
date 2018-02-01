/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-06-10 13:54:48 -0500 (Sun, 10 Jun 2012) $
 * $Revision: 17269 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.api;


import javajs.util.BS;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.V3;

public interface JmolAdapterAtomIterator {
  
	public abstract boolean hasNext();

	abstract public int getAtomSetIndex();

	abstract public BS getSymmetry();

	abstract public int getAtomSite();

	abstract public Object getUniqueID();

	abstract public int getElementNumber(); // may be atomicNumber + isotopeNumber*128

	abstract public String getAtomName();

	abstract public int getFormalCharge();

	abstract public float getPartialCharge();

	abstract public Lst<Object> getTensors();

	abstract public float getRadius();

  abstract public V3 getVib();

	abstract public P3 getXYZ();

  abstract public float getBfactor();
	
	abstract public float getOccupancy();

	abstract public boolean getIsHetero();

	abstract public int getSerial();

	abstract public int getChainID();

	abstract public char getAltLoc();
	
	abstract public String getGroup3();

	abstract public int getSequenceNumber();

	abstract public char getInsertionCode();

  abstract public int getSeqID();

}
