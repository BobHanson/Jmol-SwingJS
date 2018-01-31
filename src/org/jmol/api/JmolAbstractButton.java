package org.jmol.api;

public interface JmolAbstractButton {

  void setMnemonic(char mnemonic);

  void setToolTipText(String tip);

  void setText(String label);

  String getKey();

  boolean isSelected();

  void setEnabled(boolean b);

  void addConsoleListener(Object console);

  String getName();

}
