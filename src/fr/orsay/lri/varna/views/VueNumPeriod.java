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
import fr.orsay.lri.varna.controlers.ControleurNumPeriod;
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;

public class VueNumPeriod {
	private VARNAPanel _vp;
	private JPanel panel;
	private JSlider numPeriodSlider;

	public VueNumPeriod(VARNAPanel vp) {
		_vp = vp;
		panel = new JPanel();

		int maxPeriod = _vp.getRNA().get_listeBases().size();
		numPeriodSlider = new JSlider(JSlider.HORIZONTAL, 1, maxPeriod, Math
				.min(_vp.getNumPeriod(), maxPeriod));
		// Turn on labels at major tick marks.
		numPeriodSlider.setMajorTickSpacing(10);
		numPeriodSlider.setMinorTickSpacing(5);
		numPeriodSlider.setPaintTicks(true);
		numPeriodSlider.setPaintLabels(true);

		JLabel numLabel = new JLabel(String.valueOf(_vp.getNumPeriod()));
		numLabel.setPreferredSize(new Dimension(50,
				numLabel.getPreferredSize().height));
		numPeriodSlider.addChangeListener(new ControleurSliderLabel(numLabel,
				false));
		numPeriodSlider.addChangeListener(new ControleurNumPeriod(this));

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel labelS = new JLabel("NumPeriod:");

		panel.add(labelS);
		panel.add(numPeriodSlider);
		panel.add(numLabel);
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public JPanel getPanel() {
		return panel;
	}

	public int getNumPeriod() {
		return numPeriodSlider.getValue();
	}
}
