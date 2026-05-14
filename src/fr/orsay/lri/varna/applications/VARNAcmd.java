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
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import fr.orsay.lri.varna.applications.VARNA.ExitCode;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfigLoader;

  /**
   * A package-private class to handle command line interface for VARNA.java
   */
  class VARNAcmd {
    
    public static String setCLIOptions(Vector<String> args, VARNAapp app) throws ExitCode {
      int i = 0;
      while (i < args.size()) {
        String opt = args.elementAt(i);
        String val = (i + 1 < args.size() ? args.get(i + 1) : null);
        switch (opt) {
        case "-h":
        case "-x":
          displayHelpExit(opt.equals("-x"));
          return null;
        case "-headless":
          app.setHeadless();
          --i;
          break;
        case "-demo":
          app.setupDemo();
          --i;
          break;
        case "-noedit":
          app.editable = false;
          --i;
          break;
        default:
          if (!opt.startsWith("-")) {
            return "Missing or unknown option \"" + opt + "\"";
          }
          if (val == null || val.startsWith("-") ) {
            return "Missing value for \"" + opt + "\"";
          }
          String err = app.addOptionWithValue(opt, val);
          if (err != null)
            return err;
          break;
        }
        i += 2;
      }
      return null;
    }


    private static void displayHelpExit(boolean advanced) throws ExitCode {
      Set<String> options = new HashSet<>();
      String[] _basicOptions = { //
          VARNAConfigLoader.algoOpt, //
          VARNAConfigLoader.bpStyleOpt, //
          VARNAConfigLoader.bondColorOpt, //
          VARNAConfigLoader.backboneColorOpt, //
          VARNAConfigLoader.periodNumOpt, //
          VARNAConfigLoader.baseInnerColorOpt, //
          VARNAConfigLoader.baseOutlineColorOpt, //
      };

      for (int j = 0; j < _basicOptions.length; j++) {
        options.add(_basicOptions[j]);
      }

      String[][] info = VARNAConfigLoader.getParameterInfo();
      System.out.println(getDescription());
      printUsage();
      printMainOptions(info, options);
      if (advanced) {
        System.out.println("\nAdvanced options:");
        for (int i = 0; i < info.length; i++) {
          String key = info[i][0];
          if (!options.contains(key)) {
            addOptionLine(key, info[i][2]);
          }
        }
        addOptionLine("quality", "Sets quality (non-vector file formats only)");
        addOptionLine("resolution", "Sets resolution (non-vector file formats only)");      
        printMatrix(2);
      }
      throw (new ExitCode(1, ""));
    }

    protected static String getDescription() {
      return "VARNA v" + VARNAConfig.MAJOR_VERSION + "."
          + VARNAConfig.MINOR_VERSION
          + " Assisted drawing of RNA secondary structure (Command Line version)";
    }

    protected static void printUsage() {
      System.out.println(
          "Usage: java -cp . [-i InFile|-sequenceDBN XXX -structureDBN YYY] -o OutFile [Options]");
      System.out.println("Where:");
      System.out.println(
          indent(1) + "OutFile\tSupported formats: {JPEG,PNG,EPS,XFIG,SVG}");
      System.out.println(indent(1)
          + "InFile\tSecondary structure file: Supported formats: {BPSEQ,CT,RNAML,DBN}");

    }

    protected static void printHelpOptions() {
      System.out.println("\nMain options:");
      addOptionLine("h", "Displays a short description of main options and exits");
      addOptionLine("x", "Displays a detailled description of all options");
      printMatrix(2);
    }


    private static String indent(int k) {
      return leftAlign("", k);
    }

    private static String leftAlign(String s, int k) {
      int n = s.length() - k;
      if (n > 0) {
        s += "                ".substring(0, n);
      }
      return s;
    }

    private static Vector<String[]> helpMatrix = new Vector<String[]>();

    private static void addOptionLine(String opt, String val) {
      String[] line = { opt, val };
      helpMatrix.add(line);
    }

    private static void printMainOptions(String[][] info, Set<String> options) {
      System.out.println("\nMain options:");
      addOptionLine("h", "Displays a short description of main options and exits");
      addOptionLine("x", "Displays a detailled description of all options");
      for (int i = 0; i < info.length; i++) {
        String key = info[i][0];
        if (options.contains(key)) {
          addOptionLine(key, info[i][2]);
        }
      }
      printMatrix(2);
    }

    private final static int MAX_WIDTH = 100;

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
        opt = leftAlign("", ind) + "-" + leftAlign(opt, maxSize - ind);
        System.out.println(opt
            + msg.substring(0, Math.min(MAX_WIDTH - opt.length(), msg.length())));
        if (opt.length() + msg.length() >= MAX_WIDTH) {
          int off = MAX_WIDTH - opt.length();
          while (off < msg.length()) {
            String nmsg = msg.substring(off,
                Math.min(off + MAX_WIDTH - opt.length(), msg.length()));
            System.out.println(leftAlign("", opt.length()) + nmsg);
            off += MAX_WIDTH - opt.length();
          }
        }
      }
      helpMatrix = new Vector<String[]>();
    }


}
