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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.views.VueAnnotation;
import fr.orsay.lri.varna.views.VueUI;

/**
 * Annotation View Controller
 * 
 * @author Darty@lri.fr
 * 
 */
public class ControleurVueAnnotation implements CaretListener, ChangeListener,
		ActionListener {

	protected VueAnnotation _vueAnnot;

	/**
	 * Creates a ControleurVueAnnotation
	 * 
	 * @param vueAnnot
	 */
	public ControleurVueAnnotation(VueAnnotation vueAnnot) {
		_vueAnnot = vueAnnot;
	}

	public void caretUpdate(CaretEvent arg0) {
		_vueAnnot.update();
	}

	public void stateChanged(ChangeEvent arg0) {
		_vueAnnot.update();
	}

	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("setcolor")) {
			final VueUI vui = _vueAnnot.get_vp().getVARNAUI(); // BH SwingJS
			vui.showColorDialog("Pick a color", _vueAnnot.getTextAnnotation().getColor(), new Runnable() {

				@Override
				public void run() {
					Color c = (Color) vui.dialogReturnValue;
					if (c != null)
						_vueAnnot.updateColor(c);
					_vueAnnot.update();
				}
				
			});
			
//was:			Color c = JColorChooser.showDialog(_vueAnnot.get_vp(),
//					"Pick a color", _vueAnnot.getTextAnnotation().getColor());
//			if (c != null) {
//				_vueAnnot.updateColor(c);
//			}

		}
		_vueAnnot.update();
	}
}
