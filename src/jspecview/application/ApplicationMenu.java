/* Copyright (C) 2002-2012  The JSpecView Development Team
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

// CHANGES to 'mainFrame.java' - Main Application GUI
// University of the West Indies, Mona Campus
//
// 20-06-2005 kab - Implemented exporting JPG and PNG image files from the application
//                - Need to sort out JSpecViewFileFilters for Save dialog to include image file extensions
// 21-06-2005 kab - Adjusted export to not prompt for spectrum when exporting JPG/PNG
// 24-06-2005 rjl - Added JPG, PNG file filters to dialog
// 30-09-2005 kab - Added command-line support
// 30-09-2005 kab - Implementing Drag and Drop interface (new class)
// 10-03-2006 rjl - Added Locale overwrite to allow decimal points to be recognised correctly in Europe
// 25-06-2007 rjl - Close file now checks to see if any remaining files still open
//                - if not, then remove a number of menu options
// 05-07-2007 cw  - check menu options when changing the focus of panels
// 06-07-2007 rjl - close imported file closes spectrum and source and updates directory tree
// 06-11-2007 rjl - bug in reading displayschemes if folder name has a space in it
//                  use a default scheme if the file can't be found or read properly,
//                  but there will still be a problem if an attempt is made to
//                  write out a new scheme under these circumstances!
// 23-07-2011 jak - altered code to support drawing scales and units separately
// 21-02-2012 rmh - lots of additions  -  integrated into Jmol

package jspecview.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;

import javajs.util.Lst;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


import jspecview.api.JSVPanel;
import jspecview.common.Spectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ScriptToken;
import jspecview.source.JDXSource;

/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class ApplicationMenu extends JMenuBar {

  private static final long serialVersionUID = 1L;
  protected MainFrame mainFrame;
  protected JSViewer viewer;

  public ApplicationMenu(MainFrame si) throws Exception {
    this.mainFrame = si;
    viewer = si.vwr;
    jbInit();
  }

  // the only really necessary as fields:
  private JMenu processingMenu;
  private JMenu displayMenu;
  private JMenu exportAsMenu;
  private JMenu openRecentMenu;
  private JMenu saveAsMenu;
  private JMenu saveAsJDXMenu;
  
  private JMenuItem closeMenuItem;
  private JMenuItem closeAllMenuItem;
  private JMenuItem errorLogMenuItem;
  private JMenuItem printMenuItem;
  private JMenuItem sourceMenuItem;
	private JMenuItem integrationMenuItem;
  private JMenuItem overlayKeyMenuItem;
	private JMenuItem transmittanceMenuItem;
	private JMenuItem solutionColorMenuItem;

  private JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem pointsOnlyCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem revPlotCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleXCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleYCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem toolbarCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem sidePanelCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem statusCheckBoxMenuItem = new JCheckBoxMenuItem();


  /**
   * Initializes GUI components
   * 
   * @throws Exception
   */
  private void jbInit() throws Exception {
  	
    JMenuItem openFileMenuItem = setMenuItem(null, 'F', "Add File...", 70,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.openFileFromDialog(true, false, null, null);
          }
        });
    JMenuItem openSimulationH1MenuItem = setMenuItem(null, 'H', "Add H1 Simulation...", 72,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.openFileFromDialog(true, false, "H1", null);
          }
        });
    JMenuItem openSimulationC13MenuItem = setMenuItem(null, 'C', "Add C13 Simulation...", 67,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            viewer.openFileFromDialog(true, false, "C13", null);
          }
        });
    JMenuItem openURLMenuItem = setMenuItem(null, 'U', "Add URL...", 85,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.openFileFromDialog(true, true, null, null);
          }
        });
    
    printMenuItem = setMenuItem(null, 'P', "Print...", 80,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("print");
          }
        });
    closeMenuItem = setMenuItem(null, 'C', "Close", 115,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("CLOSE");
          }
        });
    closeAllMenuItem = setMenuItem(null, 'L', "Close All", 0,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("CLOSE ALL");
          }
        });
    JMenuItem scriptMenuItem = setMenuItem(null, 'T', "Script...", 83,
        InputEvent.ALT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("script INLINE");
          }
        });
    
    JMenuItem exitMenuItem = setMenuItem(null, 'X', "Exit", 115,
        InputEvent.ALT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            mainFrame.exitJSpecView(false);
          }
        });

    setMenuItem(gridCheckBoxMenuItem, 'G', "Grid", 71,
        InputEvent.CTRL_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.GRIDON, e);
          }
        });
    setMenuItem(coordsCheckBoxMenuItem, 'C', "Coordinates",
        67, InputEvent.CTRL_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.COORDINATESON, e);
          }
        });
    setMenuItem(pointsOnlyCheckBoxMenuItem, 'P', "Points Only",
        80, InputEvent.CTRL_MASK, new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.POINTSONLY, e);
          }
        });
    setMenuItem(revPlotCheckBoxMenuItem, 'R', "Reverse Plot",
        82, InputEvent.CTRL_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.REVERSEPLOT, e);
          }
        });
    setMenuItem(scaleXCheckBoxMenuItem, 'X', "X Scale", 88,
        InputEvent.CTRL_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.XSCALEON, e);
          }
        });
    setMenuItem(scaleYCheckBoxMenuItem, 'Y', "Y Scale", 89,
        InputEvent.CTRL_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.YSCALEON, e);
          }
        });
    JMenuItem nextZoomMenuItem = setMenuItem(null, 'N', "Next View", 78,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("zoom next");
          }
        });
    JMenuItem prevZoomMenuItem = setMenuItem(null, 'P', "Previous View", 80,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("zoom previous");
          }
        });
    JMenuItem fullZoomMenuItem = setMenuItem(null, 'F', "Full View", 70,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("zoom out");
          }
        });
    JMenuItem clearZoomMenuItem = setMenuItem(null, 'C', "Clear Views", 67,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("zoom clear");
          }
        });
    JMenuItem userZoomMenuItem = setMenuItem(null, 'Z', "Set Zoom...", 90,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("zoom ?");
          }
        });
    JMenuItem viewAllMenuItem = setMenuItem(null, 'A', "All Spectra", 65,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("view all");
          }
        });
    JMenuItem spectraMenuItem = setMenuItem(null, 'S', "Selected Spectra...", 83,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("view");
          }
        });
    JMenuItem overlayStackOffsetYMenuItem = setMenuItem(null, 'y', "Overlay Offset...", 0,
        0, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						viewer.runScript("stackOffsetY ?");
					}
        });
    sourceMenuItem = setMenuItem(null, 'S', "Source ...", 83,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("showSource");
          }
        });
    errorLogMenuItem = setMenuItem(null, '\0', "Error Log ...", 0,
        0, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("showErrors");
          }
        });


    JMenuItem propertiesMenuItem = setMenuItem(null, 'P', "Properties", 72,
        InputEvent.CTRL_MASK, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("showProperties");
          }
        });
    overlayKeyMenuItem = setMenuItem(null, '\0', "Overlay Key", 0,
        0, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            viewer.runScript("showKey toggle");
          }
        });

    JMenuItem preferencesMenuItem = setMenuItem(null, 'P', "Preferences...",
        0, 0, new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            showPreferencesDialog();
          }
        });
    JMenuItem aboutMenuItem = setMenuItem(null, '\0', "About", 0, 0,
        new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            new AboutDialog(mainFrame);
          }
        });
    setMenuItem(toolbarCheckBoxMenuItem, 'T', "Toolbar", 84,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            mainFrame.enableToolbar(e.getStateChange() == ItemEvent.SELECTED);
          }
        });

    setMenuItem(sidePanelCheckBoxMenuItem, 'S', "Side Panel",
        83, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            mainFrame.setSplitPane(e.getStateChange() == ItemEvent.SELECTED);
          }
        });

    setMenuItem(statusCheckBoxMenuItem, 'B', "Status Bar",
        66, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          @Override
					public void itemStateChanged(ItemEvent e) {
            mainFrame.enableStatus(e.getStateChange() == ItemEvent.SELECTED);
          }
        });

    JMenu fileMenu = new JMenu();
    fileMenu.setMnemonic('F');
    fileMenu.setText("File");

    JMenu helpMenu = new JMenu();
    helpMenu.setMnemonic('H');
    helpMenu.setText("Help");

    JMenu optionsMenu = new JMenu();
    optionsMenu.setMnemonic('O');
    optionsMenu.setText("Options");

    displayMenu = new JMenu();
    displayMenu.setMnemonic('D');
    displayMenu.setText("Display");
    displayMenu.addMenuListener(new MenuListener() {
      @Override
			public void menuSelected(MenuEvent e) {
        doMenuSelected();
      }

      @Override
			public void menuDeselected(MenuEvent e) {
      }

      @Override
			public void menuCanceled(MenuEvent e) {
      }
    });

    JMenu zoomMenu = new JMenu();
    zoomMenu.setMnemonic('Z');
    zoomMenu.setText("Zoom");
    
    openRecentMenu = new JMenu();
    openRecentMenu.setActionCommand("OpenRecent");
    openRecentMenu.setMnemonic('R');
    openRecentMenu.setText("Add Recent");

    saveAsMenu = new JMenu();
    saveAsMenu.setMnemonic('A');
    
    saveAsJDXMenu = new JMenu();
    saveAsJDXMenu.setMnemonic('J');
    
    exportAsMenu = new JMenu();
    exportAsMenu.setMnemonic('E');

    processingMenu = new JMenu();
    processingMenu.setMnemonic('P');
    processingMenu.setText("Processing");
    processingMenu.addMenuListener(new MenuListener() {
      @Override
			public void menuSelected(MenuEvent e) {
        //jsvpPopupMenu.setEnables(mainFrame.viewer.selectedPanel);
      }

      @Override
			public void menuDeselected(MenuEvent e) {
      }

      @Override
			public void menuCanceled(MenuEvent e) {
      }
    });
    setProcessingMenu(processingMenu);

    fileMenu.add(openFileMenuItem);
    fileMenu.add(openSimulationH1MenuItem);
    fileMenu.add(openSimulationC13MenuItem);
    fileMenu.add(openURLMenuItem);
    fileMenu.add(openRecentMenu);
    // application does not need append
    fileMenu.addSeparator();
    fileMenu.add(closeMenuItem).setEnabled(false);
    fileMenu.add(closeAllMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(scriptMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(saveAsMenu).setEnabled(false);
    fileMenu.add(exportAsMenu).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(printMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    displayMenu.add(viewAllMenuItem);
    displayMenu.add(spectraMenuItem);
    displayMenu.add(overlayStackOffsetYMenuItem);
    displayMenu.add(overlayKeyMenuItem).setEnabled(false);
    displayMenu.addSeparator();
    displayMenu.add(gridCheckBoxMenuItem);
    displayMenu.add(coordsCheckBoxMenuItem);
    displayMenu.add(scaleXCheckBoxMenuItem);
    displayMenu.add(scaleYCheckBoxMenuItem);
    displayMenu.add(revPlotCheckBoxMenuItem);
    displayMenu.add(pointsOnlyCheckBoxMenuItem);
    displayMenu.addSeparator();
    displayMenu.add(zoomMenu);
    displayMenu.addSeparator();
    displayMenu.add(sourceMenuItem).setEnabled(false);
    displayMenu.add(errorLogMenuItem).setEnabled(false);
    displayMenu.add(propertiesMenuItem);
    zoomMenu.add(nextZoomMenuItem);
    zoomMenu.add(prevZoomMenuItem);
    zoomMenu.add(fullZoomMenuItem);
    zoomMenu.add(clearZoomMenuItem);
    zoomMenu.add(userZoomMenuItem);
    optionsMenu.add(preferencesMenuItem);
    optionsMenu.addSeparator();
    optionsMenu.add(toolbarCheckBoxMenuItem);
    optionsMenu.add(sidePanelCheckBoxMenuItem);
    optionsMenu.add(statusCheckBoxMenuItem);
    helpMenu.add(aboutMenuItem);
    
    add(fileMenu);
    add(displayMenu).setEnabled(false);
    add(optionsMenu);
    add(processingMenu).setEnabled(false);
    add(helpMenu);

    setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu,
        (new ActionListener() {
          @Override
					public void actionPerformed(ActionEvent e) {
            mainFrame.exportSpectrumViaMenu(e.getActionCommand());
          }
        }));
    
    toolbarCheckBoxMenuItem.setSelected(true);
    sidePanelCheckBoxMenuItem.setSelected(true);
    statusCheckBoxMenuItem.setSelected(true);

  }

	protected void doMenuSelected() {
		PanelData pd = mainFrame.vwr.pd();
    gridCheckBoxMenuItem.setSelected(pd != null && pd.getBoolean(ScriptToken.GRIDON));
    coordsCheckBoxMenuItem.setSelected(pd != null && pd.getBoolean(ScriptToken.COORDINATESON));
    pointsOnlyCheckBoxMenuItem.setSelected(pd != null && pd.getBoolean(ScriptToken.POINTSONLY));
    revPlotCheckBoxMenuItem.setSelected(pd != null && pd.getBoolean(ScriptToken.REVERSEPLOT));
	}

	public void setProcessingMenu(JComponent menu) {
		menu.add(integrationMenuItem = setMenuItem(null, 'I', "Integration", 0, 0,
	    new ActionListener() {
	      @Override
				public void actionPerformed(ActionEvent e) {
	        viewer.runScript("showIntegration");
	      }
	    }));
	menu.add(setMenuItem(null, 'M', "Measurements", 0, 0,
	    new ActionListener() {
	      @Override
				public void actionPerformed(ActionEvent e) {
	      	viewer.runScript("showMeasurements");
	      }
	    }));
	menu.add(setMenuItem(null, 'P', "Peaks", 0, 0,
	    new ActionListener() {
	      @Override
				public void actionPerformed(ActionEvent e) {
	        viewer.runScript("showPeakList");
	      }
	    }));
	menu.add(transmittanceMenuItem = setMenuItem(null, '\0', "Transmittance/Absorbance", 0, 0,
	    new ActionListener() {
	      @Override
				public void actionPerformed(ActionEvent e) {
	        viewer.runScript("IRMODE IMPLIED");
	      }
	    }));
	menu.add(solutionColorMenuItem = setMenuItem(null, 'C', "Predicted Solution Colour", 0, 0,
	    new ActionListener() {
	      @Override
				public void actionPerformed(ActionEvent e) {
	        viewer.runScript("GETSOLUTIONCOLOR");
	      }
	    }));
	}

  protected void setBoolean(ScriptToken st, ItemEvent e) {
    boolean isOn = (e.getStateChange() == ItemEvent.SELECTED);
    viewer.runScript(st + " " + isOn);
  }

  public void setSourceEnabled(boolean b) {
    closeAllMenuItem.setEnabled(b);
    displayMenu.setEnabled(b);
    processingMenu.setEnabled(b);
    exportAsMenu.setEnabled(b);
    saveAsMenu.setEnabled(b);

    printMenuItem.setEnabled(b);
    sourceMenuItem.setEnabled(b);
    errorLogMenuItem.setEnabled(b);
  }

  void setCloseMenuItem(String fileName) {
    closeMenuItem.setEnabled(fileName != null);
    closeMenuItem.setText(fileName == null ? "Close" : "Close " + fileName);
  }

  /**
	 * @param isError 
   * @param isWarningOnly  
	 */
  void setError(boolean isError, boolean isWarningOnly) {
    errorLogMenuItem.setEnabled(isError);
  }

  public void setMenuEnables(PanelNode node) {
    if (node == null) {
      setCloseMenuItem(null);
      setSourceEnabled(false);
    } else {
      setSourceEnabled(true);
      PanelData pd = node.pd();
      Spectrum spec = pd.getSpectrum();
      setCheckBoxes(pd);
      overlayKeyMenuItem.setEnabled(pd.getNumberOfGraphSets() > 1);
      setCloseMenuItem(JSVFileManager.getTagName(node.source.getFilePath()));
      exportAsMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
      saveAsJDXMenu.setEnabled(spec.canSaveAsJDX());
      integrationMenuItem.setEnabled(spec.canIntegrate());
      transmittanceMenuItem.setEnabled(spec.isAbsorbance() || spec.isTransmittance());
      solutionColorMenuItem.setEnabled(spec.canShowSolutionColor());
    }

  }

  public boolean toggleOverlayKeyMenuItem() {
    overlayKeyMenuItem.setSelected(overlayKeyMenuItem.isSelected());
    return overlayKeyMenuItem.isSelected();
  }

  ////////// MENU ACTIONS ///////////

  /**
   * Shows the preferences dialog
   * 
   */
  void showPreferencesDialog() {
    mainFrame.showPreferences();
  }

  public void setSelections(boolean sidePanelOn, boolean toolbarOn,
                            boolean statusbarOn, JSVPanel jsvp) {
    // hide side panel if sidePanelOn property is false
    sidePanelCheckBoxMenuItem.setSelected(sidePanelOn);
    toolbarCheckBoxMenuItem.setSelected(toolbarOn);
    statusCheckBoxMenuItem.setSelected(statusbarOn);
    if (jsvp != null)
      setCheckBoxes(jsvp.getPanelData());
  }

  private void setCheckBoxes(PanelData pd) {
    gridCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.GRIDON));
    coordsCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.COORDINATESON));
    revPlotCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.REVERSEPLOT));
    scaleXCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.XSCALEON));
    scaleYCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.YSCALEON));
  }

  public void setRecentMenu(Lst<String> recentFilePaths) {
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilePaths.size(); i++) {
      String path = recentFilePaths.get(i);
      JMenuItem menuItem;
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        @Override
				public void actionPerformed(ActionEvent e) {
          viewer.openFile(((JMenuItem) e.getSource()).getText(), false);
        }
      });
    }
  }

  public void updateRecentMenus(Lst<String> recentFilePaths) {
    JMenuItem menuItem;
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilePaths.size(); i++) {
      String path = recentFilePaths.get(i);
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        @Override
				public void actionPerformed(ActionEvent e) {
          viewer.openFile(((JMenuItem) e.getSource()).getText(), true);
        }
      });
    }
  }

  public void clearSourceMenu(JDXSource source) {
    if (source == null) {
      setMenuEnables(null);
    } else {
      saveAsJDXMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
    }
    //setCloseMenuItem(null);

  }

	public static void addMenuItem(JMenu m, String key, char keyChar,
			ActionListener actionListener) {
		JMenuItem jmi = new JMenuItem();
		jmi.setMnemonic(keyChar == '\0' ? key.charAt(0) : keyChar);
		jmi.setText(key);
		jmi.addActionListener(actionListener);
		m.add(jmi);
	}

	public static void setMenus(JMenu saveAsMenu, JMenu saveAsJDXMenu,
	                            JMenu exportAsMenu, ActionListener actionListener) {
	  saveAsMenu.setText("Save As");
	  addMenuItem(saveAsMenu, JSViewer.sourceLabel, '\0', actionListener);
	  saveAsJDXMenu.setText("JDX");
	  addMenuItem(saveAsJDXMenu, "XY", '\0', actionListener);
	  addMenuItem(saveAsJDXMenu, "DIF", '\0', actionListener);
	  addMenuItem(saveAsJDXMenu, "DIFDUP", 'U', actionListener);
	  addMenuItem(saveAsJDXMenu, "FIX", '\0', actionListener);
	  addMenuItem(saveAsJDXMenu, "PAC", '\0', actionListener);
	  addMenuItem(saveAsJDXMenu, "SQZ", '\0', actionListener);
	  saveAsMenu.add(saveAsJDXMenu);
	  addMenuItem(saveAsMenu, "CML", '\0', actionListener);
	  addMenuItem(saveAsMenu, "XML (AnIML)", '\0', actionListener);
	  if (exportAsMenu != null) {
	    exportAsMenu.setText("Export As");
	    addMenuItem(exportAsMenu, "JPG", '\0', actionListener);
	    addMenuItem(exportAsMenu, "PNG", 'N', actionListener);
	    addMenuItem(exportAsMenu, "SVG", '\0', actionListener);
	    addMenuItem(exportAsMenu, "PDF", '\0', actionListener);
	  }
	}

	public static JMenuItem setMenuItem(JMenuItem item, char c, String text,
	                         int accel, int mask, EventListener el) {
		if (item == null)
			item = new JMenuItem();
	  if (c != '\0')
	    item.setMnemonic(c);
	  item.setText(text);
	  if (accel > 0)
	    item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(accel,
	        mask, false));
	  if (el instanceof ActionListener)
	    item.addActionListener((ActionListener) el);
	  else if (el instanceof ItemListener)
	    item.addItemListener((ItemListener) el);
	  return item;
	}


}
