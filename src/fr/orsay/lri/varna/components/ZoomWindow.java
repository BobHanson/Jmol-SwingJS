package fr.orsay.lri.varna.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.JPanel;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.export.SwingGraphics;
import fr.orsay.lri.varna.models.export.VueVARNAGraphics;

public class ZoomWindow extends JPanel implements ImageObserver, Runnable, MouseMotionListener, MouseListener {

	VARNAPanel _vp = null;
	BufferedImage _bi = null;
	Rectangle2D.Double rnaRect = null;
	
	public ZoomWindow(VARNAPanel vp)
	{
		_vp = vp;
		addMouseMotionListener(this);
		addMouseListener(this);
	}
	
	
	public synchronized void setPanel(VARNAPanel vp)
	{
		_vp = vp; 
	}
	
	
	
	public synchronized void drawPanel()
	{
		if (getWidth()>0 && getHeight()>0)
		{
			_bi= new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g2 = _bi.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			VueVARNAGraphics g2D = new SwingGraphics(g2);
			rnaRect =_vp.renderRNA(g2D,new Rectangle2D.Double(0,0,getWidth(),getHeight()),false,true);
			
			Point2D.Double p1 = _vp.panelToLogicPoint(new Point2D.Double(0.,0.));
			Point2D.Double p2 = _vp.panelToLogicPoint(new Point2D.Double(_vp.getWidth(),_vp.getHeight()));
			
			double w = p2.x-p1.x; 
			double h = p2.y-p1.y;
						
			Rectangle2D.Double rnaBox = _vp.getRNA().getBBox();
			
			double ratiox = w/rnaBox.width;
			double ratioy = h/rnaBox.height;
			
			Rectangle2D.Double rvisible = new Rectangle2D.Double(rnaRect.x+rnaRect.width*(double)(p1.x-rnaBox.x)/(double)rnaBox.width,
					rnaRect.y+rnaRect.height*(double)(p1.y-rnaBox.y)/(double)rnaBox.height,
					ratiox*rnaRect.width,
					ratioy*rnaRect.height);
			
			//g2D.drawRect(rleft.x,rleft.y,rleft.width,rleft.height);
			
			Color shade = new Color(.9f,.9f,.9f,.4f);
			
			g2.setStroke(new BasicStroke(1.0f));

			g2.setColor(shade);
			
			/*Polygon north = new Polygon(new int[]{0,getWidth(),(int)rvisible.x,
					(int)(rvisible.x+rvisible.width+1),},new int[]{},1);*/
			g2.fillRect(0,0,getWidth(),(int)rvisible.y);
			g2.fillRect(0,(int)rvisible.y,(int)rvisible.x,(int)rvisible.height+1);
			g2.fillRect((int)(rvisible.x+rvisible.width),(int)rvisible.y,(int)(getHeight()-(rvisible.x+rvisible.width)),(int)(rvisible.height+1));
			g2.fillRect(0,(int)(rvisible.y+rvisible.height),getWidth(),(int)(getHeight()-(rvisible.y+rvisible.height)));

			g2.setColor(new Color(.7f,.7f,.7f,.3f));
			g2.draw(rvisible);
			g2.drawLine(0,
					0,
					(int)rvisible.x,
					(int)rvisible.y);
			g2D.drawLine(getWidth(),
					0,
					rvisible.x+rvisible.width,
					rvisible.y);
			g2D.drawLine(getWidth(),
					getHeight(),
					rvisible.x+rvisible.width,
					rvisible.y+rvisible.height);
			g2D.drawLine(0,
					getHeight(),
					rvisible.x,
					rvisible.y+rvisible.height);

			g2.dispose();
		}
	}
	
	public void paintComponent(Graphics g)
	{
		setBackground(_vp.getBackground());
		super.paintComponent(g);
		drawPanel();
		if (_bi!=null)
		{
			g.drawImage(_bi,0,0,this);
		}
	}

	public void run() {
		while(true)
		{
			repaint();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}


	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void mouseClicked(MouseEvent e) {
		/*if (rnaRect!=null)
		{
			int x= e.getX();
			int y= e.getY();
			double ratioX = ((double)(x-rnaRect.getMinX())/((double)rnaRect.width));
			double ratioY = ((double)(y-rnaRect.getMinY())/((double)rnaRect.height));
			_vp.centerViewOn(ratioX,ratioY );
		}*/
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
