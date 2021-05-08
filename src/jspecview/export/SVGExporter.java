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

package jspecview.export;


import java.io.IOException;
import java.util.Map;
import java.util.Hashtable;

import javajs.api.GenericColor;
import javajs.util.CU;
import javajs.util.DF;
import javajs.util.OC;
import javajs.util.Lst;


import org.jmol.util.Logger;

import jspecview.common.ColorParameters;
import jspecview.common.Coordinate;
import jspecview.common.ExportType;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ScaleData;
import jspecview.common.ScriptToken;

/**
 * class <code>SVGExporter</code> contains static methods to export a Graph as
 * as SVG. Uses a template file called 'plot.vm'. So any changes in design should
 * be done in this file.
 * 
 * Modified 
 * 6 Oct 2010  added lastX for Inkscape SVG export so baseline could be printed
 * 
 * 2021.05.08 Bob Hanson fixes SVG export for HTML5
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 */
public class SVGExporter extends FormExporter {

  private static int svgWidth = 850;
  private static int svgHeight = 400;
  private static int leftInset = 100;
  private static int rightInset = 200;
  private static int bottomInset = 80;
  private static int topInset = 20;

  public SVGExporter() {
  	
  }
  
	/**
	 * Export a Graph as SVG to a file given by fileName
	 * @param out
	 *          the file path
	 * @param spec
	 *          the Graph
	 * @param startIndex
	 * @param endIndex
	 * @param mode
	 *          TODO
	 * 
	 * @return data if fileName is null
	 * @throws IOException
	 */
	@Override
	public String exportTheSpectrum(JSViewer viewer, ExportType mode,
			OC out, Spectrum spec, int startIndex, int endIndex,
			PanelData pd, boolean asBase64) throws IOException {
		initForm(viewer, out);
		if (pd == null)
		  pd = viewer.pd();
		GenericColor plotAreaColor, backgroundColor, plotColor, gridColor, titleColor, scaleColor, unitsColor;
		if (pd == null) {
		  plotAreaColor = backgroundColor = ColorParameters.LIGHT_GRAY;
			plotColor = ColorParameters.BLUE; 
			gridColor = titleColor = scaleColor = unitsColor = ColorParameters.BLACK;
		} else {
			plotAreaColor = pd.getColor(ScriptToken.PLOTAREACOLOR);
			backgroundColor = pd.bgcolor;
			plotColor = pd.getCurrentPlotColor(0);
			gridColor = pd.getColor(ScriptToken.GRIDCOLOR);
			titleColor = pd.getColor(ScriptToken.TITLECOLOR);
			scaleColor = pd.getColor(ScriptToken.SCALECOLOR);
			unitsColor = pd.getColor(ScriptToken.UNITSCOLOR);
		}

		Coordinate[] xyCoords = spec.getXYCoords();

		ScaleData scaleData = new ScaleData(xyCoords, startIndex, endIndex, spec
				.isContinuous(), spec.isInverted());

		double maxXOnScale = scaleData.maxXOnScale;
		double minXOnScale = scaleData.minXOnScale;
		double maxYOnScale = scaleData.maxYOnScale;
		double minYOnScale = scaleData.minYOnScale;
		double xStep = scaleData.steps[0];
		double yStep = scaleData.steps[1];
		int plotAreaWidth = svgWidth - leftInset - rightInset;
		int plotAreaHeight = svgHeight - topInset - bottomInset;
		double xScaleFactor = (plotAreaWidth / (maxXOnScale - minXOnScale));
		double yScaleFactor = (plotAreaHeight / (maxYOnScale - minYOnScale));
		int leftPlotArea = leftInset;
		int rightPlotArea = leftInset + plotAreaWidth;
		int topPlotArea = topInset;
		int bottomPlotArea = topInset + plotAreaHeight;
		int titlePosition = bottomPlotArea + 60;
		context.put("titlePosition", new Integer(titlePosition));

		double xPt, yPt;
		String xStr, yStr;

		// Grid
		Lst<Map<String, String>> vertGridCoords = new Lst<Map<String, String>>();
		Lst<Map<String, String>> horizGridCoords = new Lst<Map<String, String>>();

		for (double i = minXOnScale; i < maxXOnScale + xStep / 2; i += xStep) {
			xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
			yPt = topPlotArea;
			xStr = formatDecimalTrimmed(xPt, 6);
			yStr = formatDecimalTrimmed(yPt, 6);
			Map<String, String> hash = new Hashtable<String, String>();
			hash.put("xVal", xStr);
			hash.put("yVal", yStr);
			vertGridCoords.addLast(hash);
		}

		for (double i = minYOnScale; i < maxYOnScale + yStep / 2; i += yStep) {
			xPt = leftPlotArea;
			yPt = topPlotArea + ((i - minYOnScale) * yScaleFactor);
			xStr = formatDecimalTrimmed(xPt, 6);
			yStr = formatDecimalTrimmed(yPt, 6);
			Map<String, String> hash = new Hashtable<String, String>();
			hash.put("xVal", xStr);
			hash.put("yVal", yStr);
			horizGridCoords.addLast(hash);
		}

		// Scale

		Lst<Map<String, String>> xScaleList = new Lst<Map<String, String>>();
		Lst<Map<String, String>> xScaleListReversed = new Lst<Map<String, String>>();
		Lst<Map<String, String>> yScaleList = new Lst<Map<String, String>>();
		int precisionX = scaleData.precision[0];
		int precisionY = scaleData.precision[1];
		for (double i = minXOnScale; i < (maxXOnScale + xStep / 2); i += xStep) {
			xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
			xPt -= 10; // shift to left by 10
			yPt = bottomPlotArea + 15; // shift down by 15
			xStr = formatDecimalTrimmed(xPt, 6);
			yStr = formatDecimalTrimmed(yPt, 6);
			String iStr = DF.formatDecimalDbl(i, precisionX);
			Map<String, String> hash = new Hashtable<String, String>();
			hash.put("xVal", xStr);
			hash.put("yVal", yStr);
			hash.put("number", iStr);
			xScaleList.addLast(hash);
		}
		for (double i = minXOnScale, j = maxXOnScale; i < (maxXOnScale + xStep / 2); i += xStep, j -= xStep) {
			xPt = leftPlotArea + ((j - minXOnScale) * xScaleFactor);
			xPt -= 10;
			yPt = bottomPlotArea + 15; // shift down by 15
			xStr = formatDecimalTrimmed(xPt, 6);
			yStr = formatDecimalTrimmed(yPt, 6);
			String iStr = DF.formatDecimalDbl(i, precisionX);

			Map<String, String> hash = new Hashtable<String, String>();
			hash.put("xVal", xStr);
			hash.put("yVal", yStr);
			hash.put("number", iStr);
			xScaleListReversed.addLast(hash);

		}

		for (double i = minYOnScale; (i < maxYOnScale + yStep / 2); i += yStep) {
			xPt = leftPlotArea - 55;
			yPt = bottomPlotArea - ((i - minYOnScale) * yScaleFactor);
			yPt += 3; // shift down by three
			xStr = formatDecimalTrimmed(xPt, 6);
			yStr = formatDecimalTrimmed(yPt, 6);
			String iStr = DF.formatDecimalDbl(i, precisionY);

			Map<String, String> hash = new Hashtable<String, String>();
			hash.put("xVal", xStr);
			hash.put("yVal", yStr);
			hash.put("number", iStr);
			yScaleList.addLast(hash);
		}

		double firstTranslateX, firstTranslateY, secondTranslateX, secondTranslateY;
		double scaleX, scaleY;

		boolean increasing = (pd != null && pd.getBoolean(ScriptToken.REVERSEPLOT));//false;//spec.isXIncreasing();
		if (increasing) {
			firstTranslateX = leftPlotArea;
			firstTranslateY = bottomPlotArea;
			scaleX = xScaleFactor;
			scaleY = -yScaleFactor;
			secondTranslateX = -1 * minXOnScale;
			secondTranslateY = -1 * minYOnScale;
		} else {
			firstTranslateX = rightPlotArea;
			firstTranslateY = bottomPlotArea;
			scaleX = -xScaleFactor;
			scaleY = -yScaleFactor;
			secondTranslateX = -minXOnScale;
			secondTranslateY = -minYOnScale;
		}

		double yTickA = minYOnScale - (yStep / 2);
		double yTickB = yStep / 5;

		context.put("plotAreaColor", toRGBHexString(plotAreaColor));
		context.put("backgroundColor", toRGBHexString(backgroundColor));
		context.put("plotColor", toRGBHexString(plotColor));
		context.put("gridColor", toRGBHexString(gridColor));
		context.put("titleColor", toRGBHexString(titleColor));
		context.put("scaleColor", toRGBHexString(scaleColor));
		context.put("unitsColor", toRGBHexString(unitsColor));

		context.put("svgHeight", new Integer(svgHeight));
		context.put("svgWidth", new Integer(svgWidth));
		context.put("leftPlotArea", new Integer(leftPlotArea));
		context.put("rightPlotArea", new Integer(rightPlotArea));
		context.put("topPlotArea", new Integer(topPlotArea));
		context.put("bottomPlotArea", new Integer(bottomPlotArea));
		context.put("plotAreaHeight", new Integer(plotAreaHeight));
		context.put("plotAreaWidth", new Integer(plotAreaWidth));

		context.put("minXOnScale", new Double(minXOnScale));
		context.put("maxXOnScale", new Double(maxXOnScale));
		context.put("minYOnScale", new Double(minYOnScale));
		context.put("maxYOnScale", new Double(maxYOnScale));
		context.put("yTickA", new Double(yTickA));
		context.put("yTickB", new Double(yTickB));
		context.put("xScaleFactor", new Double(xScaleFactor));
		context.put("yScaleFactor", new Double(yScaleFactor));

		context.put("increasing", new Boolean(increasing));

		context.put("verticalGridCoords", vertGridCoords);
		context.put("horizontalGridCoords", horizGridCoords);

		Lst<Coordinate> newXYCoords = new Lst<Coordinate>();
		for (int i = startIndex; i <= endIndex; i++)
			newXYCoords.addLast(xyCoords[i]);

		double firstX, firstY, lastX;
		firstX = xyCoords[startIndex].getXVal();
		firstY = xyCoords[startIndex].getYVal();
		lastX = xyCoords[endIndex].getXVal();

		System.out.println("SVG " + spec.isXIncreasing() + " " + spec.shouldDisplayXAxisIncreasing() + " " + firstX + " "+ lastX + " " + startIndex + " " + endIndex + " " + newXYCoords.get(0).toString() + " " + increasing);
		context.put("title", spec.getTitle());
		context.put("xyCoords", newXYCoords);
		context.put("continuous", new Boolean(spec.isContinuous()));
		context.put("firstTranslateX", new Double(firstTranslateX));
		context.put("firstTranslateY", new Double(firstTranslateY));
		context.put("scaleX", new Double(scaleX));
		context.put("scaleY", new Double(scaleY));
		context.put("secondTranslateX", new Double(secondTranslateX));
		context.put("secondTranslateY", new Double(secondTranslateY));
    context.put("plotStrokeWidth", getPlotStrokeWidth(scaleX, scaleY));

		if (increasing) {
			context.put("xScaleList", xScaleList);
			context.put("xScaleListReversed", xScaleListReversed);
		} else {
			context.put("xScaleList", xScaleListReversed);
			context.put("xScaleListReversed", xScaleList);
		}
		context.put("yScaleList", yScaleList);

		context.put("xUnits", spec.getXUnits());
		context.put("yUnits", spec.getYUnits());
		context.put("firstX", Double.valueOf(firstX));
		context.put("firstY", Double.valueOf(firstY));
		context.put("lastX", Double.valueOf(lastX));

		int xUnitLabelX = rightPlotArea - 50;
		int xUnitLabelY = bottomPlotArea + 30;
		int yUnitLabelX = leftPlotArea - 80;
		int yUnitLabelY = bottomPlotArea / 2;
		int tempX = yUnitLabelX;
		yUnitLabelX = -yUnitLabelY;
		yUnitLabelY = tempX;
		context.put("xUnitLabelX", "" + xUnitLabelX);
		context.put("xUnitLabelY", "" + xUnitLabelY);
		context.put("yUnitLabelX", "" + yUnitLabelX);
		context.put("yUnitLabelY", "" + yUnitLabelY);

		context.put("numDecimalPlacesX", new Integer(Math
				.abs(scaleData.exportPrecision[0])));
		context.put("numDecimalPlacesY", new Integer(Math
				.abs(scaleData.exportPrecision[1])));

		String vm = (mode == ExportType.SVGI ? "plot_ink.vm" : "plot.vm");
		Logger.info("SVGExporter using " + vm);
		return writeForm(vm);
	}

