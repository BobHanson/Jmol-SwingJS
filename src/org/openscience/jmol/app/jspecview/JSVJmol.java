package org.openscience.jmol.app.jspecview;

import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;

import javax.swing.JFrame;

import org.jmol.api.JSVInterface;
import org.jmol.api.JmolSyncInterface;
import org.jmol.c.CBK;
import org.jmol.script.T;
import org.jmol.viewer.JC;
import org.jmol.viewer.StatusManager;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.DisplayPanel;
import org.openscience.jmol.app.plugins.JSVJmolI;

import javajs.util.PT;
import jspecview.application.JSpecView;
import jspecview.application.MainFrame;

public class JSVJmol implements JSVJmolI, JSVInterface, JmolSyncInterface {
  
  private JSpecView jsv;
  private MainFrame jSpecViewFrame;
  private JFrame jmolFrame;
  private Viewer vwr;
  private DisplayPanel display;

  private boolean jSpecViewForceNew;
  private String lastSimulate;
  private int modificationMode;

  public JSVJmol() {
  }

  @Override
  public void setViewer(Viewer vwr, JFrame jmolFrame) {
    this.vwr = vwr;
    this.jmolFrame = jmolFrame;
    this.display = (DisplayPanel) vwr.display;
    setJSpecView("", false, false);
  }

  public void setJSpecView(String peaks, boolean doLoadCheck,
                           boolean isFileLoad) {
    if (peaks.startsWith(":"))
      peaks = peaks.substring(1);
    if (peaks.equals("none") || peaks.equals("NONESimulate:")) {
      if (jsv != null) {
        jsv.syncScript("close ALL");
        if (jSpecViewFrame != null)
          jSpecViewFrame.awaken(false);
      }
      return;
    }
    boolean isC13 = peaks.equals("C13Simulate:");
    boolean isSimulation = (peaks.equals("H1Simulate:") || isC13);
    boolean isStartup = (peaks.length() == 0 || isSimulation);
    boolean newSim = (isSimulation && !peaks.equals(lastSimulate));
    String data = null;
    if (isSimulation) {
      data = vwr.extractMolData(null);
      if (data == null || data.length() == 0) {
        System.out.println("No MOL data available");
        return;
      }
    }
    if (jsv == null) {
      jsv = new JSpecView(vwr.haveDisplay, this);
      jsv.register("Jmol", this);
      if (vwr.haveDisplay) {
        jsv.setMainFrame(jSpecViewFrame = new MainFrame(jsv,
            vwr.getBoolean(T.jmolinjspecview) ? (Component) vwr.display : null,
            this));
        jSpecViewFrame.setSize(Math.max(1000, jmolFrame.getWidth() + 50),
            600);
        jSpecViewFrame.setLocation(jmolFrame.getLocation().x + 10,
            jmolFrame.getLocation().y + 100);
      } else {
        System.out.println("No display -- continuing headless");
      }
      vwr.setBooleanProperty(JC.PROP_JSPECVIEW, true); // was lowercase
      if (isStartup) {
        doLoadCheck = true;
      }
    }
    if (doLoadCheck || jSpecViewForceNew || newSim) {
      String type = "" + vwr.getP(JC.PROP_MODEL_TYPE);
      if (!isSimulation && type.equalsIgnoreCase("jcampdx")) {
        jSpecViewForceNew = false;
        String file = "" + vwr.getP("_modelFile");
        if (file.indexOf("/") < 0)
          return;
        peaks = "hidden true; load APPEND CHECK " + PT.esc(file)
            + ";view all;select last;hidden false"
            + (newSim && isC13 ? ";scaleby 0.5" : "");
      } else if (isFileLoad && !jSpecViewForceNew && !newSim) {
        return;
      } else {
        jSpecViewForceNew = false;
        if (newSim)
          lastSimulate = peaks;
        String model = "" + vwr.getP("_modelNumber");
        if (data == null) {
          peaks = "hidden false";
        } else {
          data = PT.replaceAllCharacters(data, "&", "_");
          peaks = "hidden true; load APPEND CHECK "
              + (peaks.equals("H1Simulate:") ? "H1 " : "C13 ")
              + PT.esc("id='~" + model + "';" + data)
              + ";view all;select last;hidden false #SYNC_PEAKS";
        }
        isStartup = false;
      }
    }

    if (jSpecViewFrame != null && !jSpecViewFrame.isVisible()) {
      if (peaks.contains("<PeakData"))
        return;
      jSpecViewFrame.awaken(true);
      display.setViewer(vwr);
    }
    if (isStartup)
      peaks = "HIDDEN false";
    System.out.println("sending " + peaks);
    jsv.syncScript(peaks);
  }

