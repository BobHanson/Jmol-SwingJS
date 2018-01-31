/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-16 01:43:57 -0600 (Thu, 16 Nov 2006) $
 * $Revision: 6225 $
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

package org.jmol.renderbio;

import org.jmol.shapebio.BioShape;


public class RibbonsRenderer extends MeshRibbonRenderer {

  @Override
  protected void renderBioShape(BioShape bioShape) {
    if (wingVectors == null)
      return;
    if (wireframeOnly)
      renderStrands();
    else
      render2Strand(true, isNucleic ? 1f : 0.5f, isNucleic ? 0f : 0.5f);
  }
}
