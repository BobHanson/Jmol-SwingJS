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
package fr.orsay.lri.varna.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.BaseSpecialColorEditor;
import fr.orsay.lri.varna.components.BaseTableModel;
import fr.orsay.lri.varna.components.ColorRenderer;
import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;

public class VueBases extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * for bases by kind
	 */
	public final static int KIND_MODE = 1;
	/**
	 * for all bases 
	 */
	public final static int ALL_MODE = 2;
	/**
	 * for base pairs by king
	 */
	public final static int COUPLE_MODE = 3;

	private int _mode;

	private VARNAPanel _vp;

	private ArrayList<BaseList> data = new ArrayList<BaseList>();
	
	private Hashtable<String,BaseList> revdata = new Hashtable<String,BaseList>();
	
	private JTable table;

	private BaseTableModel specialTableModel;

	public VueBases(VARNAPanel vp, int mode) {
		super(new GridLayout(1, 0));
		_vp = vp;
		switch (mode) {
		case (KIND_MODE):
			_mode = KIND_MODE;
			kindMode();
			break;
		case (ALL_MODE):
			_mode = ALL_MODE;
			allMode();
			break;
		case (COUPLE_MODE):
			_mode = COUPLE_MODE;
			coupleMode();
			break;
		default:
			break;
		}
	}
	
	private BaseList locateOrAddList(String caption)
	{
	  if (!revdata.containsKey(caption))
	  {
		  BaseList mbl = new BaseList(caption);
		  revdata.put(caption,mbl);
		  data.add(mbl);
	  }
	  return revdata.get(caption);
	}

	private void coupleMode() {
		String pairString;
		for (int i = 0; i < _vp.getRNA().get_listeBases().size(); i++) {
			
			int j = _vp.getRNA().get_listeBases().get(i).getElementStructure();
			if (j > i) {
				String tmp1 = (_vp.getRNA().get_listeBases().get(i).getContent()); 
				String tmp2 = (_vp.getRNA().get_listeBases().get(j).getContent()); 
				pairString = tmp1 +"-"+ tmp2;
				BaseList bl = locateOrAddList(pairString);
				bl.addBase(_vp.getRNA().get_listeBases().get(i));
				bl.addBase(_vp.getRNA().get_listeBases().get(j));
			}
		}
		createView();
	}

	private void allMode() {
		for (int i = 0; i < _vp.getRNA().get_listeBases().size(); i++) {
			ModeleBase mb = _vp.getRNA().get_listeBases().get(i);
			BaseList bl = locateOrAddList(""+i);
			bl.addBase(mb);
		}
		createView();
	}

	private void kindMode() {
		for (int i = 0; i < _vp.getRNA().get_listeBases().size(); i++) {
			ModeleBase mb = _vp.getRNA().get_listeBases().get(i);
			String tmp1 = (mb.getContent()); 
			BaseList bl = locateOrAddList(tmp1);
			bl.addBase(mb);
		}
		createView();
	}

	private void createView() {
		specialTableModel = new BaseTableModel(data);
		table = new JTable(specialTableModel);
		table.setPreferredScrollableViewportSize(new Dimension(500, 300));
		// TODO: Find equivalent in JRE 1.5
		//table.setFillsViewportHeight(true);
		// Create the scroll pane and add the table to it.
		JScrollPane scrollPane = new JScrollPane(table);

		// Set up renderer and editor for the Favorite Color column.
		table.setDefaultRenderer(Color.class, new ColorRenderer(true));
		table.setDefaultEditor(Color.class, new BaseSpecialColorEditor(this));
		specialTableModel.addTableModelListener(new TableModelListener(){

			public void tableChanged(TableModelEvent e) {
				_vp.repaint();
			}
			
		});

		// Add the scroll pane to this panel.
		add(scrollPane);

		UIvueBases();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	public void UIvueBases() {
		// Create and set up the content pane.
		JComponent newContentPane = this;
		newContentPane.setOpaque(true); // content panes must be opaque

		JOptionPane.showMessageDialog(_vp, newContentPane,
				"Base Colors Edition", JOptionPane.PLAIN_MESSAGE);

	}

	public int getMode() {
		return _mode;
	}

	public BaseList getDataAt(int i) {
		return data.get(i);
	}

	
	public ArrayList<BaseList> getData() {
		return data;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public JTable getTable() {
		return table;
	}

	public void setTable(JTable table) {
		this.table = table;
	}

	public BaseTableModel getSpecialTableModel() {
		return specialTableModel;
	}

	public void setSpecialTableModel(BaseTableModel specialTableModel) {
		this.specialTableModel = specialTableModel;
	}
	
}
