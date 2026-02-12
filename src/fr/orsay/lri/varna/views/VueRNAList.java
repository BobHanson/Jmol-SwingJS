package fr.orsay.lri.varna.views;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.ColorRenderer;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.RNA;

public class VueRNAList extends JPanel implements TableModelListener, ActionListener {

	private JTable table;
	private ValueTableModel _tm;
	private ArrayList<RNA> data;
	private ArrayList<Object> columns;
	private ArrayList<Boolean> included;
	
	
	public VueRNAList( ArrayList<RNA> rnas)
	{
		super(new BorderLayout());
		data = rnas;
		init();
	}
	
	public ArrayList<RNA> getSelectedRNAs()
	{
		ArrayList<RNA> result = new ArrayList<RNA>();
		for (int i = 0; i < data.size(); i++)
		{
			if (included.get(i))
			{
				result.add(data.get(i));
			}
		}		
		return result;
	}
	
	private void init()
	{
		Object[] col = {"Num","Selected","Name","ID","Length"};
		columns = new ArrayList<Object>();
		for (int i = 0; i < col.length; i++)
		{
			columns.add(col[i]);
		}
		included = new ArrayList<Boolean>();
		for (int i = 0; i < data.size(); i++)
		{
			included.add(new Boolean(true));
		}
		
		
		_tm = new ValueTableModel();
		table = new JTable(_tm);
		table.setDefaultRenderer(Color.class, new ColorRenderer(true)); 
		table.setPreferredScrollableViewportSize(new Dimension(600, 300));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableColumn c0 = table.getColumnModel().getColumn(0);
		c0.setPreferredWidth(30);
		TableColumn c1 = table.getColumnModel().getColumn(1);
		c1.setPreferredWidth(30);
		TableColumn c2 = table.getColumnModel().getColumn(2);
		c2.setPreferredWidth(200);
		TableColumn c3 = table.getColumnModel().getColumn(3);
		c3.setPreferredWidth(200);
		TableColumn c4 = table.getColumnModel().getColumn(4);
		c4.setPreferredWidth(30);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getModel().addTableModelListener(this);
		
		JScrollPane scrollPane = new JScrollPane(table);
		this.add(scrollPane,BorderLayout.CENTER);
		JPanel jp = new JPanel();
		JPanel jpl = new JPanel();
		JPanel jpr = new JPanel();
		jp.setLayout(new BorderLayout());
		jp.add(jpl,BorderLayout.WEST);
		jp.add(jpr,BorderLayout.EAST);
		jp.add(new JLabel("Please select which model(s) should be imported." ),BorderLayout.SOUTH);
		JButton selectAll = new JButton("Select All");
		selectAll.addActionListener(this);
		selectAll.setActionCommand("all");
		JButton deselectAll = new JButton("Deselect All");
		deselectAll.addActionListener(this);
		deselectAll.setActionCommand("none");
		jpl.add(selectAll);
		jpr.add(deselectAll);
		
		add(scrollPane,BorderLayout.CENTER);
		add(jp,BorderLayout.SOUTH);
		}
	


	private class ValueTableModel extends AbstractTableModel {
	    public String getColumnName(int col) {
	        return columns.get(col).toString();
	    }
	    public int getRowCount() { return data.size(); }
	    public int getColumnCount() { return columns.size(); }
	    public Object getValueAt(int row, int col) {
	    	RNA r = data.get(row);
	    	if (col==0)
	    	{
	    		return new Integer(row+1);
	    	}
	    	else if (col==1)
	    	{
	    		return new Boolean(included.get(row));
	    	}
	    	else if (col==2)
	    	{
	    		return new String(r.getName());
	    	} 
	    	else if (col==3)
	    	{
	    		return new String(r.getID());
	    	} 
	    	else if (col==4)
	    	{
	    		return new Integer(r.getSize());
	    	} 
	    	return "N/A";
	    }
	    public boolean isCellEditable(int row, int col)
	        { 
	    		if (col==1) 
	    			return true;
	    		return false;
	        }
	    public void setValueAt(Object value, int row, int col) {
	    	if (col==1)
	    	{
	    	  included.set(row, (Boolean)value);
	          fireTableCellUpdated(row, col);
	    	}
	    }
	    public Class getColumnClass(int c) {
	        return getValueAt(0, c).getClass();
	    }
	}

	public void tableChanged(TableModelEvent e) {
		if (e.getType() == TableModelEvent.UPDATE)
		{
			table.repaint();
		}
		
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("none"))
		{
			for(int i=0;i<this.included.size();i++)
			{
				included.set(i, false);
			}
			_tm.fireTableRowsUpdated(0, included.size()-1);
		}
		else if (e.getActionCommand().equals("all"))
		{
			for(int i=0;i<this.included.size();i++)
			{
				included.set(i, true);
			}
			_tm.fireTableRowsUpdated(0, included.size()-1);
		}
	}

}
