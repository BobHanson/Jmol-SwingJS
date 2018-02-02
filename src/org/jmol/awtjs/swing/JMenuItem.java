package org.jmol.awtjs.swing;

public class JMenuItem extends AbstractButton {

  public final int btnType;
  
  public static final int TYPE_SEPARATOR = 0;
  public static final int TYPE_BUTTON = 1;
  public static final int TYPE_CHECKBOX = 2;
  public static final int TYPE_RADIO = 3;
  public static final int TYPE_MENU = 4;


  public JMenuItem(String text) {
    super("btn");
    setText(text);
    btnType = (text == null ? 0 : 1);
  }

  public JMenuItem(String type, int i) {
    super(type);
    btnType = i;
  }

  @Override
  public String toHTML() {
    return htmlMenuOpener("li")
        + (text == null ? "" : "<a>" + htmlLabel() + "</a>") + "</li>";
  }

  @Override
  protected String getHtmlDisabled() {
    return " class=\"ui-state-disabled\"";  
  }

  private String htmlLabel() {
    return (btnType == TYPE_BUTTON ? text 
        : "<label><input id=\"" + id + "-" + (btnType == TYPE_RADIO ? "r" : "c") 
        + "b\" type=\""
        + (btnType == TYPE_RADIO ? "radio\" name=\"" + htmlName : "checkbox")
        + "\" " + (selected ? "checked" : "") + " />" 
        + text + "</label>");
  }


}
