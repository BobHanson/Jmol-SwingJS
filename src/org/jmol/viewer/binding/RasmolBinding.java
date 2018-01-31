package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class RasmolBinding extends JmolBinding {

  public RasmolBinding() {
    set("selectOrToggle");
  }
    
  @Override
  protected void setSelectBindings() {
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_select);
    bindAction(SINGLE|CLICK|SHIFT|LEFT, ActionManager.ACTION_selectToggle);
  }

}

