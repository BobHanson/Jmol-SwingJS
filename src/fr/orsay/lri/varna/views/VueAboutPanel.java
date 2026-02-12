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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.models.VARNAConfig;

/**
 * BH j2s SwingJS replaces thread with simple javax.swing.Timer
 * 
 */
public class VueAboutPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4525998278180950602L;

	private AboutAnimator _anim;
	private JPanel _textPanel;
	private JTextArea _textArea;

	public VueAboutPanel() {
		init();
	}

	private void init() {
		try {
			setBorder(BorderFactory.createEtchedBorder());
			setLayout(new BorderLayout());
			setBackground(Color.WHITE);

			String message = "VARNA "
					+ VARNAConfig.MAJOR_VERSION
					+ "."
					+ VARNAConfig.MINOR_VERSION
					+ "\n"
					+ "\n"
					+ "Created by: Kevin Darty, Alain Denise and Yann Ponty\n"
					+ "Contact: ponty@lri.fr\n"
					+ "\n"
					+ "VARNA is freely distributed under the terms of the GNU GPL 3.0 license.\n"
					+ "\n"
					+ "Supported by the BRASERO project (ANR-06-BLAN-0045)\n";

			_textArea = new JTextArea();
			_textArea.setText(message);
			_textArea.setEditable(false);

			_textPanel = new JPanel();
			_textPanel.setBackground(Color.WHITE);
			_textPanel.setLayout(new BorderLayout());
			_textPanel.setBorder(BorderFactory.createMatteBorder(0, 15, 0, 15,
					getBackground()));
			_textPanel.add(_textArea);

			VARNAPanel vp = new VARNAPanel("GGGGAAAACCCC", "((((....))))");
			vp.setModifiable(false);
			vp.setPreferredSize(new Dimension(100, 100));
			// vp.setBorder(BorderFactory.createLineBorder(Color.gray));

			_anim = new AboutAnimator(vp);
			_anim
					.addRNA("GGGGAAGGGGAAAACCCCAACCCC",
							"((((..((((....))))..))))");
			_anim.addRNA("GGGGAAGGGGAAGGGGAAAACCCCAACCCCAACCCC",
					"((((..((((..((((....))))..))))..))))");
			_anim
					.addRNA(
							"GGGGAGGGGAAAACCCCAGGGGAGGGGAAAACCCCAGGGGAAAACCCCAGGGGAAAACCCCACCCCAGGGGAAAACCCCACCCC",
							"((((.((((....)))).((((.((((....)))).((((....)))).((((....)))).)))).((((....)))).))))");
			_anim
					.addRNA(
							"GGGGGGGGAAAACCCCAGGGGAAAACCCCAGGGGGGGGAAAACCCCAGGGGAAAACCCCAGGGGAAAACCCCAGGGGAAAACCCCGGGGAAAACCCCACCCCAGGGGAAAACCCCAGGGGAAAACCCCCCCC",
							"((((((((....)))).((((....)))).((((((((....)))).((((....)))).((((....)))).((((....))))((((....)))).)))).((((....)))).((((....))))))))");
			_anim.addRNA("GGGGAAAACCCC", "((((....))))");
			_anim.addRNA("GGGGAAGGGGAAAACCCCAGGGGAAAACCCCACCCC",
					"((((..((((....)))).((((....)))).))))");
			_anim.addRNA("GGGGAGGGGAAAACCCCAGGGGAAAACCCCAGGGGAAAACCCCACCCC",
					"((((.((((....)))).((((....)))).((((....)))).))))");
			_anim
					.addRNA(
							"GGGGAGGGGAAAAAAACCCCAGGGGAAAAAAACCCCAGGGGAAAAAAACCCCACCCC",
							"((((.((((.......)))).((((.......)))).((((.......)))).))))");
			_anim.start();

			add(vp, BorderLayout.WEST);
			add(_textPanel, BorderLayout.CENTER);
		} catch (ExceptionNonEqualLength e) {
		}
	}

	public void gracefulStop() {
		_anim.gracefulStop();
	}

	private class AboutAnimator implements ActionListener {
		VARNAPanel _vp;
		ArrayList<String> _structures = new ArrayList<String>();
		ArrayList<String> _sequences = new ArrayList<String>();
		int _period = 2000;
		boolean _over = false;

		public AboutAnimator(VARNAPanel vp) {
			super();
			_vp = vp;
		}

		/**
		 * mode pointer for timer cycle -- DELAY1, TASK, DELAY2, STOP
		 */
		int mode = 0;
		
		/**
		 * modes for run()
		 * 
		 */
		final int DELAY1 = 0, TASK = 1, DELAY2 = 2, STOP = 3;
		int i = 0;

		@Override
		public void actionPerformed(ActionEvent e) {
			run();
		}

		public void start() {
			int mode = DELAY1;
			run();
		}

		public void addRNA(String seq, String str) {
			_sequences.add(seq);
			_structures.add(str);
		}

		public void gracefulStop() {
			_over = true;
		}

		public void run() {
			int initialDelay;
			if (_over)
				mode = STOP;
			switch (mode) {
			case DELAY1:
				mode = TASK;
				initialDelay = _period;
				break;
			case TASK:
				String seq = _sequences.get(i);
				String str = _structures.get(i);
				try {
					_vp.drawRNAInterpolated(seq, str);
					mode = DELAY2;
					initialDelay = 500;
				} catch (ExceptionNonEqualLength e) {
					initialDelay = -1;
				}
				break;
			case DELAY2:
				i = (i + 1) % _sequences.size();
				mode = DELAY1;
				initialDelay = 0;
				break;
			case STOP:
			default:
				initialDelay = -1;
				break;
			}
			if (initialDelay >= 0) {
				Timer t = new Timer(initialDelay, this);
				t.setDelay(0);
				t.setRepeats(false);
				t.start();
			} else {
				System.out.println("VueAbout done");
			}

		}
	}

}
