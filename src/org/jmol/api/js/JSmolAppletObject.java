package org.jmol.api.js;

import javajs.api.js.JSAppletObject;

public interface JSmolAppletObject extends JSAppletObject {

  void _atomPickedCallback(int imodel, int iatom);

  void _refresh(); // WebGL only

}
