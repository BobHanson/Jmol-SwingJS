/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
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
package org.jmol.jvxl.readers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;

import javajs.J2SIgnoreImport;
import javajs.api.GenericBinaryDocument;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.Rdr;

import org.jmol.api.Interface;
import org.jmol.viewer.Viewer;

/**
 * PolygonFileReader or VolumeFileReader
 */
@J2SIgnoreImport({Rdr.StreamReader.class})
abstract class SurfaceFileReader extends SurfaceReader {

  protected BufferedReader br;
  protected GenericBinaryDocument binarydoc;
  protected OC out;
  
  SurfaceFileReader() {
  }

  protected void setStream(String fileName, boolean isBigEndian) {
    if (fileName == null)
      binarydoc.setStream(null, isBigEndian);
    else
    try {
      if (br instanceof Rdr.StreamReader) {
        BufferedInputStream stream = ((Rdr.StreamReader) br).getStream();
        stream.reset();
        binarydoc.setStream(stream, true); 
      }
    } catch (Exception e) {
      System.out.println("BCifDensityReader " + e);
      binarydoc.setStream(sg.atomDataServer
          .getBufferedInputStream(fileName), isBigEndian);
    }
  }

  @Override
  void init(SurfaceGenerator sg) {
    initSR(sg);
  }

  void init2(SurfaceGenerator sg, BufferedReader br) {
    init2SFR(sg, br);
  }

  void init2SFR(SurfaceGenerator sg, BufferedReader br) {
    this.br = br;
    init(sg);
  }

  GenericBinaryDocument newBinaryDocument() {
    return (GenericBinaryDocument) Interface.getInterface("javajs.util.BinaryDocument", 
        (Viewer) sg.atomDataServer, "file");
  }
  
  @Override
  protected void setOutputChannel(OC out) {
    if (binarydoc == null)
      this.out = out;
    else
      sg.setOutputChannel(binarydoc, out);
  }

  @Override
  protected void closeReader() {
    closeReaderSFR();
  }

  protected void closeReaderSFR() {
    if (br != null)
      try {
        br.close();
      } catch (IOException e) {
        // ignore
      }
    if (out != null)
      out.closeChannel();
    if (binarydoc != null)
      binarydoc.close();
  }

  @Override
  void discardTempData(boolean discardAll) {
    closeReader();
    discardTempDataSR(discardAll);
  }

  protected String line;
  protected int[] next = new int[1];

  protected String[] getTokens() {
    return PT.getTokensAt(line, 0);
  }

  protected float parseFloat() {
    return PT.parseFloatNext(line, next);
  }

  protected float parseFloatStr(String s) {
    next[0] = 0;
    return PT.parseFloatNext(s, next);
  }

  protected float parseFloatRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseFloatRange(s, iEnd, next);
  }

  protected int parseInt() {
    return PT.parseIntNext(line, next);
  }

  protected int parseIntStr(String s) {
    next[0] = 0;
    return PT.parseIntNext(s, next);
  }

  protected int parseIntNext(String s) {
    return PT.parseIntNext(s, next);
  }

  protected float[] parseFloatArrayStr(String s) {
    next[0] = 0;
    return PT.parseFloatArrayNext(s, next, null, null, null);
  }

  protected float[] parseFloatArray(float[] a, String strStart, String strEnd) {
    return PT.parseFloatArrayNext(line, next, a, strStart, strEnd);
  }

  protected String getQuotedStringNext() {
    return PT.getQuotedStringNext(line, next);
  }

  protected void skipTo(String info, String what) throws Exception {
    if (info != null)
      while (rd().indexOf(info) < 0) {
      }
    if (what != null)
      next[0] = line.indexOf(what) + what.length() + 2;
  }

  protected String rd() throws Exception {
    line = br.readLine();
    if (line != null) {
      nBytes += line.length();
      if (out != null) {
        byte[] b = line.getBytes();
        out.write(b, 0, b.length);
        out.writeByteAsInt(0x0A);
      }
    }
    return line;
  }
}
