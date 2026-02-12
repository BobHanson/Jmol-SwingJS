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
import fr.orsay.lri.varna.controlers.ControleurInterpolator;
import fr.orsay.lri.varna.exceptions.ExceptionDrawingAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.MappingException;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;
import fr.orsay.lri.varna.models.rna.RNA;

import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;;

public class SuperpositionDemo extends JFrame implements InterfaceVARNAListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -790155708306987257L;

	private static final String DEFAULT_SEQUENCE1 = "CGCGCACGCGAUAUUUCGCGUCGCGCAUUUGCGCGUAGCGCG";
	private static final String DEFAULT_SEQUENCE2 = "CGCGCACGCGAUAUUUCGCGUCGCGCAUUUGCGCGUAGCGCG";

	private static final String DEFAULT_STRUCTURE1 = "(((((.(((((----....----))))).(((((....)))))..)))))";
	private static final String DEFAULT_STRUCTURE2 = "(((((.(((((((((....))))))))).--------------..)))))";
	// private static final String DEFAULT_STRUCTURE1 = "((((....))))";
	// private static final String DEFAULT_STRUCTURE2 =
	// "((((..(((....)))..))))";

	private VARNAPanel _vpMaster;
	private VARNAPanel _vpSlave;

	private JPanel _tools = new JPanel();
	private JPanel _input = new JPanel();

	private JPanel _seqPanel = new JPanel();
	private JPanel _struct1Panel = new JPanel();
	private JPanel _struct2Panel = new JPanel();
	private JLabel _info = new JLabel();
	private JTextField _struct1 = new JTextField(DEFAULT_STRUCTURE1);
	private JTextField _struct2 = new JTextField(DEFAULT_STRUCTURE2);
	private JTextField _seq1 = new JTextField(DEFAULT_SEQUENCE1);
	private JTextField _seq2 = new JTextField(DEFAULT_SEQUENCE2);
	private JLabel _struct1Label = new JLabel(" Str1:");
	private JLabel _struct2Label = new JLabel(" Str2:");
	private JLabel _seqLabel = new JLabel(" Seq:");
	private JButton _goButton = new JButton("Go");
	private JButton _switchButton = new JButton("Switch");

	private String _str1Backup = "";
	private String _str2Backup = "";
	private RNA _RNA1 = new RNA();
	private RNA _RNA2 = new RNA();

	private static String errorOpt = "error";
	@SuppressWarnings("unused")
	private boolean _error;

	private Color _backgroundColor = Color.white;

	@SuppressWarnings("unused")
	private int _algoCode;

	private int _currentDisplay = 1;

	public static ModelBaseStyle createStyle(String txt) 
	{
		ModelBaseStyle result = new ModelBaseStyle();
		try {
			result.assignParameters(txt);
		} catch (ExceptionModeleStyleBaseSyntaxError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public void applyTo(VARNAPanel vp, ModelBaseStyle mb, int[] indices)
	{
		for(int i=0;i<indices.length;i++)
		{ 
			ModeleBase m = vp.getRNA().getBaseAt(indices[i]);
			m.setStyleBase(mb);
			if (m.getElementStructure()!=-1)
			{
				vp.getRNA().getBaseAt(m.getElementStructure()).setStyleBase(mb);
			}
		}
		vp.repaint();
	}
	
	
	public SuperpositionDemo() {
		super();
		try {
			_vpMaster = new VARNAPanel(getText1(), getStruct1());
			_vpSlave = new VARNAPanel(getText2(), getStruct2());
		} catch (ExceptionNonEqualLength e) {
			_vpMaster.errorDialog(e);
		}
		_vpMaster.setPreferredSize(new Dimension(400, 400));
		RNAPanelDemoInit();
	}

	private void RNAPanelDemoInit() {
		int marginTools = 40;

		setBackground(_backgroundColor);
		_vpMaster.setBackground(_backgroundColor);
		_vpMaster.addVARNAListener(this);
		//_vpSlave.setModifiable(false);
		_vpSlave.setBackground(Color.decode("#F0F0F0"));

		_vpMaster.drawRNA(getRNA((_currentDisplay)%2));
		_vpSlave.drawRNA(getRNA((_currentDisplay+1)%2));

		/*ModeleStyleBase red = createStyle("label=#ff4d4d,fill=#ffdddd,outline=#ff4d4d");
		int[] ired = {6,7,8,9,10};
		applyTo(_vpMaster, red, ired);
		applyTo(_vpSlave, red, ired);
		ModeleStyleBase blue = createStyle("label=#0000ff,fill=#ddddff,outline=#0000ff");
		int[] iblue = {0,1,2,3,4};
		applyTo(_vpMaster, blue, iblue);
		applyTo(_vpSlave, blue, iblue);
		ModeleStyleBase purple = createStyle("label=#cc00cc,fill=#ffddff,outline=#cc00cc");
		int[] ipurple = {21,22,23,24,25};
		applyTo(_vpMaster, purple, ipurple);
		ModeleStyleBase green = createStyle("label=#009900,fill=#aaeeaa,outline=#009900");
		int[] igreen = {11,12,13,14};
		applyTo(_vpSlave, green, igreen);*/
		
		Font textFieldsFont = Font.decode("MonoSpaced-PLAIN-12");

		_seqLabel.setHorizontalTextPosition(JLabel.LEFT);
		_seqLabel.setPreferredSize(new Dimension(marginTools, 15));
		_seq1.setFont(textFieldsFont);
		_seq1.setText(getRNA1().getSeq());
		_seq2.setFont(textFieldsFont);
		_seq2.setText(getRNA2().getSeq());

		_goButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				_currentDisplay = (_currentDisplay + 1) % 2;
				_vpMaster.drawRNA(getRNA(_currentDisplay));
				_vpSlave.drawRNA(getRNA((_currentDisplay + 1) % 2));
				onStructureRedrawn();
			}
		});

		_switchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					_currentDisplay = (_currentDisplay + 1) % 2;

							Mapping m = Mapping.readMappingFromAlignment(
									getStruct(_currentDisplay), getStruct((_currentDisplay+1)%2));
							Mapping m2 = Mapping.readMappingFromAlignment(
									getStruct((_currentDisplay+1)%2), getStruct(_currentDisplay));
							_vpMaster.showRNAInterpolated(getRNA(_currentDisplay), m);
							_vpSlave.showRNAInterpolated(getRNA((_currentDisplay+1)%2), m2);
							onStructureRedrawn();
					} 
				catch (MappingException e3) 
				{
							try {
								_vpMaster.drawRNAInterpolated(getText(_currentDisplay),getStruct(_currentDisplay));
							} catch (ExceptionNonEqualLength e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
				_vpMaster.repaint();
				_vpSlave.repaint();
			}
		});

		_seqPanel.setLayout(new BorderLayout());
		_seqPanel.add(_seqLabel, BorderLayout.WEST);
		_seqPanel.add(_seq1, BorderLayout.CENTER);

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

		_input.setLayout(new GridLayout(3, 0));
		_input.add(_seqPanel);
		_input.add(_struct1Panel);
		_input.add(_struct2Panel);

		JPanel goPanel = new JPanel();
		goPanel.setLayout(new BorderLayout());

		_tools.setLayout(new BorderLayout());
		_tools.add(_input, BorderLayout.CENTER);
		_tools.add(_info, BorderLayout.SOUTH);
		_tools.add(goPanel, BorderLayout.EAST);

		goPanel.add(_goButton, BorderLayout.CENTER);
		goPanel.add(_switchButton, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout());
		JPanel VARNAs = new JPanel();
		VARNAs.setLayout(new GridLayout(1,2));
		VARNAs.add(_vpMaster);
		VARNAs.add(_vpSlave);
		getContentPane().add(VARNAs, BorderLayout.CENTER);
		getContentPane().add(_tools, BorderLayout.SOUTH);

		setVisible(true);

		_vpMaster.getVARNAUI().UIRadiate();
		_vpSlave.getVARNAUI().UIRadiate();		
		
		onStructureRedrawn();
	}

	public RNA getMasterRNA()
	{
		return getRNA(_currentDisplay);
	}

	public RNA getSlaveRNA1()
	{
		return getRNA((_currentDisplay+1)%2);
	}

	public RNA getSlaveRNA2()
	{
		return getRNA((_currentDisplay+2)%2);
	}

	
	public RNA getRNA(int i) {
	  if (i==0)
	  {  return getRNA1();  }
	  else 
	  {  return getRNA2();  }
	}

	
	public RNA getRNA1() {
		if (!_str1Backup.equals(getStruct1())) {
			try {
				_RNA1.setRNA(getText1(), getStruct1());
				_RNA1.drawRNA(_vpMaster.getDrawMode(),_vpMaster.getConfig());
			} catch (ExceptionUnmatchedClosingParentheses e) {
				e.printStackTrace();
			} catch (ExceptionFileFormatOrSyntax e1) {
				_vpMaster.errorDialog(e1);
			} catch (ExceptionDrawingAlgorithm e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_str1Backup = getStruct1();
		}
		return _RNA1;
	}

	public RNA getRNA2() {
		if (!_str2Backup.equals(getStruct2())) {
			try {
				_RNA2.setRNA(getText2(), getStruct2());
				_RNA2.drawRNA(_vpMaster.getDrawMode(),_vpMaster.getConfig());
			} catch (ExceptionUnmatchedClosingParentheses e) {
				e.printStackTrace();
			} catch (ExceptionFileFormatOrSyntax e1) {
				_vpMaster.errorDialog(e1);
			} catch (ExceptionDrawingAlgorithm e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_str2Backup = getStruct2();
		}
		return _RNA2;
	}

    public String getText(int i)
    {
    	return "";
    }

    public String getStruct(int i)
    {
    	if (i==0) 
    		return _struct1.getText();
    	else
    		return _struct2.getText();
    }

    
	public String getText1()
	{
		return _seq1.getText();
	}

	public String getText2()
	{
		return _seq2.getText();
	}

	public String getStruct1() {
		return cleanStruct(_struct1.getText());
	}

	public String getStruct2() {
		return cleanStruct(_struct2.getText());
	}

	private String cleanStruct(String struct) {
		struct = struct.replaceAll("[:-]", "");
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

	public JLabel get_info() {
		return _info;
	}

	public void set_info(JLabel _info) {
		this._info = _info;
	}

	public static void main(String[] args) {
		SuperpositionDemo d = new SuperpositionDemo();
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}

	public void onStructureRedrawn() {
		try{
					Mapping m = Mapping.readMappingFromAlignment(
							this.getStruct((_currentDisplay+1)%2), getStruct((_currentDisplay)%2));
					ControleurInterpolator.moveNearOtherRNA(getRNA((_currentDisplay)%2), getRNA((_currentDisplay+1)%2), m);
					_vpSlave.repaint();
					_vpMaster.repaint();
		}
		catch (MappingException e3) {System.out.println(e3.toString());}
	}

	public void onWarningEmitted(String s) {
		// TODO Auto-generated method stub
		
	}

	public void onLoad(String path) {
		// TODO Auto-generated method stub
		
	}

	public void onLoaded() {
		// TODO Auto-generated method stub
		
	}

	public void onUINewStructure(VARNAConfig v, RNA r) {
		// TODO Auto-generated method stub
		
	}

	public void onZoomLevelChanged(){
		// TODO Auto-generated method stub
		
	}

	public void onTranslationChanged() {
		// TODO Auto-generated method stub
		
	}
}
