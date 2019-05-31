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
package jspecview.popup;

import org.jmol.popup.GenericPopup;
import org.jmol.popup.PopupResource;

import javajs.util.PT;

import jspecview.api.JSVPanel;
import jspecview.api.JSVPopupMenu;
import jspecview.common.Spectrum;
import jspecview.common.JSVersion;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import org.jmol.awtjs.swing.SC;
import javajs.util.Lst;

abstract public class JSVGenericPopup extends GenericPopup implements
		JSVPopupMenu {

	// list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
	protected final static boolean dumpList = false;
	protected final static int UPDATE_NEVER = -1;
	private final static int UPDATE_ALL = 0;
	private final static int UPDATE_CONFIG = 1;
	private final static int UPDATE_SHOW = 2;

	// public void finalize() {
	// System.out.println("JmolPopup " + this + " finalize");
	// }

  protected JSViewer vwr;
	protected int updateMode;
	

	// private int itemMax = 25;
	// private int titleWidthMax = 20;

	private Lst<String> cnmrPeaks;
	private Lst<String> hnmrPeaks;
	private int aboutComputedMenuBaseCount;
	private boolean allowMenu;
	private boolean zoomEnabled;

	public JSVGenericPopup() {
		// required by reflection
	}

	protected void initialize(JSViewer viewer, PopupResource bundle, String title) {
		this.vwr = viewer;
		initSwing(title, bundle, viewer.getApplet(), viewer.isJS, viewer.isSigned, false);
	}

	// //// JmolPopupInterface methods //////

	// private final static int MENUITEM_HEIGHT = 20;

	@Override
	public void jpiDispose() {
		helper.menuClearListeners(popupMenu);
		popupMenu = thisPopup = null;
	}

	@Override
	public Object jpiGetMenuAsObject() {
		return popupMenu;
	}

	@Override
	public void jpiShow(int x, int y) {
		// main entry point from Viewer
		// called via JmolPopupInterface
		show(x, y, false);
		appRestorePopupMenu();
		menuShowPopup(popupMenu, thisx, thisy);
	}

	@Override
	public void jpiUpdateComputedMenus() {
		if (updateMode == UPDATE_NEVER)
			return;
		updateMode = UPDATE_ALL;
		getViewerData();
		updateFileMenu();
		updateFileTypeDependentMenus();
		updateMode = UPDATE_CONFIG;
		updateAboutSubmenu();
	}

	// /////// protected methods //////////

	@Override
	protected void appCheckItem(String item, SC newMenu) {
    // no special items in JSV menu
	}

	@Override
	protected String appFixLabel(String label) {
		if (label.startsWith("_"))
			label = label.substring(label.indexOf("_", 2) + 1);
		else if (label.equals("VERSION"))
			label = JSVersion.VERSION;
		label = PT.rep(label, "JAVA", "");
		label = PT.rep(label, "CB", "");
		label = PT.rep(label, "Menu", "");
		label = PT.rep(label, "_", " ");
		return label;
	}

	@Override
	protected String getScriptForCallback(SC source, String id, String script) {
		return script;
	}

	@Override
	public String appGetMenuAsString(String title) {
		return (new JSVPopupResourceBundle()).getMenuAsText(title);
	}

	@Override
	protected boolean appGetBooleanProperty(String name) {
		return false; // not implemented in JSV
	}

	@Override
	protected boolean appRunSpecialCheckBox(SC item, String basename, String what,
			boolean TF) {
		// n/a
		return false;
	}

	@Override
	protected void appRestorePopupMenu() {
		thisPopup = popupMenu;
	}

	@Override
	protected void appRunScript(String script) {
		vwr.runScript(script);
	}

	@Override
	protected void appUpdateForShow() {
		thisJsvp = vwr.selectedPanel;
		setEnables(thisJsvp);
		if (updateMode == UPDATE_NEVER)
			return;
		getViewerData();
		updateMode = UPDATE_SHOW;
		updateSpectraMenu();
		updateAboutSubmenu();
	}

	@Override
	protected void appUpdateSpecialCheckBoxValue(SC item, String what, boolean TF) {
		// not used
	}

	// /////// private methods /////////

	private void getViewerData() {
		// cnmrPeaks = (JmolList<String>) modelInfo.get("jdxAtomSelect_13CNMR");
		// hnmrPeaks = (JmolList<String>) modelInfo.get("jdxAtomSelect_1HNMR");
	}

	private void updateFileTypeDependentMenus() {
		// not used
	}

	private void updateFileMenu() {
		Object menu = htMenus.get("fileMenu");
		if (menu == null)
			return;
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

	private void updateAboutSubmenu() {
		SC menu = htMenus.get("aboutComputedMenu");
		if (menu == null)
			return;
		menuRemoveAll(menu, aboutComputedMenuBaseCount);
	}

	@Override
	public boolean getSelected(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCompoundMenu(Lst<PanelNode> panelNodes,
			boolean allowCompoundMenu) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEnabled(boolean allowMenu, boolean zoomEnabled) {
		this.allowMenu = allowMenu;
		this.zoomEnabled = zoomEnabled;
		enableMenus();
	}

	private void enableMenus() {
		// all except About and Zoom disabled
		setItemEnabled("_SIGNED_FileMenu", allowMenu);
		setItemEnabled("ViewMenu", pd != null && allowMenu);
		setItemEnabled("Open_File...", allowMenu);
		setItemEnabled("Open_Simulation...", allowMenu);
		setItemEnabled("Open_URL...", allowMenu);
		setItemEnabled("Save_AsMenu", pd != null && allowMenu);
		setItemEnabled("Export_AsMenu", pd != null && allowMenu);
		setItemEnabled("Append_File...", pd != null && allowMenu);
		setItemEnabled("Append_Simulation...", pd != null && allowMenu);
		setItemEnabled("Append_URL...", pd != null && allowMenu);
		setItemEnabled("Views...", pd != null && allowMenu);
		setItemEnabled("Script", allowMenu);
		setItemEnabled("Print...", pd != null && allowMenu);
		setItemEnabled("ZoomMenu", pd != null && zoomEnabled);
	}

	private PanelData pd;
	protected JSVPanel thisJsvp;

	private void setEnables(JSVPanel jsvp) {
		pd = (jsvp == null ? null : jsvp.getPanelData());
		Spectrum spec0 = (pd == null ? null : pd.getSpectrum());
		boolean isOverlaid = pd != null && pd.isShowAllStacked();
		boolean isSingle = pd != null && pd.haveSelectedSpectrum();

		setItemEnabled("Integration", pd != null && pd.getSpectrum().canIntegrate());
		setItemEnabled("Measurements", true);
				//pd != null && pd.hasCurrentMeasurements(AType.Measurements));
		setItemEnabled("Peaks", pd != null && pd.getSpectrum().is1D());

		setItemEnabled("Predicted_Solution_Colour_(fitted)",
				isSingle && spec0.canShowSolutionColor());
		setItemEnabled("Predicted_Solution_Colour_(interpolated)",
				isSingle && spec0.canShowSolutionColor());
		setItemEnabled("Toggle_Trans/Abs", isSingle && spec0.canConvertTransAbs());
		setItemEnabled("Show_Overlay_Key", isOverlaid
				&& pd.getNumberOfGraphSets() == 1);
		setItemEnabled("Overlay_Offset...", isOverlaid);
		setItemEnabled("JDXMenu", pd != null && spec0.canSaveAsJDX());
		setItemEnabled("Export_AsMenu", pd != null);
		enableMenus();
	}

	private void setItemEnabled(String key, boolean TF) {
		menuEnable(htMenus.get(key), TF);
	}

	@Override
	public void setSelected(String key, boolean TF) {
		SC item = htMenus.get(key);
		if (item == null || item.isSelected() == TF)
			return;
		menuEnable(item, false);
		item.setSelected(TF);
		menuEnable(item, true);
	}

	@Override
	protected String getUnknownCheckBoxScriptToRun(SC item, String name, String what, boolean TF) {
		// not used in JSV
		return null;
	}


}
