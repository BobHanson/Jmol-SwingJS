package org.jmol.console;

import javax.swing.JLabel;

import org.jmol.api.JmolAbstractButton;

public class JmolLabel extends JLabel implements JmolAbstractButton {

  public JmolLabel(String text, int horizontalAlignment) {
    super(text, horizontalAlignment);
  }

  // unused:
  
  @Override
  public String getKey() {
    return null;
  }

  @Override
  public boolean isSelected() {
    return false;
  }

  @Override
  public void setMnemonic(char mnemonic) {
  }

  @Override
  public void addConsoleListener(Object console) {
  }

}
