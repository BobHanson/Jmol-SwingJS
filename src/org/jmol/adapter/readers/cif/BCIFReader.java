/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.cif;

import java.util.Map;

import javajs.api.GenericCifDataParser;
import javajs.util.MessagePackReader;

/**
 * A very simple Binary CIF file reader extention of MMCifReader extends
 * CifReader.
 * 
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class BCIFReader extends MMCifReader {

  BCIFDataParser bcifParser;

  protected static int[] temp;

  private String version;

  String catName;
  private int colCount;

  @Override
  protected GenericCifDataParser getCifDataParser() {
    return cifParser = bcifParser = new BCIFDataParser(this, debugging);
  }

  /**
   * binary must set up initially
   * 
   * @param fullPath
   * @param htParams
   * @param reader
   */
  @Override
  protected void setup(String fullPath, Map<String, Object> htParams,
                       Object reader) {
    isBinary = true;
    setupASCR(fullPath, htParams, reader);
  }

  /**
   * The primare method initiating this reader
   * 
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void processBinaryDocument() throws Exception {
    long t = System.currentTimeMillis();

    binaryDoc.setBigEndian(true);
    Map<String, Object> msgMap = (new MessagePackReader(binaryDoc, false))
        .readMap();//Selectively(cb);
    binaryDoc.close();

    String encoder = (String) msgMap.get("encoder");
    System.out.println("BCIFReader: BCIF encoder " + encoder);
    
    version = (String) msgMap.get("version");

    System.out.println("BCIFReader: BCIF version " + version);
    Map<String, Object> dataBlock = (Map<String, Object>) ((Object[]) msgMap
        .get("dataBlocks"))[0];

    System.out.println("BCIFReader processed MessagePack in "
        + (System.currentTimeMillis() - t) + " ms");

    getCifDataParser();
    Object[] categories = (Object[]) dataBlock.get("categories");
    bcifParser.header = (String) dataBlock.get("header");
    for (int j = 0; j < categories.length; j++) {
      Map<String, Object> cat = (Map<String, Object>) categories[j];
      if (!cat.isEmpty())
        processCategory(cat);
    }
    System.out.println("BCIFReader processed binary file in "
        + (System.currentTimeMillis() - t) + " ms");
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    super.finalizeSubclassReader();
    BCIFDecoder.clearTemp();
  }

  /**
   * This method is the driver. All model values are set through it.
   * 
   * 1) Set the category name
   * 
   * 2) Check to see if this is one we are interested in.
   * 
   * 3) Initialize the BCIFParser for this category.
   * 
   * 4) Process the category.
   * 
   * @param cat
   * @return ignored
   * @throws Exception
   */
  private boolean processCategory(Map<String, Object> cat) throws Exception {
    String catName = ((String) cat.get("name")).toLowerCase();
    if (!isCategoryOfInterest(catName))
      return false;
    bcifParser.initializeCategory(catName,
        BCIFDecoder.geMapInt(cat.get("rowCount"), null), (Object[]) cat.get("columns"));
    processCategoryName(catName);
    return false;
  }

  /**
   * We are only interested in about a dozen of the fifty categories in a BCIF file.
   * 
   * @param catName
   * @return true if we should process this category
   */
  private boolean isCategoryOfInterest(String catName) {
    switch (catName) {
    case CAT_ENTRY:
    case CAT_ATOM_SITE:
    case CAT_ATOM_TYPE:
    case CAT_ATOM_SITES:
    case CAT_CELL:
    case CAT_NCS:
    case CAT_OPER:
    case CAT_ASSEM:
    case CAT_SEQUENCEDIF:
    case CAT_STRUCSITE:
    case CAT_CHEMCOMP:
    case CAT_STRUCTCONF:
    case CAT_SHEET:
    case CAT_COMPBOND:
    case CAT_STRUCTCONN:
      return true;
    }
    return false;
  }

  /**
   * OK! Here go. Make the calls to MMCifReader and CifReader that they would call themselves.
   * 
   * @param catName
   * @return ignored
   * @throws Exception
   */
  private boolean processCategoryName(String catName) throws Exception {
    this.catName = catName;
    switch (catName) {
    case CAT_ENTRY:
      return processEntry();
    case CAT_ATOM_SITE:
      return processAtomSiteLoopBlock(false);
    case CAT_ATOM_TYPE:
      return processAtomTypeLoopBlock();
    case CAT_ATOM_SITES:
      return processAtomSites();
    case CAT_CELL:
      return processCellBlock();
    }
    switch (catName) {
    case CAT_NCS:
    case CAT_OPER:
    case CAT_ASSEM:
    case CAT_SEQUENCEDIF:
    case CAT_STRUCSITE:
    case CAT_CHEMCOMP:
    case CAT_STRUCTCONF:
    case CAT_SHEET:
    case CAT_COMPBOND:
    case CAT_STRUCTCONN:
      key0 = catName + ".";
      return processSubclassLoopBlock();
    }
    return false;
  }

  /**
   * Process _entry.id
   * 
   * @return ignored
   */
  private boolean processEntry() {
    bcifParser.decodeAndGetData(0);
    pdbID = bcifParser.fieldStr;
    return true;
  }

  private boolean processAtomSites() throws Exception {
    for (int i = 0; i < colCount; i++) {
      bcifParser.decodeAndGetData(i);
      processUnitCellTransformMatrix();
    }
    return true;
  }

  private boolean processCellBlock() throws Exception {
    for (int i = 0; i < colCount; i++) {
      bcifParser.decodeAndGetData(i);
      processCellParameter();
    }
    return true;
  }

  // calls from superclasses:
  
  /**
   * This is the callback the that the MMCifReader and CifReader process.....()
   * methods call initially.
   * 
   * Pass this on to our psuedo-parser.
   * 
   */
  @Override
  protected void parseLoopParameters(String[] fields) throws Exception {
    bcifParser.parseDataBlockParameters(fields, null, null, key2col, col2key);
  }

  @Override
  protected boolean isFieldValid() {
    if (bcifParser.fieldStr != null)
      firstChar = bcifParser.fieldStr.charAt(0);
    return bcifParser.isFieldValid();
  }

  @Override
  protected int parseIntField() {
    // was a problem with CoordinateServer 1.4.10, 1aqj_full.bcif
    // CoordinateServer 1.4.9  1cbs_full
    return (bcifParser.ifield == BCIFDecoder.UNKNOWN_INT 
        ? super.parseIntField() 
            : bcifParser.ifield);
  }

  @Override
  protected double parseDoubleField() {
    return bcifParser.dfield;
  }

  @Override
  protected double parseCartesianField() {
    return Math.round(bcifParser.dfield * 1000)/1000d;
  }

  /**
   * Get a specific column's item for this category on this row.
   * 
   * In contrast to MMCifReader, we already have the integer; we don't have to parse it. 
   */
  @Override
  protected int parseIntFieldTok(byte tok) {
    //return parseIntStr(getFieldString(tok));
    getFieldString(tok);
    return bcifParser.ifield;
  }

  /**
   * Called by processAtomSites to retrieve individual matrix elements and set them by name.
   */
  @Override
  protected double getDoubleColumnData(int i) {
    bcifParser.getColumnData(i);
    return bcifParser.dfield;
  }

}
