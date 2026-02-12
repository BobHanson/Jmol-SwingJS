package fr.orsay.lri.varna.models.rna;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import fr.orsay.lri.varna.VARNAPanel;

public class VARNASecDraw {
	public static VARNAPanel _vp = null;

	
	public abstract class Portion
	{
		public abstract ArrayList<Integer> getBaseList();
		public abstract int getNumBases();
		public abstract GeneralPath getOutline(RNA r);
		
	};
	
	public class UnpairedPortion extends Portion
	{
		int _pos;
		int _len;
		public UnpairedPortion(int pos, int len)
		{
			_len = len;
			_pos = pos;
		}
		@Override
		public ArrayList<Integer> getBaseList() {
			// TODO Auto-generated method stub
			return null;
		}		
		public String toString()
		{
			return "U["+_pos+","+(_pos+_len-1)+"]";
		}

		public int getNumBases() {
			return _len;
		}

		
		public GeneralPath getOutline(RNA r) {
			
			GeneralPath gp = new GeneralPath();
			ArrayList<ModeleBase> l = r.get_listeBases();
			Point2D.Double p0 = l.get(_pos).getCoords();
			gp.moveTo((float)p0.x, (float)p0.y);
			for (int i=1;i<_len;i++)
			{
				Point2D.Double p = l.get(_pos+i).getCoords();
				gp.lineTo((float)p.x, (float)p.y);
			}
			return gp;
		}
	};
	
	public class PairedPortion extends Portion
	{
		int _pos1;
		int _pos2;
		int _len;
		RNATree _r;
		
		public PairedPortion(int pos1,int pos2, int len, RNATree r)
		{
			_pos1 = pos1;
			_pos2 = pos2;
			_len = len;
			_r =r;
		}

		@Override
		public ArrayList<Integer> getBaseList() {
			// TODO Auto-generated method stub
			return null;
		}
		
		public String toString()
		{
			return "H["+_pos1+","+(_pos1+_len-1)+"]["+(_pos2-_len+1)+","+(_pos2)+"]\n"+_r.toString();
		}

		@Override
		public int getNumBases() {
			return 2*_len;
		}

		public GeneralPath getLocalOutline(RNA r) {
			GeneralPath gp = new GeneralPath();
			if (_len>0)
			{
				ArrayList<ModeleBase> l = r.get_listeBases();
				Point2D.Double p1 = l.get(_pos1).getCoords();
				Point2D.Double p2 = l.get(_pos1+_len-1).getCoords();
				Point2D.Double p3 = l.get(_pos2-_len+1).getCoords();
				Point2D.Double p4 = l.get(_pos2).getCoords();
				gp.moveTo((float)p1.x, (float)p1.y);
				gp.lineTo((float)p2.x, (float)p2.y);
				gp.lineTo((float)p3.x, (float)p3.y);
				gp.lineTo((float)p4.x, (float)p4.y);
			}
			return gp;
		}


		public GeneralPath getOutline(RNA r) {
			return getOutline(r,false);
		}

		public GeneralPath getOutline(RNA r, boolean local) {
			ArrayList<ModeleBase> l = r.get_listeBases();
			Point2D.Double p1 = l.get(_pos1).getCoords();
			Point2D.Double p2 = l.get(_pos1+_len-1).getCoords();
			Point2D.Double p3 = l.get(_pos2-_len+1).getCoords();
			Point2D.Double p4 = l.get(_pos2).getCoords();
			GeneralPath gp = new GeneralPath();
			gp.moveTo((float)p1.x, (float)p1.y);
			gp.lineTo((float)p2.x, (float)p2.y);
			if (!local)
				gp.append(_r.getOutline(r), true);
			gp.lineTo((float)p3.x, (float)p3.y);
			gp.lineTo((float)p4.x, (float)p4.y);
			
			return gp;
			
		}
	};
	public 	int   _depth = 0;

	
	public class RNATree
	{
		ArrayList<Portion> _portions = new ArrayList<Portion>();
		int _numPairedPortions=0;
		public RNATree()
		{
			
		}
		
		
		public void addPortion(Portion p)
		{
			_portions.add(p);
			if (p instanceof PairedPortion)
			{
				_numPairedPortions++;
			}
		}

