package fr.orsay.lri.varna.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.GradientEditorPanel;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.ModeleColorMap.NamedColorMapTypes;

public class VueColorMapStyle extends JPanel implements ActionListener, ItemListener, PropertyChangeListener {
	private VARNAPanel _vp;
	private GradientEditorPanel _gp;
	private JComboBox _cb; 
	private JTextField _code; 
	private ModeleColorMap _backup;
	// TODO BH SwingJS note that the save dialog is never used in JavaScript 
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
	
	public VueColorMapStyle(VARNAPanel vp)
	{
		super();
		_vp = vp;
		init();
	}

	private void init()
	{
		JLabel gradientCaption = new JLabel("Click gradient to add new color...");
		_gp = new GradientEditorPanel(_vp.getColorMap().clone());
		_backup = _vp.getColorMap();
		_gp.setPreferredSize(new Dimension(300,70));
		_gp.addPropertyChangeListener(this);

		JPanel codePanel = new JPanel();
		JLabel codeCaption = new JLabel("Param. code: ");
		codeCaption.setVisible(true);
		_code = new JTextField("");
		_code.setFont(Font.decode("Monospaced-PLAIN-12"));
		_code.setEditable(true);
		_code.addFocusListener(new FocusListener(){

			public void focusGained(FocusEvent arg0) {
						_code.setSelectionStart(0);
						_code.setSelectionEnd(_code.getText().length());
			}

			public void focusLost(FocusEvent arg0) {
			}			
		});		
		_code.setVisible(false);
		
		NamedColorMapTypes[] palettes =  ModeleColorMap.NamedColorMapTypes.values();
		Arrays.sort(palettes,new Comparator<ModeleColorMap.NamedColorMapTypes>(){
			public int compare(ModeleColorMap.NamedColorMapTypes arg0, ModeleColorMap.NamedColorMapTypes arg1) {
				return arg0.getId().compareTo(arg1.getId());
			}			
		});
		Object[] finalArray = new Object[palettes.length+1];
		int selected = -1;
		for (int i=0;i<palettes.length;i++)
		{ 
			if (palettes[i].getColorMap().equals(_vp.getColorMap()))
			{
				selected = i; 
				//System.out.println(selected);
			}
			finalArray[i] = palettes[i]; 
		}
		String custom = new String("Custom...");
		finalArray[palettes.length] = custom;
		_cb = new JComboBox(finalArray);
		if (selected!=-1)
		{
			_cb.setSelectedIndex(selected);
		}
		else
		{
			_cb.setSelectedItem(finalArray.length-1);
		}
		_cb.addItemListener(this);
		
		_code.setText(getTextRepresentation());
		
		
		
		FileFilter CMSFiles = new FileFilter(){
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".cms") || f.isDirectory();
			}

			public String getDescription() {
				return "Color Map (*.cms) Files";
			}
			
		};
		fc.addChoosableFileFilter(CMSFiles);
		fc.setFileFilter(CMSFiles);
		
		codePanel.setLayout(new BoxLayout(codePanel,BoxLayout.LINE_AXIS));
		codePanel.add(codeCaption);
		codePanel.add(_code);
		JButton loadStyleButton = new JButton("Load");
		loadStyleButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (fc.showOpenDialog(VueColorMapStyle.this)==JFileChooser.APPROVE_OPTION)
				{
					File file = fc.getSelectedFile();
					try {
						FileInputStream fis = new FileInputStream(file);
						byte[] data = new byte[(int) file.length()];
						fis.read(data);
						fis.close();
						String str = new String(data).trim();
						ModeleColorMap ns = ModeleColorMap.parseColorMap(str);
						_gp.setColorMap(ns);
						refreshCode();
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
				if (fc.showSaveDialog(VueColorMapStyle.this)==JFileChooser.APPROVE_OPTION)
				{
					try {
						PrintWriter out = new PrintWriter(fc.getSelectedFile());
						out.println(_gp.getColorMap().getParamEncoding());
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

		JPanel jp = new JPanel(new BorderLayout());
		jp.add(_cb,BorderLayout.CENTER);
		JPanel jp2 = new JPanel();
		jp2.setLayout(new BoxLayout(jp2,BoxLayout.X_AXIS));
		jp2.add(Box.createRigidArea(new Dimension(5,0)));
		jp2.add(loadStyleButton);
		jp2.add(Box.createRigidArea(new Dimension(5,0)));
		jp2.add(saveStyleButton);
		jp.add(jp2,BorderLayout.EAST);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(jp);
		add(Box.createVerticalStrut(10));
		add(_gp);
		add(gradientCaption);
		//add(Box.createVerticalStrut(10));
		//add(codePanel);
	}
	
	public String getTextRepresentation()
	{
		int selected = _cb.getSelectedIndex();
		if ((selected!=-1) && (selected<_cb.getItemCount()-1))
		{
			return ((NamedColorMapTypes) _cb.getSelectedItem()).getId(); 
		}
		else
		{
			return _gp.getColorMap().getParamEncoding();
		}
	}
	
	public void cancelChanges()
	{
		_vp.setColorMap(_backup);
	}
	
	public ModeleColorMap getColorMap()
	{
		return _gp.getColorMap();
	}
	
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
	private void refreshCode()
	{
		int selected = -1;
		NamedColorMapTypes n = null;
		for (int i=0;i<_cb.getItemCount()-1;i++)
		{ 
			Object o = _cb.getItemAt(i);
			if (o instanceof NamedColorMapTypes)
			{
				NamedColorMapTypes ni = (NamedColorMapTypes) o;
				if (ni.getColorMap().equals(_gp.getColorMap()))
				{ 
					selected = i;	n = ni;
				}
			}
		}
		if (selected!=-1)
		{
			_code.setText(n.getId());
		}
		_code.setText(getTextRepresentation());
		_vp.setColorMap(_gp.getColorMap());
		_gp.repaint();
	}

	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getStateChange()==ItemEvent.SELECTED)
		{
		Object o = arg0.getItem();
		if (o instanceof ModeleColorMap.NamedColorMapTypes)
		{
			ModeleColorMap.NamedColorMapTypes n = ((ModeleColorMap.NamedColorMapTypes) o);
			ModeleColorMap m = n.getColorMap().clone();
			m.setMinValue(_backup.getMinValue());
			m.setMaxValue(_backup.getMaxValue());
			_gp.setColorMap(m);
			refreshCode();
		}
		}
	}

	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("PaletteChanged"))
		{
			_cb.setSelectedIndex(_cb.getItemCount()-1);
			refreshCode();
		};
	}


}
