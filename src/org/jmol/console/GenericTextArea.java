package org.jmol.console;

public interface GenericTextArea {

  // input
  
  int getCaretPosition(); 

  // output
  
  void append(String message);

  // both
  
  String getText();
  void setText(String text);

}