		public int getNumPortions()
		{
			return _portions.size();
		}

		public Portion getPortion(int i)
		{
			return _portions.get(i);
		}
		
		public String toString()
		{
			String result = "";
			_depth++;
			for (int i=0;i<_portions.size();i++ )
			{
				result += String.format("%1$#" + _depth + "s", ' ');
				result += _portions.get(i).toString();
				if (i<_portions.size()-1)
				  result += "\n";
			}
			_depth--;
			return result;
		}

		public GeneralPath getOutline(RNA r) {
			GeneralPath result = new GeneralPath();
			for (int i=0;i<_portions.size();i++)
			{
				result.append(_portions.get(i).getOutline(r),true);
			}
			return result;
		}
	};
	
	
	private void buildTree(int i, int j, RNATree parent,  RNA r) 
	{
		//LinkedList<BuildTreeArgs> s = new LinkedList<BuildTreeArgs>();
		//s.add(new BuildTreeArgs(xi, xj, xparent,xr));
		//while(s.size()!=0)
		//{
			
			//BuildTreeArgs a = s.removeLast();
			if (i >= j) {
				parent.addPortion(new UnpairedPortion(i,j-i+1));
			}
			// BasePaired
			if (r.get_listeBases().get(i).getElementStructure() == j) 
			{
				int i1 = i;
				int j1 = j;
				boolean over = false;
				while( (i+1<r.get_listeBases().size())	&&  (j-1>=0)&&  (i+1<=j-1) && !over)
				{
					if (r.get_listeBases().get(i).getElementStructure() != j)
					{ over = true; }
					else
					{ i++;j--; }
				}
				int i2 = i;
				int j2 = j;
				RNATree t = new RNATree();
				if (i<j-1)
				  buildTree(i2,j2,t,r);
				PairedPortion p = new PairedPortion(i1,j1,i2-i1,t);
				parent.addPortion(p);
			} else 
			{
				int k = i;
				int l;
				int start = k;
				int len = 0;
				while (k <= j) {
					l = r.get_listeBases().get(k).getElementStructure();
					if (l != -1) 
					{
						if (len>0)
						{ parent.addPortion(new UnpairedPortion(start,len)); }
						buildTree(k, l, parent,  r);
						k = l + 1;
						start = k;
						len = 0;
					} else {
						len++;
						k++;
					}
				}
				if (len>0)
				{
					parent.addPortion(new UnpairedPortion(start,len));
				}
			}
		}

