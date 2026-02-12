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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JTable;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.views.VueAnnotation;
import fr.orsay.lri.varna.views.VueChemProbAnnotation;
import fr.orsay.lri.varna.views.VueHighlightRegionEdit;


/**
 * Mouse action and motion listener for AnnotationTableModel
 * 
 * @author Darty@lri.fr
 * 
 */
public class ControleurTableAnnotations implements MouseListener,
		MouseMotionListener {
	public static final int REMOVE = 0, EDIT = 1;
	private JTable _table;
	private VARNAPanel _vp;
	private int _type;

	/**
	 * 
	 * @param table
	 * @param vp
	 * @param type
	 *            : REMOVE = 0, EDIT = 1
	 */
	public ControleurTableAnnotations(JTable table, VARNAPanel vp, int type) {
		_table = table;
		_vp = vp;
		_type = type;
	}

	public void mouseClicked(MouseEvent arg0) {
		switch (_type) {
		case EDIT:
			edit();
			break;
		case REMOVE:
			remove();
			break;
		default:
			break;
		}
	}

	/**
	 * if remove case
	 */
	private void remove() {
		_vp.set_selectedAnnotation(null);
		Object o = _table.getValueAt(_table.getSelectedRow(), 0);
		if (o instanceof TextAnnotation) {
			if (!_vp.removeAnnotation((TextAnnotation) o))
				_vp.errorDialog(new Exception("Impossible de supprimer"));
			_table.setValueAt("Deleted!", _table.getSelectedRow(), 0);
		}
		else if (o instanceof ChemProbAnnotation) {
			_vp.getRNA().removeChemProbAnnotation((ChemProbAnnotation) o);
			_table.setValueAt("Deleted!", _table.getSelectedRow(), 0);
		}
		else if (o instanceof HighlightRegionAnnotation) {
			_vp.getRNA().removeHighlightRegion((HighlightRegionAnnotation) o);
			_table.setValueAt("Deleted!", _table.getSelectedRow(), 0);
		}
		_vp.repaint();
	}

	/**
	 * if edit case
	 */
	private void edit() {
		Object o = _table.getValueAt(_table
				.getSelectedRow(), 0);
		if (o instanceof TextAnnotation)
		{
		TextAnnotation textAnnot = (TextAnnotation) o; 
		VueAnnotation vueAnnotation;
		vueAnnotation = new VueAnnotation(_vp, textAnnot, false);
		vueAnnotation.show();
		}else if (o instanceof HighlightRegionAnnotation)
		{
			HighlightRegionAnnotation annot = (HighlightRegionAnnotation) o; 
			HighlightRegionAnnotation an = annot.clone(); 
			VueHighlightRegionEdit vueAnnotation = new VueHighlightRegionEdit(_vp,annot);
			if (!vueAnnotation.show())
			{
				annot.setBases(an.getBases());
				annot.setFillColor(an.getFillColor());
				annot.setOutlineColor(an.getOutlineColor());
				annot.setRadius(an.getRadius());
			}
		}else if (o instanceof ChemProbAnnotation)
		{
			ChemProbAnnotation annot = (ChemProbAnnotation) o; 
			ChemProbAnnotation an = annot.clone(); 
			VueChemProbAnnotation vueAnnotation = new VueChemProbAnnotation(_vp,annot);
			if (!vueAnnotation.show())
			{
				annot.setColor(an.getColor());
				annot.setIntensity(an.getIntensity());
				annot.setType(an.getType());
				annot.setOut(an.isOut());
			}
		}

	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
		_vp.set_selectedAnnotation(null);
		_vp.repaint();
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void mouseDragged(MouseEvent arg0) {
	}

	/**
	 * update selected annotation
	 */
	public void mouseMoved(MouseEvent arg0) {
		if (_table.rowAtPoint(arg0.getPoint()) < 0)
			return;
		Object o = _table.getValueAt(_table.rowAtPoint(arg0.getPoint()), 0);
		if (o.getClass().equals(TextAnnotation.class)
				&& o != _vp.get_selectedAnnotation()) {
			_vp.set_selectedAnnotation((TextAnnotation) o);
			_vp.repaint();
		}
	}

}
