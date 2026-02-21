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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;
import fr.orsay.lri.varna.controlers.ControleurZoom;
import fr.orsay.lri.varna.models.VARNAConfig;

public class VueZoom implements ChangeListener {

	private VARNAPanel _vp;
	private JSlider zoomSlider, zoomAmountSlider;
	private JPanel panel;

	public VueZoom(VARNAPanel vp) {
		_vp = vp;

		JPanel pup = new JPanel();
		JPanel pdown = new JPanel();
		panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		pup.setLayout(new FlowLayout(FlowLayout.LEFT));
		pdown.setLayout(new FlowLayout(FlowLayout.LEFT));

		zoomSlider = new JSlider(JSlider.HORIZONTAL,
				(int) (VARNAConfig.MIN_ZOOM * 100),
				(int) (VARNAConfig.MAX_ZOOM * 100), (int) (_vp.getZoom() * 100));
		// Turn on labels at major tick marks.
		zoomSlider.setMajorTickSpacing(2000);
		zoomSlider.setMinorTickSpacing(500);
		zoomSlider.setPaintTicks(true);
		zoomSlider.setPaintLabels(true);
		zoomSlider.setPreferredSize(new Dimension(250, zoomSlider
				.getPreferredSize().height));
		zoomSlider.addChangeListener(new ControleurZoom(this));

		JLabel zoomValueLabel = new JLabel(String.valueOf(_vp.getZoom()));
		zoomValueLabel.setPreferredSize(new Dimension(50, zoomValueLabel
				.getPreferredSize().height));
		zoomSlider.addChangeListener(new ControleurSliderLabel(zoomValueLabel,
				true));

		zoomAmountSlider = new JSlider(JSlider.HORIZONTAL,
				(int) (VARNAConfig.MIN_AMOUNT * 100),
				(int) (VARNAConfig.MAX_AMOUNT * 100), (int) (_vp
						.getZoomIncrement() * 100));
		// Turn on labels at major tick marks.
		zoomAmountSlider.setMajorTickSpacing(50);
		zoomAmountSlider.setMinorTickSpacing(10);
		zoomAmountSlider.setPaintTicks(true);
		zoomAmountSlider.setPaintLabels(true);
		zoomAmountSlider.setPreferredSize(new Dimension(200, zoomAmountSlider
				.getPreferredSize().height));

		JLabel zoomAmountValueLabel = new JLabel(String.valueOf(_vp
				.getZoomIncrement()));
		zoomAmountValueLabel.setPreferredSize(new Dimension(50,
				zoomAmountValueLabel.getPreferredSize().height));
		zoomAmountSlider.addChangeListener(new ControleurSliderLabel(
				zoomAmountValueLabel, true));
		zoomAmountSlider.addChangeListener(this);

		JLabel labelZ = new JLabel("Zoom:");
		JLabel labelA = new JLabel("Increment:");

		pup.add(labelZ);
		pup.add(zoomSlider);
		pup.add(zoomValueLabel);
		pdown.add(labelA);
		pdown.add(zoomAmountSlider);
		pdown.add(zoomAmountValueLabel);
		panel.add(pup);
		panel.add(pdown);
	}

	public JPanel getPanel() {
		return panel;
	}

	public double getZoom() {
		return zoomSlider.getValue() / 100.0;
	}

	public double getZoomAmount() {
		return zoomAmountSlider.getValue() / 100.0;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public void stateChanged(ChangeEvent e) {
		_vp.setZoomIncrement(getZoomAmount());
	}
}
