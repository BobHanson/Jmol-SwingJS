/* Copyright (c) 2002-2008 The University of the West Indies
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

package jspecview.export;

import java.io.IOException;


import javajs.util.OC;
import javajs.util.Lst;

import jspecview.common.Coordinate;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;

/**
 * The XMLExporter should be a totally generic exporter
 * Subclassed by AML and CML
 *
 * no longer uses Velocity.
 *
 * @author Bob Hanson, hansonr@stolaf.edu
 *
 */
abstract class XMLExporter extends FormExporter {

  protected boolean continuous;
  protected String title;
  protected String ident;
  protected String state;
  protected String xUnits;
  protected String yUnits;
  protected String xUnitFactor = "";
  protected String xUnitExponent = "1";
  protected String xUnitLabel;
  protected String yUnitLabel;
  protected String datatype;
  protected String owner;
  protected String origin;
  protected String spectypeInitials = "";
  protected String longdate;
  protected String date;
  protected String time;
  protected String vendor = "";
  protected String model = "";
  protected String resolution = "";
  protected String pathlength = "";
  protected String molform = "";
  protected String bp = "";
  protected String mp = "";
  protected String casRN = "";
  protected String casName = "";
  protected String obNucleus = "";
 
  protected double obFreq;
  protected double firstX;
  protected double lastX;
  protected double deltaX;

  protected String solvRef = "";
  protected String solvName = "";
  
  protected int startIndex;
  protected int endIndex;
  protected Coordinate[] xyCoords;
  protected int npoints;

  protected Lst<Coordinate> newXYCoords = new Lst<Coordinate>();

