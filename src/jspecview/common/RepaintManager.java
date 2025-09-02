package jspecview.common;

import jspecview.api.js.JSVAppletObject;
import jspecview.api.js.JSVToJSmolInterface;

public class RepaintManager {

	public RepaintManager(JSViewer viewer) {
		this.vwr = viewer;
	}
  /////////// thread management ///////////
  
  boolean repaintPending;
	private JSViewer vwr;

  public boolean refresh() {
  	//n++;
    if (repaintPending) {
      return false;
    }
    repaintPending = true;
 	  JSVAppletObject applet = vwr.html5Applet;
    JSVToJSmolInterface jmol = (JSViewer.isJS && !JSViewer.isSwingJS ? JSViewer.jsmolObject : null);
    if (jmol == null) {
      vwr.selectedPanel.repaint();
    } else {
      jmol.repaint(applet, false);
      repaintDone();
    }
    return true;
  }

  synchronized public void repaintDone() {
    repaintPending = false;
      notify(); // to cancel any wait in requestRepaintAndWait()
  }
}
