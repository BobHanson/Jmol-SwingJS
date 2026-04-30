package fr.orsay.lri.varna.interfaces;

public interface VARNAviewerI {

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
  
  public Object notifyCallback(VARNACallBack type, Object[] data);

}
