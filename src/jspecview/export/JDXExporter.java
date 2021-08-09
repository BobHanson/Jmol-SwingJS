/* Copyright (c) 2002-2012 The University of the West Indies
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

import javajs.util.DF;
import javajs.util.OC;
import javajs.util.Lst;
import javajs.util.PT;

import jspecview.api.JSVExporter;
import jspecview.common.Coordinate;
import jspecview.common.ExportType;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.source.JDXReader;
import jspecview.source.JDXDataObject;

/**
 * class <code>JDXExporter</code> contains methods for exporting a
 * JCAMP-DX Spectrum in one of the compression formats DIF, FIX, PAC, SQZ or
 * as x, y values.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 */

public class JDXExporter implements JSVExporter {

	public static final String newLine = System.getProperty("line.separator");
	private OC out;
	private ExportType type;
	private Spectrum spectrum;
	private JSViewer vwr;

	public JDXExporter() {
		
	}
  /**
   * The factor divisor used in compressing spectral data in one of DIF, SQZ,
   * PAC and FIX formats
   */
  private static final double FACTOR_DIVISOR = 1000000;

  /**
   * Exports spectrum in one of several formats
   * @param type
   * @param out
   * @param startIndex
   * @param endIndex
   * @param spectrum the spectrum
   * @return data if path is null
   * @throws IOException
   */
  @Override
	public String exportTheSpectrum(JSViewer viewer, ExportType type, OC out, Spectrum spectrum, int startIndex, int endIndex, PanelData pd, boolean asBase64) throws IOException{
  	this.out = out;
  	this.type = type;
  	this.spectrum = spectrum;
  	this.vwr = viewer;
    toStringAux(startIndex, endIndex);
    out.closeChannel();
    return "OK " + out.getByteCount() + " bytes";
  }

