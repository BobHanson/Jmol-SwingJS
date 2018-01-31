package org.jmol.viewer;

import java.io.IOException;

public class JmolAsyncException extends IOException {

  private String fileName;

  public JmolAsyncException(String cacheName) {
    fileName = cacheName;
  }

  public String getFileName() {
    return fileName;
  }
  

}
