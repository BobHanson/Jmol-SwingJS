package fr.orsay.lri.varna.applications;

/*
VARNA is a Java library for quick automated drawings RNA secondary structure 
Copyright (C) 2007  Yann Ponty

This program is free software:you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.VARNAConsole;
import fr.orsay.lri.varna.controlers.ControleurDemoTextField;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.models.rna.RNA;

/**
* An RNA 2d Panel demo applet
* 
* @author Yann Ponty & Darty Kévin
* 
*/

public class VARNAConsoleDemo extends JApplet implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -790155708306987257L;

	private static final String DEFAULT_SEQUENCE = "CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIA";

	private static final String DEFAULT_STRUCTURE = "..(((((...(((((...(((((...(((((.....)))))...))))).....(((((...(((((.....)))))...))))).....)))))...)))))..";

	private VARNAPanel _vp;

	private JPanel _tools = new JPanel();
	private JPanel _input = new JPanel();

	private JPanel _seqPanel = new JPanel();
	private JPanel _structPanel = new JPanel();
	private JLabel _info = new JLabel();
	private JButton _go = new JButton("Go");
	private JTextField _struct = new JTextField();
	private JTextField _seq = new JTextField();
	private JLabel _structLabel = new JLabel(" Str:");
	private JLabel _seqLabel = new JLabel(" Seq:");

	private VARNAConsole _console;

	private static String errorOpt = "error";
	private boolean _error;

	private Color _backgroundColor = Color.white;

	private int _algoCode;

	public VARNAConsoleDemo() {
		super();
		try {
			_vp = new VARNAPanel(_seq.getText(), _struct.getText());
			_vp.setErrorsOn(false);
		} catch (ExceptionNonEqualLength e) {
			_vp.errorDialog(e);
		}
		RNAPanelDemoInit();
	}

	private void RNAPanelDemoInit() {
		
		
		int marginTools = 40;

		setBackground(_backgroundColor);
		_vp.setBackground(_backgroundColor);

		try {
			_vp.getRNA().setRNA(_seq.getText(), _struct.getText());
			_vp.setErrorsOn(false);
		} catch (Exception e1) {
			_vp.errorDialog(e1);
		}

		Font textFieldsFont = Font.decode("MonoSpaced-PLAIN-12");

		_console = new VARNAConsole(_vp); 

		
		_go.addActionListener(this);
		
		_seqLabel.setHorizontalTextPosition(JLabel.LEFT);
		_seqLabel.setPreferredSize(new Dimension(marginTools, 15));
		_seq.setFont(textFieldsFont);
		_seq.setText(_vp.getRNA().getSeq());

		_seqPanel.setLayout(new BorderLayout());
		_seqPanel.add(_seqLabel, BorderLayout.WEST);
		_seqPanel.add(_seq, BorderLayout.CENTER);

		_structLabel.setPreferredSize(new Dimension(marginTools, 15));
		_structLabel.setHorizontalTextPosition(JLabel.LEFT);
		_struct.setFont(textFieldsFont);
		_struct.setText(_vp.getRNA().getStructDBN());
		_structPanel.setLayout(new BorderLayout());
		_structPanel.add(_structLabel, BorderLayout.WEST);
		_structPanel.add(_struct, BorderLayout.CENTER);


		_input.setLayout(new GridLayout(3, 0));
		_input.add(_seqPanel);
		_input.add(_structPanel);

		_tools.setLayout(new BorderLayout());
		_tools.add(_input, BorderLayout.CENTER);
		_tools.add(_info, BorderLayout.SOUTH);
		_tools.add(_go, BorderLayout.EAST);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(_vp, BorderLayout.CENTER);
		getContentPane().add(_tools, BorderLayout.SOUTH);

		_vp.getVARNAUI().UIRadiate();
		setPreferredSize(new Dimension(400,400));
		
		setVisible(true);
		
		_console.setVisible(true);
		
	}

	public String[][] getParameterInfo() {
		String[][] info = {
				// Parameter Name Kind of Value Description,
				{ "sequenceDBN", "String", "A raw RNA sequence" },
				{ "structureDBN", "String",
						"An RNA structure in dot bracket notation (DBN)" },
				{ errorOpt, "boolean", "To show errors" }, };
		return info;
	}

	public void init() {
		retrieveParametersValues();
		_vp.setBackground(_backgroundColor);
		_error = true;
	}

	private Color getSafeColor(String col, Color def) {
		Color result;
		try {
			result = Color.decode(col);
		} catch (Exception e) {
			try {
				result = Color.getColor(col, def);
			} catch (Exception e2) {
				return def;
			}
		}
		return result;
	}

	private String getParameterValue(String key, String def) {
		String tmp;
		tmp = getParameter(key);
		if (tmp == null) {
			return def;
		} else {
			return tmp;
		}
	}

	private void retrieveParametersValues() {
		_error = Boolean.parseBoolean(getParameterValue(errorOpt, "false"));
		_vp.setErrorsOn(_error);
		_backgroundColor = getSafeColor(getParameterValue("background",
				_backgroundColor.toString()), _backgroundColor);
		_vp.setBackground(_backgroundColor);
		_seq.setText(getParameterValue("sequenceDBN", ""));
		_struct.setText(getParameterValue("structureDBN", ""));
		String _algo = getParameterValue("algorithm", "radiate");
		if (_algo.equals("circular"))
			_algoCode = RNA.DRAW_MODE_CIRCULAR;
		else if (_algo.equals("naview"))
			_algoCode = RNA.DRAW_MODE_NAVIEW;
		else if (_algo.equals("line"))
			_algoCode = RNA.DRAW_MODE_LINEAR;
		else
			_algoCode = RNA.DRAW_MODE_RADIATE;
		if (_seq.getText().equals("") && _struct.getText().equals("")) {
			_seq.setText(DEFAULT_SEQUENCE);
			_struct.setText(DEFAULT_STRUCTURE);
		}
		try {
			_vp.drawRNA(_seq.getText(), _struct.getText(), _algoCode);
		} catch (ExceptionNonEqualLength e) {
			e.printStackTrace();
		}

	}

	public VARNAPanel get_varnaPanel() {
		return _vp;
	}

	public void set_varnaPanel(VARNAPanel surface) {
		_vp = surface;
	}

	public JTextField get_struct() {
		return _struct;
	}

	public void set_struct(JTextField _struct) {
		this._struct = _struct;
	}

	public JTextField get_seq() {
		return _seq;
	}

	public void set_seq(JTextField _seq) {
		this._seq = _seq;
	}

	public JLabel get_info() {
		return _info;
	}

	public void set_info(JLabel _info) {
		this._info = _info;
	}

	public void actionPerformed(ActionEvent arg0) {
		  RNA r = new RNA();
		  try {
			_vp.drawRNAInterpolated(_seq.getText(), _struct.getText());
		} catch (ExceptionNonEqualLength e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  _vp.repaint();
	}
}
