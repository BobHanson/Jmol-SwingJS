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
package fr.orsay.lri.varna.demos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.interfaces.InterfaceVARNABasesListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfig.BP_STYLE;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;;

public class NussinovDemo extends JFrame implements InterfaceVARNAListener,InterfaceVARNABasesListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = -790155708306987257L;

	protected static final String SEQUENCE_DUMMY =        "AAAAAAAAAA";
	protected static final String SEQUENCE_A =        "AGGCACGUCU";
	protected static final String SEQUENCE_B =  "GAGUAGCCUC";
	protected static final String SEQUENCE_C = "GCAUAGCUGC";
	protected static final String SEQUENCE_INRIA = "GAGAAGUACUUGAAAUUGGCCUCCUC";
	

	protected static final String SEQUENCE_BIG = "AAAACAAAAACACCAUGGUGUUUUCACCCAAUUGGGUGAAAACAGAGAUCUCGAGAUCUCUGUUUUUGUUUU"; 

	protected static final String DEFAULT_STRUCTURE = "..........";
	// protected static final String DEFAULT_STRUCTURE1 = "((((....))))";
	// protected static final String DEFAULT_STRUCTURE2 =
	// "((((..(((....)))..))))";

	protected VARNAPanel _vpMaster;

	protected InfoPanel _infos = new InfoPanel();
	protected JPanel _tools = new JPanel();
	protected JPanel _input = new JPanel();

	protected JPanel _seqPanel = new JPanel();
	protected JPanel _structPanel = new JPanel();
	protected JLabel _actions = new JLabel();
	protected JLabel _struct = new JLabel(DEFAULT_STRUCTURE);
	protected JComboBox _seq1 = new JComboBox();
	protected JLabel _structLabel = new JLabel("Structure Secondaire");
	protected JLabel _seqLabel = new JLabel("Sequence d'ARN");
	protected JButton _goButton = new JButton("Repliement");
	protected JButton _switchButton = new JButton("Effacer");


	protected Color _backgroundColor = Color.white;
	
	public static Font textFieldsFont = Font.decode("MonoSpaced-BOLD-16");
	public static Font labelsFont = Font.decode("SansSerif-BOLD-20");
	public static final int marginTools = 250;
	public static String APP_TITLE = "Fête de la science 2015 - Inria AMIB - Repliement d'ARN";
	

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
	
	
	public NussinovDemo() {
		super();
		try {
			_vpMaster = new VARNAPanel(getSeq(), "");
		} catch (ExceptionNonEqualLength e) {
			_vpMaster.errorDialog(e);
		}
		_vpMaster.setPreferredSize(new Dimension(600, 600));
		RNAPanelDemoInit();
	}

	public static void formatLabel(JLabel j)
	{
		j.setHorizontalTextPosition(SwingConstants.LEFT);
		j.setPreferredSize(new Dimension(marginTools, 15));
		j.setFont(labelsFont);
	}
	
	protected void RNAPanelDemoInit() {
		
		

		_seq1.setFont(textFieldsFont);
		String[] seqs = {SEQUENCE_DUMMY,SEQUENCE_INRIA,SEQUENCE_A,SEQUENCE_B,SEQUENCE_C,SEQUENCE_BIG};
		_seq1.setModel(new DefaultComboBoxModel<String>(seqs));
		_seq1.setEditable(true);
		

		setBackground(_backgroundColor);
		_vpMaster.setBackground(_backgroundColor);
		_vpMaster.addVARNAListener(this);
		_vpMaster.setFlatExteriorLoop(true);
		


		formatLabel(_seqLabel);
		formatLabel(_structLabel);

		_goButton.addActionListener(new ActionListener() {
			@Override
      public void actionPerformed(ActionEvent e) {
				showSolution();
				onStructureRedrawn();
			}
		});

		_switchButton.addActionListener(new ActionListener() {
			@Override
      public void actionPerformed(ActionEvent e) {
				try {
							RNA r = new RNA();
							r.setRNA("", "");
							_struct.setText("");
							_vpMaster.setTitle("");
							_vpMaster.showRNA(r);
							onStructureRedrawn();
				} catch (ExceptionUnmatchedClosingParentheses e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				_vpMaster.repaint();
			}
		});

		_seqPanel.setLayout(new BorderLayout());
		_seqPanel.add(_seqLabel, BorderLayout.WEST);
		_seqPanel.add(_seq1, BorderLayout.CENTER);

		_structLabel.setPreferredSize(new Dimension(marginTools, 15));
		_structLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		_struct.setFont(textFieldsFont);
		_structPanel.setLayout(new BorderLayout());
		_structPanel.add(_structLabel, BorderLayout.WEST);
		_structPanel.add(_struct, BorderLayout.CENTER);

		_input.setLayout(new GridLayout(0, 1));
		_input.add(_seqPanel);
		_input.add(_structPanel);

		JPanel goPanel = new JPanel();
		goPanel.setLayout(new BorderLayout());

		_infos.setFont(labelsFont);

		
		_tools.setLayout(new BorderLayout());
		_tools.add(_infos, BorderLayout.SOUTH);
		_tools.add(_input, BorderLayout.CENTER);
		_tools.add(_actions, BorderLayout.NORTH);
		_tools.add(goPanel, BorderLayout.EAST);
		_tools.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		goPanel.add(_goButton, BorderLayout.CENTER);
		goPanel.add(_switchButton, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout());
		JPanel VARNAs = new JPanel();
		VARNAs.setLayout(new GridLayout(1,2));
		VARNAs.add(_vpMaster);
		VARNAs.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		getContentPane().add(VARNAs, BorderLayout.CENTER);
		getContentPane().add(_tools, BorderLayout.SOUTH);

		setVisible(true);

		_vpMaster.getVARNAUI().radiate();
		_vpMaster.setTitleFontSize(26f);
		_vpMaster.setTitleFontStyle(Font.PLAIN);
		_vpMaster.addVARNABasesListener(this);
		_vpMaster.setBackground(Color.decode("#308099"));
		_vpMaster.setModifiable(false);
		_vpMaster.setTitle("Repliement cible");
		_vpMaster.setBPStyle(BP_STYLE.SIMPLE);
		_vpMaster.setBackboneColor(Color.white);
		_vpMaster.setDefaultBPColor(Color.white);
		_vpMaster.setBaseNumbersColor(Color.white);
		_vpMaster.setBaseOutlineColor(Color.white);
		_vpMaster.setTitleColor(Color.white);		
		_vpMaster.setTitleFontSize(26f);

		
		
		this.setTitle(APP_TITLE);
		
		showSolution();
		onStructureRedrawn();
	}

	protected synchronized void showSolution()
	{
		ArrayList<String> sols = getStructs();
		_infos.setInfo(sols, count(getSeq()));
	}
	



 	public String getSeq()
	{
		return (""+_seq1.getSelectedItem()).toUpperCase();
	}

 	
 	protected boolean canBasePairAll(char a, char b)
 	{
		return true;
 	}

 	protected boolean canBasePairBasic(char a, char b)
 	{
 		if ((a=='G')&&(b=='C'))
 			return true;
 		if ((a=='C')&&(b=='G'))
 			return true;
 		if ((a=='U')&&(b=='A'))
 			return true;
 		if ((a=='A')&&(b=='U'))
 			return true;
 		if ((a=='G')&&(b=='U'))
 			return true;
 		if ((a=='U')&&(b=='G'))
 			return true;
 		return false;
 	}

 	protected double basePairScoreBasic(char a, char b)
 	{
 		if ((a=='G')&&(b=='C'))
 			return 1.0;
 		if ((a=='C')&&(b=='G'))
 			return 1.0;
 		if ((a=='U')&&(b=='A'))
 			return 1.0;
 		if ((a=='A')&&(b=='U'))
 			return 1.0;
 		if ((a=='G')&&(b=='U'))
 			return 1.0;
 		if ((a=='U')&&(b=='G'))
 			return 1.0;
 		return Double.NEGATIVE_INFINITY;
 	}

 	
 	protected boolean canBasePairNussinov(char a, char b)
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

 	protected double basePairScoreNussinov(char a, char b)
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

 	protected boolean canBasePairINRIA(char a, char b)
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

 	protected double basePairScoreINRIA(char a, char b)
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
 	
 	protected boolean canBasePair(char a, char b)
 	{
 		return canBasePairBasic(a,b);
 		//return canBasePairNussinov(a,b);
 		//return canBasePairINRIA(a,b);
 	}
 	
 	protected double basePairScore(char a, char b)
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

 	
 	protected ArrayList<String> backtrack(double[][] tab, String seq)
 	{
 		return backtrack(tab,seq, 0, seq.length()-1);
 	}

 	protected ArrayList<String> backtrack(double[][] tab, String seq, int i, int j)
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
 	
 	protected String _cache = "";
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
		NussinovDemo d = new NussinovDemo();
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}

	@Override
  public void onStructureRedrawn() {
		_vpMaster.repaint();
	}

	@Override
  public void onWarningEmitted(String s) {
		
		
	}

	public void onLoad(String path) {
		
		
	}

	public void onLoaded() {
		
		
	}

	@Override
  public void onUINewStructure(VARNAConfig v, RNA r) {
		
		
	}

	static final String[] _bases =     {"A","C","G","U"};
	static final String[] _basesComp = {"U","G","C","A"};
	
	@Override
  public void onBaseClicked(ModeleBase mb, int id) {
	}
	
	@Override
  public void onZoomLevelChanged() {
		
		
	}

	@Override
  public void onZoomTranslationChanged() {
		
		
	}
	protected class InfoPanel extends JPanel
	{
		ArrayList<String> _sols = new ArrayList<String>();
		BigInteger _nbFolds = BigInteger.ZERO;
		JTextArea _text = new JTextArea("");
		JTextArea _subopts = new JTextArea("");
		JPanel _suboptBrowser = new JPanel();
		JPanel _suboptCount = new JPanel();
		int _selectedIndex = 0;
		JButton next = new JButton(">");
		JButton previous = new JButton("<");
		
		InfoPanel()
		{
			setLayout(new BorderLayout());
			add(_suboptBrowser,BorderLayout.SOUTH);
			add(_suboptCount,BorderLayout.NORTH);
			
			next.addActionListener(new ActionListener(){
				@Override
        public void actionPerformed(ActionEvent arg0) {
					if (_sols.size()>0)
					{
						setSelectedIndex((_selectedIndex+1)%_sols.size());
					}
				}				
			});

			previous.addActionListener(new ActionListener(){
				@Override
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
			NussinovDemo.formatLabel(nbLab);

			_suboptCount.setLayout(new BorderLayout());
			_suboptCount.add(nbLab,BorderLayout.WEST);
			_suboptCount.add(_text,BorderLayout.CENTER);

			JLabel cooptlab = new JLabel("#Co-optimaux");
			NussinovDemo.formatLabel(cooptlab);
			
			JPanel commands = new JPanel();
			commands.add(previous);
			commands.add(next);
			
			JPanel jp = new JPanel();
			jp.setLayout(new BorderLayout());
			jp.add(_subopts,BorderLayout.WEST);
			jp.add(commands,BorderLayout.CENTER);

			_suboptBrowser.setLayout(new BorderLayout());
			_suboptBrowser.add(cooptlab,BorderLayout.WEST);
			_suboptBrowser.add(jp,BorderLayout.CENTER);
			
			
		}
			
				
		public void setSelectedIndex(int i)
		{
			_selectedIndex = i;
			RNA rfolded = new RNA();
			try {
				rfolded.setRNA(getSeq(), _sols.get(i));
				rfolded.drawRNARadiate(_vpMaster.getConfig());
				rfolded.setBaseNameColor(Color.white);
				rfolded.setBaseOutlineColor(Color.white);
				rfolded.setBaseNumbersColor(Color.white);
				_vpMaster.setBaseNumbersColor(Color.white);
				_vpMaster.setBaseOutlineColor(Color.white);
				_vpMaster.setFillBases(false);
				_vpMaster.setBaseNameColor(Color.white);
				_vpMaster.showRNAInterpolated(rfolded);		        
				
			} catch (ExceptionUnmatchedClosingParentheses e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_struct.setText(_sols.get(i));
			formatDescription();
		}
		
		@Override
    public void setFont(Font f)
		{
			super.setFont(f);
			if(_text!=null)
			{
				_text.setFont(f);
				_text.setOpaque(false);
			}
			if(_subopts!=null)
			{
			_subopts.setFont(f);
			_subopts.setOpaque(false);
			}
		}
		public void setInfo(ArrayList<String> sols, BigInteger nbFolds)
		{
			_sols = sols;
			_nbFolds = nbFolds;
			formatDescription();
			setSelectedIndex(0);
		}
		
		protected void formatDescription()
		{
			_text.setText(""+_nbFolds);
			_subopts.setText(""+_sols.size());
			next.setEnabled(_sols.size()>1);
			previous.setEnabled(_sols.size()>1);
			
		}
		
	}
	
}
