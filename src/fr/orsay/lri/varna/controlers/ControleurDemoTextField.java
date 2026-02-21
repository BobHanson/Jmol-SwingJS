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
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import fr.orsay.lri.varna.applications.VARNAOnlineDemo;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;

/**
 * It controls the sequence and structure text fields and changes their color if
 * they have different lengths or unbalanced parentheses.
 * 
 * @author darty
 * 
 */
public class ControleurDemoTextField implements CaretListener {
	private VARNAOnlineDemo _vod;
	private String _oldSeq, _oldStruct;
	private Highlighter _hilit;
	private Highlighter.HighlightPainter _painter;
	private final Color COLORERROR = Color.RED;
	private final Color COLORWARNING = Color.ORANGE;

	public ControleurDemoTextField(VARNAOnlineDemo VOD) {
		_vod = VOD;
		_oldSeq = _vod.get_seq().getText();
		_oldStruct = _vod.get_struct().getText();
		_hilit = new DefaultHighlighter();
		_painter = new DefaultHighlighter.DefaultHighlightPainter(Color.BLACK);
		_vod.get_struct().setHighlighter(_hilit);
	}

	// if there is any change
	public void caretUpdate(CaretEvent e) {
		if (_oldStruct != _vod.get_struct().getText()
				|| _oldSeq != _vod.get_seq().getText()) {
			ArrayList<String> infos = new ArrayList<String>();
			_vod.get_info().removeAll();
			_hilit.removeAllHighlights();
			_oldStruct = _vod.get_struct().getText();
			_oldSeq = _vod.get_seq().getText();
			int nbPO = 0, nbPF = 0;
			// compte les parentheses ouvrantes et fermantes
			Stack<Integer> p = new Stack<Integer>();
			boolean pb = false;
			for (int i = 0; i < _vod.get_struct().getText().length(); i++) {
				if (_vod.get_struct().getText().charAt(i) == '(') {
					nbPO++;
					p.push(i);
				} else if (_vod.get_struct().getText().charAt(i) == ')') {
					nbPF++;
					if (p.size() == 0) {
						try {
							_hilit.addHighlight(i, i + 1, _painter);
						} catch (BadLocationException e1) {
							_vod.get_varnaPanel().errorDialog(e1);
						}
						pb = true;
					} else
						p.pop();
				}
			}

			// si le nombre de parentheses ouvrantes/fermantes est different
			if (pb || p.size() > 0) {
				// colorie en rouge
				if (pb) {
					infos.add("too many closing parentheses");
				}
				if (p.size() > 0) {
					int indice;
					while (!p.isEmpty()) {
						indice = p.pop();
						try {
							_hilit.addHighlight(indice, indice + 1, _painter);
						} catch (BadLocationException e1) {
							_vod.get_varnaPanel().errorDialog(e1);
						}
					}
					infos.add("too many opening parentheses");
				}
				_vod.get_info().setForeground(COLORERROR);
				_vod.get_seq().setForeground(COLORERROR);
				_vod.get_struct().setForeground(COLORERROR);
			} else {
				try {
					// redraw the new RNA
					_vod.get_varnaPanel().drawRNA(_vod.get_seq().getText(),
							_vod.get_struct().getText(),
							_vod.get_varnaPanel().getRNA().get_drawMode());
				} catch (ExceptionNonEqualLength e1) {
					_vod.get_varnaPanel().errorDialog(e1);
				}
				// verifie la longueur de la structure et de la sequence
				if (_vod.get_seq().getText().length() != _vod.get_struct()
						.getText().length()) {
					// colorie en orange
					infos.add("different lenghts");
					_vod.get_seq().setForeground(COLORWARNING);
					_vod.get_struct().setForeground(COLORWARNING);
				} else {
					// sinon colorie en noir
					_vod.get_seq().setForeground(Color.black);
					_vod.get_struct().setForeground(Color.black);
				}
			}
			_vod.get_varnaPanel().getVARNAUI().UIReset();
			String info = new String();
			if (infos.size() != 0) {
				info += infos.get(0);
				for (int i = 1; i < infos.size(); i++) {
					info += ", " + infos.get(i);
				}
				info += ".";
			}
			_vod.get_info().setText(info);
		}
	}
}