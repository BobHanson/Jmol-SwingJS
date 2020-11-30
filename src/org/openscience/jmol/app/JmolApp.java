/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.openscience.jmol.app;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.File;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jmol.api.JmolAppAPI;
import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import javajs.util.PT;

public class JmolApp implements JmolAppAPI {

  /**
   * The data model.
   */

  public int port;
  public int startupWidth, startupHeight;
  //  public Point border;
  public boolean haveBorder;

  public File userPropsFile;
  public HistoryFile historyFile, pluginFile;

  public boolean haveConsole = true;
  public boolean haveDisplay = true;
  public boolean splashEnabled = /** @j2sNative false && */
      true;
  public boolean isDataOnly;
  public boolean isKiosk;
  public boolean isPrintOnly;
  public boolean isSilent;
  public Map<String, Object> info = new Hashtable<String, Object>();
  public Point jmolPosition;
  public float autoAnimationDelay = 0.2f; // sec

  private String modelFilename;
  private String scriptFilename;
  private String script1 = "";
  private String script2 = "";
  private boolean scanInput;
  private String menuFile;

  //private JmolViewer vwr;
  //private JmolAdapter modelAdapter;

  public JmolApp() {
    // defer parsing until we can set a few options ourselves
  }

  /**
   * standard Jmol application entry point
   * 
   * @param args
   */
  public JmolApp(String[] args) {

    if (System.getProperty("javawebstart.version") != null) {

      // If the property is found, Jmol is running with Java Web Start. To fix
      // bug 4621090, the security manager is set to null.
      System.setSecurityManager(null);
    }
    if (System.getProperty("user.home") == null) {
      System.err.println(GT
          .$("Error starting Jmol: the property 'user.home' is not defined."));
      System.exit(1);
    }
    /**
     * @j2sNative
     */
    {
      File ujmoldir = new File(new File(System.getProperty("user.home")),
          ".jmol");
      ujmoldir.mkdirs();
      userPropsFile = new File(ujmoldir, "properties");
      historyFile = new HistoryFile(new File(ujmoldir, "history"),
          "Jmol's persistent values");
      pluginFile = new HistoryFile(new File(ujmoldir, "plugins"),
          "Jmol plugin persistent values");
    }

    parseCommandLine(args);
  }

  public void parseCommandLine(String[] args) {

    Options options = getOptions();

    CommandLine line = null;
    try {
      CommandLineParser parser = new PosixParser();
      line = parser.parse(options, args);
    } catch (ParseException exception) {
      System.err.println("Unexpected exception: " + exception.toString());
    }

    args = line.getArgs();
    if (args.length > 0) {
      modelFilename = args[0];
    }
    checkOptions(line, options);

  }

  private Options getOptions() {
    Options options = new Options();

    options.addOption("a", "autoanimationdelay", true, GT.$(
        "delay time in seconds for press-and-hold operation of toolbar animation buttons (default 0.2; numbers > 10 assumed to be milliseconds; set to 0 to disable)"));

    options.addOption("b", "backgroundtransparent", false,
        GT.$("transparent background"));

    options.addOption("C", "checkload", false,
        GT.$("check script syntax only - with file loading"));
    options.addOption("c", "check", false,
        GT.$("check script syntax only - no file loading"));

    OptionBuilder.withValueSeparator();
    options.addOption("D", "property=value", true,
        GT.$("supported options are given below"));
    options.addOption("d", "debug", false, GT.$("debug"));

    options.addOption("g", "geometry", true,
        GT.o(GT.$("window width x height, e.g. {0}"), "-g500x500"));

    options.addOption("h", "help", false, GT.$("give this help page"));

    options.addOption("I", "input", false,
        GT.$("allow piping of input from System.Input"));
    options.addOption("i", "silent", false, GT.$("silent startup operation"));

    options.addOption("J", "jmolscript1", true,
        GT.$("Jmol script to execute BEFORE -s option"));

    options.addOption("j", "jmolscript2", true,
        GT.$("Jmol script to execute AFTER -s option"));

    options.addOption("k", "kiosk", false, GT.$("kiosk mode -- no frame"));

    options.addOption("L", "nosplash", false,
        GT.$("start with no splash screen"));
    options.addOption("l", "list", false,
        GT.$("list commands during script execution"));

    options.addOption("M", "multitouch", true,
        GT.$("use multitouch interface (requires \"sparshui\" parameter"));

    options.addOption("m", "menu", true, GT.$("menu file to use"));

    options.addOption("n", "nodisplay", false,
        GT.$("no display (and also exit when done)"));

    options.addOption("o", "noconsole", false,
        GT.$("no console -- all output to sysout"));

    options.addOption("P", "port", true,
        GT.$("port for JSON/MolecularPlayground-style communication"));

    options.addOption("p", "printOnly", false,
        GT.$("send only output from print messages to console (implies -i)"));

    options.addOption("q", "quality", true, GT.$(
        "JPG image quality (1-100; default 75) or PNG image compression (0-9; default 2, maximum compression 9)"));

    options.addOption("R", "restricted", false,
        GT.$("restrict local file access"));
    options.addOption("r", "restrictSpt", false,
        GT.$("restrict local file access (allow reading of SPT files)"));

    options.addOption("s", "script", true,
        GT.$("script file to execute or '-' for System.in"));

    options.addOption("T", "headlessmaxtime", true,
        GT.$("headless max time (sec)"));

    options.addOption("t", "threaded", false,
        GT.$("independent command thread"));

    options.addOption("U", "plugin", true, GT.$("plugin to start initially"));

    options.addOption("G", "Plugin", false,
        GT.$("jmol is a plugin to some other app"));

    options.addOption("w", "write", true, GT.o(GT.$("{0} or {1}:filename"),
        new Object[] { "CLIP", "GIF|JPG|JPG64|PNG|PPM" }));

    options.addOption("x", "exit", false,
        GT.$("exit after script (implicit with -n)"));

    return options;
  }

