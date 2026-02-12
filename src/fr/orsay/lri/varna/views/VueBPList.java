package fr.orsay.lri.varna.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.ActionEditor;
import fr.orsay.lri.varna.components.ActionRenderer;
import fr.orsay.lri.varna.components.ColorRenderer;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;

public class VueBPList extends JPanel implements TableModelListener, ActionListener
{

	private JTable table;
	private BPTableModel _tm;
	private VARNAPanel _vp;
	private ArrayList<ModeleBP> data;
	private ArrayList<Double> _backup;
	private ArrayList<Object> columns;
	
	
	public enum Actions{
		ACTION_DELETE,
		ACTION_EDIT_STYLE;
		
		public String toString()
		{
			switch(this)
			{
			  case ACTION_DELETE:
				  return "Delete";
			  case ACTION_EDIT_STYLE:
				  return "Edit Style";
			}
			return "N/A";
		}
	};
	
	public VueBPList(VARNAPanel vp)
	{
		super(new GridLayout(1, 0));
		_vp = vp;
		init();
	}
	
	private void init()
	{
		Object[] col = {"Sec.Str.","5' partner","3' partner","5' edge","3' edge","Orientation","Remove"};
		columns = new ArrayList<Object>();
		for (int i = 0; i < col.length; i++)
		{
			columns.add(col[i]);
		}
		
		_backup = new ArrayList<Double>();
		data = new ArrayList<ModeleBP>();
		for (ModeleBP ms: _vp.getRNA().getAllBPs()) 
		{
			data.add(ms);
		}
		Collections.sort(data);
		_tm = new BPTableModel();
		table = new JTable(_tm);
		
		table.setDefaultRenderer(Color.class, new ColorRenderer(true)); 
		table.setDefaultRenderer(Actions.class, new ActionRenderer());
		
		table.setDefaultEditor(ModeleBP.Edge.class, new DefaultCellEditor(new JComboBox(ModeleBP.Edge.values())));
		table.setDefaultEditor(ModeleBP.Stericity.class, new DefaultCellEditor(new JComboBox(ModeleBP.Stericity.values())));
		table.setDefaultEditor(Actions.class, new ActionEditor(this));

		table.setPreferredScrollableViewportSize(new Dimension(500, 500));
		table.getModel().addTableModelListener(this);
		
		table.setRowHeight(25);
		
		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane);
		setOpaque(true); // content panes must be opaque
		this.doLayout();

		JOptionPane.showMessageDialog(_vp, this, "Base pairs Edition", JOptionPane.PLAIN_MESSAGE);

	}
	
	public void cancelChanges()
	{
		for (int i = 0; i < _vp.getRNA().get_listeBases().size(); i++) 
		{
			ModeleBase mb = _vp.getRNA().get_listeBases().get(i);
			mb.setValue(_backup.get(i));
		}
  	  _vp.getRNA().rescaleColorMap(_vp.getColorMap());
	}

	private class BPTableModel extends AbstractTableModel {
	    /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public String getColumnName(int col) {
	        return columns.get(col).toString();
	    }
	    public int getRowCount() { return data.size(); }
	    public int getColumnCount() { return columns.size(); }
	    public Object getValueAt(int row, int col) {
	    	ModeleBP mb = data.get(row);
	    	if (col==0)
	    	{
	    		return new Boolean(mb.getPartner3().getElementStructure()==mb.getPartner5().getIndex());
	    	}
	    	else if (col==1)
	    	{
	    		return new String(""+mb.getPartner5().getBaseNumber()+"-"+mb.getPartner5().getContent());
	    	} 
	    	else if (col==2)
	    	{
	    		return new String(""+mb.getPartner3().getBaseNumber()+"-"+mb.getPartner3().getContent());
	    	} 
	    	else if (col==3)
	    	{
	    		return mb.getEdgePartner5();
	    	}
	    	else if (col==4)
	    	{
	    		return mb.getEdgePartner3();
	    	}
	    	else if (col==5)
	    	{
	    		return mb.getStericity();
	    	}
	    	else if (col==6)
	    	{
	    		return Actions.ACTION_DELETE;
	    	}
	    	return "N/A";
	    }
	    public boolean isCellEditable(int row, int col)
	        { 
	    		if ( col == 3 || col ==4 || col ==5 || col ==6) 
	    			return true;
	    		return false;
	        }
	    public void setValueAt(Object value, int row, int col) {
	    	if ( col == 3 || col ==4 || col ==5) 
	    	{
	    		ModeleBP mb = data.get(row);
		    	if ( col == 3)
		    	{
		    	  mb.setEdge5((ModeleBP.Edge)value);	
		    	}
		    	else if ( col == 4)
		    	{
			    	  mb.setEdge3((ModeleBP.Edge)value);	
		    	}
		    	else if ( col == 5)
		    	{
			    	  mb.setStericity((ModeleBP.Stericity)value);			    		
		    	}
	          fireTableCellUpdated(row, col);
	    	  _vp.repaint();
	    	  
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

	public void actionPerformed(ActionEvent arg0) {
		//System.out.println(""+arg0.toString());
		String[] data2 = arg0.getActionCommand().split("-");
		int row = Integer.parseInt(data2[data2.length-1]);
		if (data2[0].equals("Delete"))
		{
			ModeleBP ms = data.get(row);
			_vp.getVARNAUI().UIRemoveBP(ms);
			
			
			data.remove(row);
			
			_tm.fireTableRowsDeleted(row, row);
		}
	}

}
