/* $Author: hansonr $
 * $Date: 2010-04-22 13:16:44 -0500 (Thu, 22 Apr 2010) $
 * $Revision: 12904 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.script;

import java.util.Hashtable;
import java.util.Map;

class ContextToken extends T {
  Map<String, SV> contextVariables;
  SV[] forVars;
  static ContextToken newContext(boolean isOpen) {
    ContextToken ct = (isOpen ? newCmd(T.push, "{") : newCmd(T.pop, "}"));
    ct.intValue = 0;
    return ct;
  }

  static ContextToken newCmd(int tok, Object value) {
    ContextToken ct = new ContextToken();
    ct.tok = tok;
    ct.value = value;
    return ct;
  }

  String name0 = null;
  void addName(String name) {
    if (contextVariables == null)
      contextVariables = new Hashtable<String, SV>();
    contextVariables.put(name, SV.newS("").setName(name));
  }
  
}
