/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Universit� Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurMenu;
import fr.orsay.lri.varna.models.rna.RNA;

public class VueMenu extends JPopupMenu {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private VARNAPanel _vp;

	private ControleurMenu _controlerMenu;

	private JCheckBoxMenuItem _itemOptionSpecialBaseColored = new JCheckBoxMenuItem(
			"Custom colored", false);
	private JCheckBoxMenuItem _itemShowWarnings = new JCheckBoxMenuItem(
			"Show warnings", false);
	private JCheckBoxMenuItem _itemDrawBackbone = new JCheckBoxMenuItem(
			"Draw backbone", true);
	private JCheckBoxMenuItem _itemOptionGapsBaseColored = new JCheckBoxMenuItem(
			"Custom colored", false);
	private JCheckBoxMenuItem _itemOptionBondsColored = new JCheckBoxMenuItem(
			"Use base color for base-pairs", false);
	private JCheckBoxMenuItem _itemShowNCBP = new JCheckBoxMenuItem(
			"Show non-canonical BPs", true);
	private JCheckBoxMenuItem _itemShowOnlyPlanar = new JCheckBoxMenuItem(
			"Hide tertiary BPs", false);
	private JCheckBoxMenuItem _itemFlatExteriorLoop = new JCheckBoxMenuItem(
			"Flat exterior loop", false);
	
	private JCheckBoxMenuItem _itemShowColorMap = new JCheckBoxMenuItem(
			"Show color map", false);
	private JMenuItem _dashBasesColor;

	private ArrayList<JComponent> _disabled = new ArrayList<JComponent>();

	private JMenuItem _rotation;
	private JMenuItem _bpHeightIncrement;

	private Point _spawnOrigin = new Point(-1,-1);
	
	public VueMenu(VARNAPanel vp) {
		_vp = vp;
		_controlerMenu = new ControleurMenu(_vp, this);
	}

	private void addTitle(String title, boolean keep) {
		// TOD BH SwingJS -- this should not be necessary
		JSeparator sep = new JPopupMenu.Separator(); // BH SWingJS needs JPopupMenu.Separator
		//JSeparator sep = new JSeparator();
		JMenuItem titleItem = new JMenuItem(" " + title);//BH SwingJS was JLabel -- need to be able to do this.
		// titleItem.setAlignmentX(0.5f);
		Font previousFont = titleItem.getFont();
		Font futureFont = previousFont.deriveFont(Font.BOLD).deriveFont(
				(float) previousFont.getSize() + 1.0f);

		titleItem.setFont(futureFont);
		Color current = titleItem.getForeground();
		Color future = current.brighter().brighter();
		// titleItem.setBackground(future);
		titleItem.setForeground(future);
		add(titleItem);
		add(sep);
		if (!keep) {
			_disabled.add(sep);
			_disabled.add(titleItem);
		}
	}
	
	private void configMenuItem(JMenuItem mi, String command, String keyStroke, Container par)
	{ configMenuItem(mi,command,keyStroke,par,false); }

	private void configMenuItem(JMenuItem mi, String command, String keyStroke, Container par, boolean disabled)
	{ 
		mi.setActionCommand(command);
		mi.addActionListener(_controlerMenu);
		if (keyStroke!=null)
			if (!keyStroke.equals(""))
				mi.setAccelerator(KeyStroke.getKeyStroke(keyStroke));
		if (disabled)
		{ _disabled.add(mi);}
		par.add(mi);
	}
	
	private JMenuItem createMenuItem(String caption, String command, String keyStroke, Container par, boolean disabled)
	{
		JMenuItem mi = new JMenuItem(caption);
		configMenuItem(mi, command,keyStroke, par, disabled);
		return mi;
	}

	private JMenuItem createMenuItem(String caption, String command, String keyStroke, Container par)
	{ return createMenuItem(caption, command, keyStroke, par,false); }


	public void updateDialog() {
		for (int i = 0; i < _disabled.size(); i++) {
			JComponent j = _disabled.get(i);
			j.setVisible(_vp.isModifiable());
		}
		_itemOptionSpecialBaseColored.setState(_vp.getColorSpecialBases());
		_itemShowWarnings.setState(_vp.getShowWarnings());
		_itemOptionGapsBaseColored.setState(_vp.getColorGapsBases());
		_itemOptionGapsBaseColored.setEnabled(_vp.isComparisonMode());
		_dashBasesColor.setEnabled(_vp.isComparisonMode());
		
		_rotation.setEnabled(_vp.getDrawMode() != RNA.DRAW_MODE_LINEAR);
		_bpHeightIncrement.setEnabled(_vp.getDrawMode() == RNA.DRAW_MODE_LINEAR);

		_itemOptionBondsColored.setState(_vp.getUseBaseColorsForBPs());
		_itemShowNCBP.setState(_vp.getShowNonCanonicalBP());
		_itemShowOnlyPlanar.setState(!_vp.getShowNonPlanarBP());
		_itemShowColorMap.setState(_vp.getColorMapVisible());
		_itemFlatExteriorLoop.setState(_vp.getFlatExteriorLoop());
		_itemFlatExteriorLoop.setEnabled(_vp.getDrawMode() == RNA.DRAW_MODE_RADIATE);
	}

