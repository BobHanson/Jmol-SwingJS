package jspecview.api.js;

import javajs.api.js.JSAppletObject;

public interface JSVAppletObject extends JSAppletObject {

  void _search(String value);

  void _updateView(Object panel, String msg);

  int getOption(String[] items, String dialogName, String labelName);

  void _showStatus(String msg, String title);

  void _showTooltip(String s, int x, int y);

}
