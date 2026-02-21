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
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;


import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurBorder;
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;

public class VueBorder {

	private VARNAPanel _vp;
	private JSlider borderHeightSlider, borderWidthSlider;
	private JPanel panel;

	public VueBorder(VARNAPanel vp) {
		_vp = vp;

		JPanel pup = new JPanel();
		JPanel pdown = new JPanel();
		panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		pup.setLayout(new FlowLayout(FlowLayout.LEFT));
		pdown.setLayout(new FlowLayout(FlowLayout.LEFT));

		borderHeightSlider = new JSlider(JSlider.HORIZONTAL, 0,
				_vp.getHeight() / 2 - 10, _vp.getBorderSize().height);
		// Turn on labels at major tick marks.
		borderHeightSlider.setMajorTickSpacing(50);
		borderHeightSlider.setMinorTickSpacing(10);
		borderHeightSlider.setPaintTicks(true);
		borderHeightSlider.setPaintLabels(true);
		JLabel borderHeightLabel = new JLabel(String.valueOf(_vp
				.getBorderSize().height));
		borderHeightLabel.setPreferredSize(new Dimension(50, borderHeightLabel
				.getPreferredSize().height));
		borderHeightSlider.addChangeListener(new ControleurSliderLabel(
				borderHeightLabel, false));
		borderHeightSlider.addChangeListener(new ControleurBorder(this));

		borderWidthSlider = new JSlider(JSlider.HORIZONTAL, 0,
				_vp.getWidth() / 2 - 10, _vp.getBorderSize().width);
		// Turn on labels at major tick marks.
		borderWidthSlider.setMajorTickSpacing(50);
		borderWidthSlider.setMinorTickSpacing(10);
		borderWidthSlider.setPaintTicks(true);
		borderWidthSlider.setPaintLabels(true);
		JLabel borderWidthLabel = new JLabel(String
				.valueOf(_vp.getBorderSize().width));
		borderWidthLabel.setPreferredSize(new Dimension(50, borderWidthLabel
				.getPreferredSize().height));
		borderWidthSlider.addChangeListener(new ControleurSliderLabel(
				borderWidthLabel, false));
		borderWidthSlider.addChangeListener(new ControleurBorder(this));

		JLabel labelW = new JLabel("Width:");
		JLabel labelH = new JLabel("Height:");

		pup.add(labelW);
		pup.add(borderWidthSlider);
		pup.add(borderWidthLabel);

		pdown.add(labelH);
		pdown.add(borderHeightSlider);
		pdown.add(borderHeightLabel);

		panel.add(pup);
		panel.add(pdown);
	}

	public JPanel getPanel() {
		return panel;
	}

	public Dimension getDimension() {
		return new Dimension(borderWidthSlider.getValue(), borderHeightSlider
				.getValue());
	}

	public int getHeight() {
		return borderHeightSlider.getValue();
	}

	public int getWidth() {
		return borderWidthSlider.getValue();
	}

	public VARNAPanel get_vp() {
		return _vp;
	}
}
