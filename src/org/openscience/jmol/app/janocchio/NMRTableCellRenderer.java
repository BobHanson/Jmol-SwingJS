/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * cfRenderer.java
 *
 * Created on 22 September 2006, 11:36
 *
 */

package org.openscience.jmol.app.janocchio;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 
 * @author ye91009
 */
class NMRTableCellRenderer extends DefaultTableCellRenderer {
  Color red, yellow, green, white;
  double redLevel, yellowLevel;

  public NMRTableCellRenderer() {
    super();
    red = new Color(255, 200, 200); //RED
    yellow = new Color(255, 255, 200); //YELLOW
    green = new Color(200, 255, 200);//green
    white = new Color(255, 255, 255);//white
  }

  public void setRedLevel(double d) {
    redLevel = d;
  }

  public void setYellowLevel(double d) {
    yellowLevel = d;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus, int row,
                                                 int column) {

    if (isSelected) {
      setForeground(table.getSelectionForeground());
      setBackground(table.getSelectionBackground());
    } else {
      setBackground(white); // as a default
      // Expect value to be a Measure object 
      // Dont do colour coding if expVlaue isn't set
      if (value != null && ((Measure) value).getExpValue() != null) {
        try {

          double myValue = ((Measure) value).getDiff();
          if (myValue >= redLevel) {
            setBackground(red);
          } else if (myValue >= yellowLevel && myValue < redLevel) {
            setBackground(yellow);
          } else {
            setBackground(green);
          }

        } catch (Exception e) {
          setBackground(white);
        }
      }
    }

    setFont(table.getFont());

    if (hasFocus) {
      setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      if (table.isCellEditable(row, column)) {
        super.setForeground(UIManager.getColor("Table.focusCellForeground"));
        super.setBackground(UIManager.getColor("Table.focusCellBackground"));
      }
    } else {
      setBorder(noFocusBorder);
    }

    setText(((Measure) value).round());

    return this;
  }

}
