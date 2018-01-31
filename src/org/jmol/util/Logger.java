/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

import java.util.Hashtable;
import java.util.Map;

import javajs.J2SIgnoreImport;


/**
 * Logger mechanism.
 */
@J2SIgnoreImport({Runtime.class})
public final class Logger {

  private Logger(){}
  
  private static LoggerInterface _logger = new DefaultLogger();

  public final static int LEVEL_FATAL = 1;
  public final static int LEVEL_ERROR = 2;
  public final static int LEVEL_WARN = 3;
  public final static int LEVEL_INFO = 4;
  public final static int LEVEL_DEBUG = 5;
  public final static int LEVEL_DEBUGHIGH = 6;
  public final static int LEVEL_MAX = 7;
  
  private final static boolean[] _activeLevels = new boolean[LEVEL_MAX];
  private       static boolean   _logLevel = false;
  public static boolean debugging;
  public static boolean debuggingHigh;
  
  static {
    
    // OK for J2S because this is a final class
    
    _activeLevels[LEVEL_DEBUGHIGH] = getProperty("debugHigh",    false);
    _activeLevels[LEVEL_DEBUG] = getProperty("debug",    false);
    _activeLevels[LEVEL_INFO]  = getProperty("info",     true);
    _activeLevels[LEVEL_WARN]  = getProperty("warn",     true);
    _activeLevels[LEVEL_ERROR] = getProperty("error",    true);
    _activeLevels[LEVEL_FATAL] = getProperty("fatal",    true);
    _logLevel                  = getProperty("logLevel", false);
    debugging = (_logger != null && (_activeLevels[LEVEL_DEBUG] || _activeLevels[LEVEL_DEBUGHIGH]));
    debuggingHigh = (debugging && _activeLevels[LEVEL_DEBUGHIGH]);
  }

  private static boolean getProperty(String level, boolean defaultValue) {
    try {
      String property = System.getProperty("jmol.logger." + level, null);
      if (property != null) {
        return (property.equalsIgnoreCase("true"));
      }
    } catch (Exception e) {
      // applet can't do this.
    }
    return defaultValue;
  }

  /**
   * Replaces the current logger implementation by a new one.
   * 
   * @param logger New logger implementation.
   */
  public static void setLogger(LoggerInterface logger) {
    _logger = logger;
    debugging = isActiveLevel(LEVEL_DEBUG) || isActiveLevel(LEVEL_DEBUGHIGH);
    debuggingHigh = (debugging && _activeLevels[LEVEL_DEBUGHIGH]);
  }

  /**
   * Tells if a logging level is active.
   * 
   * @param level Logging level.
   * @return Active.
   */
  public static boolean isActiveLevel(int level) {
    return _logger != null && level >= 0 && level < LEVEL_MAX 
        && _activeLevels[level];
  }

  /**
   * Changes the activation state for a logging level.
   * 
   * @param level Level.
   * @param active New activation state.
   */
  public static void setActiveLevel(int level, boolean active) {
    if (level < 0)
      level = 0;
    if (level >= LEVEL_MAX)
      level = LEVEL_MAX - 1;
    _activeLevels[level] = active;
    debugging = isActiveLevel(LEVEL_DEBUG) || isActiveLevel(LEVEL_DEBUGHIGH);
    debuggingHigh = (debugging && _activeLevels[LEVEL_DEBUGHIGH]);
  }

  /**
   * Activates all logging levels up through a given level.
   * 
   * @param level
   */
  public static void setLogLevel(int level) {
    for (int i = LEVEL_MAX; --i >= 0;)
      setActiveLevel(i, i <= level);
  }

  /**
   * Returns the text corresponding to a level.
   * 
   * @param level Level.
   * @return Corresponding text.
   */
  public static String getLevel(int level) {
    switch (level) {
    case LEVEL_DEBUGHIGH:
      return "DEBUGHIGH";
    case LEVEL_DEBUG:
      return "DEBUG";
    case LEVEL_INFO:
      return "INFO";
    case LEVEL_WARN:
      return "WARN";
    case LEVEL_ERROR:
      return "ERROR";
    case LEVEL_FATAL:
      return "FATAL";
    }
    return "????";
  }

