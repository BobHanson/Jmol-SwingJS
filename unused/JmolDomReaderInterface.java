package org.jmol.api;

import java.util.Map;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public interface JmolDomReaderInterface extends Runnable {
  
  public void set(FileManager fileManager, Viewer vwr, Object DOMNode, Map<String, Object> htParams);

  public Object getAtomSetCollection();
  
}