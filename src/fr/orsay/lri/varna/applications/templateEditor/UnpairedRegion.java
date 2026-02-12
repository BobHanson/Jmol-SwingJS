package fr.orsay.lri.varna.applications.templateEditor;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

import fr.orsay.lri.varna.applications.templateEditor.GraphicalTemplateElement.RelativePosition;
import fr.orsay.lri.varna.exceptions.ExceptionEdgeEndpointAlreadyConnected;
import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.models.geom.CubicBezierCurve;
import fr.orsay.lri.varna.models.templates.RNATemplate;
import fr.orsay.lri.varna.models.templates.RNATemplate.EdgeEndPointPosition;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateUnpairedSequence;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement.EdgeEndPoint;

public class UnpairedRegion extends GraphicalTemplateElement{
	private RNATemplateUnpairedSequence _e;
	public static final double DEFAULT_VECTOR_LENGTH = 35;
	public static final double DEFAULT_VECTOR_DISTANCE = 35;

	private Point2D.Double[] sequenceBasesCoords = null;

	public UnpairedRegion(double x, double y, RNATemplate tmp)
	{
		_e = tmp.new RNATemplateUnpairedSequence("");
		_e.setVertex5(new Point2D.Double(x,y));
		_e.setVertex3(new Point2D.Double(x+DEFAULT_VECTOR_DISTANCE,y));
		_e.setInTangentVectorLength(DEFAULT_VECTOR_LENGTH);
		_e.setInTangentVectorAngle(-Math.PI/2.0);
		_e.setOutTangentVectorLength(DEFAULT_VECTOR_LENGTH);
		_e.setOutTangentVectorAngle(-Math.PI/2.0);
		updateLength();

	}

	/**
	 * Build an UnpairedRegion object from a RNATemplateUnpairedSequence
	 * object. The RNATemplateUnpairedSequence must be connected to
	 * an helix on both sides.
	 */
	public UnpairedRegion(RNATemplateUnpairedSequence templateSequence)
	{
		_e = templateSequence;
	}

	public Point2D.Double getEdge5()
	{ 
		RelativePosition r = RelativePosition.RP_CONNECT_START5;
		Couple<RelativePosition,GraphicalTemplateElement> c = getAttachedElement(r);
		return (isAnchored5()? c.second.getEdgePosition(c.first): _e.getVertex5()); 
	}

	public Point2D.Double getEdge3()
	{ 
		RelativePosition r = RelativePosition.RP_CONNECT_END3;
		Couple<RelativePosition,GraphicalTemplateElement> c = getAttachedElement(r);
		return (isAnchored3()? c.second.getEdgePosition(c.first): _e.getVertex3()); 
	}

	public Point2D.Double getCenter()
	{ 
		Point2D.Double p1 = getEdge5();
		Point2D.Double p2 = getEdge3();
		return new Point2D.Double((p1.x+p2.x)/2.,(p1.y+p2.y)/2.); 
	}
	
	
	public void setEdge5(Point2D.Double d)
	{
		_e.setVertex5(d);
		updateLength();
	}

	public void setEdge3(Point2D.Double d)
	{
		_e.setVertex3(d);
		updateLength();
	}

	public void setCenter(Point2D.Double d)
	{
		Point2D.Double p1 = getEdge5();
		Point2D.Double p2 = getEdge3();
		double dx = p1.x-p2.x;
		double dy = p1.y-p2.y;
		_e.setVertex3(new Point2D.Double(d.x-dx/2.,d.y-dy/2.));
		_e.setVertex5(new Point2D.Double(d.x+dx/2.,d.y+dy/2.));
		invalidateCoords();
	}
	
	
	public boolean isAnchored5()
	{
		return (_e.getIn().getOtherElement()!=null);
	}

	public boolean isAnchored3()
	{
		return (_e.getOut().getOtherElement()!=null);
	}


	public static Shape bezToShape(CubicBezierCurve c)
	{
		GeneralPath p = new GeneralPath();
		int nb = 9;
		double[] tab = new double[nb];
		for (int i=0;i<nb;i++)
		{
			tab[i] = (c.getApproxCurveLength()*(double)i)/(double)nb;
		}
		Point2D.Double[] points = c.uniformParam(tab);
		System.out.println(points.length);
		p.moveTo((float)points[0].x,(float)points[0].y);
		for (int i=1;i<nb;i++)
		{ 
			Point2D.Double a = points[i];
			System.out.println(a);
			p.lineTo((float)a.x,(float)a.y);
		}
		p.lineTo((float)c.getP3().x,(float)c.getP3().y);
		return p;
	}



