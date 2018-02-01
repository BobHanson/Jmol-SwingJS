package org.jmol.api.js;

public interface GenericConsoleTextArea {

  // input
  
  public int getCaretPosition(); 

  // output
  
  public void append(String message);

  // both
  
  public String getText();
  
  public void setText(String text);

}
