package fr.orsay.lri.varna.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JColorChooser;
import javax.swing.JPanel;

import fr.orsay.lri.varna.models.rna.ModeleColorMap;

public class GradientEditorPanel extends JPanel implements MouseListener, MouseMotionListener{
		ModeleColorMap _mcm;
		
		public GradientEditorPanel (ModeleColorMap mcm)
		{ 
			_mcm = mcm;
			this.addMouseListener( this);
			this.addMouseMotionListener(this);
		}
		
		public void setColorMap(ModeleColorMap mcm)
		{
			_mcm = mcm;
			repaint();
			firePropertyChange("PaletteChanged","a","b");
		}
		
		public ModeleColorMap getColorMap()
		{
			return _mcm;
		}

		private final static int TRIGGERS_SEMI_WIDTH = 2;
		private final static int PALETTE_HEIGHT = 11;
		private final static int REMOVE_HEIGHT = 11;
		private final static int TOLERANCE = 5;
		private final static int GAP = 4;
		private final Color EDGES = Color.gray.brighter();
		private final Color BUTTONS = Color.LIGHT_GRAY.brighter();
		
		public int getStartChoose()
		{
			return getHeight()-PALETTE_HEIGHT-REMOVE_HEIGHT-GAP-1;
		}
		
		public int getEndChoose()
		{
			return getStartChoose()+PALETTE_HEIGHT;
		}
		
		public int getStartRemove()
		{
			return getEndChoose()+GAP;
		}

		public int getEndRemove()
		{
			return getStartRemove()+REMOVE_HEIGHT;
		}
		
		private int getStripeHeight()
		{
			return getHeight()-PALETTE_HEIGHT-REMOVE_HEIGHT-2*GAP-1;
		}
		