	/*
	public void drawTree(double x0, double y0, RNATree t, double dir, RNA r, double straightness)
	{
		boolean collision = true;
		double x=x0;
		double y=y0;
		double multRadius = 1.0;
		double initCirc = r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE;
		for (int i=0;i<t.getNumPortions();i++ )
		{
			Portion p = t.getPortion(i);
			if (p instanceof PairedPortion)
			{
				initCirc += (r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE);				
			}
			else
			{
				initCirc += r.LOOP_DISTANCE*(p.getNumBases());
			}
		}
		while(collision)
		{
				double totCirc = r.BASE_PAIR_DISTANCE+straightness*r.LOOP_DISTANCE;
				for (int i=0;i<t.getNumPortions();i++ )
				{
					Portion p = t.getPortion(i);
					if (p instanceof PairedPortion)
					{
						totCirc += (r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE);				
					}
					else
					{
						double mod = 1.0;
						if ((i==0) || (i==t.getNumPortions()-1))
							mod = straightness;
						  totCirc += mod*r.LOOP_DISTANCE*(p.getNumBases());
					}
				}
				double radius = multRadius*initCirc/(2.0*Math.PI);
			//radius = 2.0;
			x = x0+radius*Math.cos(dir+Math.PI);
			y = y0+radius*Math.sin(dir+Math.PI);
			dir += 2.0*Math.PI;
			double angleIncr = (2.0*Math.PI)/(totCirc);
			double circ = r.BASE_PAIR_DISTANCE/2.0+straightness*r.LOOP_DISTANCE;
			double ndir;
			ArrayList<GeneralPath> shapes = new ArrayList<GeneralPath>(); 
			for (int i=0;i<t.getNumPortions();i++ )
			{
				Portion p = t.getPortion(i);
				if (p instanceof PairedPortion)
				{
					circ+=r.BASE_PAIR_DISTANCE/2.0;
					ndir = dir + circ*angleIncr;
					PairedPortion pp = (PairedPortion) p; 
					for(int j=0;j<pp._len;j++)
					{
						int i1 = pp._pos1+j;
						int i2 = pp._pos2-j;
						double vx = Math.cos(ndir);
						double vy = Math.sin(ndir);
						double nx = x+((j*r.LOOP_DISTANCE+radius)*vx);
						double ny = y+((j*r.LOOP_DISTANCE+radius)*vy);
						r.get_listeBases().get(i1).set_coords(new Point2D.Double(nx+r.BASE_PAIR_DISTANCE*vy/ 2.0,ny-r.BASE_PAIR_DISTANCE*vx/ 2.0));
						r.get_listeBases().get(i2).set_coords(new Point2D.Double(nx-r.BASE_PAIR_DISTANCE*vy/ 2.0,ny+r.BASE_PAIR_DISTANCE*vx/ 2.0));
					}
					double nx = x+(((pp._len-1)*r.LOOP_DISTANCE+radius)*Math.cos(ndir));
					double ny = y+(((pp._len-1)*r.LOOP_DISTANCE+radius)*Math.sin(ndir));
					drawTree(nx, ny, pp._r, ndir+Math.PI, r, straightness);
					shapes.add(pp.getOutline(r));
					circ += r.LOOP_DISTANCE + r.BASE_PAIR_DISTANCE/2.0;
				}
				else if (p instanceof UnpairedPortion)
				{
					UnpairedPortion up = (UnpairedPortion) p;
					double mod = 1.0;
					if ((i==0) || (i==t.getNumPortions()-1))
						  mod = straightness;
					for(int j=0;j<up._len;j++)
					{
						ndir = dir + circ*angleIncr;
						double vx = Math.cos(ndir);
						double vy = Math.sin(ndir);
						double nx = x+((radius)*vx);
						double ny = y+((radius)*vy);
						r.get_listeBases().get(up._pos+j).set_coords(new Point2D.Double(nx,ny));
						circ += mod*r.LOOP_DISTANCE;
					}
				}
				//System.out.println(dir);
			}
			circ += r.BASE_PAIR_DISTANCE/2.0;
			System.out.println(""+circ+"/"+totCirc);
			if(shapes.size()>0)
			{
				collision = false;
				for (int i=0;(i<shapes.size()) && !collision;i++)
				{	
					Area a1 = new Area(shapes.get(i));
					for (int j=i+1;(j<shapes.size())&& !collision;j++)
					{	
						Area a2 = new Area(shapes.get(j));
						a1.intersect(a2);
						if (!a1.isEmpty())
						{
							collision = true;
						}
					}
				}
				if (collision)
				{
					straightness *= 1.2;
					multRadius *= 1.5;
				}
					
			}
			else 
			{
				collision = false;
			}
		}
	}
	*/
	
