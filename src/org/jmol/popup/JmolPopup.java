/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.popup;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.i18n.GT;
import org.jmol.i18n.Language;
import org.jmol.modelset.Group;
import org.jmol.script.T;
import org.jmol.util.Elements;
import org.jmol.viewer.JC;

import org.jmol.awtjs.swing.SC;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * An abstract popup class that is
 * instantiated for a given platform and
 * context as one of:
 * 
 * <pre>
 *   -- abstract JmolPopup
 *      -- AwtJmolPopup
 *      -- JSJmolPopup
 * </pre>
 * 
 */
abstract public class JmolPopup extends JmolGenericPopup {

  //list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc

  protected final static int UPDATE_NEVER = -1;
  private final static int UPDATE_ALL = 0;
  private final static int UPDATE_CONFIG = 1;
  private final static int UPDATE_SHOW = 2;

  //public void finalize() {
  //  Sygstem.out.println("JmolPopup " + this + " finalize");
  //}

  protected int updateMode = UPDATE_ALL;

  private static final int itemMax = 25;
  private int titleWidthMax = 20;
  private String nullModelSetName, modelSetName;
  private String modelSetFileName, modelSetRoot;
  private String currentFrankId = null;
  private String configurationSelected = "";
  private String altlocs;

  private Object[][] frankList = new Object[10][]; //enough to cover menu drilling

  private Map<String, Object> modelSetInfo;
  private Map<String, Object> modelInfo;

  private Lst<SC> NotPDB = new Lst<SC>();
  private Lst<SC> PDBOnly = new Lst<SC>();
  private Lst<SC> FileUnitOnly = new Lst<SC>();
  private Lst<SC> FileMolOnly = new Lst<SC>();
  private Lst<SC> UnitcellOnly = new Lst<SC>();
  private Lst<SC> SingleModelOnly = new Lst<SC>();
  private Lst<SC> FramesOnly = new Lst<SC>();
  private Lst<SC> VibrationOnly = new Lst<SC>();
  private Lst<SC> Special = new Lst<SC>();
  private Lst<SC> SymmetryOnly = new Lst<SC>();
  private Lst<SC> ChargesOnly = new Lst<SC>();
  private Lst<SC> TemperatureOnly = new Lst<SC>();

  private boolean fileHasUnitCell;
  private boolean haveBFactors;
  private boolean haveCharges;
  private boolean isLastFrame;
  private boolean isMultiConfiguration;
  private boolean isMultiFrame;
  private boolean isPDB;
  private boolean hasSymmetry;
  private boolean isUnitCell;
  private boolean isVibration;
  private boolean isZapped;

  private int modelIndex, modelCount, ac;

  private String group3List;
  private int[] group3Counts;
  private Lst<String> cnmrPeaks;
  private Lst<String> hnmrPeaks;

  ////// JmolPopupInterface methods //////

  private final static int MENUITEM_HEIGHT = 20;
  
  @Override
  public void jpiDispose() {
    super.jpiDispose();
    helper.menuClearListeners(frankPopup);
    frankPopup = null;
  }

  @Override
  protected PopupResource getBundle(String menu) {
    return new MainPopupResourceBundle(strMenuStructure = menu,
        menuText);
  }


