/* Copyright (c) 2002-2011 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.api.JmolJDXMOLReader;
import org.jmol.api.JmolJDXMOLParser;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import jspecview.api.JSVZipReader;
import jspecview.api.SourceReader;
import jspecview.common.Coordinate;
import jspecview.common.Spectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.common.PeakInfo;
import jspecview.exception.JSVException;

/**
 * <code>JDXFileReader</code> reads JDX data, including complex BLOCK files that
 * contain NTUPLE blocks or nested BLOCK data. 
 * 
 * In addition, this reader allows for simple concatenation -- no LINK record is 
 * required. This allows for testing simply by joining files. 
 * 
 * We also might be able to adapt this to reading a ZIP file collection.
 * 
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public class JDXReader implements JmolJDXMOLReader {

  /**
   * Labels for the exporter
   * 
   */
  private final static String[] VAR_LIST_TABLE = {
      "PEAKTABLE   XYDATA      XYPOINTS",
      " (XY..XY)    (X++(Y..Y)) (XY..XY)    " };

  public static String getVarList(String dataClass) {
    int index = VAR_LIST_TABLE[0].indexOf(dataClass);
    return VAR_LIST_TABLE[1].substring(index + 1, index+12).trim();
  }

  final static String ERROR_SEPARATOR = "=====================\n";
  
  private float nmrMaxY = Float.NaN;
  
