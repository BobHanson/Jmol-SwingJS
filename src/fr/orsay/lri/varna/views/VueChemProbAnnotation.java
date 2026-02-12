	package fr.orsay.lri.varna.views;


	import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;


public class VueChemProbAnnotation implements ChangeListener, ActionListener, ItemListener {

		protected VARNAPanel _vp;
		private JPanel panel;
		protected ChemProbAnnotation _an;
		private static int CONTROL_HEIGHT = 50;
		private static int TITLE_WIDTH = 70;
		private static int CONTROL_WIDTH = 200;
		protected JButton color  = new JButton();
		JSpinner intensity;
		JComboBox outward = new JComboBox(new String[]{"Inward","Outward"});
		JComboBox type = new JComboBox(ChemProbAnnotation.ChemProbAnnotationType.values());

		public VueChemProbAnnotation(VARNAPanel vp, ChemProbAnnotation an) {
			_an = an;
			_vp = vp;

			panel = new JPanel();
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));


			JPanel outlinep = new JPanel();
			JLabel l1 = new JLabel("Color: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			color.setContentAreaFilled(false);
			color.setOpaque(true);
			color.setPreferredSize(new Dimension(CONTROL_WIDTH,CONTROL_HEIGHT));
			color.setBackground(_an.getColor());
			color.addActionListener(this);
			color.setActionCommand("outline");
			outlinep.add(l1);
			outlinep.add(color);
			
			
			JPanel radiusp = new JPanel();
			l1 = new JLabel("Intensity: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			SpinnerNumberModel jm = new SpinnerNumberModel(_an.getIntensity(),0.01,10.0,0.01);
			intensity = new JSpinner(jm);
			radiusp.add(l1);
			radiusp.add(intensity);
			intensity.addChangeListener(this);

			JPanel dirp = new JPanel();
			l1 = new JLabel("Direction: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			outward.addItemListener(this);
			dirp.add(l1);
			dirp.add(outward);

			JPanel typep = new JPanel();
			l1 = new JLabel("Type: ");
			l1.setPreferredSize(new Dimension(TITLE_WIDTH,CONTROL_HEIGHT));
			type.addItemListener(this);
			typep.add(l1);
			typep.add(type);
			
			
			
			JPanel jp = new JPanel();
			jp.setLayout(new GridLayout(4,1));
			jp.add(outlinep);
			jp.add(radiusp);
			jp.add(dirp);
			jp.add(typep);
			panel.add(jp);
		}

		public JPanel getPanel() {
			return panel;
		}

		public VARNAPanel get_vp() {
			return _vp;
		}
		
		HighlightRegionAnnotation _backup = null;
		
		public boolean show() {
			boolean accept = false;
			intensity.setValue(_an.getIntensity());
			color.setBackground(_an.getColor());
			type.setSelectedItem(_an.getType());
			outward.setSelectedItem((_an.isOut()?"Inward":"Outward"));
			
			if (JOptionPane.showConfirmDialog(_vp, getPanel(),
					"Edit chemical probing annotation", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) 
			{
				accept = true;
			} 
			_vp.repaint();
			return accept;
		}

		public void stateChanged(ChangeEvent e) {
			if (e.getSource().equals(intensity))
			{
				Object val = intensity.getValue();
				if (val instanceof Double)
				{					
					_an.setIntensity(((Double)val).doubleValue());
					_vp.repaint();
				}
			}
			
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("outline")) {
			// BH j2s SwingJS asynchronous for JavaScript; synchronous for Java
			_vp.getVARNAUI().showColorDialog("Choose new outline color", _an.getColor(), new Runnable() {

				@Override
				public void run() {
					Color c = (Color) _vp.getVARNAUI().dialogReturnValue;
					if (c != null) {
						_an.setColor(c);
						color.setBackground(_an.getColor());
						_vp.repaint();
					}
				}

			});
		}

	}

		public void itemStateChanged(ItemEvent e) {
			if (e.getSource()==outward)
			{
				_an.setOut(!e.getItem().equals("Outward"));
				_vp.repaint();
			}
			else if ((e.getSource()==type)&&(e.getItem() instanceof ChemProbAnnotation.ChemProbAnnotationType))
			{
				ChemProbAnnotation.ChemProbAnnotationType t = (ChemProbAnnotation.ChemProbAnnotationType) e.getItem();
				_an.setType(t);
				_vp.repaint();
			}
			
		}
	}


