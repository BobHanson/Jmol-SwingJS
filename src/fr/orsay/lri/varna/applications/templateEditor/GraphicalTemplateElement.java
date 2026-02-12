package fr.orsay.lri.varna.applications.templateEditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import fr.orsay.lri.varna.exceptions.ExceptionEdgeEndpointAlreadyConnected;
import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.models.templates.RNATemplate.EdgeEndPointPosition;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement.EdgeEndPoint;

public abstract class GraphicalTemplateElement {
		
	public boolean _debug = false;
    protected HashMap<RelativePosition,Color> _mainColors = new HashMap<RelativePosition,Color>();
    
    public Color _dominantColor = new Color(0.5f,0.5f,0.5f,0.9f);
	
	public enum RelativePosition {
		RP_OUTER,RP_INNER_GENERAL,RP_INNER_MOVE,RP_EDIT_START,RP_EDIT_END,RP_CONNECT_START5,RP_CONNECT_START3,RP_CONNECT_END5,RP_CONNECT_END3,
		RP_EDIT_TANGENT_3,RP_EDIT_TANGENT_5;
	};
	
	static final Color  BACKBONE_COLOR = Color.gray;
	static final Color  CONTROL_COLOR = Color.decode("#D0D0FF");
	static final Font  NUMBER_FONT = new Font("Arial", Font.BOLD,18);
	static final Color NUMBER_COLOR = Color.gray;
	static final Color  BASE_PAIR_COLOR = Color.blue;
	static final Color  BASE_COLOR = Color.gray;
	static final Color  BASE_FILL_COLOR = Color.white;
	static final Color  BASE_FILL_3_COLOR = Color.red;
	static final Color  BASE_FILL_5_COLOR = Color.green;
	static final Color  MAGNET_COLOR = CONTROL_COLOR;

	public void setDominantColor(Color c)
	{
		_dominantColor = c;
	}

	public Color getDominantColor()
	{
		return _dominantColor;
	}

	public abstract RelativePosition getRelativePosition(double x, double y);
	
	public abstract void draw(Graphics2D g2d, boolean selected);
	public abstract Polygon getBoundingPolygon();
	public abstract void translate(double x, double y);
	public abstract RelativePosition getClosestEdge(double x, double y);
	public abstract ArrayList<RelativePosition> getConnectedEdges();
	public abstract RNATemplateElement getTemplateElement();
	public void setMainColor(RelativePosition edge,Color c)
	{
		_mainColors.put(edge, c);
	}

	public abstract Shape getArea();
	
	public void attach(GraphicalTemplateElement e, RelativePosition edgeOrig, RelativePosition edgeDest) throws ExceptionEdgeEndpointAlreadyConnected, ExceptionInvalidRNATemplate
	{
		_attachedElements.put(edgeOrig, new Couple<RelativePosition,GraphicalTemplateElement>(edgeDest,e));
	}
	
	
	/**
	 * Same as attach(), but without touching the underlying
	 * RNATemplateElement objects.
	 * This is useful if the underlying RNATemplate already contains edges
	 * but not the graphical template.
	 */
	public void graphicalAttach(GraphicalTemplateElement e, RelativePosition edgeOrig, RelativePosition edgeDest)
	{
		_attachedElements.put(edgeOrig, new Couple<RelativePosition,GraphicalTemplateElement>(edgeDest,e));
	}
	
	public void detach(RelativePosition edge)
	{
		if (_attachedElements.containsKey(edge))
		{
			Couple<RelativePosition,GraphicalTemplateElement> c = _attachedElements.get(edge);
			_attachedElements.remove(edge);
			c.second.detach(c.first);
		}
	}
	public Couple<RelativePosition,GraphicalTemplateElement> getAttachedElement(RelativePosition localedge)
	{
		if (_attachedElements.containsKey(localedge))
			return _attachedElements.get(localedge);
		return null;
	}
	public boolean hasAttachedElement(RelativePosition localedge)
	{
		return _attachedElements.containsKey(localedge);
	}
	public abstract EdgeEndPoint getEndPoint(RelativePosition r);
	public abstract boolean isIn(RelativePosition r);
	
	public void draw(Graphics2D g2d)
	{
		draw(g2d,false);
	}
	