  @Override
  protected boolean showFrankMenu() {
      // frank has been clicked
      getViewerData();
      setFrankMenu(currentMenuItemId);
      thisx = -thisx - 50;
      if (nFrankList > 1) {
        thisy -= nFrankList * MENUITEM_HEIGHT;
        menuShowPopup(frankPopup, thisx, thisy);
        return false;
      }
      return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void jpiUpdateComputedMenus() {
    if (updateMode == UPDATE_NEVER)
      return;
    isTainted = true;
    updateMode = UPDATE_ALL;
    getViewerData();
    updateSelectMenu();
    updateFileMenu();
    updateElementsComputedMenu(vwr.getElementsPresentBitSet(modelIndex));
    updateHeteroComputedMenu(vwr.ms.getHeteroList(modelIndex));
    updateSurfMoComputedMenu((Map<String, Object>) modelInfo.get("moData"));
    updateFileTypeDependentMenus();
    updatePDBComputedMenus();
    updateMode = UPDATE_CONFIG;
    updateConfigurationComputedMenu();
    updateSYMMETRYComputedMenus();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateLanguageSubmenu();
    updateAboutSubmenu();
  }

  ///////// protected methods //////////

  @Override
  protected void appCheckItem(String item, SC newMenu) {
    if (item.indexOf("!PDB") >= 0) {
      NotPDB.addLast(newMenu);
    } else if (item.indexOf("PDB") >= 0) {
      PDBOnly.addLast(newMenu);
    }
    if (item.indexOf("CHARGE") >= 0) {
      ChargesOnly.addLast(newMenu);
    } else if (item.indexOf("BFACTORS") >= 0) {
      TemperatureOnly.addLast(newMenu);
    } else if (item.indexOf("UNITCELL") >= 0) {
      UnitcellOnly.addLast(newMenu);
    } else if (item.indexOf("FILEUNIT") >= 0) {
      FileUnitOnly.addLast(newMenu);
    } else if (item.indexOf("FILEMOL") >= 0) {
      FileMolOnly.addLast(newMenu);
    }
    if (item.indexOf("!FRAMES") >= 0) {
      SingleModelOnly.addLast(newMenu);
    } else if (item.indexOf("FRAMES") >= 0) {
      FramesOnly.addLast(newMenu);
    }
    if (item.indexOf("VIBRATION") >= 0) {
      VibrationOnly.addLast(newMenu);
    } else if (item.indexOf("SYMMETRY") >= 0) {
      SymmetryOnly.addLast(newMenu);
    }
    if (item.indexOf("SPECIAL") >= 0)
      Special.addLast(newMenu);
  }

  @Override
  protected String appGetMenuAsString(String title) {
    return (new MainPopupResourceBundle(strMenuStructure, null))
        .getMenuAsText(title);
  }

  @Override
  protected String getScriptForCallback(SC source, String id, String script) {
    int pt;
    if (script == "" || id.endsWith("Checkbox"))
      return script;

    if (script.indexOf("SELECT") == 0) {
      return "select thisModel and (" + script.substring(6) + ")";
    }

    if ((pt = id.lastIndexOf("[")) >= 0) {
      // setSpin
      id = id.substring(pt + 1);
      if ((pt = id.indexOf("]")) >= 0)
        id = id.substring(0, pt);
      id = id.replace('_', ' ');
      if (script.indexOf("[]") < 0)
        script = "[] " + script;
      script = script.replace('_', ' ');
      return PT.rep(script, "[]", id);
    } else if (script.indexOf("?FILEROOT?") >= 0) {
      script = PT.rep(script, "FILEROOT?", modelSetRoot);
    } else if (script.indexOf("?FILE?") >= 0) {
      script = PT.rep(script, "FILE?", modelSetFileName);
    } else if (script.indexOf("?PdbId?") >= 0) {
      script = PT.rep(script, "PdbId?", "=xxxx");
    }
    return script;
  }

  @Override
  protected void appRestorePopupMenu() {
    thisPopup = popupMenu;
    // JavaScript does not have to re-insert the menu
    // because it never gets removed in the first place.
    // first entry is just the main item
    if (vwr.isJSNoAWT || nFrankList < 2) // TODO BOBJOB
      return;
    for (int i = nFrankList; --i > 0;) {
      Object[] f = frankList[i];
      helper.menuInsertSubMenu((SC) f[0], (SC) f[1],
          ((Integer) f[2]).intValue());
    }
    nFrankList = 1;
  }

  /**
   * (1) setOption --> set setOption true or set setOption false
   * 
   * @param item
   * 
   * @param what
   *        option to set
   * @param TF
   *        true or false
   */
  @Override
  protected void appUpdateSpecialCheckBoxValue(SC item, String what, boolean TF) {
    if (!updatingForShow && what.indexOf("#CONFIG") >= 0) {
      configurationSelected = what;
      updateConfigurationComputedMenu();
      updateModelSetComputedMenu();
    }
  }

  ///////// private methods /////////

  private void setFrankMenu(String id) {
    if (currentFrankId != null && currentFrankId == id && nFrankList > 0)
      return;
    if (frankPopup == null)
      frankPopup = helper.menuCreatePopup("Frank", vwr.html5Applet);
    // thisPopup is needed by the JavaScript side of the operation
    thisPopup = frankPopup;
    menuRemoveAll(frankPopup, 0);
    menuCreateItem(frankPopup, getMenuText("mainMenuText"), "MAIN", "");
    currentFrankId = id;
    nFrankList = 0;
    frankList[nFrankList++] = new Object[] { null, null, null };
    if (id != null)
      for (int i = id.indexOf(".", 2) + 1;;) {
        int iNew = id.indexOf(".", i);
        if (iNew < 0)
          break;
        SC menu = htMenus.get(id.substring(i, iNew));
        frankList[nFrankList++] = new Object[] { menu.getParent(), menu,
            Integer.valueOf(vwr.isJSNoAWT ? 0 : menuGetListPosition(menu)) };
        menuAddSubMenu(frankPopup, menu);
        i = iNew + 1;
      }
    thisPopup = popupMenu;
  }

  private boolean checkBoolean(String key) {
    return (modelSetInfo != null && modelSetInfo.get(key) == Boolean.TRUE); // not "Boolean.TRUE.equals(...)" (not working in Java2Script yet)
  }

  @SuppressWarnings("unchecked")
  private void getViewerData() {
    modelSetName = vwr.ms.modelSetName;
    modelSetFileName = vwr.getModelSetFileName();
    int i = modelSetFileName.lastIndexOf(".");
    isZapped = (JC.ZAP_TITLE.equals(modelSetName));
    if (isZapped || "string".equals(modelSetFileName)
        || "String[]".equals(modelSetFileName))
      modelSetFileName = "";
    modelSetRoot = modelSetFileName.substring(0,
        i < 0 ? modelSetFileName.length() : i);
    if (modelSetRoot.length() == 0)
      modelSetRoot = "Jmol";
    modelIndex = vwr.am.cmi;
    modelCount = vwr.ms.mc;
    ac = vwr.ms.getAtomCountInModel(modelIndex);
    modelSetInfo = vwr.getModelSetAuxiliaryInfo();
    modelInfo = vwr.ms.getModelAuxiliaryInfo(modelIndex);
    if (modelInfo == null)
      modelInfo = new Hashtable<String, Object>();
    isPDB = checkBoolean("isPDB");
    isMultiFrame = (modelCount > 1);
    hasSymmetry = modelInfo.containsKey("hasSymmetry");
    isUnitCell = modelInfo.containsKey("unitCellParams");
    fileHasUnitCell = (isPDB && isUnitCell || checkBoolean("fileHasUnitCell"));
    isLastFrame = (modelIndex == modelCount - 1);
    altlocs = vwr.ms.getAltLocListInModel(modelIndex);
    isMultiConfiguration = (altlocs.length() > 0);
    isVibration = (vwr.modelHasVibrationVectors(modelIndex));
    haveCharges = (vwr.ms.getPartialCharges() != null);
    haveBFactors = (vwr.getBooleanProperty("haveBFactors"));
    cnmrPeaks = (Lst<String>) modelInfo.get("jdxAtomSelect_13CNMR");
    hnmrPeaks = (Lst<String>) modelInfo.get("jdxAtomSelect_1HNMR");
  }

  @Override
  protected void appCheckSpecialMenu(String item, SC subMenu, String word) {
    // these will need tweaking:
    if ("modelSetMenu".equals(item)) {
      nullModelSetName = word;
      menuEnable(subMenu, false);
    }
  }

  @Override
  protected void appUpdateForShow() {
    if (updateMode == UPDATE_NEVER)
      return;
    isTainted = true;
    getViewerData();
    updateMode = UPDATE_SHOW;
    updateSelectMenu();
    updateSpectraMenu();
    updateFRAMESbyModelComputedMenu();
    updateSceneComputedMenu();
    updateModelSetComputedMenu();
    updateAboutSubmenu();
    for (int i = Special.size(); --i >= 0;)
      updateSpecialMenuItem(Special.get(i));
  }

  private void updateFileMenu() {
    SC menu = htMenus.get("fileMenu");
    if (menu == null)
      return;
    String text = getMenuText("writeFileTextVARIABLE");
    menu = htMenus.get("writeFileTextVARIABLE");
    boolean ignore = (modelSetFileName.equals(JC.ZAP_TITLE) || modelSetFileName
        .equals(""));
    if (ignore) {
      menuSetLabel(menu, "");
      menuEnable(menu, false);
    } else {
      menuSetLabel(menu, GT.o(GT.$(text), modelSetFileName));
      menuEnable(menu, true);
    }
  }

  private String getMenuText(String key) {
    String str = menuText.getProperty(key);
    return (str == null ? key : str);
  }

  private void updateSelectMenu() {
    SC menu = htMenus.get("selectMenuText");
    if (menu == null)
      return;
    menuEnable(menu, ac != 0);
    menuSetLabel(menu, gti("selectMenuText", vwr.slm.getSelectionCount()));
  }

  private void updateElementsComputedMenu(BS elementsPresentBitSet) {
    SC menu = htMenus.get("elementsComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (elementsPresentBitSet == null)
      return;
    for (int i = elementsPresentBitSet.nextSetBit(0); i >= 0; i = elementsPresentBitSet
        .nextSetBit(i + 1)) {
      String elementName = Elements.elementNameFromNumber(i);
      String elementSymbol = Elements.elementSymbolFromNumber(i);
      String entryName = elementSymbol + " - " + elementName;
      menuCreateItem(menu, entryName, "SELECT " + elementName, null);
    }
    for (int i = Elements.firstIsotope; i < Elements.altElementMax; ++i) {
      int n = Elements.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = Elements.altElementNumberFromIndex(i);
        String elementName = Elements.elementNameFromNumber(n);
        String elementSymbol = Elements.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        menuCreateItem(menu, entryName, "SELECT " + elementName, null);
      }
    }
    menuEnable(menu, true);
  }

  private void updateSpectraMenu() {
    SC menuh = htMenus.get("hnmrMenu");
    SC menuc = htMenus.get("cnmrMenu");
    if (menuh != null)
      menuRemoveAll(menuh, 0);
    if (menuc != null)
      menuRemoveAll(menuc, 0);
    SC menu = htMenus.get("spectraMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    // yes binary | not logical || here -- need to try to set both
    boolean isOK = setSpectraMenu(menuh, hnmrPeaks)
        | setSpectraMenu(menuc, cnmrPeaks);
    if (isOK) {
      if (menuh != null)
        menuAddSubMenu(menu, menuh);
      if (menuc != null)
        menuAddSubMenu(menu, menuc);
    }
    menuEnable(menu, isOK);
  }

  private boolean setSpectraMenu(SC menu, Lst<String> peaks) {
    if (menu == null)
      return false;
    menuEnable(menu, false);
    int n = (peaks == null ? 0 : peaks.size());
    if (n == 0)
      return false;
    for (int i = 0; i < n; i++) {
      String peak = peaks.get(i);
      String title = PT.getQuotedAttribute(peak, "title");
      String atoms = PT.getQuotedAttribute(peak, "atoms");
      if (atoms != null)
        menuCreateItem(menu, title,
            "select visible & (@" + PT.rep(atoms, ",", " or @") + ")", "Focus"
                + i);
    }
    menuEnable(menu, true);
    return true;
  }

  private void updateHeteroComputedMenu(Map<String, String> htHetero) {
    SC menu = htMenus.get("PDBheteroComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (htHetero == null)
      return;
    int n = 0;
    for (Map.Entry<String, String> hetero : htHetero.entrySet()) {
      String heteroCode = hetero.getKey();
      String heteroName = hetero.getValue();
      if (heteroName.length() > 20)
        heteroName = heteroName.substring(0, 20) + "...";
      String entryName = heteroCode + " - " + heteroName;
      menuCreateItem(menu, entryName, "SELECT [" + heteroCode + "]", null);
      n++;
    }
    menuEnable(menu, (n > 0));
  }

  @SuppressWarnings("unchecked")
  private void updateSurfMoComputedMenu(Map<String, Object> moData) {
    SC menu = htMenus.get("surfMoComputedMenuText");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    Lst<Map<String, Object>> mos = (moData == null ? null
        : (Lst<Map<String, Object>>) (moData.get("mos")));
    int nOrb = (mos == null ? 0 : mos.size());
    String text = getMenuText("surfMoComputedMenuText");
    if (nOrb == 0) {
      menuSetLabel(menu, GT.o(GT.$(text), ""));
      menuEnable(menu, false);
      return;
    }
    menuSetLabel(menu, GT.i(GT.$(text), nOrb));
    menuEnable(menu, true);
    SC subMenu = menu;
    int nmod = (nOrb % itemMax);
    if (nmod == 0)
      nmod = itemMax;
    int pt = (nOrb > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = nOrb; --i >= 0;) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        if (pt == nmod + 1)
          nmod = itemMax;
        String id = "mo" + pt + "Menu";
        subMenu = menuNewSubMenu(Math.max(i + 2 - nmod, 1) + "..." + (i + 1),
            menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      Map<String, Object> mo = mos.get(i);
      String entryName = "#"
          + (i + 1)
          + " "
          + (mo.containsKey("type") ? mo.get("type") + " " : "")
          + (mo.containsKey("symmetry") ? mo.get("symmetry") + " "
              : "") 
          + (mo.containsKey("occupancy") ? "(" + mo.get("occupancy") + ") " : "")
              
              + (mo.containsKey("energy") ? mo.get("energy") : "");
      String script = "mo " + (i + 1);
      menuCreateItem(subMenu, entryName, script, null);
    }
  }

  private void updateFileTypeDependentMenus() {
    for (int i = NotPDB.size(); --i >= 0;)
      menuEnable(NotPDB.get(i), !isPDB);
    for (int i = PDBOnly.size(); --i >= 0;)
      menuEnable(PDBOnly.get(i), isPDB);
    for (int i = UnitcellOnly.size(); --i >= 0;)
      menuEnable(UnitcellOnly.get(i), isUnitCell);
    for (int i = FileUnitOnly.size(); --i >= 0;)
      menuEnable(FileUnitOnly.get(i), isUnitCell || fileHasUnitCell);
    for (int i = FileMolOnly.size(); --i >= 0;)
      menuEnable(FileMolOnly.get(i), isUnitCell || fileHasUnitCell);
    for (int i = SingleModelOnly.size(); --i >= 0;)
      menuEnable(SingleModelOnly.get(i), isLastFrame);
    for (int i = FramesOnly.size(); --i >= 0;)
      menuEnable(FramesOnly.get(i), isMultiFrame);
    for (int i = VibrationOnly.size(); --i >= 0;)
      menuEnable(VibrationOnly.get(i), isVibration);
    for (int i = SymmetryOnly.size(); --i >= 0;)
      menuEnable(SymmetryOnly.get(i), hasSymmetry && isUnitCell);
    for (int i = ChargesOnly.size(); --i >= 0;)
      menuEnable(ChargesOnly.get(i), haveCharges);
    for (int i = TemperatureOnly.size(); --i >= 0;)
      menuEnable(TemperatureOnly.get(i), haveBFactors);
    updateSignedAppletItems();
  }

  private void updateSceneComputedMenu() {
    SC menu = htMenus.get("sceneComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    String[] scenes = (String[]) vwr.ms.getInfoM("scenes");
    if (scenes == null)
      return;
    for (int i = 0; i < scenes.length; i++)
      menuCreateItem(menu, scenes[i], "restore scene " + PT.esc(scenes[i])
          + " 1.0", null);
    menuEnable(menu, true);
  }

  private void updatePDBComputedMenus() {

    SC menu = htMenus.get("PDBaaResiduesComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);

    SC menu1 = htMenus.get("PDBnucleicResiduesComputedMenu");
    if (menu1 == null)
      return;
    menuRemoveAll(menu1, 0);
    menuEnable(menu1, false);

    SC menu2 = htMenus.get("PDBcarboResiduesComputedMenu");
    if (menu2 == null)
      return;
    menuRemoveAll(menu2, 0);
    menuEnable(menu2, false);
    if (modelSetInfo == null)
      return;
    int n = (modelIndex < 0 ? 0 : modelIndex + 1);
    String[] lists = ((String[]) modelSetInfo.get("group3Lists"));
    group3List = (lists == null ? null : lists[n]);
    group3Counts = (lists == null ? null : ((int[][]) modelSetInfo
        .get("group3Counts"))[n]);

    if (group3List == null)
      return;
    //next is correct as "<=" because it includes "UNK"
    int nItems = 0;
    String groupList = Group.standardGroupList;
    for (int i = 1; i < JC.GROUPID_AMINO_MAX; ++i)
      nItems += updateGroup3List(menu, groupList.substring(i * 6 - 4, i * 6 - 1).trim());
    nItems += augmentGroup3List(menu, "p>", true);
    menuEnable(menu, (nItems > 0));
    menuEnable(htMenus.get("PDBproteinMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu1, "n>", false);
    menuEnable(menu1, nItems > 0);
    menuEnable(htMenus.get("PDBnucleicMenu"), (nItems > 0));
    @SuppressWarnings("unchecked")
    Map<String, Object> dssr = (nItems > 0 && modelIndex >= 0 ? (Map<String, Object>) vwr.ms.getInfo(modelIndex, "dssr") : null);
    if (dssr != null)
      setSecStrucMenu(htMenus.get("aaStructureMenu"), dssr);
    nItems = augmentGroup3List(menu2, "c>", false);
    menuEnable(menu2, nItems > 0);
    menuEnable(htMenus.get("PDBcarboMenu"), (nItems > 0));
  }

  @SuppressWarnings("unchecked")
  private boolean setSecStrucMenu(SC menu, Map<String, Object> dssr) {
     Map<String, Object> counts = (Map<String, Object>) dssr.get("counts");
    if (counts == null)
      return false;
    String[] keys = new String[counts.size()];
    counts.keySet().toArray(keys);
    Arrays.sort(keys);
    if (keys.length == 0)
      return false;
    menu.removeAll();
    for (int i = 0; i < keys.length; i++)
      menuCreateItem(menu, keys[i] + " (" +counts.get(keys[i]) +")", "select modelIndex=" + modelIndex + " && within('dssr', '"+keys[i]+"');", null);
    return true;
  }

  private int updateGroup3List(SC menu, String name) {
    int nItems = 0;
    int n = group3Counts[group3List.indexOf(name) / 6];
    name = name.trim();
    String script = null;
    if (n > 0) {
      script = "SELECT " + name;
      name += "  (" + n + ")";
      nItems++;
    }
    SC item = menuCreateItem(menu, name, script, menuGetId(menu) + "." + name);
    if (n == 0)
      menuEnable(item, false);
    return nItems;
  }

  private int augmentGroup3List(SC menu, String type, boolean addSeparator) {
    int pt = JC.GROUPID_AMINO_MAX * 6 - 6;
    // ...... p>AFN]o>ODH]n>+T ]
    int nItems = 0;
    while (true) {
      pt = group3List.indexOf(type, pt);
      if (pt < 0)
        break;
      if (nItems++ == 0 && addSeparator)
        menuAddSeparator(menu);
      int n = group3Counts[pt / 6];
      String heteroCode = group3List.substring(pt + 2, pt + 5);
      String name = heteroCode + "  (" + n + ")";
      menuCreateItem(menu, name, "SELECT [" + heteroCode + "]", menuGetId(menu)
          + "." + name);
      pt++;
    }
    return nItems;
  }

  private void updateSYMMETRYComputedMenus() {
    updateSYMMETRYSelectComputedMenu();
    updateSYMMETRYShowComputedMenu();
  }

  @SuppressWarnings("unchecked")
  private void updateSYMMETRYShowComputedMenu() {
    SC menu = htMenus.get("SYMMETRYShowComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (!hasSymmetry || modelIndex < 0)
      return;
    Map<String, Object> info = (Map<String, Object>) vwr.getProperty(
        "DATA_API", "spaceGroupInfo", null);
    if (info == null)
      return;
    Object[][] infolist = (Object[][]) info.get("operations");
    if (infolist == null)
      return;
    String name = (String) info.get("spaceGroupName");
    menuSetLabel(menu, name == null ? GT.$("Space Group") : name);
    SC subMenu = menu;
    int pt = (infolist.length > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < infolist.length; i++) {
      if (pt >= 0 && (pt++ % itemMax) == 0) {
        String id = "drawsymop" + pt + "Menu";
        subMenu = menuNewSubMenu(
            (i + 1) + "..." + Math.min(i + itemMax, infolist.length),
            menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      if (i == 0)
        menuEnable(
            menuCreateItem(subMenu, GT.$("none"), "draw sym_* delete", null),
            true);        
      String sym = (String) infolist[i][1]; // XYZoriginal
      if (sym.indexOf("x1") < 0)
        sym = (String) infolist[i][0]; // normalized XYZ
      String entryName = (i + 1) + " " + infolist[i][2] + " (" + sym + ")";
      menuEnable(
          menuCreateItem(subMenu, entryName, "draw SYMOP " + (i + 1), null),
          true);
    }
    menuEnable(menu, true);
  }

  private void updateSYMMETRYSelectComputedMenu() {
    SC menu = htMenus.get("SYMMETRYSelectComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (!hasSymmetry || modelIndex < 0)
      return;
    String[] list = (String[]) modelInfo.get("symmetryOperations");
    if (list == null)
      return;
    int[] cellRange = (int[]) modelInfo.get("unitCellRange");
    boolean haveUnitCellRange = (cellRange != null);
    SC subMenu = menu;
    int nmod = itemMax;
    int pt = (list.length > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < list.length; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "symop" + pt + "Menu";
        subMenu = menuNewSubMenu(
            (i + 1) + "..." + Math.min(i + itemMax, list.length),
            menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String entryName = "symop=" + (i + 1) + " # " + list[i];
      menuEnable(
          menuCreateItem(subMenu, entryName, "SELECT symop=" + (i + 1), null),
          haveUnitCellRange);
    }
    menuEnable(menu, true);
  }

  private void updateFRAMESbyModelComputedMenu() {
    //allowing this in case we move it later
    SC menu = htMenus.get("FRAMESbyModelComputedMenu");
    if (menu == null)
      return;
    menuEnable(menu, (modelCount > 0));
    menuSetLabel(menu, (modelIndex < 0 ? gti("allModelsText", modelCount)
        : gto("modelMenuText", (modelIndex + 1) + "/" + modelCount)));
    menuRemoveAll(menu, 0);
    if (modelCount < 1)
      return;
    if (modelCount > 1)
      menuCreateCheckboxItem(menu, GT.$("All"), "frame 0 ##", null,
          (modelIndex < 0), false);

    SC subMenu = menu;
    int pt = (modelCount > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < modelCount; i++) {
      if (pt >= 0 && (pt++ % itemMax) == 0) {
        String id = "model" + pt + "Menu";
        subMenu = menuNewSubMenu(
            (i + 1) + "..." + Math.min(i + itemMax, modelCount),
            menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String script = "" + vwr.getModelNumberDotted(i);
      String entryName = vwr.getModelName(i);
      String spectrumTypes = (String) vwr.ms.getInfo(i,
          "spectrumTypes");
      if (spectrumTypes != null && entryName.startsWith(spectrumTypes))
        spectrumTypes = null;
      if (!entryName.equals(script)) {
        int ipt = entryName.indexOf(";PATH");
        if (ipt >= 0)
          entryName = entryName.substring(0, ipt);
        if (entryName.indexOf("Model[") == 0
            && (ipt = entryName.indexOf("]:")) >= 0)
          entryName = entryName.substring(ipt + 2);
        entryName = script + ": " + entryName;
      }
      if (entryName.length() > 60)
        entryName = entryName.substring(0, 55) + "...";
      if (spectrumTypes != null)
        entryName += " (" + spectrumTypes + ")";
      menuCreateCheckboxItem(subMenu, entryName, "model " + script + " ##",
          null, (modelIndex == i), false);
    }
  }

  private void updateConfigurationComputedMenu() {
    SC menu = htMenus.get("configurationComputedMenu");
    if (menu == null)
      return;
    menuEnable(menu, isMultiConfiguration);
    if (!isMultiConfiguration)
      return;
    int nAltLocs = altlocs.length();
    menuSetLabel(menu, gti("configurationMenuText", nAltLocs));
    menuRemoveAll(menu, 0);
    String script = "hide none ##CONFIG";
    menuCreateCheckboxItem(menu, GT.$("All"), script, null,
        (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)),
        false);
    for (int i = 0; i < nAltLocs; i++) {
      script = "configuration " + (i + 1)
          + "; hide thisModel and not selected ##CONFIG";
      String entryName = "" + (i + 1) + " -- \"" + altlocs.charAt(i) + "\"";
      menuCreateCheckboxItem(
          menu,
          entryName,
          script,
          null,
          (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)),
          false);
    }
  }

  private final String[] noZapped = { "surfaceMenu", "measureMenu",
      "pickingMenu", "computationMenu",
      "SIGNEDJAVAcaptureMenuSPECIAL" };

  @SuppressWarnings("unchecked")
  private void updateModelSetComputedMenu() {
    SC menu = htMenus.get("modelSetMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuSetLabel(menu, nullModelSetName);
    menuEnable(menu, false);
    for (int i = noZapped.length; --i >= 0;)
      menuEnable(htMenus.get(noZapped[i]), !isZapped);
    if (modelSetName == null || isZapped)
      return;
    if (isMultiFrame) {
      modelSetName = gti("modelSetCollectionText", modelCount);
      if (modelSetName.length() > titleWidthMax)
        modelSetName = modelSetName.substring(0, titleWidthMax) + "...";
    } else if (vwr.getBooleanProperty("hideNameInPopup")) {
      modelSetName = getMenuText("hiddenModelSetText");
    } else if (modelSetName.length() > titleWidthMax) {
      modelSetName = modelSetName.substring(0, titleWidthMax) + "...";
    }
    menuSetLabel(menu, modelSetName);
    menuEnable(menu, true);

    // 100 here is totally arbitrary. You can do a minimization on any number of atoms
    menuEnable(htMenus.get("computationMenu"), ac <= 100);
    addMenuItem(menu, gti("atomsText", ac));
    addMenuItem(menu, gti("bondsText", vwr.ms.getBondCountInModel(modelIndex)));
    if (isPDB) {
      menuAddSeparator(menu);
      addMenuItem(menu,
          gti("groupsText", vwr.ms.getGroupCountInModel(modelIndex)));
      addMenuItem(menu,
          gti("chainsText", vwr.ms.getChainCountInModelWater(modelIndex, false)));
      addMenuItem(menu,
          gti("polymersText", vwr.ms.getBioPolymerCountInModel(modelIndex)));
      SC submenu = htMenus.get("BiomoleculesMenu");
      if (submenu == null) {
        submenu = menuNewSubMenu(GT.$(getMenuText("biomoleculesMenuText")),
            menuGetId(menu) + ".biomolecules");
        menuAddSubMenu(menu, submenu);
      }
      menuRemoveAll(submenu, 0);
      menuEnable(submenu, false);
      Lst<Map<String, Object>> biomolecules;
      if (modelIndex >= 0
          && (biomolecules = (Lst<Map<String, Object>>) vwr
              .ms.getInfo(modelIndex, "biomolecules")) != null) {
        menuEnable(submenu, true);
        int nBiomolecules = biomolecules.size();
        for (int i = 0; i < nBiomolecules; i++) {
          String script = (isMultiFrame ? ""
              : "save orientation;load \"\" FILTER \"biomolecule " + (i + 1)
                  + "\";restore orientation;");
          int nAtoms = ((Integer) biomolecules.get(i).get("atomCount"))
              .intValue();
          String entryName = gto(isMultiFrame ? "biomoleculeText"
              : "loadBiomoleculeText", new Object[] { Integer.valueOf(i + 1),
              Integer.valueOf(nAtoms) });
          menuCreateItem(submenu, entryName, script, null);
        }
      }
    }
    if (isApplet && !vwr.getBooleanProperty("hideNameInPopup")) {
      menuAddSeparator(menu);
      menuCreateItem(menu, gto("viewMenuText", modelSetFileName), "show url",
          null);
    }
  }

  private String gti(String s, int n) {
    return GT.i(GT.$(getMenuText(s)), n);
  }

  private String gto(String s, Object o) {
    return GT.o(GT.$(getMenuText(s)), o);
  }

  private void updateAboutSubmenu() {
    if (isApplet)
      setText("APPLETid", vwr.appletName);

    /**
     * @j2sNative
     * 
     */
    {
      Runtime runtime = Runtime.getRuntime();
      int n = runtime.availableProcessors();
      if (n > 0)
        setText("JAVAprocessors", GT.i(GT.$("{0} processors"), n));
      setText("JAVAmemTotal",
          GT.i(GT.$("{0} MB total"), convertToMegabytes(runtime.totalMemory())));
      //     memFree.setText(GT.i(GT.$("{0} MB free"), convertToMegabytes(runtime.freeMemory())));
      setText("JAVAmemMax",
          GT.i(GT.$("{0} MB maximum"), convertToMegabytes(runtime.maxMemory())));
    }

  }

  private void updateLanguageSubmenu() {
    SC menu = htMenus.get("languageComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    String language = GT.getLanguage();
    String id = menuGetId(menu);
    Language[] languages = GT.getLanguageList(null);
    for (int i = 0, p = 0; i < languages.length; i++) {
      if (language.equals(languages[i].code))
        languages[i].display = true;
      if (languages[i].display) {
        String code = languages[i].code;
        String name = languages[i].language;
        String nativeName = languages[i].nativeLanguage;
        String menuLabel = code + " - " + GT.$(name);
        if ((nativeName != null) && (!nativeName.equals(GT.$(name)))) {
          menuLabel += " - " + nativeName;
        }
        if (p++ > 0 && (p % 4 == 1))
          menuAddSeparator(menu);
        menuCreateCheckboxItem(menu, menuLabel, "language = \"" + code
            + "\" ##" + name, id + "." + code, language.equals(code), false);
      }
    }
  }

  private void updateSpecialMenuItem(SC m) {
    m.setText(getSpecialLabel(m.getName(), m.getText()));
  }

  /**
   * menus or menu items with SPECIAL in their name are sent here for on-the-fly
   * labeling
   * 
   * @param name
   * @param text
   * @return revised text
   */
  protected String getSpecialLabel(String name, String text) {
    int pt = text.indexOf(" (");
    if (pt < 0)
      pt = text.length();
    String info = null;
    if (name.indexOf("captureLooping") >= 0)
      info = (vwr.am.animationReplayMode == T.once ? "ONCE" : "LOOP");
    else if (name.indexOf("captureFps") >= 0)
      info = "" + vwr.getInt(T.animationfps);
    else if (name.indexOf("captureMenu") >= 0)
      info = (vwr.captureParams == null ? GT.$("not capturing") : vwr.fm
          .getFilePath((String) vwr.captureParams.get("captureFileName"),
              false, true)
          + " " + vwr.captureParams.get("captureCount"));
    return (info == null ? text : text.substring(0, pt) + " (" + info + ")");
  }

}
