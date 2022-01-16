package org.jmol.viewer;

import org.jmol.modelset.Atom;

import javajs.util.SB;

public interface JmolChimeMessenger {

  JmolChimeMessenger set(Viewer vwr);
  
  String getInfoXYZ(Atom a);

  void reportSelection(int n);

  String scriptCompleted(StatusManager statusManager, String statusMessage,
                         String strErrorMessageUntranslated);

  void showHash(SB outputBuffer, String msg);

  void update(String msg);

  void getAllChimeInfo(SB sb);

}