	private HashMap<RelativePosition,Couple<RelativePosition,GraphicalTemplateElement>> _attachedElements = new HashMap<RelativePosition,Couple<RelativePosition,GraphicalTemplateElement>>();

	
	private Dimension getStringDimension(Graphics2D g, String s) {
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D r = fm.getStringBounds(s, g);
		return (new Dimension((int) r.getWidth(), (int) fm.getAscent()
				- fm.getDescent()));
	}

	

	public void drawStringCentered(Graphics2D g2, String res, double x,
			double y) {
		Dimension d = getStringDimension(g2, res);
		x -= (double) d.width / 2.0;
		y += (double) d.height / 2.0;
		if (_debug)
			g2.drawRect((int) x, (int) y - d.height, d.width, d.height);
		g2.drawString(res, (int) Math.round(x), (int) Math.round(y));
	}
	
	protected Stroke  _boldStroke = new BasicStroke(2.5f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND, 3.0f);
	protected Stroke  _solidStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND, 3.0f);
	private float[] dash = { 5.0f, 5.0f };
	protected Stroke  _dashedStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND, 3.0f, dash, 0);
	
	public abstract RelativePosition getConnectedEdge(RelativePosition edge);
	public abstract Point2D.Double getEdgePosition(RelativePosition edge);
	public abstract void setEdgePosition(RelativePosition edge, Point2D.Double pos);
	public abstract RelativePosition relativePositionFromEdgeEndPointPosition(EdgeEndPointPosition endPointPosition);
	
	public static boolean canConnect(GraphicalTemplateElement el1, RelativePosition e1,GraphicalTemplateElement el2, RelativePosition e2)
	{
		return (!el1.hasAttachedElement(e1))&&(!el2.hasAttachedElement(e2))&&(el1.isIn(e1)!=el2.isIn(e2));
	}

	
	protected void drawMove(Graphics2D g2d, Point2D.Double center)
	{
		g2d.setStroke(_solidStroke);
		g2d.setColor(CONTROL_COLOR);
		g2d.fillOval((int)((center.x-Helix.MOVE_RADIUS)), (int)((center.y-Helix.MOVE_RADIUS)), (int)(2.0*Helix.MOVE_RADIUS), (int)(2.0*Helix.MOVE_RADIUS));
		g2d.setColor(BACKBONE_COLOR);
		g2d.drawOval((int)((center.x-Helix.MOVE_RADIUS)), (int)((center.y-Helix.MOVE_RADIUS)), (int)(2.0*Helix.MOVE_RADIUS), (int)(2.0*Helix.MOVE_RADIUS));
		double arrowLength = Helix.MOVE_RADIUS-2.0;
		double width = 3.0;
		drawArrow(g2d,center,new Point2D.Double(center.x+arrowLength,center.y),width);
		drawArrow(g2d,center,new Point2D.Double(center.x-arrowLength,center.y),width);
		drawArrow(g2d,center,new Point2D.Double(center.x,center.y+arrowLength),width);
		drawArrow(g2d,center,new Point2D.Double(center.x,center.y-arrowLength),width);
	}
	
	protected void drawEditStart(Graphics2D g2d, Helix h, double dx,double dy,double nx,double ny)
	{
		Point2D.Double center = h.getCenterEditStart();
		drawEdit(g2d, center, dx,dy,nx,ny);
	}
	protected void drawEditEnd(Graphics2D g2d, Helix h, double dx,double dy,double nx,double ny)
	{
		Point2D.Double center = h.getCenterEditEnd();
		drawEdit(g2d, center, dx,dy,nx,ny);
	}

	protected void drawEdit(Graphics2D g2d, Point2D.Double center, double dx,double dy,double nx,double ny)
	{
		g2d.setColor(CONTROL_COLOR);
		g2d.fillOval((int)((center.x-Helix.EDIT_RADIUS)), (int)((center.y-Helix.EDIT_RADIUS)), (int)(2.0*Helix.EDIT_RADIUS), (int)(2.0*Helix.EDIT_RADIUS));
		g2d.setColor(BACKBONE_COLOR);
		g2d.drawOval((int)((center.x-Helix.EDIT_RADIUS)), (int)((center.y-Helix.EDIT_RADIUS)), (int)(2.0*Helix.EDIT_RADIUS), (int)(2.0*Helix.EDIT_RADIUS));
		double arrowLength = Helix.EDIT_RADIUS-2.0;
		double width = 3.0;
		drawArrow(g2d,center,new Point2D.Double(center.x+nx*arrowLength,center.y+ny*arrowLength),width);
		drawArrow(g2d,center,new Point2D.Double(center.x-nx*arrowLength,center.y-ny*arrowLength),width);
		drawArrow(g2d,center,new Point2D.Double(center.x+dx*arrowLength,center.y+dy*arrowLength),width);
		drawArrow(g2d,center,new Point2D.Double(center.x-dx*arrowLength,center.y-dy*arrowLength),width);
	}
	
	protected void drawArrow(Graphics2D g2d, Point2D.Double orig, Point2D.Double dest, double width)
	{
		g2d.setStroke(_solidStroke);
		g2d.drawLine((int)orig.x,(int)orig.y,(int)dest.x,(int)dest.y);
		double dx = (orig.x-dest.x)/(orig.distance(dest));
		double dy = (orig.y-dest.y)/(orig.distance(dest));
		double nx = dy;
		double ny = -dx;
		g2d.drawLine((int)dest.x,(int)dest.y,(int)(dest.x-width*(-dx+nx)),(int)(dest.y-width*(-dy+ny)));
		g2d.drawLine((int)dest.x,(int)dest.y,(int)(dest.x-width*(-dx-nx)),(int)(dest.y-width*(-dy-ny)));
	}

	
	
	protected void drawAnchor(Graphics2D g2d, Point2D.Double p)
	{ drawAnchor(g2d,p,CONTROL_COLOR); }

	protected void drawAnchor5(Graphics2D g2d, Point2D.Double p)
	{ drawAnchor(g2d,p,BASE_FILL_5_COLOR); }

	protected void drawAnchor3(Graphics2D g2d, Point2D.Double p)
	{ drawAnchor(g2d,p,BASE_FILL_3_COLOR); }
	
	protected void drawAnchor(Graphics2D g2d, Point2D.Double p, Color c)
	{
		g2d.setColor(c);				
		g2d.fillOval((int)(p.x-Helix.EDGE_BASE_RADIUS),(int)(p.y-Helix.EDGE_BASE_RADIUS),(int)(2.0*Helix.EDGE_BASE_RADIUS),(int)(2.0*Helix.EDGE_BASE_RADIUS));
		g2d.setColor(BASE_COLOR);				
		g2d.drawOval((int)(p.x-Helix.EDGE_BASE_RADIUS),(int)(p.y-Helix.EDGE_BASE_RADIUS),(int)(2.0*Helix.EDGE_BASE_RADIUS),(int)(2.0*Helix.EDGE_BASE_RADIUS));

	}

	protected void drawMagnet(Graphics2D g2d, Point2D.Double p)
	{
		drawAnchor(g2d, p, MAGNET_COLOR);				
		g2d.setColor(BASE_COLOR);				
		g2d.drawOval((int)(p.x-Helix.EDGE_BASE_RADIUS),(int)(p.y-Helix.EDGE_BASE_RADIUS),(int)(2.0*Helix.EDGE_BASE_RADIUS),(int)(2.0*Helix.EDGE_BASE_RADIUS));
		g2d.drawOval((int)(p.x-2),(int)(p.y-2),(int)(2.0*2),(int)(2.0*2));

	}
	
	
	
	protected void drawBase(Graphics2D g2d, Point2D.Double p)
	{
		g2d.setColor(BASE_FILL_COLOR);
		g2d.fillOval((int)(p.x-Helix.BASE_RADIUS),(int)(p.y-Helix.BASE_RADIUS),(int)(2.0*Helix.BASE_RADIUS),(int)(2.0*Helix.BASE_RADIUS));
		g2d.setColor(BASE_COLOR);
		g2d.drawOval((int)(p.x-Helix.BASE_RADIUS),(int)(p.y-Helix.BASE_RADIUS),(int)(2.0*Helix.BASE_RADIUS),(int)(2.0*Helix.BASE_RADIUS));
	}

	public boolean equals(Object b)
	{
	  if (b instanceof GraphicalTemplateElement)
	  {
		return b==this;  
	  }
	  else
		  return false;
	}
	
}
