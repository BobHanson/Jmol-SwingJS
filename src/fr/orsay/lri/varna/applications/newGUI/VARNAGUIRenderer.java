package fr.orsay.lri.varna.applications.newGUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

class VARNAGUIRenderer extends DefaultTreeCellRenderer {  
	   
	JTree _j;
	VARNAGUITreeModel _m;
	
	private static VARNAGUIRenderer _default = new VARNAGUIRenderer(null,null);

	

	public VARNAGUIRenderer (JTree j, VARNAGUITreeModel m)
	{
		_j = j;
		_m = m;
	}
	
	public JComponent baseElements(JTree tree,VARNAGUITreeModel m,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row, boolean hasFocus)
	{
		JLabel initValue = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        initValue.setBorder(null);
        if (hasFocus)
        { 
        	//renderer.setBackground(Color.blue);            
        	result.setBorder(BorderFactory.createLineBorder(Color.blue));
            result.setBackground(UIManager.getColor("Tree.selectionBackground"));
            initValue.setOpaque(true);
        }
        else
        {
        	result.setBackground(Color.white);
        	result.setBorder(BorderFactory.createLineBorder(initValue.getBackground()));
            
        }
        DefaultMutableTreeNode t = (DefaultMutableTreeNode)value;
        Object o = t.getUserObject();
        if (!( o instanceof VARNAGUIModel))    
        {  
            if (expanded)  
            {  
            	initValue.setIcon(_default.getOpenIcon());  
            }  
            else  
            {  
            	initValue.setIcon(_default.getClosedIcon());  
            }  
            result.add(initValue,BorderLayout.WEST);
        	JButton del = new JButton("X");
        	del.addActionListener(new FolderCloses((String)o,tree,m));
            result.add(del,BorderLayout.EAST);
        }  
        else  
        {  
        	VARNAGUIModel mod = (VARNAGUIModel) o;
        	initValue.setIcon(_default.getLeafIcon());  
            result.add(initValue,BorderLayout.WEST);
            if (mod.hasChanged())
            {
        	  JButton refresh = new JButton("Refresh");
              result.add(refresh,BorderLayout.EAST);
            }
        }
        return result;
	}
	
    public Component getDefaultTreeCellRendererComponent(JTree tree,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row, boolean hasFocus)  
    {
      return super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
    }

	public Component getTreeCellRendererComponent(JTree tree,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row, boolean hasFocus)  
    {
    	
        return baseElements(tree,_m,value,sel,expanded,leaf,row,hasFocus);  
    }  
    public Dimension getPreferredSize(int row) {
        Dimension size = super.getPreferredSize();
        size.width = _j.getWidth();
        System.out.println(size);
        return size;
    }


//    @Override
//    public void setBounds(final int x, final int y, final int width, final int height) {
//    super.setBounds(x, y, Math.min(_j.getWidth()-x, width), height);
//    }
    
    
    public class FolderCloses implements ActionListener{
    	String _path;
    	JComponent _p;
    	VARNAGUITreeModel _m;
    	
    	public FolderCloses(String path, JComponent p, VARNAGUITreeModel m)
    	{
    		_path = path;
    		_p = p;
    		_m = m;
    	}
		public void actionPerformed(ActionEvent e) {
			if (JOptionPane.showConfirmDialog(_p, "This folder will cease to be watched. Confirm?", "Closing folder", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
			{
				_m.removeFolder(_path);
				System.out.println(_j);
				_j.updateUI();
			}
		}
		    
    }
}  