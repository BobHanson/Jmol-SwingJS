package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class PfaatBinding extends JmolBinding {

  public PfaatBinding() {
    set("extendedSelect");
  }

  @Override
  protected void setSelectBindings() {
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_select);    
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_selectNone);    
    bindAction(SINGLE|CLICK|SHIFT|LEFT, ActionManager.ACTION_selectToggle);
    bindAction(SINGLE|CLICK|ALT|SHIFT|LEFT, ActionManager.ACTION_selectAndNot);
    bindAction(SINGLE|CLICK|ALT|LEFT, ActionManager.ACTION_selectOr);
  }


}

