/* Copyright (c) 2007-2009 The University of the West Indies
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

import javajs.util.Lst;
import javajs.util.SB;



import org.jmol.util.Logger;

import jspecview.api.SourceReader;
import jspecview.common.Coordinate;
import jspecview.common.Spectrum;

/**
 * Representation of a XML Source.
 * @author Craig Walters
 * @author Prof. Robert J. Lancashire
 */

abstract class XMLReader implements SourceReader {

  abstract protected JDXSource getXML(BufferedReader br);
  
  abstract protected boolean processTag(int tagId) throws Exception;
  abstract protected void processEndTag(int tagId) throws Exception;

  protected JDXSource source;
  protected String filePath = "";
  
  protected XMLParser parser;

  protected String tagName = "START", attrList = "",
      title = "", owner = "UNKNOWN", origin = "UNKNOWN";
  protected String tmpEnd = "END", molForm = "", techname = "";
  protected int npoints = -1, samplenum = -1;
  protected double[] yaxisData;
  protected double[] xaxisData;
  protected String xUnits = "";
  protected String yUnits = "ARBITRARY UNITS";
  protected String vendor = "na", modelType = "MODEL UNKNOWN", LongDate = "";
  protected String pathlength = "na", identifier = "", plLabel = "";
  protected String resolution = "na", resLabel = "", LocName = "";
  protected String LocContact = "", casName = "";
  protected String sampleowner = "", obNucleus = "", StrObFreq = "";
  protected boolean increasing = false, continuous = false;
  protected int ivspoints, evspoints, sampleRefNum = 0;
  protected double deltaX = JDXDataObject.ERROR;
  protected double xFactor = JDXDataObject.ERROR;
  protected double yFactor = JDXDataObject.ERROR;
  protected double firstX = JDXDataObject.ERROR;
  protected double lastX = JDXDataObject.ERROR;
  protected double firstY = JDXDataObject.ERROR;
  protected double obFreq = JDXDataObject.ERROR;
  protected double refPoint = JDXDataObject.ERROR;
  protected String casRN = "";
  protected String sampleID;
  protected SB errorLog = new SB();

  public XMLReader() {
  	// for reflection
  }
  
	@Override
	public JDXSource getSource(String filePath, BufferedReader br) {
		this.filePath = filePath;
		return getXML(br);
	}

  protected void getSimpleXmlReader(BufferedReader br) {
    parser = new XMLParser(br);
  }

  protected void checkStart() throws Exception {
    if (parser.peek() == XMLParser.START_ELEMENT)
      return;
    String errMsg = "Error: XML <xxx> not found at beginning of file; not an XML document?";
    errorLog.append(errMsg);
    throw new IOException(errMsg);
  }

  protected void populateVariables() {
    // end of import of CML document
    // now populate all the JSpecView spectrum variables.....

    Lst<String[]> LDRTable = new Lst<String[]>();
    Spectrum spectrum = new Spectrum();

    spectrum.setTitle(title);
    spectrum.setJcampdx("5.01");
    spectrum.setDataClass("XYDATA");
    spectrum.setDataType(techname);
    spectrum.setContinuous(continuous);
    spectrum.setIncreasing(increasing);
    spectrum.setXFactor(xFactor);
    spectrum.setYFactor(yFactor);
    spectrum.setLongDate(LongDate);
    spectrum.setOrigin(origin);
    spectrum.setOwner(owner);
    //spectrum.setPathlength(pathlength);

    //  now fill in what we can of a HashMap with parameters from the CML file
    //  syntax is:
    //      JDXFileReader.addHeader(LDRTable, )
    //      Key kk = new Key;
    JDXReader.addHeader(LDRTable, "##PATHLENGTH", pathlength);
    JDXReader.addHeader(LDRTable, "##RESOLUTION", resolution);
    if (!StrObFreq.equals(""))
      JDXReader.addHeader(LDRTable, "##.OBSERVEFREQUENCY", StrObFreq);
    if (!obNucleus.equals(""))
      JDXReader.addHeader(LDRTable, "##.OBSERVENUCLEUS", obNucleus);
    JDXReader.addHeader(LDRTable, "##$MANUFACTURER", vendor);
    if (!casRN.equals(""))
      JDXReader.addHeader(LDRTable, "##CASREGISTRYNO", casRN);
    if (!molForm.equals(""))
      JDXReader.addHeader(LDRTable, "##MOLFORM", molForm);
    if (!modelType.equals(""))
      JDXReader.addHeader(LDRTable, "##SPECTROMETER/DATA SYSTEM", modelType);

    //etc etc.
    spectrum.setHeaderTable(LDRTable);

    double xScale = 1; // NMR data stored internally as ppm
    if (obFreq != JDXDataObject.ERROR) {
      spectrum.setObservedFreq(obFreq);
      if (xUnits.toUpperCase().equals("HZ")) {
        xUnits = "PPM";
        spectrum.setHZtoPPM(true);
        xScale = obFreq;
      }
    }

    Coordinate[] xyCoords = new Coordinate[npoints];

    //   for ease of plotting etc. all data is stored internally in increasing order
    for (int x = 0; x < npoints; x++)
      xyCoords[x] = new Coordinate().set(xaxisData[x] / xScale, yaxisData[x]);

    if (!increasing)
      xyCoords = Coordinate.reverse(xyCoords);
      
    spectrum.setXUnits(xUnits);
    spectrum.setYUnits(yUnits);

    spectrum.setXYCoords(xyCoords);
    source.addJDXSpectrum(filePath, spectrum, false);
  }

