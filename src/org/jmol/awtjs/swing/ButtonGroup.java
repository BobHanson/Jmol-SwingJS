package javajs.swing;

import javajs.api.SC;
import javajs.awt.Component;

public class ButtonGroup {
  
  private String id;
  
  public ButtonGroup() {
    id = Component.newID("bg");
  }
  
  public void add(SC item) {
    ((AbstractButton) item).htmlName = this.id;
  }

}
