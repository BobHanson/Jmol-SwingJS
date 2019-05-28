package org.jmol.popup;

import java.util.Properties;

import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

import javajs.awt.SC;

public abstract class JmolSwingPopup extends GenericSwingPopup {


  protected SC frankPopup;
  protected int nFrankList = 0;
  protected Viewer vwr;
  protected Properties menuText = new Properties();

  @Override
  public void jpiInitialize(PlatformViewer vwr, String menu) {
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = getBundle(menu);
    initialize((Viewer) vwr, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }
  
  abstract protected PopupResource getBundle(String menu);

  protected void initialize(Viewer vwr, PopupResource bundle, String title) {
    this.vwr = vwr;
    initSwing(title, bundle, vwr.html5Applet, vwr.isJSNoAWT, 
        vwr.getBooleanProperty("_signedApplet"), vwr.isWebGL);
  }

  @Override
  public void jpiDispose() {
    helper.menuClearListeners(popupMenu);
    popupMenu = thisPopup = null;
  }

  @Override
  public SC jpiGetMenuAsObject() {
    return popupMenu;
  }


  @Override
  public void jpiUpdateComputedMenus() {
    // main menu only
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // main menu only
  }

  @Override
  protected void appCheckItem(String item, SC newMenu) {
    // main menu only
  }

  @Override
  protected void appCheckSpecialMenu(String item, SC subMenu, String word) {
    // main menu only
  }

  @Override
  protected String appFixLabel(String label) {
    return label;
  }
  
  @Override
  public void jpiShow(int x, int y) {
    if (!vwr.haveDisplay)
      return;
    show(x, y, false);
    if (x < 0 && showFrankMenu())
      return;
    appRestorePopupMenu();
    menuShowPopup(popupMenu, thisx, thisy);
  }

  protected boolean showFrankMenu() {
    return false;
  }

  @Override
  protected String appFixScript(String id, String script) {
    return script;
  }


  @Override
  protected boolean appGetBooleanProperty(String name) {
    return vwr.getBooleanProperty(name);
  }


  @Override
  protected String appGetMenuAsString(String title) {
    // main menu only
    return null;
  }

  @Override
  protected boolean appIsSpecialCheckBox(SC item, String basename, String what,
                                         boolean TF) {
    if (appGetBooleanProperty(basename) == TF)
      return true;
    if (!basename.endsWith("P!"))
      return false;
    if (basename.indexOf("??") >= 0) {
      what = menuSetCheckBoxOption(item, basename, what, TF);
    } else {
      if (!TF)
        return true;
      what = "set picking " + basename.substring(0, basename.length() - 2);
    }
    if (what != null)
      appRunScript(what);
    return true;
  }

  @Override
  protected void appRestorePopupMenu() {
    thisPopup = popupMenu;
  }

  @Override
  protected void appRunScript(String script) {
    vwr.evalStringQuiet(script);
  }


}
