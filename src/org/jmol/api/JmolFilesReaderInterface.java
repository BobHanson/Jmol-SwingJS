package org.jmol.api;

import java.util.Map;

import javajs.util.DataReader;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public interface JmolFilesReaderInterface extends Runnable {
  Object getBufferedReaderOrBinaryDocument(int i, boolean isBinary);
  
  public Object getAtomSetCollection();

  public void set(FileManager fileManager, Viewer vwr, String[] fullPathNames,
           String[] namesAsGiven, String[] fileTypes, DataReader[] readers,
           Map<String, Object> htParams, boolean isAppend);

}