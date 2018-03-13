package org.gennbo;

public interface NBOFileAcceptor {

  String getDefaultFilterOption();
  
  void acceptFile(int mode, String dir, String name, String ext,
                  String labelText);
  
}
