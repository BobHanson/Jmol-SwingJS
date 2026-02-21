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
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import fr.orsay.lri.varna.VARNAPanel;


public class VueManualInput {

	private VARNAPanel _vp;
	private JPanel panel;
	private JTextField tseq, tstr;

	public VueManualInput(VARNAPanel vp) {
		_vp = vp;
		buildView();
	}

	private void buildView() {
		panel = new JPanel();
		JPanel pup = new JPanel();
		JPanel pdown = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		pup.setLayout(new FlowLayout(FlowLayout.LEFT));
		pdown.setLayout(new FlowLayout(FlowLayout.LEFT));

		Font _textFieldsFont = Font.decode("MonoSpaced-PLAIN-12");

		JLabel lseq = new JLabel("Sequence:");
		tseq = new JTextField(_vp.getRNA().getListeBasesToString());
		JLabel lstr = new JLabel("Structure:");
		tstr = new JTextField(_vp.getRNA().getStructDBN());
		tstr
				.setPreferredSize(new Dimension(400,
						tstr.getPreferredSize().height));
		tseq
				.setPreferredSize(new Dimension(400,
						tseq.getPreferredSize().height));
		tstr.setFont(_textFieldsFont);
		tseq.setFont(_textFieldsFont);
		pup.add(lseq);
		pup.add(tseq);
		pdown.add(lstr);
		pdown.add(tstr);
		panel.add(pup);
		panel.add(pdown);
	}

	public JPanel getPanel() {
		return panel;
	}

	public void setPanel(JPanel panel) {
		this.panel = panel;
	}

	public JTextField getTseq() {
		return tseq;
	}

	public JTextField getTstr() {
		return tstr;
	}
}
