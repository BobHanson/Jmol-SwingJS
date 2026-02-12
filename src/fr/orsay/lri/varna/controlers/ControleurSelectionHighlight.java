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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.rna.ModeleBase;


public class ControleurSelectionHighlight implements ChangeListener {
	private Collection<? extends ModeleBase> _selection;
	private VARNAPanel _target;
	private JMenuItem _parent;

	public ControleurSelectionHighlight(int elem, VARNAPanel v, JMenuItem parent) {
		ArrayList<Integer> sel = new ArrayList<Integer>();
		sel.add(elem);
		_selection = v.getRNA().getBasesAt(sel);
		_target = v;
		_parent = parent;
	}

	public ControleurSelectionHighlight(Vector<Integer> sel, VARNAPanel v,
			JMenuItem parent) {
		this(new ArrayList<Integer>(sel), v, parent);
	}

	public ControleurSelectionHighlight(ArrayList<Integer> sel, VARNAPanel v,
			JMenuItem parent) {
		this(v.getRNA().getBasesAt(sel),v,parent);
	}
	
	public ControleurSelectionHighlight(Collection<? extends ModeleBase> sel, VARNAPanel v,
			JMenuItem parent) {
		_selection = sel;
		_target = v;
		_parent = parent;
	}

	public void stateChanged(ChangeEvent e) {
		if (_parent.isSelected()) {
			_target.saveSelection();
			_target.setSelection(_selection);
		} else {
			_target.restoreSelection();
		}

	}

}
