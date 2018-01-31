package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class JmolBinding extends Binding {

  public JmolBinding() {
    set("toggle");
  }
    
  protected void set(String name) {
    this.name = name;
    setGeneralBindings();
    setSelectBindings();
  }
  
  protected void setSelectBindings() {
    // these are only utilized for  set picking select
    bindAction(DOUBLE|CLICK|LEFT, ActionManager.ACTION_select);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_selectToggleExtended);
  }

  private void setGeneralBindings() {
    
    bindAction(SINGLE|CTRL|ALT|LEFT|DRAG, ActionManager.ACTION_translate);
    bindAction(SINGLE|CTRL|RIGHT|DRAG, ActionManager.ACTION_translate);
    bindAction(DOUBLE|SHIFT|LEFT|DRAG, ActionManager.ACTION_translate); 
    bindAction(DOUBLE|MIDDLE|DRAG, ActionManager.ACTION_translate);

    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_rotate);
    bindAction(DOUBLE|LEFT|DRAG, ActionManager.ACTION_rotate);
    bindAction(SINGLE|ALT|LEFT|DRAG, ActionManager.ACTION_rotateZ);
    bindAction(SINGLE|SHIFT|RIGHT|DRAG, ActionManager.ACTION_rotateZ);
    bindAction(SINGLE|SHIFT|LEFT|DRAG, ActionManager.ACTION_rotateZorZoom);
    bindAction(SINGLE|MIDDLE|DRAG, ActionManager.ACTION_rotateZorZoom);
    bindAction(SINGLE|WHEEL, ActionManager.ACTION_wheelZoom);
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_slideZoom);

    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_navTranslate);
    
    bindAction(SINGLE|CTRL|LEFT|DOWN, ActionManager.ACTION_popupMenu);
    bindAction(SINGLE|RIGHT|DOWN, ActionManager.ACTION_popupMenu);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_clickFrank);

    bindAction(SINGLE|CTRL|SHIFT|LEFT|DRAG, ActionManager.ACTION_slab);
    bindAction(DOUBLE|CTRL|SHIFT|LEFT|DRAG, ActionManager.ACTION_depth); 
    bindAction(SINGLE|CTRL|ALT|SHIFT|LEFT|DRAG, ActionManager.ACTION_slabAndDepth);

    bindAction(SINGLE|CTRL|WHEEL, ActionManager.ACTION_wheelZoom);
    bindAction(SINGLE|SHIFT|WHEEL, ActionManager.ACTION_wheelZoom);
    bindAction(SINGLE|CTRL|SHIFT|WHEEL, ActionManager.ACTION_wheelZoom);
    
    bindAction(SINGLE|CTRL|WHEEL, ActionManager.ACTION_slab);
    bindAction(SINGLE|SHIFT|WHEEL, ActionManager.ACTION_depth); 
    bindAction(SINGLE|CTRL|SHIFT|WHEEL, ActionManager.ACTION_slabAndDepth);
    
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_swipe);
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_spinDrawObjectCCW);
    bindAction(SINGLE|SHIFT|LEFT|DRAG, ActionManager.ACTION_spinDrawObjectCW);

    bindAction(SINGLE|ALT|SHIFT|LEFT|DRAG, ActionManager.ACTION_dragSelected);
    bindAction(SINGLE|SHIFT|LEFT|DRAG, ActionManager.ACTION_dragZ);
    bindAction(SINGLE|ALT|LEFT|DRAG, ActionManager.ACTION_rotateSelected);
    bindAction(SINGLE|SHIFT|LEFT|DRAG, ActionManager.ACTION_rotateBranch);

    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_dragLabel);    
    bindAction(SINGLE|ALT|LEFT|DRAG, ActionManager.ACTION_dragDrawPoint);
    bindAction(SINGLE|SHIFT|LEFT|DRAG, ActionManager.ACTION_dragDrawObject);
    
    bindAction(DOUBLE|CLICK|SHIFT|LEFT, ActionManager.ACTION_reset);
    bindAction(DOUBLE|CLICK|MIDDLE, ActionManager.ACTION_reset); 
    
    bindAction(DOUBLE|CLICK|LEFT, ActionManager.ACTION_stopMotion); 
    
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_dragAtom);
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_dragMinimize);
    bindAction(SINGLE|LEFT|DRAG, ActionManager.ACTION_dragMinimizeMolecule);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_pickAtom);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_pickPoint);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_pickLabel);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_pickMeasure);
    bindAction(DOUBLE|CLICK|LEFT, ActionManager.ACTION_setMeasure);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_pickIsosurface); // requires set drawPicking     
    bindAction(SINGLE|CLICK|CTRL|SHIFT|LEFT, ActionManager.ACTION_pickNavigate);      
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_deleteAtom);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_deleteBond);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_connectAtoms);
    bindAction(SINGLE|CLICK|LEFT, ActionManager.ACTION_assignNew);
    bindAction(SINGLE|CLICK|CTRL|SHIFT|LEFT, ActionManager.ACTION_center);
  }
  
}

