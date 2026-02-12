package fr.orsay.lri.varna.applications.templateEditor;

import java.awt.geom.Point2D;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import fr.orsay.lri.varna.applications.templateEditor.GraphicalTemplateElement.RelativePosition;
import fr.orsay.lri.varna.models.templates.RNATemplate;

public class TemplateEditorPanelUI {

	private UndoableEditSupport _undoableEditSupport;
	private TemplatePanel _tp;
	private Tool selectedTool = Tool.CREATE_HELIX;
	


	public enum Tool {
		SELECT, CREATE_HELIX, CREATE_UNPAIRED
	}

	public TemplateEditorPanelUI(TemplatePanel tp)
	{
		_tp = tp;
		 _undoableEditSupport = new UndoableEditSupport(tp);
	}
	
	public Tool getSelectedTool() {
		return selectedTool;
	}

	public void setSelectedTool(Tool selectedTool) {
		this.selectedTool = selectedTool;
	}
	
	/* Generic undoable event firing for edge movement */
	public void undoableEdgeMove(GraphicalTemplateElement h, GraphicalTemplateElement.RelativePosition edge,double nx, double ny)
	{
		_undoableEditSupport.postEdit(new TemplateEdits.ElementEdgeMoveTemplateEdit( h,edge,nx,ny,_tp));
		h.setEdgePosition(edge, new Point2D.Double(nx,ny));
		_tp.repaint();		
	}
	
	public void setEdge5UI(GraphicalTemplateElement h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_CONNECT_START5, nx,ny); }	
	public void setEdge3UI(UnpairedRegion h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_CONNECT_END3, nx,ny); }	
	public void setEdge5TangentUI(UnpairedRegion h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_EDIT_TANGENT_5, nx,ny); }
	public void setEdge3TangentUI(UnpairedRegion h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_EDIT_TANGENT_3, nx,ny); }	
	public void moveUnpairedUI(UnpairedRegion u, double nx, double ny)
	{ undoableEdgeMove(u,GraphicalTemplateElement.RelativePosition.RP_INNER_MOVE, nx,ny); }	
	public void moveHelixUI(Helix h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_INNER_MOVE, nx,ny); }	
	public void setHelixPosUI(Helix h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_EDIT_START, nx,ny); }	
	public void setHelixExtentUI(Helix h, double nx, double ny)
	{ undoableEdgeMove(h,GraphicalTemplateElement.RelativePosition.RP_EDIT_END, nx,ny); }	
	

	public void addElementUI(GraphicalTemplateElement h)
	{
		_undoableEditSupport.postEdit(new TemplateEdits.ElementAddTemplateEdit( h,_tp));
		_tp.addElement(h);
	}

	public void removeElementUI(GraphicalTemplateElement h)
	{
		_undoableEditSupport.postEdit(new TemplateEdits.ElementRemoveTemplateEdit( h,_tp));
		_tp.removeElement(h);
	}
	

	public void addUndoableEditListener(UndoManager manager)
	{
		_undoableEditSupport.addUndoableEditListener(manager);
	}
	
	public void addConnectionUI(GraphicalTemplateElement h1,
			GraphicalTemplateElement.RelativePosition e1,  
			GraphicalTemplateElement h2,
			GraphicalTemplateElement.RelativePosition e2)
	{
		if (GraphicalTemplateElement.canConnect(h1, e1,h2, e2))
		{
		Connection c = _tp.addConnection(h1,e1,h2,e2);
		_undoableEditSupport.postEdit(new TemplateEdits.ElementAttachTemplateEdit(c,_tp));
		}
	}

	public void removeConnectionUI(Connection c)
	{
		_undoableEditSupport.postEdit(new TemplateEdits.ElementDetachTemplateEdit(c,_tp));
		_tp.removeConnection(c);
	}

	public void flipHelixUI(Helix h)
	{
			  _undoableEditSupport.postEdit(new TemplateEdits.HelixFlipTemplateEdit(h,_tp));
			  _tp.flip(h);
			  _tp.repaint();
	}
	
	public RNATemplate getTemplate()
	{
		return _tp.getTemplate();
	}
	
	
}
