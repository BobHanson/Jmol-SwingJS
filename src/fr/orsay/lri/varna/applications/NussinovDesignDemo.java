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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.BevelBorder;

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
import fr.orsay.lri.varna.models.VARNAConfig.BP_STYLE;
import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.interfaces.InterfaceVARNABasesListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;;

public class NussinovDesignDemo extends JFrame implements InterfaceVARNAListener,InterfaceVARNABasesListener, ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -790155708306987257L;

	private static final String SEQUENCE_A =        "AGGCACGUCU";
	private static final String SEQUENCE_B =  "GAGUAGCCUC";
	private static final String SEQUENCE_C = "GCAUAGCUGC";
	private static final String SEQUENCE_INRIA = "CGAUUGCAUCGCAAGU";
	
	private static final String TARGET_STRUCTURE_1 = "(((((((..)))))))";
	private static final String TARGET_STRUCTURE_2 = "(((())))(((())))";
	private static final String TARGET_STRUCTURE_3 = "(.((.((..).)).))";
	private static final String TARGET_STRUCTURE_4 = "((((((())))(((())(()))))))";
	private static final String TARGET_STRUCTURE_5 = "(((())))(((())))(((())))(((())))(((())))(((())))";

	private static final String SEQUENCE_BIG = "AAAACAAAAACACCAUGGUGUUUUCACCCAAUUGGGUGAAAACAGAGAUCUCGAGAUCUCUGUUUUUGUUUU"; 

	private static final String DEFAULT_STRUCTURE = "..........";
	// private static final String DEFAULT_STRUCTURE1 = "((((....))))";
	// private static final String DEFAULT_STRUCTURE2 =
	// "((((..(((....)))..))))";

	private VARNAPanel _vpMaster;
	private VARNAPanel _vpTarget;

	private InfoPanel _infos = new InfoPanel();
	private JPanel _tools = new JPanel();
	private JPanel _input = new JPanel();

	private JPanel _seqPanel = new JPanel();
	private JPanel _structPanel = new JPanel();
	private JLabel _actions = new JLabel();
	private JComboBox _struct = new JComboBox();
	private JLabel _seq1 = new JLabel();
	private JLabel _structLabel = new JLabel("Structure Cible");
	private JLabel _seqLabel = new JLabel("Sequence d'ARN");
	private JButton _switchButton = new JButton("Reset");


	private Color _backgroundColor = Color.white;

	private Color _okColor = Color.decode("#E33729");
	private Color _koColor = new Color(250,200,200);


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
	
	
	public NussinovDesignDemo() {
		super();
		try {
			_vpMaster = new VARNAPanel(getSeq(), "");
			_vpTarget = new VARNAPanel();
		} catch (ExceptionNonEqualLength e) {
			_vpMaster.errorDialog(e);
		}
		_vpMaster.setPreferredSize(new Dimension(600, 600));
		_vpTarget.setPreferredSize(new Dimension(600, 600));
		RNAPanelDemoInit();
	}
	
	static Font labelsFont = Font.decode("Dialog-BOLD-25");

	private void RNAPanelDemoInit() {
		int marginTools = 250;
		Font textFieldsFont = Font.decode("MonoSpaced-BOLD-16");
		
		

		_seq1.setFont(textFieldsFont.deriveFont(25f));
		

		setBackground(_backgroundColor);

		
		_structLabel.setFont(labelsFont.deriveFont(Font.PLAIN));

		
		String[] secstr = {TARGET_STRUCTURE_1,TARGET_STRUCTURE_2,TARGET_STRUCTURE_3,TARGET_STRUCTURE_4,TARGET_STRUCTURE_5};
		DefaultComboBoxModel cm = new DefaultComboBoxModel(secstr); 
		_struct.setModel(cm);
		_struct.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(e.getActionCommand());
				setTarget(((JComboBox)e.getSource()).getSelectedItem().toString());
			}
		});
		_struct.setFont(textFieldsFont);
		_struct.setEnabled(true);
		_struct.setEditable(true);


		_switchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setTarget(_struct.getSelectedItem().toString());		
			}
		});

		_seqPanel.setLayout(new BorderLayout());
		_seqPanel.add(_seqLabel, BorderLayout.WEST);
		_seqPanel.add(_seq1, BorderLayout.CENTER);

		_seqPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		_structLabel.setPreferredSize(new Dimension(marginTools, 15));
		_structLabel.setHorizontalTextPosition(JLabel.LEFT);
		_structPanel.setLayout(new BorderLayout());
		_structPanel.add(_structLabel, BorderLayout.WEST);
		_structPanel.add(_struct, BorderLayout.CENTER);

		_input.setLayout(new GridLayout(0, 1));
		_input.add(_structPanel);
		
		JPanel goPanel = new JPanel();
		goPanel.setLayout(new BorderLayout());

		_infos.setFont(labelsFont);


		formatLabel(_seqLabel);
		formatLabel(_seq1);

		
		_tools.setLayout(new BorderLayout());
		//_tools.add(_infos, BorderLayout.NORTH);
		_tools.add(_input, BorderLayout.CENTER);
		_tools.add(_actions, BorderLayout.SOUTH);
		_tools.add(goPanel, BorderLayout.EAST);
		
		_tools.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		//goPanel.add(_goButton, BorderLayout.CENTER);
		goPanel.add(_switchButton, BorderLayout.CENTER);

		getContentPane().setLayout(new BorderLayout());
		JSplitPane VARNAs = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		VARNAs.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		//VARNAs.setLayout(new GridLayout(1,2));
		JPanel mast2 = new JPanel();
		mast2.setLayout(new BorderLayout());
		mast2.add(_seqPanel, BorderLayout.NORTH);
		mast2.add(_infos, BorderLayout.SOUTH);

		JPanel mast = new JPanel();
		mast.setLayout(new BorderLayout());
		mast.add(_vpMaster, BorderLayout.CENTER);
		mast.add(mast2,BorderLayout.SOUTH);
		
		VARNAs.add(mast);
		VARNAs.add(_vpTarget);
		VARNAs.doLayout();
		VARNAs.setDividerSize(5);
		
		getContentPane().add(VARNAs, BorderLayout.CENTER);
		getContentPane().add(_tools, BorderLayout.NORTH);

		setVisible(true);


		_vpMaster.setBackground(_backgroundColor);
		_vpMaster.addVARNAListener(this);
		_vpMaster.setTitle("Meilleur repliement - Séquence courante");
		_vpMaster.setBPStyle(BP_STYLE.SIMPLE);
		_vpMaster.getVARNAUI().UIRadiate();
		_vpMaster.setTitleFontSize(26f);
		_vpMaster.setTitleFontStyle(Font.PLAIN);
		
		_vpTarget.setBackground(Color.decode("#308099"));
		_vpTarget.setModifiable(false);
		_vpTarget.setTitle("Repliement cible");
		_vpTarget.setBPStyle(BP_STYLE.SIMPLE);
		_vpTarget.setBackboneColor(Color.white);
		_vpTarget.setDefaultBPColor(Color.white);
		_vpTarget.setBaseNumbersColor(Color.white);
		_vpTarget.setBaseOutlineColor(Color.white);
		_vpTarget.setTitleColor(Color.white);		
		_vpTarget.setTitleFontSize(26f);

		
		_okColor = Color.decode("#F39126");
		_koColor = new Color(250,200,200);
		
		_seqPanel.setBackground(Color.decode("#E33729"));
		_infos.setBackground(Color.decode("#E33729"));


		
		_vpMaster.addVARNABasesListener(this);
		setTitle("Fête de la science 2015 - Inria AMIB - Design d'ARN");
		
		setTarget(secstr[0]);
		
	}

	private synchronized void showSolution()
	{
		ArrayList<String> sols = getStructs();
		_infos.setInfo(sols, count(getSeq()));
		if ((sols.size()==1)&&(sols.get(0).equals(_struct.getSelectedItem().toString())))
		{
			/*JOptionPane.showMessageDialog(null, 
					"Vous avez trouvé une séquence pour cette structure !!!\n Saurez vous faire le design de molécules plus complexes ?",
					"Félicitations !", 
					JOptionPane.INFORMATION_MESSAGE);*/
			
		}
		else
		{
			this._vpMaster.setTitle("Meilleur repliement - Séquence courante");
		}
	}
	

	public void setTarget(String target)
	{
		try {
			_vpTarget.drawRNA(String.format("%"+target.length()+"s", ""),target);
			_vpTarget.setBaseNumbersColor(Color.white);
			_vpTarget.setBaseOutlineColor(Color.white);
			//_vpTarget.toggleDrawOutlineBases();
			createDummySeq();
			showSolution();
			onStructureRedrawn();
		} catch (ExceptionNonEqualLength e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void createDummySeq()
	{
		RNA r = _vpTarget.getRNA();
		String seq = new String();
		for (int i=0;i<r.getSize();i++)
		{
			seq += 'A';
		}
		try {
		RNA rn = new RNA();
			rn.setRNA(seq,r.getStructDBN());
		for(ModeleBP mbp: r.getAllBPs())
		{
			rn.getBaseAt(mbp.getIndex5()).setContent("A"); 
			rn.getBaseAt(mbp.getIndex3()).setContent("U"); 
		}
		_vpMaster.drawRNA(rn);
		_vpMaster.repaint();
		_seq1.setText(_vpMaster.getRNA().getSeq());
		} catch (ExceptionUnmatchedClosingParentheses e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionFileFormatOrSyntax e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


 	public String getSeq()
	{
		return (""+_seq1.getText()).toUpperCase();
	}

 	
 	private boolean canBasePairAll(char a, char b)
 	{
		return true;
 	}

 	private boolean canBasePairBasic(char a, char b)
 	{
 		if ((a=='G')&&(b=='C'))
 			return true;
 		if ((a=='C')&&(b=='G'))
 			return true;
 		if ((a=='U')&&(b=='A'))
 			return true;
 		if ((a=='A')&&(b=='U'))
 			return true;
 		return false;
 	}

 	private double basePairScoreBasic(char a, char b)
 	{
 		if ((a=='G')&&(b=='C'))
 			return 1.0;
 		if ((a=='C')&&(b=='G'))
 			return 1.0;
 		if ((a=='U')&&(b=='A'))
 			return 1.0;
 		if ((a=='A')&&(b=='U'))
 			return 1.0;
 		return Double.NEGATIVE_INFINITY;
 	}

 	
 	private boolean canBasePairNussinov(char a, char b)
 	{
 		if ((a=='G')&&(b=='C'))
 			return true;
 		if ((a=='C')&&(b=='G'))
 			return true;
 		if ((a=='U')&&(b=='A'))
 			return true;
 		if ((a=='A')&&(b=='U'))
 			return true;
 		if ((a=='U')&&(b=='G'))
 			return true;
 		if ((a=='G')&&(b=='U'))
 			return true;
 		return false;
 	}

 	private double basePairScoreNussinov(char a, char b)
 	{
 		if ((a=='G')&&(b=='C'))
 			return 3.0;
 		if ((a=='C')&&(b=='G'))
 			return 3.0;
 		if ((a=='U')&&(b=='A'))
 			return 2.0;
 		if ((a=='A')&&(b=='U'))
 			return 2.0;
 		if ((a=='U')&&(b=='G'))
 			return 1.0;
 		if ((a=='G')&&(b=='U'))
 			return 1.0;
 		return Double.NEGATIVE_INFINITY;
 	}

 	private boolean canBasePairINRIA(char a, char b)
 	{
 		if ((a=='U')&&(b=='A'))
 			return true;
 		if ((a=='A')&&(b=='U'))
 			return true;
 		if ((a=='G')&&(b=='C'))
 			return true;
 		if ((a=='C')&&(b=='G'))
 			return true;

 		if ((a=='A')&&(b=='G'))
 			return true;
 		if ((a=='G')&&(b=='A'))
 			return true;
 		if ((a=='U')&&(b=='C'))
 			return true;
 		if ((a=='C')&&(b=='U'))
 			return true;
 		if ((a=='A')&&(b=='A'))
 			return true;
 		if ((a=='U')&&(b=='U'))
 			return true;

 		if ((a=='U')&&(b=='G'))
 			return true;
 		if ((a=='G')&&(b=='U'))
 			return true;
 		if ((a=='A')&&(b=='C'))
 			return true;
 		if ((a=='C')&&(b=='A'))
 			return true;
 		return false;
 	}

 	private double basePairScoreINRIA(char a, char b)
 	{
 		if ((a=='U')&&(b=='A'))
 			return 3;
 		if ((a=='A')&&(b=='U'))
 			return 3;
 		if ((a=='G')&&(b=='C'))
 			return 3;
 		if ((a=='C')&&(b=='G'))
 			return 3;

 		if ((a=='A')&&(b=='G'))
 			return 2;
 		if ((a=='G')&&(b=='A'))
 			return 2;
 		if ((a=='U')&&(b=='C'))
 			return 2;
 		if ((a=='C')&&(b=='U'))
 			return 2;
 		if ((a=='A')&&(b=='A'))
 			return 2;
 		if ((a=='U')&&(b=='U'))
 			return 2;

 		if ((a=='U')&&(b=='G'))
 			return 1;
 		if ((a=='G')&&(b=='U'))
 			return 1;
 		if ((a=='A')&&(b=='C'))
 			return 1;
 		if ((a=='C')&&(b=='A'))
 			return 1;
 		return Double.NEGATIVE_INFINITY;
 	}
 	
 	private boolean canBasePair(char a, char b)
 	{
 		return canBasePairBasic(a,b);
 		//return canBasePairNussinov(a,b);
 		//return canBasePairINRIA(a,b);
 	}
 	
 	private double basePairScore(char a, char b)
 	{
 		return basePairScoreBasic(a,b);
 		//return basePairScoreNussinov(a,b);
 		//return basePairScoreINRIA(a,b);
 	}
 	
 	public double[][] fillMatrix(String seq)
 	{
		int n = seq.length();
		double[][] tab = new double[n][n];
		for(int m=1;m<=n;m++)
		{
			for(int i=0;i<n-m+1;i++)
			{
				int j = i+m-1;
				tab[i][j] = 0;
				if (i<j)
				{ 
					tab[i][j] = Math.max(tab[i][j], tab[i+1][j]); 
					for (int k=i+1;k<=j;k++)
					{
						if (canBasePair(seq.charAt(i),seq.charAt(k)))
						{
							double fact1 = 0;
							if (k>i+1)
							{
								fact1 = tab[i+1][k-1];
							}
							double fact2 = 0;
							if (k<j)
							{
								fact2 = tab[k+1][j];
							}
							tab[i][j] = Math.max(tab[i][j],basePairScore(seq.charAt(i),seq.charAt(k))+fact1+fact2);
						} 
					}
				}
			}			
		}
 		return tab;
 	}

 	public static ArrayList<Double> combine(double bonus, ArrayList<Double> part1, ArrayList<Double> part2)
 	{
 		ArrayList<Double> base = new ArrayList<Double>();
 		for(double d1: part1)
 		{
 	 		for(double d2: part2)
 	 		{
 	 			base.add(bonus+d1+d2);
 	 		}
 		}
 		return base;
 	}

 	public static ArrayList<Double> selectBests(ArrayList<Double> base)
 	{
 		ArrayList<Double> result = new ArrayList<Double>();
 		double best = Double.NEGATIVE_INFINITY;
 		for(double val: base)
 		{
 			best = Math.max(val, best);
 		}
 		for(double val: base)
 		{
 			if (val == best)
 				result.add(val);
 		}
 		return result;
 	}

 	
 	private ArrayList<String> backtrack(double[][] tab, String seq)
 	{
 		return backtrack(tab,seq, 0, seq.length()-1);
 	}

 	private ArrayList<String> backtrack(double[][] tab, String seq, int i, int j)
 	{
 		ArrayList<String> result = new ArrayList<String>();
 		if (i<j)
		{ 
 			ArrayList<Integer> indices = new ArrayList<Integer>();
 			indices.add(-1);
			for (int k=i+1;k<=j;k++)
			{
				indices.add(k);
			}
			for (int k : indices)
 			{
 				if (k==-1)
 				{
 					if (tab[i][j] == tab[i+1][j])
 					{
 						for (String s:backtrack(tab, seq, i+1,j))
 						{
 							result.add("."+s);
 						}
 					}
 				}
 				else
 				{
 					if (canBasePair(seq.charAt(i),seq.charAt(k)))
 					{
 						double fact1 = 0;
 						if (k>i+1)
 						{
 							fact1 = tab[i+1][k-1];
 						}
 						double fact2 = 0;
 						if (k<j)
 						{
 							fact2 = tab[k+1][j];
 						}
 						if (tab[i][j]==basePairScore(seq.charAt(i),seq.charAt(k))+fact1+fact2)
 						{ 
 	 						for (String s1:backtrack(tab, seq, i+1,k-1))
 	 						{
 	 	 						for (String s2:backtrack(tab, seq, k+1,j))
 	 	 						{
 	 	 							result.add("("+s1+")"+s2);
 	 	 						}
 	 						}
 						}
 					}  					
 				} 				
			}
		}
		else if  (i==j)
		{
			result.add(".");
		}
		else 
		{
			result.add("");
		}
 		return result;
 	}
 	
 	public BigInteger count(String seq)
 	{
		int n = seq.length();
		
		BigInteger[][] tab = new BigInteger[n][n];
		for(int m=1;m<=n;m++)
		{
			for(int i=0;i<n-m+1;i++)
			{
				int j = i+m-1;
				tab[i][j] = BigInteger.ZERO;
				if (i<j)
				{ 
					tab[i][j] = tab[i][j].add(tab[i+1][j]); 
					for (int k=i+1;k<=j;k++)
					{
						if (canBasePair(seq.charAt(i),seq.charAt(k)))
						{
							BigInteger fact1 = BigInteger.ONE;
							if (k>i+1)
							{
								fact1 = tab[i+1][k-1];
							}
							BigInteger fact2 = BigInteger.ONE;
							if (k<j)
							{
								fact2 = tab[k+1][j];
							}
							tab[i][j] = tab[i][j].add(fact1.multiply(fact2));
						} 
					}
				}
				else
				{
					tab[i][j] = BigInteger.ONE;
				}
			}			
		}
 		return tab[0][n-1];
 	}
 	
 	private String _cache = "";
 	ArrayList<String> _cacheStructs = new ArrayList<String>(); 
 	
	public ArrayList<String> getStructs() {
		String seq = getSeq();
		seq = seq.toUpperCase();
		if (!_cache.equals(seq))
		{
			double[][] mfe = fillMatrix(seq);
			_cacheStructs = backtrack(mfe,seq);
			_cache = seq;
		}
		return _cacheStructs;
	}

	public VARNAPanel get_varnaPanel() {
		return _vpMaster;
	}

	public void set_varnaPanel(VARNAPanel surface) {
		_vpMaster = surface;
	}


	public JLabel get_info() {
		return _actions;
	}

	public void set_info(JLabel _info) {
		this._actions = _info;
	}

	public static void main(String[] args) {
		NussinovDesignDemo d = new NussinovDesignDemo();
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}

	public void onStructureRedrawn() {
		_vpMaster.repaint();
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

	static final String[] _bases =     {"A","C","G","U"};
	static final String[] _basesComp = {"U","G","C","A"};
	
	public void onBaseClicked(ModeleBase mb, MouseEvent e) {
		int index = -1;
		for(int i =0;i<_bases.length;i++)
		{
			if (mb.getContent().equalsIgnoreCase(_bases[i]))
			{
				index = i;
			}
		}
		index = (index+1)%_bases.length;
		mb.setContent(_bases[index].toUpperCase());
		ArrayList<ModeleBase> partners =_vpTarget.getRNA().getAllPartners(mb.getIndex());
		if (partners.size()!=0)
		{
			ModeleBase mbPartner = _vpMaster.getRNA().getBaseAt(partners.get(0).getIndex());
			mbPartner.setContent(_basesComp[index].toUpperCase());
		}
		_vpMaster.repaint();
		_seq1.setText(_vpMaster.getRNA().getSeq());
		new Temporizer(_vpMaster.getRNA().getSeq()).start();
	}
	
	private class Temporizer extends Thread{
		String _seq;
		public Temporizer(String seq)
		{
			_seq = seq;
		}
		public void run() {
            try {
				this.sleep(1000);
				if (_vpMaster.getRNA().getSeq().equalsIgnoreCase(_seq))
				{
					showSolution();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}
	public void onZoomLevelChanged() {
		// TODO Auto-generated method stub
		
	}

	public void onTranslationChanged() {
		// TODO Auto-generated method stub
		
	}

	public void itemStateChanged(ItemEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println();
	}

	
	public static void formatLabel(JLabel j)
	{
		j.setHorizontalTextPosition(JLabel.LEFT);
		j.setPreferredSize(new Dimension(NussinovDemo.marginTools, 25));
		j.setFont(labelsFont);
		j.setForeground(Color.white);
	}
	
	public static void formatLabel(JTextArea j)
	{
		j.setPreferredSize(new Dimension(NussinovDemo.marginTools, 25));
		j.setFont(labelsFont);
		j.setForeground(Color.white);
	}

	public class InfoPanel extends JPanel
	{
		ArrayList<String> _sols = new ArrayList<String>();
		BigInteger _nbFolds = BigInteger.ZERO;
		JLabel _text = new JLabel("");
		JLabel _subopts = new JLabel("");
		JPanel _suboptBrowser = new JPanel();
		JPanel _suboptCount = new JPanel();
		int _selectedIndex = 0;
		JButton next = new JButton(">");
		JButton previous = new JButton("<");
		
		InfoPanel()
		{
			this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setLayout(new BorderLayout());
			add(_suboptBrowser,BorderLayout.SOUTH);
			add(_suboptCount,BorderLayout.NORTH);
			
			next.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (_sols.size()>0)
					{
						setSelectedIndex((_selectedIndex+1)%_sols.size());
					}
				}				
			});

			previous.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (_sols.size()>0)
					{
						setSelectedIndex((_selectedIndex+_sols.size()-1)%_sols.size());
					}
				}				
			});
			next.setEnabled(false);
			previous.setEnabled(false);

			JLabel nbLab = new JLabel("#Repliements");
			NussinovDesignDemo.formatLabel(nbLab);
			

			JLabel cooptlab = new JLabel("#Co-optimaux");
			NussinovDesignDemo.formatLabel(cooptlab);
			
			NussinovDesignDemo.formatLabel(_text);
			NussinovDesignDemo.formatLabel(_subopts);

			
			_suboptCount.setLayout(new BorderLayout());
			_suboptCount.add(nbLab,BorderLayout.WEST);
			_suboptCount.add(_text,BorderLayout.CENTER);
			_suboptCount.setBackground(Color.decode("#E33729"));

			
			JPanel commands = new JPanel();
			commands.add(previous);
			commands.add(next);
			commands.setBackground(Color.decode("#E33729"));
			
			JPanel jp = new JPanel();
			jp.setLayout(new BorderLayout());
			jp.add(_subopts,BorderLayout.WEST);
			jp.add(commands,BorderLayout.CENTER);
			jp.setBackground(Color.decode("#E33729"));

			_suboptBrowser.setLayout(new BorderLayout());
			_suboptBrowser.add(cooptlab,BorderLayout.WEST);
			_suboptBrowser.add(jp,BorderLayout.CENTER);
			_suboptBrowser.setBackground(Color.decode("#E33729"));
			
		}
			
				
		/*public void setSelectedIndex(int i)
		{
			_selectedIndex = i;
			RNA rfolded = new RNA();
			try {
				rfolded.setRNA(getSeq(), _sols.get(i));
				rfolded.drawRNARadiate(_vpMaster.getConfig());
		        _vpMaster.showRNAInterpolated(rfolded);
			} catch (ExceptionUnmatchedClosingParentheses e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExceptionFileFormatOrSyntax e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//_struct.setText(_sols.get(i));
			formatDescription();
		}*/
		
		public void setSelectedIndex(int i)
		{
			_selectedIndex = i;
			RNA rfolded = new RNA();
			try {
				rfolded.setRNA(getSeq(), _sols.get(i));
				RNA target = _vpTarget.getRNA();
				for(ModeleBase mb: rfolded.get_listeBases())
				{
					ModeleBase mbref = target.getBaseAt(mb.getIndex());
					if (mb.getElementStructure()==mbref.getElementStructure())
					{
						mb.getStyleBase().setBaseInnerColor(_okColor);
						mb.getStyleBase().setBaseNameColor(Color.white);
					}
				}				
				for(ModeleBase mb: target.get_listeBases())
				{
					ModeleBase mbref = rfolded.getBaseAt(mb.getIndex());
					if (mb.getElementStructure()==mbref.getElementStructure())
					{
						mb.getStyleBase().setBaseInnerColor(_okColor);
					}
					else
					{
						mb.getStyleBase().setBaseInnerColor(Color.white);
					}
				}				
				rfolded.drawRNARadiate(_vpMaster.getConfig());
				if ((_sols.size()==1)&& (target.getStructDBN().equals(_sols.get(0))))					
					rfolded.setName("Félicitations !");
				else
					rfolded.setName("Repliement stable - "+(i+1)+"/"+_sols.size());
		        _vpMaster.showRNAInterpolated(rfolded);
		        _vpTarget.repaint();
			} catch (ExceptionUnmatchedClosingParentheses e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExceptionFileFormatOrSyntax e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//_struct.setSelectedItem(_sols.get(i));
			formatDescription();
		}
		
//		public void setFont(Font f)
//		{
//			super.setFont(f);
//			if(_text!=null)
//			{
//				_text.setFont(f);
//				_text.setOpaque(false);
//			}
//			if(_subopts!=null)
//			{
//			_subopts.setFont(f);
//			_subopts.setOpaque(false);
//			}
//		}
		public void setInfo(ArrayList<String> sols, BigInteger nbFolds)
		{
			_sols = sols;
			_nbFolds = nbFolds;
			formatDescription();
			setSelectedIndex(0);
		}
		
		private void formatDescription()
		{
			_text.setText(""+_nbFolds);
			_subopts.setText(""+_sols.size());
			next.setEnabled(_sols.size()>1);
			previous.setEnabled(_sols.size()>1);
			
		}
		
	}

}
