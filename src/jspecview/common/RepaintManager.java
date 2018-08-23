package jspecview.common;

import jspecview.api.js.JSVAppletObject;

public class RepaintManager {

	public RepaintManager(JSViewer viewer) {
		this.vwr = viewer;
	}
  /////////// thread management ///////////
  
  boolean repaintPending;
	private JSViewer vwr;

//  private int n;
  public boolean refresh() {
  	//n++;
    if (repaintPending) {
    	//System.out.println("Repaint " + n + " skipped");
      return false;
    }
    repaintPending = true;
    //if (vwr.pd() != null) // all closed
    	//vwr.pd().setTaintedAll();
    @SuppressWarnings("unused")
		JSVAppletObject applet = vwr.html5Applet;
    /**
     * @j2sNative
     * 
     *  if (typeof Jmol != "undefined" && Jmol.repaint && applet) 
     *    Jmol.repaint(applet, false);
     *  this.repaintDone();
     */
    {
    	vwr.selectedPanel.repaint();
    }
    return true;
  }

  synchronized public void repaintDone() {
    repaintPending = false;
      notify(); // to cancel any wait in requestRepaintAndWait()
  }
}
