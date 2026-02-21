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

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;


import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurGlobalRescale;
import fr.orsay.lri.varna.controlers.ControleurGlobalRotation;
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;

public class VueGlobalRescale {

	private VARNAPanel _vp;
	private JSlider rescaleSlider;
	private JPanel panel;

	public VueGlobalRescale(VARNAPanel vp) {
		_vp = vp;
		rescaleSlider = new JSlider(JSlider.HORIZONTAL, 1, 500, 100);
		rescaleSlider.setMajorTickSpacing(100);
		rescaleSlider.setPaintTicks(true);
		rescaleSlider.setPaintLabels(true);
		rescaleSlider.setPreferredSize(new Dimension(500, 50));

		JLabel rescaleLabel = new JLabel(String.valueOf(0));
		rescaleLabel.setPreferredSize(new Dimension(50, rescaleLabel
				.getPreferredSize().height));
		rescaleSlider.addChangeListener(new ControleurSliderLabel(
				rescaleLabel, false));
		rescaleSlider.addChangeListener(new ControleurGlobalRescale(this,vp));

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel labelZ = new JLabel("Scale (%):");

		panel.add(labelZ);
		panel.add(rescaleSlider);
		panel.add(rescaleLabel);
	}

	public JPanel getPanel() {
		return panel;
	}

	public double getScale() {
		return rescaleSlider.getValue()/100.0;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}
}