  private String getPlotStrokeWidth(double scaleX, double scaleY) {
    String s = formatDecimalTrimmed(Math.abs(scaleY/1e12*2),10);
    return s;
  }

  private static String toRGBHexString(GenericColor c) {
    return "#" + CU.toRGBHexString(c);
  }

  private static String formatDecimalTrimmed(double x, int precision) {
    return DF.formatDecimalTrimmed0(x, precision);
  }

  /**
   * Export an overlaid graph as SVG with specified Coordinates and Colors
   * @param fileName
   * @param xyCoordsList an array of arrays of Coordinates
   * @param title the title of the graph
   * @param startDataPointIndices the start indices of the coordinates
   * @param endDataPointIndices the end indices of the coordinates
   * @param xUnits the units of the x axis
   * @param yUnits the units of the y axis
   * @param continuous true if the graph is continuous, otherwise false
   * @param increasing true is the graph is increasing, otherwise false
   * @param plotAreaColor the color of the plot area
   * @param backgroundColor the color of the background
   * @param plotColor the color of the plot
   * @param gridColor the color of the grid
   * @param titleColor the color of the title
   * @param scaleColor the color of the scales
   * @param unitsColor the color of the units
   * @return data if fileName is null
   * @throws IOException
   */
  /*
  public String exportAsSVG(String fileName, Graph[] spectra,
                                 String title, int[] startDataPointIndices,
                                 int[] endDataPointIndices, String xUnits,
                                 String yUnits, boolean continuous,
                                 boolean increasing, Color plotAreaColor,
                                 Color backgroundColor, Color plotColor,
                                 Color gridColor, Color titleColor,
                                 Color scaleColor, Color unitsColor) throws IOException {
	  return exportAsSVG(fileName, spectra, title, startDataPointIndices, 
			  endDataPointIndices, xUnits, yUnits, continuous, increasing, plotAreaColor, 
			  backgroundColor, plotColor, gridColor, titleColor, scaleColor, unitsColor, false);
  }
*/
  /**
   * Export an overlaid graph as SVG with specified Coordinates and Colors
   * 
   * @param fileName
   * @param xyCoordsList
   *        an array of arrays of Coordinates
   * @param title
   *        the title of the graph
   * @param startDataPointIndices
   *        the start indices of the coordinates
   * @param endDataPointIndices
   *        the end indices of the coordinates
   * @param xUnits
   *        the units of the x axis
   * @param yUnits
   *        the units of the y axis
   * @param continuous
   *        true if the graph is continuous, otherwise false
   * @param increasing
   *        true is the graph is increasing, otherwise false
   * @param plotAreaColor
   *        the color of the plot area
   * @param backgroundColor
   *        the color of the background
   * @param plotColor
   *        the color of the plot
   * @param gridColor
   *        the color of the grid
   * @param titleColor
   *        the color of the title
   * @param scaleColor
   *        the color of the scales
   * @param unitsColor
   *        the color of the units
   * @param exportForInkscape
   *        determines if inkscape svg is exported
   * @return data if fileName is null
   * @throws IOException
   */
  
