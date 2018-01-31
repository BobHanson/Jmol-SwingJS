/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-08-30 23:43:59 -0500 (Sun, 30 Aug 2015) $
 * $Revision: 20747 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

/**
 * Exception thrown for invalid SMILES String
 */
public class InvalidSmilesException extends Exception {

  private static String lastError;

  public static String getLastError() {
    return lastError;
  }
  
  public static void clear() {
    lastError = null;
  }
  
  @Override
  public String getMessage() {
    return lastError;
  }
  /**
   * Constructs a <code>InvalidSmilesException</code> with a detail message.
   * 
   * @param message The detail message.
   */
  public InvalidSmilesException(String message) {
    super(message);
    lastError = (message.startsWith("Jmol SMILES") ? message : "Jmol SMILES Exception: " +  message);
  }

}
