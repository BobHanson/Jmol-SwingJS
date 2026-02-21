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
package fr.orsay.lri.varna.components;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;


public class BaseTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String[] columnNames = { "Numbers", "Base", "Outline Color", "Inner Color",
			"Name Color", "Number Color" };
	private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
	private ArrayList<BaseList> _bases;
	private boolean _singleBases = true;

	public BaseTableModel(ArrayList<BaseList> bases) {
		_bases = bases;
		ArrayList<Object> ligne;
		for (int i = 0; i < bases.size(); i++) {
			ligne = new ArrayList<Object>();
			BaseList bl  = bases.get(i);
			if (bl.size()!=1)
			{
				_singleBases = false;
			}
			ligne.add(bl.getNumbers());
			ligne.add(bl.getContents());
			ligne.add(bl.getAverageOutlineColor());
			ligne.add(bl.getAverageInnerColor());
			ligne.add(bl.getAverageNameColor());
			ligne.add(bl.getAverageNumberColor());
			this.data.add(ligne);	
		}

	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.size();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		return data.get(row).get(col);
	}

	/*
	 * JTable uses this method to determine the default renderer/ editor for
	 * each cell. If we didn't implement this method, then the last column would
	 * contain text ("true"/"false"), rather than a check box.
	 */
	@SuppressWarnings("unchecked")
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	public boolean isCellEditable(int row, int col) {
		// Note that the data/cell address is constant,
		// no matter where the cell appears onscreen.
		if (col < 1) {
			return false;
		} else {
			return true;
		}
	}

	public void setValueAt(Object value, int row, int col) {
		data.get(row).set(col, value);
		if (col == 1 && _singleBases)
		{
			ModeleBase mb = _bases.get(row).getBases().get(0);
			mb.setContent(value.toString());
		}
		fireTableCellUpdated(row, col);

	}
}