package org.jmol.popup;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.jmol.api.GenericMenuInterface;
import org.jmol.api.SC;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

/**
 * 
 * The overall parent of all popup classes in Jmol and JSmol.
 * Contains methods and fields common to the "SwingComponent" SC class, 
 * which allows for both JavaScript (org.jmol.awtjs.swing) and Java (java.awt) components. 
 * 
 * This solution predates Jmol-SwingJS by about six years (2012 vs. 2018)
 * 
 * <pre>
 * abstract GenericPopop 
 * -- abstract JmolGenericPopup
 *   -- abstract JmolPopup
 *      -- AwtJmolPopup
 *      -- JSJmolPopup
 *   -- abstract ModelKitPopup
 *      -- AwtModelKitPopup
 *      -- JSModelKitPopup
 * -- abstract JSVGenericPopup
 *   -- AwtPopup
 *   -- JsPopup
 * </pre>
 * 
 * @author Bob Hanson
 * 
 */
public abstract class GenericPopup implements GenericMenuInterface {

  abstract protected Object getImageIcon(String fileName);

  abstract protected void menuShowPopup(SC popup, int x, int y);

  abstract protected String getUnknownCheckBoxScriptToRun(SC item, String name,
                                                  String what, boolean TF);

  /**
   * Opportunity to do something special with an item. 
   * 
   * @param item
   * @param newMenu
   */
  protected void appCheckItem(String item, SC newMenu) {
  }


  /**
   * Opportunity to do something special with a given submenu is created
   * @param item
   * @param subMenu
   * @param word
   */
  protected void appCheckSpecialMenu(String item, SC subMenu, String word) {
    // when adding a menu item
  }

  abstract protected String appFixLabel(String label);

  abstract protected String getScriptForCallback(SC source, String name, String script);

  abstract protected boolean appGetBooleanProperty(String name);

  abstract protected boolean appRunSpecialCheckBox(SC item, String basename,
                                                  String what, boolean TF);

  abstract protected void appRestorePopupMenu();

  abstract protected void appRunScript(String script);

  abstract protected void appUpdateSpecialCheckBoxValue(SC source,
                                                        String actionCommand,
                                                        boolean selected);

  abstract protected void appUpdateForShow();

  protected PopupHelper helper;

  protected String strMenuStructure;

  protected boolean allowSignedFeatures;
  protected boolean isJS, isApplet, isSigned, isWebGL;
  public int thisx, thisy;

  protected boolean isTainted = true;

  protected String menuName;
  protected SC popupMenu;
  protected SC thisPopup;
  protected Map<String, SC> htCheckbox = new Hashtable<String, SC>();
  protected Object buttonGroup;
  protected String currentMenuItemId;
  protected Map<String, SC> htMenus = new Hashtable<String, SC>();
  private Lst<SC> SignedOnly = new Lst<SC>();

  protected boolean updatingForShow;

  protected void initSwing(String title, PopupResource bundle, Object applet,
                           boolean isJS, boolean isSigned, boolean isWebGL) {
    this.isJS = isJS;
    this.isApplet = (applet != null);
    this.isSigned = isSigned;
    this.isWebGL = isWebGL;
    this.allowSignedFeatures = (!isApplet || isSigned);
    menuName = title;
    popupMenu = helper.menuCreatePopup(title, applet);
    thisPopup = popupMenu;
    htMenus.put(title, popupMenu);
    addMenuItems("", title, popupMenu, bundle);
    try {
      jpiUpdateComputedMenus();
    } catch (NullPointerException e) {
      // ignore -- the frame just wasn't ready yet;
      // updateComputedMenus() will be called again when the frame is ready; 
    }
  }

  protected void addMenuItems(String parentId, String key, SC menu,
                              PopupResource popupResourceBundle) {
    String id = parentId + "." + key;
    String value = popupResourceBundle.getStructure(key);
    if (Logger.debugging)
      Logger.debug(id + " --- " + value);
    if (value == null) {
      menuCreateItem(menu, "#" + key, "", "");
      return;
    }
    // process predefined @terms
    StringTokenizer st = new StringTokenizer(value);
    String item;
    while (value.indexOf("@") >= 0) {
      String s = "";
      while (st.hasMoreTokens())
        s += " " + ((item = st.nextToken()).startsWith("@")
            ? popupResourceBundle.getStructure(item)
            : item);
      value = s.substring(1);
      st = new StringTokenizer(value);
    }
    while (st.hasMoreTokens()) {
      item = st.nextToken();
      if (!checkKey(item))
        continue;
      if ("-".equals(item)) {
        menuAddSeparator(menu);
        helper.menuAddButtonGroup(null);
        continue;
      }
      String label = popupResourceBundle.getWord(item);
      SC newItem = null;
      String script = "";
      boolean isCB = false;
      label = appFixLabel(label == null ? item : label);
      if (label.equals("null")) {
        // user has taken this menu item out
        continue;
      }
      if (item.indexOf("Menu") >= 0) {
        if (item.indexOf("more") < 0)
          helper.menuAddButtonGroup(null);
        SC subMenu = menuNewSubMenu(label, id + "." + item);
        menuAddSubMenu(menu, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle);
        appCheckSpecialMenu(item, subMenu, label);
        newItem = subMenu;
      } else if (item.endsWith("Checkbox")
          || (isCB = (item.endsWith("CB") || item.endsWith("RD")))) {
        // could be "PRD" -- set picking radio
        script = popupResourceBundle.getStructure(item);
        String basename = item.substring(0, item.length() - (!isCB ? 8 : 2));
        boolean isRadio = (isCB && item.endsWith("RD"));
        if (script == null || script.length() == 0 && !isRadio)
          script = "set " + basename + " T/F";
        newItem = menuCreateCheckboxItem(menu, label, basename + ":" + script,
            id + "." + item, false, isRadio);
        rememberCheckbox(basename, newItem);
        if (isRadio)
          helper.menuAddButtonGroup(newItem);
      } else {
        script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newItem = menuCreateItem(menu, label, script, id + "." + item);
      }
      // menus or menu items:
      htMenus.put(item, newItem);
      // signed items are listed, but not enabled
      if (item.startsWith("SIGNED")) {
        SignedOnly.addLast(newItem);
        if (!allowSignedFeatures)
          menuEnable(newItem, false);
      }
      appCheckItem(item, newItem);
    }
  }

