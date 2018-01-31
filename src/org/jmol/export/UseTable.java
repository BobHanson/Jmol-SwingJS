/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

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

package org.jmol.export;

import java.util.Hashtable;



class UseTable extends Hashtable<String, String> {
  private int iObj;
  private String keyword;
  private char term = '\0';

  UseTable(String keyword) {
    this.keyword = keyword;
    term = keyword.charAt(keyword.length() - 1);
  }
  
  /**
   * This Hashtable contains references to _n where n is a number. 
   * we look up a key for anything and see if an object has been assigned.
   * If it is there, we just return the phrase "USE _n".
   * If it is not there, we return the DEF name that needs to be assigned.
   * The calling method must then make that definition.
   * 
   * @param key
   * @return "_n" or "[keyword]_n"
   */

  String getDef(String key) {
    if (containsKey(key))
      return keyword + get(key) + term;
    String id = "_" + (iObj++);
    put(key, id);
    return id;
  }
  
  /**
   * Used by JSExporter for WebGL
   * 
   * @param key
   * @param ret 
   * @return found
   */

  boolean getDefRet(String key, String[] ret) {
    if ((ret[0] = get(key)) != null)
      return true;
    put(key, ret[0] = "_" + key.charAt(0) + (iObj++));
    return false;
  }
    
}