  /**
   * Auxiliary function for the toString functions
   * 
   * @param startIndex
   *        the start Coordinate Index
   * @param endIndex
   *        the end Coordinate Index
   */
  private void toStringAux(int startIndex, int endIndex) {

    Coordinate[] newXYCoords = spectrum.getXYCoords();
    String tabDataSet = "", tmpDataClass = "XYDATA";

    if (spectrum.isHZtoPPM()) {
      // convert back to Hz.
      Coordinate[] xyCoords = newXYCoords;
      newXYCoords = new Coordinate[xyCoords.length];
      for (int i = 0; i < xyCoords.length; i++)
        newXYCoords[i] = xyCoords[i].copy();
      Coordinate.applyScale(newXYCoords, spectrum.getObservedFreq(), 1);
    }

    double xCompFactor = spectrum.getXFactor();
    boolean isIntegerX = areIntegers(newXYCoords, startIndex, endIndex, 1.0, true);
    if (!isIntegerX && !areIntegers(newXYCoords, startIndex, endIndex, xCompFactor, true))
      xCompFactor = 1;
    
    double minY = Coordinate.getMinY(newXYCoords, startIndex, endIndex);
    double maxY = Coordinate.getMaxY(newXYCoords, startIndex, endIndex);
    double yCompFactor = spectrum.getYFactor();

    switch (type) {
    case XY:
      yCompFactor = 1;
      tmpDataClass = (spectrum.isContinuous() ?  "XYDATA" : "XYPOINTS");
      break;
    case PAC:
      yCompFactor = 1;
      break;
    default:
      boolean isIntegerY = areIntegers(newXYCoords, startIndex, endIndex, 1.0, false);
      if (!isIntegerY && !areIntegers(newXYCoords, startIndex, endIndex, yCompFactor, false)) {
        yCompFactor = (maxY - minY) / FACTOR_DIVISOR;
      }
      break;
    }
    int step = 1;
    if (spectrum.isExportXAxisLeftToRight() != (spectrum.getFirstX() < spectrum.getLastX())) {
      int t = startIndex;
      startIndex = endIndex;
      endIndex = t;
      step = -1;
    }
    switch (type) {
    case DIF:
    case DIFDUP:
      tabDataSet = JDXCompressor.compressDIF(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor, type == ExportType.DIFDUP);
      break;
    case FIX:
      tabDataSet = JDXCompressor.compressFIX(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case PAC:
      tabDataSet = JDXCompressor.compressPAC(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case SQZ:
      tabDataSet = JDXCompressor.compressSQZ(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case XY:
      tabDataSet = JDXCompressor.getXYList(newXYCoords, startIndex, endIndex, step);
      break;
    default:
			break;
    }

    String varList = JDXReader.getVarList(tmpDataClass);
    getHeaderString(tmpDataClass, minY, maxY,
        xCompFactor, yCompFactor, startIndex, endIndex);
    out.append("##" + tmpDataClass + "= " + varList + newLine);
    out.append(tabDataSet);
    out.append("##END=");
  }

  /**
   * Returns the String for the header of the spectrum
   * @param tmpDataClass
   *        the dataclass
   * @param minY 
   * @param maxY 
   * @param tmpXFactor
   *        the x factor
   * @param tmpYFactor
   *        the y factor
   * @param startIndex
   *        the index of the starting coordinate
   * @param endIndex
   *        the index of the ending coordinate
   */
  private void getHeaderString(String tmpDataClass,
                                        double minY, double maxY,
                                        double tmpXFactor, double tmpYFactor,
                                        int startIndex, int endIndex) {

    //final String CORE_STR = "TITLE,ORIGIN,OWNER,DATE,TIME,DATATYPE,JCAMPDX";

    // start of header
    out.append("##TITLE= ").append(spectrum.getTitle()).append(
        newLine);
    out.append("##JCAMP-DX= 5.01").append(newLine); /*+ getJcampdx()*/
    out.append("##DATA TYPE= ").append(spectrum.getDataType()).append(
        newLine);
    out.append("##DATA CLASS= ").append(tmpDataClass).append(
        newLine);
    out.append("##ORIGIN= ").append(spectrum.getOrigin()).append(
        newLine);
    out.append("##OWNER= ").append(spectrum.getOwner()).append(
        newLine);
    String d = spectrum.getDate();
    String longdate = "";
    String currentTime = vwr.apiPlatform.getDateFormat(null);
    if (spectrum.getLongDate().equals("") || d.length() != 8) {
      longdate = currentTime + " $$ export date from JSpecView";
    } else if (d.length() == 8) { // give a 50 year window; Y2K compliant
      longdate = (d.charAt(0) < '5' ? "20" : "19") + d + " " + spectrum.getTime();
    } else {
      longdate = spectrum.getLongDate();
    }
    out.append("##LONGDATE= ").append(longdate).append(newLine);

    // optional header
    Lst<String[]> headerTable = spectrum.getHeaderTable();
    for (int i = 0; i < headerTable.size(); i++) {
      String[] entry = headerTable.get(i);
      String label = entry[0];
      String dataSet = entry[1];
      String nl = (dataSet.startsWith("<") && dataSet.contains("</") ? newLine
          : "");
      out.append(label).append("= ").append(nl).append(dataSet).append(
          newLine);
    }
    double observedFreq = spectrum.getObservedFreq();
    if (!spectrum.is1D())
      out.append("##NUM DIM= ").append("" + spectrum.getNumDim()).append(
          newLine);
    if (observedFreq != JDXDataObject.ERROR)
      out.append("##.OBSERVE FREQUENCY= ").append("" + observedFreq).append(
          newLine);
    String nuc = spectrum.getObservedNucleus();
    if (!"".equals(nuc))
      out.append("##.OBSERVE NUCLEUS= ").append(nuc).append(
          newLine);
    //now need to put pathlength here

    // last part of header

    //boolean toHz = (observedFreq != JDXSpectrum.ERROR && !spec.getDataType()
      //  .toUpperCase().contains("FID"));
    out.append("##XUNITS= ").append(spectrum.isHZtoPPM() ? "HZ" : spectrum.getXUnits()).append(
        newLine);
    out.append("##YUNITS= ").append(spectrum.getYUnits()).append(
        newLine);
    out.append("##XFACTOR= ").append(fixExponentInt(tmpXFactor))
        .append(newLine);
    out.append("##YFACTOR= ").append(fixExponentInt(tmpYFactor))
        .append(newLine);
    double f = (spectrum.isHZtoPPM() ? observedFreq : 1);
    Coordinate[] xyCoords = spectrum.getXYCoords();
    out.append("##FIRSTX= ").append(
        fixExponentInt(xyCoords[startIndex].getXVal() * f)).append(
        newLine);
    out.append("##FIRSTY= ").append(
        fixExponentInt(xyCoords[startIndex].getYVal())).append(
        newLine);
    out.append("##LASTX= ").append(
        fixExponentInt(xyCoords[endIndex].getXVal() * f)).append(
        newLine);
    out.append("##NPOINTS= ").append("" + (Math.abs(endIndex - startIndex) + 1))
        .append(newLine);
    out.append("##MINY= ").append(fixExponentInt(minY)).append(
        newLine);
    out.append("##MAXY= ").append(fixExponentInt(maxY)).append(
        newLine);
  }

  private static boolean areIntegers(Coordinate[] xyCoords, int startIndex,
                                     int endIndex, double factor, boolean isX) {
    for (int i = startIndex; i <= endIndex; i++) {
      double x = (isX ? xyCoords[i].getXVal() : xyCoords[i].getYVal()) / factor;
      if (isAlmostInteger(x))
          return false;
    }
    return true;
  }

	private static boolean isAlmostInteger(double x) {
	  return (x != 0 && Math.abs(x - Math.floor(x)) / x > 1e-8);
	}

	private static String fixExponentInt(double x) {
	  return (x == Math.floor(x) ? String.valueOf((int) x) : PT.rep(fixExponent(x), "E+00", ""));
	}

	/**
	 * JCAMP-DX requires 1.5E[+|-]nn or 1.5E[+|-]nnn only
	 * not Java's 1.5E3 or 1.5E-2
	 * 
	 * @param x
	 * @return exponent fixed
	 */
	private static String fixExponent(double x) {
	  String s = DF.formatDecimalDbl(x, -7); // "0.000000"
	  int pt = s.indexOf("E");
	  if (pt < 0) {
	    return s;
	  }
	  // 4.3E+3
	  // 4.3E-3
	  if (s.length() == pt + 3) 
	    s = s.substring(0, pt + 2) + "0" + s.substring(pt + 2);
	  return s;
	}


}
