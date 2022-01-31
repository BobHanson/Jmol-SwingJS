/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javajs.util.AU;
import javajs.util.CompoundDocument;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.ZipTools;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class JmolUtil {

  public JmolUtil() {
    // for reflection
  }

  // Only three entry points here: 
  //  getImage
  //  getAtomSetCollectionOrBufferedReaderFromZip
  //  getCachedPngjBytes
  
  public Object getImage(Viewer vwr, Object fullPathNameOrBytes,
                         String echoName, boolean forceSync) {
    Object image = null;
    Object info = null;
    GenericPlatform apiPlatform = vwr.apiPlatform;
    boolean createImage = false;
    String fullPathName = "" + fullPathNameOrBytes;
    if (fullPathNameOrBytes instanceof String) {
      boolean isBMP = fullPathName.toUpperCase().endsWith("BMP");
      // previously we were loading images 
      if (forceSync || fullPathName.indexOf("|") > 0 || isBMP) {
        Object ret = vwr.fm.getFileAsBytes(fullPathName, null);
        if (!AU.isAB(ret))
          return "" + ret;
        // converting bytes to an image in JavaScript is a synchronous process
        if (vwr.isJSNoAWT)
          info = new Object[] { echoName, fullPathNameOrBytes, ret };
        else
          image = apiPlatform.createImage(ret);
      } else if (OC.urlTypeIndex(fullPathName) >= 0) {
        // if JavaScript returns an image, than it must have been cached, and 
        // the call was not asynchronous after all.
        if (vwr.isJSNoAWT)
          info = new Object[] { echoName, fullPathNameOrBytes, null };
        else
          try {
            image = apiPlatform.createImage(new URL((URL) null, fullPathName,
                null));
          } catch (Exception e) {
            return "bad URL: " + fullPathName;
          }
      } else {
        createImage = true;
      }
    } else if (vwr.isJSNoAWT) {
      // not sure that this can work. 
      // ensure that apiPlatform.createImage is called
      //
      info = new Object[] { echoName,
          Rdr.guessMimeTypeForBytes((byte[]) fullPathNameOrBytes),
          fullPathNameOrBytes };
    } else {
      createImage = true;
    }
    if (createImage)
      image = apiPlatform
          .createImage("\1close".equals(fullPathNameOrBytes) ? "\1close"
              + echoName : fullPathNameOrBytes);
    else if (info != null) // JavaScript only
      image = apiPlatform.createImage(info);

    /**
     * 
     * @j2sNative
     * 
     *            return image;
     * 
     */
    {
      if (image == null)
        return null;
      try {
        if (!apiPlatform.waitForDisplay(info, image))
          return null;
        return (apiPlatform.getImageWidth(image) < 1 ? "invalid or missing image "
            + fullPathName
            : image);
      } catch (Exception e) {
        return e.toString() + " opening " + fullPathName;
      }
    }
  }

  /**
   * A rather complicated means of reading a ZIP file, which could be a single
   * file, or it could be a manifest-organized file, or it could be a Spartan
   * directory.
   * 
   * @param vwr
   * @param is
   * @param fileName
   * @param zipDirectory
   * @param htParams
   * @param subFilePtr
   * @param asBufferedReader
   * @return a single atomSetCollection
   * 
   */
  public Object getAtomSetCollectionOrBufferedReaderFromZip(Viewer vwr,
                                                            InputStream is,
                                                            String fileName,
                                                            String[] zipDirectory,
                                                            Map<String, Object> htParams,
                                                            int subFilePtr,
                                                            boolean asBufferedReader) {

    // we're here because user is using | in a load file name
    // or we are opening a zip file.

    JmolAdapter adapter = vwr.getModelAdapter();
    boolean doCombine = (subFilePtr == 1);
    htParams.put("zipSet", fileName);
    String[] subFileList = (String[]) htParams.get("subFileList");
    if (subFileList == null)
      subFileList = getSpartanSubfiles(zipDirectory);
    String subFileName = (subFileList == null
        || subFilePtr >= subFileList.length ? (String) htParams.get("SubFileName") : subFileList[subFilePtr]);
    if (subFileName != null
        && (subFileName.startsWith("/") || subFileName.startsWith("\\")))
      subFileName = subFileName.substring(1);
    int selectedFile = 0;
    if (subFileName == null && htParams.containsKey("modelNumber")) {
      selectedFile = ((Integer) htParams.get("modelNumber")).intValue();
      if (selectedFile > 0 && doCombine)
        htParams.remove("modelNumber");
    }

    // zipDirectory[0] is the manifest if present
    String manifest = (String) htParams.get("manifest");
    boolean useFileManifest = (manifest == null);
    if (useFileManifest)
      manifest = (zipDirectory.length > 0 ? zipDirectory[0] : "");
    boolean haveManifest = (manifest.length() > 0);
    if (haveManifest) {
      if (Logger.debugging)
        Logger.debug("manifest for  " + fileName + ":\n" + manifest);
    }
    boolean ignoreErrors = (manifest.indexOf("IGNORE_ERRORS") >= 0);
    boolean selectAll = (manifest.indexOf("IGNORE_MANIFEST") >= 0);
    boolean exceptFiles = (manifest.indexOf("EXCEPT_FILES") >= 0);
    if (selectAll || subFileName != null)
      haveManifest = false;
    if (useFileManifest && haveManifest) {
      String path = FileManager.getManifestScriptPath(manifest);
      if (path != null) {
        return JC.NOTE_SCRIPT_FILE + fileName + path + "\n";
      }
    }
    Lst<Object> vCollections = new Lst<Object>();
    Map<String, Object> htCollections = (haveManifest ? new Hashtable<String, Object>()
        : null);
    int nFiles = 0;
    // 0 entry is manifest

    try {
      SB spartanData = (isSpartanZip(zipDirectory) ? vwr.fm.spartanUtil()
          .getData(is, zipDirectory) : null);
      Object ret;
      if (spartanData != null) {
        BufferedReader reader = Rdr.getBR(spartanData.toString());
        if (asBufferedReader)
          return reader;
        ret = adapter
            .getAtomSetCollectionFromReader(fileName, reader, htParams);
        if (ret instanceof String)
          return ret;
        if (ret instanceof AtomSetCollection) {
          AtomSetCollection atomSetCollection = (AtomSetCollection) ret;
          if (atomSetCollection.errorMessage != null) {
            if (ignoreErrors)
              return null;
            return atomSetCollection.errorMessage;
          }
          return atomSetCollection;
        }
        if (ignoreErrors)
          return null;
        return "unknown reader error";
      }
      if (is instanceof BufferedInputStream && Rdr.isPngZipStream(is))
        is = ZipTools.getPngZipStream((BufferedInputStream) is, true);
      ZipInputStream zis = ZipTools.newZipInputStream(is);
      ZipEntry ze;
      if (haveManifest)
        manifest = '|' + manifest.replace('\r', '|').replace('\n', '|') + '|';
      while ((ze = zis.getNextEntry()) != null
          && (selectedFile <= 0 || vCollections.size() < selectedFile)) {
        if (ze.isDirectory())
          continue;
        String thisEntry = ze.getName();
        if (subFileName != null && !thisEntry.equals(subFileName))
          continue;
        if (subFileName != null)
          htParams.put("subFileName", subFileName);
        if (thisEntry.startsWith("JmolManifest") || haveManifest
            && exceptFiles == manifest.indexOf("|" + thisEntry + "|") >= 0)
          continue;
        byte[] bytes = Rdr.getLimitedStreamBytes(zis, ze.getSize());
        //        String s = new String(bytes);
        //        System.out.println("ziputil " + s.substring(0, 100));
        if (Rdr.isGzipB(bytes))
          bytes = Rdr.getLimitedStreamBytes(ZipTools.getUnGzippedInputStream(bytes),
              -1);
        if (Rdr.isZipB(bytes) || Rdr.isPngZipB(bytes)) {
          BufferedInputStream bis = Rdr.getBIS(bytes);
          String[] zipDir2 = ZipTools.getZipDirectoryAndClose(bis, "JmolManifest");
          bis = Rdr.getBIS(bytes);
          Object atomSetCollections = getAtomSetCollectionOrBufferedReaderFromZip(
              vwr, bis, fileName + "|" + thisEntry, zipDir2, htParams,
              ++subFilePtr, asBufferedReader);
          if (atomSetCollections instanceof String) {
            if (ignoreErrors)
              continue;
            return atomSetCollections;
          } else if (atomSetCollections instanceof AtomSetCollection
              || atomSetCollections instanceof Lst<?>) {
            if (haveManifest && !exceptFiles)
              htCollections.put(thisEntry, atomSetCollections);
            else
              vCollections.addLast(atomSetCollections);
          } else if (atomSetCollections instanceof BufferedReader) {
            if (doCombine)
              zis.close();
            return atomSetCollections; // FileReader has requested a zip file
            // BufferedReader
          } else {
            if (ignoreErrors)
              continue;
            zis.close();
            return "unknown zip reader error";
          }
        } else if (Rdr.isPickleB(bytes)) {
          BufferedInputStream bis = Rdr.getBIS(bytes);
          if (doCombine)
            zis.close();
          return bis;
        } else {
          String sData;
          if (Rdr.isCompoundDocumentB(bytes)) {
            CompoundDocument jd = (CompoundDocument) Interface
                .getInterface("javajs.util.CompoundDocument", vwr, "file");
            jd.setDocStream(Rdr.getBIS(bytes));
            sData = jd.getAllDataFiles("Molecule", "Input").toString();
          } else {
            // could be a PNGJ file with an internal pdb.gz entry, for instance
            sData = Rdr.fixUTF(bytes);
          }
          BufferedReader reader = Rdr.getBR(sData);
          if (asBufferedReader) {
            if (doCombine)
              zis.close();
            return reader;
          }
          String fname = fileName + "|" + ze.getName();

          ret = adapter.getAtomSetCollectionFromReader(fname, reader, htParams);

          if (!(ret instanceof AtomSetCollection)) {
            if (ignoreErrors)
              continue;
            zis.close();
            return "" + ret;
          }
          if (haveManifest && !exceptFiles)
            htCollections.put(thisEntry, ret);
          else
            vCollections.addLast(ret);
          AtomSetCollection a = (AtomSetCollection) ret;
          if (a.errorMessage != null) {
            if (ignoreErrors)
              continue;
            zis.close();
            return a.errorMessage;
          }
        }
      }

      if (doCombine)
        zis.close();

      // if a manifest exists, it sets the files and file order

      if (haveManifest && !exceptFiles) {
        String[] list = PT.split(manifest, "|");
        for (int i = 0; i < list.length; i++) {
          String file = list[i];
          if (file.length() == 0 || file.indexOf("#") == 0)
            continue;
          if (htCollections.containsKey(file))
            vCollections.addLast(htCollections.get(file));
          else if (Logger.debugging)
            Logger.debug("manifested file " + file + " was not found in "
                + fileName);
        }
      }
      if (!doCombine)
        return vCollections;

      AtomSetCollection result = (vCollections.size() == 1
          && vCollections.get(0) instanceof AtomSetCollection ? (AtomSetCollection) vCollections
          .get(0) : new AtomSetCollection("Array", null, null, vCollections));
      if (result.errorMessage != null) {
        if (ignoreErrors)
          return null;
        return result.errorMessage;
      }
      if (nFiles == 1)
        selectedFile = 1;
      if (selectedFile > 0 && selectedFile <= vCollections.size())
        return vCollections.get(selectedFile - 1);
      return result;

    } catch (Exception e) {
      if (ignoreErrors)
        return null;
      Logger.error("" + e);
      return "" + e;
    } catch (Error er) {
      Logger.errorEx(null, er);
      return "" + er;
    }
  }

  /**
   * Get the cached PNGJ bytes for xxx.png or xxx.png|yyyy. Does not extract the |yyyy bytes.
   * 
   * @param fm
   * @param pathName
   * @return byte array
   */
  public byte[] getCachedPngjBytes(FileManager fm, String pathName) {
    if (pathName.startsWith("file:///"))
      pathName = "file:" + pathName.substring(7);
    Logger.info("JmolUtil checking PNGJ cache for " + pathName);
    String shortName = shortSceneFilename(pathName);
    if (fm.pngjCache == null
        && !clearAndCachePngjFile(fm, new String[] { pathName, null }))
      return null;
    Map<String, Object> cache = fm.pngjCache;
    boolean isMin = (pathName.indexOf(".min.") >= 0);
    if (!isMin) {
      String cName = fm.getCanonicalName(Rdr.getZipRoot(pathName));
      if (!cache.containsKey(cName)
          && !clearAndCachePngjFile(fm, new String[] { pathName, null }))
        return null;
      if (pathName.indexOf("|") < 0)
        shortName = cName;
    }
    if (cache.containsKey(shortName)) {
      Logger.info("FileManager using memory cache " + shortName);
      return (byte[]) fm.pngjCache.get(shortName);
    }
    //    for (String key : pngjCache.keySet())
    //    System.out.println(" key=" + key);
    //System.out.println("FileManager memory cache size=" + pngjCache.size()
    //  + " did not find " + pathName + " as " + shortName);
    if (!isMin || !clearAndCachePngjFile(fm, new String[] { pathName, null }))
      return null;
    Logger.info("FileManager using memory cache " + shortName);
    return (byte[]) cache.get(shortName);
  }

  private boolean clearAndCachePngjFile(FileManager fm, String[] data) {
    fm.pngjCache = new Hashtable<String, Object>();
    if (data == null || data[0] == null)
      return false;
    data[0] = Rdr.getZipRoot(data[0]);
    String shortName = shortSceneFilename(data[0]);
    Map<String, Object> cache = fm.pngjCache;
    try {
      data[1] = ZipTools.cacheZipContents(
          ZipTools.getPngZipStream((BufferedInputStream) fm
              .getBufferedInputStreamOrErrorMessageFromName(data[0], null,
                  false, false, null, false, true), true), shortName, cache,
          false);
    } catch (Exception e) {
      return false;
    }
    if (data[1] == null)
      return false;
    byte[] bytes = data[1].getBytes();
    //System.out.println("jmolutil caching " + bytes.length + " bytes as " + fm.getCanonicalName(data[0]));
    cache.put(fm.getCanonicalName(data[0]), bytes); // marker in case the .all. file is changed
    if (shortName.indexOf("_scene_") >= 0) {
      cache.put(shortSceneFilename(data[0]), bytes); // good for all .min. files of this scene set
      bytes = (byte[]) cache.remove(shortName + "|state.spt");
      if (bytes != null)
        cache.put(shortSceneFilename(data[0] + "|state.spt"), bytes);
    }
    //for (String key : cache.keySet())
    //System.out.println(key);
    return true;
  }

  private String shortSceneFilename(String pathName) {
    int pt = pathName.indexOf("_scene_") + 7;
    if (pt < 7)
      return pathName;
    String s = "";
    if (pathName.endsWith("|state.spt")) {
      int pt1 = pathName.indexOf('.', pt);
      if (pt1 < 0)
        return pathName;
      s = pathName.substring(pt, pt1);
    }
    int pt2 = pathName.lastIndexOf("|");
    return pathName.substring(0, pt) + s
        + (pt2 > 0 ? pathName.substring(pt2) : "");
  }

  /**
   * Called to see if we have a zipped up Mac directory. Assignment can be made
   * if (1) there is only one file in the collection and (2) that file is
   * "xxxx.spardir/"
   * 
   * Note that __MACOS files are ignored by the ZIP file reader.
   * 
   * @param zipDirectory
   * @return subFileList
   */
  private String[] getSpartanSubfiles(String[] zipDirectory) {
    String name = (zipDirectory.length < 2 ? null : zipDirectory[1]);
    return (name == null || zipDirectory.length != 2 || !name.endsWith(".spardir/")? 
        null : new String[] { "", PT.trim(name, "/") });
  }

  /**
   * 
   * check for a Spartan directory. This is not entirely satisfying, because we
   * aren't reading the file in the proper sequence. this code is a hack that
   * should be replaced with the sort of code running in FileManager now. 0
   * entry is not used here, as it is the root directory
   * 
   * @param zipDirectory
   * @return true if a zipped-up Spartan directory
   */
  private boolean isSpartanZip(String[] zipDirectory) {
    for (int i = 1; i < zipDirectory.length; i++)
      if (zipDirectory[i].endsWith(".spardir/")
          || zipDirectory[i].indexOf("_spartandir") >= 0)
        return true;
    return false;
  }

}
