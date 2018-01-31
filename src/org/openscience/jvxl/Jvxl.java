/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:09:49 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7221 $
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
package org.openscience.jvxl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.jmol.jvxl.readers.SurfaceGenerator;
import org.jmol.util.Logger;
import javajs.util.P4;
import javajs.util.PT;

public class Jvxl {

  private final static String VERSION = "JVXL.java Version 1.0";
  public static void main(String[] args) {

    boolean blockData = false;
    int fileIndex = Integer.MAX_VALUE;
    String inputFile = null;
    String mapFile = null;
    String outputFile = null;

    float cutoff = Float.NaN;
    boolean isPositiveOnly = false;

    P4 plane = null;

    boolean bicolor = false;
    boolean reverseColor = false;
    float min = Float.NaN;
    float max = Float.NaN;

    Options options = new Options();
    options.addOption("h", "help", false, "give this help page");

    /*
     *  examples: 
     *  
     *  jvxl ch3cl-density.cub --min=0.0 --max=0.2 --map ch3cl-esp.cub
     *  jvxl ethene-HOMO.cub --bicolor --output ethene.jvxl
     *  jvxl d_orbitals.jvxl --index 2 --phase yz
     *  jvxl d_orbitals.jvxl --map sets
     *  jvxl --plane xy  --min=0.0 --max=0.2 --map data/ch3cl-density.cub 
     */

    // file options
    options.addOption("B", "blockdata", false,
        "multiple cube data are in blocks, not interspersed");

    options.addOption("P", "progressive", false,
        "create JVXL+ progressive X low-to-high format");

    OptionBuilder.withLongOpt("file");
    OptionBuilder.withDescription("file containing surface data");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("f"));

