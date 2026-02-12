/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.controlers;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBasesComparison;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;


/**
 * Controller of the mouse click
 * 
 * @author darty
 * 
 */
public class ControleurClicMovement implements MouseListener,
		MouseMotionListener, PopupMenuListener {
	private VARNAPanel _vp;
	private boolean _presenceMenuSelection;
	private JMenu _submenuSelection;
	public Point _spawnPoint;	
	public Point _initialPoint;
	public Point _prevPoint;
	public Point _currentPoint;

	public static final double MIN_SELECTION_DISTANCE = 40.0;
	public static final double HYSTERESIS_DISTANCE = 10.0;
	
	private ModeleBase _selectedBase = null;
	
	
	public enum MouseStates {
		NONE,
		MOVE_ELEMENT,
		MOVE_OR_SELECT_ELEMENT,
		SELECT_ELEMENT,
		SELECT_REGION_OR_UNSELECT,
		SELECT_REGION,
		CREATE_BP,
		POPUP_MENU,
		MOVE_ANNOTATION,
	};
	private MouseStates _currentState = MouseStates.NONE;
	


	public ControleurClicMovement(VARNAPanel _vuep) {
		_vp = _vuep;
		_vp.getPopup().addPopupMenuListener(this);
		_presenceMenuSelection = false;
	}

	public void mouseClicked(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent arg0) 
	{
		_vp.requestFocus();
		boolean button1 = (arg0.getButton() == MouseEvent.BUTTON1);
		boolean button2 = (arg0.getButton() == MouseEvent.BUTTON2);
		boolean button3 = (arg0.getButton() == MouseEvent.BUTTON3);
		boolean shift = arg0.isShiftDown();
		boolean ctrl = arg0.isControlDown();
		boolean alt = arg0.isAltDown();
		_vp.removeSelectedAnnotation();
		if (button1 && !ctrl && !alt && !shift)
		{
			if (_vp.isModifiable()) 
			{
				_currentState = MouseStates.MOVE_OR_SELECT_ELEMENT;
				if (_vp.getRealCoords() != null
						&& _vp.getRealCoords().length != 0
						&& _vp.getRNA().get_listeBases().size() != 0) 
				{
					_selectedBase = _vp.getNearestBase(arg0.getX(),arg0.getY(),false,false);
					TextAnnotation selectedAnnotation = _vp.getNearestAnnotation(arg0.getX(), arg0.getY());
					_initialPoint = new Point(arg0.getX(),arg0.getY());
					_currentPoint = new Point(_initialPoint);
					_prevPoint = new Point(_initialPoint);
					if (_selectedBase != null)
					{ 
						if (_vp.getRNA().get_drawMode() == RNA.DRAW_MODE_RADIATE) 
						{
							_vp.highlightSelectedBase(_selectedBase);
						} else {
							if (!_vp.getSelectionIndices().contains(_selectedBase.getIndex()))
							{
								_vp.highlightSelectedBase(_selectedBase);
							}
							else
							{
							  // Otherwise, keep current selection as it is and move it
							}
						}
					}
					else
					{
						if (selectedAnnotation != null)
						{
							_currentState = MouseStates.MOVE_ANNOTATION;
							_vp.set_selectedAnnotation(selectedAnnotation);
							_vp.highlightSelectedAnnotation();
						}
						else
						{
							_vp.clearSelection();
							_selectedBase = null;
							_currentState = MouseStates.SELECT_REGION_OR_UNSELECT;
							_initialPoint = new Point(arg0.getX(),arg0.getY());
							_prevPoint = new Point(_initialPoint);
							_currentPoint = new Point(_initialPoint);
						}
					}
				}
			}
		}
		else if (button1 && ctrl && !alt && !shift)
		{
			_selectedBase = _vp.getNearestBase(arg0.getX(),arg0.getY(),false,false);
			if (_selectedBase != null)
			{ 
				_vp.clearSelection();
				_currentState = MouseStates.CREATE_BP;
				_vp.highlightSelectedBase(_selectedBase);
				_vp.setOriginLink(_vp.logicToPanel(_selectedBase.getCoords()));
				_initialPoint = new Point(arg0.getX(),arg0.getY());
				_currentPoint = new Point(_initialPoint);
			}
		}
		else if (button1 && !ctrl && !alt && shift)
		{
			_currentState = MouseStates.SELECT_ELEMENT;
			_initialPoint = new Point(arg0.getX(),arg0.getY());
			_currentPoint = new Point(_initialPoint);
		}
		else if (button3) 
		{
			_currentState = MouseStates.POPUP_MENU;
			if (_presenceMenuSelection) {
				_vp.getPopupMenu().removeSelectionMenu();
			}
			if ((_vp.getRealCoords() != null) && _vp.getRNA().get_listeBases().size() != 0) {
				updateNearestBase(arg0);
				// on insere dans le menu les nouvelles options
				addMenu(arg0);
				if (_vp.get_selectedAnnotation() != null)
					_vp.highlightSelectedAnnotation();
			}
			// affichage du popup menu
			if (_vp.getRNA().get_drawMode() == RNA.DRAW_MODE_LINEAR) {
				_vp.getPopup().get_rotation().setEnabled(false);
			} else {
				_vp.getPopup().get_rotation().setEnabled(true);
			}
			_vp.getPopup().updateDialog();
			_vp.getPopup().show(_vp, arg0.getX(), arg0.getY());
		}
		_vp.repaint();
	}

	public void mouseDragged(MouseEvent me) {
		if ((_currentState == MouseStates.MOVE_OR_SELECT_ELEMENT)||(_currentState == MouseStates.MOVE_ELEMENT))
		{
			_vp.lockScrolling();

			_currentState = MouseStates.MOVE_ELEMENT;
				// si on deplace la souris et qu'une base est selectionnÃ©e
				if (_selectedBase != null) {
					if (_vp.getRNA().get_drawMode() == RNA.DRAW_MODE_RADIATE) {
						_vp.highlightSelectedStem(_selectedBase);
						// dans le cas radiale on deplace une helice
						_vp.getVARNAUI().UIMoveHelixAtom(_selectedBase.getIndex(), _vp.panelToLogicPoint(new Point2D.Double(me.getX(), me.getY())));
					} else {
						// dans le cas circulaire naview ou line on deplace une base
						_currentPoint = new Point(me.getX(), me.getY());
						moveSelection(_prevPoint,_currentPoint);
						_prevPoint = new Point(_currentPoint);
					}
					_vp.repaint();
				}
		}
		else if (_currentState == MouseStates.MOVE_ANNOTATION)
		{
			if (_vp.get_selectedAnnotation()!=null)
			{
				Point2D.Double p = _vp.panelToLogicPoint(new Point2D.Double(me.getX(), me.getY()));
				_vp.get_selectedAnnotation().setAncrage(p.x,p.y);
				_vp.repaint();
			}			
		}
		else if ((_currentState == MouseStates.SELECT_ELEMENT)||(_currentState == MouseStates.SELECT_REGION_OR_UNSELECT))
		{
			if (_initialPoint.distance(me.getX(),me.getY())>HYSTERESIS_DISTANCE)
				_currentState = MouseStates.SELECT_REGION;
		}
		else if (_currentState == MouseStates.SELECT_REGION)
		{
			_currentPoint = new Point(me.getX(),me.getY());
			int minx = Math.min(_currentPoint.x, _initialPoint.x);
			int miny = Math.min(_currentPoint.y, _initialPoint.y);
			int maxx = Math.max(_currentPoint.x, _initialPoint.x);
			int maxy = Math.max(_currentPoint.y, _initialPoint.y);
			_vp.setSelectionRectangle(new Rectangle(minx,miny,maxx-minx,maxy-miny));
		}
		else if (_currentState == MouseStates.CREATE_BP)
		{
			if (_initialPoint.distance(me.getX(),me.getY())>HYSTERESIS_DISTANCE)
			{
				ModeleBase newSelectedBase = _vp.getNearestBase(me.getX(),me.getY(),false,false);
				_vp.setHoverBase(newSelectedBase);
				if (newSelectedBase==null)
				{
					_vp.setDestinationLink(new Point2D.Double(me.getX(),me.getY()));
					_vp.clearSelection();
					_vp.addToSelection(_selectedBase.getIndex());
				}
				else
				{
					ModeleBase mborig = _selectedBase;
					_vp.clearSelection();
					_vp.addToSelection(newSelectedBase.getIndex());
					_vp.addToSelection(mborig.getIndex());
					_vp.setDestinationLink(_vp.logicToPanel(newSelectedBase.getCoords()));
				}
				_vp.repaint();
			}
			
		}
	}


	public void mouseReleased(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON1)
		{
			_vp.fireBaseClicked(_selectedBase, arg0);	
			//System.out.println(""+_currentState);
			
			if (_currentState == MouseStates.MOVE_ELEMENT)
			{
				_vp.clearSelection();
				_selectedBase = null;
				_vp.unlockScrolling();
				_vp.removeSelectedAnnotation();
			}
			else if (_currentState == MouseStates.SELECT_REGION_OR_UNSELECT)
			{
				_vp.clearSelection();
				_selectedBase = null;
				_vp.removeSelectedAnnotation();
			}
			else if (_currentState == MouseStates.SELECT_ELEMENT)
			{
				if (_vp.getRealCoords() != null
						&& _vp.getRealCoords().length != 0
						&& _vp.getRNA().get_listeBases().size() != 0) 
					{
						int selectedIndex = _vp.getNearestBaseIndex(arg0.getX(),arg0.getY(),false,false);
						if (selectedIndex !=-1)
						{ 
							_vp.toggleSelection(selectedIndex);
						}
					}
				_selectedBase = null;
			}
			else if (_currentState == MouseStates.SELECT_REGION)
			{
			  _vp.removeSelectionRectangle();
			}
			else if (_currentState == MouseStates.CREATE_BP)
			{
				if (_initialPoint.distance(arg0.getX(),arg0.getY())>HYSTERESIS_DISTANCE)
				{
					int selectedIndex = _vp.getNearestBaseIndex(arg0.getX(),arg0.getY(),false,false);
					if (selectedIndex>=0)
					{
						ModeleBase mb = _vp.getNearestBase(arg0.getX(),arg0.getY(),false,false);
						ModeleBase mborig = _selectedBase;
						ModeleBP msbp = new ModeleBP(mb,mborig); 
						if (mb!=mborig)
						{
						  _vp.getVARNAUI().UIAddBP(mb.getIndex(),mborig.getIndex(),msbp);
						}
					}
				}
				_vp.removeLink();
				_vp.clearSelection();
				_vp.repaint();
			}
			else
			{
				_vp.clearSelection();
			}

		}
		_currentState = MouseStates.NONE;
		_vp.repaint();		
	}


	private void addMenu(MouseEvent arg0) {
		// creation du menu
		_submenuSelection = new JMenu("Selection");
		addCurrent();
		// ajout des option sur base
		addMenuBase();
		// ajout des option sur paire de base
		if (_vp.getRNA().get_listeBases().get(_vp.getNearestBase())
				.getElementStructure() != -1) {
			addMenuBasePair();
		}

		// detection renflement
		detectBulge();
		// detection 3'
		detect3Prime();
		// detection 5'
		detect5Prime();
		// detection boucle
		detectLoop();
		// detection d'helice
		detectHelix();
		// detection tige
		detectStem();
		// Ajout de toutes bases
		addAllBase();
		// detection d'annotation
		detectAnnotation(arg0);

		_vp.getPopup().addSelectionMenu(_submenuSelection);
		_presenceMenuSelection = true;
	}

	private void detectAnnotation(MouseEvent arg0) {
		if (_vp.getListeAnnotations().size() != 0) {
			double dist = Double.MAX_VALUE;
			double d2;
			Point2D.Double position;
			for (TextAnnotation textAnnot : _vp.getListeAnnotations()) {
				// calcul de la distance
				position = textAnnot.getCenterPosition();
				position = _vp.transformCoord(position);
				d2 = Math.sqrt(Math.pow((position.x - arg0.getX()), 2)
						+ Math.pow((position.y - arg0.getY()), 2));
				// si la valeur est inferieur au minimum actuel
				if (dist > d2) {
					_vp.set_selectedAnnotation(textAnnot);
					dist = d2;
				}
			}
			_submenuSelection.addSeparator();
			_vp.getPopup().addAnnotationMenu(_submenuSelection,true);
		}
	}

	private void detectBulge() {
		int indiceB = _vp.getNearestBase();
		ArrayList<Integer> indices = _vp.getRNA().findBulge(indiceB);
		if ((indices.size() > 0)
				&& (_vp.getRNA().getHelixCountOnLoop(_vp.getNearestBase()) == 2)) {
			JMenu submenuBulge = new JMenu("Bulge");
			submenuBulge.addChangeListener(new ControleurSelectionHighlight(
					new Vector<Integer>(indices), _vp, submenuBulge));
			submenuBulge.setActionCommand("bulge");
			if (!_vp.isModifiable())
				submenuBulge.setEnabled(false);
			_vp.getPopupMenu().addColorOptions(submenuBulge);
			_submenuSelection.add(submenuBulge);
		}
	}

	private void detectHelix() {
		int indiceH = _vp.getNearestBase();
		ArrayList<Integer> indices = _vp.getRNA().findHelix(indiceH);
		if (indices.size() != 0) {
			// ajout menu helice
			JMenu submenuHelix = new JMenu("Helix");
			submenuHelix.addChangeListener(new ControleurSelectionHighlight(
					new Vector<Integer>(indices), _vp, submenuHelix));
			submenuHelix.setActionCommand("helix");
			if (!_vp.isModifiable())
				submenuHelix.setEnabled(false);
			_vp.getPopupMenu().addColorOptions(submenuHelix);
			submenuHelix.addSeparator();
			_vp.getPopupMenu().addAnnotationMenu(submenuHelix);
			_submenuSelection.add(submenuHelix);
		}
	}

	private void detectStem() {
		int indiceS = _vp.getNearestBase();
		ArrayList<Integer> indices = _vp.getRNA().findStem(indiceS);
		if (indices.size() > 0) {
			JMenu submenuStem = new JMenu("Stem");
			submenuStem.addChangeListener(new ControleurSelectionHighlight(
					new Vector<Integer>(indices), _vp, submenuStem));
			submenuStem.setActionCommand("stem");
			if (!_vp.isModifiable())
				submenuStem.setEnabled(false);
			_vp.getPopupMenu().addColorOptions(submenuStem);
			_submenuSelection.add(submenuStem);
		}
	}

	private void detect3Prime() {
		// detection 3'
		int indice3 = _vp.getNearestBase();
		ArrayList<Integer> indices = _vp.getRNA().find3Prime(indice3);
		if (indices.size() != 0) {
			JMenu submenu3Prime = new JMenu("3'");
			submenu3Prime.addChangeListener(new ControleurSelectionHighlight(
					new Vector<Integer>(indices), _vp, submenu3Prime));
			submenu3Prime.setActionCommand("3'");
			if (!_vp.isModifiable())
				submenu3Prime.setEnabled(false);
			_vp.getPopupMenu().addColorOptions(submenu3Prime);
			_submenuSelection.add(submenu3Prime);
		}
	}

	private void detect5Prime() {
		int indice5 = _vp.getNearestBase();
		ArrayList<Integer> indices = _vp.getRNA().find5Prime(indice5);
		if (indices.size() != 0) {
			JMenu submenu5Prime = new JMenu("5'");
			submenu5Prime.addChangeListener(new ControleurSelectionHighlight(
					new Vector<Integer>(indices), _vp, submenu5Prime));
			submenu5Prime.setActionCommand("5'");
			if (!_vp.isModifiable())
				submenu5Prime.setEnabled(false);
			_vp.getPopupMenu().addColorOptions(submenu5Prime);
			_submenuSelection.add(submenu5Prime);
		}
	}

	private void detectLoop() {
		int indexL = _vp.getNearestBase();
		if (_vp.getRNA().get_listeBases().get(indexL).getElementStructure() == -1) {
			ArrayList<Integer> listLoop = _vp.getRNA().findLoop(indexL);
			JMenu submenuLoop = new JMenu("Loop");
			submenuLoop.addChangeListener(new ControleurSelectionHighlight(
					listLoop, _vp, submenuLoop));
			submenuLoop.setActionCommand("loop1");
			if (!_vp.isModifiable())
				submenuLoop.setEnabled(false);
			_vp.getPopupMenu().addColorOptions(submenuLoop);
			submenuLoop.addSeparator();
			_vp.getPopupMenu().addAnnotationMenu(submenuLoop);
			_submenuSelection.add(submenuLoop);
		} else {
			ArrayList<Integer> listLoop1 = _vp.getRNA().findLoopForward(indexL);
			if (listLoop1.size() > 0) {
				JMenu submenuLoop1 = new JMenu("Forward loop");
				submenuLoop1
						.addChangeListener(new ControleurSelectionHighlight(
								listLoop1, _vp, submenuLoop1));
				submenuLoop1.setActionCommand("loop1");
				if (!_vp.isModifiable())
					submenuLoop1.setEnabled(false);
				_vp.getPopupMenu().addColorOptions(submenuLoop1);
				submenuLoop1.addSeparator();
				_vp.getPopupMenu().addAnnotationMenu(submenuLoop1);
				_submenuSelection.add(submenuLoop1);
			}
			ArrayList<Integer> listLoop2 = _vp.getRNA()
					.findLoopBackward(indexL);
			if (listLoop2.size() > 0) {
				JMenu submenuLoop2 = new JMenu("Backward loop");
				submenuLoop2
						.addChangeListener(new ControleurSelectionHighlight(
								listLoop2, _vp, submenuLoop2));
				submenuLoop2.setActionCommand("loop2");
				if (!_vp.isModifiable())
					submenuLoop2.setEnabled(false);
				_vp.getPopupMenu().addColorOptions(submenuLoop2);
				submenuLoop2.addSeparator();
				_vp.getPopupMenu().addAnnotationMenu(submenuLoop2);
				_submenuSelection.add(submenuLoop2);
			}
		}
	}

	private void addCurrent() {
		Collection<? extends ModeleBase> mbs = _vp.getSelection().getBases();
		if (mbs.size()>0)
		{
		JMenu submenuAll = new JMenu("Current");
		submenuAll.addChangeListener(new ControleurSelectionHighlight(
				mbs, _vp, submenuAll));
		submenuAll.setActionCommand("current");
		if (!_vp.isModifiable())
			submenuAll.setEnabled(false);
		_vp.getPopupMenu().addColorOptions(submenuAll);
		_submenuSelection.add(submenuAll);
		}
	}
	
	
	private void addMenuBase() {
		JMenu submenuBase = new JMenu();
		ModeleBase mb = _vp.getRNA().get_listeBases().get(_vp.getNearestBase());
		if (mb instanceof ModeleBasesComparison) {
			submenuBase.setText("Base #" + (mb.getBaseNumber()) + ":"
					+ ((ModeleBasesComparison) mb).getBases());
		} else {
			submenuBase.setText("Base #" + (mb.getBaseNumber()) + ":"
					+ ((ModeleBaseNucleotide) mb).getBase());
		}
		submenuBase.addChangeListener(new ControleurSelectionHighlight(mb
				.getIndex(), _vp, submenuBase));
		submenuBase.setActionCommand("base");
		// option disponible seulement en mode modifiable
		if (!_vp.isModifiable())
			submenuBase.setEnabled(false);

		JMenuItem baseChar = new JMenuItem("Edit base");
		baseChar.setActionCommand("baseChar");
		baseChar.addActionListener(_vp.getPopupMenu().get_controleurMenu());
		submenuBase.add(baseChar);
		_vp.getPopupMenu().addColorOptions(submenuBase);
		submenuBase.addSeparator();
		_vp.getPopupMenu().addAnnotationMenu(submenuBase);
		_submenuSelection.add(submenuBase);
	}

	private void addAllBase() {
		ArrayList<Integer> indices = _vp.getRNA().findAll();
		JMenu submenuAll = new JMenu("All");
		submenuAll.addChangeListener(new ControleurSelectionHighlight(
				new Vector<Integer>(indices), _vp, submenuAll));
		submenuAll.setActionCommand("all");
		if (!_vp.isModifiable())
			submenuAll.setEnabled(false);
		_vp.getPopupMenu().addColorOptions(submenuAll);
		_submenuSelection.add(submenuAll);
	}

	private void addMenuBasePair() {
			int indiceBP = _vp.getNearestBase();
			ArrayList<Integer> indices = _vp.getRNA().findPair(indiceBP);
			ModeleBase base = _vp.getRNA()
					.get_listeBases().get(_vp.getNearestBase());
			if (base.getElementStructure() != -1) {
				JMenu submenuBasePair = new JMenu();
				ModeleBase partner = _vp
						.getRNA().get_listeBases().get(
								base.getElementStructure());
				submenuBasePair
						.addChangeListener(new ControleurSelectionHighlight(
								indices, _vp, submenuBasePair));
				submenuBasePair.setText("Base pair #("
						+ (Math.min(base.getBaseNumber(), partner
								.getBaseNumber()))
						+ ","
						+ (Math.max(base.getBaseNumber(), partner
								.getBaseNumber())) + ")");
				submenuBasePair.setActionCommand("bp");
				// option disponible seulement en mode modifiable
				if (!_vp.isModifiable())
					submenuBasePair.setEnabled(false);

				JMenuItem basepair = new JMenuItem("Edit BP");
				basepair.setActionCommand("basepair");
				basepair.addActionListener(_vp.getPopupMenu()
						.get_controleurMenu());

				_vp.getPopupMenu().addColorOptions(submenuBasePair);
				Component[] comps = submenuBasePair.getMenuComponents();
				int offset = -1;
				for (int i = 0; i < comps.length; i++) {
					Component c = comps[i];
					if (c instanceof JMenuItem) {
						JMenuItem jmi = (JMenuItem) c;
						if (jmi.getActionCommand().contains(",BPColor")) {
							offset = i;
						}
					}
				}
				if (offset != -1) {
					submenuBasePair.insert(basepair, offset);
				} else {
					submenuBasePair.add(basepair);
				}
				_submenuSelection.add(submenuBasePair);
			}
		}

	private void updateNearestBase(MouseEvent arg0) {
		int i = _vp.getNearestBaseIndex(arg0.getX(),arg0.getY(),true,false);
		if (i!=-1)
			_vp.setNearestBase(i);
	}



	
	public void mouseMoved(MouseEvent arg0) {
		_selectedBase = _vp.getNearestBase(arg0.getX(),arg0.getY());
		TextAnnotation selectedAnnotation = _vp.getNearestAnnotation(arg0.getX(),arg0.getY());
		_vp.setHoverBase(_selectedBase);
		if (_selectedBase != null)
		{
		}
		else if (selectedAnnotation!=null)
		{
			_vp.set_selectedAnnotation(selectedAnnotation);
			_vp.highlightSelectedAnnotation();
			_vp.repaint();
		}
		_vp.setLastSelectedPosition(new Point2D.Double(arg0.getX(),arg0.getY()));
	}

	
	
	private void moveSelection(Point prev, Point cur) 
	{
		Point2D.Double p1 =  _vp.panelToLogicPoint(new Point2D.Double(prev.x,prev.y));
		Point2D.Double p2 =  _vp.panelToLogicPoint(new Point2D.Double(cur.x,cur.y));
		double dx = (p2.x - p1.x);
		double dy = (p2.y - p1.y);
		
		if (_vp.isModifiable())
		{  
			double ndx = dx;
			double ndy = dy;
			if (_vp.getRNA().get_drawMode() == RNA.DRAW_MODE_LINEAR) 
			{
				ndy=0.0;
			}
	 		_vp.getVARNAUI().UIShiftBaseCoord(_vp.getSelectionIndices(), ndx, ndy);
			_vp.fireLayoutChanged();
			
		}
	}


	public void popupMenuCanceled(PopupMenuEvent arg0) {
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
		_vp.resetAnnotationHighlight();
		_selectedBase = null;
	}

	public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
	}
}