	public int[] nextPlacement(int[] p) throws Exception
	{
		 //System.out.println(Arrays.toString(p));
		int i=p.length-1;
		int prev = MAX_NUM_DIR;
		boolean stop = false;
		while((i>=0) && !stop)
		{
			if (p[i]==prev-1)
			{ 
				prev = p[i]; 
				i--; 
			}
			else
			{ stop = true; }
		}
		if (i<0)
			throw new Exception("No more placement available"); 
		p[i]++;
		i++;
		while(i<p.length)
		{
			p[i] = p[i-1]+1;
			i++;
		}
		 //System.out.println(Arrays.toString(p));		
		return p;
	}
	
	
	public void drawTree(double x0, double y0, RNATree t, double dir, RNA r) throws Exception
	{
		boolean collision = true;
		double x=x0;
		double y=y0;
		int numHelices = 0;
		int nbHel = 1;
		int nbUn = 0;
		double totCirc = r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE;
		for (int i=0;i<t.getNumPortions();i++ )
		{
			Portion p = t.getPortion(i);
			if (p instanceof PairedPortion)
			{
				totCirc += (r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE);
				nbHel += 1;
			}
			else
			{
				totCirc += r.LOOP_DISTANCE*(p.getNumBases());
				nbUn += p.getNumBases()+1;
			}
		}
		double radius = r.determineRadius(nbHel, nbUn, totCirc/(2.0*Math.PI));

		for (int i=0;i<t.getNumPortions();i++ )
		{
			Portion p = t.getPortion(i);
			if (p instanceof PairedPortion)
			{
				numHelices++;				
			}
		}
		int[] placement = new int[numHelices];
		double inc = ((double)MAX_NUM_DIR)/((double)numHelices+1);
		double val = inc;
		for (int i=0;i<numHelices;i++ )
		{
			placement[i] = (int)Math.round(val);
			val += inc;
		}
		System.out.println();
		double angleIncr = 2.0*Math.PI/(double)MAX_NUM_DIR;
		while(collision)
		{
			x = x0+radius*Math.cos(dir+Math.PI);
			y = y0+radius*Math.sin(dir+Math.PI);
			ArrayList<GeneralPath> shapes = new ArrayList<GeneralPath>();
			int curH = 0;
			for (int i=0;i<t.getNumPortions();i++ )
			{
				Portion p = t.getPortion(i);
				if (p instanceof PairedPortion)
				{
					double ndir = dir + placement[curH]*angleIncr;
					curH++;
					PairedPortion pp = (PairedPortion) p; 
					for(int j=0;j<pp._len;j++)
					{
						int i1 = pp._pos1+j;
						int i2 = pp._pos2-j;
						double vx = Math.cos(ndir);
						double vy = Math.sin(ndir);
						double nx = x+(((j)*r.LOOP_DISTANCE+radius)*vx);
						double ny = y+(((j)*r.LOOP_DISTANCE+radius)*vy);
						r.get_listeBases().get(i1).setCoords(new Point2D.Double(nx+r.BASE_PAIR_DISTANCE*vy/ 2.0,ny-r.BASE_PAIR_DISTANCE*vx/ 2.0));
						r.get_listeBases().get(i2).setCoords(new Point2D.Double(nx-r.BASE_PAIR_DISTANCE*vy/ 2.0,ny+r.BASE_PAIR_DISTANCE*vx/ 2.0));
					}
					double nx = x+(((pp._len-1)*r.LOOP_DISTANCE+radius)*Math.cos(ndir));
					double ny = y+(((pp._len-1)*r.LOOP_DISTANCE+radius)*Math.sin(ndir));
					drawTree(nx, ny, pp._r, ndir+Math.PI, r);
					shapes.add(pp.getOutline(r));
				}
				else if (p instanceof UnpairedPortion)
				{
					UnpairedPortion up = (UnpairedPortion) p;
					for(int j=0;j<up._len;j++)
					{
						/*ndir = dir + circ*angleIncr;
							double vx = Math.cos(ndir);
							double vy = Math.sin(ndir);
							double nx = x+((radius)*vx);
							double ny = y+((radius)*vy);
							r.get_listeBases().get(up._pos+j).set_coords(new Point2D.Double(nx,ny));
							circ += mod*r.LOOP_DISTANCE;*/
						r.get_listeBases().get(up._pos+j).setCoords(new Point2D.Double(x,y));
					}
				}
				//System.out.println(dir);
			}
			if(shapes.size()>0)
			{
				collision = false;
				for (int i=0;(i<shapes.size()) && !collision;i++)
				{	
					Area a1 = new Area(shapes.get(i));
					for (int j=i+1;(j<shapes.size())&& !collision;j++)
					{	
						Area a2 = new Area(shapes.get(j));
						a1.intersect(a2);
						if (!a1.isEmpty())
						{
							collision = true;
						}
					}
				}
				if (collision)
				{
					placement = nextPlacement(placement);
				}
					
			}
			else 
			{
				collision = false;
			}
		}
	}


	private class HelixEmbedding
	{
		private GeneralPath _clip;
		Point2D.Double _support;
		ArrayList<HelixEmbedding> _children = new ArrayList<HelixEmbedding>();
		ArrayList<Integer> _indices = new ArrayList<Integer>();
		PairedPortion _p;
		RNA _r;
		HelixEmbedding _parent;
		
