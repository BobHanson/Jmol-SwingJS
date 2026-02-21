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

import java.awt.Dimension;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.views.VueBorder;


public class ControleurBorder implements ChangeListener {

	private VueBorder _vb;

	public ControleurBorder(VueBorder vb) {
		_vb = vb;
	}

	public void stateChanged(ChangeEvent e) {
		if (_vb.getDimension().getHeight() < _vb.get_vp().getHeight()
				&& _vb.getDimension().getWidth() < _vb.get_vp().getWidth()) {
			_vb.get_vp().setBorderSize(_vb.getDimension());
			_vb.get_vp().setMinimumSize(
					new Dimension(_vb.get_vp().getBorderSize().width * 2, _vb
							.get_vp().getBorderSize().height * 2));
			_vb.get_vp().repaint();
		}
	}
}
