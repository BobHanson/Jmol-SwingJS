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

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ControleurSliderLabel implements ChangeListener {

	private JLabel _l;
	private double _factor = 1.0;

	public ControleurSliderLabel(JLabel zoomAmountValueLabel) {
		_l = zoomAmountValueLabel;
		_factor = 1.0;
	}

	public ControleurSliderLabel(JLabel zoomAmountValueLabel, boolean percent) {
		_l = zoomAmountValueLabel;
		if (percent)
			_factor = 0.01;
	}

	public ControleurSliderLabel(JLabel zoomAmountValueLabel, double factor) {
		_l = zoomAmountValueLabel;
		_factor = factor;
	}

	public void stateChanged(ChangeEvent e) {
		double value = ((JSlider) e.getSource()).getValue();
		value *= _factor;
		_l.setText(String.valueOf(value));
	}
}
