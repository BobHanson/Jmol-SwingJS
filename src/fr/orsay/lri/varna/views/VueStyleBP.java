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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.VARNAConfig;


public class VueStyleBP implements ActionListener {

	private VARNAPanel _vp;
	private JComboBox _cmb;
	private JPanel panel;

	public VueStyleBP(VARNAPanel vp) {
		_vp = vp;
		VARNAConfig.BP_STYLE[] styles = VARNAConfig.BP_STYLE.values();
		VARNAConfig.BP_STYLE bck = vp.getConfig()._mainBPStyle;
		_cmb = new JComboBox(styles);
		for (int i = 0; i < styles.length; i++) {
			if (styles[i] == bck)
				_cmb.setSelectedIndex(i);
		}
		_cmb.addActionListener(this);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel labelZ = new JLabel("Base pair style: ");

		panel.add(labelZ);
		panel.add(_cmb);
	}

	public JPanel getPanel() {
		return panel;
	}

	public VARNAConfig.BP_STYLE getStyle() {
		return (VARNAConfig.BP_STYLE) _cmb.getSelectedItem();
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public void actionPerformed(ActionEvent e) {
		VARNAConfig.BP_STYLE newSel = (VARNAConfig.BP_STYLE) _cmb
				.getSelectedItem();
		_vp.setBPStyle(newSel);
		_vp.repaint();
	}
}