  protected void updateSignedAppletItems() {
    for (int i = SignedOnly.size(); --i >= 0;)
      menuEnable(SignedOnly.get(i), allowSignedFeatures);
  }

  /**
   * @param key
   * @return true unless a JAVA-only key in JavaScript
   */
  private boolean checkKey(String key) {
    return (key.indexOf(isApplet ? "JAVA" : "APPLET") < 0
        && (!isWebGL || key.indexOf("NOGL") < 0));
  }

  private void rememberCheckbox(String key, SC checkboxMenuItem) {
    htCheckbox.put(key + "::" + htCheckbox.size(), checkboxMenuItem);
  }

  protected void updateButton(SC b, String entry, String script) {
    String[] ret = new String[] { entry };
    Object icon = getEntryIcon(ret);
    entry = ret[0];
    b.init(entry, icon, script, thisPopup);
    isTainted = true;
  }

  protected Object getEntryIcon(String[] ret) {
    String entry = ret[0];
    if (!entry.startsWith("<"))
      return null;
    int pt = entry.indexOf(">");
    ret[0] = entry.substring(pt + 1);
    String fileName = entry.substring(1, pt);
    return getImageIcon(fileName);
  }

  protected SC addMenuItem(SC menuItem, String entry) {
    return menuCreateItem(menuItem, entry, "", null);
  }

  protected void menuSetLabel(SC m, String entry) {
    m.setText(entry);
    isTainted = true;
  }

  /////// run time event-driven methods

  abstract public void menuFocusCallback(String name, String actionCommand, boolean gained);

  public void menuClickCallback(SC source, String script) {
    doMenuClickCallback(source, script);
  }

  protected void doMenuClickCallback(SC source, String script) {
    appRestorePopupMenu();
    if (script == null || script.length() == 0)
      return;
    if (script.equals("MAIN")) {
      show(thisx, thisy, true);
      return;
    }
    String id = menuGetId(source);
    if (id != null) {
      script = getScriptForCallback(source, id, script);
      currentMenuItemId = id;
    }
    if (script != null)
      appRunScript(script);
  }

  public void menuCheckBoxCallback(SC source) {
    doMenuCheckBoxCallback(source);
  }

  protected void doMenuCheckBoxCallback(SC source) {
    appRestorePopupMenu();
    boolean isSelected = source.isSelected();
    String what = source.getActionCommand();
    runCheckBoxScript(source, what, isSelected);
    appUpdateSpecialCheckBoxValue(source, what, isSelected);
    isTainted = true;
    String id = menuGetId(source);
    if (id != null) {
      currentMenuItemId = id;
    }
  }

  private void runCheckBoxScript(SC item, String what, boolean TF) {
    if (!item.isEnabled())
      return;
    if (what.indexOf("##") < 0) {
      int pt = what.indexOf(":");
      if (pt < 0) {
        Logger.error("check box " + item + " IS " + what);
        return;
      }
      // name:trueAction|falseAction
      String basename = what.substring(0, pt);
      if (appRunSpecialCheckBox(item, basename, what, TF))
        return;
      what = what.substring(pt + 1);
      if ((pt = what.indexOf("|")) >= 0)
        what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
      what = PT.rep(what, "T/F", (TF ? " TRUE" : " FALSE"));
    }
    appRunScript(what);
  }

  protected SC menuCreateItem(SC menu, String entry, String script, String id) {
    SC item = helper.getMenuItem(entry);
    item.addActionListener(helper);
    return newMenuItem(item, menu, entry, script, id);
  }

  protected SC menuCreateCheckboxItem(SC menu, String entry, String basename,
                                      String id, boolean state,
                                      boolean isRadio) {
    SC jmi = (isRadio ? helper.getRadio(entry) : helper.getCheckBox(entry));
    jmi.setSelected(state);
    jmi.addItemListener(helper);
    return newMenuItem(jmi, menu, entry, basename, id);
  }