  /*
  public String exportAsSVG(String fileName, Graph[] spectra,
                            String title, int[] startDataPointIndices,
                            int[] endDataPointIndices, String xUnits,
                            String yUnits, boolean continuous,
                            boolean increasing, Color plotAreaColor,
                            Color backgroundColor, Color plotColor,
                            Color gridColor, Color titleColor,
                            Color scaleColor, Color unitsColor,
                            boolean exportForInkscape) throws IOException {
    initForm(fileName);
    //DecimalFormat formatter = JSpecViewUtils.getDecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));
    DecimalFormat formatter2 = TextFormat.getDecimalFormat("0.######");

    MultiScaleData scaleData = new MultiScaleData(spectra,
        0, 0, startDataPointIndices, endDataPointIndices, 10, 10, false);

    double maxXOnScale = scaleData.maxXOnScale;
    double minXOnScale = scaleData.minXOnScale;
    double maxYOnScale = scaleData.maxYOnScale;
    double minYOnScale = scaleData.minYOnScale;
    double xStep = scaleData.xStep;
    double yStep = scaleData.yStep;
    int hashNumX = scaleData.hashNums[0];
    int hashNumY = scaleData.hashNums[1];

    int plotAreaWidth = svgWidth - leftInset - rightInset;
    int plotAreaHeight = svgHeight - topInset - bottomInset;
    double xScaleFactor = (plotAreaWidth / (maxXOnScale - minXOnScale));
    double yScaleFactor = (plotAreaHeight / (maxYOnScale - minYOnScale));
    int leftPlotArea = leftInset;
    int rightPlotArea = leftInset + plotAreaWidth;
    int topPlotArea = topInset;
    int bottomPlotArea = topInset + plotAreaHeight;

    //BufferedWriter buffWriter = null;
    //buffWriter = new BufferedWriter(writer);

    double xPt, yPt;
    String xStr, yStr;

    //Grid
    JmolList<Map<String, String>> vertGridCoords = new JmolList<Map<String, String>>();
    JmolList<Map<String, String>> horizGridCoords = new JmolList<Map<String, String>>();

    for (double i = minXOnScale; i < maxXOnScale + xStep / 2; i += xStep) {
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      yPt = topPlotArea;
      xStr = JSVTextFormat.formatDecimalTrimmed(xPt);
      yStr = JSVTextFormat.formatDecimalTrimmed(yPt);

      Map<String, String> hash = new Hashtable<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      vertGridCoords.add(hash);
    }

    for (double i = minYOnScale; i < maxYOnScale + yStep / 2; i += yStep) {
      xPt = leftPlotArea;
      yPt = topPlotArea + ((i - minYOnScale) * yScaleFactor);
      xStr = JSVTextFormat.formatDecimalTrimmed(xPt);
      yStr = JSVTextFormat.formatDecimalTrimmed(yPt);

      Map<String, String> hash = new Hashtable<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      horizGridCoords.add(hash);
    }

    // Scale

    JmolList<Map<String, String>> xScaleList = new JmolList<Map<String, String>>();
    JmolList<Map<String, String>> xScaleListReversed = new JmolList<Map<String, String>>();
    JmolList<Map<String, String>> yScaleList = new JmolList<Map<String, String>>();

    String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";

    if (hashNumX <= 0)
      hashX = hash1.substring(0, Math.abs(hashNumX) + 3);

    DecimalFormat displayXFormatter = TextFormat.getDecimalFormat(hashX);

    if (hashNumY <= 0)
      hashY = hash1.substring(0, Math.abs(hashNumY) + 3);

    DecimalFormat displayYFormatter = TextFormat.getDecimalFormat(hashY);

    for (double i = minXOnScale; i < (maxXOnScale + xStep / 2); i += xStep) {
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      xPt -= 10; // shift to left by 10
      yPt = bottomPlotArea + 15; // shift down by 15
      xStr = JSVTextFormat.formatDecimalTrimmed(xPt);
      yStr = JSVTextFormat.formatDecimalTrimmed(yPt);
      String iStr = displayXFormatter.format(i);

      Map<String, String> hash = new Hashtable<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      xScaleList.add(hash);
    }

    for (double i = minXOnScale, j = maxXOnScale; i < (maxXOnScale + xStep / 2); i += xStep, j -= xStep) {
      xPt = leftPlotArea + ((j - minXOnScale) * xScaleFactor);
      xPt -= 10;
      yPt = bottomPlotArea + 15; // shift down by 15
      xStr = JSVTextFormat.formatDecimalTrimmed(xPt);
      yStr = JSVTextFormat.formatDecimalTrimmed(yPt);
      String iStr = displayXFormatter.format(i);

      Map<String, String> hash = new Hashtable<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      xScaleListReversed.add(hash);

    }

    for (double i = minYOnScale; (i < maxYOnScale + yStep / 2); i += yStep) {
      xPt = leftPlotArea - 55;
      yPt = bottomPlotArea - ((i - minYOnScale) * yScaleFactor);
      yPt += 3; // shift down by three
      xStr = JSVTextFormat.formatDecimalTrimmed(xPt, 6);
      yStr = JSVTextFormat.formatDecimalTrimmed(yPt, 6);
      String iStr = displayYFormatter.format(i);

      Map<String, String> hash = new Hashtable<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      yScaleList.add(hash);
    }

    double firstTranslateX, firstTranslateY, secondTranslateX, secondTranslateY;
    double scaleX, scaleY;

    if (increasing) {
      firstTranslateX = leftPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -1 * minXOnScale;
      secondTranslateY = -1 * minYOnScale;
    } else {
      firstTranslateX = rightPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = -xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -minXOnScale;
      secondTranslateY = -minYOnScale;
    }

    context.put("plotAreaColor", AppUtils.colorToHexString(plotAreaColor));
    context.put("backgroundColor", AppUtils.colorToHexString(backgroundColor));
    context.put("plotColor", AppUtils.colorToHexString(plotColor));
    context.put("gridColor", AppUtils.colorToHexString(gridColor));
    context.put("titleColor", AppUtils.colorToHexString(titleColor));
    context.put("scaleColor", AppUtils.colorToHexString(scaleColor));
    context.put("unitsColor", AppUtils.colorToHexString(unitsColor));

    context.put("svgHeight", new Integer(svgHeight));
    context.put("svgWidth", new Integer(svgWidth));
    context.put("leftPlotArea", new Integer(leftPlotArea));
    context.put("rightPlotArea", new Integer(rightPlotArea));
    context.put("topPlotArea", new Integer(topPlotArea));
    context.put("bottomPlotArea", new Integer(bottomPlotArea));
    context.put("plotAreaHeight", new Integer(plotAreaHeight));
    context.put("plotAreaWidth", new Integer(plotAreaWidth));

    context.put("minXOnScale", new Double(minXOnScale));
    context.put("maxXOnScale", new Double(maxXOnScale));
    context.put("minYOnScale", new Double(minYOnScale));
    context.put("maxYOnScale", new Double(maxYOnScale));
    context.put("xScaleFactor", new Double(xScaleFactor));
    context.put("yScaleFactor", new Double(yScaleFactor));

    context.put("increasing", new Boolean(increasing));

    context.put("verticalGridCoords", vertGridCoords);
    context.put("horizontalGridCoords", horizGridCoords);

    JmolList<JmolList<Coordinate>> newXYCoordsList = new JmolList<JmolList<Coordinate>>();
    JmolList<Coordinate> coords = new JmolList<Coordinate>();
    for (int i = 0; i < spectra.length; i++) {
      Coordinate[] xyCoords = spectra[i].getXYCoords();
      for (int j = startDataPointIndices[i]; j <= endDataPointIndices[i]; j++)
        coords.add(xyCoords[j]);
      newXYCoordsList.add(coords);
    }

    context.put("overlaid", new Boolean(true));
    context.put("title", title);
    context.put("xyCoords", newXYCoordsList);
    context.put("continuous", new Boolean(continuous));
    context.put("firstTranslateX", new Double(firstTranslateX));
    context.put("firstTranslateY", new Double(firstTranslateY));
    context.put("scaleX", new Double(scaleX));
    context.put("scaleY", new Double(scaleY));
    context.put("secondTranslateX", new Double(secondTranslateX));
    context.put("secondTranslateY", new Double(secondTranslateY));

    if (increasing) {
      context.put("xScaleList", xScaleList);
      context.put("xScaleListReversed", xScaleListReversed);
    } else {
      context.put("xScaleList", xScaleListReversed);
      context.put("xScaleListReversed", xScaleList);
    }
    context.put("yScaleList", yScaleList);

    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);

    context.put("numDecimalPlacesX", new Integer(Math.abs(hashNumX)));
    context.put("numDecimalPlacesY", new Integer(Math.abs(hashNumY)));

    String vm = (exportForInkscape ? "plot_ink.vm" : "plot.vm");
    Logger.info("SVGExport using " + vm);
    return writeForm(vm);
  }
  */
}
