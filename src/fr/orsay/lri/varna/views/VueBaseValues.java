package fr.orsay.lri.varna.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import fr.orsay.lri.varna.components.ColorRenderer;
import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.models.rna.ModeleBase;

public class VueBaseValues extends JPanel implements TableModelListener {

	protected JTable table;
	protected ValueTableModel _tm;
	protected VARNAPanel _vp;
	protected ArrayList<ModeleBase> data;
	protected ArrayList<Double> _backup;
	protected ArrayList<Object> columns;
	
	
	public VueBaseValues(VARNAPanel vp)
	{
		super(new GridLayout(1, 0));
		_vp = vp;
		init();
	}
	
	protected void init()
	{
		Object[] col = {"Number","Base","Value","Preview"};
		columns = new ArrayList<Object>();
		for (int i = 0; i < col.length; i++)
		{
			columns.add(col[i]);
		}
		
		_backup = new ArrayList<Double>();
		data = new ArrayList<ModeleBase>();
		for (int i = 0; i < _vp.getRNA().get_listeBases().size(); i++) 
		{
			ModeleBase mb = _vp.getRNA().get_listeBases().get(i);
			data.add(mb);
			_backup.add(Double.valueOf(mb.getValue()));
		}
		_tm = new ValueTableModel();
		table = new JTable(_tm);
		table.setDefaultRenderer(Color.class, new ColorRenderer(true)); 
		table.setPreferredScrollableViewportSize(new Dimension(300, 300));
		table.getModel().addTableModelListener(this);
		
		JScrollPane scrollPane = new JScrollPane(table);
		this.add(scrollPane);
	}
	
	public void cancelChanges()
	{
		for (int i = 0; i < _vp.getRNA().get_listeBases().size(); i++) 
		{
			ModeleBase mb = _vp.getRNA().get_listeBases().get(i);
			mb.setValue(_backup.get(i).doubleValue());
		}
  	  _vp.getRNA().rescaleColorMap(_vp.getColorMap());
	}

	protected class ValueTableModel extends AbstractTableModel {
	    @Override
      public String getColumnName(int col) {
	        return columns.get(col).toString();
	    }
	    @Override
      public int getRowCount() { return data.size(); }
	    @Override
      public int getColumnCount() { return columns.size(); }
	    @Override
      public Object getValueAt(int row, int col) {
	    	ModeleBase mb = data.get(row);
	    	if (col==0)
	    	{
	    		return new Integer(mb.getResidueNumber());
	    	}
	    	else if (col==1)
	    	{
	    		return new String(mb.getContent());
	    	} 
	    	else if (col==2)
	    	{
	    		return new Double(mb.getValue());
	    	} 
	    	else if (col==3)
	    	{
	    		return _vp.getColorMap().getColorForValue(mb.getValue());
	    	}
	    	return "N/A";
	    }
	    @Override
      public boolean isCellEditable(int row, int col)
	        { 
	    		if (getColumnName(col).equals("Value")) 
	    			return true;
	    		return false;
	        }
	    @Override
      public void setValueAt(Object value, int row, int col) {
	    	if (getColumnName(col).equals("Value"))
	    	{
	    	  data.get(row).setValue(((Double)value).doubleValue());
	    	  _vp.getRNA().rescaleColorMap(_vp.getColorMap());
	    	  _vp.repaint();
	          fireTableCellUpdated(row, col);
	    	}
	    }
	    @Override
      public Class<?> getColumnClass(int c) {
	        return getValueAt(0, c).getClass();
	    }
	}

	@Override
  public void tableChanged(TableModelEvent e) {
		if (e.getType() == TableModelEvent.UPDATE)
		{
			table.repaint();
		}
		
	}

}