  protected boolean checkPointCount() {
    //test to see if we have any contiuous data to plot
    //if not, then stop
    if (continuous && npoints < 5) {
      System.err.println("Insufficient points to plot");
      errorLog.append("Insufficient points to plot \n");
      source.setErrorLog(errorLog.toString());
      return false;
    }
    return true;
  }


  protected void processErrors(String type) {
    // for ease of processing later, return a source rather than a spectrum
    //    return XMLSource.getXMLInstance(spectrum);
    //factory = null;
    parser = null;
    if (errorLog.length() > 0) {
      errorLog.append("these errors were found in " + type + " \n");
      errorLog.append(JDXReader.ERROR_SEPARATOR);
    }
    source.setErrorLog(errorLog.toString());
  }

  final static String[] tagNames = {
    // aml:
    "audittrail",
    "experimentstepset",
    "sampleset",
    "xx result",
    // cml:
    "spectrum",
    "metadatalist",
    "conditionlist",
    "parameterlist",
    "sample",
    "spectrumdata",
    "peaklist",
    // not processed in XMLSource, only subclasses thereof
    "author",
    "peaklist"
  };

  final static int AML_0 = 0;
  final static int AML_AUDITTRAIL = 0;
  final static int AML_EXPERIMENTSTEPSET = 1;
  final static int AML_SAMPLESET = 2;
  final static int AML_RESULT = 3;
  final static int AML_1 = 3;

  final static int CML_0 = 4;
  final static int CML_SPECTRUM = 4;
  final static int CML_METADATALIST = 5;
  final static int CML_CONDITIONLIST = 6;
  final static int CML_PARAMETERLIST = 7;
  final static int CML_SAMPLE = 8;
  final static int CML_SPECTRUMDATA = 9;
  final static int CML_PEAKLIST = 10;
  final static int CML_1 = 10;

  final static int AML_AUTHOR = 11;
  final static int CML_PEAKLIST2 = 12;

  protected void processXML(int i0, int i1) throws Exception {
    while (parser.hasNext()) {
      if (parser.nextEvent() != XMLParser.START_ELEMENT)
        continue;
      String theTag = parser.getTagName();
      boolean requiresEndTag = parser.requiresEndTag();
      if (Logger.debugging)
        Logger.info(tagName);
      for (int i = i0; i <= i1; i++)
        if (theTag.equals(tagNames[i])) {
          process(i, requiresEndTag);
          break;
        }
    }
  }

  /**
   * Process the audit XML events
   * @param tagId
   * @param requiresEndTag
   */
  protected void process(int tagId, boolean requiresEndTag) {
    String thisTagName = tagNames[tagId];
    try {
      tagName = parser.getTagName();
      attrList = parser.getAttributeList();
      if (!processTag(tagId) || !requiresEndTag)
        return;
      while (parser.hasNext()) {
        switch (parser.nextEvent()) {
        default:
          continue;
        case XMLParser.END_ELEMENT:
          if (parser.getEndTag().equals(thisTagName)) {
            processEndTag(tagId);
            return;
          }
          continue;
        case XMLParser.START_ELEMENT:
          break;
        }
        tagName = parser.getTagName();
        if (tagName.startsWith("!--"))
          continue;
        attrList = parser.getAttributeList();
        if (!processTag(tagId))
          return;
      }
    } catch (Exception e) {
      String msg = "error reading " + tagName + " section: " + e + "\n" + e.getStackTrace();
      Logger.error(msg);
      errorLog.append(msg + "\n");
    }
  }

}