  class OptSort implements Comparator<Option> {

    @Override
    public int compare(Option o1, Option o2) {
      char c1 = o1.getOpt().charAt(0);
      char c2 = o2.getOpt().charAt(0);
      char uc1 = Character.toUpperCase(c1);
      char uc2 = Character.toUpperCase(c2);
      return (uc1 == uc2 ? (c1 < c2 ? -1 : 1) : Character.compare(uc1, uc2));
    }
  }

  private void checkOptions(CommandLine line, Options options) {
    if (line.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.setOptionComparator(new OptSort());
      formatter.printHelp("Jmol", options);

      // now report on the -D options
      System.out.println();
      System.out.println(GT.$("For example:"));
      System.out.println();
      System.out
          .println("Jmol -ions myscript.spt -w JPEG:myfile.jpg > output.txt");
      System.out.println();
      System.out.println(GT.$(
          "The -D options are as follows (defaults in parenthesis) and must be called preceding '-jar Jmol.jar':"));
      System.out.println();
      System.out.println("  display.speed=[fps|ms] (ms)");
      //System.out.println("  JmolConsole=[true|false] (true)");
      System.out.println("  logger.debug=[true|false] (false)");
      System.out.println("  logger.error=[true|false] (true)");
      System.out.println("  logger.fatal=[true|false] (true)");
      System.out.println("  logger.info=[true|false] (true)");
      System.out.println("  logger.logLevel=[true|false] (false)");
      System.out.println("  logger.warn=[true|false] (true)");
      System.out.println("  plugin.dir (unset)");
      System.out.println(
          "  user.language=[ca|cs|de|en_GB|en_US|es|fr|hu|it|ko|nl|pt_BR|tr|zh_TW] (en_US)");

      System.exit(0);
    }

    if (line.hasOption("a")) {
      autoAnimationDelay = PT.parseFloat(line.getOptionValue("a"));
      if (autoAnimationDelay > 10)
        autoAnimationDelay /= 1000;
      Logger.info(
          "setting autoAnimationDelay to " + autoAnimationDelay + " seconds");
    }

    // Process more command line arguments
    // these are also passed to vwr

    // debug mode
    if (line.hasOption("d")) {
      Logger.setLogLevel(Logger.LEVEL_DEBUG);
    }

    // note that this is set up so that if JmolApp is 
    // invoked with just new JmolApp(), we can 
    // set options ourselves. 

    info.put(isDataOnly ? "JmolData" : "Jmol", Boolean.TRUE);

    // kiosk mode -- no frame

    if (line.hasOption("k"))
      info.put("isKiosk", Boolean.valueOf(isKiosk = true));

    // port for JSON mode communication

    if (line.hasOption("P"))
      port = PT.parseInt(line.getOptionValue("P"));
    if (port > 0)
      info.put("port", Integer.valueOf(port));

    // print command output only (implies silent)

    if (line.hasOption("p"))
      isPrintOnly = true;
    if (isPrintOnly) {
      info.put("printOnly", Boolean.TRUE);
      isSilent = true;
    }

    // silent startup
    if (line.hasOption("i"))
      isSilent = true;
    if (isSilent)
      info.put("silent", Boolean.TRUE);

    // output to sysout
    if (/** @j2sNative true || */line.hasOption("o"))
      haveConsole = false;
    if (!haveConsole) // might also be set in JmolData
      info.put("noConsole", Boolean.TRUE);

    // transparent background
    if (line.hasOption("b"))
      info.put("transparentBackground", Boolean.TRUE);

    // restricted file access
    if (line.hasOption("R"))
      info.put("access:NONE", Boolean.TRUE);

    // restricted file access (allow reading of SPT files)
    if (line.hasOption("r"))
      info.put("access:READSPT", Boolean.TRUE);

    // independent command thread
    if (line.hasOption("t"))
      info.put("useCommandThread", Boolean.TRUE);

    // list commands during script operation
    if (line.hasOption("l"))
      info.put("listCommands", Boolean.TRUE);

    // no splash screen
    if (line.hasOption("L"))
      splashEnabled = false;

    // check script only -- don't open files
    if (line.hasOption("c"))
      info.put("check", Boolean.TRUE);
    if (line.hasOption("C"))
      info.put("checkLoad", Boolean.TRUE);

    // menu file
    if (line.hasOption("m"))
      menuFile = line.getOptionValue("m");

    // run pre Jmol script
    if (line.hasOption("J"))
      script1 = line.getOptionValue("J");

    // use SparshUI
    if (line.hasOption("M"))
      info.put("multitouch", line.getOptionValue("M"));

    // run script from file
    if (line.hasOption("s")) {
      scriptFilename = line.getOptionValue("s");
    }

    // plugin 
    if (line.hasOption("U"))
      info.put("plugin", line.getOptionValue("U"));

    // run post Jmol script
    if (line.hasOption("j")) {
      script2 = line.getOptionValue("j");
    }

    //Point b = null;    
    Dimension size = null;
    if (haveDisplay && historyFile != null) {
      String vers = System.getProperty("java.version");
      if (vers.compareTo("1.1.2") < 0) {
        System.out.println("!!!WARNING: Swing components require a "
            + "1.1.2 or higher version VM!!!");
      }

      if (!isKiosk) {
        size = historyFile.getWindowInnerDimension("Jmol");
        if (size != null) {
          startupWidth = size.width;
          startupHeight = size.height;
        }
        //      historyFile.getWindowBorder("Jmol");
        //      // first one is just approximate, but this is set in doClose()
        //      // so it will reset properly -- still, not perfect
        //      // since it is always one step behind.
        //      //if (b == null || b.x > 50)
        //        border = new Point(12, 116);
        //      //else
        //        //border = new Point(b.x, b.y);
        //        // note -- the first time this is run after changes it will not work
        //      // because there is a bootstrap problem.
      }
    }
    // INNER frame dimensions
    int width = (isKiosk ? 0 : 500);
    int height = 500;

    if (line.hasOption("g")) {
      String geometry = line.getOptionValue("g");
      int indexX = geometry.indexOf('x');
      if (indexX > 0) {
        width = PT.parseInt(geometry.substring(0, indexX));
        height = PT.parseInt(geometry.substring(indexX + 1));
      } else {
        width = height = PT.parseInt(geometry);
      }
      startupWidth = -1;
    }

    if (startupWidth <= 0 || startupHeight <= 0) {
      //      if (haveDisplay && !isKiosk && border != null) {
      //        startupWidth = width + border.x;
      //        startupHeight = height + border.y;
      //      } else {
      startupWidth = width;
      startupHeight = height;
      //      }
    }

    // write image to clipboard or image file
    if (line.hasOption("w")) {
      int quality = -1;
      if (line.hasOption("q"))
        quality = PT.parseInt(line.getOptionValue("q"));
      String type_name = line.getOptionValue("w");
      if (type_name != null) {
        if (type_name.length() == 0)
          type_name = "JPG:jpg";
        if (type_name.indexOf(":") < 0)
          type_name += ":jpg";
        int i = type_name.indexOf(":");
        String type = type_name.substring(0, i).toUpperCase();
        type_name = type_name.substring(i + 1).trim();
        if (type.indexOf(" ") >= 0) {
          quality = PT.parseInt(type.substring(type.indexOf(" ")).trim());
          type.substring(0, type.indexOf(" "));
        }
        if (GraphicsEnvironment.isHeadless()) {
          Map<String, Object> data = new Hashtable<String, Object>();
          data.put("fileName", type_name);
          data.put("type", type);
          data.put("quality", Integer.valueOf(quality));
          data.put("width", Integer.valueOf(width));
          data.put("height", Integer.valueOf(height));
          info.put("headlessImage", data);
        } else
          script2 += ";write image "
              + (width > 0 && height > 0 ? width + " " + height : "") + " "
              + type + " " + quality + " " + PT.esc(type_name);
      }
    }
    if (GraphicsEnvironment.isHeadless())
      info.put("headlistMaxTimeMs",
          Integer.valueOf(1000
              * (line.hasOption("T") ? PT.parseInt(line.getOptionValue("T"))
                  : 60)));

    // the next three are coupled -- if the -n command line option is 
    // given, but -I is not, then the -x is added, but not vice-versa. 
    // however, if this is an application-embedded object, then
    // it is ok to have no display and no exit.

    // scanner input
    if (line.hasOption("I"))
      scanInput = true;

    boolean exitUponCompletion = false;
    if (line.hasOption("n")) {
      // no display (and exit)
      haveDisplay = false;
      exitUponCompletion = !scanInput;
    }
    if (line.hasOption("x"))
      // exit when script completes (or file is read)
      exitUponCompletion = true;

    if (!haveDisplay)
      info.put("noDisplay", Boolean.TRUE);
    if (exitUponCompletion) {
      info.put("exit", Boolean.TRUE);
      script2 += ";exitJmol;";
    }

  }

