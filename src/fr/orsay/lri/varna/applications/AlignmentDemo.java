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
package fr.orsay.lri.varna.applications;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import fr.orsay.lri.varna.VARNAPanel;

import fr.orsay.lri.varna.models.rna.RNA;


public class AlignmentDemo extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = -790155708306987257L;

	private static final String DEFAULT_SEQUENCE1 =  "CGCGCACGCGA----UAUU----UCGCGUCGCGCAUUUGCGCGUAGCGCG";
	private static final String DEFAULT_STRUCTURE1 = "(((((.(((((----....----))))).(((((....)))))..)))))";

	private static final String DEFAULT_SEQUENCE2 =  "CGCGCACGCGSGCGCGUUUGCGCUCGCGU---------------AGCGCG";
	private static final String DEFAULT_STRUCTURE2 = "(((((.(((((((((....))))))))).--------------..)))))";
	// private static final String DEFAULT_STRUCTURE1 = "((((....))))";
	// private static final String DEFAULT_STRUCTURE2 =
	// "((((..(((....)))..))))";

	private VARNAPanel _vpMaster;

	private JPanel _tools = new JPanel();
	private JPanel _input = new JPanel();

	private JPanel _seq1Panel = new JPanel();
	private JPanel _seq2Panel = new JPanel();
	private JPanel _struct1Panel = new JPanel();
	private JPanel _struct2Panel = new JPanel();
	private JLabel _info = new JLabel();
	private JTextField _struct1 = new JTextField(DEFAULT_STRUCTURE1);
	private JTextField _struct2 = new JTextField(DEFAULT_STRUCTURE2);
	private JTextField _seq1 = new JTextField(DEFAULT_SEQUENCE1);
	private JTextField _seq2 = new JTextField(DEFAULT_SEQUENCE2);
	private JLabel _struct1Label = new JLabel(" Str1:");
	private JLabel _struct2Label = new JLabel(" Str2:");
	private JLabel _seq1Label = new JLabel(" Seq1:");
	private JLabel _seq2Label = new JLabel(" Seq2:");
	private JButton _goButton = new JButton("Go");

	private String _str1Backup = "";
	private String _str2Backup = "";
	private String _seq1Backup = "";
	private String _seq2Backup = "";
	private RNA _RNA = new RNA();

	private static String errorOpt = "error";
	@SuppressWarnings("unused")
	private boolean _error;

	private Color _backgroundColor = Color.white;

	@SuppressWarnings("unused")
	private int _algoCode;


	public AlignmentDemo() {
		super();
			_vpMaster = new VARNAPanel(getSeq1(), getStruct1(), getSeq2(), getStruct2(), RNA.DRAW_MODE_RADIATE,"");
		_vpMaster.setPreferredSize(new Dimension(600, 400));
		RNAPanelDemoInit();
	}

	private void RNAPanelDemoInit() {
		int marginTools = 40;

		setBackground(_backgroundColor);
		_vpMaster.setBackground(_backgroundColor);

		Font textFieldsFont = Font.decode("MonoSpaced-PLAIN-12");


		_goButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
					_vpMaster.drawRNA(getSeq1(), getStruct1(), getSeq2(), getStruct2(), _vpMaster.getDrawMode());
				_vpMaster.repaint();
			}
		});

		
		_seq1Label.setHorizontalTextPosition(JLabel.LEFT);
		_seq1Label.setPreferredSize(new Dimension(marginTools, 15));
		_seq1.setFont(textFieldsFont);

		_seq1Panel.setLayout(new BorderLayout());
		_seq1Panel.add(_seq1Label, BorderLayout.WEST);
		_seq1Panel.add(_seq1, BorderLayout.CENTER);

		_seq2Label.setHorizontalTextPosition(JLabel.LEFT);
		_seq2Label.setPreferredSize(new Dimension(marginTools, 15));
		_seq2.setFont(textFieldsFont);
		
		_seq2Panel.setLayout(new BorderLayout());
		_seq2Panel.add(_seq2Label, BorderLayout.WEST);
		_seq2Panel.add(_seq2, BorderLayout.CENTER);

		_struct1Label.setPreferredSize(new Dimension(marginTools, 15));
		_struct1Label.setHorizontalTextPosition(JLabel.LEFT);
		_struct1.setFont(textFieldsFont);
		_struct1Panel.setLayout(new BorderLayout());
		_struct1Panel.add(_struct1Label, BorderLayout.WEST);
		_struct1Panel.add(_struct1, BorderLayout.CENTER);

		_struct2Label.setPreferredSize(new Dimension(marginTools, 15));
		_struct2Label.setHorizontalTextPosition(JLabel.LEFT);
		_struct2.setFont(textFieldsFont);
		_struct2Panel.setLayout(new BorderLayout());
		_struct2Panel.add(_struct2Label, BorderLayout.WEST);
		_struct2Panel.add(_struct2, BorderLayout.CENTER);

		_input.setLayout(new GridLayout(4, 0));
		_input.add(_seq1Panel);
		_input.add(_struct1Panel);
		_input.add(_seq2Panel);
		_input.add(_struct2Panel);

		JPanel goPanel = new JPanel();
		goPanel.setLayout(new BorderLayout());

		_tools.setLayout(new BorderLayout());
		_tools.add(_input, BorderLayout.CENTER);
		_tools.add(_info, BorderLayout.SOUTH);
		_tools.add(goPanel, BorderLayout.EAST);

		goPanel.add(_goButton, BorderLayout.CENTER);
		
		getContentPane().setLayout(new BorderLayout());
		JPanel VARNAs = new JPanel();
		VARNAs.setLayout(new GridLayout(1,1));
		VARNAs.add(_vpMaster);
		getContentPane().add(VARNAs, BorderLayout.CENTER);
		getContentPane().add(_tools, BorderLayout.SOUTH);

		setVisible(true);
		_vpMaster.getVARNAUI().UIRadiate();
	}

	public RNA getRNA() {

		if (!( _str1Backup.equals(getStruct1())
			&& _str2Backup.equals(getStruct2())
			&& _seq1Backup.equals(getSeq1())
			&& _seq2Backup.equals(getSeq2())
		)) {
			_vpMaster.drawRNA(getSeq1(), getStruct1(), getSeq2(), getStruct2(), _vpMaster.getDrawMode());
			_RNA = _vpMaster.getRNA();
			_str1Backup = getStruct1();
			_str2Backup = getStruct2();
			_seq1Backup = getSeq1();
			_seq2Backup = getSeq2();
		}
		return _RNA;
	}


	public String getStruct1() {
		return cleanStruct(_struct1.getText());
	}

	public String getStruct2() {
		return cleanStruct(_struct2.getText());
	}

	public String getSeq1() {
		return cleanStruct(_seq1.getText());
	}

	public String getSeq2() {
		return cleanStruct(_seq2.getText());
	}
	
	
	private String cleanStruct(String struct) {
		struct = struct.replaceAll("[:-]", "-");
		return struct;
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
		_vpMaster.setBackground(_backgroundColor);
		_error = true;
	}

	@SuppressWarnings("unused")
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

	public VARNAPanel get_varnaPanel() {
		return _vpMaster;
	}

	public void set_varnaPanel(VARNAPanel surface) {
		_vpMaster = surface;
	}

	public JTextField get_struct() {
		return _struct1;
	}

	public void set_struct(JTextField _struct) {
		this._struct1 = _struct;
	}

	public JTextField get_seq() {
		return _seq1;
	}

	public void set_seq(JTextField _seq) {
		this._seq1 = _seq;
	}

	public JLabel get_info() {
		return _info;
	}

	public void set_info(JLabel _info) {
		this._info = _info;
	}

	public static void main(String[] args) {
		AlignmentDemo d = new AlignmentDemo();
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}

	public void onWarningEmitted(String s) {
		// TODO Auto-generated method stub
		
	}
}
