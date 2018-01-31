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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;

import javajs.util.Base64;
import javajs.util.BC;




/**
 * Representation of a XML Source.
 * @author Craig A.D. Walters
 * @author Prof. Robert J. Lancashire
 */

public class AnIMLReader extends XMLReader {

	public AnIMLReader() {
		// called by reflection from FileReader
	}
   
  @Override
	protected JDXSource getXML(BufferedReader br) {
    try {
      source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
      getSimpleXmlReader(br);
      parser.nextEvent();
      processXML(AML_0, AML_1);
      if (!checkPointCount())
        return null;
      xFactor = 1;
      yFactor = 1;
      populateVariables();
    } catch (Exception pe) {
      System.err.println("That file may be empty...");
      errorLog.append("That file may be empty... \n");
    }
    processErrors("anIML");
    try {
      br.close();
    } catch (IOException e1) {
      //
    }
    return source;
  }

  /**
   * Process the XML events.
   * Invoked for every start tag.
   *
   * Invoked by the superclass method
   *   XMLSource.process(tagId, requiresEndTag)
   *
   * @param tagId
   * @return true to continue looking for encapsulated tags
   *         false to process once only (no encapsulated tags of interest)
   * @throws Exception
   */
  @Override
  protected boolean processTag(int tagId) throws Exception {
    switch (tagId) {
    case AML_AUDITTRAIL:
      processAuditTrail();
      return true;
    case AML_EXPERIMENTSTEPSET:
      processExperimentStepSet();
      return true;
    case AML_SAMPLESET:
      processSampleSet();
      return true;
    case AML_AUTHOR:
      // AML_AUTHOR is processed via AML_EXPERIMENTSTEPSET
      processAuthor();
      return true;
    case AML_RESULT:
      inResult = true;
      return true;
    default:
      System.out.println("AnIMLReader not processing tag " + tagNames[tagId]);
      // should not be here
      return false;
    }
  }

  @Override
  protected void processEndTag(int tagId) throws Exception {
    switch(tagId) {
    case AML_RESULT:
    case AML_EXPERIMENTSTEPSET:
      inResult = false;
      break;
    }
  }

  private void processAuditTrail() throws Exception {
    if (tagName.equals("user")) {
      parser.qualifiedValue();
    } else if (tagName.equals("timestamp")) {
      parser.qualifiedValue();
    }
  }

  private void processSampleSet() throws Exception {
    if (tagName.equals("sample"))
      samplenum++;
    else if (tagName.equals("parameter")) {
      attrList = parser.getAttrValueLC("name");
      if (attrList.equals("name")) {
        parser.qualifiedValue();
      } else if (attrList.equals("owner")) {
        parser.qualifiedValue();
      } else if (attrList.equals("molecular formula")) {
        molForm = parser.qualifiedValue();
      } else if (attrList.equals("cas registry number")) {
        casRN = parser.qualifiedValue();
      }
    }
  }

  private boolean inResult;
  
  private void processExperimentStepSet() throws Exception {
    if (tagName.equals("result")) {
      inResult = true; 
    } else if (tagName.equals("sampleref")) {
      if (parser.getAttrValueLC("role").contains("samplemeasurement"))
        sampleID = parser.getAttrValue("sampleID");
    } else if (tagName.equals("author")) {
      process(AML_AUTHOR, true);
    } else if (tagName.equals("timestamp")) {
      LongDate = parser.thisValue();
    } else if (tagName.equals("technique")) {
      techname = parser.getAttrValue("name").toUpperCase() + " SPECTRUM";
    } else if (tagName.equals("vectorset") || tagName.equals("seriesset") && inResult) {
      npoints = Integer.parseInt(parser.getAttrValue("length"));
      //System.out.println("AnIML No. of points= " + npoints);
      xaxisData = new double[npoints];
      yaxisData = new double[npoints];
    } else if (tagName.equals("vector") || tagName.equals("series") && inResult) {
      String axisLabel = parser.getAttrValue("name");
      String dependency = parser.getAttrValueLC("dependency");
      if (dependency.equals("independent")) {
        xUnits = axisLabel;
        getXValues();
      } else if (dependency.equals("dependent")) {
        yUnits = axisLabel;
        getYValues();
      }
    } else if (tagName.equals("parameter")) {
      if ((attrList = parser.getAttrValueLC("name")).equals("identifier")) {
        title = parser.qualifiedValue();
      } else if (attrList.equals("nucleus")) {
        obNucleus = parser.qualifiedValue();
      } else if (attrList.equals("observefrequency")) {
        StrObFreq = parser.qualifiedValue();
        obFreq = Double.parseDouble(StrObFreq);
      } else if (attrList.equals("referencepoint")) {
        refPoint = Double.parseDouble(parser.qualifiedValue());
      } else if (attrList.equals("sample path length")) {
        pathlength = parser.qualifiedValue();
      } else if (attrList.equals("scanmode")) {
        parser.thisValue(); // ignore?
      } else if (attrList.equals("manufacturer")) {
        vendor = parser.thisValue();
      } else if (attrList.equals("model name")) {
        modelType = parser.thisValue();
      } else if (attrList.equals("resolution")) {
        resolution = parser.qualifiedValue();
      }
    }
  }

  private void getXValues() throws Exception {
    parser.nextTag();
    if (parser.getTagName().equals("autoincrementedvalueset")) {
      parser.nextTag();
      if (parser.getTagName().equals("startvalue"))
        firstX = Double.parseDouble(parser.qualifiedValue());
      nextStartTag();
      if (parser.getTagName().equals("increment"))
        deltaX = Double.parseDouble(parser.qualifiedValue());
    }
    if (!inResult) {
      nextStartTag();
      xUnits = parser.getAttrValue("label");
    }
    increasing = (deltaX > 0 ? true : false);
    continuous = true;
    for (int j = 0; j < npoints; j++)
      xaxisData[j] = firstX + (deltaX * j);
    lastX = xaxisData[npoints - 1];
  }

  private void nextStartTag() throws Exception {
    parser.nextStartTag();
    while (parser.getTagType() == XMLParser.COMMENT) {
      parser.nextStartTag();
    }
  }

  private void getYValues() throws Exception {
    String vectorType = parser.getAttrValueLC("type");
    if (vectorType.length() == 0)
      vectorType = parser.getAttrValueLC("vectorType");
    parser.nextTag();
    tagName = parser.getTagName();
    if (tagName.equals("individualvalueset")) {
      for (int ii = 0; ii < npoints; ii++)
        yaxisData[ii] = Double.parseDouble(parser.qualifiedValue());
      //System.out.println(npoints + " individual Y values now read");
    } else if (tagName.equals("encodedvalueset")) {
      attrList = parser.getCharacters();
      byte[] dataArray = Base64.decodeBase64(attrList);
      if (dataArray.length != 0) {       
        if (vectorType.equals("float64")) {
        	for (int i = 0, pt = 0; i  < npoints; i++, pt += 8)
        		yaxisData[i] = BC.bytesToDoubleToFloat(dataArray, pt, false);
        } else {
        	for (int i = 0, pt = 0; i  < npoints; i++, pt += 4)
        		yaxisData[i] = BC.bytesToFloat(dataArray, pt, false);
        }
      }
    }
    parser.nextStartTag();
    tagName = parser.getTagName();
    yUnits = parser.getAttrValue("label");
    firstY = yaxisData[0];
  }

  private void processAuthor() throws Exception {
    if (tagName.equals("name"))
      owner = parser.thisValue();
    else if (tagName.contains("location"))
      origin = parser.thisValue();
  }

}
