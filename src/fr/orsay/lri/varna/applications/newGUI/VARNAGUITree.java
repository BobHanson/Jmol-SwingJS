package fr.orsay.lri.varna.applications.newGUI;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class VARNAGUITree extends JTree implements MouseListener {
	private Watcher _w;
	
	
	public VARNAGUITree(VARNAGUITreeModel m)
	{
		super(m);
		_w = new Watcher(m ); 
		_w.start();
	}
	
	public DefaultMutableTreeNode getSelectedNode()
	{
		TreePath t = getSelectionPath();
		if (t!= null)
		{
			return (DefaultMutableTreeNode) t.getLastPathComponent();
		}
		return null;
	}

	public void mouseClicked(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		TreePath tp = this.getPathForLocation(x, y);
		if (tp!=null)
		{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) tp.getLastPathComponent();
			
		}
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



}
