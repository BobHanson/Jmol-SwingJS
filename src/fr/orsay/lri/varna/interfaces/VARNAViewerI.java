package fr.orsay.lri.varna.interfaces;

import fr.orsay.lri.varna.applications.VARNA;

public interface VARNAViewerI {

  String version = VARNA.version;
  String license = VARNA.license;

  enum VARNACallBack {
    COLOR,
    DESTROY,
    GETFRAME,
    GETRESNOS,
    HOVER,
    SCRIPT,
    SELECT,
    SETDSSR,
    SETPLUGIN,
    ZAP    
  }
  
  String ACTION_SELECT_MODEL = "selectModel";
  String ACTION_SELECT_BASES = "selectBases";
  String ACTION_HOVER = "hover";
  

  
  public Object notifyCallback(VARNACallBack type, Object[] data);

}
