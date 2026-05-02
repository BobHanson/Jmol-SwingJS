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

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBasesComparison;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * Controller of the mouse click
 * 
 * @author darty
 * 
 */
public class ControleurClicMovement
    implements MouseListener, MouseMotionListener, PopupMenuListener {
  private VARNAPanel _vp;
  private boolean _presenceMenuSelection;
  private JMenu _submenuSelection;
  public Point _spawnPoint;
  public Point _initialPoint;
  public Point _prevPoint;
  public Point _currentPoint;

  public static final double MIN_SELECTION_DISTANCE = 40.0;
  public static final double HYSTERESIS_DISTANCE = 10.0;

  public enum MouseStates {
    NONE, MOVE_ELEMENT, MOVE_OR_SELECT_ELEMENT, SELECT_ELEMENT, SELECT_REGION_OR_UNSELECT, SELECT_REGION, CREATE_BP, POPUP_MENU, MOVE_ANNOTATION,
  }

  private MouseStates _currentState = MouseStates.NONE;

  public ControleurClicMovement(VARNAPanel _vuep) {
    _vp = _vuep;
    _vp.getPopup().addPopupMenuListener(this);
    _presenceMenuSelection = false;
  }

  @Override
  public void mouseClicked(MouseEvent arg0) {
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
  }

  @Override
  public void mouseMoved(MouseEvent me) {
    int x = me.getX();
    int y = me.getY();
    _vp.doMouseMove(x, y);
  }

  @Override
  public void mousePressed(MouseEvent me) {
    boolean button1 = (me.getButton() == MouseEvent.BUTTON1);
    boolean button3 = (me.getButton() == MouseEvent.BUTTON3);
    boolean shift = me.isShiftDown();
    boolean ctrl = me.isControlDown();
    boolean alt = me.isAltDown();
    int x = me.getX();
    int y = me.getY();
    int id = me.getID();
    _vp.fireMouseEvent(id, true);
    if (button1 && !ctrl && !alt && !shift) {
      if (_vp.isModifiable()) {
        doMoveElement(x, y, id);
      }
    } else if (button1 && ctrl && !alt && !shift) {
      doCreateBP(x, y, id);
    } else if (button1 && shift && !ctrl && !alt) {
      doSelectElement(x, y, id);
    } else if (button3) {
      doPopupMenu(x, me.getY());
    }
    _vp.fireMouseEvent(id, false);
  }

  private void doPopupMenu(int x, int y) {
    _currentState = MouseStates.POPUP_MENU;
    if (_presenceMenuSelection) {
      _vp.getPopupMenu().removeSelectionMenu();
    }
    if (_vp.setNearestBase(x, y))
      addMenu(x,y);
    _vp.showPopup(x, y);
  }


  @Override
  public void mouseDragged(MouseEvent me) {
    int x = me.getX();
    int y = me.getY();
    int id = me.getID();
    _vp.fireMouseEvent(id, true);
    switch (_currentState) {
    case MOVE_OR_SELECT_ELEMENT:
    case MOVE_ELEMENT:
      doMoveElement(x, y, id);
      break;
    case MOVE_ANNOTATION:
      _vp.dragAnnotation(x, y);
      break;
    case SELECT_ELEMENT:
    case SELECT_REGION_OR_UNSELECT:
      doSelectElement(x, y, id);
      break;
    case SELECT_REGION:
      doSelectRegion(x, y, id);
      break;
    case CREATE_BP:
      doCreateBP(x, y, id);
      break;
    default:
      break;
    }
  }

  @Override
  public void mouseReleased(MouseEvent me) {
    if (me.getButton() == MouseEvent.BUTTON1) {
      int x = me.getX();
      int y = me.getY();
      int id = me.getID();
      _vp.fireMouseEvent(id, true);
      switch (_currentState) {
      case MOVE_ELEMENT:
        doMoveElement(x, y, id);
        break;
      case SELECT_REGION_OR_UNSELECT:
        doSelectOrUnselect(id);
        break;
      case SELECT_ELEMENT:
        doSelectElement(x, y, id);
        break;
      case SELECT_REGION:
        doSelectRegion(x, y, id);
        break;
      case CREATE_BP:
        doCreateBP(x, y, id);
        break;
      default:
        _vp.clearSelection();
        break;
      }
      _vp.fireMouseEvent(id, false);
    }
    _currentState = MouseStates.NONE;
    _vp.repaint();
  }

  private void doSelectRegion(int x, int y, int action) {
    switch(action) {
    case MouseEvent.MOUSE_PRESSED:
      _vp.clearSelection();
      _currentState = MouseStates.SELECT_REGION_OR_UNSELECT;
      _initialPoint = new Point(x, y);
      _prevPoint = new Point(_initialPoint);
      _currentPoint = new Point(_initialPoint);
      break;
    case MouseEvent.MOUSE_DRAGGED:
      _currentPoint = new Point(x, y);
      int minx = Math.min(_currentPoint.x, _initialPoint.x);
      int miny = Math.min(_currentPoint.y, _initialPoint.y);
      int maxx = Math.max(_currentPoint.x, _initialPoint.x);
      int maxy = Math.max(_currentPoint.y, _initialPoint.y);
      _vp.setSelectionRectangle(
          new Rectangle(minx, miny, maxx - minx, maxy - miny));
      break;
    case MouseEvent.MOUSE_RELEASED:
      _vp.selectionComplete();      
      break;
    }
  }

  private void doCreateBP(int x, int y, int action) {
    ModeleBase base;
    switch (action) {
    case MouseEvent.MOUSE_PRESSED:
      base = _vp.getNearestBase(x, y, false, false);
      if (base != null) {
        _initialPoint = new Point(x, y);
        _currentPoint = new Point(_initialPoint);
        _currentState = MouseStates.CREATE_BP;
        _vp.setSelectedBase(base);
      }
      break;
    case MouseEvent.MOUSE_DRAGGED:
      if (_initialPoint.distance(x, y) > HYSTERESIS_DISTANCE) {
        base = _vp.getNearestBase(x, y, false, false);
        _vp.dragBase(base, x, y);
      }
      break;
    case MouseEvent.MOUSE_RELEASED:
      int selectedIndex = -1;
      if (_initialPoint.distance(x, y) > HYSTERESIS_DISTANCE) {
        selectedIndex = _vp.getNearestBaseIndex(x, y, false, false);
      }
      _vp.addBP(selectedIndex >= 0 ? _vp.getNearestBase(x, y, false, false) : null);
      break;
    }
  }
  
  private ModeleBase movingBase;

  private void doMoveElement(int x, int y, int action) {
    switch (action) {
    case MouseEvent.MOUSE_PRESSED:
      movingBase = null;
      _currentState = MouseStates.MOVE_OR_SELECT_ELEMENT;
      if (_vp.getRealCoords() != null && _vp.getRealCoords().length != 0
          && _vp.getRNA().get_listeBases().size() != 0) {
        ModeleBase base = _vp.getNearestBase(x, y, false, false);
        _initialPoint = new Point(x, y);
        _currentPoint = new Point(_initialPoint);
        _prevPoint = new Point(_initialPoint);
        if (base != null) {
          _vp.moveSelectedBase(base);
        } else {
          TextAnnotation t =_vp.setNearestAnnotation(x, y);
          if (t != null) {
            _currentState = MouseStates.MOVE_ANNOTATION;
            _vp.startMoveAnnotation();
          } else {
            doSelectRegion(x, y, action);
          }
        }
      }
      break;
    case MouseEvent.MOUSE_DRAGGED:
      _vp.lockScrolling();
      _currentState = MouseStates.MOVE_ELEMENT;
      // si on deplace la souris et qu'une base est selectionnée
      if (_vp._selectedBase == null) {
        return;
      }
      if (_vp.getRNA().get_drawMode() == RNA.DRAW_MODE_RADIATE) {
        movingBase = _vp.moveElement(movingBase, x, y);
      } else {
        // dans le cas circulaire naview ou line on deplace une base
        _currentPoint = new Point(x, y);
        _vp.moveSelection(_prevPoint, _currentPoint);
        _prevPoint = new Point(_currentPoint);
      }
      _vp.repaint();
      break;
    case MouseEvent.MOUSE_RELEASED:
      _vp.endDragging();
      break;
    }
  }

  private void doSelectOrUnselect(int action) {
    switch(action) {
    case MouseEvent.MOUSE_DRAGGED:
      break;
    case MouseEvent.MOUSE_RELEASED:
      _vp.endSelectOrUnselect();
      break;
    }
  }

  private void doSelectElement(int x, int y, int action) {
    switch(action) {
    case MouseEvent.MOUSE_PRESSED:
      _currentState = MouseStates.SELECT_ELEMENT;
      _initialPoint = new Point(x, y);
      _currentPoint = new Point(_initialPoint);
      break;
    case MouseEvent.MOUSE_DRAGGED:
      if (_initialPoint.distance(x, y)>HYSTERESIS_DISTANCE)
        _currentState = MouseStates.SELECT_REGION;
      break;
    case MouseEvent.MOUSE_RELEASED:
      _vp.endSelect(x, y);
      break;
    }
  }

  private void addMenu(int x, int y) {
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
    detectAnnotation(x, y);

    _vp.getPopup().addSelectionMenu(_submenuSelection);
    _presenceMenuSelection = true;
  }

  private void detectAnnotation(int x, int y) {
    if (_vp.getListeAnnotations().size() != 0) {
      double dist = Double.MAX_VALUE;
      double d2;
      Point2D.Double position;
      for (TextAnnotation textAnnot : _vp.getListeAnnotations()) {
        // calcul de la distance
        position = textAnnot.getCenterPosition();
        position = _vp.transformCoord(position);
        d2 = Math.sqrt(Math.pow((position.x - x), 2)
            + Math.pow((position.y - y), 2));
        // si la valeur est inferieur au minimum actuel
        if (dist > d2) {
          _vp.setSelectedAnnotation(textAnnot);
          dist = d2;
        }
      }
      _submenuSelection.addSeparator();
      _vp.getPopup().addAnnotationMenu(_submenuSelection, true);
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
      submenuLoop.addChangeListener(
          new ControleurSelectionHighlight(listLoop, _vp, submenuLoop));
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
        submenuLoop1.addChangeListener(
            new ControleurSelectionHighlight(listLoop1, _vp, submenuLoop1));
        submenuLoop1.setActionCommand("loop1");
        if (!_vp.isModifiable())
          submenuLoop1.setEnabled(false);
        _vp.getPopupMenu().addColorOptions(submenuLoop1);
        submenuLoop1.addSeparator();
        _vp.getPopupMenu().addAnnotationMenu(submenuLoop1);
        _submenuSelection.add(submenuLoop1);
      }
      ArrayList<Integer> listLoop2 = _vp.getRNA().findLoopBackward(indexL);
      if (listLoop2.size() > 0) {
        JMenu submenuLoop2 = new JMenu("Backward loop");
        submenuLoop2.addChangeListener(
            new ControleurSelectionHighlight(listLoop2, _vp, submenuLoop2));
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
    Collection<? extends ModeleBase> mbs = _vp.getSelection().getBaseList();
    if (mbs.size() > 0) {
      JMenu submenuAll = new JMenu("Current");
      submenuAll.addChangeListener(
          new ControleurSelectionHighlight(mbs, _vp, submenuAll));
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
      submenuBase.setText("Base #" + (mb.getResidueNumber()) + ":"
          + ((ModeleBasesComparison) mb).getBases());
    } else {
      submenuBase.setText("Base #" + (mb.getResidueNumber()) + ":"
          + ((ModeleBaseNucleotide) mb).getBase());
    }
    submenuBase.addChangeListener(
        new ControleurSelectionHighlight(mb.getIndex(), _vp, submenuBase));
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
    ModeleBase base = _vp.getRNA().get_listeBases().get(_vp.getNearestBase());
    if (base.getElementStructure() != -1) {
      JMenu submenuBasePair = new JMenu();
      ModeleBase partner = _vp.getRNA().get_listeBases()
          .get(base.getElementStructure());
      submenuBasePair.addChangeListener(
          new ControleurSelectionHighlight(indices, _vp, submenuBasePair));
      submenuBasePair.setText("Base pair #("
          + (Math.min(base.getResidueNumber(), partner.getResidueNumber())) + ","
          + (Math.max(base.getResidueNumber(), partner.getResidueNumber())) + ")");
      submenuBasePair.setActionCommand("bp");
      // option disponible seulement en mode modifiable
      if (!_vp.isModifiable())
        submenuBasePair.setEnabled(false);

      JMenuItem basepair = new JMenuItem("Edit BP");
      basepair.setActionCommand("basepair");
      basepair.addActionListener(_vp.getPopupMenu().get_controleurMenu());

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

  @Override
  public void popupMenuCanceled(PopupMenuEvent arg0) {
  }

  @Override
  public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
    _vp.endPopup();
  }

  @Override
  public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
  }
}
