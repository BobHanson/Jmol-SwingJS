/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.geom.Point2D.Double;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicBorders;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurSliderLabel;
import fr.orsay.lri.varna.controlers.ControleurVueAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;

/**
 * 
 * BH SwingJS using asynchronous JOptionPane.showConfirmDialog
 * 
 * annoted text view for edition
 * 
 * @author Darty@lri.fr
 * 
 */
public class VueAnnotation {

	protected VARNAPanel _vp;
	private JSlider ySlider, xSlider;
	private JButton colorButton;
	private JTextArea textArea;
	private JPanel panel;
	protected TextAnnotation textAnnotation, textAnnotationSave;
	private VueFont vueFont;
	private ControleurVueAnnotation _controleurVueAnnotation;
	protected boolean newAnnotation, limited;
	private Double position;
	private JSlider rotationSlider;

	/**
	 * creates a view for a new annoted text
	 * 
	 * @param vp
	 * @param limited
	 *            if true, lets custom position and angle.
	 */
	public VueAnnotation(VARNAPanel vp, boolean limited) {
		this(
				vp,
				(int) (vp.getExtendedRNABBox().x + vp.getExtendedRNABBox().width / 2.0),
				(int) (vp.getExtendedRNABBox().y + vp.getExtendedRNABBox().height / 2.0),
				limited);
	}

	/**
	 * creates a view for a new annoted text, without limited option
	 * 
	 * @param vp
	 */
	public VueAnnotation(VARNAPanel vp) {
		this(vp, false);
	}

	/**
	 * creates a view for a new annoted text at a given position, without
	 * limited option
	 * 
	 * @param vp
	 */
	public VueAnnotation(VARNAPanel vp, int x, int y) {
		this(vp, x, y, false);
	}

	/**
	 * creates a view for a new annoted text at a given position, without
	 * limited option
	 * 
	 * @param vp
	 */
	public VueAnnotation(VARNAPanel vp, int x, int y, boolean limited) {
		this(vp, new TextAnnotation("", x, y), false, true);
	}

	/**
	 * creates a view for an annoted text, without limited option
	 * 
	 * @param vp
	 * @param textAnnot
	 */
	public VueAnnotation(VARNAPanel vp, TextAnnotation textAnnot,
			boolean newAnnotation) {
		this(vp, textAnnot, (textAnnot.getType()!=TextAnnotation.AnchorType.POSITION), newAnnotation);
	}

	/**
	 * creates a view for an annoted text
	 * 
	 * 
	 * @param vp
	 * @param textAnnot
	 * @param reduite
	 *            if true, lets custom position and angle.
	 * @param newAnnotation
	 *            if true, deleted if cancelled.
	 */
	public VueAnnotation(VARNAPanel vp, TextAnnotation textAnnot,
			boolean reduite, boolean newAnnotation) {
		this.limited = reduite;
		this.newAnnotation = newAnnotation;
		_vp = vp;
		textAnnotation = textAnnot;
		textAnnotationSave = textAnnotation.clone();

		if (!_vp.getListeAnnotations().contains(textAnnot)) {
			_vp.addAnnotation(textAnnotation);
		}

		_controleurVueAnnotation = new ControleurVueAnnotation(this);

		position = textAnnotation.getCenterPosition();

		/*
		 * if (textAnnotation.getType() != TextAnnotation.POSITION) { position =
		 * _vp.transformCoord(position); }
		 */

		JPanel py = new JPanel();
		JPanel px = new JPanel();
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		py.setLayout(new FlowLayout(FlowLayout.LEFT));
		px.setLayout(new FlowLayout(FlowLayout.LEFT));

		ySlider = new JSlider(JSlider.HORIZONTAL, 0, (int) (_vp
				.getExtendedRNABBox().height), Math.max(0, Math.min((int) (_vp
				.getExtendedRNABBox().height), (int) (position.y - _vp
				.getExtendedRNABBox().y))));
		// Turn on labels at major tick marks.
		ySlider.setMajorTickSpacing(500);
		ySlider.setMinorTickSpacing(100);
		ySlider.setPaintTicks(true);
		ySlider.setPaintLabels(true);
		ySlider.setPreferredSize(new Dimension(400,
				ySlider.getPreferredSize().height));

		JLabel yValueLabel = new JLabel(String.valueOf((int) position.y
				- _vp.getExtendedRNABBox().y));
		yValueLabel.setPreferredSize(new Dimension(50, yValueLabel
				.getPreferredSize().height));
		ySlider
				.addChangeListener(new ControleurSliderLabel(yValueLabel, false));
		ySlider.addChangeListener(_controleurVueAnnotation);

		xSlider = new JSlider(JSlider.HORIZONTAL, 0, (int) (_vp
				.getExtendedRNABBox().width), Math.max(0, Math.min((int) _vp
				.getExtendedRNABBox().width, (int) (position.x - _vp
				.getExtendedRNABBox().x))));
		// Turn on labels at major tick marks.
		xSlider.setMajorTickSpacing(500);
		xSlider.setMinorTickSpacing(100);
		xSlider.setPaintTicks(true);
		xSlider.setPaintLabels(true);
		xSlider.setPreferredSize(new Dimension(400,
				xSlider.getPreferredSize().height));

		JLabel xValueLabel = new JLabel(String.valueOf((int) position.x
				- _vp.getExtendedRNABBox().x));
		xValueLabel.setPreferredSize(new Dimension(50, xValueLabel
				.getPreferredSize().height));
		xSlider
				.addChangeListener(new ControleurSliderLabel(xValueLabel, false));
		xSlider.addChangeListener(_controleurVueAnnotation);

		JLabel labelY = new JLabel("Y:");
		JLabel labelX = new JLabel("X:");

		py.add(labelY);
		py.add(ySlider);
		py.add(yValueLabel);
		px.add(labelX);
		px.add(xSlider);
		px.add(xValueLabel);

		/*if (!limited) {
			panel.add(px);
			panel.add(py);
		}*/

		JPanel panelTexte = new JPanel();
		panelTexte.setLayout(new BorderLayout());
		textArea = new JTextArea(textAnnotation.getTexte());
		textArea.addCaretListener(_controleurVueAnnotation);
		textArea.setPreferredSize(panelTexte.getSize());
		Border border = new BasicBorders.FieldBorder(Color.black, Color.black,
				Color.black, Color.black);
		textArea.setBorder(border);
		JLabel labelTexte = new JLabel("Text:");
		panelTexte.add(textArea, BorderLayout.CENTER);
		panelTexte.add(labelTexte, BorderLayout.NORTH);
		panel.add(panelTexte);

		vueFont = new VueFont(textAnnot.getFont());
		vueFont.getBoxPolice().addActionListener(_controleurVueAnnotation);
		vueFont.getSizeSlider().addChangeListener(_controleurVueAnnotation);
		vueFont.getStylesBox().addActionListener(_controleurVueAnnotation);

		colorButton = new JButton("Set color");
		colorButton.setActionCommand("setcolor");
		colorButton.setForeground(textAnnot.getColor());
		colorButton.addActionListener(_controleurVueAnnotation);

		JPanel fontAndColor = new JPanel();
		fontAndColor.add(vueFont.getPanel());
		fontAndColor.add(colorButton);

		panel.add(fontAndColor);

		JPanel rotationPanel = new JPanel();

		rotationSlider = new JSlider(JSlider.HORIZONTAL, -360, 360,
				(int) textAnnotation.getAngleInDegres());
		rotationSlider.setMajorTickSpacing(60);
		rotationSlider.setPaintTicks(true);
		rotationSlider.setPaintLabels(true);
		rotationSlider.setPreferredSize(new Dimension(500, 50));

		JLabel rotationLabel = new JLabel(String.valueOf(0));
		rotationLabel.setPreferredSize(new Dimension(50, rotationLabel
				.getPreferredSize().height));
		rotationSlider.addChangeListener(new ControleurSliderLabel(
				rotationLabel, false));
		rotationSlider.addChangeListener(_controleurVueAnnotation);

		JLabel labelZ = new JLabel("Rotation (degrees):");

		rotationPanel.add(labelZ);
		rotationPanel.add(rotationSlider);
		rotationPanel.add(rotationLabel);

		/*
		 * if (!limited) { panel.add(rotationPanel); }
		 */

		if (limited) {
			ySlider.setEnabled(false);
			xSlider.setEnabled(false);
			rotationSlider.setEnabled(false);
		}
		textArea.requestFocusInWindow();
		
	}