		public HelixEmbedding(Point2D.Double support, PairedPortion p, RNA r, HelixEmbedding parent)
		{
			_support = support;
			_clip = p.getLocalOutline(r);
			_p = p;
			_r = r;
			_parent = parent;
		}
		
		public void addHelixEmbedding(HelixEmbedding h, int index)
		{
			_children.add(h);
			_indices.add(index);
		}
		
		public GeneralPath getShape()
		{
			return _clip;
		}
		
		
		public int chooseNextMove()
		{
			int i = _parent._children.indexOf(this);
			int min;
			int max;
			if (_parent._children.size()<VARNASecDraw.MAX_NUM_DIR-1)
			{
				if (_parent._children.size()==1)
				{ min=1;max=VARNASecDraw.MAX_NUM_DIR-1;	}
				else 
				{
					if (i==0)
					{ min = 1; }
					else
					{ min = _parent._indices.get(i-1)+1;}
					if (i==_parent._children.size()-1)
					{ max = VARNASecDraw.MAX_NUM_DIR-1; }
					else
					{ max = _parent._indices.get(i+1)-1;}
				}
				int prevIndex = _parent._indices.get(i);
				int newIndex = min+_rnd.nextInt(max+1-min);
				double rot = ((double)(newIndex-prevIndex)*Math.PI*2.0)/MAX_NUM_DIR;
				_parent._indices.set(i, newIndex);
				rotate(rot);
				return newIndex-prevIndex;
			}
			return 0;
		}
		
		public void cancelMove(int delta)
		{
			int i = _parent._children.indexOf(this);
			int prevIndex = _parent._indices.get(i);
			double rot = ((double)(-delta)*Math.PI*2.0)/MAX_NUM_DIR;
			_parent._indices.set(i, prevIndex-delta);
			rotate(rot);
		}
		
		public void rotate(double angle)
		{
			transform(AffineTransform.getRotateInstance(angle, _support.x, _support.y));
		}
		
		private void transform(AffineTransform a)
		{
			_clip.transform(a);
			Point2D p = a.transform(_support, null);
			_support.setLocation(p.getX(), p.getY());
			for (int i=0;i<_children.size();i++)
			{
				_children.get(i).transform(a);
			}
		}
		
		public void reflectCoordinates()
		{
			ArrayList<ModeleBase> mbl = _r.get_listeBases();

			if (_p._len>0)
			{
				PathIterator pi = _clip.getPathIterator(AffineTransform.getRotateInstance(0.0));
				ArrayList<Point2D.Double> p = new ArrayList<Point2D.Double>(); 
				while(!pi.isDone())
				{
					double[] args = new double[6];
					int type= pi.currentSegment(args);
					if ((type == PathIterator.SEG_MOVETO)  || (type == PathIterator.SEG_LINETO))
					{

						Point2D.Double np = new Point2D.Double(args[0],args[1]); 
						p.add(np);
						System.out.println(Arrays.toString(args));
					}
					pi.next();
				}
				if (p.size()<4)
				{ return; }
				
				Point2D.Double startLeft = p.get(0);
				Point2D.Double endLeft = p.get(1);
				Point2D.Double endRight = p.get(2);
				Point2D.Double startRight = p.get(3);
				
				double d = startLeft.distance(endLeft);
				double vx = endLeft.x-startLeft.x;
				double vy = endLeft.y-startLeft.y;
				double interval = 0.0;
				if (_p._len>1)
				{
					vx/=d;
					vy/=d;
					interval = d/((double)_p._len-1);
					System.out.println("DELTA: "+interval+" "+_r.LOOP_DISTANCE);
				}
				for (int n=0;n<_p._len;n++)
				{
					int i = _p._pos1 + n;
					int j = _p._pos2 - n;
					ModeleBase mbLeft = mbl.get(i);
					mbLeft.setCoords(new Point2D.Double(startLeft.x+n*vx*interval, startLeft.y+n*vy*interval));
					ModeleBase mbRight = mbl.get(j);
					mbRight.setCoords(new Point2D.Double(startRight.x+n*vx*interval, startRight.y+n*vy*interval));
				}
			}
			for (int i=0;i<_children.size();i++)
			{
				_children.get(i).reflectCoordinates();
			}
			if (_children.size()>0)
			{
				Point2D.Double center = _children.get(0)._support;
				for (int i=0;i<_p._r.getNumPortions();i++)
				{
					Portion p = _p._r.getPortion(i);
					if (p instanceof UnpairedPortion)
					{
						UnpairedPortion up = (UnpairedPortion) p;
						for (int j=0;j<up._len;j++)
						{
							int n = up._pos + j;
							ModeleBase mbLeft = mbl.get(n);
							mbLeft.setCoords(center);
						}
					}
				}	
			}
			else
			{
				placeTerminalLoop(mbl,_r);
			}
		}
		
