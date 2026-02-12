package fr.orsay.lri.varna.models.annotations;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurClicMovement;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.models.rna.VARNAPoint;
import fr.orsay.lri.varna.utils.XMLUtils;
import fr.orsay.lri.varna.views.VueHighlightRegionEdit;
import fr.orsay.lri.varna.views.VueUI;

public class HighlightRegionAnnotation implements Serializable {
	
	public static final String HEADER_TEXT = "HighlightRegionAnnotation";

	private static final long serialVersionUID = 7087014168028684775L;
	public static final Color DEFAULT_OUTLINE_COLOR = Color.decode("#6ed86e");
	public static final Color DEFAULT_FILL_COLOR = Color.decode("#bcffdd");
	public static final double DEFAULT_RADIUS = 16.0;
	
	private Color _outlineColor = DEFAULT_OUTLINE_COLOR;
	private Color _fillColor = DEFAULT_FILL_COLOR;
	private double _radius = DEFAULT_RADIUS;
	private ArrayList<ModeleBase> _bases;
	
	public static String XML_ELEMENT_NAME = "region";
	public static String XML_VAR_OUTLINE_NAME = "outline";
	public static String XML_VAR_FILL_NAME = "fill";
	public static String XML_VAR_RADIUS_NAME = "radius";

	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_OUTLINE_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_outlineColor));
		atts.addAttribute("","",XML_VAR_FILL_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_fillColor));
		atts.addAttribute("","",XML_VAR_RADIUS_NAME,"CDATA",""+_radius);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		XMLUtils.toXML(hd, _bases);
		hd.endElement("","",XML_ELEMENT_NAME);
	}

  public HighlightRegionAnnotation(RNA r, int startIndex, int stopIndex)
  {
	  this(r.getBasesBetween(startIndex, stopIndex));
  }

  public HighlightRegionAnnotation()
  {
	  this(new ArrayList<ModeleBase>());
  }

  public HighlightRegionAnnotation(ArrayList<ModeleBase> b)
  {
	  this(b,DEFAULT_FILL_COLOR,DEFAULT_OUTLINE_COLOR,DEFAULT_RADIUS);
  }
 
  
  public HighlightRegionAnnotation(ArrayList<ModeleBase> b,Color fill, Color outline, double radius)
  {
	  _bases = b;
	  _fillColor = fill;
	  _outlineColor = outline;
	  _radius = radius;
  }
  
	public HighlightRegionAnnotation clone()
	{
		return new HighlightRegionAnnotation(_bases,_fillColor,_outlineColor,_radius);
	}
	
  public int getMinIndex()
  {
	  int min = Integer.MAX_VALUE;
	  for (ModeleBase mb : _bases)
	  {
		  min = Math.min(min, mb.getIndex());
	  }
	  return min;
  }

  public int getMaxIndex()
  {
	  int max = Integer.MIN_VALUE;
	  for (ModeleBase mb : _bases)
	  {
		  max = Math.max(max, mb.getIndex());
	  }
	  return max;
  }

  
  public void setOutlineColor(Color c)
  {
	  _outlineColor = c;  
  }

  public ArrayList<ModeleBase> getBases()
  {
	  return _bases;
  }

  public void setBases(ArrayList<ModeleBase> b)
  {
	  _bases = b;
  }
  
  public void setFillColor(Color c)
  {
	  _fillColor = c;  
  }

  public Color getFillColor()
  {
	  return _fillColor;  
  }
 
  public Color getOutlineColor()
  {
	  return _outlineColor;  
  }

  public double getRadius()
  {
	  return _radius;  
  }

  public void setRadius(double v)
  {
	  _radius = v;  
  }
  
  public static final int NUM_STEPS_ROUNDED_CORNERS = 16;
  
  private Point2D.Double symImage(Point2D.Double p, Point2D.Double center)
  {
	  return new Point2D.Double(2.*center.x-p.x, 2.*center.y-p.y);
  }
  
  private LinkedList<Point2D.Double> buildRoundedCorner(Point2D.Double p1, Point2D.Double p2, Point2D.Double anotherPoint)
  {
		LinkedList<Point2D.Double> result = new LinkedList<Point2D.Double>();
		Point2D.Double m = new Point2D.Double((p1.x+p2.x)/2.0,(p1.y+p2.y)/2.0);
		double rad = p1.distance(p2)/2.;
		double angle = Math.atan2(p1.y-m.y, p1.x-m.x);
		
		double incr = Math.PI/((double)NUM_STEPS_ROUNDED_CORNERS+1);
		
		Point2D.Double pdir = new Point2D.Double(m.x+rad*Math.cos(angle+Math.PI/2.),m.y+rad*Math.sin(angle+Math.PI/2.));
		if (pdir.distance(anotherPoint)<p1.distance(anotherPoint))
		{ incr = -incr; }
		
		for(int k=1;k<=NUM_STEPS_ROUNDED_CORNERS;k++)
		{
			double angle2 = angle+k*incr;
			Point2D.Double interForward = new Point2D.Double(m.x+rad*Math.cos(angle2),m.y+rad*Math.sin(angle2));
			result.addLast(interForward);
		}
		return result;
  }
  
  
	public GeneralPath getShape(Point2D.Double[] realCoords,Point2D.Double[] realCenters, double scaleFactor)
	{
		GeneralPath p = new GeneralPath();
		LinkedList<Point2D.Double> pointList = new LinkedList<Point2D.Double>();
		for (int i = 0;i<getBases().size();i++)
		{ 
			int j1 = getBases().get(i).getIndex();
			{
				int j0 = j1-1;
				int j2 = j1+1;
				Point2D.Double p0 = new Point2D.Double(0., 0.);
				Point2D.Double p1 = new Point2D.Double(0., 0.);			
				Point2D.Double p2 = new Point2D.Double(0., 0.);
				if (i==0)
				{
					// Single point
					if (i==getBases().size()-1)
					{
						p1 = realCoords[j1];			
						p0 = new Point2D.Double(p1.x+scaleFactor *getRadius(), p1.y);
						p2 = new Point2D.Double(p1.x-scaleFactor *getRadius(), p1.y);
					}
					else
					{
						p1 = realCoords[j1];			
						p2 = realCoords[j2];
						p0 = symImage(p2, p1);
					}
				}
				else if (i==getBases().size()-1)
				{
					p0 = realCoords[j0];
					p1 = realCoords[j1];				
					p2 = symImage(p0, p1);;					
				}
				else
				{
					p0 = realCoords[j0];
					p1 = realCoords[j1];			
					p2 = realCoords[j2];
				}

				double dist1 = p2.distance(p1);
				Point2D.Double v1 = new Point2D.Double((p2.x-p1.x)/dist1,(p2.y-p1.y)/dist1);
				Point2D.Double vn1 = new Point2D.Double(v1.y,-v1.x);
				double dist2 = p1.distance(p0);
				Point2D.Double v2 = new Point2D.Double((p1.x-p0.x)/dist2,(p1.y-p0.y)/dist2);
				Point2D.Double vn2 = new Point2D.Double(v2.y,-v2.x);
				double h = (new Point2D.Double(vn2.x-vn1.x,vn2.y-vn1.y).distance(new Point2D.Double(0,0))/2.0);
				Point2D.Double vn = new Point2D.Double((vn1.x+vn2.x)/2.0,(vn1.y+vn2.y)/2.0);
				double D = vn.distance(new Point2D.Double(0.0,0.0));
				vn.x/= D;
				vn.y/= D;
				double nnorm = (D+h*h/D);
				
				double nnormF = nnorm;
				double nnormB = nnorm;
				
				
				Point2D.Double interForward = new Point2D.Double(p1.x + nnormF*scaleFactor *getRadius()*vn.x, 
						p1.y + nnormF*scaleFactor *getRadius()*vn.y);
				Point2D.Double interBackward = new Point2D.Double(p1.x - nnormB*scaleFactor *getRadius()*vn.x, 
						p1.y - nnormB*scaleFactor *getRadius()*vn.y);


				if (pointList.size()>0)
				{
					Point2D.Double prev1 = pointList.getLast();			
					Point2D.Double prev2 = pointList.getFirst();

					if ((interForward.distance(prev1)+interBackward.distance(prev2))<(interForward.distance(prev2)+interBackward.distance(prev1)))
					{
						pointList.addLast(interForward);
						pointList.addFirst(interBackward);
					}
					else
					{
						pointList.addFirst(interForward);
						pointList.addLast(interBackward);
					}
				}
				else
				{
					pointList.addLast(interForward);
					pointList.addFirst(interBackward);
				}
			}
		}
		if (getBases().size()==1)
		{
			int midl = pointList.size()/2;
			Point2D.Double mid = pointList.get(midl);
			Point2D.Double apoint = new Point2D.Double(mid.x+1.,mid.y);
			LinkedList<Point2D.Double> pointListStart = buildRoundedCorner(pointList.get(midl-1), pointList.get(midl), apoint);
			pointList.addAll(midl, pointListStart);
			mid = pointList.get(midl);
			apoint = new Point2D.Double(mid.x+1.,mid.y);
			LinkedList<Point2D.Double> pointListEnd = buildRoundedCorner(pointList.get(pointList.size()-1),pointList.get(0), apoint);
			pointList.addAll(0,pointListEnd);			
		}
		else if (getBases().size()>1)
		{
			int midl = pointList.size()/2;
			Point2D.Double apoint = symImage(pointList.get(midl),pointList.get(midl-1));
			LinkedList<Point2D.Double> pointListStart = buildRoundedCorner(pointList.get(midl-1), pointList.get(midl), apoint);
			pointList.addAll(midl, pointListStart);
			apoint = symImage(realCoords[getBases().get(getBases().size()-1).getIndex()],
					realCoords[getBases().get(getBases().size()-2).getIndex()]);
			LinkedList<Point2D.Double> pointListEnd = buildRoundedCorner(pointList.get(pointList.size()-1),pointList.get(0), apoint);
			pointList.addAll(0,pointListEnd);
		}
		

		if (pointList.size()>0)
		{
			Point2D.Double point = pointList.get(0);
			p.moveTo((float)point.x, (float)point.y);

			for (int i=1;i<pointList.size();i++)
			{
				point = pointList.get(i);
				p.lineTo((float)point.x, (float)point.y);				
			}
			p.closePath();
		}
		return p;
	}

	
	public static HighlightRegionAnnotation parseHighlightRegionAnnotation(String txt, VARNAPanel vp)
	{
		try
		{
		String[] parts = txt.split(":");
		String[] coords = parts[0].split("-");
		int from = Integer.parseInt(coords[0]);
		int to = Integer.parseInt(coords[1]);
		int  i = vp.getRNA().getIndexFromBaseNumber(from);
		int  j = vp.getRNA().getIndexFromBaseNumber(to);
		Color fill = HighlightRegionAnnotation.DEFAULT_FILL_COLOR;
		Color outline = HighlightRegionAnnotation.DEFAULT_OUTLINE_COLOR;
		double radius = HighlightRegionAnnotation.DEFAULT_RADIUS;
		ArrayList<ModeleBase> bases = vp.getRNA().getBasesBetween(i, j);
		if (parts.length>1)
		{
		try
		{
			String[] options = parts[1].split(",");
			for (int k = 0; k < options.length; k++) 
			{
				//System.out.println(options[k]);
				try
				{
					String[] data = options[k].split("=");
					String lhs = data[0].toLowerCase();
					String rhs = data[1];
					if (lhs.equals("fill"))
					{
						fill = VARNAConfigLoader.getSafeColor(rhs, fill);
					}
					else if (lhs.equals("outline"))
					{
						outline = VARNAConfigLoader.getSafeColor(rhs, outline);
					}
					else if (lhs.equals("radius"))
					{
						radius = Double.parseDouble(rhs);
					}	
				}
				catch(Exception e)
				{
				}				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		}
		return new HighlightRegionAnnotation(bases,fill,outline,radius);
		}
	catch(Exception e)
	{
		e.printStackTrace();
	}
	return null;
	}
	
	public String toString()
	{
		//String result = "HighlightRegionAnnotation[";
		//result += "fill:"+_fillColor.toString();
		//result += ",outline:"+_outlineColor.toString();
		//result += ",radius:"+_radius;
		//return result+"]";
		String result = "Highlighted region "+getMinIndex()+"-"+getMaxIndex();
		return result;
	}
	
}
