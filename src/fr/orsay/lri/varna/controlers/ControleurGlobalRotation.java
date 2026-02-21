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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.VARNAEdits;
import fr.orsay.lri.varna.views.VueGlobalRotation;


public class ControleurGlobalRotation implements ChangeListener {

	private VueGlobalRotation _vGR;
	private double _oldAngle;
	private VARNAPanel _vp;

	public ControleurGlobalRotation(VueGlobalRotation vGR, VARNAPanel vp) {
		_vGR = vGR;
		_oldAngle = 0;
		_vp = vp;
	}

	public void stateChanged(ChangeEvent e) {
		_vp.getVARNAUI().UIGlobalRotation(_vGR.getAngle() - _oldAngle);
		_oldAngle = _vGR.getAngle();
	}
}
