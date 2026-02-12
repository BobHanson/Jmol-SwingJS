/*
 * VARNA is a tool for the automated drawing, visualization and annotation
 * of the secondary structure of RNA, designed as a companion software for
 * web servers and databases. Copyright (C) 2008 Kevin Darty, Alain Denise
 * and Yann Ponty. electronic mail : Yann.Ponty@lri.fr paper mail : LRI, bat
 * 490 Université Paris-Sud 91405 Orsay Cedex France
 * 
 * This file is part of VARNA version 3.1. VARNA version 3.1 is free
 * software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * VARNA version 3.1 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with VARNA version 3.1. If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.AnnotationTableModel;
import fr.orsay.lri.varna.controlers.ControleurTableAnnotations;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;

/**
 * a view for all annoted texts on the VARNAPanel
 * 
 * @author Darty@lri.fr
 * 
 */
public class VueListeAnnotations extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * if this view is for removing annoted texts
	 */
	public static final int REMOVE = 0;
	/**
	 * if this view is for editing annoted texts
	 */
	public static final int EDIT = 1;

	private VARNAPanel _vp;
	private ArrayList<Object> data;
	private JTable table;
	private int type;
	private AnnotationTableModel specialTableModel;
	// BH SwingJS - this is never used in JavaScript
	private static JFileChooser fc = new JFileChooser(){
	    public void approveSelection(){
	        File f = getSelectedFile();
	        if(f.exists() && getDialogType() == SAVE_DIALOG){
	            int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_OPTION);
	            switch(result){
	                case JOptionPane.YES_OPTION:
	                    super.approveSelection();
	                    return;
	                case JOptionPane.NO_OPTION:
	                    return;
	                case JOptionPane.CLOSED_OPTION:
	                    return;
	                case JOptionPane.CANCEL_OPTION:
	                    cancelSelection();
	                    return;
	            }
	        }
	        super.approveSelection();
	    }        
	};


	/**
	 * creates the view
	 * 
	 * @param vp
	 * @param type
	 *            (REMOVE or EDIT)
	 */
	public VueListeAnnotations(VARNAPanel vp, int type) {
		super(new BorderLayout());
		this.type = type;
		_vp = vp;
		data = new ArrayList<Object>();
		data.addAll(_vp.getListeAnnotations());
		data.addAll(_vp.getRNA().getHighlightRegion());
		data.addAll(_vp.getRNA().getChemProbAnnotations());
		createView();
	}

	private void createView() {
		specialTableModel = new AnnotationTableModel(data);
		table = new JTable(specialTableModel);
		ControleurTableAnnotations ctrl = new ControleurTableAnnotations(table,
				_vp, type);
		table.addMouseListener(ctrl);
		table.addMouseMotionListener(ctrl);
		// table.setPreferredScrollableViewportSize(new Dimension(500, 100));
		// TODO: Find equivalent in JRE 1.5
		// table.setFillsViewportHeight(true);
		// Create the scroll pane and add the table to it.
		JScrollPane scrollPane = new JScrollPane(table);

		add(scrollPane, BorderLayout.CENTER);
		
		FileFilter CPAFiles = new FileFilter(){
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".cpa") || f.isDirectory();
			}

			public String getDescription() {
				return "Chemical Probing Annotations (*.cpa) Files";
			}
			
		};
		fc.addChoosableFileFilter(CPAFiles);
		fc.setFileFilter(CPAFiles);

		
		JButton loadStyleButton = new JButton("Load");
		loadStyleButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (fc.showOpenDialog(VueListeAnnotations.this)==JFileChooser.APPROVE_OPTION)
				{
					File file = fc.getSelectedFile();
					try {
						BufferedReader br = new BufferedReader(new FileReader(file));
						String s = br.readLine();
						while(s != null)
						{
							if (s.startsWith(TextAnnotation.HEADER_TEXT))
							s = br.readLine();
						}
						// TODO
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			
		});
		JButton saveStyleButton = new JButton("Save");
		saveStyleButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (fc.showSaveDialog(VueListeAnnotations.this)==JFileChooser.APPROVE_OPTION)
				{
					try {
						PrintWriter out = new PrintWriter(fc.getSelectedFile());
						// TODO out.println(_gp.getColorMap().getParamEncoding());
						out.close();
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			
		});
		saveStyleButton.setAlignmentX(CENTER_ALIGNMENT);
		loadStyleButton.setAlignmentX(CENTER_ALIGNMENT);

		JPanel jp2 = new JPanel();
		BoxLayout bl = new BoxLayout(jp2, BoxLayout.X_AXIS);
		jp2.setLayout(bl);
		jp2.setAlignmentX(CENTER_ALIGNMENT);
		jp2.add(loadStyleButton);
		jp2.add(Box.createRigidArea(new Dimension(5,0)));
		jp2.add(saveStyleButton);
		this.add(jp2,BorderLayout.SOUTH);

		

		UIvueListeAnnotations();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	public void UIvueListeAnnotations() {
		JComponent newContentPane = this;
		newContentPane.setOpaque(true); 
		JOptionPane.showMessageDialog(_vp, newContentPane,
				"Annotation edition", JOptionPane.PLAIN_MESSAGE);
	}

	public ArrayList<Object> getData() {
		return data;
	}

	public void setData(ArrayList<Object> data) {
		this.data = data;
	}

	public VARNAPanel get_vp() {
		return _vp;
	}

	public JTable getTable() {
		return table;
	}

	public void setTable(JTable table) {
		this.table = table;
	}

	public AnnotationTableModel getSpecialTableModel() {
		return specialTableModel;
	}

	public void setSpecialTableModel(AnnotationTableModel specialTableModel) {
		this.specialTableModel = specialTableModel;
	}
}
