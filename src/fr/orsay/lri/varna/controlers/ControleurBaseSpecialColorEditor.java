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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import fr.orsay.lri.varna.components.BaseSpecialColorEditor;
import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.rna.ModeleBase;

/**
 * BH SwingJS - additions here to allow asynchronous JColorChooser  
 */
public class ControleurBaseSpecialColorEditor implements ActionListener, ComponentListener {  // BH SwingJS

	private BaseSpecialColorEditor _specialColorEditor;

	private int _selectedRow;
	private int _selectedCol;
	private Color _selectedColor;
	private String _selectedColTitle;

	public ControleurBaseSpecialColorEditor(BaseSpecialColorEditor specialColorEditor) {
		_specialColorEditor = specialColorEditor;
	}

	/**
	 * Handles events from the editor button and from the dialog's OK button.
	 */
	public void actionPerformed(ActionEvent e) {
		if (BaseSpecialColorEditor.getEDIT().equals(e.getActionCommand())) {
			// The user has clicked the cell, so
			// bring up the dialog.
			_specialColorEditor.getButton().setBackground(
					_specialColorEditor.getCurrentColor());
			_specialColorEditor.getColorChooser().setColor(
					_specialColorEditor.getCurrentColor());
			
			// BH SwingJS in JavaScript, this is not modal. 
			// We have to set a callback to stop the editing.
			
			_specialColorEditor.getDialog().removeComponentListener(this);
			_specialColorEditor.getDialog().addComponentListener(this);
			_specialColorEditor.getDialog().setVisible(true);
			
			
			// Make the renderer reappear.
			// BH SwingJS not so fast...
			//_specialColorEditor.callFireEditingStopped();

		} else { // User pressed dialog's "OK" button.
			_specialColorEditor.setCurrentColor(_specialColorEditor
					.getColorChooser().getColor());

			_selectedRow = _specialColorEditor.get_vueBases().getTable()
					.getSelectedRow();

			_selectedCol = _specialColorEditor.get_vueBases().getTable()
					.getSelectedColumn();

			_selectedColor = _specialColorEditor.getCurrentColor();

			_selectedColTitle = _specialColorEditor.get_vueBases()
					.getSpecialTableModel().getColumnName(_selectedCol);
			BaseList lb = _specialColorEditor.get_vueBases().getDataAt(_selectedRow);
			for(ModeleBase mb: lb.getBases())
			{
				applyColor(_selectedColTitle, _selectedColor,mb);
			}
			_specialColorEditor.get_vueBases().get_vp().repaint();
		}
	}


	private void applyColor(String titreCol, Color couleur, ModeleBase mb) {
		if (titreCol.equals("Inner Color")) {
			mb.getStyleBase()
					.setBaseInnerColor(couleur);
		} else if (titreCol.equals("Outline Color")) {
			mb.getStyleBase()
					.setBaseOutlineColor(couleur);
		} else if (titreCol.equals("Name Color")) {
			mb.getStyleBase()
					.setBaseNameColor(couleur);
		} else if (titreCol.equals("Number Color")) {
			mb.getStyleBase()
					.setBaseNumberColor(couleur);
		}

	}


	@Override
	public void componentResized(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		
		_specialColorEditor.callFireEditingStopped(); // BH SwingJS -- need to catch this
		
	}

}
