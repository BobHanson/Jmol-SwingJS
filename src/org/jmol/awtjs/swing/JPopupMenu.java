package org.jmol.awtjs.swing;

import org.jmol.awtjs.swing.Component;

public class JPopupMenu extends AbstractButton {
  
  // note that in Java Swing JPopupMenu extends 
  // JComponent, but here we extend AbstractButton
  // so that it shares the SwingComponent interface
  
  boolean tainted = true;

  static {
    /**
     * @j2sNative
     * 
     *            SwingController.setDraggable(org.jmol.awtjs.swing.JPopupMenu); 
     */
    {
    }
  }
  
  public JPopupMenu(String name) {
    super("mnu");
    this.name = name;
  }

  public void setInvoker(Object applet) {
    this.applet = applet;
    /**
     * @j2sNative
     * 
     * SwingController.setMenu(this);
     * 
     */
    {}
  }
  
  /**
   * @param applet  
   * @param x 
   * @param y 
   */
  public void show(Component applet, int x, int y) {
    if (applet != null)
       tainted = true;
    /**
     * @j2sNative
     * 
     * SwingController.showMenu(this, x, y);
     * 
     */
    {}
  }

  public void disposeMenu() {
    /**
     * @j2sNative
     * 
     * SwingController.disposeMenu(this);
     */
    {}
  }
  
  @Override
  public String toHTML() {
    return getMenuHTML();
  }

}
