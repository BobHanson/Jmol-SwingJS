package fr.orsay.lri.varna;
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
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JOptionPane;

import fr.orsay.lri.varna.controlers.ControleurScriptParser;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.RNA;


// @j2s issues -- see README_SWINGJS.txt

public class VARNA extends JApplet implements InterfaceParameterLoader,DropTargetListener {
	ArrayList<VARNAPanel> _vpl = null;
	static{
		/**
		 * 
		 * @j2sNative
		 * 
		 * 
		 * if (!thisApplet.__Info.sequenceDBN) {
		 *  thisApplet.__Info.sequenceDBN = "GGGGCCAAUAUGGCCAUCC";
		 *  thisApplet.__Info.structureDBN = "((((((.....))))..))";
		 *  thisApplet.__Info.title = "Hello RNA world, from SwingJS!";//prompt("Title?","Hello RNA world!");
		 * } 
		 * 
		 * 
		 * 
		 */		
	}

	
//	private static final_long serialVersionUID = -2598221520127067670L;

	public VARNA() {
		super();
	}

	public void init() {
		try {
			VARNAConfigLoader VARNAcfg = new VARNAConfigLoader(this);
			
			try {
				_vpl = VARNAcfg.createVARNAPanels();
				for (int i=0;i<_vpl.size();i++)
				{
				    new DropTarget(_vpl.get(i), this);
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(),
						"VARNA Error", JOptionPane.ERROR_MESSAGE);
			} catch (ExceptionFileFormatOrSyntax e) {
				JOptionPane.showMessageDialog(this, e.getMessage(),
						"VARNA Error", JOptionPane.ERROR_MESSAGE);
			} catch (ExceptionLoadingFailed e) {
				JOptionPane.showMessageDialog(this, e.getMessage(),
						"VARNA Error", JOptionPane.ERROR_MESSAGE);
			}
			setLayout(new GridLayout(VARNAcfg.getNbColumns(), VARNAcfg
					.getNbRows()));
			for (int i = 0; i < _vpl.size(); i++) {
				getContentPane().add(_vpl.get(i));
			}
		} catch (ExceptionParameterError e) {
			VARNAPanel.errorDialogStatic(e, this);
		} catch (ExceptionModeleStyleBaseSyntaxError e) {
			VARNAPanel.errorDialogStatic(e, this);
		} catch (ExceptionNonEqualLength e) {
			VARNAPanel.errorDialogStatic(e, this);
		}

	}

	public void start() {
		//setVisible(true);
		//repaint();
		//getContentPane().setVisible(true);		
		//getContentPane().repaint();		
	}
	
	public void update() {
		System.out.println("update");
	}
	
	public String getParameterValue(String key, String def) {
		if (getParameter(key) == null) {
			return def;
		} else {
			return getParameter(key);
		}
	}

	public String[][] getParameterInfo() {
		return VARNAConfigLoader.getParameterInfo();
	}
	
	public ArrayList<VARNAPanel> getPanels()
	{
		return _vpl;
	}
	
	public String getSelection()
	{
		return getSelection(0);
	}
	
	public String getSelection(int panel)
	{
		String result = "[";
		VARNAPanel v = _vpl.get(panel);
		List<Integer> l = v.getSelectionIndices();
		for(int i=0;i<l.size();i++)
		{
			int n = l.get(i);
			if (i>0)
			{result += ",";}
			result += n;
			
		}
		result += "]";
		return result;
	}

	public void runScript(String script)
	{
		if (_vpl.size()>0)
		{ 
			VARNAPanel _vp = _vpl.get(0);
			try {
				ControleurScriptParser.executeScript(_vp, script);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void setRNA(String seq, String str) 
	{
		if (_vpl.size()>0)
		{ 
			try {
				_vpl.get(0).drawRNA(seq, str);
			} catch (ExceptionNonEqualLength e) {
				e.printStackTrace();
			} 
		}
	}

	public void setSmoothedRNA(String seq, String str) 
	{
		if (_vpl.size()>0)
		{ 
			try {
				  
				  _vpl.get(0).drawRNAInterpolated(seq, str);
				  _vpl.get(0).repaint();
			} catch (ExceptionNonEqualLength e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}

	public void dragEnter(DropTargetDragEvent arg0) {
	}

	public void dragExit(DropTargetEvent arg0) {
	}

	public void dragOver(DropTargetDragEvent arg0) {
	}

	public void drop(DropTargetDropEvent dtde) 
	{
	  try 
	  {
	    Transferable tr = dtde.getTransferable();
	    DataFlavor[] flavors = tr.getTransferDataFlavors();
	    for (int i = 0; i < flavors.length; i++) 
	    {
	      if (flavors[i].isFlavorJavaFileListType()) 
	      {
	    	  dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
	    	  List list = (List) tr.getTransferData(flavors[i]);
	    	  for (int j = 0; j < list.size(); j++) 
	    	  {
	    		  Object o = list.get(j);
	    		  if (dtde.getSource() instanceof DropTarget)
	    		  {
	    			  DropTarget dt = (DropTarget) dtde.getSource();
	    			  Component c = dt.getComponent();
	    			  if (c instanceof VARNAPanel)
	    			  {
	    				  VARNAPanel vp = (VARNAPanel) c;
	    				  // BH -- in JavaScript, the File object has a .bytes 
	    				  // property that we need to maintain.
	    				  //String path = o.toString();
	    				  vp.loadFile((File) o,true);
	    				  //vp.repaint(); BH unnecessary
	    			  }
	    		  }
	    	  }
	    	  dtde.dropComplete(true);
	    	  return;
	      }
	    }
        dtde.rejectDrop();
	 } 
	 catch (Exception e) 
	 {
		 e.printStackTrace();
	     dtde.rejectDrop();
	  }
	}

	public void dropActionChanged(DropTargetDragEvent arg0) {
	}

	
}