//  static {
//    Arrays.sort(TABULAR_DATA_LABELS);  OUCH! - Breaks J2S
//  }
  private JDXSource source;
  private JDXSourceStreamTokenizer t;
  private SB errorLog;
  private boolean obscure;

  private boolean done;

  private boolean isZipFile;

  private String filePath;

  private boolean loadImaginary = true;

  private boolean isSimulation;

  /**
   * For JEOL, skip ##SHIFTREFERENCE 
   */
  private boolean ignoreShiftReference; // BH 2020.09.16

  private boolean ignorePeakTables;

  private JDXReader(String filePath, boolean obscure, boolean loadImaginary,
      int iSpecFirst, int iSpecLast, float nmrNormalization) {
    filePath = PT.trimQuotes(filePath);
    isSimulation = (filePath != null && filePath.startsWith(JSVFileManager.SIMULATION_PROTOCOL)); 
    if (isSimulation) {
      //TODO: H1 vs. C13 here?
      nmrMaxY = (Float.isNaN(nmrNormalization) ? 10000 : nmrNormalization);
      //filePath = JSVFileManager.getAbbrSimulationFileName(filePath);
    }
    // this.filePath is used for sending information back to Jmol
    // and also for setting the application tree label
    this.filePath = filePath;
    this.obscure = obscure;
    firstSpec = iSpecFirst;
    lastSpec = iSpecLast;
    this.loadImaginary = loadImaginary;
  }
  
  
  /**
   * used only for preferences display and Android
   * 
   * @param in
   * @param obscure
   * @param loadImaginary 
   * @param nmrMaxY 
   * @return source
   * @throws Exception
   */
  public static JDXSource createJDXSourceFromStream(InputStream in, boolean obscure, boolean loadImaginary, float nmrMaxY)
      throws Exception {
    return createJDXSource(in,
        "stream", obscure, loadImaginary, -1, -1, nmrMaxY);
  }

  public static Map<String, String> getHeaderMap(InputStream in, Map<String, String> map) throws Exception {
    if (map == null)
      map = new LinkedHashMap<String, String>();
    Lst<String[]> hlist = createJDXSource(in, null, false, false, 0, -1, 0).getJDXSpectrum(0).headerTable;
    for (int i = 0, n = hlist.size(); i < n; i++) {
      String[] h = hlist.get(i);
      // element [2] is the cleaned LABEL
      
      // "When DATALABELS
      // are parsed, alphabetic characters are converted to upper case, and all spaces,
      // dashes, slashes, and underlines are discarded. (XUNITS, xunits XUNITS, and X_UNI-TS
      // are equivalent, thus these characters can be used optionally as separators for readability."
      
      map.put(h[2], h[1]);
    }
    return map;    
  }

    
  /**
   * general entrance method
   * 
   * @param in
   *        one of: BufferedReader, InputStream, String, byte[]
   * @param filePath
   * @param obscure
   * @param loadImaginary
   * @param iSpecFirst
   * @param iSpecLast
   * @param nmrMaxY
   * @return source
   * @throws Exception
   */
  public static JDXSource createJDXSource(Object in, String filePath,
                                          boolean obscure,
                                          boolean loadImaginary, int iSpecFirst,
                                          int iSpecLast, float nmrMaxY)
      throws Exception {
    boolean isHeaderOnly = (iSpecLast < iSpecFirst);

    String data = null;
    BufferedReader br;
    if (in instanceof String || AU.isAB(in)) {
      if (in instanceof String)
        data = (String) in;
      br = JSVFileManager.getBufferedReaderForStringOrBytes(in);
    } else if (in instanceof InputStream) {
      br = JSVFileManager.getBufferedReaderForInputStream((InputStream) in);
    } else {
      br = (BufferedReader) in;
    }

    String header = null;
    JDXSource source = null;
    try {
      if (br == null)
        br = JSVFileManager.getBufferedReaderFromName(filePath, "##TITLE");
      if (!isHeaderOnly) {
        br.mark(400);
        char[] chs = new char[400];
        br.read(chs, 0, 400);
        br.reset();
        header = new String(chs);
        int pt1 = header.indexOf('#');
        int pt2 = header.indexOf('<');
        if (pt1 < 0 || pt2 >= 0 && pt2 < pt1) {
          String xmlType = header.toLowerCase();
          xmlType = (xmlType.contains("<animl")
              || xmlType.contains("<!doctype technique") ? "AnIML"
                  : xmlType.contains("xml-cml") ? "CML" : null);
          if (xmlType != null)
            source = ((SourceReader) JSViewer
                .getInterface("jspecview.source." + xmlType + "Reader"))
                    .getSource(filePath, br);
          br.close();
          if (source == null) {
            Logger.error(header + "...");
            throw new JSVException("File type not recognized");
          }
        }
      }
      if (source == null) {
        source = (new JDXReader(filePath, obscure, loadImaginary, iSpecFirst,
            iSpecLast, nmrMaxY)).getJDXSource(br, isHeaderOnly);
      }
      if (data != null)
        source.setInlineData(data);
      return source;
    } catch (Exception e) {
      if (!Viewer.isJS)
        e.printStackTrace();
      if (br != null)
        br.close();
      if (header != null)
        Logger.error(header + "...");
      String s = e.getMessage();
      /*
       * @j2sNative
       *
       * if (header != null)s += "\n\n" + header;
       * 
       */
      {
      }
      throw new JSVException("Error reading data: " + s);
    }
  }

  /**
   * The starting point for reading all data.
   * 
   * @param reader
   *        a BufferedReader or a JSVZipFileSequentialReader
   * @param isHeaderOnly
   * 
   * @return source
   * @throws JSVException
   */
  private JDXSource getJDXSource(Object reader, boolean isHeaderOnly)
      throws JSVException {

    source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
    isZipFile = (reader instanceof JSVZipReader);
    t = new JDXSourceStreamTokenizer((BufferedReader) reader);
    errorLog = new SB();
    String label = null;
    String value = null;
    boolean isOK = false;
    while (!done && "##TITLE".equals(t.peakLabel())) {
      isOK = true;
      if (label != null && !isZipFile)
        logError(
            "Warning - file is a concatenation without LINK record -- does not conform to JCAMP-DX standard 6.1.3!");
      Spectrum spectrum = new Spectrum();
      Lst<String[]> dataLDRTable = new Lst<String[]>();
      if (isHeaderOnly)
        spectrum.setHeaderTable(dataLDRTable);
      while (!done && (label = t.getLabel()) != null
          && (value = getValue(label)) != null) {
        if (isTabularData) {
          setTabularDataType(spectrum, label);
          if (!processTabularData(spectrum, dataLDRTable, isHeaderOnly))
            throw new JSVException("Unable to read JDX file");
          addSpectrum(spectrum, false);
          if (isSimulation && spectrum.getXUnits().equals("PPM"))
            spectrum.setHZtoPPM(true);
          spectrum = null;
          if (isHeaderOnly) {
            addHeader(dataLDRTable, t.rawLabel, "<data>");
            break;
          }
          continue;
        }
        if (!isHeaderOnly) {
          if (label.equals("##DATATYPE")
              && value.toUpperCase().equals("LINK")) {

            getBlockSpectra(dataLDRTable);
            spectrum = null;
            continue;
          }
          if (label.equals("##NTUPLES") || label.equals("##VARNAME")) {
            getNTupleSpectra(dataLDRTable, spectrum, label);
            spectrum = null;
            continue;
          }
        }
        if (label.equals("##JCAMPDX")) {
          setVenderSpecificValues(t.rawLine);
        }
        if (spectrum == null)
          spectrum = new Spectrum();
        if (readDataLabel(spectrum, label, value, errorLog, obscure, isHeaderOnly) && !isHeaderOnly)
          continue;
        addHeader(dataLDRTable, t.rawLabel, value);
        if (!isHeaderOnly && checkCustomTags(spectrum, label, value))
          continue; // BH ??? nothing after this?
      }
      if (isHeaderOnly && spectrum != null)
        addSpectrum(spectrum, false);
    }
    if (!isOK)
      throw new JSVException("##TITLE record not found");
    source.setErrorLog(errorLog.toString());
    return source;
  }

  String lastErrPath = null;
  private void logError(String err) {
    errorLog.append(filePath == null || filePath.equals(lastErrPath) ? "" : filePath).append("\n").append(err).append("\n");
    lastErrPath = filePath;
  }


  /**
   * Set any vendor-specific values. For example, JEOL implementation must ignore ##SHIFTREFERENCE
   * @param rawLine
   */
  private void setVenderSpecificValues(String rawLine) {
    if (rawLine.indexOf("JEOL") >= 0) {
      System.out.println("Skipping ##SHIFTREFERENCE for JEOL " + rawLine);
      ignoreShiftReference = true;
    }
    if (rawLine.indexOf("MestReNova") >= 0) {
      ignorePeakTables = true;
      
    }
  }


  private String getValue(String label) {
    String value = (isTabularDataLabel(label) ? "" : t.getValue());
    return ("##END".equals(label) ? null : value);
  }

  private boolean isTabularData;  
  private boolean isTabularDataLabel(String label) {
    return (isTabularData = ("##DATATABLE##PEAKTABLE##XYDATA##XYPOINTS#".indexOf(label + "#") >= 0));
  }
  
  private int firstSpec = 0;
  private int lastSpec = 0;
  private int nSpec = 0;

  private double blockID;

  private JmolJDXMOLParser mpr;

  private BufferedReader reader;

  private Spectrum modelSpectrum;

  private Lst<String[]> acdAssignments;
  private String acdMolFile;

  private boolean addSpectrum(Spectrum spectrum, boolean forceSub) {
    if (!loadImaginary && spectrum.isImaginary()) {
      Logger
          .info("FileReader skipping imaginary spectrum -- use LOADIMAGINARY TRUE to load this spectrum.");
      return true;
    }
    if (acdAssignments != null) {
      if (!spectrum.dataType.equals("MASS SPECTRUM") && !spectrum.isContinuous()) {
        Logger.info("Skipping ACD Labs line spectrum for " + spectrum);
        return true;
      }
      if (acdAssignments.size() > 0) {
        try {
          mpr.setACDAssignments(spectrum.title, spectrum.getTypeLabel(), 
              source.peakCount, acdAssignments, acdMolFile);
        } catch (Exception e) {
          Logger.info("Failed to create peak data: " + e);
        }
      }
      if (acdMolFile != null)
        JSVFileManager.cachePut("mol", acdMolFile);
    }
    if (!Float.isNaN(nmrMaxY))
      spectrum.doNormalize(nmrMaxY);
    else if (spectrum.getMaxY() >= 10000)
      spectrum.doNormalize(1000);
    if (isSimulation)
      spectrum.setSimulated(filePath);
    nSpec++;
    if (firstSpec > 0 && nSpec < firstSpec)
      return true;
    if (lastSpec > 0 && nSpec > lastSpec)
      return !(done = true);
    spectrum.setBlockID(blockID);
    source.addJDXSpectrum(null, spectrum, forceSub);
    return true;
  }

  /**
   * reads BLOCK data
   * 
   * @param sourceLDRTable
   * @return source
   * @throws JSVException
   */
  private JDXSource getBlockSpectra(Lst<String[]> sourceLDRTable)
      throws JSVException {

    Logger.debug("--JDX block start--");
    String label = "";
    String value = null;
    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    boolean forceSub = false;
    while ((label = t.getLabel()) != null && !label.equals("##TITLE")) {
      value = getValue(label);
      if (isNew && !readHeaderLabel(source, label, value, errorLog, obscure))
        addHeader(sourceLDRTable, t.rawLabel, value);
      if (label.equals("##BLOCKS")) {
        int nBlocks = PT.parseInt(value);
        if (nBlocks > 100 && firstSpec <= 0)
          forceSub = true;
      }
    }
    value = getValue(label);
    // If ##TITLE not found throw Exception
    if (!"##TITLE".equals(label))
      throw new JSVException("Unable to read block source");
    if (isNew)
      source.setHeaderTable(sourceLDRTable);
    source.type = JDXSource.TYPE_BLOCK;
    source.isCompoundSource = true;
    Lst<String[]> dataLDRTable;
    Spectrum spectrum = new Spectrum();
    dataLDRTable = new Lst<String[]>();
    readDataLabel(spectrum, label, value, errorLog, obscure, false);
    try {
      String tmp;
      while ((tmp = t.getLabel()) != null) {
        if ((value = getValue(tmp)) == null && "##END".equals(label)) {
          Logger.debug("##END= " + t.getValue());
          break;
        }
        label = tmp;
        if (isTabularData) {
          setTabularDataType(spectrum, label);
          if (!processTabularData(spectrum, dataLDRTable, false))
            throw new JSVException("Unable to read Block Source");
          continue;
        }
        if (label.equals("##DATATYPE")) {
          if (value.toUpperCase().equals("LINK")) {
            // embedded LINK
            getBlockSpectra(dataLDRTable);
            spectrum = null;
            label = null;
          } else if (value.toUpperCase().startsWith("NMR PEAK")) {
            // we cannot handle XYW tables
            if (ignorePeakTables) {
              done = true;
              return source;
            }
          }
        } else if (label.equals("##NTUPLES") || label.equals("##VARNAME")) {
          getNTupleSpectra(dataLDRTable, spectrum, label);
          spectrum = null;
          label = "";
        }
        if (done)
          break;
        if (spectrum == null) {
          spectrum = new Spectrum();
          dataLDRTable = new Lst<String[]>();
          if (label == "")
            continue;
          if (label == null) {
            label = "##END";
            continue;
          }
        }
        if (value == null) {
          // ##END -- Process Block

          if (spectrum.getXYCoords().length > 0
              && !addSpectrum(spectrum, forceSub))
            return source;
          spectrum = new Spectrum();
          dataLDRTable = new Lst<String[]>();
          continue;
        }
        if (readDataLabel(spectrum, label, value, errorLog, obscure, false))
          continue;

        addHeader(dataLDRTable, t.rawLabel, value);
        if (checkCustomTags(spectrum, label, value))
          continue;
      } // End Source File
    } catch (Exception e) {
      if (!Viewer.isJS)
        e.printStackTrace();
      throw new JSVException(e.getMessage());
    }
    addErrorLogSeparator();
    source.setErrorLog(errorLog.toString());
    Logger.debug("--JDX block end--");
    return source;
  }

