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

public class VueJPEG {
	private JSlider qualitySlider;
	private JSlider scaleSlider;
	private JPanel panel;

	// Turn on labels at major tick marks.
//	public VueJPEG() {
//	}

	public VueJPEG(boolean showQuality, boolean showScale) {
		qualitySlider = new JSlider(JSlider.HORIZONTAL, 10, 100, 75);
		qualitySlider.setMajorTickSpacing(5);
		qualitySlider.setPaintTicks(true);
		qualitySlider.setPaintLabels(true);
		qualitySlider.setPreferredSize(new Dimension(400, 50));
		scaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 600, 100);
		scaleSlider.setPreferredSize(new Dimension(400, 50));
		scaleSlider.setMajorTickSpacing(100);
		scaleSlider.setPaintTicks(true);
		scaleSlider.setPaintLabels(true);
		panel = new JPanel();
		JPanel pup = new JPanel();
		JPanel pdown = new JPanel();
		int nbPanels = 0;
		if (showQuality)
			nbPanels++;
		if (showScale)
			nbPanels++;
		panel.setLayout(new GridLayout(nbPanels, 1));
		pup.setLayout(new FlowLayout(FlowLayout.LEFT));
		pdown.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel lseq = new JLabel("Resolution:");
		JLabel lstr = new JLabel("Quality:");
		pup.add(lseq);
		pup.add(scaleSlider);
		pdown.add(lstr);
		pdown.add(qualitySlider);
		if (showQuality) {
			panel.add(pup);
		}
		if (showScale) {
			panel.add(pdown);
		}
	}

	public JSlider getQualitySlider() {
		return qualitySlider;
	}

	public JSlider getScaleSlider() {
		return scaleSlider;
	}

	public JPanel getPanel() {
		return panel;
	}
}