	private void applyFont() {
		textAnnotation.setFont(vueFont.getFont());
	}

	/**
	 * update the annoted text on the VARNAPanel
	 */
	public void update() {
		applyFont();
		if (textAnnotation.getType() == TextAnnotation.AnchorType.POSITION)
			textAnnotation.setAncrage((double) xSlider.getValue()
					+ _vp.getExtendedRNABBox().x, ySlider.getValue()
					+ _vp.getExtendedRNABBox().y);
		textAnnotation.setText(textArea.getText());
		textAnnotation.setAngleInDegres(rotationSlider.getValue());
		_vp.clearSelection();
		_vp.repaint();
	}

	public JPanel getPanel() {
		return panel;
	}

	/**
	 * 
	 * @return the annoted text
	 */
	public TextAnnotation getTextAnnotation() {
		return textAnnotation;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	/**
	 * shows the dialog which add it to the VARNAPanel for previsualization.
	 * <p>
	 * if validate, just update the annoted text
	 * <p>
	 * if cancelled : remove the annoted text if it was a new one, otherwise
	 * cancel modifications
	 * <p>
	 * 
	 */
	public void show() {
		_vp.set_selectedAnnotation(textAnnotation);
		_vp.highlightSelectedAnnotation();
		
		// BH SwingjS using asynchronous dialog
		Runnable ok = new Runnable() {

			@Override
			public void run() {
				update();
			}
			
		};

		Runnable cancel = new Runnable() {

			@Override
			public void run() {
				if (newAnnotation) {
					_vp.set_selectedAnnotation(null);
					if (!_vp.removeAnnotation(textAnnotation))
						_vp.errorDialog(new Exception("Impossible de supprimer"));
				} else {
					textAnnotation.copy(textAnnotationSave);
				}
			}
			
		};

		Runnable final_ = new Runnable() {

			@Override
			public void run() {
				_vp.resetAnnotationHighlight();
				_vp.set_selectedAnnotation(null);
				_vp.repaint();
			}
			
		};
		
		_vp.getVARNAUI().showConfirmDialog(getPanel(), "Add/edit annotation", ok, cancel, cancel, final_);
	}

	public boolean isLimited() {
		return limited;
	}

	public void setLimited(boolean limited) {
		this.limited = limited;
	}

	public boolean isNewAnnotation() {
		return this.newAnnotation;
	}

	public void updateColor(Color c) {
		colorButton.setForeground(c);
		textAnnotation.setColor(c);

	}
	
}
