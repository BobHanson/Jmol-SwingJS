package fr.orsay.lri.varna.views;


	import java.awt.Color;
import java.awt.Dimension;
	import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JColorChooser;
	import javax.swing.JLabel;
import javax.swing.JOptionPane;
	import javax.swing.JPanel;
	import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBase;

	public class VueHighlightRegionEdit implements ChangeListener, ActionListener {

		private VARNAPanel _vp;
		private JSlider _fromSlider;
		private JSlider _toSlider;
		private JPanel panel;
		private HighlightRegionAnnotation _an;
		private static int CONTROL_HEIGHT = 50;
		private static int TITLE_WIDTH = 70;
		private static int CONTROL_WIDTH = 200;
		private JButton fillShow  = new JButton();
		private JButton outlineShow  = new JButton();
		JSpinner rad;

		public VueHighlightRegionEdit(VARNAPanel vp, HighlightRegionAnnotation an) {
			_an = an;
			_vp = vp;
			_toSlider = new JSlider(JSlider.HORIZONTAL, 0,vp.getRNA().getSize()-1,0);
			_toSlider.setMajorTickSpacing(10);
			_toSlider.setPaintTicks(true);
			_toSlider.setPaintLabels(true);
			_toSlider.setPreferredSize(new Dimension(CONTROL_WIDTH, CONTROL_HEIGHT));

			_fromSlider = new JSlider(JSlider.HORIZONTAL, 0,vp.getRNA().getSize()-1,0);
			_fromSlider.setMajorTickSpacing(10);
			_fromSlider.setPaintTicks(true);
			_fromSlider.setPaintLabels(true);
			_fromSlider.setPreferredSize(new Dimension(CONTROL_WIDTH, CONTROL_HEIGHT));

			_fromSlider.addChangeListener(this);
			_toSlider.addChangeListener(this);

			panel = new JPanel();
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));

			JPanel fromp = new JPanel();
			JLabel l1 = new JLabel("From: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			fromp.add(l1);
			fromp.add(_fromSlider);

			JPanel top = new JPanel();
			l1 = new JLabel("To: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			top.add(l1);
			top.add(_toSlider);

			JPanel outlinep = new JPanel();
			l1 = new JLabel("Outline color: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			outlineShow.setContentAreaFilled(false);
			outlineShow.setOpaque(true);
			outlineShow.setPreferredSize(new Dimension(CONTROL_WIDTH,CONTROL_HEIGHT));
			outlineShow.setBackground(an.getOutlineColor());
			outlineShow.addActionListener(this);
			outlineShow.setActionCommand("outline");
			outlinep.add(l1);
			outlinep.add(outlineShow);
			
			JPanel fillp = new JPanel();
			l1 = new JLabel("Fill color: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			fillShow.setContentAreaFilled(false);
			fillShow.setOpaque(true);
			fillShow.setPreferredSize(new Dimension(CONTROL_WIDTH,CONTROL_HEIGHT));
			fillShow.setBackground(an.getFillColor());
			fillShow.addActionListener(this);
			fillShow.setActionCommand("fill");
			fillp.add(l1);
			fillp.add(fillShow);
			
			JPanel radiusp = new JPanel();
			l1 = new JLabel("Radius: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			SpinnerNumberModel jm = new SpinnerNumberModel(_an.getRadius(),1.0,50.0,0.1);
			rad = new JSpinner(jm);
			rad.setPreferredSize(new Dimension(CONTROL_WIDTH,CONTROL_HEIGHT));
			radiusp.add(l1);
			radiusp.add(rad);
			rad.addChangeListener(this);
			
			JPanel jp = new JPanel();
			jp.setLayout(new GridLayout(5,1));
			jp.add(fromp);
			jp.add(top);
			jp.add(outlinep);
			jp.add(fillp);
			jp.add(radiusp);
			panel.add(jp);
		}

		public JPanel getPanel() {
			return panel;
		}

		public double getAngle() {
			return _toSlider.getValue();
		}

		public VARNAPanel get_vp() {
			return _vp;
		}
		
		HighlightRegionAnnotation _backup = null;
		
		public boolean show() {
			boolean accept = false;
			int from = _an.getMinIndex();
			int to = _an.getMaxIndex();
			_fromSlider.setValue(from);
			_toSlider.setValue(to );
			if (JOptionPane.showConfirmDialog(_vp, getPanel(),
					"Edit region annotation", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) 
			{
				accept = true;
			} 
			_vp.repaint();
			return accept;
		}

		public void stateChanged(ChangeEvent e) {
			if ((e.getSource()==_toSlider)||(e.getSource()==_fromSlider))
			{
			int from  = _fromSlider.getValue(); 
			int to  = _toSlider.getValue(); 
			if (from>to)
			{
				if (e.getSource().equals(_fromSlider))
				{
					_toSlider.setValue(from);
				}
				else if (e.getSource().equals(_toSlider))
				{
					_fromSlider.setValue(to);
				}
			}
			from  = _fromSlider.getValue(); 
			to  = _toSlider.getValue(); 
			_an.setBases(_vp.getRNA().getBasesBetween(from, to));
			_vp.repaint();
			}
			else if (e.getSource().equals(rad))
			{
				Object val = rad.getValue();
				if (val instanceof Double)
				{					
					_an.setRadius(((Double)val).doubleValue());
				}
			}
			
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("outline"))
			{
			  Color c = JColorChooser.showDialog(getPanel(), "Choose new outline color", _an.getOutlineColor());
			  if (c!= null)
			  {   _an.setOutlineColor(c);  }
			}
			else if (e.getActionCommand().equals("fill"))
			{
				  Color c = JColorChooser.showDialog(getPanel(), "Choose new fill color", _an.getFillColor());
				  if (c!= null)
				  {  _an.setFillColor(c); }
			}
			outlineShow.setBackground(_an.getOutlineColor());
			fillShow.setBackground(_an.getFillColor());
			_vp.repaint();
			
		}
	}