	public Shape getCurve()
	{
		Point2D.Double p5 = getEdge5(); 
		Point2D.Double p3 = getEdge3(); 
		Point2D.Double t5 = getControl5(); 
		Point2D.Double t3 = getControl3();
		return new CubicCurve2D.Double(p5.x,p5.y,t5.x,t5.y,t3.x,t3.y,p3.x,p3.y);
		//CubicBezierCurve c = new CubicBezierCurve( p5, t5, t3, p3, 30);
		//return bezToShape(c);

	}

	private int estimateNumberOfBases()
	{
		Point2D.Double p5 = getEdge5(); 
		Point2D.Double p3 = getEdge3(); 
		Point2D.Double t5 = getControl5(); 
		Point2D.Double t3 = getControl3();
		CubicBezierCurve c = new CubicBezierCurve( p5, t5, t3, p3, 30);
		// Extremities don't count as unpaired bases because they are part of the connected helix.
		return Math.max((int)Math.round(c.getApproxCurveLength()/Helix.LOOP_DISTANCE)-1, 1);
	}

	private void updateLength()
	{
		this._e.setLength(estimateNumberOfBases());
		invalidateCoords();
	}

	
	/**
	 * Mark the coordinates as invalid, ie. need to be calculated again if we want to draw them.
	 */
	private void invalidateCoords() {
		sequenceBasesCoords = null;
	}

	/**
	 * Calculate the positions of the unpaired bases.
	 */
	private void calculeCoords() {
		//System.out.println("calculate coords");
		
		Point2D.Double p5 = getEdge5(); 
		Point2D.Double p3 = getEdge3(); 
		Point2D.Double t5 = getControl5(); 
		Point2D.Double t3 = getControl3();
		
		// Draw bases on curve:
		int n = _e.getLength();
		// We choose to approximate the Bezier curve by 10*n straight lines.
		CubicBezierCurve bezier = new CubicBezierCurve(p5, t5, t3, p3, 10*n);
		double curveLength = bezier.getApproxCurveLength();
		double delta_t = curveLength / (n+1);
		double[] t = new double[n];
		for (int k=0; k<n; k++) {
			t[k] = (k+1) * delta_t;
		}
		sequenceBasesCoords = bezier.uniformParam(t);
	}

	public void draw(Graphics2D g2d, boolean selected) {
		Point2D.Double p5 = getEdge5(); 
		Point2D.Double p3 = getEdge3(); 
		Point2D.Double t5 = getControl5(); 
		Point2D.Double t3 = getControl3();
		if (selected)
		{
			g2d.setStroke(_dashedStroke);
			g2d.setColor(BACKBONE_COLOR);
			g2d.draw(getBoundingPolygon());
			g2d.setStroke(_solidStroke);
			drawAnchor(g2d,t5);
			drawAnchor(g2d,t3);
			double d5x = (t5.x-p5.x)/(t5.distance(p5));
			double d5y = (t5.y-p5.y)/(t5.distance(p5));
			double d3x = (t3.x-p3.x)/(t3.distance(p3));
			double d3y = (t3.y-p3.y)/(t3.distance(p3));
			double shift = -3.5;
			Point2D.Double tp5 = new Point2D.Double(t5.x-shift*d5x,t5.y-shift*d5y); 
			Point2D.Double tp3 = new Point2D.Double(t3.x-shift*d3x,t3.y-shift*d3y);

			drawArrow(g2d, p5, tp5, UNPAIRED_ARROW_WIDTH);
			drawArrow(g2d, p3, tp3, UNPAIRED_ARROW_WIDTH);
		}
		g2d.setColor(BACKBONE_COLOR);
		g2d.setStroke(_solidStroke);
		g2d.draw(getCurve());

		if (sequenceBasesCoords == null) {
			calculeCoords();
		}
		for (int k=0; k<sequenceBasesCoords.length; k++) {
			drawBase(g2d, sequenceBasesCoords[k]);
		}

		if (!isAnchored5())
		{drawAnchor5(g2d,p5); }
		else
		{ drawMagnet(g2d,p5);}
		if (!isAnchored3())
		{drawAnchor3(g2d,p3); }
		else
		{ drawMagnet(g2d,p3);}
		
		if (!isAnchored5() && !isAnchored3())
		drawMove(g2d, getCenter());
	}


