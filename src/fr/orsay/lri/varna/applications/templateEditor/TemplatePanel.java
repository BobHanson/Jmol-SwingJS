/**
 * 
 */
package fr.orsay.lri.varna.applications.templateEditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.undo.UndoManager;

import fr.orsay.lri.varna.applications.templateEditor.GraphicalTemplateElement.RelativePosition;
import fr.orsay.lri.varna.controlers.ControleurMolette;
import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.exceptions.ExceptionXmlLoading;
import fr.orsay.lri.varna.models.templates.RNATemplate;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateUnpairedSequence;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement.EdgeEndPoint;




/**
 * @author ponty
 *
 */
public class TemplatePanel extends JPanel {
  /**
	 * 
	 */
	private static final long serialVersionUID = 3162771335587335679L;
	
	
	private ArrayList<GraphicalTemplateElement> _RNAComponents;
	private ArrayList<Connection> _RNAConnections;
	private Hashtable<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Connection> _helixToConnection;
	private TemplateEditorPanelUI _ui;
	
	private RNATemplate _template;

	
	private static Color[] BackgroundColors = {Color.blue,Color.red,Color.cyan,Color.green,Color.lightGray,Color.magenta,Color.PINK};

	private int _nextBackgroundColor = 0;
	
	private static double scaleFactorDefault = 0.7;
	private double scaleFactor = scaleFactorDefault;
	
	
	private TemplateEditor _editor;
	
	
	public double getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public Color nextBackgroundColor()
	{
		Color c = BackgroundColors[_nextBackgroundColor++];
		_nextBackgroundColor = _nextBackgroundColor % BackgroundColors.length;
		return new Color(c.getRed(),c.getBlue(),c.getGreen(),50);
	}
	
	public TemplatePanel(TemplateEditor parent)
	{
		_editor = parent;
		init();
	}

	public RNATemplate getTemplate()
	{
		return _template;
	}
	
	List<GraphicalTemplateElement> getRNAComponents() {
		return _RNAComponents;
	}
	
