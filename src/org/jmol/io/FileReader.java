/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javajs.api.GenericBinaryDocument;
import javajs.api.ZInputStream;
import javajs.util.AU;
import javajs.util.PT;
import javajs.util.Rdr;

import org.jmol.api.Interface;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class FileReader {
  /**
   * 
   */
  private final Viewer vwr;
  private String fileNameIn;
  private String fullPathNameIn;
  private String nameAsGivenIn;
  private String fileTypeIn;
  private Object atomSetCollection;
  private Object readerOrDocument;
  private Map<String, Object> htParams;
  private boolean isAppend;
  private Object bytesOrStream;

  /**
   * 
   * @param vwr
   * @param fileName  "t.xyz" may be null
   * @param fullPathName "c:\temp\t.xyz" may be null
   * @param nameAsGiven "c:/temp/t.xyz" may be null
   * @param type forced file type
   * @param reader optional Reader, BufferedReader, byte[], or BufferedInputStream
   * @param htParams information for file reader
   * @param isAppend append to current models or not
   */
  public FileReader(Viewer vwr, String fileName,
      String fullPathName, String nameAsGiven, String type, Object reader,
      Map<String, Object> htParams, boolean isAppend) {
    this.vwr = vwr;
    fileNameIn = (fileName == null ? fullPathName : fileName);
    fullPathNameIn = (fullPathName == null ? fileNameIn : fullPathName);
    nameAsGivenIn = (nameAsGiven == null ? fileNameIn : nameAsGiven);
    fileTypeIn = type;
    if (reader != null) {
      if (AU.isAB(reader) || reader instanceof BufferedInputStream) {
        // we allow an external program to submit a BufferedInputStream or byte[] 
        // for any file type.
        bytesOrStream = reader;
        reader = null;
      } else if (reader instanceof Reader
          && !(reader instanceof BufferedReader)) {
        reader = new BufferedReader((Reader) reader);
      }
    }
    this.readerOrDocument = reader;
    this.htParams = htParams;
    this.isAppend = isAppend;
  }

  public void run() {
    if (!isAppend && vwr.displayLoadErrors)
      vwr.zap(false, true, false);
    String errorMessage = null;
    Object t = null;
    if (fullPathNameIn.contains("#_DOCACHE_"))
      readerOrDocument = getChangeableReader(vwr, nameAsGivenIn, fullPathNameIn);
    if (readerOrDocument == null) {
      // note that since bytes comes from reader, bytes will never be non-null here
           t = vwr.fm.getUnzippedReaderOrStreamFromName(fullPathNameIn,
          bytesOrStream, true, false, false, true, htParams);
      if (t == null || t instanceof String) {
        errorMessage = (t == null ? "error opening:" + nameAsGivenIn
            : (String) t);
        if (!errorMessage.startsWith("NOTE:"))
          Logger.error("file ERROR: " + fullPathNameIn + "\n" + errorMessage);
        atomSetCollection = errorMessage;
        return;
      }
      if (t instanceof BufferedReader) {
        readerOrDocument = t;
      } else if (t instanceof ZInputStream) {
        String name = fullPathNameIn;
        String[] subFileList = null;
        name = name.replace('\\', '/');
        if (name.indexOf("|") >= 0 && !name.endsWith(".zip")) {
          subFileList = PT.split(name, "|");
          name = subFileList[0];
        }
        if (subFileList != null)
          htParams.put("subFileList", subFileList);
        InputStream zis = (InputStream) t;
        String[] zipDirectory = vwr.fm.getZipDirectory(name, true, true);
        atomSetCollection = t = vwr.fm.getJzu().getAtomSetCollectionOrBufferedReaderFromZip(vwr, zis, name, zipDirectory, htParams, 1, false);
        try {
          zis.close();
        } catch (Exception e) {
          //
        }
      }
    }
    if (t instanceof BufferedInputStream)
      readerOrDocument = ((GenericBinaryDocument) Interface
          .getInterface("javajs.util.BinaryDocument", vwr, "file")).setStream((BufferedInputStream) t, !htParams.containsKey("isLittleEndian"));
    if (readerOrDocument != null) {
      atomSetCollection = vwr.getModelAdapter().getAtomSetCollectionReader(
          fullPathNameIn, fileTypeIn, readerOrDocument, htParams);
      if (!(atomSetCollection instanceof String))
        atomSetCollection = vwr.getModelAdapter().getAtomSetCollection(
            atomSetCollection);
      try {
        if (readerOrDocument instanceof BufferedReader)
          ((BufferedReader) readerOrDocument).close();
        else if (readerOrDocument instanceof GenericBinaryDocument)
          ((GenericBinaryDocument) readerOrDocument).close();
      } catch (IOException e) {
        // ignore
      }
    }

    if (atomSetCollection instanceof String)
      return;

    if (!isAppend && !vwr.displayLoadErrors)
      vwr.zap(false, true, false);

    vwr.fm.setFileInfo(new String[] { fullPathNameIn, fileNameIn, nameAsGivenIn });
  }
  
  final static BufferedReader getChangeableReader(Viewer vwr, 
                               String nameAsGivenIn, String fullPathNameIn) {
    return Rdr.getBR((String) vwr.getLigandModel(nameAsGivenIn, fullPathNameIn, "_file", null));
  }

  public Object getAtomSetCollection() {
    return atomSetCollection;
  }


}