	public Point2D.Double getControl5()
	{
		Point2D.Double p5 = getEdge5();
		double angle = _e.getInTangentVectorAngle();
		return new Point2D.Double(p5.x+Math.cos(angle)*_e.getInTangentVectorLength(),
				p5.y+Math.sin(angle)*_e.getInTangentVectorLength());
	}

	public Point2D.Double getControl3()
	{
		Point2D.Double p3 = getEdge3(); 
		double angle = _e.getOutTangentVectorAngle();
		return new Point2D.Double(p3.x+Math.cos(angle)*_e.getOutTangentVectorLength(),
				p3.y+Math.sin(angle)*_e.getOutTangentVectorLength());
	}

	public static final double MAX_UNPAIRED_CONTROL_DISTANCE = 10.0;
	public static final double UNPAIRED_ARROW_WIDTH = 6.0;



	public Polygon getBoundingPolygon() {
		Point2D.Double p5 = getEdge5(); 
		Point2D.Double p3 = getEdge3(); 
		Point2D.Double t5 = getControl5(); 
		Point2D.Double t3 = getControl3();

		double minx = Math.min(p5.x,Math.min(p3.x,Math.min(t5.x,t3.x)));
		double maxx = Math.max(p5.x,Math.max(p3.x,Math.max(t5.x,t3.x)));
		double miny = Math.min(p5.y,Math.min(p3.y,Math.min(t5.y,t3.y)));
		double maxy = Math.max(p5.y,Math.max(p3.y,Math.max(t5.y,t3.y)));
		minx -= 10;
		maxx += 10;
		miny -= 10;
		maxy += 10;
		int[] x = {(int)minx,(int)maxx,(int)maxx,(int)minx};
		int[] y = {(int)miny,(int)miny,(int)maxy,(int)maxy};
		return new Polygon(x,y,4);
	}

	public RelativePosition getClosestEdge(double x, double y) {
		Point2D.Double p = new Point2D.Double(x,y);
		Point2D.Double p5 = getEdge5(); 
		Point2D.Double p3 = getEdge3(); 
		Point2D.Double t5 = getControl5(); 
		Point2D.Double t3 = getControl3();
		Point2D.Double ct = getCenter();
		ArrayList<Couple<java.lang.Double,RelativePosition>> v = new ArrayList<Couple<java.lang.Double,RelativePosition>>(); 
		v.add(new Couple<java.lang.Double,RelativePosition>(p.distance(p5),RelativePosition.RP_CONNECT_START5));
		v.add(new Couple<java.lang.Double,RelativePosition>(p.distance(p3),RelativePosition.RP_CONNECT_END3));
		v.add(new Couple<java.lang.Double,RelativePosition>(p.distance(t5),RelativePosition.RP_EDIT_TANGENT_5));
		v.add(new Couple<java.lang.Double,RelativePosition>(p.distance(t3),RelativePosition.RP_EDIT_TANGENT_3));
		v.add(new Couple<java.lang.Double,RelativePosition>(p.distance(ct),RelativePosition.RP_INNER_MOVE));
		double dist = java.lang.Double.MAX_VALUE;
		RelativePosition r = RelativePosition.RP_OUTER;
		
		for (Couple<java.lang.Double,RelativePosition> c : v)
		{
			if (c.first<dist)
			{
				dist = c.first;
				r = c.second;
			}
		}
		return r;
	}

	public RelativePosition getConnectedEdge(RelativePosition edge) {
		switch(edge)
		{
		case RP_CONNECT_START5:
			return RelativePosition.RP_CONNECT_END3;
		case RP_CONNECT_END3:
			return RelativePosition.RP_CONNECT_START5;
		default:
			return RelativePosition.RP_OUTER;
		}
	}

	public Point2D.Double getEdgePosition(RelativePosition edge)
	{
		switch(edge)
		{
		case RP_INNER_MOVE:
			return getCenter();
		case RP_CONNECT_START5:
			return getEdge5();
		case RP_CONNECT_END3:
			return getEdge3();
		case RP_EDIT_TANGENT_5:
			return getControl5();
		case RP_EDIT_TANGENT_3:
			return getControl3();
		default:
			return getEdge5();
		}
	}

