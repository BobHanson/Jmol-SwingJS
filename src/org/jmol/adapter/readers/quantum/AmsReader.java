/* $RCSfile: ADFReader.java,v $
 * $Author: egonw $
 * $Date: 2004/02/23 08:52:55 $
 * $Revision: 1.3.2.4 $
 * Copyright (C) 2002-2024  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.adapter.readers.quantum;

/**
 * 
 * A reader for AMS output subclassing the older AdfReader.
 * 
 * Amsterdam Modeling Suite (AMS) is a quantum chemistry program
 * by Scientific Computing & Modelling NV (SCM)
 * (http://www.scm.com/), superseding previous ADF
 *
 * The reader was directly adapted from the code of the ADF reader
 * authored by Bradley A. Smith, to make it work on the new structure
 * of AMS output files.
 * 
 * Added note by Bob Hanson 9/12/2023
 *    Most of this patch was duplicate code in AdfReader.
 *    This code was consolidated by subclassing.
 *    It has not been tested.
 *
 * @author Diego Garay-Ruiz (dgaray@iciq.es)
 * @version 1.0
 */
public class AmsReader extends AdfReader {

  @Override
  public void initializeReader() {
    // a flag for various new checks
    isADF = false;
  }
}
