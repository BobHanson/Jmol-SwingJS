package org.jmol.api.js;

public interface JSmolAppletObject {

  void _refresh();

  void _atomPickedCallback(int imodel, int iatom);

  void _showInfo(boolean b);

  void _clearConsole();

  // TODO -- for all html5Applet
}
