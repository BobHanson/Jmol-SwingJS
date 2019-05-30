package org.jmol.api;

public interface GenericMenuInterface {

  public void jpiDispose();
  public Object jpiGetMenuAsObject();
  public String jpiGetMenuAsString(String string);
  public void jpiInitialize(PlatformViewer vwr, String menu);
  public void jpiShow(int x, int y);
  public void jpiUpdateComputedMenus();
  
}
