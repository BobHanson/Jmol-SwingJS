/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

/**
 * Interface used for the logging mechanism.
 */
public interface LoggerInterface {

  /**
   * Writes a log at DEBUG level.
   * 
   * @param txt String to write.
   */
  public void debug(String txt);

  /**
   * Writes a log at INFO level.
   * 
   * @param txt String to write.
   */
  public void info(String txt);

  /**
   * Writes a log at WARN level.
   * 
   * @param txt String to write.
   */
  public void warn(String txt);

  /**
   * Writes a log at WARN level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public void warnEx(String txt, Throwable e);

  /**
   * Writes a log at ERROR level.
   * 
   * @param txt String to write.
   */
  public void error(String txt);

  /**
   * Writes a log at ERROR level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public void errorEx(String txt, Throwable e);

  /**
   * Writes a log at FATAL level.
   * 
   * @param txt String to write.
   */
  public void fatal(String txt);

  /**
   * Writes a log at ERROR level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public void fatalEx(String txt, Throwable e);
}
