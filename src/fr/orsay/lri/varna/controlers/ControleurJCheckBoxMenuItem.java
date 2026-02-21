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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import fr.orsay.lri.varna.VARNAPanel;


/**
 * Controller of check box menu items to repaint if item state changes
 * 
 * @author darty
 * 
 */
public class ControleurJCheckBoxMenuItem implements ItemListener {

	private VARNAPanel _vp;

	public ControleurJCheckBoxMenuItem(VARNAPanel v) {
		_vp = v;
	}

	public void itemStateChanged(ItemEvent e) {
		_vp.repaint();
	}
}
