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

import org.jmol.c.STR;
import javajs.util.BS;

public abstract class JmolAdapterStructureIterator {
	public abstract boolean hasNext();

	public abstract STR getStructureType();

	public abstract STR getSubstructureType();

	public abstract String getStructureID();

	public abstract int getSerialID();

	public abstract int getStrandCount();

	public abstract int getStartChainID();

	public abstract int getStartSequenceNumber();

	public abstract char getStartInsertionCode();

	public abstract int getEndChainID();

	public abstract int getEndSequenceNumber();

	public abstract char getEndInsertionCode();

  public abstract BS getStructuredModels();

  public abstract int[] getAtomIndices();

  public abstract int[] getModelIndices();

  public abstract BS[] getBSAll();
}
