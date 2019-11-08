package org.jmol.awtjs.swing;

import org.jmol.awtjs.swing.Container;

public abstract class JComponent extends Container {

  protected boolean autoScrolls;
  protected String actionCommand;
  protected Object actionListener;

  protected JComponent(String type) {
    super(type);
  }
  
  public void setAutoscrolls(boolean b) {
    autoScrolls = b;
  }
  
  /** 
   * Note that it will be the job of the JavaScript on the 
   * page to do with actionListener what is desired.
   * 
   * In javax.swing, these methods are in AbstractButton, but
   * this is better for org.jmol.awtjs.swing, reducing the duplication
   * of JTextField's actionListener business. 
   * 
   * @param listener 
   * 
   */
  public void addActionListener(Object listener) {
    actionListener = listener;
  }

  public String getActionCommand() {
    return actionCommand;
  }

  public void setActionCommand(String actionCommand) {
    this.actionCommand = actionCommand;
  }



  
}
