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
 * Implementation of Logger for JUnit tests 
 */
public class JUnitLogger implements LoggerInterface {

  /**
   * Activate the JUnit logger.
   */
  public static void activateLogger() {
    Logger.setLogger(new JUnitLogger());
  }

  private static String information = null;
  
  /**
   * Add information to log in case of an error.
   * 
   * @param txt Information to log.
   */
  public static void setInformation(String txt) {
    information = txt;
  }
  
  /**
   * Private constructor, use activateLogger() instead. 
   */
  private JUnitLogger() {
    //
  }

  /**
   * Log errors.
   * 
   * @param txt Text to log
   * @param e Exception
   */
  private void logError(String txt, Throwable e) {
    System.err.println(
        "Error: " +
        ((information != null) ? ("[" + information + "] ") : "") +
        ((txt != null) ? (txt + " - ") : "") +
        ((e != null) ? e.getClass().getName() : "") +
        ((e != null) && (e.getMessage() != null) ? (" - " + e.getMessage()) : ""));
    if (e != null) {
      StackTraceElement[] elements = e.getStackTrace();
      if (elements != null) {
        for (int i = 0; i < elements.length; i++) {
          System.err.println(
              elements[i].getClassName() + " - " +
              elements[i].getLineNumber() + " - " +
              elements[i].getMethodName());
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#debug(java.lang.String)
   */
  @Override
  public void debug(String txt) {
    System.out.println(txt);
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#info(java.lang.String)
   */
  @Override
  public void info(String txt) {
    System.out.println(txt);
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#warn(java.lang.String)
   */
  @Override
  public void warn(String txt) {
    System.out.println(txt);
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#warn(java.lang.String, java.lang.Throwable)
   */
  @Override
  public void warnEx(String txt, Throwable e) {
    logError(txt, e);
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#error(java.lang.String)
   */
  @Override
  public void error(String txt) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#error(java.lang.String, java.lang.Throwable)
   */
  @Override
  public void errorEx(String txt, Throwable e) {
    logError(txt, e);
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#fatal(java.lang.String)
   */
  @Override
  public void fatal(String txt) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.util.LoggerInterface#fatal(java.lang.String, java.lang.Throwable)
   */
  @Override
  public void fatalEx(String txt, Throwable e) {
    logError(txt, e);
  }

}
