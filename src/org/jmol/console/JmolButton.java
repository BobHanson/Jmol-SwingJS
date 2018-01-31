package org.jmol.console;

import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.jmol.api.JmolAbstractButton;

public class JmolButton extends JButton implements JmolAbstractButton {

  public JmolButton(String text) {
    super(text);
  }

  public JmolButton(ImageIcon ii) {
    super(ii);
  }

  @Override
  public void addConsoleListener(Object console) {
    addActionListener((ActionListener) console);
  }

  @Override
  public String getKey() {
    return null;
  }
  

}
