package fr.orsay.lri.varna.applications.templateEditor;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.models.templates.RNATemplate;
import fr.orsay.lri.varna.models.templates.RNATemplate.EdgeEndPointPosition;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement.EdgeEndPoint;


public class Helix extends GraphicalTemplateElement{
	
  RNATemplateHelix _h;
  
  public Helix(double x, double y, RNATemplate tmp, List<GraphicalTemplateElement> existingRNAElements)
  {
	  this(x,y,getNextAutomaticCaption(existingRNAElements),tmp);
  }
  
  public Helix(double x, double y, String cap, RNATemplate tmp)
  {
	  _h = tmp.new RNATemplateHelix(cap);
	  _h.setStartPosition(new Point2D.Double(x,y));
	  _h.setEndPosition(new Point2D.Double(x,y));
	  _h.setLength(1);
	  _h.setCaption(cap);
  }
  
  public Helix(RNATemplateHelix templateHelix) {
	  _h = templateHelix;
  }
  
  
  private static String getNextAutomaticCaption(List<GraphicalTemplateElement> existingRNAElements) {
	  // Find which captions are already used
	  Set<String> captions = new HashSet<String>();
	  for (GraphicalTemplateElement element: existingRNAElements) {
		  if (element instanceof Helix) {
			  Helix helix = (Helix) element;
			  if (helix.getCaption() != null) {
				  captions.add(helix.getCaption());
			  }
		  }
	  }
	  // Find a non-conflicting name for this helix
	  for (int i=1;;i++) {
		  String candidateCaption = "H" + i;
		  if (! captions.contains(candidateCaption)) {
			  return candidateCaption;
		  }
	  }
  }
  
  public void toggleFlipped()
  {
	  _h.setFlipped(!_h.isFlipped());
	  updateAttachedUnpairedRegions();
  }
  

  /**
   * When an helix is moved/resized/etc... it is necessary to update
   * the positions of endpoints from unpaired regions that are attached
   * to the helix. This function updates the endpoints positions of
   * attached unpaired regions.
   */
  public void updateAttachedUnpairedRegions() {
	  for (RelativePosition rpos: getConnectedEdges()) {
		  Couple<RelativePosition,GraphicalTemplateElement> c = getAttachedElement(rpos);
		  if (c != null && c.second instanceof UnpairedRegion) {
			  UnpairedRegion unpairedRegion = (UnpairedRegion) c.second;
			  Point2D.Double pos = getEdgePosition(rpos);
			  if (c.first == RelativePosition.RP_CONNECT_START5) {
				  unpairedRegion.setEdge5(pos);
			  } else if (c.first == RelativePosition.RP_CONNECT_END3) {
				  unpairedRegion.setEdge3(pos);
			  }
		  }
	  }
  }

  public double getPosX()
  {
	  return _h.getStartPosition().x;
  }

  public String getCaption()
  {
	  return _h.getCaption();
  }
  
  public double getPosY()
  {
	  return _h.getStartPosition().y;
  }
  
  public RNATemplateHelix getTemplateElement()
  {
	  return _h;
  }

  public void setX(double x)
  {
	  _h.getStartPosition().x = x;
  }
  
  public void setY(double y)
  {
	  _h.getStartPosition().y = y;
  }

  public void setPos(Point2D.Double p)
  {
	 _h.setStartPosition(p);
	  updateLength();
  }

  public void setPos(double x, double y)
  {
	  setPos(new Point2D.Double(x,y));
  }

  public Point2D.Double getPos()
  {
	  return _h.getStartPosition();
  }

  public void moveCenter(double x, double y)
  {
	  Point2D.Double center = new Point2D.Double((_h.getStartPosition().x+_h.getEndPosition().x)/2.0,(_h.getStartPosition().y+_h.getEndPosition().y)/2.0);
	  double dx = x-center.x;
	  double dy = y-center.y;
	  _h.setStartPosition(new Point2D.Double(_h.getStartPosition().x+dx,_h.getStartPosition().y+dy));
	  _h.setEndPosition(new Point2D.Double(_h.getEndPosition().x+dx,_h.getEndPosition().y+dy));
  }

  
  public void setExtent(double x, double y)
  {
	 setExtent(new Point2D.Double(x,y));
  }

  private void updateLength()
  {
	  _h.setLength(getNbBP());
  }
  
  public void setExtent(Point2D.Double p)
  {
	 _h.setEndPosition(p);
	  updateLength();
  }

  public double getExtentX()
  {
	  return _h.getEndPosition().x;
  }

  public Point2D.Double getExtent()
  {
	  return _h.getEndPosition();
  }