		private Color alterColor(Color c, int inc)
		{
			int nr = Math.min(Math.max(c.getRed()+inc, 0),255);
			int ng = Math.min(Math.max(c.getGreen()+inc, 0),255);
			int nb = Math.min(Math.max(c.getBlue()+inc, 0),255);
			return new Color(nr,ng,nb);
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D)  g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			int height = getStripeHeight();
			double v1 = _mcm.getMinValue();
			double v2 = _mcm.getMaxValue();
			for (int i=0;i<=getWidth();i++)
			{
				double ratio = (((double)i)/((double)getWidth()));
				double val = v1+(v2-v1)*ratio;
				g2d.setColor(_mcm.getColorForValue(val));
				g2d.drawLine(i, 0, i, height);	
			}
			for(int i=0;i<_mcm.getNumColors();i++)
			{
				double val = _mcm.getValueAt(i);
				Color c = _mcm.getColorAt(i);
				double norm = (val-_mcm.getMinValue())/(_mcm.getMaxValue()-_mcm.getMinValue());
				int x = (int)(norm*(getWidth()-1));
				// Target
				g2d.setColor(c);
				g2d.fillRect(x-TRIGGERS_SEMI_WIDTH+1, 0, 2*TRIGGERS_SEMI_WIDTH-1, getHeight()-1);					
				g2d.setColor(EDGES);
				g2d.drawLine(x-TRIGGERS_SEMI_WIDTH, 0, x-TRIGGERS_SEMI_WIDTH, getHeight());					
				g2d.drawLine(x+TRIGGERS_SEMI_WIDTH, 0, x+TRIGGERS_SEMI_WIDTH, getHeight());
				
				if (i==0)
				{
					// Choose Color
					g2d.setColor(EDGES);
					g2d.drawRect(x,height+GAP,PALETTE_HEIGHT,2*PALETTE_HEIGHT+GAP);					
					g2d.setColor(c);
					g2d.fillRect(x+1,height+GAP+1,PALETTE_HEIGHT-1,2*PALETTE_HEIGHT+GAP-1);
				}
				else if (i==_mcm.getNumColors()-1)
				{
					// Choose Color
					g2d.setColor(EDGES);
					g2d.drawRect(x-PALETTE_HEIGHT,height+GAP,PALETTE_HEIGHT,2*PALETTE_HEIGHT+GAP);					
					g2d.setColor(c);
					g2d.fillRect(x-PALETTE_HEIGHT+1,height+GAP+1,PALETTE_HEIGHT-1,2*PALETTE_HEIGHT+GAP-1);					
				}
				else
				{
				// Choose Color
				g2d.setColor(EDGES);
				g2d.drawRect(x-PALETTE_HEIGHT/2,height+GAP,PALETTE_HEIGHT,PALETTE_HEIGHT);					
				g2d.setColor(alterColor(c,-15));
				g2d.fillRect(x-PALETTE_HEIGHT/2+1,height+GAP+1,PALETTE_HEIGHT-1,PALETTE_HEIGHT-1);					
				g2d.setColor(c);
				g2d.fillOval(x-PALETTE_HEIGHT/2+1,height+GAP+1,PALETTE_HEIGHT-1,PALETTE_HEIGHT-1);
				g2d.setColor(alterColor(c,10));
				g2d.fillOval(x-PALETTE_HEIGHT/2+1+2,height+GAP+1+2,PALETTE_HEIGHT-1-4,PALETTE_HEIGHT-1-4);


				// Remove Color
				g2d.setColor(EDGES);
				g2d.drawRect(x-PALETTE_HEIGHT/2,height+2*GAP+PALETTE_HEIGHT,REMOVE_HEIGHT,REMOVE_HEIGHT);					
				g2d.setColor(BUTTONS);
				g2d.fillRect(x-PALETTE_HEIGHT/2+1,height+2*GAP+1+PALETTE_HEIGHT,REMOVE_HEIGHT-1,REMOVE_HEIGHT-1);						
				int xcross1 = x-PALETTE_HEIGHT/2+2;
				int ycross1 = height+2*GAP+PALETTE_HEIGHT +2;
				int xcross2 = xcross1+REMOVE_HEIGHT-4;
				int ycross2 = ycross1+REMOVE_HEIGHT-4;
				g2d.setColor(Color.red);
				g2d.drawLine(xcross1, ycross1, xcross2 , ycross2);				
				g2d.drawLine(xcross1, ycross2, xcross2 , ycross1);
				}
			}

		}
		
		private boolean isChooseColor(int x, int y)
		{
			if (_selectedIndex != -1)
			{
				if ((_selectedIndex ==0)||(_selectedIndex == _mcm.getNumColors()-1))
					return (y<=getEndRemove() && y>=getStartChoose() && Math.abs(getXPos(_selectedIndex)-x)<=PALETTE_HEIGHT);
				if (y<=getEndChoose() && y>=getStartChoose())
				{
					return Math.abs(getXPos(_selectedIndex)-x)<=PALETTE_HEIGHT/2;
				}
			}
			return false;
		}
		
		private boolean isRemove(int x, int y)
		{
			if (_selectedIndex != -1)
			{
				if ((_selectedIndex ==0)||(_selectedIndex == _mcm.getNumColors()-1))
					return false;
				if (y<=getEndRemove() && y>=getStartRemove())
				{
					return Math.abs(getXPos(_selectedIndex)-x)<=PALETTE_HEIGHT/2;
				}
			}
			return false;
		}
		
		private int getXPos(int i)
		{
			double val = _mcm.getValueAt(i);
			double norm = (val-_mcm.getMinValue())/(_mcm.getMaxValue()-_mcm.getMinValue());
			return (int)(norm*(getWidth()-1));			
		}

		
		private int locateSelectedIndex(int x, int y)
		{
			double dist = Double.MAX_VALUE;
			int index = -1;
			for(int i=0;i<_mcm.getNumColors();i++)
			{
				int xp = getXPos(i);
				double tmpDist = Math.abs(x-xp);
				if (tmpDist<dist)
				{
					index = i;
					dist = tmpDist; 
				}
			}	
			return index;
		}
		
		private int _selectedIndex = -1;
		
		public void mouseClicked(MouseEvent arg0) {
			_selectedIndex = locateSelectedIndex(arg0.getX(),arg0.getY());
			if (_selectedIndex!=-1)
			{
				if (isRemove(arg0.getX(),arg0.getY()))
				{
					removeEntry(_selectedIndex);
				}
				else if (Math.abs(getXPos(_selectedIndex)-arg0.getX())>TOLERANCE)
				{
					double val = _mcm.getMinValue()+ arg0.getX()*(_mcm.getMaxValue()-_mcm.getMinValue())/(getWidth()-1);
					Color nc = JColorChooser.showDialog(this, "Choose new color" ,_mcm.getColorAt(_selectedIndex));
					if (nc != null)
					{
						_mcm.addColor(val, nc);
						repaint();
						firePropertyChange("PaletteChanged","a","b");
					}					
				}
			}
		}

		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mousePressed(MouseEvent arg0) {
			requestFocus();
			_selectedIndex = locateSelectedIndex(arg0.getX(),arg0.getY());
			if (_selectedIndex!=-1)
			{
				if (isChooseColor(arg0.getX(),arg0.getY()))
				{
					Color nc = JColorChooser.showDialog(this, "Choose new color" ,_mcm.getColorAt(_selectedIndex));
					if (nc != null)
					{
						double nv = _mcm.getValueAt(_selectedIndex);
						replaceEntry(_selectedIndex, nc, nv);
						_selectedIndex = -1;
					}
				}
			}
		}

		public void mouseReleased(MouseEvent arg0) {
			_selectedIndex = -1;
		}

		private void replaceEntry(int index, Color nc, double nv)
		{
			ModeleColorMap cm = new ModeleColorMap();
			for(int i=0;i<_mcm.getNumColors();i++)
			{
				if (i!=index)
				{
					double val = _mcm.getValueAt(i);
					Color c = _mcm.getColorAt(i);
					cm.addColor(val, c);
				}
				else
				{
					cm.addColor(nv, nc);
				}
			}
			_mcm = cm;
			repaint();
			firePropertyChange("PaletteChanged","a","b");
		}

		private void removeEntry(int index)
		{
			ModeleColorMap cm = new ModeleColorMap();
			for(int i=0;i<_mcm.getNumColors();i++)
			{
				if (i!=index)
				{
					double val = _mcm.getValueAt(i);
					Color c = _mcm.getColorAt(i);
					cm.addColor(val, c);
				}
			}
			_mcm = cm;
			repaint();
			firePropertyChange("PaletteChanged","a","b");
		}
		
		
		public void mouseDragged(MouseEvent arg0) {
			if ((_selectedIndex!=-1)&&(_selectedIndex!=0)&&(_selectedIndex!=_mcm.getNumColors()-1))
			{
				Color c = _mcm.getColorAt(_selectedIndex);
				double val = _mcm.getMinValue()+ arg0.getX()*(_mcm.getMaxValue()-_mcm.getMinValue())/(getWidth()-1);
				replaceEntry(_selectedIndex, c, val);
			}
		}

		public void mouseMoved(MouseEvent arg0) {
			Cursor c = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
			_selectedIndex = locateSelectedIndex(arg0.getX(),arg0.getY());
			if (_selectedIndex!=-1)
			{
				if (isChooseColor(arg0.getX(),arg0.getY()))
				{
					c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
				}
				else if ((_selectedIndex != 0)&&(_selectedIndex != _mcm.getNumColors()-1))
				{
					if (isRemove(arg0.getX(),arg0.getY()))
					{
						c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);					
					}
					else if (Math.abs(getXPos(_selectedIndex)-arg0.getX())<=TOLERANCE)
					{
						c = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
					}
					else if (arg0.getY()<getHeight()-this.getStripeHeight())
					{
						c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);										
					}
					else
					{
						Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
					}
				}
			}
			setCursor(c);
		}
	}