	/**
	 * Builds the popup menu
	 */
	public void buildPopupMenu() {
		addTitle("File", true);
		fileMenu();
		exportMenu();
		createMenuItem("Print...", "print", "control P", this);
		addSeparator();

		addTitle("Display", true);
		viewMenu();
		displayMenu();
		//JSeparator sep = new JSeparator();
		// TODO BH SwingJS - this should not be necessary
		JSeparator sep = new JPopupMenu.Separator(); // BH SWingJS needs JPopupMenu.Separator
		add(sep);
		_disabled.add(sep);

		addTitle("Edit", false);
		editRNAMenu();
		redrawMenu();
		colorClassesMenu();
		annotationMenu();
		_disabled.add(_itemShowNCBP);
		_disabled.add(_itemShowOnlyPlanar);
		aboutMenu();
	}

	private void annotationMenu() {
		JMenu submenuAnnotations = new JMenu("Annotations");
		JMenu addAnnotations = new JMenu("New");
		createMenuItem("Here", "annotationsaddPosition", "", addAnnotations);
		createMenuItem("Base", "annotationsaddBase", "", addAnnotations);
		createMenuItem("Loop", "annotationsaddLoop", "", addAnnotations);
		createMenuItem("Helix", "annotationsaddHelix", "", addAnnotations);
		//JSeparator sep = new JSeparator();
		JSeparator sep = new JPopupMenu.Separator(); // BH SWingJS needs JPopupMenu.Separator

		addAnnotations.add(sep);
		createMenuItem("Region", "annotationsaddRegion", "", addAnnotations);
		createMenuItem("Chem. prob.", "annotationsaddChemProb", "", addAnnotations);
		submenuAnnotations.add(addAnnotations);
		createMenuItem("Edit from list...", "annotationsedit", "", submenuAnnotations);
		createMenuItem("Remove from list...", "annotationsremove", "", submenuAnnotations);
		submenuAnnotations.addSeparator();
		createMenuItem("Auto 5'/3'", "annotationsautoextremites", "control alt Q", submenuAnnotations);
		createMenuItem("Auto helices", "annotationsautohelices", "control Q", submenuAnnotations);
		createMenuItem("Auto interior loops", "annotationsautointerior", "alt shift Q", submenuAnnotations);
		createMenuItem("Auto terminal loops", "annotationsautoterminal", "control shift Q", submenuAnnotations);
		add(submenuAnnotations);
	}

	private void fileMenu() {
		createMenuItem("New...", "userInput", "control N", this,true);
		createMenuItem("Open...", "file", "control O", this,true);
		createMenuItem("Save...", "saveas", "control S", this,true);
		JMenu submenuSave = new JMenu("Save as");
		createMenuItem("DBN (Vienna)", "dbn", "", submenuSave);
		createMenuItem("BPSEQ", "bpseq", "", submenuSave);
		createMenuItem("CT", "ct", "", submenuSave);
		add(submenuSave);
	}

	private void exportMenu() {
		// Export menu
		JMenu submenuExport = new JMenu("Export");
		createMenuItem("SVG", "svg", "", submenuExport);
		createMenuItem("PGF/TIKZ", "tikz", "", submenuExport);
		createMenuItem("XFIG", "xfig", "", submenuExport);
		submenuExport.addSeparator();
		createMenuItem("EPS", "eps", "", submenuExport);
		submenuExport.addSeparator();
		createMenuItem("PNG", "png", "", submenuExport);
		createMenuItem("JPEG", "jpeg", "", submenuExport);
		add(submenuExport);
	}


