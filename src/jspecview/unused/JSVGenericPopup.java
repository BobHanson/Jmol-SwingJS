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
package jspecview.unused;

//import org.jmol.util.Logger;
//import javajs.util.PT;
//
//import jspecview.api.JSVAbstractMenu;
//import jspecview.api.JSVPanel;
//import jspecview.api.JSVPopupMenu;
//import jspecview.common.JDXSpectrum;
//import jspecview.common.PanelNode;
//import jspecview.common.JSVersion;
//import jspecview.common.JSViewer;
//import jspecview.common.PanelData;
//import jspecview.common.Annotation.AType;
//
//import java.util.Map;
//import java.util.Properties;
//import java.util.StringTokenizer;
//import java.util.Hashtable;
//
//import javajs.util.List;
//import javajs.util.SB;


abstract public class JSVGenericPopup {//implements JSVPopupMenu, JSVAbstractMenu {

//	// list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
//	protected final static boolean dumpList = false;
//	protected final static int UPDATE_NEVER = -1;
//	private final static int UPDATE_ALL = 0;
//	private final static int UPDATE_CONFIG = 1;
//	private final static int UPDATE_SHOW = 2;
//
//	// public void finalize() {
//	// System.out.println("JmolPopup " + this + " finalize");
//	// }
//
//	protected JSViewer viewer;
//	protected Map<String, Object> htCheckbox = new Hashtable<String, Object>();
//	protected Properties menuText = new Properties();
//	protected Object buttonGroup;
//	protected String currentMenuItemId;
//	protected String strMenuStructure;
//	protected int updateMode;
//
//	protected String menuName;
//	private Object popupMenu;
//	protected Object thisPopup;
////	private int itemMax = 25;
////	private int titleWidthMax = 20;
//
//	private Map<String, Object> htMenus = new Hashtable<String, Object>();
//	private List<Object> SignedOnly = new List<Object>();
//	private List<Object> AppletOnly = new List<Object>();
//
//	private boolean allowSignedFeatures;
//	private boolean isJS;
//	private boolean isApplet;
//	private boolean isSigned;
//
//	private List<String> cnmrPeaks;
//	private List<String> hnmrPeaks;
//	private int aboutComputedMenuBaseCount;
//	private boolean allowMenu;
//	private boolean zoomEnabled;
//
//	public JSVGenericPopup() {
//		// required by reflection
//	}
//
//	protected void initialize(JSViewer viewer, PopupResource bundle, String title) {
//		this.viewer = viewer;
//		menuName = title;
//		popupMenu = menuCreatePopup(title);
//		thisPopup = popupMenu;
//		menuSetListeners();
//		htMenus.put(title, popupMenu);
//		isJS = viewer.isJS;
//		allowSignedFeatures = (!viewer.isApplet || viewer.isSigned);
//		addMenuItems("", title, popupMenu, bundle);
//		try {
//			jpiUpdateComputedMenus();
//		} catch (NullPointerException e) {
//			System.out.println("JSVGenericPopup error " + e);
//			// ignore -- the frame just wasn't ready yet;
//			// updateComputedMenus() will be called again when the frame is ready;
//		}
//	}
//
//	// //// JmolPopupInterface methods //////
//
////	private final static int MENUITEM_HEIGHT = 20;
//
//	@Override
//	public void jpiDispose() {
//		menuClearListeners(popupMenu);
//		popupMenu = thisPopup = null;
//	}
//
//	@Override
//	public Object jpiGetMenuAsObject() {
//		return popupMenu;
//	}
//
//	@Override
//	public String jpiGetMenuAsString(String title) {
//		updateForShow();
//		int pt = title.indexOf("|");
//		if (pt >= 0) {
//			String type = title.substring(pt);
//			title = title.substring(0, pt);
//			if (type.indexOf("current") >= 0) {
//				SB sb = new SB();
//				Object menu = htMenus.get(menuName);
//				menuGetAsText(sb, 0, menu, "PopupMenu");
//				return sb.toString();
//			}
//		}
//		return (new JSVPopupResourceBundle()).getMenuAsText(title);
//	}
//
//	@Override
//	public void jpiShow(int x, int y) {
//		// main entry point from Viewer
//		// called via JmolPopupInterface
//		show(x, y, false);
//		restorePopupMenu();
//		menuShowPopup(popupMenu, thisX, thisY);
//	}
//
//	@Override
//	public void jpiUpdateComputedMenus() {
//		if (updateMode == UPDATE_NEVER)
//			return;
//		updateMode = UPDATE_ALL;
//		getViewerData();
//		// System.out.println("jmolPopup updateComputedMenus " + modelSetFileName +
//		// " " + modelSetName + " " + atomCount);
//		updateFileMenu();
//		updateFileTypeDependentMenus();
//		updateMode = UPDATE_CONFIG;
//		updateAboutSubmenu();
//	}
//
//	// /////// protected methods //////////
//
//	/**
//	 * used only by ModelKit
//	 * 
//	 * @param ret
//	 * @return Object
//	 */
//	protected Object getEntryIcon(String[] ret) {
//		String entry = ret[0];
//		if (!entry.startsWith("<"))
//			return null;
//		int pt = entry.indexOf(">");
//		ret[0] = entry.substring(pt + 1);
//		String fileName = entry.substring(1, pt);
//		return getImageIcon(fileName);
//	}
//
//	/**
//	 * modelkit menu only
//	 * 
//	 * @param fileName
//	 * @return ImageIcon or null
//	 */
//	protected Object getImageIcon(String fileName) {
//		return null;
//	}
//
//	public void checkBoxStateChanged(Object source) {
//		restorePopupMenu();
//		menuSetCheckBoxValue(source);
//		String id = menuGetId(source);
//		if (id != null) {
//			currentMenuItemId = id;
//		}
//	}
//
//	static protected void addItemText(SB sb, char type, int level, String name,
//			String label, String script, String flags) {
//		sb.appendC(type).appendI(level).appendC('\t').append(name);
//		if (label == null) {
//			sb.append(".\n");
//			return;
//		}
//		sb.append("\t").append(label).append("\t").append(
//				script == null || script.length() == 0 ? "-" : script).append("\t")
//				.append(flags).append("\n");
//	}
//
//	/**
//	 * @param id  
//	 * @param script 
//	 * @return script
//	 */
//	protected String fixScript(String id, String script) {
//		return script;
//	}
//
//	protected void restorePopupMenu() {
//		thisPopup = popupMenu;
//	}
//
//	/**
//	 * (1) setOption --> set setOption true or set setOption false
//	 * 
//	 * @param item
//	 * 
//	 * @param what
//	 *          option to set
//	 * @param TF
//	 *          true or false
//	 */
//	protected void setCheckBoxValue(Object item, String what, boolean TF) {
//		checkForCheckBoxScript(item, what, TF);
//	}
//
//	// /////// private methods /////////
//
//	private void getViewerData() {
//		isApplet = viewer.isApplet;
//		isSigned = viewer.isSigned;
//		// cnmrPeaks = (JmolList<String>) modelInfo.get("jdxAtomSelect_13CNMR");
//		// hnmrPeaks = (JmolList<String>) modelInfo.get("jdxAtomSelect_1HNMR");
//	}
//
//	private void updateFileTypeDependentMenus() {
//		for (int i = SignedOnly.size(); --i >= 0;)
//			menuEnable(SignedOnly.get(i), isSigned || !isApplet);
//		for (int i = AppletOnly.size(); --i >= 0;)
//			menuEnable(AppletOnly.get(i), isApplet);
//	}
//
//	private void addMenuItems(String parentId, String key, Object menu,
//			PopupResource popupResourceBundle) {
//		String id = parentId + "." + key;
//		String value = popupResourceBundle.getStructure(key);
//		if (Logger.debugging)
//			Logger.debug(id + " --- " + value);
//		if (value == null) {
//			menuCreateItem(menu, "#" + key, "", "");
//			return;
//		}
//		// process predefined @terms
//		StringTokenizer st = new StringTokenizer(value);
//		String item;
//		while (value.indexOf("@") >= 0) {
//			String s = "";
//			while (st.hasMoreTokens())
//				s += " "
//						+ ((item = st.nextToken()).startsWith("@") ? popupResourceBundle
//								.getStructure(item) : item);
//			value = s.substring(1);
//			st = new StringTokenizer(value);
//		}
//		while (st.hasMoreTokens()) {
//			item = st.nextToken();
//			if (!checkKey(item))
//				continue;
//			String label = popupResourceBundle.getWord(item);
//			Object newMenu = null;
//			String script = "";
//			boolean isCB = false;
//			label = fixLabel(label == null ? item : label);
//			if (label.equals("null")) {
//				// user has taken this menu item out
//				continue;
//			} else if (item.indexOf("Menu") >= 0) {
//				if (item.indexOf("more") < 0)
//					buttonGroup = null;
//				Object subMenu = menuNewSubMenu(label, id + "." + item);
//				menuAddSubMenu(menu, subMenu);
//				htMenus.put(item, subMenu);
//				if (item.indexOf("Computed") < 0)
//					addMenuItems(id, item, subMenu, popupResourceBundle);
//				newMenu = subMenu;
//			} else if ("-".equals(item)) {
//				menuAddSeparator(menu);
//				continue;
//			} else if ((item.contains("CB") || item.contains("RD"))) {
//				isCB = true;
//				script = popupResourceBundle.getStructure(item);
//				String basename = label;
//				boolean isRadio = (isCB && item.contains("RD"));
//				newMenu = menuCreateCheckboxItem(menu, label,  basename + ":" + script,
//						id + "." + item, false, isRadio);
//				rememberCheckbox(basename, newMenu);
//				if (isRadio)
//					menuAddButtonGroup(newMenu);
//			} else {
//        script = popupResourceBundle.getStructure(item);
//        if (script == null)
//          script = item;
//				if (!isJS && item.contains("JS"))
//					continue;
//				newMenu = menuCreateItem(menu, label, script, id + "." + item);
//			}
//
//			if (!allowSignedFeatures && item.contains("SIGNED"))
//				menuEnable(newMenu, false);
//			if (item.startsWith("SIGNED"))
//				SignedOnly.addLast(newMenu);
//			htMenus.put(item, newMenu);
//			// menus or menu items:
//			if (dumpList) {
//				String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t"
//						+ label + "\t" + fixScript(id + "." + item, script);
//				str = "addMenuItem('\t" + str + "\t')";
//				Logger.info(str);
//			}
//		}
//	}
//
//	private String fixLabel(String label) {
//		if (label.startsWith("_"))
//			label = label.substring(label.indexOf("_", 2) + 1);
//		else if (label.equals("VERSION"))
//			label = JSVersion.VERSION;
//		label = PT.rep(label, "CB", "");
//		label = PT.rep(label, "Menu", "");
//		label = PT.rep(label, "_", " ");
//		return label;
//	}
//
//	/**
//	 * @param key
//	 * @return true unless a JAVA-only key in JavaScript
//	 */
//	private boolean checkKey(String key) {
//		/**
//		 * @j2sNative
//		 * 
//		 *            return (key.indexOf("JAVA") < 0 && !(key.indexOf("NOGL") &&
//		 *            this.viewer.isWebGL));
//		 * 
//		 */
//		{
//			return true;
//		}
//	}
//
//	private void rememberCheckbox(String key, Object checkboxMenuItem) {
//		htCheckbox.put(key + "::" + htCheckbox.size(), checkboxMenuItem);
//	}
//
//	private void checkForCheckBoxScript(Object item, String what, boolean TF) {
//		if (!menuIsEnabled(item))
//			return;
//		if (what.indexOf("##") < 0) {
//			int pt = what.indexOf(":");
//			if (pt < 0) {
//				Logger.error("check box " + item + " IS " + what);
//				return;
//			}
//			// name:trueAction|falseAction
//			String basename = what.substring(0, pt);
//			if (basename.endsWith("P!")) {
//				if (basename.indexOf("??") >= 0) {
//					what = menuSetCheckBoxOption(item, basename, what);
//				} else {
//					if (!TF)
//						return;
//					what = "set picking " + basename.substring(0, basename.length() - 2);
//				}
//			} else {
//				what = what.substring(pt + 1);
//				if ((pt = what.indexOf("|")) >= 0)
//					what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
//				what = PT.rep(what, "T/F", (TF ? " TRUE" : " FALSE"));
//			}
//		}
//		viewer.runScript(what);
//	}
//
//	@Override
//	public void checkMenuClick(Object source, String script) {
//		checkMenuClickGP(source, script);
//	}
//
//	protected void checkMenuClickGP(Object source, String script) {
//		restorePopupMenu();
//		if (script == null || script.length() == 0)
//			return;
//		String id = menuGetId(source);
//		if (id != null) {
//			script = fixScript(id, script);
//			currentMenuItemId = id;
//		}
//		viewer.runScript(script);
//	}
//
//	private void updateFileMenu() {
//		Object menu = htMenus.get("fileMenu");
//		if (menu == null)
//			return;
//	}
//
//	private void updateSpectraMenu() {
//		Object menuh = htMenus.get("hnmrMenu");
//		Object menuc = htMenus.get("cnmrMenu");
//		if (menuh != null)
//			menuRemoveAll(menuh, 0);
//		if (menuc != null)
//			menuRemoveAll(menuc, 0);
//		Object menu = htMenus.get("spectraMenu");
//		if (menu == null)
//			return;
//		menuRemoveAll(menu, 0);
//		// yes binary | not logical || here -- need to try to set both
//		boolean isOK = setSpectraMenu(menuh, hnmrPeaks)
//				| setSpectraMenu(menuc, cnmrPeaks);
//		if (isOK) {
//			if (menuh != null)
//				menuAddSubMenu(menu, menuh);
//			if (menuc != null)
//				menuAddSubMenu(menu, menuc);
//		}
//		menuEnable(menu, isOK);
//	}
//
//	private boolean setSpectraMenu(Object menu, List<String> peaks) {
//		if (menu == null)
//			return false;
//		menuEnable(menu, false);
//		int n = (peaks == null ? 0 : peaks.size());
//		if (n == 0)
//			return false;
//		for (int i = 0; i < n; i++) {
//			String peak = peaks.get(i);
//			String title = PT.getQuotedAttribute(peak, "title");
//			String atoms = PT.getQuotedAttribute(peak, "atoms");
//			if (atoms != null)
//				menuCreateItem(menu, title, "select visible & (@"
//						+ PT.rep(atoms, ",", " or @") + ")", "Focus" + i);
//		}
//		menuEnable(menu, true);
//		return true;
//	}
//
//	private void updateAboutSubmenu() {
//		Object menu = htMenus.get("aboutComputedMenu");
//		if (menu == null)
//			return;
//		menuRemoveAll(menu, aboutComputedMenuBaseCount);
//	}
//
//	private void updateForShow() {
//		if (updateMode == UPDATE_NEVER)
//			return;
//		getViewerData();
//		updateMode = UPDATE_SHOW;
//		updateSpectraMenu();
//		updateAboutSubmenu();
//	}
//
//	
//	private void show(int x, int y, boolean doPopup) {
//		thisJsvp = viewer.selectedPanel;
//		setEnables(thisJsvp);
//		if (x == Integer.MIN_VALUE) {
//			x = thisX;
//			y = thisY;
//		} else {
//			thisX = x;
//			thisY = y;
//		}
//		updateForShow();
//		if (doPopup)
//			menuShowPopup(popupMenu, thisX, thisY);
//	}
//
//	public void checkMenuFocus(String name, String cmd, boolean isFocus) {
//		if (name.indexOf("Focus") < 0)
//			return;
//		if (isFocus)
//			viewer.runScript(cmd);
//	}
//
//	@Override
//	public boolean getSelected(String key) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public void setCompoundMenu(List<PanelNode> panelNodes,
//			boolean allowCompoundMenu) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void setEnabled(boolean allowMenu, boolean zoomEnabled) {
//		this.allowMenu = allowMenu;
//		this.zoomEnabled = zoomEnabled;
//		enableMenus();
//	}
//	
//  private void enableMenus() {
//		// all except About and Zoom disabled
//		setItemEnabled("_SIGNED_FileMenu", allowMenu);
//		setItemEnabled("ViewMenu", pd != null && allowMenu);
//		setItemEnabled("Open_File...", allowMenu);
//		setItemEnabled("Open_Simulation...", allowMenu);
//		setItemEnabled("Open_URL...", allowMenu);
//		setItemEnabled("Save_AsMenu", pd != null && allowMenu);
//		setItemEnabled("Export_AsMenu", pd != null && allowMenu);
//		setItemEnabled("Append_File...", pd != null && allowMenu);
//		setItemEnabled("Append_Simulation...", pd != null && allowMenu);
//		setItemEnabled("Append_URL...", pd != null && allowMenu);
//		setItemEnabled("Views...", pd != null && allowMenu);
//		setItemEnabled("Script", allowMenu);
//		setItemEnabled("Print...", pd != null && allowMenu);
//		setItemEnabled("ZoomMenu", pd != null && zoomEnabled);
//	}
//
//
//	private PanelData pd;
//  private int thisX, thisY;
//  protected JSVPanel thisJsvp;
//  
//  private void setEnables(JSVPanel jsvp) {
//    pd = (jsvp == null ? null : jsvp.getPanelData());
//    JDXSpectrum spec0 = (pd == null ? null : pd.getSpectrum());
//    boolean isOverlaid = pd != null && pd.isShowAllStacked();
//    boolean isSingle = pd != null && pd.haveSelectedSpectrum();
//    
//    setItemEnabled("Integration", pd != null && pd.getSpectrum().canIntegrate());
//    setItemEnabled("Measurements", pd != null && pd.hasCurrentMeasurements(AType.Measurements));
//    setItemEnabled("Peaks", pd != null && pd.getSpectrum().is1D());
//    
//    setItemEnabled("Predicted_Solution_Colour", isSingle && spec0.canShowSolutionColor());
//    setItemEnabled("Toggle_Trans/Abs", isSingle && spec0.canConvertTransAbs());
//    setItemEnabled("Show_Overlay_Key", isOverlaid && pd.getNumberOfGraphSets() == 1);
//    setItemEnabled("Overlay_Offset...", isOverlaid);
//    setItemEnabled("JDXMenu", pd != null && spec0.canSaveAsJDX());
//    setItemEnabled("Export_AsMenu", pd != null);
//  	enableMenus();
//  }
//
//  private void setItemEnabled(String key, boolean TF) {
//  	Object item = htMenus.get(key);
//  	if (item == null)
//  		return;
//    menuEnable(item, TF);
//	}
//
//	@Override
//	public void setSelected(String key, boolean TF) {
//  	Object item = htMenus.get(key);
//  	if (item == null)
//  		return;
//    menuEnable(item, false);
//    menuSetCheckBoxState(item, TF);
//    menuEnable(item, true);
//  }
//

}
