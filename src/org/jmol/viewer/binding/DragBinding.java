package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class DragBinding extends JmolBinding {

  public DragBinding() {
    set("drag");
  }
    
  @Override
  protected void setSelectBindings() {
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_select);
    bindAction(SINGLE|CLICK|SHIFT|LEFT, ActionManager.ACTION_selectToggle);
    bindAction(SINGLE|CLICK|ALT|LEFT, ActionManager.ACTION_selectOr);
    bindAction(SINGLE|CLICK|ALT|SHIFT|LEFT, ActionManager.ACTION_selectAndNot);
    bindAction(SINGLE|DOWN|LEFT, ActionManager.ACTION_selectAndDrag);
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_dragSelected);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_pickAtom);
  }


}

