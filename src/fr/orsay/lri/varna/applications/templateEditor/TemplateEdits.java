package fr.orsay.lri.varna.applications.templateEditor;

import java.awt.geom.Point2D;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

public class TemplateEdits {
	public static final double MAX_DISTANCE= 15.0;
		
	public static  class ElementAddTemplateEdit extends AbstractUndoableEdit
	{
		private GraphicalTemplateElement _h;
		private TemplatePanel _p;
		public ElementAddTemplateEdit(GraphicalTemplateElement h,TemplatePanel p)
		{
			_h = h;
			_p = p;
		}
		public void undo() throws CannotUndoException {
			_p.removeElement(_h);
			_p.repaint();			
		}
		public void redo() throws CannotRedoException {
			_p.addElement(_h);
			_p.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Graphical element added"; }
	};
	public static  class ElementRemoveTemplateEdit extends AbstractUndoableEdit
	{
		private GraphicalTemplateElement _h;
		private TemplatePanel _p;
		public ElementRemoveTemplateEdit(GraphicalTemplateElement h,TemplatePanel p)
		{
			_h = h;
			_p = p;
		}
		public void undo() throws CannotUndoException {
			_p.addElement(_h);
			_p.repaint();			
		}
		public void redo() throws CannotRedoException {
			_p.removeElement(_h);
			_p.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Graphical element removed"; }
	};
	public static  class ElementAttachTemplateEdit extends AbstractUndoableEdit
	{
		Connection _c;
		private TemplatePanel _p;
		public ElementAttachTemplateEdit(Connection c,
				TemplatePanel p)
		{
			_c = c;
			_p = p;
		}
		public void undo() throws CannotUndoException {
			_p.removeConnection(_c);
			_p.repaint();			
		}
		public void redo() throws CannotRedoException {
			_c = _p.addConnection(_c._h1,_c._edge1,_c._h2,_c._edge2);
			_p.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Graphical elements attached"; }
	};
	public static  class ElementDetachTemplateEdit extends AbstractUndoableEdit
	{
		Connection _c;
		private TemplatePanel _p;
		public ElementDetachTemplateEdit(Connection c,
				TemplatePanel p)
		{
			_c = c;
			_p = p;
		}
		public void undo() throws CannotUndoException {
			_c = _p.addConnection(_c._h1,_c._edge1,_c._h2,_c._edge2);
			_p.repaint();			
		}
		public void redo() throws CannotRedoException {
			_p.removeConnection(_c);
			_p.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Graphical elements detached"; }
	};
	
	public static  class ElementEdgeMoveTemplateEdit extends AbstractUndoableEdit
	{
		private GraphicalTemplateElement _ur;
		GraphicalTemplateElement.RelativePosition _edge;
		private double _ox; 
		private double _oy;
		private double _nx; 
		private double _ny;
		private TemplatePanel _p;
		public ElementEdgeMoveTemplateEdit(GraphicalTemplateElement ur, GraphicalTemplateElement.RelativePosition edge, double nx, double ny, TemplatePanel p)
		{
			_ur = ur;
			_edge = edge;
			_ox = ur.getEdgePosition(edge).x;
			_oy = ur.getEdgePosition(edge).y;
			_nx = nx;
			_ny = ny;
			_p = p;
		}
		public void undo() throws CannotUndoException {
			_ur.setEdgePosition(_edge,new Point2D.Double(_ox,_oy));
			_p.repaint();			
		}
		public void redo() throws CannotRedoException {
			_ur.setEdgePosition(_edge,new Point2D.Double(_nx,_ny));
			_p.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Edge moved "+_edge; }
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit instanceof ElementEdgeMoveTemplateEdit)
			{
				ElementEdgeMoveTemplateEdit e = (ElementEdgeMoveTemplateEdit) anEdit;
				if (e._edge==_edge)
				{
					Point2D.Double po1 = new Point2D.Double(_ox,_oy);
					Point2D.Double pn1 = new Point2D.Double(_nx,_ny);
					Point2D.Double po2 = new Point2D.Double(e._ox,e._oy);
					Point2D.Double pn2 = new Point2D.Double(e._nx,e._ny);
					if ((_ur==e._ur)&&(pn1.equals(po2))&&(po1.distance(pn2)<MAX_DISTANCE))
					{
						_nx = e._nx;
						_ny = e._ny;
						return true;
					}
				}
			}
			return false;
		}
	};
	public static  class HelixFlipTemplateEdit extends AbstractUndoableEdit
	{
		private Helix _h;
		private TemplatePanel _p;
		public HelixFlipTemplateEdit(Helix h, TemplatePanel p)
		{
			_h = h;
			_p = p;
		}
		public void undo() throws CannotUndoException {
			_h.toggleFlipped();
			_p.repaint();			
		}
		public void redo() throws CannotRedoException {
			_h.toggleFlipped();
			_p.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Helix flipped "; }
	};
	
}
