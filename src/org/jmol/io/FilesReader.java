package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.util.Map;

import javajs.api.GenericBinaryDocument;
import javajs.util.DataReader;
import javajs.util.PT;
import javajs.util.Rdr;

import org.jmol.api.Interface;
import org.jmol.api.JmolFilesReaderInterface;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

/**
 * open a set of models residing in different files
 * 
 */
public class FilesReader implements JmolFilesReaderInterface {
  /**
   * 
   */
  private FileManager fm;
  private Viewer vwr;
  private String[] fullPathNamesIn;
  private String[] namesAsGivenIn;
  private String[] fileTypesIn;
  private Object atomSetCollection;
  private DataReader[] dataReaders;
  private Map<String, Object> htParams;
  private boolean isAppend;

  public FilesReader() {
  }

  @Override
  public void set(FileManager fileManager, Viewer vwr, String[] name,
                  String[] nameAsGiven, String[] types, DataReader[] readers,
                  Map<String, Object> htParams, boolean isAppend) {
    fm = fileManager;
    this.vwr = vwr;
    fullPathNamesIn = name;
    namesAsGivenIn = nameAsGiven;
    fileTypesIn = types;
    dataReaders = readers;
    this.htParams = htParams;
    this.isAppend = isAppend;
  }

  @Override
  public void run() {

    if (!isAppend && vwr.displayLoadErrors)
      vwr.zap(false, true, false);

    boolean getReadersOnly = !vwr.displayLoadErrors;
    atomSetCollection = vwr.getModelAdapter().getAtomSetCollectionReaders(
        this, fullPathNamesIn, fileTypesIn, htParams, getReadersOnly);
    dataReaders = null;
    if (getReadersOnly && !(atomSetCollection instanceof String)) {
      atomSetCollection = vwr.getModelAdapter().getAtomSetCollectionFromSet(
          atomSetCollection, null, htParams);
    }
    if (atomSetCollection instanceof String) {
      Logger.error("file ERROR: " + atomSetCollection);
      return;
    }
    if (!isAppend && !vwr.displayLoadErrors)
      vwr.zap(false, true, false);

    fm.setFileInfo(new String[] { dataReaders == null ? fullPathNamesIn[0] : "String[]" });
  }

  /**
   * called by SmartJmolAdapter to request another buffered reader or binary
   * document, rather than opening all the readers at once.
   * 
   * @param i
   *        the reader index
   * @param forceBinary
   * @return a BufferedReader or null in the case of an error
   * 
   */
  @Override
  public Object getBufferedReaderOrBinaryDocument(int i, boolean forceBinary) {
    if (dataReaders != null)
      return (forceBinary ? null : dataReaders[i].getBufferedReader()); // no binary strings
    String name = fullPathNamesIn[i];
//    String[] subFileList = null;
//    htParams.remove("subFileList");
//    if (name.indexOf("|") >= 0 && !htParams.containsKey("isStateScript")) {
//      subFileList = PT.split(name, "|");
//      name = subFileList[0];
//    }
    if (name.contains("#_DOCACHE_"))
      return FileReader.getChangeableReader(vwr, namesAsGivenIn[i], name);
    Object t = fm.getUnzippedReaderOrStreamFromName(name, null, false,
        forceBinary, false, true, htParams);
    if (t instanceof BufferedInputStream && Rdr.isZipS((BufferedInputStream) t)) {
//      if (subFileList != null)
//        htParams.put("subFileList", subFileList);
      String[] zipDirectory = fm.getZipDirectory(name, true, true);
      t = fm.getBufferedInputStreamOrErrorMessageFromName(name,
          fullPathNamesIn[i], false, false, null, false, true);
      t = fm.getJzu().getAtomSetCollectionOrBufferedReaderFromZip(vwr,
          (BufferedInputStream) t, name, zipDirectory, htParams, 1, true);
    }
    return (t instanceof BufferedInputStream ? ((GenericBinaryDocument) Interface
        .getInterface("javajs.util.BinaryDocument", vwr, "file")).setStream(
        (BufferedInputStream) t, true) : t instanceof BufferedReader
        || t instanceof GenericBinaryDocument ? t
        : t == null ? "error opening:" + namesAsGivenIn[i] : (String) t);
  }

  @Override
  public Object getAtomSetCollection() {
    return atomSetCollection;
  }

}