  public double getExtentY()
  {
	  return _h.getEndPosition().y;
  }
  
	public static final double BASE_PAIR_DISTANCE = RNA.BASE_PAIR_DISTANCE;
	public static final double LOOP_DISTANCE = RNA.LOOP_DISTANCE;
	public static final double SELECTION_RADIUS = 15.0;
	
	
	

	public Point2D.Double getAbsStart5()
	{
		double dx = (_h.getStartPosition().x-_h.getEndPosition().x)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double dy = (_h.getStartPosition().y-_h.getEndPosition().y)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double nx = dy;
		double ny = -dx;
		Point2D.Double start5 = new Point2D.Double((getPosX()-Helix.BASE_PAIR_DISTANCE*nx/2.0),(getPosY()-Helix.BASE_PAIR_DISTANCE*ny/2.0)); 
		return start5;
	}

	public Point2D.Double getAbsStart3()
	{
		double dx = (_h.getStartPosition().x-_h.getEndPosition().x)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double dy = (_h.getStartPosition().y-_h.getEndPosition().y)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double nx = dy;
		double ny = -dx;
		Point2D.Double start3 = new Point2D.Double((getPosX()+Helix.BASE_PAIR_DISTANCE*nx/2.0),(getPosY()+Helix.BASE_PAIR_DISTANCE*ny/2.0)); 
		return start3;
	}

	public Point2D.Double getAbsEnd5()
	{
		double dx = (_h.getStartPosition().x-_h.getEndPosition().x)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double dy = (_h.getStartPosition().y-_h.getEndPosition().y)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double nx = dy;
		double ny = -dx;
		Point2D.Double end5 = new Point2D.Double((getExtentX()-Helix.BASE_PAIR_DISTANCE*nx/2.0),(getExtentY()-Helix.BASE_PAIR_DISTANCE*ny/2.0)); 
		return end5;
	}

	public Point2D.Double getAbsEnd3()
	{
		double dx = (_h.getStartPosition().x-_h.getEndPosition().x)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double dy = (_h.getStartPosition().y-_h.getEndPosition().y)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double nx = dy;
		double ny = -dx;
		Point2D.Double end3 = new Point2D.Double((getExtentX()+Helix.BASE_PAIR_DISTANCE*nx/2.0),(getExtentY()+Helix.BASE_PAIR_DISTANCE*ny/2.0));
		return end3;
	}
	
	public Point2D.Double getStart5()
	{
		if (_h.isFlipped())
			return getAbsStart3();
		else
			return getAbsStart5();
		
	}

	public Point2D.Double getStart3()
	{
		if (_h.isFlipped())
			return getAbsStart5();
		else
			return getAbsStart3();
	}

	public Point2D.Double getEnd5()
	{
		if (_h.isFlipped())
			return getAbsEnd3();
		else
			return getAbsEnd5();
	}

	public Point2D.Double getEnd3()
	{
		if (_h.isFlipped())
			return getAbsEnd5();
		else
			return getAbsEnd3();
	}

	public Polygon getBoundingPolygon()
	{
		double dx = (_h.getStartPosition().x-_h.getEndPosition().x)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double dy = (_h.getStartPosition().y-_h.getEndPosition().y)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double nx = dy;
		double ny = -dx;
		Point2D.Double start5 = new Point2D.Double((getPosX()+Helix.BASE_PAIR_DISTANCE*nx/2.0),(getPosY()+Helix.BASE_PAIR_DISTANCE*ny/2.0)); 
		Point2D.Double end5 = new Point2D.Double((getExtentX()+Helix.BASE_PAIR_DISTANCE*nx/2.0),(getExtentY()+Helix.BASE_PAIR_DISTANCE*ny/2.0)); 
		Point2D.Double start3 = new Point2D.Double((getPosX()-Helix.BASE_PAIR_DISTANCE*nx/2.0),(getPosY()-Helix.BASE_PAIR_DISTANCE*ny/2.0)); 
		Point2D.Double end3 = new Point2D.Double((getExtentX()-Helix.BASE_PAIR_DISTANCE*nx/2.0),(getExtentY()-Helix.BASE_PAIR_DISTANCE*ny/2.0));
		Polygon p = new Polygon();
		p.addPoint((int)start5.x, (int)start5.y);
		p.addPoint((int)end5.x, (int)end5.y);
		p.addPoint((int)end3.x, (int)end3.y);
		p.addPoint((int)start3.x, (int)start3.y);
		return p;
	}

	
	public Point2D.Double getCenter()
	{
		return new Point2D.Double((int)((_h.getStartPosition().x+_h.getEndPosition().x)/2.0),
				(int)((_h.getStartPosition().y+_h.getEndPosition().y)/2.0));
	}


