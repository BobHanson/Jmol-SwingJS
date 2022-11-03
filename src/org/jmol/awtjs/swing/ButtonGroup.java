package org.jmol.awtjs.swing;

import org.jmol.api.SC;

public class ButtonGroup {
  
  private String id;
  
  public ButtonGroup() {
    id = Component.newID("bg");
  }
  
  private int count;
  
  public void add(SC item) {
    count++;
    ((AbstractButton) item).htmlName = this.id;
  }
  
  public int getButtonCount() {
    return count;
  }

}