	private void init()
	{
		_ui = new TemplateEditorPanelUI(this); 
		
		_RNAComponents = new ArrayList<GraphicalTemplateElement>();
		_RNAConnections = new ArrayList<Connection>();
		_helixToConnection = new Hashtable<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Connection>();
		
		_template = new RNATemplate();
		
		setBackground(Color.WHITE);
		MouseControler mc = new MouseControler(this,_ui); 
		addMouseListener(mc);
		addMouseMotionListener(mc);
		addMouseWheelListener(mc);
		_solidStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND, 3.0f);
		float[] dash = { 5.0f, 5.0f };
		_dashedStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND, 3.0f, dash, 0);
	}
	
	public void addUndoableEditListener(UndoManager manager)
	{
		_ui.addUndoableEditListener(manager);
	}
	
	public TemplateEditorPanelUI getTemplateUI()
	{
		return _ui;
	}

	
	public void flip(Helix h)
	{ 
		h.toggleFlipped(); 
	}
	
	public void addElement(GraphicalTemplateElement h)
	{ 
		_RNAComponents.add(h); 
	}

	public void removeElement(GraphicalTemplateElement h)
	{ 
		_RNAComponents.remove(h);
		try {
			_template.removeElement(h.getTemplateElement());
		} catch (ExceptionInvalidRNATemplate e) {
			//e.printStackTrace();
		}
	}

	private GraphicalTemplateElement _selected = null;
	
	public GraphicalTemplateElement getSelected()
	{
		return _selected;
	}

	public void setSelected(GraphicalTemplateElement sel)
	{
		_selected = sel;
		if (_selected instanceof Helix) {
			_editor.flipButtonEnable();
		} else {
			_editor.flipButtonDisable();
		}
	}

	Helix.RelativePosition _relpos = Helix.RelativePosition.RP_OUTER;
	
	public void setSelectedEdge(Helix.RelativePosition rel)
	{
		_relpos = rel;
	}

	public void unselectEdge(Helix.RelativePosition rel)
	{
		_relpos = rel;
	}
	
	Point2D.Double _mousePos = new Point2D.Double();

	public void setPointerPos(Point2D.Double p)
	{
		_mousePos = p;
	}	
	
	public void Unselect()
	{
		_editor.flipButtonDisable();
		_selected = null;
	}

	public GraphicalTemplateElement getElement(RNATemplateElement t)
	{
		for(GraphicalTemplateElement t2: _RNAComponents)
			if (t==t2.getTemplateElement())
				return t2;
		return null;
	}

	
	public GraphicalTemplateElement getElementAt(double x, double y)
	{
		return getElementAt(x, y, null);
	}
	public GraphicalTemplateElement getElementAt(double x, double y, GraphicalTemplateElement excluded)
	{
		GraphicalTemplateElement h = null;
		for (int i=0; i<_RNAComponents.size();i++)
		{
			GraphicalTemplateElement h2 = _RNAComponents.get(i);
			if ((
					   (h2.getRelativePosition(x, y)== Helix.RelativePosition.RP_CONNECT_END3)
					|| (h2.getRelativePosition(x, y)== Helix.RelativePosition.RP_CONNECT_END5)
					|| (h2.getRelativePosition(x, y)== Helix.RelativePosition.RP_CONNECT_START3)
					|| (h2.getRelativePosition(x, y)== Helix.RelativePosition.RP_CONNECT_START5))
				&& (excluded!=h2))
			{
				h = h2;
			}
		}
		if (h==null)
		{ h = getElementCloseTo(x, y, excluded);};
		return h;
	}

	public GraphicalTemplateElement getElementCloseTo(double x, double y)
	{
		return getElementCloseTo(x, y, null);
	}
	public GraphicalTemplateElement getElementCloseTo(double x, double y, GraphicalTemplateElement excluded)
	{
		GraphicalTemplateElement h = null;
		for (int i=0; i<_RNAComponents.size();i++)
		{
			GraphicalTemplateElement h2 = _RNAComponents.get(i);
			if ((h2.getRelativePosition(x, y) != Helix.RelativePosition.RP_OUTER)
				&& (excluded!=h2))
			{
				h = h2;
			}
		}
		return h;
	}
	
	public void addConnection(Connection c)
	{
		_RNAConnections.add(c);
		_helixToConnection.put(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h1,c._edge1), c);
		_helixToConnection.put(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h2,c._edge2), c);
			try {
				c._h1.attach(c._h2, c._edge1, c._edge2);
				c._h2.attach(c._h1, c._edge2, c._edge1);
			} catch (ExceptionInvalidRNATemplate e) {
				System.out.println(e.toString());// TODO Auto-generated catch block
		}

	}

	public Connection addConnection(GraphicalTemplateElement h1, GraphicalTemplateElement.RelativePosition edge1,GraphicalTemplateElement h2, GraphicalTemplateElement.RelativePosition edge2)
	{
		if ((h1!=h2)&&(getPartner(h1,edge1)==null)&&(getPartner(h2,edge2)==null))
		{
			Connection c = new Connection(h1,edge1,h2,edge2);
			addConnection(c);
			return c;
		}
		return null;
	}
	
	/**
	 * When there is already a connection in the underlying RNATemplate
	 * and we want to create one at the graphical level.
	 */
	public void addGraphicalConnection(GraphicalTemplateElement h1, GraphicalTemplateElement.RelativePosition edge1,GraphicalTemplateElement h2, GraphicalTemplateElement.RelativePosition edge2) {
		//System.out.println("Connecting " + h1 + " " + edge1 + " to " + h2 + " " + edge2);
		Connection c = new Connection(h1,edge1,h2,edge2);
		_RNAConnections.add(c);
		_helixToConnection.put(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h1,c._edge1), c);
		_helixToConnection.put(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h2,c._edge2), c);
		c._h1.graphicalAttach(c._h2, c._edge1, c._edge2);
		c._h2.graphicalAttach(c._h1, c._edge2, c._edge1);
	}
	

	public void removeConnection(Connection c)
	{
		_RNAConnections.remove(c);
		_helixToConnection.remove(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h1,c._edge1));
		_helixToConnection.remove(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h2,c._edge2));
		System.out.println("[A]"+c);
		c._h1.detach(c._edge1);
	}
	
	public boolean isInCycle(GraphicalTemplateElement el, GraphicalTemplateElement.RelativePosition edge)
	{
		Stack<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> > p = new Stack<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>>();
		Hashtable<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Integer> alreadySeen = new Hashtable<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Integer>(); 
		p.add(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(el,edge));
		while(!p.empty())
		{
			Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> c2 = p.pop();
			if (alreadySeen.containsKey(c2))
			{
				return true;
			}
			else
			{
				alreadySeen.put(c2, new Integer(1));
			}
			GraphicalTemplateElement.RelativePosition next = c2.first.getConnectedEdge(c2.second);
			Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> otherEnd = new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c2.first,next);
			if (!alreadySeen.containsKey(otherEnd))
			{
				p.push(otherEnd);
			}
			else
			{
				Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> child =  getPartner(c2.first,c2.second);
				if (child!=null)
				{
					p.push(child);
				}
			}
			
		}
		
		return false;	
	}
	
	private static Color[] _colors = {Color.gray,Color.pink,Color.cyan,Color.RED,Color.green,Color.orange};
	
	public static Color getIndexedColor(int n)
	{
		return _colors[n%_colors.length];
	}
	
	public HashMap<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Integer> buildConnectedComponents()
	{
		HashMap<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Integer> alreadySeen = new HashMap<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>,Integer>();
		int numConnectedComponents = 0;
		for (GraphicalTemplateElement el : this._RNAComponents)
		{
			for (GraphicalTemplateElement.RelativePosition edge : el.getConnectedEdges())
			{
				Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> c = new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(el,edge);
				if (!alreadySeen.containsKey(c))
				{
					Stack<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> > p = new Stack<Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>>();
					p.add(c);
					p.add(new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(el,el.getConnectedEdge(edge)));
					while(!p.empty())
					{
						Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> c2 = p.pop();
						if (!alreadySeen.containsKey(c2))
						{
							//System.out.println("  "+numConnectedComponents+"  "+c2);
							c2.first.setMainColor(c2.second, getIndexedColor(numConnectedComponents));
							alreadySeen.put(c2, new Integer(numConnectedComponents));
							GraphicalTemplateElement.RelativePosition next = c2.first.getConnectedEdge(c2.second);
							Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> otherEnd = new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c2.first,next);
							p.push(otherEnd);
							Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> child =  getPartner(c2.first,c2.second);
							if (child!=null)
							{ p.push(child); }
						}	
					}
					numConnectedComponents += 1;
				}
			}
		}
		return alreadySeen;
	}
	
	public boolean isInCycle(Connection c)
	{
		return isInCycle(c._h1,c._edge1); 
	}
	
	public Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> getPartner(GraphicalTemplateElement h, GraphicalTemplateElement.RelativePosition edge)
	{
		Connection c = getConnection(h, edge);
		if (c != null)
		{
			  if ((c._h1==h)&&(c._edge1==edge))
			  {  return new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h2,c._edge2);  }
			  else 
			  {  return new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(c._h1,c._edge1); }
		}
		else
		{
			return null;
		}
	}

	public Connection getConnection(GraphicalTemplateElement h, Helix.RelativePosition edge)
	{
		Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> target = new Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition>(h,edge);
		if (_helixToConnection.containsKey(target))
		{
			return _helixToConnection.get(target);
		}
		else
		{ return null; }
	}
	
	private boolean isConnected(Helix h, GraphicalTemplateElement.RelativePosition edge)
	{
		Couple<GraphicalTemplateElement,GraphicalTemplateElement.RelativePosition> partner = getPartner(h,edge);
		return partner!=null;
	}
	

	// Aspects graphiques
	
	private static final Color  CYCLE_COLOR = Color.red;
	private static final Color  NON_EXISTANT_COLOR = Color.gray.brighter();
	private static final Color  CONTROL_COLOR = Color.gray.darker();
	private static final Color  BACKGROUND_COLOR = Color.white;
	
	private Stroke  _solidStroke;
	private Stroke  _dashedStroke;
	

	
	
	private void drawConnections(Graphics2D g2d, Connection c)
	{
		GraphicalTemplateElement h1 = c._h1;
		GraphicalTemplateElement.RelativePosition edge1 = c._edge1;
		Point2D.Double p1 = h1.getEdgePosition(edge1);
		GraphicalTemplateElement h2 = c._h2;
		GraphicalTemplateElement.RelativePosition edge2 = c._edge2;
		Point2D.Double p2 = h2.getEdgePosition(edge2);
		if (isInCycle(c))
		{
			g2d.setColor(CYCLE_COLOR);
		}
		else
		{
			g2d.setColor(GraphicalTemplateElement.BACKBONE_COLOR);
		}
		g2d.drawLine((int)p1.x,(int)p1.y,(int)p2.x,(int)p2.y);
	}
	

	public void paintComponent(Graphics g)
	{
		//rescale();
		
		// Debug code to show drawing area
//		g.setColor(Color.red);
//		g.fillRect(0,0,getWidth(),getHeight());
//		g.setColor(Color.white);
//		g.fillRect(10,10,getWidth()-20,getHeight()-20);
		
		g.setColor(Color.white);
		g.fillRect(0,0,getWidth(),getHeight());
		
		Graphics2D g2d = (Graphics2D) g;
		g2d.scale(scaleFactor, scaleFactor);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		removeAll();
		//super.paintComponent(g2d);

		buildConnectedComponents();
		
		if (_selected!=null)
		{
			if (_relpos != GraphicalTemplateElement.RelativePosition.RP_OUTER)
			{	
			Point2D.Double p = _selected.getEdgePosition(_relpos);
			g2d.setStroke(_solidStroke);
			g2d.drawLine((int)_mousePos.x, (int)_mousePos.y, (int)p.x, (int)p.y);
			}
		}
		for (int i=0;i<_RNAConnections.size();i++)
		{
			Connection c = _RNAConnections.get(i);
			drawConnections(g2d,c);
		}
		for (int i=0;i<_RNAComponents.size();i++)
		{
			GraphicalTemplateElement elem = _RNAComponents.get(i);
			//g2d.setColor(elem.getDominantColor());
			//g2d.fill(elem.getArea());
			if (_selected == elem)
			{  
				elem.draw(g2d,true); 
			}
			else
			{  elem.draw(g2d,false);  }
		}
	}
	
	/**
	 * Get the bounding rectangle of the RNA components, always including the origin (0,0).
	 */
	public Rectangle getBoundingRectange() {
		int minX = 0;
		int maxX = 0;
		int minY = 0;
		int maxY = 0;
		for(int i=0;i<this._RNAComponents.size();i++)
		{
			GraphicalTemplateElement h = _RNAComponents.get(i);
			Polygon p = h.getBoundingPolygon();
			Rectangle r = p.getBounds();
			minX = Math.min(minX,r.x);
			maxX = Math.max(maxX,r.x+r.width);
			minY = Math.min(minY,r.y);
			maxY = Math.max(maxY,r.y+r.height);
		}
		Rectangle res = new Rectangle();
		res.x = minX;
		res.y = minY;
		res.width = maxX - minX;
		res.height = maxY - minY;
		return res;
	}

	public void rescale()
	{
		Rectangle rect = getBoundingRectange();
		
		if (rect.x < 0 || rect.y < 0) {
            for(int i=0;i<this._RNAComponents.size();i++)
            {
                    GraphicalTemplateElement h = _RNAComponents.get(i);
                    h.translate(rect.x<0 ? -rect.x : 0, rect.y<0 ? -rect.y : 0);
            }
            rect = getBoundingRectange();
		}
		
		// areaW and H are width and height, in pixels, of the drawing area
		// including the "outside" parts available with scrolling
		int areaW = (int) ((rect.width + 100) * scaleFactor);
		int areaH = (int) ((rect.height + 100) * scaleFactor);
		// make it cover at least the space visible in the window
		//areaW = Math.max(areaW, (int) (_editor.getJp().getViewport().getViewSize().width));
		//areaH = Math.max(areaH, (int) (_editor.getJp().getViewport().getViewSize().height));
		//System.out.println(areaW + " x " + areaH);
		setPreferredSize(new Dimension(areaW, areaH));
		revalidate();
	}

	public void clearTemplate() {
		loadTemplate(new RNATemplate());
	}

	/**
	 * Load an existing RNATemplate object in this panel.
	 */
	public void loadTemplate(RNATemplate template) {
		_template = template;
		_RNAComponents.clear();
		_RNAConnections.clear();
		_helixToConnection.clear();
		
		// We need a template element -> graphical template element mapping
		Map<RNATemplateElement, GraphicalTemplateElement> map = new HashMap<RNATemplateElement, GraphicalTemplateElement>();
		
		// First, we load elements
		{
			Iterator<RNATemplateElement> iter = template.classicIterator();
			while (iter.hasNext()) {
				RNATemplateElement templateElement = iter.next();
				if (templateElement instanceof RNATemplateHelix) {
					RNATemplateHelix templateHelix = (RNATemplateHelix) templateElement;
					Helix graphicalHelix = new Helix(templateHelix);
					graphicalHelix.setDominantColor(nextBackgroundColor());
					_RNAComponents.add(graphicalHelix);
					map.put(templateHelix, graphicalHelix);
				} else if (templateElement instanceof RNATemplateUnpairedSequence) {
					RNATemplateUnpairedSequence templateSequence = (RNATemplateUnpairedSequence) templateElement;
					UnpairedRegion graphicalSequence = new UnpairedRegion(templateSequence);
					graphicalSequence.setDominantColor(nextBackgroundColor());
					_RNAComponents.add(graphicalSequence);
					map.put(templateSequence, graphicalSequence);
				}
			}
		}
		
		// Now, we load edges
		{
			Iterator<EdgeEndPoint> iter = template.makeEdgeList().iterator();
			while (iter.hasNext()) {
				EdgeEndPoint v1 = iter.next();
				EdgeEndPoint v2 = v1.getOtherEndPoint();
				GraphicalTemplateElement gte1 = map.get(v1.getElement());
				GraphicalTemplateElement gte2 = map.get(v2.getElement());
				RelativePosition rp1 = gte1.relativePositionFromEdgeEndPointPosition(v1.getPosition());
				RelativePosition rp2 = gte2.relativePositionFromEdgeEndPointPosition(v2.getPosition());
				addGraphicalConnection(gte1, rp1, gte2, rp2);
			}
		}
		
		zoomFit();
		//repaint();
	}

	/**
	 * Load a template from an XML file.
	 */
	public void loadFromXmlFile(File filename) {
		try {
			RNATemplate newTemplate = RNATemplate.fromXMLFile(filename);
			loadTemplate(newTemplate);
		} catch (ExceptionXmlLoading e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getMessage(), "Template loading error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void zoomFinish() {
		rescale();
		repaint();
	}
	
	public void zoomIn() {
		scaleFactor *= 1.2;
		zoomFinish();
	}
	
	public void zoomOut() {
		scaleFactor /= 1.2;
		zoomFinish();
	}
	
	public void zoomReset() {
		scaleFactor = scaleFactorDefault;
		zoomFinish();
	}
	
	public void zoomFit() {
		if (_RNAComponents.isEmpty()) {
			zoomReset();
		} else {
			Rectangle rect = getBoundingRectange();
			double areaW = (rect.width + 100);
			double areaH = (rect.height + 100);
			// make it cover at least the space visible in the window
			scaleFactor = 1;
			scaleFactor = Math.min(scaleFactor, _editor.getJp().getViewport().getSize().width / areaW);
			scaleFactor = Math.min(scaleFactor, _editor.getJp().getViewport().getSize().height / areaH);
			zoomFinish();
		}
	}
	
	public void translateView(Point trans) {
		int newX = _editor.getJp().getHorizontalScrollBar().getValue() - trans.x;
		int newY = _editor.getJp().getVerticalScrollBar().getValue() - trans.y;
		newX = Math.max(0, Math.min(newX, _editor.getJp().getHorizontalScrollBar().getMaximum()));
		newY = Math.max(0, Math.min(newY, _editor.getJp().getVerticalScrollBar().getMaximum()));
		_editor.getJp().getHorizontalScrollBar().setValue(newX);
		_editor.getJp().getVerticalScrollBar().setValue(newY);
	}
}
