package fr.orsay.lri.varna;
import java.awt.Color;
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
import java.awt.Dimension;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JFrame;

import fr.orsay.lri.varna.controlers.ControleurScriptParser;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.models.VARNAConfigLoader;

/**
 * Adapted from VARNA by Bob Hanson for Jmol integration in Java and JavaScript. 
 * 
 * 
 *  Switched from JApplet to JPanel
 */
public class VARNA extends JApplet implements InterfaceParameterLoader,DropTargetListener {
	ArrayList<VARNAPanel> _vpl = null;
  private JFrame frame;
  private Map<String, String> htParams = new HashMap<>();
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
    this(null);
  }
  
	public VARNA(JFrame frame) {
	  if (frame == null)
	    return;
	  this.frame = frame;
    setSize(600,600);
	  setPreferredSize(new Dimension(600,600));
	  init();
	  frame.add(this);
	  frame.pack();
	  frame.setVisible(true);
	}

  @Override
  public void init() {
    try {
      VARNAConfigLoader VARNAcfg = new VARNAConfigLoader(this);

      _vpl = VARNAcfg.createVARNAPanels();
      for (int i = 0; i < _vpl.size(); i++) {
        new DropTarget(_vpl.get(i), this);
      }
      setLayout(new GridLayout(VARNAcfg.getNbColumns(), VARNAcfg.getNbRows()));
      for (int i = 0; i < _vpl.size(); i++) {
        VARNAPanel vp = _vpl.get(i);
        if (vp.getWidth() == 0) {
          vp.setPreferredSize(new Dimension(600, 600));
          vp.setSize(600, 600);
        }
        add(vp);
      }
    } catch (ExceptionParameterError e) {
      VARNAPanel.errorDialogStatic(e, this);
    }

  }

	@Override
  public void start() {
		//setVisible(true);
		//repaint();
		//getContentPane().setVisible(true);		
		//getContentPane().repaint();		
	}
	
	public void update() {
		System.out.println("update");
	}
	
	@Override
  public String getParameterValue(String key, String def) {
	  String p = htParams .get(key);
	  return (p == null ? def : p);
	}

	@Override
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
			int n = l.get(i).intValue();
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
				_vpl.get(0).setRNA(seq, str);
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

	@Override
  public void dragEnter(DropTargetDragEvent arg0) {
	}

	@Override
  public void dragExit(DropTargetEvent arg0) {
	}

	@Override
  public void dragOver(DropTargetDragEvent arg0) {
	}

	@Override
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
	    	  List<?> list = (List<?>) tr.getTransferData(flavors[i]);
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

	@Override
  public void dropActionChanged(DropTargetDragEvent arg0) {
	}

	public static void main(String[] args) {
	  JFrame frame = new JFrame("VARNA");
	  frame.setSize(800,400);
    frame = new JFrame("VARNA");
	  VARNA v = new VARNA(frame);
	  v.setBackground(Color.BLUE);
	  System.out.println(v._vpl);	  
	  frame.pack();
    frame.setVisible(true);
	  System.out.println(v.isVisible());
    System.out.println(v.getSize());
	}

  public void close() {
    if (frame != null)
      frame.setVisible(false);
    frame = null;
  }

  public JFrame getFrame() {
    return frame;
  }
	
}

