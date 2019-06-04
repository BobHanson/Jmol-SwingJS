package org.jmol.awtjs.swing;

import org.jmol.api.SC;
import org.jmol.awtjs.swing.Component;

public class ButtonGroup {
  
  private String id;
  
  public ButtonGroup() {
    id = Component.newID("bg");
  }
  
  public void add(SC item) {
    ((AbstractButton) item).htmlName = this.id;
  }

}
