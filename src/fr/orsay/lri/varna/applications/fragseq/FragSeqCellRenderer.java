package fr.orsay.lri.varna.applications.fragseq;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

class FragSeqCellRenderer extends DefaultTreeCellRenderer {  
	   
	JTree _j;
	FragSeqTreeModel _m;
	
	private static FragSeqCellRenderer _default = new FragSeqCellRenderer(null,null);

	

	public FragSeqCellRenderer (JTree j, FragSeqTreeModel m)
	{
		_j = j;
		_m = m;
	}
	
	public JComponent baseElements(JTree tree,FragSeqTreeModel m,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row, boolean hasFocus)
	{
		JLabel initValue = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        initValue.setBorder(null);
        result.setBorder(null);
        result.setBackground(initValue.getBackground());
        /*if (hasFocus)
        { 
        	//renderer.setBackground(Color.blue);            
        	//result.setBorder(BorderFactory.createLineBorder(Color.blue));
            result.setBackground(UIManager.getColor("Tree.selectionBackground"));
            result.setBorder(BorderFactory.createLineBorder(initValue.getBackground()));
            initValue.setOpaque(true);
        }
        else
        {
        	result.setBackground(Color.white);
        	result.setBorder(BorderFactory.createLineBorder(initValue.getBackground()));
            
        }*/
        DefaultMutableTreeNode t = (DefaultMutableTreeNode)value;
        Object o = t.getUserObject();
        if (( o instanceof String))    
        {  
            if (expanded)  
            {  
            	initValue.setIcon(_default.getOpenIcon());  
            }  
            else  
            {  
            	initValue.setIcon(_default.getClosedIcon());  
            }  
            result.add(initValue,BorderLayout.WEST);
        	JButton del = new JButton();
        	del.setIcon(new SimpleIcon(Color.red,26,false));
        	Dimension d = getPreferredSize();
        	d.width=24;
        	del.setPreferredSize(d);
        	del.addActionListener(new FolderCloses((String)o,tree,m));
            result.add(del,BorderLayout.EAST);
        }  
        else  if (( o instanceof FragSeqRNASecStrModel))
        {  
        	initValue.setIcon(new SimpleIcon(Color.blue.darker()));  
        	result.add(initValue,BorderLayout.WEST);
        }
        else  if (( o instanceof FragSeqFileModel))
        {  
        	initValue.setIcon(_default.getLeafIcon());  
            FragSeqFileModel mod = (FragSeqFileModel) o;
            result.add(initValue,BorderLayout.WEST);
            if (mod.hasChanged())
            {
        	  JButton refresh = new JButton("Refresh");
              result.add(refresh,BorderLayout.EAST);
            }
        }
        else  if (( o instanceof FragSeqModel))
        {
        	FragSeqModel mod = (FragSeqModel) o;
        	initValue.setIcon(new SimpleIcon());  
            result.add(initValue,BorderLayout.WEST);        	
        }
        return result;
	}
	
    public Component getDefaultTreeCellRendererComponent(JTree tree,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row, boolean hasFocus)  
    {
      return super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
    }

	public Component getTreeCellRendererComponent(JTree tree,  
            Object value, boolean sel, boolean expanded, boolean leaf,  
            int row, boolean hasFocus)  
    {
    	
        return baseElements(tree,_m,value,sel,expanded,leaf,row,hasFocus);  
    }  
    public Dimension getPreferredSize(int row) {
        Dimension size = super.getPreferredSize();
        size.width = _j.getWidth();
        System.out.println(size);
        return size;
    }


//    @Override
//    public void setBounds(final int x, final int y, final int width, final int height) {
//    super.setBounds(x, y, Math.min(_j.getWidth()-x, width), height);
//    }
    
    
    public class FolderCloses implements ActionListener{
    	String _path;
    	JComponent _p;
    	FragSeqTreeModel _m;
    	
    	public FolderCloses(String path, JComponent p, FragSeqTreeModel m)
    	{
    		_path = path;
    		_p = p;
    		_m = m;
    	}
		public void actionPerformed(ActionEvent e) {
			if (JOptionPane.showConfirmDialog(_p, "This folder will cease to be watched. Confirm?", "Closing folder", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
			{
				_m.removeFolder(_path);
				System.out.println(_j);
				_j.updateUI();
			}
		}
		    
    }

    
    public class SimpleIcon implements Icon{

        private int _w = 16;
        private int _h = 16;

        private BasicStroke stroke = new BasicStroke(3);
        private Color _r;
        private boolean _drawBackground = true;

        public SimpleIcon()
        {
        	this(Color.magenta.darker());
        }
 
        public SimpleIcon(Color r)
        {
        	this(r,16,true);
        }
        public SimpleIcon(Color r, int dim, boolean drawBackground)
        {
        	this(r,dim,dim,drawBackground);
        }

        public SimpleIcon(Color r, int width, int height,boolean drawBackground)
        {
        	_r=r;
        	_w=width;
        	_h=height;
        	_drawBackground=drawBackground;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();

            if (_drawBackground)
            {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(x +1 ,y + 1,_w -2 ,_h -2);

            g2d.setColor(Color.BLACK);
            g2d.drawRect(x +1 ,y + 1,_w -2 ,_h -2);
            }

            g2d.setColor(_r);

            g2d.setStroke(stroke);
            g2d.drawLine(x +10, y + 10, x + _w -10, y + _h -10);
            g2d.drawLine(x +10, y + _h -10, x + _w -10, y + 10);

            g2d.dispose();
        }

        public int getIconWidth() {
            return _w;
        }

        public int getIconHeight() {
            return _h;
        }
    }
}  