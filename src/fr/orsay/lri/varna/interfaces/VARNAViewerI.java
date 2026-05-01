package fr.orsay.lri.varna.interfaces;

public interface VARNAViewerI {

  enum VARNACallBack {
    COLOR,
    DESTROY,
    GETFRAME,
    GETRESNOS,
    SCRIPT,
    SELECT,
    SETDSSR,
    SETPLUGIN,
    ZAP    
  }
  
  String ACTION_SELECT_MODEL = "selectModel";
  String ACTION_SELECT_BASES = "selectBASES";
  String ACTION_HOVER = "hover";
  
  

  
  public Object notifyCallback(VARNACallBack type, Object[] data);

}
