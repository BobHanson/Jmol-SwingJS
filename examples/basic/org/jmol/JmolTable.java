package org.jmol;
/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.util.Logger;

/**
 * A example of integrating the Jmol viewer into a JTable.
 *
 * <p>This code can be compiled and run by doing:
 * <pre>
 * javac -classpath ../Jmol.jar JmolTable.java
 * java -cp .:../Jmol.jar JmolTable FILE1 FILE2 FILE3 ....
 * </pre>
 *
 * @author Rajarshi Guha
 */

public class JmolTable {

    static int STRUCTURE_COL = 0;

    public static void main(String[] args) {

    if (args.length == 0)
      args = new String[] {"caffeine.xyz", "C6H6.smol" };
    int nobject = args.length;
    int ncol = 2;
    
    Object[][] data = new Object[nobject][ncol];
    for (int i = 0; i < nobject; i++) {
      data[i][0] = new JmolPanel();
      JmolViewer v = ((JmolPanel) data[i][0]).getViewer();
      String strError = v.openFile(args[i]);
      if (strError != null) {
        Logger.error(strError);
      }
      data[i][1] = args[i];
    }
    String[] colNames = { "Structure", "Filename" };

    showMolecules(colNames, data, nobject);
  }

    /**
     * 
     * @param colNames
     * @param data
     * @param nmol  NOT USED
     */
    public static void showMolecules(String[] colNames, Object[][] data, int nmol) {

        // set up the toplevel frame
        JFrame frame = new JFrame("Structure Viewer");
        frame.addWindowListener(new AppCloser());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
        // create a JTable with an AbstractTableModel
        JTable mtable = new JTable( new JmolPanelJTableModel(data, colNames) );
        mtable.setShowGrid(true);

        // add a TableolumnModelListener so we can catch column
        // resizes and change row heights accordingly
        mtable.getColumnModel().addColumnModelListener( new JmolColumnModelListener(mtable) );
        

        // allow cell selections
        mtable.setCellSelectionEnabled(true);

        // disable movement of columns. This is needed since we
        // set the CellRenderer and CellEditor for a specific column
        mtable.getTableHeader().setReorderingAllowed(false);
            

        // set up scroll bar
        JScrollPane scrollpane = new JScrollPane(mtable);
        mtable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        frame.getContentPane().add(scrollpane);

        // set the TableCellRenderer for the structure column
        // we also set up a TableCellEditor so that events on a JmolPanel
        // cell get forwarded to the actual JmolPanel
        TableColumn col = mtable.getColumnModel().getColumn( STRUCTURE_COL);
        col.setCellRenderer( new JmolPanelCellRenderer() );
        col.setCellEditor( new JmolPanelCellEditor() );

        // start the show!
        frame.pack();
        frame.setSize(300, 300);
        frame.setVisible(true);
    }
       
    static class JmolColumnModelListener implements TableColumnModelListener {
        JTable table;
        public JmolColumnModelListener(JTable t) {
            this.table = t;
        }
        @Override
        public void columnAdded(TableColumnModelEvent e) {
        }
        @Override
        public void columnRemoved(TableColumnModelEvent e) {
        }
        @Override
        public void columnMoved(TableColumnModelEvent e) {
        }
        @Override
        public void columnMarginChanged(ChangeEvent e) {
            int colwidth = this.table.getColumnModel().getColumn(STRUCTURE_COL).getWidth();
            for (int i = 0; i < this.table.getRowCount(); i++) {
                this.table.setRowHeight(i, colwidth);
            }

        }
        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }

    static class JmolPanelJTableModel extends AbstractTableModel {
        private Object[][] rows;
        private String[] columns;

        public JmolPanelJTableModel(Object[][] objs, String[] cols) {
            rows = objs;
            columns = cols;
        }

        @Override
        public String getColumnName(int column) { 
            return columns[column];
        }

        @Override
        public int getRowCount() {
            return rows.length;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int row, int column) { 
            return rows[row][column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column == STRUCTURE_COL) {
                return true;
            }
            return false;
        }

        @Override
        public Class<? extends Object> getColumnClass(int column) {
            return getValueAt(0, column).getClass();
        }
    }
    
            
                
    static class JmolPanelCellRenderer extends JmolPanel implements TableCellRenderer{
        @Override
        public Component getTableCellRendererComponent( 
                JTable table,  Object value, boolean isSelected, 
                boolean hasFocus, int rowIndex, int vColIndex ) {
           return (JmolPanel)value;
                }

        // The following methods override the defaults for performance reasons
        @Override
        public void validate() {}
        @Override
        public void revalidate() {}
        @Override
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        @Override
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    static class JmolPanelCellEditor extends AbstractCellEditor implements TableCellEditor{

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {
            return (JmolPanel)value;
                }
        @Override
        public Object getCellEditorValue() {
            return new Object();
        }
        /**
         * 
         * @param row
         * @param column
         * @return  whether editable
         */
        public boolean isCellEditable(int row, int column) {
            if (column == STRUCTURE_COL) {
                return true;
            }
            return false;
        }
        @Override
        public boolean stopCellEditing() {
            return true;
        }
    }


}


class JmolPanel extends JPanel {
  JmolViewer viewer;
  JmolAdapter adapter;

  JmolPanel() {
    adapter = new SmarterJmolAdapter();
    viewer = JmolViewer.allocateViewer(this, adapter);
    viewer
        .scriptWait("frank off;set defaultDirectory 'http://chemapps.stolaf.edu/jmol/docs/examples-11/data/'");
  }

  public JmolViewer getViewer() {
    return viewer;
  }

  final Dimension currentSize = new Dimension();

  @Override
  public void paint(Graphics g) {
    getSize(currentSize);
    viewer.setScreenDimension(currentSize.width, currentSize.height);
    //Rectangle rectClip = new Rectangle();
    //g.getClipBounds(rectClip);
    viewer.renderScreenImage(g, currentSize.width, currentSize.height);
  }
 
}

/**
 * To shutdown when run as an application.  This is a
 * fairly lame implementation.   A more self-respecting
 * implementation would at least check to see if a save
 * was needed.
 */
class AppCloser extends WindowAdapter {
  @Override
  public void windowClosing(WindowEvent e) {
    System.exit(0);
  }
}