  protected void menuAddSeparator(SC menu) {
    menu.add(helper.getMenuItem(null));
    isTainted = true;
  }

  protected SC menuNewSubMenu(String entry, String id) {
    SC jm = helper.getMenu(entry);
    jm.addMouseListener(helper);
    updateButton(jm, entry, null);
    jm.setName(id);
    jm.setAutoscrolls(true);
    return jm;
  }

  protected void menuRemoveAll(SC menu, int indexFrom) {
    if (indexFrom <= 0)
      menu.removeAll();
    else
      for (int i = menu.getComponentCount(); --i >= indexFrom;)
        menu.remove(i);
    isTainted = true;
  }

  private SC newMenuItem(SC item, SC menu, String text, String script,
                         String id) {
    updateButton(item, text, script);
    item.addMouseListener(helper);
    item.setName(id == null ? menu.getName() + "." : id);
    menuAddItem(menu, item);
    return item;
  }

  protected SC setText(String item, String text) {
    SC m = htMenus.get(item);
    if (m != null)
      m.setText(text);
    return m;
  }

  private void menuAddItem(SC menu, SC item) {
    menu.add(item);
    isTainted = true;
  }

  protected void menuAddSubMenu(SC menu, SC subMenu) {
    subMenu.addMouseListener(helper);
    menuAddItem(menu, subMenu);
  }

  protected void menuEnable(SC component, boolean enable) {
    if (component == null || component.isEnabled() == enable)
      return;
    component.setEnabled(enable);
  }

  protected String menuGetId(SC menu) {
    return menu.getName();
  }

  protected void menuSetAutoscrolls(SC menu) {
    menu.setAutoscrolls(true);
    isTainted = true;
  }

  protected int menuGetListPosition(SC item) {
    SC p = (SC) item.getParent();
    int i;
    for (i = p.getComponentCount(); --i >= 0;)
      if (helper.getSwingComponent(p.getComponent(i)) == item)
        break;
    return i;
  }

  protected void show(int x, int y, boolean doPopup) {
    appUpdateForShow();
    updateCheckBoxesForShow();
    if (doPopup)
      menuShowPopup(popupMenu, thisx, thisy);
  }

  private void updateCheckBoxesForShow() {
    for (Map.Entry<String, SC> entry : htCheckbox.entrySet()) {
      String key = entry.getKey();
      SC item = entry.getValue();
      String basename = key.substring(0, key.indexOf(":"));
      boolean b = appGetBooleanProperty(basename);
      updatingForShow = true;
      if (item.isSelected() != b) {
        item.setSelected(b);
        isTainted = true;
      }
      updatingForShow = false;
    }
  }

  @Override
  public String jpiGetMenuAsString(String title) {
    appUpdateForShow();
    int pt = title.indexOf("|");
    if (pt >= 0) {
      String type = title.substring(pt);
      title = title.substring(0, pt);
      if (type.indexOf("current") >= 0) {
        SB sb = new SB();
        SC menu = htMenus.get(menuName);
        menuGetAsText(sb, 0, menu, "PopupMenu");
        return sb.toString();
      }
    }
    return appGetMenuAsString(title);
  }

  protected String appGetMenuAsString(String title) {
    // main Jmol menu and JSpecView menu only
    return null;
  }

  private void menuGetAsText(SB sb, int level, SC menu, String menuName) {
    String name = menuName;
    Object[] subMenus = menu.getComponents();
    String flags = null;
    String script = null;
    String text = null;
    char key = 'S';
    for (int i = 0; i < subMenus.length; i++) {
      SC source = helper.getSwingComponent(subMenus[i]);
      int type = helper.getItemType(source);
      switch (type) {
      case 4:
        key = 'M';
        name = source.getName();
        flags = "enabled:" + source.isEnabled();
        text = source.getText();
        script = null;
        break;
      case 0:
        key = 'S';
        flags = script = text = null;
        break;
      default:
        key = 'I';
        flags = "enabled:" + source.isEnabled();
        if (type == 2 || type == 3)
          flags += ";checked:" + source.isSelected();
        script = getScriptForCallback(source, source.getName(), source.getActionCommand());
        name = source.getName();
        text = source.getText();
        break;
      }
      addItemText(sb, key, level, name, text, script, flags);
      if (type == 2)
        menuGetAsText(sb, level + 1, helper.getSwingComponent(source.getPopupMenu()),
            name);
    }
  }

  private static void addItemText(SB sb, char type, int level, String name,
                                  String label, String script, String flags) {
    sb.appendC(type).appendI(level).appendC('\t').append(name);
    if (label == null) {
      sb.append(".\n");
      return;
    }
    sb.append("\t").append(label).append("\t")
        .append(script == null || script.length() == 0 ? "-" : script)
        .append("\t").append(flags).append("\n");
  }

  static protected int convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512 * 1024)
      num += 512 * 1024;
    return (int) (num / (1024 * 1024));
  }

}
