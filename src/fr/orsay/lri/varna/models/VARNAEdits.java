package fr.orsay.lri.varna.models;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.applications.templateEditor.GraphicalTemplateElement;
import fr.orsay.lri.varna.applications.templateEditor.TemplatePanel;
import fr.orsay.lri.varna.applications.templateEditor.TemplateEdits.ElementEdgeMoveTemplateEdit;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;

public class VARNAEdits {
	public static final double MAX_DISTANCE= 55.0;
	public static  class BasesShiftEdit extends AbstractUndoableEdit
	{
		private ArrayList<Integer> _indices; 
		private double _dx; 
		private double _dy;
		private VARNAPanel _vp;
		public BasesShiftEdit(ArrayList<Integer> indices, double dx, double dy, VARNAPanel p)
		{
			_indices = indices;
			_dx = dx;
			_dy = dy;
			_vp = p;
		}
		public void undo() throws CannotUndoException {
			for (int index: _indices)
			{
				ModeleBase mb = _vp.getRNA().getBaseAt(index);
			    _vp.getRNA().setCoord(index,new Point2D.Double(mb.getCoords().x-_dx,mb.getCoords().y-_dy));
			    _vp.getRNA().setCenter(index,new Point2D.Double(mb.getCenter().x-_dx,mb.getCenter().y-_dy));
			}
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			for (int index: _indices)
			{
				ModeleBase mb = _vp.getRNA().getBaseAt(index);
			    _vp.getRNA().setCoord(index,new Point2D.Double(mb.getCoords().x+_dx,mb.getCoords().y+_dy));
			    _vp.getRNA().setCenter(index,new Point2D.Double(mb.getCenter().x-_dx,mb.getCenter().y-_dy));
			}
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Base #"+_indices+" shifted"; }
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit instanceof BasesShiftEdit)
			{
				BasesShiftEdit e = (BasesShiftEdit) anEdit;
				if (e._indices.equals(_indices))
				{
					Point2D.Double tot = new Point2D.Double(_dx+e._dx,_dy+e._dy);
					if (tot.distance(0.0, 0.0)<MAX_DISTANCE)
					{
						_dx = _dx+e._dx;
						_dy = _dy+e._dy;
						return true;
					}
				}
			}
			return false;
		}
	};

	public static  class HelixRotateEdit extends AbstractUndoableEdit
	{
		private double _delta; 
		private double _base; 
		private double _pLimL; 
		private double _pLimR; 
		private Point _h; 
		private Point _ml; 
		private VARNAPanel _vp;
		public HelixRotateEdit(double delta, double base, double pLimL, double pLimR, Point h, Point ml, VARNAPanel vp)
		{
			_delta = delta;
			_base = base;
			_pLimL = pLimL;
			_pLimR = pLimR;
			_h = h;
			_ml = ml;
			_vp = vp;
		}
		public void undo() throws CannotUndoException {
			_vp.getVARNAUI().UIRotateEverything(-_delta, _base, _pLimL, _pLimR, _h, _ml);
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getVARNAUI().UIRotateEverything(_delta, _base, _pLimL, _pLimR, _h, _ml);
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Helix #"+_h+" rotated angle:"+_delta; }
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit instanceof HelixRotateEdit)
			{
				HelixRotateEdit e = (HelixRotateEdit) anEdit;
				if (e._h.equals(_h))
				{
					double totAngle = e._delta+_delta;
					while (totAngle>Math.PI)
					{ totAngle -= 2.0*Math.PI; }
					if (Math.abs(totAngle)<Math.PI/8.0)
					{
						_delta=totAngle;
						return true;
					}
				}
			}
			return false;
		}
	};
	
	
	public static  class SingleBaseMoveEdit extends AbstractUndoableEdit
	{
		private int _index; 
		private double _ox; 
		private double _oy;
		private double _nx; 
		private double _ny;
		private VARNAPanel _vp;
		public SingleBaseMoveEdit(int index, double nx, double ny, VARNAPanel p)
		{
			_index = index;
			ModeleBase mb = p.getRNA().getBaseAt(index);
			_ox = mb.getCoords().x;
			_oy = mb.getCoords().y;
			_nx = nx;
			_ny = ny;
			_vp = p;
		}
		public void undo() throws CannotUndoException {
			_vp.getRNA().setCoord(_index,new Point2D.Double(_ox,_oy));
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getRNA().setCoord(_index,new Point2D.Double(_nx,_ny));
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Base #"+_index+" moved"; }
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit instanceof SingleBaseMoveEdit)
			{
				SingleBaseMoveEdit e = (SingleBaseMoveEdit) anEdit;
				if (e._index==_index)
				{
					Point2D.Double po1 = new Point2D.Double(_ox,_oy);
					Point2D.Double pn1 = new Point2D.Double(_nx,_ny);
					Point2D.Double po2 = new Point2D.Double(e._ox,e._oy);
					Point2D.Double pn2 = new Point2D.Double(e._nx,e._ny);
					if ((pn1.equals(po2))&&(po1.distance(pn2)<MAX_DISTANCE))
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

	public static  class HelixFlipEdit extends AbstractUndoableEdit
	{
		private Point _h; 
		private VARNAPanel _vp;
		public HelixFlipEdit(Point h, VARNAPanel vp)
		{
			_h = h;
			_vp = vp;
		}
		public void undo() throws CannotUndoException {
			_vp.getVARNAUI().UIFlipHelix(_h);
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getVARNAUI().UIFlipHelix(_h);
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Helix #"+_h+" flipped";}
		public boolean addEdit(UndoableEdit anEdit)
		{
			return false;
		}
	};

	public static  class AddBPEdit extends AbstractUndoableEdit
	{
		private ModeleBP _msbp; 
		private int _i;
		private int _j;
		private VARNAPanel _vp;
		public AddBPEdit(int i, int j, ModeleBP msbp, VARNAPanel vp)
		{
			_msbp = msbp;
			_i = i;
			_j = j;
			_vp = vp;
		}
		public void undo() throws CannotUndoException {
			_vp.getRNA().removeBP(_msbp);
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getRNA().addBP(_i,_j,_msbp);
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Add BP ("+_i+","+_j+")";}
		public boolean addEdit(UndoableEdit anEdit)
		{
			return false;
		}
	};

	public static  class RemoveBPEdit extends AbstractUndoableEdit
	{
		private ModeleBP _msbp; 
		private int _i;
		private int _j;
		private VARNAPanel _vp;
		public RemoveBPEdit( int i, int j,ModeleBP msbp, VARNAPanel vp)
		{
			_msbp = msbp;
			_i = i;
			_j = j;
			_vp = vp;
		}
		public void undo() throws CannotUndoException {
			_vp.getRNA().addBP(_i,_j,_msbp);
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getRNA().removeBP(_msbp);
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Remove BP ("+_i+","+_j+")";}
		public boolean addEdit(UndoableEdit anEdit)
		{
			return false;
		}
	};

	public static  class RescaleRNAEdit extends AbstractUndoableEdit
	{
		private double _factor;
		private VARNAPanel _vp;
		public RescaleRNAEdit( double angle, VARNAPanel vp)
		{
			_factor = angle;
			_vp = vp;
		}
		public void undo() throws CannotUndoException {
			_vp.getRNA().rescale(1.0/_factor);
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getRNA().rescale(_factor);
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Rescale RNA factor:"+_factor+"";}
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit instanceof RescaleRNAEdit)
			{
				RescaleRNAEdit e = (RescaleRNAEdit) anEdit;
				double cumFact = _factor*e._factor;
				if (cumFact>.7 || cumFact<1.3)
				{
					_factor *= e._factor;
					return true;
				}
			}
			return false;
		}
	};

	public static  class RotateRNAEdit extends AbstractUndoableEdit
	{
		private double _angle;
		private VARNAPanel _vp;
		public RotateRNAEdit( double angle, VARNAPanel vp)
		{
			_angle = angle;
			_vp = vp;
		}
		public void undo() throws CannotUndoException {
			_vp.getRNA().globalRotation(-_angle);
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			_vp.getRNA().globalRotation(_angle);
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Rotate RNA angle:"+_angle+"";}
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (anEdit instanceof RotateRNAEdit)
			{
				RotateRNAEdit e = (RotateRNAEdit) anEdit;
				if (Math.abs(_angle+e._angle)<30)
				{
					_angle += e._angle;
					return true;
				}
			}
			return false;
		}
	};

	public static  class RedrawEdit extends AbstractUndoableEdit
	{
		private int _prevMode;
		private int _newMode;
		private boolean _prevFlat;
		private boolean _newFlat;
		private ArrayList<Point2D.Double> _backupCoords = new ArrayList<Point2D.Double>();
		private ArrayList<Point2D.Double> _backupCenters = new ArrayList<Point2D.Double>();
		private VARNAPanel _vp;
		

		public RedrawEdit(VARNAPanel vp,boolean newFlat)
		{
			this(vp.getRNA().getDrawMode(),vp,newFlat);
		}

		public RedrawEdit(int newMode, VARNAPanel vp)
		{
			this(newMode,vp,vp.getFlatExteriorLoop());
		}
	
		public RedrawEdit(int newMode, VARNAPanel vp, boolean newFlat)
		{
			_vp = vp;
			_newMode = newMode;
			_newFlat = newFlat;
			_prevFlat = _vp.getFlatExteriorLoop();
			for (ModeleBase mb: _vp.getRNA().get_listeBases())
			{
				_backupCoords.add(new Point2D.Double(mb.getCoords().x,mb.getCoords().y));
				_backupCenters.add(new Point2D.Double(mb.getCenter().x,mb.getCenter().y));
			}
			_prevMode = _vp.getDrawMode();
		}
		public void undo() throws CannotUndoException {
			RNA r = _vp.getRNA();
			_vp.setFlatExteriorLoop(_prevFlat);
			r.setDrawMode(_prevMode);
			for (int index =0;index<_vp.getRNA().get_listeBases().size();index++)
			{
			    Point2D.Double oldCoord = _backupCoords.get(index);
			    Point2D.Double oldCenter = _backupCenters.get(index);
			    r.setCoord(index, oldCoord);
			    r.setCenter(index, oldCenter);
			}
			_vp.repaint();			
		}
		public void redo() throws CannotRedoException {
			try {
				_vp.setFlatExteriorLoop(_newFlat);
				_vp.getRNA().drawRNA(_newMode,_vp.getConfig());
			} catch (ExceptionNAViewAlgorithm e) {
				e.printStackTrace();
			}
			_vp.repaint();
		}
		public boolean canUndo() { return true; }
		public boolean canRedo() { return true; }
		public String getPresentationName() { return "Redraw whole RNA";}
		public boolean addEdit(UndoableEdit anEdit)
		{
			return false;
		}
	};

}
