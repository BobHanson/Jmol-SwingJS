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
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.ModeleBP;

public class VueBPThickness implements ChangeListener {

	private VARNAPanel _vp;
	ArrayList<ModeleBP> _msbp;
	private JSlider _thicknessSlider;
	private JPanel panel;

	private ArrayList<Double> _backupThicknesses = new ArrayList<Double>();

	private double FACTOR = 10.0;

	public VueBPThickness(VARNAPanel vp, ArrayList<ModeleBP> msbp) {
		_vp = vp;
		_msbp = msbp;
		backupThicknesses();

		_thicknessSlider = new JSlider(JSlider.HORIZONTAL, 1, 100,
				(int) (msbp.get(0).getStyle().getThickness(
						VARNAConfig.DEFAULT_BP_THICKNESS) * FACTOR));
		_thicknessSlider.setMajorTickSpacing(10);
		_thicknessSlider.setPaintTicks(true);
		_thicknessSlider.setPaintLabels(false);
		_thicknessSlider.setPreferredSize(new Dimension(500, 50));

		JLabel thicknessLabel = new JLabel(String.valueOf(msbp.get(0).getStyle()
				.getThickness(VARNAConfig.DEFAULT_BP_THICKNESS)));
		thicknessLabel.setPreferredSize(new Dimension(50, thicknessLabel
				.getPreferredSize().height));
		_thicknessSlider.addChangeListener(new ControleurSliderLabel(
				thicknessLabel, 1.0 / FACTOR));
		_thicknessSlider.addChangeListener(this);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel labelZ = new JLabel("Thickness:");

		panel.add(labelZ);
		panel.add(_thicknessSlider);
		panel.add(thicknessLabel);
	}

	private void backupThicknesses() {
		for (int i = 0; i < _msbp.size(); i++) {
			this._backupThicknesses.add(_msbp.get(i).getStyle().getThickness(
					VARNAConfig.DEFAULT_BP_THICKNESS));
		}
	}

	public void restoreThicknesses() {
		for (int i = 0; i < _msbp.size(); i++) {
			_msbp.get(i).getStyle().setThickness(_backupThicknesses.get(i));
		}
	}

	public JPanel getPanel() {
		return panel;
	}

	public double getThickness() {
		return (double) _thicknessSlider.getValue() / FACTOR;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public void stateChanged(ChangeEvent e) {
		for (int i = 0; i < _msbp.size(); i++) {
			_msbp.get(i).getStyle().setThickness(
					((double) _thicknessSlider.getValue()) / FACTOR);
		}
		_vp.repaint();
	}
}
