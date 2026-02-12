package fr.orsay.lri.varna.applications.newGUI;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EventObject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

class VARNAGUICellEditor extends DefaultTreeCellEditor {  
	
	VARNAGUITreeModel _m;
	
	private VARNAGUIRenderer _base;

    public VARNAGUICellEditor(JTree tree, DefaultTreeCellRenderer renderer, VARNAGUITreeModel m) {
		super(tree, renderer);
		_base= new VARNAGUIRenderer(tree,m);
		_m=m;
	}
    
    
    public Component getTreeCellEditorComponent(JTree tree,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row)  
    {  
     
    JPanel renderer = (JPanel) _base.baseElements(tree,_m,value,sel,expanded,leaf,row,true);
    return renderer;  
    }

    public boolean isCellEditable(EventObject evt)
    {
        if (evt instanceof MouseEvent) {
            int clickCount;
            // For single-click activation
            clickCount = 1;

            return ((MouseEvent)evt).getClickCount() >= clickCount;
        }
        return true;

    }

    
}  