package org.jmol.popup;

import org.jmol.api.SC;

public interface PopupHelper {

  SC menuCreatePopup(String title, Object html5Applet);

  SC getRadio(String name);

  SC getCheckBox(String name);

  SC getMenu(String name);

  SC getMenuItem(String name);

  void menuAddButtonGroup(SC item);

  Object getButtonGroup();

  void menuInsertSubMenu(SC menu, SC subMenu, int index);

  int getItemType(SC m);

  SC getSwingComponent(Object component);

  void menuClearListeners(SC c);
 
}