		private void placeTerminalLoop(ArrayList<ModeleBase> mbl, RNA r)
		{
			if ((_children.size()==0)&&(_p._r.getNumPortions()==1))
			{
				Portion p = _p._r.getPortion(0);
				if (p instanceof UnpairedPortion)
				{
					UnpairedPortion up = (UnpairedPortion) p;
					double rad = determineRadius(1,up.getNumBases(),_r);
					int a = _p._pos1+_p._len-1;
					int b = _p._pos2-(_p._len-1);
					ModeleBase mbLeft = mbl.get(a);
					ModeleBase mbRight = mbl.get(b);
					Point2D.Double pl = mbLeft.getCoords();
					Point2D.Double pr = mbRight.getCoords();
					Point2D.Double pm = new Point2D.Double((pl.x+pr.x)/2.0,(pl.y+pr.y)/2.0);
					double vx = (pl.x-pr.x)/pl.distance(pr);
					double vy = (pl.y-pr.y)/pl.distance(pr);
					double vnx = -vy, vny = vx;
					Point2D.Double pc = new Point2D.Double(pm.x+rad*vnx,pm.y+rad*vny);
					double circ = r.LOOP_DISTANCE*(1.0+up.getNumBases())+r.BASE_PAIR_DISTANCE; 
					double incrLoop = Math.PI*2.0*r.LOOP_DISTANCE/circ;  
					double angle = Math.PI*2.0*r.BASE_PAIR_DISTANCE/(2.0*circ);
					for (int j=0;j<up._len;j++)
					{
						int n = up._pos + j;
						ModeleBase mb = mbl.get(n);
						angle += incrLoop;
						double dx = -Math.cos(angle)*vnx+Math.sin(angle)*vx;
						double dy = -Math.cos(angle)*vny+Math.sin(angle)*vy;
//						Point2D.Double pf = new Point2D.Double(pc.x,pc.y);
						Point2D.Double pf = new Point2D.Double(pc.x+rad*dx,pc.y+rad*dy);
						mb.setCoords(pf);
					}
					
				}
			}
		}
		
		
		
		public String toString()
		{
			return "Emb.Hel.: "+_p.toString();
		}
	}
	
