package fr.orsay.lri.varna.components;

import java.awt.Component;
import java.awt.Event;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class ActionEditor extends AbstractCellEditor implements TableCellEditor { 

	JButton _btn = new JButton();

	public ActionEditor (ActionListener a) {
	  // add all elments you need to your panel
	  _btn.addActionListener(a);
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int vColIndex) { 
	   _btn.setText(value.toString());
	   _btn.setActionCommand(value.toString()+"-"+rowIndex);
	   // set all elemnts of you panel to the according values
	   // or add dynamically an action listener
	   
	   return _btn;
	}
	public Object getCellEditorValue() 
	{ 
		return ""; 
	} 
	
	public boolean shouldSelectCell(EventObject anEvent)
	{
		return super.shouldSelectCell(anEvent);
		
	}

	public boolean isCellEditable(EventObject anEvent)
	{
		return super.isCellEditable(anEvent);		
	}
	
	public boolean stopCellEditing()
	{
		return super.stopCellEditing();
	}

	
} 