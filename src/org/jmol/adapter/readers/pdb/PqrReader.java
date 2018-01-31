/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
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

package org.jmol.adapter.readers.pdb;

import org.jmol.adapter.readers.pdb.PdbReader;
import org.jmol.adapter.smarter.Atom;

/**
 * PQR file reader.
 *
Angel Herraez, 2009 04 19

free form added Bob Hanson hansonr@stolaf.edu 2012 06 25

filter "FREE"

PQR format is a format based on pdb, where the occupancy is replaced with the atomic
charge and the temperature (or B factor) is replaced with atomic radius (however, 
the column positions in many pqr files do not match those of pdb files). This gives 
the acronym: P for pdb, Q for charge, R for radius. Jmol interprets the charge values 
(property partialcharge) and the radii (property vanderwaals), and can hence use them 
e.g. in color atoms partialCharge and spacefill.

The PQR format has somewhat uncertain origins, but is used by several computational biology packages, including MEAD, AutoDock and APBS[1], for which it is the primary input format.

PQR format description[2] within APBS documentation. Note that APBS reads PQR loosely, based only on white space delimiters, but Jmol may be more strict about column positions.

PDB files can be converted to PQR by the PDB2PQR software[3], which adds missing hydrogen atoms and calculates the charge and radius parameters from a variety of force fields. 


1.- http://apbs.sourceforge.net/
2.- http://apbs.sourceforge.net/doc/user-guide/index.html#pqr-format
3.- http://pdb2pqr.sourceforge.net/
4.- http://cardon.wustl.edu/MediaWiki/index.php/PQR_format

 *  
 */

public class PqrReader extends PdbReader {

  
  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    if (gromacsWideFormat) {
      atom.partialCharge = parseFloatRange(line, 60, 68);
      atom.radius = fixRadius(parseFloatRange(line, 68, 76));
    } else {
      String[] tokens = getTokens();
      int pt = tokens.length - 2 - (line.length() > 75 ? 1 : 0);
      atom.partialCharge = parseFloatStr(tokens[pt++]);
      atom.radius = fixRadius(parseFloatStr(tokens[pt]));
    }
  }
}

