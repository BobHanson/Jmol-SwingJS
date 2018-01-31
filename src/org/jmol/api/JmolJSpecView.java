package org.jmol.api;

import org.jmol.viewer.Viewer;

public interface JmolJSpecView {

  void setViewer(Viewer vwr);
  
  void atomPicked(int atomIndex);

  void setModel(int modelIndex);

  int getBaseModelIndex(int modelIndex);

  String processSync(String script, int mode);

}
