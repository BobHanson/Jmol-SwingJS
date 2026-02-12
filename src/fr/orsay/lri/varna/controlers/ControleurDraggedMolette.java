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
package fr.orsay.lri.varna.controlers;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import fr.orsay.lri.varna.VARNAPanel;


/**
 * Controller of the mouse for scroll wheel click and dragged events
 * 
 * @author darty
 * 
 */
public class ControleurDraggedMolette implements MouseListener,
		MouseMotionListener {
	private VARNAPanel _vp;
	/**
	 * <code>true</code> if the right button is pressed<br>
	 * <code>false</code> if not
	 */
	private static Boolean _rightButtonClick;
	/**
	 * The vector which contains the direction of the mouse movement
	 */
	private static Point _direction;
	/**
	 * The position of the cursor before the mouse drag
	 */
	private static Point _avant;
	/**
	 * The position of the cursor after the mouse drag
	 */
	private static Point _apres;

	public ControleurDraggedMolette(VARNAPanel vp) {
		_vp = vp;
		_rightButtonClick = false;
		_avant = _apres = _direction = new Point();
	}

	public void mouseDragged(MouseEvent e) {
		// si le bon boutton a été pressé
		if (_rightButtonClick) {
			_apres = e.getPoint();
			_direction = new Point(_apres.x - _avant.x, _apres.y - _avant.y);
			_vp.setTranslation(new Point(_vp.getTranslation().x + _direction.x,
					_vp.getTranslation().y + _direction.y));
			_avant = _apres;
			_vp.checkTranslation();
			_vp.repaint();
		}
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		// lors du clic, la position du curseur est enregistrée
		_avant = e.getPoint();
		// si le boutton molette est pressé ou si le boutton gauche et shift
		// sont pressés
		if (e.getButton() == MouseEvent.BUTTON2)
		{
			_rightButtonClick = true;
		}
		else
		{
			_rightButtonClick = false;
		}
			
			
	}

	public void mouseReleased(MouseEvent e) {
		// si le boutton molette est relaché ou si le boutton gauche et shift
		// sont relachés
		if (e.getButton() == MouseEvent.BUTTON2)
			_rightButtonClick = false;
	}
}