	public Point2D.Double getCenterEditStart()
	{
		double dist = _h.getStartPosition().distance(_h.getEndPosition());
		double dx = (_h.getEndPosition().x-_h.getStartPosition().x)/(dist);
		double dy = (_h.getEndPosition().y-_h.getStartPosition().y)/(dist);
		return new Point2D.Double((int)(_h.getStartPosition().x+(dist-10.0)*dx),
				(int)(_h.getStartPosition().y+(dist-10.0)*dy));
	}	

	public Point2D.Double getCenterEditEnd()
	{
		double dist = _h.getStartPosition().distance(_h.getEndPosition());
		double dx = (_h.getEndPosition().x-_h.getStartPosition().x)/(dist);
		double dy = (_h.getEndPosition().y-_h.getStartPosition().y)/(dist);
		return new Point2D.Double((int)(_h.getStartPosition().x+(10.0)*dx),
				(int)(_h.getStartPosition().y+(10.0)*dy));
	}	

	
	public Shape getSelectionBox()
	{
		double dx = (_h.getStartPosition().x-_h.getEndPosition().x)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double dy = (_h.getStartPosition().y-_h.getEndPosition().y)/(_h.getStartPosition().distance(_h.getEndPosition()));
		double nx = dy;
		double ny = -dx;
		Polygon hbox = getBoundingPolygon();
		Polygon p = new Polygon();
		Point2D.Double start5 = new Point2D.Double(hbox.xpoints[0]+SELECTION_RADIUS*(dx+nx),hbox.ypoints[0]+SELECTION_RADIUS*(dy+ny)); 
		Point2D.Double end5 = new Point2D.Double(hbox.xpoints[1]+SELECTION_RADIUS*(-dx+nx),hbox.ypoints[1]+SELECTION_RADIUS*(-dy+ny)); 
		Point2D.Double end3  = new Point2D.Double(hbox.xpoints[2]+SELECTION_RADIUS*(-dx-nx),hbox.ypoints[2]+SELECTION_RADIUS*(-dy-ny));;
		Point2D.Double start3 = new Point2D.Double(hbox.xpoints[3]+SELECTION_RADIUS*(dx-nx),hbox.ypoints[3]+SELECTION_RADIUS*(dy-ny));; 
		p.addPoint((int)start5.x, (int)start5.y);
		p.addPoint((int)end5.x, (int)end5.y);
		p.addPoint((int)end3.x, (int)end3.y);
		p.addPoint((int)start3.x, (int)start3.y);
		return p;
	}

	public Shape getArea()
	{
		return getSelectionBox();
	}

	
	public static final double EDIT_RADIUS = 10.0;
	public static final double MOVE_RADIUS = 13.0;
	public static final double BASE_RADIUS = 8.0;
	public static final double EDGE_BASE_RADIUS = 7.0;
	
	
	public RelativePosition getRelativePosition(double x, double y)
	{
		Point2D.Double current = new Point2D.Double(x,y);
		Shape p = getSelectionBox();
		if (p.contains(current))
		{
			if (getCenterEditStart().distance(current)<EDIT_RADIUS)
			{
				return RelativePosition.RP_EDIT_START;
			}
			else if (getCenterEditEnd().distance(current)<EDIT_RADIUS)
			{
				return RelativePosition.RP_EDIT_END;
			}
			else if (getCenter().distance(current)<MOVE_RADIUS)
			{
				return RelativePosition.RP_INNER_MOVE;
			}
			else if (getEnd3().distance(current)<EDGE_BASE_RADIUS)
			{
				return RelativePosition.RP_CONNECT_END3;
			}
			else if (getEnd5().distance(current)<EDGE_BASE_RADIUS)
			{
				return RelativePosition.RP_CONNECT_END5;
			}
			else if (getStart3().distance(current)<EDGE_BASE_RADIUS)
			{
				return RelativePosition.RP_CONNECT_START3;
			}
			else if (getStart5().distance(current)<EDGE_BASE_RADIUS)
			{
				return RelativePosition.RP_CONNECT_START5;
			}
				
			return RelativePosition.RP_INNER_GENERAL;
		}
		else 
			return RelativePosition.RP_OUTER;  
	}