	public double determineRadius(int numHelices, int numUnpaired, RNA r)
	{
		double circ = numHelices*r.BASE_PAIR_DISTANCE+(numHelices+numUnpaired)*r.LOOP_DISTANCE;
		return circ/(2.0*Math.PI);
	}
	
	
	public void predrawTree(double x0, double y0, RNATree t, double dir, RNA r, HelixEmbedding parent, ArrayList<HelixEmbedding> all) throws Exception
	{
		double x=x0;
		double y=y0;
		int numHelices = 0;
		int numUBases = 0;
		double totCirc = r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE;
		for (int i=0;i<t.getNumPortions();i++ )
		{
			Portion p = t.getPortion(i);
			if (p instanceof PairedPortion)
			{
				totCirc += (r.BASE_PAIR_DISTANCE+r.LOOP_DISTANCE);
				numHelices++;	
			}
			else
			{
				  totCirc += r.LOOP_DISTANCE*(p.getNumBases());
				  numUBases += p.getNumBases();
			}
		}
		double radius = determineRadius(numHelices+1,numUBases,r);

		int[] placement = new int[numHelices];
		double inc = ((double)MAX_NUM_DIR)/((double)numHelices+1);
		double val = inc;
		for (int i=0;i<numHelices;i++ )
		{
			placement[i] = (int)Math.round(val);
			val += inc;
		}
		double angleIncr = 2.0*Math.PI/(double)MAX_NUM_DIR;
			x = x0+radius*Math.cos(dir+Math.PI);
			y = y0+radius*Math.sin(dir+Math.PI);
			int curH = 0;
			for (int i=0;i<t.getNumPortions();i++ )
			{
				Portion p = t.getPortion(i);
				if (p instanceof PairedPortion)
				{
					double ndir = dir + placement[curH]*angleIncr;
					PairedPortion pp = (PairedPortion) p; 
					for(int j=0;j<pp._len;j++)
					{
						int i1 = pp._pos1+j;
						int i2 = pp._pos2-j;
						double vx = Math.cos(ndir);
						double vy = Math.sin(ndir);
						double nx = x+(((j)*r.LOOP_DISTANCE+radius)*vx);
						double ny = y+(((j)*r.LOOP_DISTANCE+radius)*vy);
						r.get_listeBases().get(i1).setCoords(new Point2D.Double(nx+r.BASE_PAIR_DISTANCE*vy/ 2.0,ny-r.BASE_PAIR_DISTANCE*vx/ 2.0));
						r.get_listeBases().get(i2).setCoords(new Point2D.Double(nx-r.BASE_PAIR_DISTANCE*vy/ 2.0,ny+r.BASE_PAIR_DISTANCE*vx/ 2.0));
					}
					double nx = x+(((pp._len-1)*r.LOOP_DISTANCE+radius)*Math.cos(ndir));
					double ny = y+(((pp._len-1)*r.LOOP_DISTANCE+radius)*Math.sin(ndir));
					HelixEmbedding nh = new HelixEmbedding(new Point2D.Double(x,y),pp,r,parent);
					parent.addHelixEmbedding(nh,placement[curH]);
					all.add(nh);
					predrawTree(nx, ny, pp._r, ndir+Math.PI, r, nh, all);
					curH++;
				}
				else if (p instanceof UnpairedPortion)
				{
					UnpairedPortion up = (UnpairedPortion) p;
					for(int j=0;j<up._len;j++)
					{
						r.get_listeBases().get(up._pos+j).setCoords(new Point2D.Double(x,y));
					}
				}
				//System.out.println(dir);
			}
	}
	
	
	public static Random _rnd = new Random();
	
	private static int MAX_NUM_DIR = 8;

	public RNATree drawRNA(double dirAngle, RNA r) {
		RNATree t = new RNATree();
		buildTree(0, r.get_listeBases().size() - 1, t,  r );
		System.out.println(t);
		ArrayList<HelixEmbedding> all = new ArrayList<HelixEmbedding>();
		HelixEmbedding root = null;
		try {
			root = new HelixEmbedding(new Point2D.Double(0.0,0.0),new PairedPortion(0,0,0,t),r,null); 
			predrawTree(0,0,t,0.0,r,root,all);
			int steps=1000;
			double prevbadness = Double.MAX_VALUE;
			while((steps>0)&&(prevbadness>0))
			{

				// Generating new structure
				HelixEmbedding chosen = all.get(_rnd.nextInt(all.size()));
				int delta =  chosen.chooseNextMove();
				// Draw current
				if (_vp!=null)
					{ 
						GeneralPath p = new GeneralPath();
						for (int i=0;i<all.size();i++)
						{ p.append(all.get(i).getShape(),false); }
						r._debugShape = p;
						_vp.paintImmediately(0, 0, _vp.getWidth(), _vp.getHeight());	
					}				

				//Evaluating solution
				double badness = 0.0;
				for (int i=0;i<all.size();i++)
				{ 
					Shape s1 = all.get(i).getShape();
					for (int j=i+1;j<all.size();j++)
					{ 
						Shape s2 = all.get(j).getShape();
						Area a = new Area(s1);
						a.intersect(new Area(s2));
						if (!a.isEmpty())
						{
							badness ++;
						}
					}
				}

				if (badness-prevbadness>0)
				{
					chosen.cancelMove(delta);
				}
				else
				{
					prevbadness = badness;
				}

				System.out.println(badness);

				steps--;
			}
			if (root!=null)
			{ root.reflectCoordinates(); }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return t;
	};
	
}