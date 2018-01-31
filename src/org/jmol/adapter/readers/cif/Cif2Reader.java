/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
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
package org.jmol.adapter.readers.cif;

import javajs.api.GenericCifDataParser;


/**
 * 
 * Preliminary Cif2 reader. Just a shell. 
 * 
 * See http://journals.iucr.org/j/issues/2016/01/00/aj5269/index.html
 * 
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class Cif2Reader extends CifReader {
  
  @Override
  protected GenericCifDataParser getCifDataParser() {
    return new Cif2DataParser().set(this, null, debugging);
  }


  
  
  
}