	public RelativePosition getClosestEdge(double x, double y)
	{
		RelativePosition result = RelativePosition.RP_OUTER;
		double dist = Double.MAX_VALUE;
		Point2D.Double current = new Point2D.Double(x,y);
		double dcand = getStart5().distance(current);
	    if (dcand<dist)
	    {
	    	dist = dcand;
	    	result = RelativePosition.RP_CONNECT_START5;
	    }
		dcand = getStart3().distance(current);
	    if (dcand<dist)
	    {
	    	dist = dcand;
	    	result = RelativePosition.RP_CONNECT_START3;
	    }
		dcand = getEnd5().distance(current);
	    if (dcand<dist)
	    {
	    	dist = dcand;
	    	result = RelativePosition.RP_CONNECT_END5;
	    }
		dcand = getEnd3().distance(current);
	    if (dcand<dist)
	    {
	    	dist = dcand;
	    	result = RelativePosition.RP_CONNECT_END3;
	    }
		return result;
	}
	
	public Point2D.Double getEdgePosition(Helix.RelativePosition edge)
	{
		switch (edge)
		{
			case RP_CONNECT_END3:
				return getEnd3();
			case RP_CONNECT_END5:
				return getEnd5();
			case RP_CONNECT_START5:
				return getStart5();
			case RP_CONNECT_START3:
				return getStart3();
			case RP_EDIT_START:
				return getPos();
			case RP_EDIT_END:
				return  getExtent();
			case RP_INNER_MOVE:
				return  getCenter();
		}
		return getCenter();
	}

	public RelativePosition getConnectedEdge(RelativePosition edge)
	{
		switch (edge)
		{
			case RP_CONNECT_END3:
				return RelativePosition.RP_CONNECT_START3;
			case RP_CONNECT_END5:
				return RelativePosition.RP_CONNECT_START5;
			case RP_CONNECT_START5:
				return RelativePosition.RP_CONNECT_END5;
			case RP_CONNECT_START3:
				return RelativePosition.RP_CONNECT_END3;
		}
		return RelativePosition.RP_OUTER;
	}
	
	public boolean isAnchored5Start()
	{
		return (_h.getIn1().getOtherElement()!=null);
	}

	public boolean isAnchored5End()
	{
		return (_h.getOut1().getOtherElement()!=null);
	}
	
	public boolean isAnchored3Start()
	{
		return (_h.getOut2().getOtherElement()!=null);
	}

	public boolean isAnchored3End()
	{
		return (_h.getIn2().getOtherElement()!=null);
	}
	
	public int getNbBP()
	{
		Point2D.Double pos = getPos();
		Point2D.Double extent = getExtent();
		double helLength =  pos.distance(extent);
		return Math.max((int)Math.round(helLength/Helix.LOOP_DISTANCE) + 1, 2);
	}
	
	public void draw(Graphics2D g2d,boolean isSelected)
	{
		g2d.setStroke(_solidStroke);
		Point2D.Double pos = getPos();
		Point2D.Double extent = getExtent();
		double dx = (pos.x-extent.x)/pos.distance(extent);
		double dy = (pos.y-extent.y)/pos.distance(extent);
		double nx = Helix.BASE_PAIR_DISTANCE*dy/2.0;
		double ny = -Helix.BASE_PAIR_DISTANCE*dx/2.0;
		Point2D.Double start5 = getStart5(); 
		Point2D.Double end5 = getEnd5(); 
		Point2D.Double start3 = getStart3(); 
		Point2D.Double end3 = getEnd3();
		
		for (RelativePosition e:this.getConnectedEdges())
		{
			g2d.setStroke(_solidStroke);
			g2d.setColor(BACKBONE_COLOR);
			Point2D.Double p1 = this.getEdgePosition(e);
			Point2D.Double p2 = this.getEdgePosition(getConnectedEdge(e));
			if (_mainColors.containsKey(e))
			{ 
				g2d.setColor(_mainColors.get(e));
				g2d.setStroke(this._boldStroke);
			}
			g2d.drawLine((int)p1.x,(int)p1.y,(int)(p1.x+p2.x)/2,(int)(p1.y+p2.y)/2);
		}

		g2d.setColor(NUMBER_COLOR);
		double captionx = (_h.isFlipped()?-1.0:1.0)*1.5*nx+(start3.x+end3.x)/2.0;
		double captiony = (_h.isFlipped()?-1.0:1.0)*1.5*ny+(start3.y+end3.y)/2.0;
		drawStringCentered(g2d, getCaption(),captionx,captiony);

		int nbBasePairs = _h.getLength();

		g2d.setStroke(_solidStroke);

		for (int i=0;i<nbBasePairs;i++)
		{
			g2d.setColor(BASE_PAIR_COLOR);
			Point2D.Double p5 = new Point2D.Double(
					(i*start5.x+(nbBasePairs-1-i)*end5.x)/(nbBasePairs-1),
					(i*start5.y+(nbBasePairs-1-i)*end5.y)/(nbBasePairs-1));
			Point2D.Double p3 = new Point2D.Double(
					(i*start3.x+(nbBasePairs-1-i)*end3.x)/(nbBasePairs-1),
					(i*start3.y+(nbBasePairs-1-i)*end3.y)/(nbBasePairs-1));
			g2d.drawLine((int)p3.x,(int)p3.y,(int)p5.x,(int)p5.y);
			if (i==0)
			{
				if (isAnchored5End())
				{ drawMagnet(g2d, p5); }
				else
				{ drawAnchor3(g2d, p5); }
				
				if (isAnchored3End())
				{ drawMagnet(g2d, p3); }
				else
				{ drawAnchor5(g2d, p3); }
			}
			else if (i==nbBasePairs-1)
			{
				if (isAnchored5Start())
				{ drawMagnet(g2d, p5); }
				else
				{ drawAnchor5(g2d, p5); }
				
				if (isAnchored3Start())
				{ drawMagnet(g2d, p3); }
				else
				{ drawAnchor3(g2d, p3); }
			}
			else
			{
				drawBase(g2d, p3);
				drawBase(g2d, p5);
			}
		}

		if (isSelected)
		{
			nx = dy;
			ny = -dx;
			Shape p = getSelectionBox();
			g2d.setColor(BACKBONE_COLOR);
			g2d.setStroke(_dashedStroke);
			g2d.draw(p);
			Point2D.Double center = getCenter();
			g2d.setStroke(_solidStroke);
			drawMove(g2d,center);
			drawEditStart(g2d,this,-dx,-dy,nx,ny);			
			drawEditEnd(g2d,this,-dx,-dy,nx,ny);			
		}
	}