  /**
   * Indicates if the level is logged.
   * 
   * @return Indicator.
   */
  public static boolean logLevel() {
    return _logLevel;
  }

  /**
   * Indicates if the level is logged.
   * 
   * @param log Indicator.
   */
  public static void doLogLevel(boolean log) {
    _logLevel = log;
  }

  /**
   * Writes a log at DEBUG level.
   * 
   * @param txt String to write.
   */
  public static void debug(String txt) {
    if (!debugging)
      return;
    try {
      _logger.debug(txt);
    } catch (Throwable t) {
      //
    }
  }

  /**
   og* Writes a log at INFO level.
   * 
   * @param txt String to write.
   */
  public static void info(String txt) {
    try {
      if (isActiveLevel(LEVEL_INFO)) {
        _logger.info(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at WARN level.
   * 
   * @param txt String to write.
   */
  public static void warn(String txt) {
    try {
      if (isActiveLevel(LEVEL_WARN)) {
        _logger.warn(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at WARN level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public static void warnEx(String txt, Throwable e) {
    try {
      if (isActiveLevel(LEVEL_WARN)) {
        _logger.warnEx(txt, e);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at ERROR level.
   * 
   * @param txt String to write.
   */
  public static void error(String txt) {
    try {
      if (isActiveLevel(LEVEL_ERROR)) {
        _logger.error(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at ERROR level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public static void errorEx(String txt, Throwable e) {
    try {
      if (isActiveLevel(LEVEL_ERROR)) {
        _logger.errorEx(txt, e);
      }
    } catch (Throwable t) {
      //
    }
  }

  public static int getLogLevel() {
    for (int i = LEVEL_MAX; --i >= 0;)
      if (isActiveLevel(i))
        return i;
    return 0;
  }

  /**
   * Writes a log at FATAL level.
   * 
   * @param txt String to write.
   */
  public static void fatal(String txt) {
    try {
      if (isActiveLevel(LEVEL_FATAL)) {
        _logger.fatal(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at FATAL level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public static void fatalEx(String txt, Throwable e) {
    try {
      if (isActiveLevel(LEVEL_FATAL)) {
        _logger.fatalEx(txt, e);
      }
    } catch (Throwable t) {
      //
    }
  }

  static Map<String, Long>htTiming = new Hashtable<String, Long>();
  public static void startTimer(String msg) {
    if (msg != null)
      htTiming.put(msg, Long.valueOf(System.currentTimeMillis()));
  }

  public static String getTimerMsg(String msg, int time) {
    if (time == 0)
      time = getTimeFrom(msg);
    return "Time for " + msg + ": " + (time) + " ms";
  }
  
  private static int getTimeFrom(String msg) {
    Long t;
    return (int) (msg == null || (t = htTiming.get(msg)) == null ? -1 : System
        .currentTimeMillis() - t.longValue());
  }

  public static int checkTimer(String msg, boolean andReset) {
    int time = getTimeFrom(msg);
    if (time >= 0 && !msg.startsWith("("))
        info(getTimerMsg(msg, time));
    if (andReset)
      startTimer(msg);
    return time;
  }
  
  public static void checkMemory() {
    long bTotal = 0, bFree = 0, bMax = 0;
    /**
     * @j2sIgnore
     */
    {
      Runtime runtime = Runtime.getRuntime();
      runtime.gc();
      bTotal = runtime.totalMemory();
      bFree = runtime.freeMemory();
      bMax = runtime.maxMemory();
    }
    info("Memory: Total-Free="+ (bTotal - bFree)+"; Total=" +  bTotal + "; Free=" + bFree 
        + "; Max=" + bMax);
  }
  
}