	private void displayMenu() {
		
		// SubMenu Base-pairs
		JMenu subMenuBasePairs = new JMenu("Base Pairs");
		createMenuItem("BP style...", "bpstyle", "control shift P", subMenuBasePairs);
		configMenuItem(_itemShowNCBP, "shownc", "control W", subMenuBasePairs);
		configMenuItem(_itemShowOnlyPlanar, "shownp", "control E", subMenuBasePairs);
		// SubMenu Non standard Bases
		JMenu subMenuNSBases = new JMenu("Non-standard bases");
		configMenuItem(_itemOptionSpecialBaseColored, "specialbasecolored", "control J", subMenuNSBases);
		createMenuItem("Color", "specialBasesColor", "control shift J", subMenuNSBases);
		// SubMenu Gaps Bases
		JMenu subMenuGapsBases = new JMenu("'Gaps' bases");
		configMenuItem(_itemOptionGapsBaseColored, "dashbasecolored", "control D", subMenuGapsBases);		
		_dashBasesColor = createMenuItem("Color", "dashBasesColor", "control shift D", subMenuGapsBases);
		// Removable separator 
		//JSeparator sep = new JSeparator();
		JSeparator sep = new JPopupMenu.Separator(); // BH SWingJS needs JPopupMenu.Separator
		_disabled.add(sep);
		
		// Style menu
		JMenu submenuStyle = new JMenu("RNA style");
		createMenuItem("Toggle draw bases", "gaspin", "alt G", submenuStyle,true);
		submenuStyle.add(subMenuBasePairs);
		submenuStyle.addSeparator();
		submenuStyle.add(subMenuNSBases);
		submenuStyle.add(subMenuGapsBases);
		submenuStyle.add(sep);
		createMenuItem("Backbone color", "backbone", "control K", submenuStyle,true);
		configMenuItem(_itemDrawBackbone, "showbackbone", "alt B", submenuStyle);
		
		// Submenu Title
		JMenu submenuTitle = new JMenu("Title");		
		createMenuItem("Set Title", "setTitle", "control T", submenuTitle, true);
		createMenuItem("Font", "titleDisplay", "control shift T", submenuTitle, true);
		createMenuItem("Color", "titleColor", "control alt T", submenuTitle, true);
		_disabled.add(submenuTitle);

		// Color map menu
		JMenu submenuColorMap = new JMenu("Color map");
		configMenuItem(_itemShowColorMap, "toggleshowcolormap", "control shift L", submenuColorMap, false);
		createMenuItem("Caption", "colormapcaption", "control shift C", submenuColorMap,true);
		createMenuItem("Style...", "colormapstyle", "control L", submenuColorMap,false);
		submenuColorMap.addSeparator();
		createMenuItem("Edit values...", "colormapvalues", "shift L", submenuColorMap,true);
		createMenuItem("Load values...", "colormaploadvalues", "control shift K", submenuColorMap,true);
		_disabled.add(submenuColorMap);
		
		// Menu Misc
		JMenu submenuMisc = new JMenu("Misc");
		createMenuItem("Num. period.", "numPeriod", "control M", submenuMisc);
		createMenuItem("Background color", "background", "control G", submenuMisc);
		submenuMisc.add(submenuTitle);
		
		// Main menu
		add(submenuStyle);		
		add(submenuColorMap);
		add(submenuMisc);

	}
	
	private void editRNAMenu()
	{
		createMenuItem("Bases...","editallbases","",this,true);
		createMenuItem("BasePairs...","editallbps","",this,true);
	}

	private void redrawMenu() {
		JMenu submenuRedraw = new JMenu("Redraw");
		_disabled.add(submenuRedraw);

		JMenu submenuAlgorithms = new JMenu("Algorithm");
		_disabled.add(submenuAlgorithms);

		createMenuItem("Linear","line","control 1",submenuAlgorithms,true);
		createMenuItem("Circular","circular","control 2",submenuAlgorithms,true);
		createMenuItem("Radiate","radiate","control 3",submenuAlgorithms,true);
		createMenuItem("NAView","naview","control 4",submenuAlgorithms,true);
		//createMenuItem("VARNAView","varnaview","control 5",submenuAlgorithms,true);
		//createMenuItem("MOTIFView","motifview","control 6",submenuAlgorithms,true);
		submenuRedraw.add(submenuAlgorithms);
		
		// Sets the height increment in LINEAR_MODE type of drawing
		_bpHeightIncrement = createMenuItem("BP height increment","bpheightincrement","control H",submenuRedraw);
		configMenuItem(_itemFlatExteriorLoop, "flat", "control F", submenuRedraw, true);

		// Item pour le r�glage de l'espace entre chaques bases
		createMenuItem("Space between bases","spaceBetweenBases","control shift S",submenuRedraw,true);		
		createMenuItem("Reset","reset","control shift R",submenuRedraw,true);		

		add(submenuRedraw);
	}

	@SuppressWarnings("unused")
	private void warningMenu() {
		// Menu showWarning
		configMenuItem(_itemShowWarnings, "showwarnings", "", this, true);
	}

	private void viewMenu() {
		// View menu
		JMenu submenuView = new JMenu("View");

		// Zoom submenu
		JMenu zoomDisplay = new JMenu("Zoom");
		createMenuItem("25%","zoom25","",zoomDisplay);
		createMenuItem("50%","zoom50","",zoomDisplay);
		createMenuItem("100%","zoom100","",zoomDisplay);
		createMenuItem("150%","zoom150","",zoomDisplay);
		createMenuItem("200%","zoom200","",zoomDisplay);
		createMenuItem("Custom","zoom","control Z",zoomDisplay);
		submenuView.add(zoomDisplay);		
		_rotation = createMenuItem("Rotation...","rotation","control R",submenuView);
		createMenuItem("Rescale...","rescale","",submenuView);
		submenuView.addSeparator();		
		createMenuItem("Border size","borderSize","control B",submenuView);
		
		add(submenuView);

	}