  protected boolean setup(JSViewer viewer, Spectrum spec, OC out, int startIndex,
                             int endIndex) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    initForm(viewer, out);
    return setParameters(spec);
  }

  protected boolean setParameters(Spectrum spec) {

    continuous = spec.isContinuous();

    // no template ready for Peak Tables so exit
    if (!continuous)
      return false;

    xyCoords = spec.getXYCoords();
    npoints = endIndex - startIndex + 1;
    for (int i = startIndex; i <= endIndex; i++)
      newXYCoords.addLast(xyCoords[i]);

    title = spec.getTitle();

    // QUESTION: OK to set units UpperCase for both AnIML and CML?

    xUnits = spec.getXUnits().toUpperCase();
    yUnits = spec.getYUnits().toUpperCase();

    if (xUnits.equals("1/CM")) {
      xUnitLabel = "1/cm";
      xUnitFactor = "0.01";
      xUnitExponent = "-1";
    } else if (xUnits.equals("UM") || xUnits.equals("MICROMETERS")) {
      xUnitLabel = "um";
      xUnitFactor = "0.000001";
    } else if (xUnits.equals("NM") || xUnits.equals("NANOMETERS")
        || xUnits.equals("WAVELENGTH")) {
      xUnitLabel = "nm";
      xUnitFactor = "0.000000001";
    } else if (xUnits.equals("PM") || xUnits.equals("PICOMETERS")) {
      xUnitLabel = "pm";
      xUnitFactor = "0.000000000001";
    } else {
      xUnitLabel = "Arb. Units";
      xUnitFactor = "";
    }

    yUnitLabel = (yUnits.equals("A") || yUnits.equals("ABS")
        || yUnits.equals("ABSORBANCE") || yUnits.equals("AU")
        || yUnits.equals("AUFS") || yUnits.equals("OPTICAL DENSITY") ? "Absorbance"
        : yUnits.equals("T") || yUnits.equals("TRANSMITTANCE") ? "Transmittance"
            : yUnits.equals("COUNTS") || yUnits.equals("CTS") ? "Counts"
                : "Arb. Units");

    owner = spec.getOwner();
    origin = spec.getOrigin();
    time = spec.getTime();

    longdate = spec.getLongDate();
    date = spec.getDate();
    if ((longdate.equals("")) || (date.equals("")))
      longdate = currentTime;
    if ((date.length() == 8) && (date.charAt(0) < '5'))
      longdate = "20" + date + " " + time;
    if ((date.length() == 8) && (date.charAt(0) > '5'))
      longdate = "19" + date + " " + time;

    //pathlength = spec.getPathlength(); // ignored
    obFreq = spec.getObservedFreq();
    firstX = xyCoords[startIndex].getXVal();
    lastX = xyCoords[endIndex].getXVal();
    deltaX = spec.getDeltaX();
    datatype = spec.getDataType();
    if (datatype.contains("NMR")) {
      firstX *= obFreq; // NMR stored internally as ppm
      lastX *= obFreq;
      deltaX *= obFreq; // convert to Hz before exporting
    }

    // these may come back null, but context.put() turns that into ""
    // still, one must check for == null in tests here.

    setParams(spec.getHeaderTable());
    return true;
  }

  private final static String[] params = {
    "##STATE",
    "##RESOLUTION",
    "##SPECTROMETER",
    "##$MANUFACTURER",
    "##MOLFORM",
    "##CASREGISTRYNO",
    "##CASNAME",
    "##MP",
    "##BP",
    "##.OBSERVENUCLEUS",
    "##.SOLVENTNAME",
    "##.SOLVENTREFERENCE"
    }; // should really try to parse info from SHIFTREFERENCE
  
  private static final int PARAM_STATE = 0;
  private static final int PARAM_RESOLUTION = 1;
  private static final int PARAM_SPECTROMETER = 2;
  private static final int PARAM_MANUFACTURER = 3;
  private static final int PARAM_MOLFORM = 4;
  private static final int PARAM_CASREGISTRYNO = 5;
  private static final int PARAM_CASNAME = 6;
  private static final int PARAM_MP = 7;
  private static final int PARAM_BP = 8;
  private static final int PARAM_OBSERVENUCLEUS = 9;
  private static final int PARAM_SOLVENTNAME = 10;
  private static final int PARAM_SOLVENTREFERENCE = 11;
  
  private static int getParamIndex(String label) {
    for (int i = 0; i < params.length; i++)
      if (params[i].equalsIgnoreCase(label))
        return i;
    return -1;
  }

  private void setParams(Lst<String[]> table) {
    for (int i = 0; i < table.size(); i++) {
      String[] entry = table.get(i);
      String val = entry[1];
      switch (getParamIndex(entry[0])) {
      case PARAM_STATE:
        state = val;
        break;
      case PARAM_RESOLUTION:
        resolution = val;
        break;
      case PARAM_SPECTROMETER:
        model = val;
        break;
      case PARAM_MANUFACTURER:
        vendor = val;
        break;
      case PARAM_MOLFORM:
        molform = val;
        break;
      case PARAM_CASREGISTRYNO:
        casRN = val;
        break;
      case PARAM_CASNAME:
        casName = val;
        break;
      case PARAM_MP:
        mp = val;
        break;
      case PARAM_BP:
        bp = val;
        break;
      case PARAM_OBSERVENUCLEUS:
        obNucleus = val;
        break;
      case PARAM_SOLVENTNAME:
        solvName = val;
        break;
      case PARAM_SOLVENTREFERENCE:
        solvRef = val;
        break;
      }
    }
  }

  protected void setContext() {
    context.put("continuous", Boolean.valueOf(continuous));
    context.put("file", out.getFileName() + "");
    context.put("title", title);
    context.put("ident", ident);
    context.put("state", state);
    context.put("firstX", new Double(firstX));
    context.put("lastX", new Double(lastX));
    context.put("xyCoords", newXYCoords);
    context.put("xdata_type", "Float32");
    context.put("ydata_type", "Float32");
    context.put("npoints", Integer.valueOf(npoints));
    context.put("xencode", "avs");
    context.put("yencode", "ivs");
    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);
    context.put("xUnitLabel", xUnitLabel);
    context.put("yUnitLabel", yUnitLabel);
    context.put("specinits", spectypeInitials);
    context.put("deltaX", new Double(deltaX));
    context.put("owner", owner);
    context.put("origin", origin);
    context.put("timestamp", longdate);
    context.put("DataType", datatype);
    context.put("currenttime", currentTime);
    context.put("resolution", resolution);
    context.put("pathlength", pathlength); //required for UV and IR
    context.put("molform", molform);
    context.put("CASrn", casRN);
    context.put("CASn", casName);
    context.put("mp", mp);
    context.put("bp", bp);
    context.put("ObFreq", new Double(obFreq));
    context.put("ObNucleus", obNucleus);
    context.put("SolvName", solvName);
    context.put("SolvRef", solvRef);
    context.put("vendor", vendor);
    context.put("model", model);
  }

  String writeFormType(String type) throws IOException {
    return writeForm(type + (datatype.contains("NMR") ? "_nmr" : "_tmp") + ".vm");
  }

}