	public void translate(double x, double y) {
		  Point2D.Double pos = getPos();
		  Point2D.Double extent = getExtent();
		  setPos(pos.x+x,pos.y+y);
		  setExtent(extent.x+x,extent.y+y);
	}

	public RNATemplateHelix getHelix() {
		return _h;
	}
	

	public EdgeEndPoint getEndPoint(RelativePosition r) {
		switch(r)
		{
			case RP_CONNECT_START5:
    			return _h.getIn1();				
			case RP_CONNECT_START3:
    			return _h.getOut2();				
			case RP_CONNECT_END3:
				return _h.getIn2();
			case RP_CONNECT_END5:
				return _h.getOut1();
		}
		return null;
	}

	public boolean isIn(RelativePosition r) {
		switch(r)
		{
		case RP_CONNECT_START5:
			return true;				
		case RP_CONNECT_START3:
			return false;				
		case RP_CONNECT_END3:
			return true;
		case RP_CONNECT_END5:
			return false;
		}
		return true;
	}
	
	
	public void attach(GraphicalTemplateElement e, RelativePosition edgeOrig, RelativePosition edgeDest) throws ExceptionInvalidRNATemplate
	{
		super.attach(e,edgeOrig,edgeDest);
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
	


	public void detach(RelativePosition edge)
	{
		// If the underlying template element is still connected, disconnect it
		if (getEndPoint(edge).isConnected())
		{
			getEndPoint(edge).disconnect();
		}
		
		// Call the parent class detach function, which will also take care to disconnect this other endpoint of this edge
		super.detach(edge);
	}

	public void setEdgePosition(RelativePosition edge, java.awt.geom.Point2D.Double pos) {
		switch (edge)
		{
			case RP_EDIT_START:
				setPos(pos);
				break;
			case RP_EDIT_END:
				setExtent(pos);
				break;
			case RP_INNER_MOVE:
				moveCenter(pos.x,pos.y);
				break;
		}
		updateAttachedUnpairedRegions();
	}

	public ArrayList<RelativePosition> getConnectedEdges() {
		ArrayList<RelativePosition> result = new ArrayList<RelativePosition>();
		result.add(RelativePosition.RP_CONNECT_START5);
		result.add(RelativePosition.RP_CONNECT_START3);
		result.add(RelativePosition.RP_CONNECT_END5);
		result.add(RelativePosition.RP_CONNECT_END3);
		return result;
	}
	
	public String toString()
	{
		return "Helix " + getCaption();
	}

	public RelativePosition relativePositionFromEdgeEndPointPosition(
			EdgeEndPointPosition pos) {
		switch (pos) {
		case IN1:
			return RelativePosition.RP_CONNECT_START5;
		case OUT1:
			return RelativePosition.RP_CONNECT_END5;
		case IN2:
			return RelativePosition.RP_CONNECT_END3;
		case OUT2:
			return RelativePosition.RP_CONNECT_START3;
		default:
			return null;
		}
	}

}