  public void startViewer(JmolViewer vwr, SplashInterface splash,
                          boolean isJmolData) {
    try {
    } catch (Throwable t) {
      System.out.println("uncaught exception: " + t);
      t.printStackTrace();
    }

    if (menuFile != null)
      vwr.setMenu(menuFile, true);

    // Open a file if one is given as an argument -- note, this CAN be a
    // script file
    if (modelFilename != null) {
      if (script1 == null)
        script1 = "";
      script1 = (modelFilename.endsWith(".spt") ? "script " : "load ")
          + PT.esc(modelFilename) + ";" + script1;
    }

    // OK, by now it is time to execute the script

    // then command script
    if (script1 != null && script1.length() > 0) {
      if (!isSilent)
        Logger.info("Executing script: " + script1);
      if (splash != null)
        splash.showStatus(GT.$("Executing script 1..."));
      runScript(script1, isJmolData, vwr);
    }

    // next the file

    if (scriptFilename != null) {
      if (!isSilent)
        Logger.info("Executing script from file: " + scriptFilename);
      if (splash != null)
        splash.showStatus(GT.$("Executing script file..."));
      if (scriptFilename.equals("-")) {

        // -s - option

        Scanner scan = new Scanner(System.in);
        String linein = "";
        StringBuilder script = new StringBuilder();
        while (scan.hasNextLine() && (linein = scan.nextLine()) != null
            && !linein.equals("!quit"))
          script.append(linein).append("\n");
        scan.close();
        runScript(script.toString(), isJmolData, vwr);
      } else {
        vwr.evalFile(scriptFilename);
      }
    }
    // then command script
    if (script2 != null && script2.length() > 0) {
      if (!isSilent)
        Logger.info("Executing script: " + script2);
      if (splash != null)
        splash.showStatus(GT.$("Executing script 2..."));
      runScript(script2, isJmolData, vwr);
    }

    // finally plugin

    // -U flag
    String plugin = (String) info.get("plugin");
    if (plugin != null)
      ((Viewer) vwr).startPlugin(plugin);

    // scanner input
    if (scanInput) {
      new InputScannerThread(vwr, isSilent);
    }
  }

  private void runScript(String script, boolean outputResults, JmolViewer vwr) {
    if (outputResults)
      System.out.print(vwr.scriptWaitStatus(script, null));
    else
      vwr.script(script);
  }

  @Override
  public void addHistoryWindowInfo(String name, Component window,
                                   Point border) {
    historyFile.addWindowInfo(name, window, border);
  }

  @Override
  public void addHistoryWindowDimInfo(String name, Component window,
                                      Dimension inner) {
    historyFile.addWindowInnerInfo(name, window, inner);
  }

  @Override
  public Point getHistoryWindowPosition(String name) {
    return historyFile.getWindowPosition(name);
  }

  @Override
  public Dimension getHistoryWindowSize(String name) {
    return historyFile.getWindowSize(name);
  }

}
