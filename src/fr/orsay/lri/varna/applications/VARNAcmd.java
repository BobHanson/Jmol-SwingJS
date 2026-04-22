/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.applications;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfigLoader;

/**
 * A command-line interface that either opens a VARNAGUI isntance or just
 * creates an output image or session flle.
 */
public class VARNAcmd {

  public static class ExitCode extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -3011196062868355584L;
    private int _c;
    private String _msg;

    public ExitCode(int c, String msg) {
      _c = c;
      _msg = msg;
    }

    public int getExitCode() {
      return _c;
    }

    public String getExitMessage() {
      return _msg;
    }
  }

  private VARNAapp app;
  Vector<String> opts = new Vector<String>();

  /**
   * @param args
   */
  public VARNAcmd(String[] args) {
    for (int i = 0; i < args.length; i++) {
      opts.add(args[i]);
    }
  }

  /**
   * modified by Bob Hanson to not automatically run()
   * @param opts
   */
  public VARNAcmd(Vector<String> opts) {
    this.opts = opts;
   }

  private void run() throws ExitCode {
    app = new VARNAapp(true);
    String[] err = new String[1];
    String opt = app.setCLIOptions(opts, err);
    if (err[0] != null) {
      errorExit(err[0]);
    } else if (opt != null) {
      switch (opt) {
      case "-h":
        displayLightHelpExit();
        return; // not reachable
      case "-x":
        displayDetailledHelpExit();
        return; // not reachable
      }
    }
    String msg = app.processCLI();
    if (msg == null)
      return;
    switch (msg) {
    case "exit0":
      throw new ExitCode(0, "");
    case "exit1":
      throw new ExitCode(1, "");
    default:
      errorExit(msg);
    }
  }

  public void addOption(String key, String value) {
    app.addOption(key, value);
  }

  private static String getDescription() {
    return "VARNA v" + VARNAConfig.MAJOR_VERSION + "."
        + VARNAConfig.MINOR_VERSION
        + " Assisted drawing of RNA secondary structure (Command Line version)";
  }

  private static String indent(int k) {
    String result = "";
    for (int i = 0; i < k; i++) {
      result += "  ";
    }
    return result;
  }

  private static String complete(String s, int k) {
    String result = s;
    while (result.length() < k) {
      result += " ";
    }
    return result;
  }

  private static Vector<String[]> helpMatrix = new Vector<String[]>();

  private static void addLine(String opt, String val) {
    String[] line = { opt, val };
    helpMatrix.add(line);
  }

  private static int MAX_WIDTH = 100;

  private static void printMatrix(int ind) {
    String[][] values = new String[helpMatrix.size()][];
    helpMatrix.toArray(values);
    Arrays.sort(values, new Comparator<String[]>() {
      @Override
      public int compare(String[] o1, String[] o2) {
        return o1[0].compareTo(o2[0]);
      }
    });

    int maxSize = 0;
    for (int i = 0; i < values.length; i++) {
      String[] elem = values[i];
      maxSize = Math.max(maxSize, elem[0].length());
    }
    maxSize += ind + 2;
    for (int i = 0; i < values.length; i++) {
      String[] elem = values[i];
      String opt = elem[0];
      String msg = elem[1];
      opt = complete("", ind) + "-" + complete(opt, maxSize - ind);
      System.out.println(opt
          + msg.substring(0, Math.min(MAX_WIDTH - opt.length(), msg.length())));
      if (opt.length() + msg.length() >= MAX_WIDTH) {
        int off = MAX_WIDTH - opt.length();
        while (off < msg.length()) {
          String nmsg = msg.substring(off,
              Math.min(off + MAX_WIDTH - opt.length(), msg.length()));
          System.out.println(complete("", opt.length()) + nmsg);
          off += MAX_WIDTH - opt.length();
        }
      }
    }
    helpMatrix = new Vector<String[]>();
  }

  private static void printUsage() {
    System.out.println(
        "Usage: java -cp . [-i InFile|-sequenceDBN XXX -structureDBN YYY] -o OutFile [Options]");
    System.out.println("Where:");
    System.out.println(
        indent(1) + "OutFile\tSupported formats: {JPEG,PNG,EPS,XFIG,SVG}");
    System.out.println(indent(1)
        + "InFile\tSecondary structure file: Supported formats: {BPSEQ,CT,RNAML,DBN}");

  }

  private static void printHelpOptions() {
    System.out.println("\nMain options:");
    addLine("h", "Displays a short description of main options and exits");
    addLine("x", "Displays a detailled description of all options");
    printMatrix(2);
  }

  private void printMainOptions(String[][] info) {
    System.out.println("\nMain options:");
    addLine("h", "Displays a short description of main options and exits");
    addLine("x", "Displays a detailled description of all options");
    for (int i = 0; i < info.length; i++) {
      String key = info[i][0];
      if (app._basicOptsInv.containsKey(key)) {
        addLine(key, info[i][2]);
      }
    }
    printMatrix(2);
  }

  private void printAdvancedOptions(String[][] info) {
    System.out.println("\nAdvanced options:");
    for (int i = 0; i < info.length; i++) {
      String key = info[i][0];
      if (!app._basicOptsInv.containsKey(key)) {
        addLine(key, info[i][2]);
      }
    }
    addLine("quality", "Sets quality (non-vector file formats only)");
    addLine("resolution", "Sets resolution (non-vector file formats only)");
    printMatrix(2);
  }

  private void displayLightHelpExit() throws ExitCode {
    String[][] info = VARNAConfigLoader.getParameterInfo();
    System.out.println(getDescription());
    printUsage();
    printMainOptions(info);
    throw (new ExitCode(1, ""));
  }

  private void displayDetailledHelpExit() throws ExitCode {
    String[][] info = VARNAConfigLoader.getParameterInfo();
    System.out.println(getDescription());
    printUsage();
    printMainOptions(info);
    printAdvancedOptions(info);
    throw (new ExitCode(1, ""));
  }

  private static void errorExit(String msg) throws ExitCode {
    System.out.println(getDescription());
    System.out.println("Error: " + msg + "\n");
    printUsage();
    printHelpOptions();
    throw (new ExitCode(1, ""));
  }

  public static void main(String[] args) {
    try {
      new VARNAcmd(args).run();
    } catch (ExitCode e) {
      System.err.println(e.getExitMessage());
      System.exit(e.getExitCode());
    }
  }

}