    OptionBuilder.withLongOpt("index");
    OptionBuilder.withDescription("index of surface in file (starting with 1)");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("i"));

    OptionBuilder.withLongOpt("plane");
    OptionBuilder
        .withDescription("plane: x, y, z, xy, xz, yz, z2, x2-y2, or {a,b,c,d}");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("p"));

    OptionBuilder.withLongOpt("map");
    OptionBuilder
        .withDescription("file containing data to map onto the surface or \"sets\"");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("m"));

    OptionBuilder.withLongOpt("output");
    OptionBuilder.withDescription("JVXL output file");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("o"));

    // surface options

    OptionBuilder.withLongOpt("cutoff");
    OptionBuilder.withDescription("isosurface cutoff value");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("c"));

    // color mapping options

    options.addOption("b", "bicolor", false, "bicolor map (orbital)");
    options.addOption("r", "reversecolor", false, "reverse color");

    OptionBuilder.withLongOpt("colorScheme");
    OptionBuilder
        .withDescription("VRML color scheme: bw, wb, roygb, bgyor, rwb, bwr, low, high");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("s"));

    OptionBuilder.withLongOpt("phase");
    OptionBuilder
        .withDescription("color by phase: x, y, z, xy, xz, yz, z2, x2-y2");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("F"));

    OptionBuilder.withLongOpt("min");
    OptionBuilder.withDescription("color absolute minimum value");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("n"));

    OptionBuilder.withLongOpt("max");
    OptionBuilder.withDescription("color absolute maximum value");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("x"));

    CommandLine line = null;
    try {
      CommandLineParser parser = new PosixParser();
      line = parser.parse(options, args);
    } catch (ParseException exception) {
      Logger.error("Unexpected exception: " + exception.toString());
    }

    if (line.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Jvxl", options);
      return;
    }

    args = line.getArgs();
    if (args.length > 0) {
      inputFile = args[0];
    }

    // files 

    blockData = (line.hasOption("B"));

    if (line.hasOption("i")) {
      fileIndex = PT.parseInt(line.getOptionValue("i"));
    }

    if (line.hasOption("f")) {
      inputFile = line.getOptionValue("f");
    }

    if (line.hasOption("m")) {
      mapFile = line.getOptionValue("m");
    }

    if (line.hasOption("p")) {
      plane = getPlane(line.getOptionValue("p"));
      if (plane == null) {
        Logger.error("invalid plane");
        return;
      }
      Logger.info("using plane " + plane);
      if (mapFile == null)
        mapFile = inputFile;
      if (inputFile == null)
        inputFile = mapFile;
    }

    if (line.hasOption("o")) {
      outputFile = line.getOptionValue("o");
    } else {
      outputFile = inputFile;
      if (outputFile.indexOf(".") < 0)
        outputFile += ".";
      String sIndex = (fileIndex == Integer.MAX_VALUE ? "" : "_" + fileIndex);
      if (sIndex.length() == 0 && outputFile.indexOf(".jvxl") >= 0)
        sIndex += "_new";
      outputFile = outputFile.substring(0, outputFile.lastIndexOf("."))
          + sIndex + ".jvxl";
    }

    // Process more command line arguments
    // these are also passed to vwr

    bicolor = (line.hasOption("b"));
    reverseColor = (line.hasOption("r"));

    if (bicolor && mapFile != null) {
      Logger.warn("--map option ignored; incompatible with --bicolor");
      mapFile = null;
    }

    if (line.hasOption("c")) {
      String s = line.getOptionValue("c");
      if (s.indexOf("+") == 0) {
        isPositiveOnly = true;
        s = s.substring(1);
      }
      cutoff = PT.parseFloat(s);
    }

    if (line.hasOption("n")) {
      if (bicolor)
        Logger.warn("--min option ignored; incompatible with --bicolor");
      else
        min = PT.parseFloat(line.getOptionValue("n"));
    }

    if (line.hasOption("x")) {
      if (bicolor)
        Logger.warn("--max option ignored; incompatible with --bicolor");
      else
        max = PT.parseFloat(line.getOptionValue("x"));
    }

    //    if (line.hasOption("P")) {
    //      phase = line.getOptionValue("P");
    //    }

    boolean progressive = line.hasOption("P");

    // compose the surface

    SurfaceGenerator sg = new SurfaceGenerator(null, null, null, null);

    // input file

    sg.version = VERSION;
    if (blockData)
      sg.setProp("blockData", Boolean.TRUE, null);
    if (!Float.isNaN(cutoff))
      sg.setProp(isPositiveOnly ? "cutoffPositive" : "cutoff", Float.valueOf(
          cutoff), null);
    if (bicolor)
      sg.setProp("sign", null, null);
    if (reverseColor)
      sg.setProp("reverseColor", null, null);
    //if (phase != null)
      //sg.setProp("phase", phase);

    if (progressive)
      sg.setProp("progressive", null, null);

    if (plane != null)
      sg.setProp("plane", plane, null);
    else {
      if (fileIndex != Integer.MAX_VALUE)
        sg.setProp("fileIndex", Integer.valueOf(fileIndex), null);
      Object t = FileReader
      .getBufferedReaderOrErrorMessageFromName(inputFile);
      if (t instanceof String) {
        Logger.error((String) t);
        return;
      }
      BufferedReader br = (BufferedReader) t;
      sg.setProp("readFile", br, null);
      try {
        br.close();
      } catch (Exception e) {
        //
      }
    }

    sg.setProp("title", line.toString(), null);

    //color scheme is only for VMRL

    //if (colorScheme != null) {
     // ColorEncoder ce = new ColorEncoder(null);
     // ce.setColorScheme(colorScheme, false);
     // sg.setProp("colorScheme", ce);
   // }
    if (!Float.isNaN(min))
      sg.setProp("red", Float.valueOf(min), null);
    if (!Float.isNaN(max))
      sg.setProp("blue", Float.valueOf(max), null);
    if (mapFile != null) {
      Object t = FileReader
      .getBufferedReaderOrErrorMessageFromName(mapFile);
      if (t instanceof String) {
        Logger.error((String) t);
        return;
      }
      BufferedReader br2 = (BufferedReader) t;
      sg.setProp("mapColor", br2, null);
      try {
        br2.close();
      } catch (Exception e) {
        //
      }
    }

    writeFile(outputFile, (String) sg.getProperty("jvxlFileData", 0));
    Logger.info((String) sg.getProperty("jvxlFileInfo", 0));
    Logger.info("\ncreated " + outputFile);
  }

  static void writeFile(String fileName, String text) {
    try {
      FileOutputStream os = new FileOutputStream(fileName);
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os), 8192);
      bw.write(text);
      bw.close();
      os = null;
    } catch (IOException e) {
      Logger.error("IO Exception: " + e.toString());
    }
  }
  
  static P4 getPlane(String str) {
    if (str.equalsIgnoreCase("xy"))
      return P4.new4(0, 0, 1, 0);
    if (str.equalsIgnoreCase("xz"))
      return P4.new4(0, 1, 0, 0);
    if (str.equalsIgnoreCase("yz"))
      return P4.new4(1, 0, 0, 0);
    if (str.indexOf("x=") == 0) {
      return P4.new4(1, 0, 0, -PT.parseFloat(str.substring(2)));
    }
    if (str.indexOf("y=") == 0) {
      return P4.new4(0, 1, 0, -PT.parseFloat(str.substring(2)));
    }
    if (str.indexOf("z=") == 0) {
      return P4.new4(0, 0, 1, -PT.parseFloat(str.substring(2)));
    }
    if (str.indexOf("{") == 0) {
      str = str.replace(',', ' ');
      int[] next = new int[1];
      return P4.new4(PT.parseFloatNext(str, next), PT.parseFloatNext(str,
          next), PT.parseFloatNext(str, next), PT.parseFloatNext(str, next));
    }
    return null;
  }
}
class FileReader {
  
