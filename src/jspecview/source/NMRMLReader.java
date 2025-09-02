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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import org.jmol.util.Elements;

import javajs.util.Base64;
import javajs.util.PT;




/**
 * Representation of an nmrML Source.
 */

public class NMRMLReader extends XMLReader {

  
  private String structure;
  private String assignment;
  private int dim = 1;

  public NMRMLReader() {
		// called by reflection from FileReader
	}
   
  @Override
	protected JDXSource getXML(BufferedReader br) {
    try {
      source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
      getSimpleXmlReader(br);
      parser.nextEvent();
      processXML(NMRML_0, NMRML_1);
      if (!checkPointCount())
        return null;
      xFactor = 1;
      yFactor = 1;
      populateVariables();
    } catch (Exception pe) {
      System.err.println("That file may be empty...");
      errorLog.append("That file may be empty... \n");
    }
    processErrors("nmrML");
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
    System.out.println(tagNames[tagId]);
    switch (tagId) {
//    <acquisition>
//    <acquisition1D>
//    <acquisitionParameterSet>
//            <DirectDimensionParameterSet  >
//                <acquisitionNucleus name="hydrogen atom" />
//                <effectiveExcitationField value="400"  unitName="megaHertz"  />
//            </DirectDimensionParameterSet> 
//    </acquisitionParameterSet>
//    </acquisition1D>
//    </acquisition>
        case NMRML_acquisitionnucleus:
          String val = parser.getAttrValue("name").replace(" atom", "");
          obNucleus = (PT.isDigit(val.charAt(0)) ? val
              : Elements.getNmrNucleusFromName(val.toLowerCase()));
      return true;
    case NMRML_effectiveexcitationfield:
      strObFreq = parser.getAttrValueLC("value");
      obFreq = Double.parseDouble(strObFreq);
      return true;
    case NMRML_spectrum1d:
      dim = 1;
      npoints = Integer.parseInt(parser.getAttrValue("numberOfDataPoints"));
      // <spectrum1D numberOfDataPoints= "6553"  id="1D-1H" >
      break;
    case NMRML_spectrumdataarray:
      // spectrumDataArray  compressed="false"  encodedLength="139800" byteFormat="complex128">    case NMRML_spectrumdataarray: // 31   
      String type = parser.getAttrValue("byteFormat");
      switch(type) {
      case "complex128":
        getXYFromBase64Complex128(parser.getCharacters());
        break;
      case "float64":
        getXYFromBase64Float64(parser.getCharacters());
        break;
      default:
        System.err.println("NMRML spectrum data array type unknown: " + type);
      }
      break;
    case NMRML_identifier:
      title = parser.getAttrValue("name");
      break;
//    case NMRML_npmrd_id: // 19;
    case NMRML_structure: // 20;
      structure = toCML(parser.getInnerXML());
      break;
    case NMRML_atomAssignmentList: // 25;
      assignment = parser.getOuterXML();
      break;
    case NMRML_bondlist: // 25;
      break;
    }
    return false;
  }

  private static String toCML(String structure) {
    structure = PT.rep(structure, "x=", "x3=");
    structure = PT.rep(structure, "y=", "y3=");
    structure = PT.rep(structure, "z=", "z3=");
    structure = PT.rep(structure, "atomList", "atomArray");
    structure = PT.rep(structure, "bondList", "bondArray");
    structure = PT.rep(structure, "atomRefs", "atomRefs2");
    structure = PT.rep(structure, ">\"", ">"); // bond fix
    structure = "<cml>\n<molecule>\n" + structure + "\n</molecule>\n</cml>";
    return structure;
  }

  @Override
  protected void processEndTag(int tagId) throws Exception {
    // n/a
  }

  /**
   * Decode 1D nmrML BASE64-encoded spectrum (XY..XY) data.
   * 
   * @param sdata
   */
  private void getXYFromBase64Float64(String sdata) {
    byte[] bytes = Base64.decodeBase64(sdata);
    DoubleBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
    System.out.println(npoints + " " + bytes.length/16);
    if ((bytes.length % 16) != 0) {
      throw new RuntimeException("NMRMLReader byte length not multiple of 16 " + bytes.length);
    }
    try {
      int n = bytes.length / 16;
      xaxisData = new double[n];
      yaxisData = new double[n];
      //reverse order of list to make [0-16] for 1H, for example, not [16-0]
      for (int i = 0; i < n; i++) {
        xaxisData[i] = b.get();
        yaxisData[i] = b.get();
      }
      npoints = n;
      firstX = xaxisData[0];
      deltaX = xaxisData[1] - firstX;
      increasing = false;//(deltaX > 0);
      continuous = true;
      lastX = xaxisData[npoints - 1];
      yUnits = "";
      firstY = yaxisData[0];
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  

  private void getXYFromBase64Complex128(String sdata) {
    byte[] bytes = Base64.decodeBase64(sdata);
    DoubleBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
    if ((bytes.length % 16) != 0) {
      throw new RuntimeException("NMRMLReader byte length not multiple of 16 " + bytes.length);
    }
    try {
      int n = bytes.length / 16;
      xaxisData = new double[n];
      yaxisData = new double[n];
      //FileOutputStream fos = new FileOutputStream("C:/temp/t.xls");
      //reverse order of list to make [0-16] for 1H, for example, not [16-0]
      for (int i = 0; i < n; i++) {
        xaxisData[n - i - 1] = b.get();
        yaxisData[n - i - 1] = b.get();
        //fos.write((xaxisData[i] + "\t" + yaxisData[i] + "\n").getBytes());
      }
      //fos.close();
      npoints = n;
      firstX = xaxisData[0];
      deltaX = xaxisData[1] - firstX;
      increasing = false;//(deltaX > 0);
      continuous = true;
      lastX = xaxisData[npoints - 1];
      yUnits = "";
      firstY = yaxisData[0];
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  

}