//  /**
//   * 
//   * @return ##TITLE or null
//   */
//  private String skipBlock() {
//    String label;
//    while ((label = t.getLabel()) != null && !label.equals("##TITLE"))
//      t.getValue();
//    return label;
//  }


  private void addErrorLogSeparator() {
    if (errorLog.length() > 0
        && errorLog.lastIndexOf(ERROR_SEPARATOR) != errorLog.length()
            - ERROR_SEPARATOR.length())
      logError(ERROR_SEPARATOR);
  }


  /**
   * reads NTUPLE data
   * 
   * @param sourceLDRTable
   * @param spectrum0
   * @param label 
   * 
   * @throws JSVException
   * @return source
   */
  @SuppressWarnings("null")
  private JDXSource getNTupleSpectra(Lst<String[]> sourceLDRTable,
                                     JDXDataObject spectrum0, String label)
      throws JSVException {
    double[] minMaxY = new double[] { Double.MAX_VALUE, Double.MIN_VALUE };
    blockID = Math.random();
    boolean isOK = true;//(spectrum0.is1D() || firstSpec > 0);
    if (firstSpec > 0)
      spectrum0.numDim = 1; // don't display in 2D if only loading some spectra

    boolean isVARNAME = label.equals("##VARNAME");
    if (!isVARNAME) {
      label = "";
    }
    Map<String, Lst<String>> nTupleTable = new Hashtable<String, Lst<String>>();
    String[] plotSymbols = new String[2];

    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    if (isNew) {
      source.type = JDXSource.TYPE_NTUPLE;
      source.isCompoundSource = true;
      source.setHeaderTable(sourceLDRTable);
    }

    // Read NTuple Table
    while (!(label = (isVARNAME ? label : t.getLabel())).equals("##PAGE")) {
      isVARNAME = false;
      StringTokenizer st = new StringTokenizer(t.getValue(), ",");
      Lst<String> attrList = new Lst<String>();
      while (st.hasMoreTokens())
        attrList.addLast(st.nextToken().trim());
      nTupleTable.put(label, attrList);
    }//Finished With Page Data
    Lst<String> symbols = nTupleTable.get("##SYMBOL");
    if (!label.equals("##PAGE"))
      throw new JSVException("Error Reading NTuple Source");
    String page = t.getValue();
    /*
     * 7.3.1 ##PAGE= (STRING).
    This LDR indicates the start of a PAGE which contains tabular data. It may have no
    argument, or it may be omitted when the data consists of one PAGE. When the Data Table
    represents a property like a spectrum or a particular fraction, or at a particular time, or at a
    specific location in two or three dimensional space, the appropriate PAGE VARIABLE
    values will be given as arguments of the ##PAGE= LDR, as in the following examples:
    ##PAGE= N=l $$ Spectrum of first fraction of GCIR run
    ##PAGE= T=10:21 $$ Spectrum of product stream at time: 10:21
    ##PAGE= X=5.2, Y=7.23 $$ Spectrum of known containing 5.2 % X and 7.23% Y
     */

    Spectrum spectrum = null;
    boolean isFirst = true;
    while (!done) {
      if ((label = t.getLabel()).equals("##ENDNTUPLES")) {
        t.getValue();
        break;
      }

      if (label.equals("##PAGE")) {
        page = t.getValue();
        continue;
      }

      // Create and add Spectra
      if (spectrum == null) {
        spectrum = new Spectrum();
        spectrum0.copyTo(spectrum);
        spectrum.setTitle(spectrum0.getTitle());
        if (!spectrum.is1D()) {
          int pt = page.indexOf('=');
          if (pt >= 0)
            try {
              spectrum
                  .setY2D(parseAFFN(page.substring(0, pt), page.substring(pt + 1).trim()));
              String y2dUnits = page.substring(0, pt).trim();
              int i = symbols.indexOf(y2dUnits);
              if (i >= 0)
                spectrum.setY2DUnits(nTupleTable.get("##UNITS").get(i));
            } catch (Exception e) {
              //we tried.            
            }
        }
      }

      Lst<String[]> dataLDRTable = new Lst<String[]>();
      spectrum.setHeaderTable(dataLDRTable);

      while (!label.equals("##DATATABLE")) {
        addHeader(dataLDRTable, t.rawLabel, t.getValue());
        label = t.getLabel();
      }

      boolean continuous = true;
      String line = t.flushLine();
      if (line.trim().indexOf("PEAKS") > 0)
        continuous = false;

      // parse variable list
      int index1 = line.indexOf('(');
      int index2 = line.lastIndexOf(')');
      if (index1 == -1 || index2 == -1)
        throw new JSVException("Variable List not Found");
      String varList = line.substring(index1, index2 + 1);

      int countSyms = 0;
      for (int i = 0; i < symbols.size(); i++) {
        String sym = symbols.get(i).trim();
        if (varList.indexOf(sym) != -1) {
          plotSymbols[countSyms++] = sym;
        }
        if (countSyms == 2)
          break;
      }

      setTabularDataType(spectrum, "##" + (continuous ? "XYDATA" : "PEAKTABLE"));

      if (!readNTUPLECoords(spectrum, nTupleTable, plotSymbols, minMaxY))
        throw new JSVException("Unable to read Ntuple Source");
      if (!spectrum.nucleusX.equals("?"))
        spectrum0.nucleusX = spectrum.nucleusX;
      spectrum0.nucleusY = spectrum.nucleusY;
      spectrum0.freq2dX = spectrum.freq2dX;
      spectrum0.freq2dY = spectrum.freq2dY;
      spectrum0.y2DUnits = spectrum.y2DUnits;
      for (int i = 0; i < sourceLDRTable.size(); i++) {
        String[] entry = sourceLDRTable.get(i);
        String key = JDXSourceStreamTokenizer.cleanLabel(entry[0]);
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          dataLDRTable.addLast(entry);
      }
      if (isOK)
        addSpectrum(spectrum, !isFirst);
      isFirst = false;
      spectrum = null;
    }
    addErrorLogSeparator();
    source.setErrorLog(errorLog.toString());
    Logger.info("NTUPLE MIN/MAX Y = " + minMaxY[0] + " " + minMaxY[1]);
    return source;
  }

  /**
   * 
   * @param spectrum
   * @param label
   * @param value
   * @param errorLog
   * @param obscure
   * @param isHeaderOnly 
   * @return  true to skip saving this key in the spectrum headerTable
   */
  private boolean readDataLabel(JDXDataObject spectrum, String label,
                                       String value, 
                                       SB errorLog, boolean obscure, boolean isHeaderOnly) {
    if (readHeaderLabel(spectrum, label, value, errorLog, obscure))
      return true;

    // NOTE: returning TRUE for these means they are 
    // not included in the header map -- is that what we want?

    label += " ";
    if (("##MINX ##MINY ##MAXX ##MAXY ##FIRSTY ##DELTAX ##DATACLASS ").indexOf(label) >= 0)
      return true;

    // NMR variations: need observedFreq, offset, dataPointNum, and shiftRefType 
    switch (("##FIRSTX  "
          + "##LASTX   "
          + "##NPOINTS "
          + "##XFACTOR "
          + "##YFACTOR "
          + "##XUNITS  "
          + "##YUNITS  "
          + "##XLABEL  "
          + "##YLABEL  "
          + "##NUMDIM  "
          + "##OFFSET  "
          ).indexOf(label)) {
      case 0:
        spectrum.fileFirstX = parseAFFN(label, value);
        return true;
      case 10:
        spectrum.fileLastX = parseAFFN(label, value);
        return true;
      case 20:
        spectrum.nPointsFile = Integer.parseInt(value);
        return true;
      case 30:
        spectrum.xFactor = parseAFFN(label, value);
        return true;
      case 40:
        spectrum.yFactor = parseAFFN(label, value);
        return true;
      case 50:
        spectrum.setXUnits(value);
        return true;
      case 60:
        spectrum.setYUnits(value);
        return true;
      case 70:
        spectrum.setXLabel(value);
        return false; // store in hashtable
      case 80:
        spectrum.setYLabel(value);
        return false; // store in hashtable
      case 90:
        spectrum.numDim = Integer.parseInt(value);
        return true;
      case 100:
        if (spectrum.shiftRefType != JDXDataObject.REF_TYPE_STANDARD) {
          if (spectrum.offset == JDXDataObject.ERROR)
            spectrum.offset = parseAFFN(label, value);
          // bruker doesn't need dataPointNum
          spectrum.dataPointNum = 1;
          // bruker type
          spectrum.shiftRefType = JDXDataObject.REF_TYPE_BRUKER;
        }
        return false;
      default:
        //    if (label.equals("##PATHLENGTH")) {
        //      jdxObject.pathlength = value;
        //      return true;
        //    }

        if (label.length() < 17)
          return false;   
        if (label.equals("##.OBSERVEFREQUENCY ")) {
          spectrum.observedFreq = parseAFFN(label, value);
          return true;
        }
        if (label.equals("##.OBSERVENUCLEUS ")) {
          spectrum.setObservedNucleus(value);
          return true;    
        }
        if ((label.equals("##$REFERENCEPOINT ")) && (spectrum.shiftRefType != JDXDataObject.REF_TYPE_STANDARD)) {
          spectrum.offset = parseAFFN(label, value);
          // varian doesn't need dataPointNum
          spectrum.dataPointNum = 1;
          spectrum.shiftRefType = JDXDataObject.REF_TYPE_VARIAN;
          return false; // save in file  
        }
        if (label.equals("##.SHIFTREFERENCE ")) {
          //TODO: don't save in file??
          if (ignoreShiftReference || !(spectrum.dataType.toUpperCase().contains("SPECTRUM")))
            return true;
          value = PT.replaceAllCharacters(value, ")(", "");
          StringTokenizer srt =   new StringTokenizer(value, ",");
          if (srt.countTokens() != 4)
            return true;
          try {
            srt.nextToken();
            srt.nextToken();
            spectrum.dataPointNum = Integer.parseInt(srt.nextToken().trim());
            spectrum.offset = parseAFFN(label, srt.nextToken().trim());
            if (spectrum.dataPointNum <= 0)
              spectrum.dataPointNum = 1;
            spectrum.shiftRefType = JDXDataObject.REF_TYPE_STANDARD;
          } catch (Exception e) {
          }
          return true;
        }
    }
    return false;
  }

  private static boolean readHeaderLabel(JDXHeader jdxHeader, String label,
                                         String value, SB errorLog,
                                         boolean obscure) {
    switch (("##TITLE###" +
             "##JCAMPDX#" +
             "##ORIGIN##" +
             "##OWNER###" +
             "##DATATYPE" +
             "##LONGDATE" +
             "##DATE####" +
             "##TIME####").indexOf(label + "#")) {
    case 0:
      jdxHeader.setTitle(obscure || value == null || value.equals("") ? "Unknown"
          : value);
      return true;
    case 10:
      jdxHeader.jcampdx = value;
      float version = PT.parseFloat(value);
      if (version >= 6.0 || Float.isNaN(version)) {
        if (errorLog != null)
          errorLog
              .append("Warning: JCAMP-DX version may not be fully supported: "
                  + value);
      }
      return true;
    case 20:
      jdxHeader.origin = (value != null && !value.equals("") ? value
          : "Unknown");
      return true;
    case 30:
      jdxHeader.owner = (value != null && !value.equals("") ? value : "Unknown");
      return true;
    case 40:
      jdxHeader.dataType = value;
      return true;
    case 50:
      jdxHeader.longDate = value;
      return true;
    case 60:
      jdxHeader.date = value;
      return true;
    case 70:
      jdxHeader.time = value;
      return true;
    }
    return false;
  }

  private void setTabularDataType(JDXDataObject spectrum, String label) {
    if (label.equals("##PEAKASSIGNMENTS"))
      spectrum.setDataClass("PEAKASSIGNMENTS");
    else if (label.equals("##PEAKTABLE"))
      spectrum.setDataClass("PEAKTABLE");
    else if (label.equals("##XYDATA"))
      spectrum.setDataClass("XYDATA");
    else if (label.equals("##XYPOINTS"))
      spectrum.setDataClass("XYPOINTS");
//    try {
//      t.readLineTrimmed();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }

  private boolean processTabularData(JDXDataObject spec, Lst<String[]> table, boolean isHeaderOnly)
      throws JSVException {
    spec.setHeaderTable(table);

    if (spec.dataClass.equals("XYDATA")) {
      spec.checkRequiredTokens();
      if (!isHeaderOnly)
        decompressData(spec, null);
      return true;
    }
    if (spec.dataClass.equals("PEAKTABLE") || spec.dataClass.equals("XYPOINTS")) {
      spec.setContinuous(spec.dataClass.equals("XYPOINTS"));
      // check if there is an x and y factor
      try {
        t.readLineTrimmed();
      } catch (IOException e) {
        // ignore
      }
      Coordinate[] xyCoords;

      if (spec.xFactor != JDXDataObject.ERROR
          && spec.yFactor != JDXDataObject.ERROR) {
        String data = t.getValue();
        xyCoords = Coordinate
            .parseDSV(data, spec.xFactor, spec.yFactor);
      } else {
        xyCoords = Coordinate.parseDSV(t.getValue(), 1, 1);
      }
      spec.setXYCoords(xyCoords);
      double fileDeltaX = Coordinate.deltaX(
          xyCoords[xyCoords.length - 1].getXVal(), xyCoords[0].getXVal(),
          xyCoords.length);
      spec.setIncreasing(fileDeltaX > 0);
      return true;
    }
    return false;
  }

  private boolean readNTUPLECoords(JDXDataObject spec, 
                                          Map<String, Lst<String>> nTupleTable,
                                          String[] plotSymbols,
                                          double[] minMaxY) {
    Lst<String> list;
    String label;
    if (spec.dataClass.equals("XYDATA")) {
      // Get Label Values

      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##VARNAME");
      spec.varName = list.get(index2).toUpperCase();

      list = nTupleTable.get(label = "##FACTOR");
      spec.xFactor = parseAFFN(label, list.get(index1));
      spec.yFactor = parseAFFN(label, list.get(index2));

      list = nTupleTable.get(label = "##LAST");
      spec.fileLastX = parseAFFN(label, list.get(index1));

      list = nTupleTable.get(label = "##FIRST");
      spec.fileFirstX = parseAFFN(label, list.get(index1));
      //firstY = parseDouble((String)list.get(index2));

      list = nTupleTable.get("##VARDIM");
      spec.nPointsFile = Integer.parseInt(list.get(index1));

      list = nTupleTable.get("##UNITS");
      spec.setXUnits(list.get(index1));
      spec.setYUnits(list.get(index2));

      if (spec.nucleusX == null && (list = nTupleTable.get("##.NUCLEUS")) != null) {
        spec.setNucleusAndFreq(list.get(0), false);
        spec.setNucleusAndFreq(list.get(index1), true);
      } else {
        if (spec.nucleusX == null)
          spec.nucleusX = "?";
      }

      decompressData(spec, minMaxY);
      return true;
    }
    if (spec.dataClass.equals("PEAKTABLE") || spec.dataClass.equals("XYPOINTS")) {
      spec.setContinuous(spec.dataClass.equals("XYPOINTS"));
      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##UNITS");
      spec.setXUnits(list.get(index1));
      spec.setYUnits(list.get(index2));
      spec.setXYCoords(Coordinate.parseDSV(t.getValue(), spec.xFactor, spec.yFactor));
      return true;
    }
    return false;
  }

  /**
   * See JCAMP-DX standard 4.5.3 AFFN (ASCII FREE FORMAT NUMERIC)
   * @param label
   * @param val
   * @return val
   */
  private double parseAFFN(String label, String val) {
    // only E+/-mm or E+/-mmm 
    // logging an error if anything else
    int pt = val.indexOf("E");
    if (pt > 0) {
      //.nnE+mm
      //...01234
      //.nnE-mmm
      //...012345
      int len = val.length();
      char ch;
      switch (len - pt) {
      case 2:
      case 3:
        // 1.0E6
        // 1.0E06
        // 1.0E-6
        logError("Warning - " + label + " value " + val + " is not of the format xxxE[+/-]nn or xxxE[+/-]nnn (spec. 4.5.3) -- warning only; accepted");
        break;
      case 4:
      case 5:
        // 1.0E-06
        // 1.0E-006
        if ((ch = val.charAt(pt + 1)) == '+' || ch == '-')
          break;
        //$FALL-THROUGH$
      default: // ignore E part and flag error
        logError("Error - " + label + " value " + val + " is not of the format xxxE[+/-]nn or xxxE[+/-]nnn (spec. 4.5.3) -- " + val.substring(pt) + " ignored!");
        val = val.substring(0, pt);
      }
    }
    return Double.parseDouble(val);
  }

  private void decompressData(JDXDataObject spec, double[] minMaxY) {

    int errPt = errorLog.length();
    spec.setIncreasing(spec.fileLastX > spec.fileFirstX);
    spec.setContinuous(true);
    JDXDecompressor decompressor = new JDXDecompressor(t, spec.fileFirstX,
        spec.fileLastX, spec.xFactor, spec.yFactor, spec.nPointsFile);

    long t = System.currentTimeMillis();
    Coordinate[] xyCoords = decompressor.decompressData(errorLog);
    if (Logger.debugging)
      Logger.debug(
          "decompression time = " + (System.currentTimeMillis() - t) + " ms");
    spec.setXYCoords(xyCoords);
    double d = decompressor.getMinY();
    if (minMaxY != null) {
      if (d < minMaxY[0])
        minMaxY[0] = d;
      d = decompressor.getMaxY();
      if (d > minMaxY[1])
        minMaxY[1] = d;
    }
    double freq = (Double.isNaN(spec.freq2dX) ? spec.observedFreq
        : spec.freq2dX);
    // apply offset
    boolean isHz = freq != JDXDataObject.ERROR
        && spec.getXUnits().toUpperCase().equals("HZ");
    if (spec.offset != JDXDataObject.ERROR && freq != JDXDataObject.ERROR
        && spec.dataType.toUpperCase().contains("SPECTRUM")
        && spec.jcampdx.indexOf("JEOL") < 0 // BH 2020.09.16 J Muzyka Centre College
    ) {
      Coordinate.applyShiftReference(xyCoords, spec.dataPointNum,
          spec.fileFirstX, spec.fileLastX, spec.offset, isHz ? freq : 1,
          spec.shiftRefType);
    }

    if (isHz) {
      Coordinate.applyScale(xyCoords, (1.0 / freq), 1);
      spec.setXUnits("PPM");
      spec.setHZtoPPM(true);
    }
    if (errorLog.length() != errPt) {
      double fileDeltaX = Coordinate.deltaX(spec.fileLastX, spec.fileFirstX,
          spec.nPointsFile);
      logError(spec.getTitle());
      logError("firstX from Header " + spec.fileFirstX);
      logError("lastX from Header " + spec.fileLastX + " Found "
          + decompressor.lastX);
      logError("deltaX from Header " + fileDeltaX);
      logError("Number of points in Header " + spec.nPointsFile + " Found "
          + decompressor.getNPointsFound());
    } else {
      //logError("No Errors decompressing data");
    }

    if (Logger.debugging) {
      System.err.println(errorLog.toString());
    }

  }

  public static void addHeader(Lst<String[]> table, String label, String value) {
    String[] entry;
    for (int i = 0; i < table.size(); i++)
      if ((entry = table.get(i))[0].equals(label)) {
        entry[1] = value;
        return;
      }
    table.addLast(new String[] { label, value, JDXSourceStreamTokenizer.cleanLabel(label) });
  }


  ////// JCAMP-DX/MOL reading //////
  
  private boolean checkCustomTags(Spectrum spectrum, String label,
      String value) throws JSVException {
    if (label.length() > 10)
      label = label.substring(0, 10);
    if (spectrum == null)
      System.out.println(label);
    else
      modelSpectrum = spectrum;
    int pt = "##$MODELS ##$PEAKS  ##$SIGNALS##$MOLFILE##PEAKASSI##$UVIRASS##$MSFRAGM".indexOf(label);
    //        0         10        20        30        40        50        60        
    if (pt < 0)
      return false;
    getMpr().set(this, filePath, null);
    try {
      reader = new BufferedReader(new StringReader(value));
      switch (pt) {
      case 0:
        mpr.readModels();
        break;
      case 10:
      case 20:
        peakData = new Lst<PeakInfo>();
        source.peakCount += mpr.readPeaks(pt == 20, source.peakCount);
        break;
      case 30:
        // moldata - skip
        acdAssignments = new Lst<String[]>();
        acdMolFile = PT.rep(value, "$$ Empty String", "");
        break;
      case 40:
      case 50:
      case 60:
        acdAssignments = mpr.readACDAssignments(spectrum.nPointsFile, pt == 40);
        break;
      }       
    } catch (Exception e) {
      throw new JSVException(e.getMessage());
    } finally {
      reader = null;
    }
    return true;
  }

  // methods called by JDXModelPeakParser()

  private JmolJDXMOLParser getMpr() {
    return (mpr == null?
      mpr = (JmolJDXMOLParser) JSViewer.getInterface("org.jmol.jsv.JDXMOLParser") : mpr);
  }

  @Override
  public String rd() throws Exception {
  return reader.readLine();
  }

  private Lst<PeakInfo> peakData;

  @Override
  public void setSpectrumPeaks(int nH, String piUnitsX, String piUnitsY) {
    modelSpectrum.setPeakList(peakData, piUnitsX, piUnitsY);
    if (modelSpectrum.isNMR())
      modelSpectrum.setNHydrogens(nH);    
  }

  @Override
  public void addPeakData(String info) {
    if (peakData == null)
      peakData = new Lst<PeakInfo>();
    peakData.addLast(new PeakInfo(info));
  }


  @Override
  public void processModelData(String id, String data, String type,
      String base, String last, float modelScale, float vibScale, boolean isFirst)
      throws Exception {
    // Jmol only
  }


  @Override
  public String discardLinesUntilContains(String containsMatch)
      throws Exception {
    String line;
    while ((line = rd()) != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  @Override
  public String discardLinesUntilContains2(String s1, String s2)
      throws Exception {
    String line;
    while ((line = rd()) != null && line.indexOf(s1) < 0 && line.indexOf(s2) < 0) {
    }
    return line;
  }

  @Override
  public String discardLinesUntilNonBlank() throws Exception {
    String line;
    while ((line = rd()) != null && line.trim().length() == 0) {
    }
    return line;
  }
}