	double v2a(Point2D.Double p)
	{
		return (double)Math.atan2(p.y, p.x);
	}

	public void updateControl5(Point2D.Double p)
	{
		Point2D.Double p5 = getEdge5();
		_e.setInTangentVectorLength(p5.distance(p));
		Point2D.Double x = new Point2D.Double(p.x-p5.x,p.y-p5.y); 
		_e.setInTangentVectorAngle(v2a(x));
		updateLength();
	}

	public void updateControl3(Point2D.Double p)
	{
		Point2D.Double p3 = getEdge3();
		_e.setOutTangentVectorLength(p3.distance(p));
		Point2D.Double x = new Point2D.Double(p.x-p3.x,p.y-p3.y); 
		_e.setOutTangentVectorAngle(v2a(x));
		updateLength();
	}

	public void translate(double x, double y) {
		_e.getVertex5().x += x;
		_e.getVertex5().y += y;
		_e.getVertex3().x += x;
		_e.getVertex3().y += y;
		invalidateCoords();
	}

	public RelativePosition getRelativePosition(double x, double y) {
		RelativePosition rp = getClosestEdge(x, y);
		double d = getEdgePosition(rp).distance(new Point2D.Double(x,y));
		if (d<MAX_UNPAIRED_CONTROL_DISTANCE)
			return rp;
		if (getCurve().contains(new Point2D.Double(x,y)))
			return RelativePosition.RP_INNER_GENERAL;
		return RelativePosition.RP_OUTER;
	}

	public Shape getArea()
	{
		return getCurve();
	}


	public void attach(GraphicalTemplateElement e, RelativePosition edgeOrig, RelativePosition edgeDest) throws ExceptionInvalidRNATemplate
	{
		super.attach(e,edgeOrig,edgeDest);
		if (e instanceof Helix)
		{
			EdgeEndPoint e1 = this.getEndPoint(edgeOrig);
			EdgeEndPoint e2 = e.getEndPoint(edgeDest);
			boolean parity1 = this.isIn(edgeOrig);
			boolean parity2 = e.isIn(edgeDest);
			if ((e1!=null)&&(e2!=null)&&(parity1!=parity2))
			{
				e1.disconnect();
				e2.disconnect();
				e1.connectTo(e2);   
			}
		}
	}


	public EdgeEndPoint getEndPoint(RelativePosition r) {
		switch(r)
		{
		case RP_CONNECT_START5:
			return _e.getIn();				
		case RP_CONNECT_END3:
			return _e.getOut();
		}
		return null;
	}

	public boolean isIn(RelativePosition r) {
		switch(r)
		{
		case RP_CONNECT_START5:
			return true;				
		case RP_CONNECT_END3:
			return false;
		}
		return true;
	}

	public void detach(RelativePosition edge)
	{
		// If the underlying template element is still connected, disconnect it
		if (getEndPoint(edge).isConnected())
		{
			Couple<GraphicalTemplateElement.RelativePosition, GraphicalTemplateElement> c = getAttachedElement(edge);
			getEndPoint(edge).disconnect();
		}

		// Call the parent class detach function, which will also take care to disconnect this other endpoint of this edge
		super.detach(edge);
	}

	public void setEdgePosition(RelativePosition edge, Point2D.Double pos) {
		switch(edge)
		{
		case RP_CONNECT_START5:
			setEdge5(pos);
			break;
		case RP_INNER_MOVE:
			setCenter(pos);
			break;
		case RP_CONNECT_END3:
			setEdge3(pos);
			break;
		case RP_EDIT_TANGENT_5:
			updateControl5(pos);
			break;
		case RP_EDIT_TANGENT_3:
			updateControl3(pos);
			break;
		}
	}

	public ArrayList<RelativePosition> getConnectedEdges() {
		ArrayList<RelativePosition> result = new ArrayList<RelativePosition>();
		result.add(RelativePosition.RP_CONNECT_START5);
		result.add(RelativePosition.RP_CONNECT_END3);
		return result;
	}

	public RNATemplateElement getTemplateElement() {
		return _e;
	}


	public RelativePosition relativePositionFromEdgeEndPointPosition(
			EdgeEndPointPosition pos) {
		switch (pos) {
		case IN1:
			return RelativePosition.RP_CONNECT_START5;
		case OUT1:
			return RelativePosition.RP_CONNECT_END3;
		default:
			return null;
		}
	}


}
