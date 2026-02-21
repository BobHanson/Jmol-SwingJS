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

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

import fr.orsay.lri.varna.VARNAPanel;

public class VueFont {

	private VARNAPanel _vp;
	private Font font;
	private JComboBox stylesBox;
	private JComboBox boxPolice;
	private JPanel panel;
	private JSlider sizeSlider;

	public VueFont(VARNAPanel vp) {
		_vp = vp;
		init();
		buildViewVPTitle();
	}

	public VueFont(Font f) {
		font = f;
		init();
		buildViewFont();
	}

	private void init() {
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		String[] polices = ge.getAvailableFontFamilyNames();
		boxPolice = new JComboBox(polices);

		sizeSlider = new JSlider(JSlider.HORIZONTAL, 4, 88, 14);
		// Turn on labels at major tick marks.
		sizeSlider.setMajorTickSpacing(10);
		sizeSlider.setMinorTickSpacing(5);
		sizeSlider.setPaintTicks(true);
		sizeSlider.setPaintLabels(true);

		String[] styles = { "Plain", "Italic", "Bold" };
		stylesBox = new JComboBox(styles);

		panel = new JPanel();
		panel.add(boxPolice);
		panel.add(sizeSlider);
		panel.add(stylesBox);
	}

	private void buildViewFont() {
		boxPolice.setSelectedItem(font.getFamily());
		sizeSlider.setValue(font.getSize());
		stylesBox.setSelectedItem(styleIntToString(font.getStyle()));
	}

	private void buildViewVPTitle() {
		boxPolice.setSelectedItem(_vp.getTitleFont().getFamily());
		sizeSlider.setValue(_vp.getTitleFont().getSize());
		stylesBox.setSelectedItem(styleIntToString(_vp.getTitleFont()
				.getStyle()));
	}

	public String styleIntToString(int styleInt) {
		switch (styleInt) {
		case Font.PLAIN:// Plain
			return "Plain";
		case Font.ITALIC:// Italic
			return "Italic";
		case Font.BOLD:// Bold
			return "Bold";
		default:// Plain
			return "Plain";
		}
	}

	public JComboBox getStylesBox() {
		return stylesBox;
	}

	public JComboBox getBoxPolice() {
		return boxPolice;
	}

	public JPanel getPanel() {
		return panel;
	}

	public JSlider getSizeSlider() {
		return sizeSlider;
	}

	public Font getFont() {
		int style;
		switch (getStylesBox().getSelectedIndex()) {
		case 0:// Plain
			style = Font.PLAIN;
			break;
		case 1:// Italic
			style = Font.ITALIC;
			break;
		case 2:// Bold
			style = Font.BOLD;
			break;
		default:// Plain
			style = Font.PLAIN;
			break;
		}
		return new Font((String) getBoxPolice().getSelectedItem(), style,
				getSizeSlider().getValue());
	}
}
