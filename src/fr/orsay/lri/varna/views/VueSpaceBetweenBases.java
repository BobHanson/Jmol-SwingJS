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
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;
import fr.orsay.lri.varna.controlers.ControleurSpaceBetweenBases;

public class VueSpaceBetweenBases {
	private VARNAPanel _vp;
	private JPanel panel;
	private JSlider spaceSlider;

	public VueSpaceBetweenBases(VARNAPanel vp) {
		_vp = vp;
		panel = new JPanel();

		spaceSlider = new JSlider(JSlider.HORIZONTAL, 10, 200, Integer
				.valueOf(String.valueOf(Math.round(_vp.getSpaceBetweenBases() * 100))));
		// Turn on labels at major tick marks.
		spaceSlider.setMajorTickSpacing(30);
		spaceSlider.setPaintTicks(true);
		spaceSlider.setPaintLabels(true);

		JLabel spaceLabel = new JLabel(String.valueOf(100.0 * _vp.getSpaceBetweenBases()));
		spaceLabel.setPreferredSize(new Dimension(50, spaceLabel
				.getPreferredSize().height));
		spaceSlider.addChangeListener(new ControleurSliderLabel(spaceLabel,
				false));
		spaceSlider.addChangeListener(new ControleurSpaceBetweenBases(this));

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel labelS = new JLabel("Space:");

		panel.add(labelS);
		panel.add(spaceSlider);
		panel.add(spaceLabel);
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public JPanel getPanel() {
		return panel;
	}

	public Double getSpace() {
		return spaceSlider.getValue() / 100.0;
	}
}
