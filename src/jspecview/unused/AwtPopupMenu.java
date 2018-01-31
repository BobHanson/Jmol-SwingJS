/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.unused;

//import java.awt.Container;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//
//import javax.swing.JCheckBoxMenuItem;
//import javax.swing.JComponent;
//import javax.swing.JMenu;
//import javax.swing.JMenuItem;
//import javax.swing.JPopupMenu;
//
//import org.jmol.util.JmolList;
//import org.jmol.util.Logger;
//
//import jspecview.api.JSVPanel;
//import jspecview.api.JSVPopupMenu;
//import jspecview.application.AppMenu;
//import jspecview.common.JDXSpectrum;
//import jspecview.common.JSVPanelNode;
//import jspecview.common.JSViewer;
//import jspecview.common.PanelData;
//import jspecview.common.ScriptToken;
//import jspecview.common.Annotation.AType;

/**
 * see JSVPopupResourceBundle
 * 
 * Popup Menu for JSVPanel.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.api.JSVPanel
 */
public class AwtPopupMenu {//extends JPopupMenu implements JSVPopupMenu {
//
//  protected boolean isApplet;
//
//  private static final long serialVersionUID = 1L;
//
//  protected JSViewer viewer;
//  
//  public void dispose() {
//    pd = null;
//    //scripter = null;
//  }
//
//  /**
//   * Menu Item that allows user to navigate to the next view of a JSVPanel
//   * that has been zoomed
//   */
//  public JMenuItem nextMenuItem = new JMenuItem();
//  /**
//   * Menu Item for navigating to previous view
//   */
//  public JMenuItem previousMenuItem = new JMenuItem();
//  /**
//   * Allows for all view to be cleared
//   */
//  public JMenuItem clearMenuItem = new JMenuItem();
//  /**
//   * Allows for the JSVPanel to be reset to it's original display
//   */
//  public JMenuItem resetMenuItem = new JMenuItem();
//  /**
//   * Allows for the viewing of the properties of the Spectrum that is
//   * displayed on the <code>JSVPanel</code>
//   */
//  public JMenuItem properties = new JMenuItem();
//
////  protected JMenuItem userZoomMenuItem = new JMenuItem();
//  protected JMenuItem scriptMenuItem = new JMenuItem();
//  public JMenuItem overlayStackOffsetMenuItem = new JMenuItem();
//  protected JCheckBoxMenuItem windowMenuItem = new JCheckBoxMenuItem();
//
//  public JMenuItem integrationMenuItem = new JMenuItem();
//  public JMenuItem measurementsMenuItem = new JMenuItem();
//  public JMenuItem peakListMenuItem = new JMenuItem();
//  public JMenuItem transAbsMenuItem = new JMenuItem();
//  public JMenuItem solColMenuItem = new JMenuItem();
//  
//  public JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
//  public JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
//  public JCheckBoxMenuItem reversePlotCheckBoxMenuItem = new JCheckBoxMenuItem();
//
//
//// applet only:
//  
//  protected JMenu appletSaveAsJDXMenu; // applet only
//  protected JMenu appletExportAsMenu;  // applet only
//  //protected JMenuItem appletAdvancedMenuItem;
//  public JMenuItem spectraMenuItem = new JMenuItem();
//  public JMenuItem overlayKeyMenuItem = new JMenuItem();
//  
//	public void initialize(JSViewer viewer, String menu) {
//		// TODO Auto-generated method stub
//    this.viewer = viewer;
//    jbInit();		
//	}
//
//  /**
//   * Initialises GUI components
//   */
//  protected void jbInit() {
//    nextMenuItem.setText("Next View");
//    nextMenuItem.addActionListener(new java.awt.event.ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//      	runScript("view next;showmenu");
//      }
//    });
//    previousMenuItem.setText("Previous View");
//    previousMenuItem.addActionListener(new java.awt.event.ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//      	runScript("view previous;showmenu");
//      }
//    });
//    clearMenuItem.setText("Clear Views");
//    clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//      	runScript("view clear");
//      }
//    });
//    resetMenuItem.setText("Reset View");
//    resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//      	runScript("view reset");
//      }
//    });
//    
//    setOverlayItems();
//    
//    scriptMenuItem.setText("Script...");
//    scriptMenuItem.addActionListener(new ActionListener() {
//       public void actionPerformed(ActionEvent e) {
//         runScript("script INLINE");
//       }
//     });
////    userZoomMenuItem.setText("Set Zoom...");
////    userZoomMenuItem.addActionListener(new ActionListener() {
////       public void actionPerformed(ActionEvent e) {
////         runScript("zoom");
////       }
////     });
//    properties.setActionCommand("Properties");
//    properties.setText("Properties");
//    properties.addActionListener(new java.awt.event.ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//      	runScript("showProperties");
//      }
//    });
//    gridCheckBoxMenuItem.setText("Show Grid");
//    gridCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        runScript("GRIDON toggle;showMenu");
//      }
//    });
//    coordsCheckBoxMenuItem.setText("Show Coordinates");
//    coordsCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        runScript("COORDINATESON toggle;showMenu");
//      }
//    });
//    reversePlotCheckBoxMenuItem.setText("Reverse Plot");
//    reversePlotCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        runScript("REVERSEPLOT toggle;showMenu");
//      }
//    });
//    
//    setPopupMenu();
//  }
//
//  protected void setOverlayItems() {
//    spectraMenuItem.setText("Views...");
//    spectraMenuItem.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				runScript("view");
//			}
//     });
//    overlayStackOffsetMenuItem.setEnabled(false);
//    overlayStackOffsetMenuItem.setText("Overlay Offset...");
//    overlayStackOffsetMenuItem.addActionListener(new ActionListener() {
//       public void actionPerformed(ActionEvent e) {
//         runScript("stackOffsetY");
//       }
//     });
//	}
//
//	/**
//   * overridden in applet
//   */
//  protected void setPopupMenu() {
//    add(gridCheckBoxMenuItem);
//    add(coordsCheckBoxMenuItem);
//    add(reversePlotCheckBoxMenuItem);
//    addSeparator();
//    add(nextMenuItem);
//    add(previousMenuItem);
//    add(clearMenuItem);
//    add(resetMenuItem);
////    add(userZoomMenuItem);
//    addSeparator();
//    add(spectraMenuItem);
//    add(overlayStackOffsetMenuItem);
//    add(scriptMenuItem);
//    addSeparator();
//    add(properties);
//  }
//  protected void reboot() {
//    if (thisJsvp == null)
//      return;
//    thisJsvp.doRepaint();
//    show((Container) thisJsvp, thisX, thisY);
//  }
//
//  public void setProcessingMenu(JComponent menu) {
//    AppMenu.setMenuItem(integrationMenuItem, 'I', "Integration", 0, 0,
//        new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            runScript("showIntegration");
//          }
//        });
//    AppMenu.setMenuItem(measurementsMenuItem, 'M', "Measurements", 0, 0,
//        new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            runScript("showMeasurements");
//          }
//        });
//    AppMenu.setMenuItem(peakListMenuItem, 'P', "Peaks", 0, 0,
//        new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            runScript("showPeakList");
//          }
//        });
//    AppMenu.setMenuItem(transAbsMenuItem, '\0', "Transmittance/Absorbance", 0, 0,
//        new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            runScript("IRMODE IMPLIED");
//          }
//        });
//    AppMenu.setMenuItem(solColMenuItem, 'C', "Predicted Solution Colour", 0, 0,
//        new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            runScript("GETSOLUTIONCOLOR");
//          }
//        });
//    menu.add(measurementsMenuItem);
//    menu.add(peakListMenuItem);
//    menu.add(integrationMenuItem);
//    menu.add(transAbsMenuItem);
//    menu.add(solColMenuItem);
//  }
//
//  public void runScript(String cmd) {
//    if (viewer == null)
//      Logger.error("viewer was null for " + cmd);
//    else
//      viewer.runScript(cmd);
//  }
//
//  private PanelData pd;
//  private int thisX, thisY;
//  protected JSVPanel thisJsvp;
//  
//	public void jpiShow(int x, int y) {
//		thisJsvp = viewer.selectedPanel;
//		setEnables(thisJsvp);
//		if (x == Integer.MIN_VALUE) {
//			x = thisX;
//			y = thisY;
//		} else {
//			thisX = x;
//			thisY = y;
//		}
//		super.show((Container) thisJsvp, x, y);
//	}
//
//  public void setEnables(JSVPanel jsvp) {
//    pd = jsvp.getPanelData();
//    JDXSpectrum spec0 = pd.getSpectrum();
//    setSelected(gridCheckBoxMenuItem, pd.getBoolean(ScriptToken.GRIDON));
//    setSelected(coordsCheckBoxMenuItem, pd.getBoolean(ScriptToken.COORDINATESON));
//    setSelected(reversePlotCheckBoxMenuItem, pd.getBoolean(ScriptToken.REVERSEPLOT));
//
//    boolean isOverlaid = pd.isShowAllStacked();
//    boolean isSingle = pd.haveSelectedSpectrum();
//    
//    integrationMenuItem.setEnabled(pd.getSpectrum().canIntegrate());
//    measurementsMenuItem.setEnabled(pd.hasCurrentMeasurements(AType.Measurements));
//    peakListMenuItem.setEnabled(pd.getSpectrum().is1D());
//    
//    solColMenuItem.setEnabled(isSingle && spec0.canShowSolutionColor());
//    transAbsMenuItem.setEnabled(isSingle && spec0.canConvertTransAbs());
//    overlayKeyMenuItem.setEnabled(isOverlaid && pd.getNumberOfGraphSets() == 1);
//    overlayStackOffsetMenuItem.setEnabled(isOverlaid);
//    // what about its selection???
//    if (appletSaveAsJDXMenu != null)
//      appletSaveAsJDXMenu.setEnabled(spec0.canSaveAsJDX());
//    if (appletExportAsMenu != null)
//      appletExportAsMenu.setEnabled(true);
//    //if (appletAdvancedMenuItem != null)
//      //appletAdvancedMenuItem.setEnabled(!isOverlaid);
//  }
//
//  private void setSelected(JCheckBoxMenuItem item, boolean TF) {
//    item.setEnabled(false);
//    item.setSelected(TF);
//    item.setEnabled(true);
//  }
//
//  public void setCompoundMenu(JmolList<JSVPanelNode> panelNodes,
//			boolean allowSelection) {
//		spectraMenuItem.setEnabled(allowSelection && panelNodes.size() > 1);		
//		spectraMenuItem.setEnabled(true);
//	}
//
//	public boolean getSelected(String key) {
//		if (key.equals("overlay"))
//			return overlayKeyMenuItem.isSelected();
//		return false;
//	}
//	
//	public void setSelected(String key, boolean b) {
//		if (key.equals("Window"))
//			windowMenuItem.setSelected(false);
//	}
//
//	public void setEnabled(boolean allowMenu, boolean zoomEnabled) {
//		// applet only
//	}
//

}