  private void checkJSpecView(boolean closeAll) {
    boolean isAfterChange = (modificationMode <= StatusManager.NOTIFY_MOD_COMPLETE);
    if (jsv != null && isAfterChange) {
      jSpecViewForceNew = (jSpecViewFrame != null
          && jSpecViewFrame.isVisible());
      setJSpecView(closeAll ? "none" : "", true, true);
      jSpecViewForceNew = true;
    }
  }

                                       
  // -- JSVInterface -- 

  private static String propertiesFileName = "jspecview.properties";

  /**
   * Called from JSpecView.java
   */
  @Override
  public void setProperties(Properties properties) {
    try {
      FileInputStream fileIn = new FileInputStream(propertiesFileName);
      properties.load(fileIn);
    } catch (Exception e) {
    }
  }

  @Override
  public void saveProperties(Properties properties) {
    // Write out current properties
    try {
      FileOutputStream fileOut = new FileOutputStream(propertiesFileName);
      properties.store(fileOut, "JSpecView Application Properties");
    } catch (Exception e) {
    }
  }

  /**
   * @param withDialog
   * @param frame
   */
  @Override
  public void exitJSpecView(boolean withDialog, Object frame) {
    // no exit from Jmol
  }

  /**
   * @param msg
   */
  @Override
  public void syncToJmol(String msg) {
    // not utilized in Jmol application -- jmolSyncInterface used instead
  }

  @Override
  public void runScript(String script) {
    // not used
    jsv.runScriptNow(script);
  }

  @Override
  public void syncScript(String script) {
    // called from JSpecView to send "Select: <Peaks...." script
    vwr.syncScript(script, "~", 0);
  }

  @Override
  @Deprecated
  public void register(String id, JmolSyncInterface jsi) {
    // this would be a call from the JSpecView applet requesting that Jmol 
    // register the JSpecView applet in the JmolAppletRegistry. 
  }

  @Override
  public boolean isVisible() {
    return (jSpecViewFrame != null && jSpecViewFrame.isVisible());
  }

  @Override
  public void setVisible(boolean b) {
    if (jSpecViewFrame == null)
      return;
    jSpecViewFrame.awaken(false);
    if (b)
      jSpecViewFrame.awaken(true);    
  }

  @Override
  public Map<String, Object> getJSpecViewProperty(String type) {
    if (type.toLowerCase().startsWith("jspecview")) {
      type = type.substring(9);
      if (type.startsWith(":"))
          type = type.substring(1);
      return (jsv == null ? null : jsv.getJSpecViewProperty(type));
    }
    return null;
  }  
  
  
  @Override
  public void notifyCallback(CBK type, Object[] data) {
    switch (type) {
    default:
      break;
    case SYNC:
      String cmd = (String) data[1];
      if (cmd.startsWith(JC.JSV_SYNC_KEYWORD_PREFIX)) {
        setJSpecView(cmd.substring(JC.JSV_SYNC_KEYWORD_PREFIX_LENGTH), false, false);
      }
      break;
    case LOADSTRUCT:
      String fullPathName = (String) data[2];
      checkJSpecView(fullPathName == null);
      break;
    case STRUCTUREMODIFIED:
      int mode = ((Integer) data[1]).intValue();
     // positive values are at the start, negative values are at the end of changes.
     // only looking for ends here
     modificationMode = mode;
     switch (mode) {
     case StatusManager.NOTIFY_MOD_ASSIGN_ATOM:
     case StatusManager.NOTIFY_MOD_ASSIGN_BOND:
     case StatusManager.NOTIFY_MOD_CONNECT_ATOM:
     case StatusManager.NOTIFY_MOD_DELETE_ATOM:
     case StatusManager.NOTIFY_MOD_DELETE_MODELS:
       checkJSpecView(false);
       return;
     }

      break;
    }
  }



  
}