	JMenu _subMenuBases;

	private Component _selectionMenuIndex = null;

	public void addSelectionMenu(JMenuItem s) {
		_selectionMenuIndex = s;
		_disabled.add(s);
		insert(s, getComponentCount() - 2);
	}

	public void removeSelectionMenu() {
		if (_selectionMenuIndex != null) {
			this.remove(_selectionMenuIndex);
			_selectionMenuIndex = null;
		}
	}

	private void colorClassesMenu() {
		// Menu Bases
		_subMenuBases = new JMenu("Colors");
		_disabled.add(_subMenuBases);
		createMenuItem("By Base","eachKind","control U",_subMenuBases,true);
		createMenuItem("By BP","eachCouple","shift U",_subMenuBases,true);
		createMenuItem("By Position","eachBase","alt U",_subMenuBases,true);
		add(_subMenuBases);
	}

	/**
	 * add default color options to a menu
	 */
	public void addColorOptions(JMenu submenu) {
		createMenuItem("Fill Color",submenu.getActionCommand() + ",InnerColor","",submenu,true);
		createMenuItem("Stroke Color",submenu.getActionCommand() + ",OutlineColor","",submenu,true);
		createMenuItem("Label Color",submenu.getActionCommand() + ",NameColor","",submenu,true);		
		submenu.addSeparator();
		createMenuItem("BP Color",submenu.getActionCommand() + ",BPColor","",submenu,true);		
		createMenuItem("BP Thickness",submenu.getActionCommand() + ",BPThickness","",submenu,true);		
		submenu.addSeparator();
		createMenuItem("Number Color",submenu.getActionCommand() + ",NumberColor","",submenu,true);		
	}

	private void aboutMenu() {
		addSeparator();
		createMenuItem("About VARNA", "about", "control A", this);
	}

	public void addAnnotationMenu(JMenu menu) {
		addAnnotationMenu(menu, false);
	}	
	
	public void addAnnotationMenu(JMenu menu, boolean existingAnnot) {
		String title = "Annotation";
		if (existingAnnot)
		{
			String debut = "";
			String texte = _vp.get_selectedAnnotation().getTexte();
			if (texte.length() < 5)
				debut = texte;
			else
				debut = texte.substring(0, 5) + "...";
			title = "Annotation: " + debut;			
		}
		JMenu menuAnnotation = new JMenu(title);
		if (!existingAnnot)
			createMenuItem("Add",menu.getActionCommand() + "annotationadd","",menuAnnotation,true);		
		createMenuItem("Edit",menu.getActionCommand() + "annotationedit","",menuAnnotation,true);		
		createMenuItem("Remove",menu.getActionCommand() + "annotationremove","",menuAnnotation,true);		
		menu.add(menuAnnotation);
	}	
	
	
	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public ControleurMenu get_controleurMenu() {
		return _controlerMenu;
	}

	public JCheckBoxMenuItem get_itemOptionSpecialBaseColored() {
		return _itemOptionSpecialBaseColored;
	}

	public JCheckBoxMenuItem get_itemShowWarnings() {
		return _itemShowWarnings;
	}

	public JCheckBoxMenuItem get_itemOptionDashBaseColored() {
		return _itemOptionGapsBaseColored;
	}

	public void set_controleurMenu(ControleurMenu menu) {
		_controlerMenu = menu;
	}

	public void set_itemOptionSpecialBaseColored(
			JCheckBoxMenuItem optionSpecialBaseColored) {
		_itemOptionSpecialBaseColored = optionSpecialBaseColored;
	}

	public void set_itemShowWarnings(JCheckBoxMenuItem showWarnings) {
		_itemShowWarnings = showWarnings;
	}

	public void set_itemOptionDashBaseColored(
			JCheckBoxMenuItem optionDashBaseColored) {
		_itemOptionGapsBaseColored = optionDashBaseColored;
	}

	public JMenuItem get_rotation() {
		return _rotation;
	}

	public void set_rotation(JMenuItem _rotation) {
		this._rotation = _rotation;
	}

	public JCheckBoxMenuItem get_itemOptionBondsColored() {
		return _itemOptionBondsColored;
	}

	public void set_itemOptionBondsColored(JCheckBoxMenuItem optionBondsColored) {
		_itemOptionBondsColored = optionBondsColored;
	}


	public void show(Component invoker,int x,int y) {
		 _spawnOrigin = new Point(x,y);
		 super.show(invoker,x,y); 
	 }
	
	public Point getSpawnPoint()
	{
		return _spawnOrigin ;
	}

}