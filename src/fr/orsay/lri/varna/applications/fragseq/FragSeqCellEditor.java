package fr.orsay.lri.varna.applications.fragseq;

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

class FragSeqCellEditor extends DefaultTreeCellEditor {  
	
	FragSeqTreeModel _m;
	
	private FragSeqCellRenderer _base;

    public FragSeqCellEditor(JTree tree, DefaultTreeCellRenderer renderer, FragSeqTreeModel m) {
		super(tree, renderer);
		_base= new FragSeqCellRenderer(tree,m);
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