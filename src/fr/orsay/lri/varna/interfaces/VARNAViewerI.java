package fr.orsay.lri.varna.interfaces;

public interface VARNAViewerI {

  enum VARNACallBack {
    DESTROY,
    GETFRAME,
    SCRIPT,
    SELECT,
    SETDSSR,
    SETPLUGIN,
    ZAP    
  }
  
  public Object notifyCallback(VARNACallBack type, Object[] data);

}
