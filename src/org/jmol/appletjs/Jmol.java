/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-09-11 19:29:26 -0500 (Tue, 11 Sep 2012) $
 * $Revision: 17556 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.appletjs;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.GenericApplet;

/**
 * Java2Script rendition of Jmol using HTML5-only or WebGL-based graphics
 * 
 * @author Bob Hanson hansonr@stolaf.edu, Takanori Nakane (WebGL), with the assistance  
 *         of Jhou Renjian (SwingJS)
 *         
 * This version of Jmol predates SwingJS and is not compatible with general 
 * SwingJS applications. See org.jmol.applet.Jmol for the SwingJS version.
 * 
 * 
 */

public class Jmol extends GenericApplet  {

  public Jmol(Map<String, Object> vwrOptions) {
    htParams = new Hashtable<String, Object>();
    if (vwrOptions == null)
      vwrOptions = new Hashtable<String, Object>();
    this.vwrOptions = vwrOptions;
    for (Map.Entry<String, Object> entry : vwrOptions.entrySet())
      htParams.put(entry.getKey().toLowerCase(), entry.getValue());
    documentBase = "" + vwrOptions.get("documentBase");
    codeBase = "" + vwrOptions.get("codePath");
    isJS = true;
    init(this);
  }
  
}
