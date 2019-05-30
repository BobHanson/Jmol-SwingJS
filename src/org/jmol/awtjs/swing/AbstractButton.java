package org.jmol.awtjs.swing;

import org.jmol.awtjs.swing.Component;
import org.jmol.awtjs.swing.SC;

public abstract class AbstractButton extends JComponent implements SC {

  Object itemListener;
  Object applet;
  String htmlName;
  boolean selected;
  
  private SC popupMenu;

  private String icon;

  protected AbstractButton(String type) {
    super(type);
    enabled = true;
  }
  
  @Override
  public void setSelected(boolean selected) {
    this.selected = selected;
    /**
     * @j2sNative
     * 
     * SwingController.setSelected(this);
     * 
     */
    {
    }
  }

  @Override
  public boolean isSelected() {
    return selected;
  }
  
  @Override
  public void addItemListener(Object listener) {
    itemListener = listener;
  }
  
  @Override
  public Object getIcon() {
    return icon;
  }
  
  @Override
  public void setIcon(Object icon) {
    this.icon = (String) icon;
  }

  @Override
  public void init(String text, Object icon, String actionCommand, SC popupMenu) {
    this.text = text;
    this.icon = (String) icon;
    this.actionCommand = actionCommand;
    this.popupMenu = popupMenu;
    /**
     * @j2sNative
     * 
     *  SwingController.initMenuItem(this);
     *  
     */
    {
    }
  }
 
  public SC getTopPopupMenu() {
    // note that JMenu.getPopupMenu refers to ITSELF, not the main one)
    return popupMenu;
  }
  
  @Override
  public void add(SC item) {
    addComponent((Component) item);
  }

  @Override
  public void insert(SC subMenu, int index) {
    // JMenu, JPopupMenu only, but implemented here as well
    // for simplicity
    insertComponent((Component) subMenu, index);
  }

  @Override
  public Object getPopupMenu() {
    // JMenu only
    return null;
  }

  protected String getMenuHTML() {
    String label = (this.icon != null ? this.icon
        : this.text != null ? this.text 
         : null);
    String s = (label == null ? "" : "<li><a>" + label + "</a>"
      + htmlMenuOpener("ul"));
    int n = getComponentCount();
    if (n > 0)
      for(int i = 0; i < n; i++)
        s += getComponent(i).toHTML();
    if (label != null)
      s += "</ul></li>";
    return s;
  }

  protected String htmlMenuOpener(String type) {
    return "<" + type + " id=\"" + this.id + "\"" + (this.enabled ? "" : getHtmlDisabled()) + ">";
  }

  protected String getHtmlDisabled() {
    return " disabled=\"disabled\"";
  }  

}