  FileReader() {
    
  }
  
  static Object getBufferedReaderOrErrorMessageFromName(String name) {
    Object t = getInputStreamOrErrorMessageFromName(name);
    if (t instanceof String)
      return t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream)t, 8192);
      InputStream is = bis;
      return new BufferedReader(new InputStreamReader(is));
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  private static Object getInputStreamOrErrorMessageFromName(String name) {
    String errorMessage = null;
    try {
        Logger.info("opening " + name);
        File file = new File(name);
        int length = (int) file.length();
        InputStream in = new FileInputStream(file);
        return new MonitorInputStream(in, length);
    } catch (Exception e) {
      errorMessage = "" + e;
    }
    return errorMessage;
  }
}

class MonitorInputStream extends FilterInputStream {
  int length;
  int position;
  int markPosition;
  int readEventCount;
  long timeBegin;

  MonitorInputStream(InputStream in, int length) {
    super(in);
    this.length = length;
    this.position = 0;
    timeBegin = System.currentTimeMillis();
  }

  @Override
  public int read() throws IOException{
    ++readEventCount;
    int nextByte = super.read();
    if (nextByte >= 0)
      ++position;
    return nextByte;
  }

  @Override
  public int read(byte[] b) throws IOException {
    ++readEventCount;
    int cb = super.read(b);
    if (cb > 0)
      position += cb;
    return cb;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    ++readEventCount;
    int cb = super.read(b, off, len);
    if (cb > 0)
      position += cb;
    return cb;
  }

  @Override
  public long skip(long n) throws IOException {
    long cb = super.skip(n);
    // this will only work in relatively small files ... 2Gb
    position = (int)(position + cb);
    return cb;
  }

  @Override
  public synchronized void mark(int readlimit) {
    super.mark(readlimit);
    markPosition = position;
  }

  @Override
  public synchronized void reset() throws IOException {
    position = markPosition;
    super.reset();
  }

  int getPosition() {
    return position;
  }

  int getLength() {
    return length;
  }

  int getPercentageRead() {
    return position * 100 / length;
  }

  int getReadingTimeMillis() {
    return (int)(System.currentTimeMillis() - timeBegin);
  }
}
