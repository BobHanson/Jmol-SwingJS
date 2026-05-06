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
 */
package org.openscience.jmol.app.janocchio;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.DisplayPanel;
import org.openscience.jmol.app.jmolpanel.JmolPanel;
import org.openscience.jmol.app.jmolpanel.StatusBar;

public class NMR_DisplayPanel extends DisplayPanel {

  StatusBar status;
  NmrGuiMap guimap;
  NMR_Viewer viewer;
  FrameDeltaDisplay frameDeltaDisplay;
  PopulationDisplay populationDisplay;

  NMR_DisplayPanel(JmolPanel jmol) {
    super(jmol);
  }

  @Override
  public void setViewer(Viewer viewer) {
    this.viewer = (NMR_Viewer) viewer;
    this.vwr = viewer;
    getSize(dimSize);
    Dimension d = (haveDisplay && dimSize.width > 0 ? dimSize : startupDim);
    setSize(d);
    setPreferredSize(d);
    viewer.setScreenDimension(d.width, d.height);
    //    super.setViewer(viewer);
  }

  public void setPopulationDisplay(PopulationDisplay populationDisplay) {
    this.populationDisplay = populationDisplay;
  }

  public void setFrameDeltaDisplay(FrameDeltaDisplay frameDeltaDisplay) {
    this.frameDeltaDisplay = frameDeltaDisplay;
  }

  // The actions:

  void moveTo(String move) {
    if (viewer.getShowBbcage() || viewer.getBooleanProperty("showUnitCell"))
      viewer.evalStringQuiet(move);
    else
      viewer.evalStringQuiet("boundbox on;" + move + ";delay 1;boundbox off");
  }

  class PopulationDisplayAction extends AbstractAction {

    public PopulationDisplayAction() {
      super("populationDisplayCheck");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      populationDisplay.setVisible(cbmi.isSelected());
    }
  }

  class FrameDeltaDisplayAction extends AbstractAction {

    public FrameDeltaDisplayAction() {
      super("frameDeltaDisplayCheck");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      frameDeltaDisplay.setVisible(cbmi.isSelected());
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      viewer.evalStringQuiet("select all");
    }
  }

  class DeselectallAction extends AbstractAction {

    public DeselectallAction() {
      super("deselectall");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      viewer.evalStringQuiet("select none");
    }
  }

  class PickAction extends AbstractAction {

    public PickAction() {
      super("pick");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      viewer.setSelectionHalos(false);
      status.setStatus(1, GT.$("Select Atoms"));
    }
  }

  class RotateAction extends AbstractAction {

    public RotateAction() {
      super("rotate");
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      viewer.setSelectionHalos(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  @Override
  public void addActions(List<Action> list) {
    super.addActions(list);
    list.add(new PickAction());
    list.add(new RotateAction());
    list.add(new PopulationDisplayAction());
    list.add(new FrameDeltaDisplayAction());
    list.add(new SelectallAction());
    list.add(new DeselectallAction());
  }
}
