/* Copyright (c) 2002-2008 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 * Author: Bob Hanson (hansonr@stolaf.edu) and Jmol developers -- 2008
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

package jspecview.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javajs.util.SB;
import jspecview.api.JSVZipReader;

import org.jmol.util.Logger;

/**
 * Reads the entire contents of a ZIP file as though it were one straight file
 * Skips and entry that contains '\0' 
 * Allows for a moderate amount of buffered reading via mark() 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *
 */
public class JSVZipFileSequentialReader extends BufferedReader implements JSVZipReader {

  private String[] subFileList;
  private ZipInputStream zis;
  private ZipEntry ze;
  private int ptMark;  
  private String data;
  private String startCode;
  private int lineCount;
  
  public JSVZipFileSequentialReader()  {
    super(new StringReader(""));
  }
  
  @Override
	public JSVZipFileSequentialReader set(InputStream bis, String[] subFileList, String startCode) {
    this.subFileList = subFileList;
    zis = new ZipInputStream(bis);
    this.startCode = startCode;
    nextEntry();
    return this;
  }

  @Override
  public void close() {
    try {
      zis.close();
    } catch (IOException e) {
    }
  }
  @Override
  public void mark(int limit) {
    ptMark = pt;
    if (len == 0) {
      readLine();
      pt = ptMark;
    }
  }
  
  @Override
  public void reset() {
    pt = ptMark;
  }
  
  @Override
  public int read(char[] chars, int chPt, int chLen) {
    int l = Math.min(len - pt, chLen);
    data.getChars(0, l, chars, chPt);
    return l;
  }
  
  @Override
  public String readLine() {
    while (ze != null) {
        try {
          String line = getEntryLine();
          if (line != null)
            return line;
        } catch (IOException e) {
          break;
        }
        nextEntry();
    }
    return null;    
  }
  
  private void nextEntry() {
    len = pt = 0;
    cr = '\0';
    lineCount = 0;
    try {
      while ((ze = zis.getNextEntry()) != null)
        if (isEntryOK(ze.getName()))
          return;
    } catch (Exception e) {
      ze = null;
    }
  }

  private boolean isEntryOK(String name) {
    if (subFileList == null || subFileList.length == 1)
      return true;
    for (int i = subFileList.length; --i >= 0; )
      if (subFileList[i].equals(name)) {
        Logger.info("...reading zip entry " + name);        
        return true;
      }
    Logger.info("...skipping zip entry " + name);        
    return false;
  }

  private byte[] buf = new byte[1024];
  private int len;
  private int pt;
  private char cr = '\0';

  private String getEntryLine() throws IOException {
    SB line = null;
    while (len >= 0 && (pt < len || zis.available() == 1)) {
      int pt0 = pt;
      char ch = ' ';
      while (pt < len && ch != cr) {
        switch (ch = data.charAt(pt++)) {
        case '\n':
          if (cr == '\r') {
            pt0 = pt;
            continue;
          }
          cr = '\n';
          break;
        case '\r':
          if (cr == '\n')
            continue;
          cr = '\r';
          break;
        }
      }
      if (line == null)
        line = new SB();
      if (pt != pt0)
        line.append(data.substring(pt0, pt + (ch == cr ? -1 : 0)));
      if (ch == cr || zis.available() != 1 || (len = zis.read(buf, 0, 1024)) < 0) {
        if (lineCount++ == 0 && startCode != null && line.indexOf(startCode) < 0)
          return null;
       return line.toString();
      }
      pt = 0;
      data = new String(buf, 0, len);
      if (data.indexOf('\0') >= 0)
        return  null; // binary file -- forget it!
    }
    return (line == null ? null : line.toString());
  }
}
