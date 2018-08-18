/* $RCSfile$
 * J. Gutow
 * $July 22, 2011$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.Hashtable;


import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
//import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.openscience.jmol.app.HistoryFile;

import java.util.List;

/**
 * GUI for the Jmol surfaceTool
 * 
 * @author Jonathan Gutow (gutow@uwosh.edu)
 */
class SurfaceToolGUI extends JPanel implements WindowConstants, WindowListener,
    WindowFocusListener, ChangeListener, ActionListener, ListSelectionListener {

  private HistoryFile historyFile;
  private String histWinName;
  private JFrame slicerFrame;
  private SurfaceTool slicer;
  private JPanel tabPanel;
  private JPanel objectsPanel;
  private JPanel topPanel;
  private JPanel angleUnitsPanel;
  private JComboBox<String> angleUnitsList;
  private JPanel originPanel;
  private JRadioButton viewCenterButton;
  private JRadioButton absoluteButton;
  private JCheckBox capCheck;
  private JPanel capPlanesPanel;
  private JPanel ghostPanel;
  private JCheckBox ghostCheck;
  private JCheckBox boundaryPlaneCheck;
  private JPanel sliderPanel;
  private JPanel normAnglePanel;
  private JSlider angleXYSlider;
  //  private JSpinner angleXYSpinner;
  private JSlider angleZSlider;
  //  private JSpinner angleZSpinner;
  private JPanel positionThicknessPanel;
  private JSlider positionSlider;
  //  private JSpinner positionSpinner;
  private JSlider thicknessSlider;
  //  private JSpinner thicknessSpinner;
  private ButtonGroup whichOrigin;
  private JScrollPane surfaceScrollPane;
  private JList<SurfaceStatus> surfaceList;

  /**
   * Builds and opens a GUI to control slicing. Called automatically when a new
   * SurfaceTool is created with useGUI = true.
   * 
   * @param vwr
   *        (JmolViewer) the vwr that called for this surfaceTool.
   * @param hfile
   *        (HistoryFile) the history file used by this instance of Jmol
   * @param winName
   *        (String) name used for this window in history probably
   *        JmolPanel.SURFACETOOL_WINDOW_NAME
   * @param slicer
   *        (SurfaceTool) the surfaceTool that activated this GUI
   */
  SurfaceToolGUI(JmolViewer vwr, HistoryFile hfile, String winName,
      SurfaceTool slicer) {
    super(new BorderLayout());
    this.historyFile = hfile;
    this.histWinName = winName;
    this.slicer = slicer;
    if (slicerFrame != null) {
      slicerFrame.setVisible(true);
      slicerFrame.toFront();
    } else {
      slicerFrame = new JFrame(GT.$("SurfaceTool"));
      slicerFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      String imageName = "org/openscience/jmol/app/images/icon.png";
      URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
      ImageIcon jmolIcon = new ImageIcon(imageUrl);
      slicerFrame.setIconImage(jmolIcon.getImage());
      slicerFrame.addWindowFocusListener(this);
      slicerFrame.addWindowListener(this);
      //Create and set up the content pane.
      setOpaque(true); //content panes must be opaque

      //left tabpanel
      tabPanel = new JPanel(new BorderLayout());

      //Top panel
      topPanel = new JPanel(new GridLayout(1, 0));
      whichOrigin = new ButtonGroup();
      originPanel = new JPanel(new GridLayout(0, 1));
      if (slicer.getUseMolecular()) {
        viewCenterButton = new JRadioButton(GT.$("View Center"), false);
        absoluteButton = new JRadioButton(GT.$("Absolute"), true);
      } else {
        viewCenterButton = new JRadioButton(GT.$("View Center"), true);
        absoluteButton = new JRadioButton(GT.$("Absolute"), false);
      }
      viewCenterButton.addActionListener(this);
      absoluteButton.addActionListener(this);
      whichOrigin.add(viewCenterButton);
      whichOrigin.add(absoluteButton);
      originPanel.add(viewCenterButton);
      originPanel.add(absoluteButton);
      originPanel.setBorder(BorderFactory.createTitledBorder(GT.$("Origin")));

      capPlanesPanel = new JPanel(new GridLayout(0, 1));
      capCheck = new JCheckBox(GT.$("Cap"));
      capCheck
          .setToolTipText(GT
              .$("Caps slice with opaque surfaces.\nIgnores MOs and surfaces with interior layers."));
      capCheck.setSelected(slicer.getCapOn());
      capCheck.addActionListener(this);
      capPlanesPanel.add(capCheck);
      boundaryPlaneCheck = new JCheckBox(GT.$("Slice Planes"));
      boundaryPlaneCheck.setToolTipText(GT
          .$("Shows planes at slicing surfaces."));
      boundaryPlaneCheck.setSelected(false);
      slicer.showSliceBoundaryPlanes(false);
      boundaryPlaneCheck.addActionListener(this);
      capPlanesPanel.add(boundaryPlaneCheck);

      ghostPanel = new JPanel(new GridLayout(0, 1));
      ghostCheck = new JCheckBox(GT.$("Ghost On"));
      ghostCheck.setSelected(slicer.getGhostOn());
      ghostCheck.addActionListener(this);
      ghostCheck.setToolTipText(GT.$("Shows an unsliced \"ghost\"."));
      ghostPanel.add(ghostCheck);

      topPanel.add(originPanel);
      topPanel.add(capPlanesPanel);
      topPanel.add(ghostPanel);
      topPanel.setSize(200, 40);

      //slider panel
      sliderPanel = new JPanel(new GridLayout(0, 1));
      normAnglePanel = new JPanel(new GridLayout(0, 1));
      angleUnitsPanel = new JPanel(new BorderLayout());
      JLabel space = new JLabel("   ");
      angleUnitsPanel.add(space, BorderLayout.WEST);
      String[] angleUnits = slicer.getAngleUnitsList();
      angleUnitsList = new JComboBox<String>(angleUnits);
      angleUnitsList.setSelectedIndex(slicer.getAngleUnits());
      angleUnitsList.addActionListener(this);
      angleUnitsPanel.add(angleUnitsList, BorderLayout.EAST);
      JPanel labelAndUnits = new JPanel(new GridLayout(1, 0));
      JLabel sliderLabel = new JLabel(GT.$("Angle from X-axis in XY plane"),
          SwingConstants.CENTER);
      sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      labelAndUnits.add(sliderLabel);
      labelAndUnits.add(angleUnitsPanel);
      normAnglePanel.add(labelAndUnits);
      angleXYSlider = new JSlider(0, 180, 0);
      angleXYSlider.setMajorTickSpacing(30);
      angleXYSlider.setPaintTicks(true);
      angleXYSlider.addChangeListener(this);
      normAnglePanel.add(angleXYSlider);
      JLabel sliderLabel2 = new JLabel(GT.$("Angle from Z-axis"),
          SwingConstants.CENTER);
      sliderLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);
      normAnglePanel.add(sliderLabel2);
      angleZSlider = new JSlider(0, 180, 0);
      angleZSlider.setMajorTickSpacing(30);
      angleZSlider.setPaintTicks(true);
      angleZSlider.addChangeListener(this);
      updateAngleSliders();
      normAnglePanel.add(angleZSlider);
      normAnglePanel.setBorder(BorderFactory.createTitledBorder(GT
          .$("Direction vector of normal to slice")));
      sliderPanel.add(normAnglePanel);
      positionThicknessPanel = new JPanel(new GridLayout(0, 1));
      JLabel sliderLabel3 = new JLabel(GT.$("Distance of slice from origin"),
          SwingConstants.CENTER);
      sliderLabel3.setAlignmentX(Component.CENTER_ALIGNMENT);
      positionThicknessPanel.add(sliderLabel3);
      int tempPos = (int) (180 * (slicer.getSlicePosition() - slicer
          .getPositionMin()) / slicer.getThicknessMax());
      positionSlider = new JSlider(0, 180, tempPos);
      positionSlider.setMajorTickSpacing(30);
      positionSlider.setPaintTicks(true);
      positionSlider.addChangeListener(this);
      updatePositionSlider();
      positionThicknessPanel.add(positionSlider);
      JLabel sliderLabel4 = new JLabel(GT.$("Thickness of slice"),
          SwingConstants.CENTER);
      sliderLabel4.setAlignmentX(Component.CENTER_ALIGNMENT);
      positionThicknessPanel.add(sliderLabel4);
      thicknessSlider = new JSlider(0, 180,
          (int) (180 * slicer.getSliceThickness() / slicer.getThicknessMax()));
      thicknessSlider.setMajorTickSpacing(30);
      thicknessSlider.setPaintTicks(true);
      thicknessSlider.addChangeListener(this);
      updateThicknessSlider();
      positionThicknessPanel.add(thicknessSlider);
      sliderPanel.add(positionThicknessPanel);
      tabPanel.add(topPanel, BorderLayout.NORTH);
      tabPanel.add(sliderPanel, BorderLayout.SOUTH);

      //objects panel
      objectsPanel = new JPanel();
      objectsPanel.setBorder(BorderFactory.createTitledBorder(GT
          .$("Select Surface(s)")));
      surfaceList = new JList<SurfaceStatus>(new DefaultListModel<SurfaceStatus>());
      surfaceList.setCellRenderer(new SurfaceListCellRenderer());
      surfaceList.addListSelectionListener(this);
      updateSurfaceList();
      surfaceScrollPane = new JScrollPane(surfaceList);
      surfaceScrollPane.setPreferredSize(new Dimension(120, 300));
      objectsPanel.add(surfaceScrollPane);

      //add everything
      add(tabPanel, BorderLayout.WEST);
      add(objectsPanel, BorderLayout.EAST);

      slicerFrame.setContentPane(this);
      slicerFrame.addWindowListener(this);
      historyFile.repositionWindow(winName, slicerFrame, 200, 300, true);

      //Display the window.
      slicerFrame.pack();
      slicerFrame.setVisible(true);

      //save the window properties
      saveHistory();

    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == angleUnitsList) {
      slicer.setAngleUnits(angleUnitsList.getSelectedIndex());
      updateAngleSliders();
    }
    if (e.getSource() == viewCenterButton || e.getSource() == absoluteButton) {
      if (absoluteButton.isSelected() && !slicer.getUseMolecular()) {
        slicer.setUseMolecular(true);
        slicer.setSurfaceToolParam();
        updatePositionSlider();
      }
      if (viewCenterButton.isSelected() && slicer.getUseMolecular()) {
        slicer.setUseMolecular(false);
        slicer.setSurfaceToolParam();
        updatePositionSlider();
      }
    }
    if (e.getSource() == ghostCheck) {
      boolean isOn = ghostCheck.isSelected();
      slicer.setGhostOn(isOn);
      if (isOn) {
        slicer.setCapOn(false);
        capCheck.setSelected(false);
      }
      sliceSelected();
    }
    if (e.getSource() == boundaryPlaneCheck) {
      slicer.showSliceBoundaryPlanes(boundaryPlaneCheck.isSelected());
    }
    if (e.getSource() == capCheck) {
      boolean isOn = capCheck.isSelected();
      slicer.setCapOn(isOn);
      if (isOn) {
        slicer.setGhostOn(false);
        ghostCheck.setSelected(false);
      }
      sliceSelected();
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    JSlider source = (JSlider) e.getSource();
    if (source == angleXYSlider || source == angleZSlider) {
      float tempAngleZ = (float) (Math.PI * angleZSlider.getValue() / 180);
      float tempAngleXY = (float) (Math.PI * angleXYSlider.getValue() / 180);
      slicer.setSliceAnglefromZ(tempAngleZ);
      slicer.setSliceAngleXY(tempAngleXY);
      if (!source.getValueIsAdjusting())
        sliceSelected();
    }
    if (source == positionSlider || source == thicknessSlider) {
      float tempThickness = thicknessSlider.getValue()
          * slicer.getThicknessMax() / 180;
      float tempPos = positionSlider.getValue() * slicer.getThicknessMax()
          / 180 + slicer.getPositionMin();
      slicer.setSliceThickness(tempThickness);
      slicer.setSlicePosition(tempPos);
      if (!source.getValueIsAdjusting())
        sliceSelected();
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting())
      return; // wait until done
    JList<?> whichList = (JList<?>) e.getSource();
    if (whichList.isSelectionEmpty())
      return;// nothing selected
    int[] selected = whichList.getSelectedIndices();
    if (selected != null) {
      int lastIndex = selected[(selected.length - 1)];
      List<SurfaceStatus> surfaces = slicer.getSurfaces();
      SurfaceStatus lastSurface = surfaces.get(lastIndex);
      if (lastSurface.beenSliced) {
        slicer.setSlice(lastSurface.slice.angleXY,
            lastSurface.slice.anglefromZ, lastSurface.slice.position,
            lastSurface.slice.thickness);
        slicer.setCapOn(lastSurface.capOn);
        capCheck.setSelected(lastSurface.capOn);
        slicer.setGhostOn(lastSurface.ghostOn);
        ghostCheck.setSelected(lastSurface.ghostOn);
        updateAngleSliders();
        updatePositionSlider();
        updateThicknessSlider();
      }
      sliceSelected();
    }
  }

  private void sliceSelected() {
    if (surfaceList == null)
      return;
    int[] whichSelected = surfaceList.getSelectedIndices();
    if (whichSelected == null || whichSelected.length == 0)
      return;
    for (int i = 0; i < whichSelected.length; i++) {
      List<SurfaceStatus> surfaces = slicer.getSurfaces();
      int k = whichSelected[i];
      slicer.sliceObject(surfaces.get(k).id, surfaces.get(k).kind);
      surfaces.get(k).beenSliced = true;
      surfaces.get(k).capOn = slicer.getCapOn();
      surfaces.get(k).ghostOn = slicer.getGhostOn();
      surfaces.get(k).slice.setSlice(slicer.getSliceAngleXY(),
          slicer.getAnglefromZ(), slicer.getSlicePosition(),
          slicer.getSliceThickness(), slicer.getCenter(), slicer.getBoxVec(),
          slicer.getUseMolecular());
    }
  }

  private void updatePositionSlider() {
    Hashtable<Integer, JLabel> positionLabels = new Hashtable<Integer, JLabel>();
    String temp = "";
    for (int i = 0; i < 7; i++) {
      float tempVal = (float) (slicer.getPositionMin() + i * 0.16666666666
          * slicer.getThicknessMax());
      if (Math.abs(tempVal) < 0.001)
        tempVal = 0;
      temp = "" + tempVal;
      if (temp.length() > 5) {
        if (tempVal < 0) {
          temp = temp.substring(0, 5);
        } else {
          temp = temp.substring(0, 4);
        }
      }
      positionLabels.put(Integer.valueOf(i * 30), new JLabel(temp));
    }
    positionSlider.setLabelTable(positionLabels);
    positionSlider.setPaintLabels(true);
    int tempPos = (int) (180 * (slicer.getSlicePosition() - slicer
        .getPositionMin()) / slicer.getThicknessMax());
    positionSlider.setValue(tempPos);
  }

  private void updateThicknessSlider() {
    Hashtable<Integer, JLabel> thicknessLabels = new Hashtable<Integer, JLabel>();
    String temp = "";
    for (int i = 0; i < 7; i++) {
      float tempVal = (float) (i * 0.16666666666 * slicer.getThicknessMax());
      temp = "" + tempVal;
      if (temp.length() > 5) {
        temp = temp.substring(0, 4);
      }
      thicknessLabels.put(Integer.valueOf(i * 30), new JLabel(temp));
    }
    thicknessSlider.setLabelTable(thicknessLabels);
    thicknessSlider.setPaintLabels(true);
    int tempPos = (int) (180 * slicer.getSliceThickness() / slicer
        .getThicknessMax());
    thicknessSlider.setValue(tempPos);
  }

  private void updateAngleSliders() {
    Hashtable<Integer, JLabel> angleLabels = new Hashtable<Integer, JLabel>();
    angleLabels.put(Integer.valueOf(0), new JLabel("0"));
    switch (slicer.getAngleUnits()) {
    case SurfaceTool.DEGREES:
      angleLabels.put(Integer.valueOf(30), new JLabel("30"));
      angleLabels.put(Integer.valueOf(60), new JLabel("60"));
      angleLabels.put(Integer.valueOf(90), new JLabel("90"));
      angleLabels.put(Integer.valueOf(120), new JLabel("120"));
      angleLabels.put(Integer.valueOf(150), new JLabel("150"));
      angleLabels.put(Integer.valueOf(180), new JLabel("180"));
      break;
    case SurfaceTool.RADIANS:
      angleLabels.put(Integer.valueOf(30), new JLabel("0.52"));
      angleLabels.put(Integer.valueOf(60), new JLabel("1.05"));
      angleLabels.put(Integer.valueOf(90), new JLabel("1.75"));
      angleLabels.put(Integer.valueOf(120), new JLabel("2.09"));
      angleLabels.put(Integer.valueOf(150), new JLabel("2.62"));
      angleLabels.put(Integer.valueOf(180), new JLabel("3.14"));
      break;
    case SurfaceTool.GRADIANS:
      angleLabels.put(Integer.valueOf(30), new JLabel("33.3"));
      angleLabels.put(Integer.valueOf(60), new JLabel("66.7"));
      angleLabels.put(Integer.valueOf(90), new JLabel("100"));
      angleLabels.put(Integer.valueOf(120), new JLabel("133"));
      angleLabels.put(Integer.valueOf(150), new JLabel("167"));
      angleLabels.put(Integer.valueOf(180), new JLabel("200"));
      break;
    case SurfaceTool.CIRCLE_FRACTION:
      angleLabels.put(Integer.valueOf(30), new JLabel("1/12"));
      angleLabels.put(Integer.valueOf(60), new JLabel("1/6"));
      angleLabels.put(Integer.valueOf(90), new JLabel("1/4"));
      angleLabels.put(Integer.valueOf(120), new JLabel("1/3"));
      angleLabels.put(Integer.valueOf(150), new JLabel("5/12"));
      angleLabels.put(Integer.valueOf(180), new JLabel("1/2"));
      break;
    case SurfaceTool.UNITS_PI:
      String piStr = "\u03C0";
      angleLabels.put(Integer.valueOf(30), new JLabel(piStr + "/6"));
      angleLabels.put(Integer.valueOf(60), new JLabel(piStr + "/3"));
      angleLabels.put(Integer.valueOf(90), new JLabel(piStr + "/2"));
      angleLabels.put(Integer.valueOf(120), new JLabel("2" + piStr + "/3"));
      angleLabels.put(Integer.valueOf(150), new JLabel("5" + piStr + "/6"));
      angleLabels.put(Integer.valueOf(180), new JLabel(piStr));
      break;
    }
    angleXYSlider.setLabelTable(angleLabels);
    angleXYSlider.setPaintLabels(true);
    angleZSlider.setLabelTable(angleLabels);
    angleZSlider.setPaintLabels(true);
    int tempAngle = (int) (180 * slicer.getSliceAngleXY() / Math.PI);
    angleXYSlider.setValue(tempAngle);
    tempAngle = (int) (180 * slicer.getAnglefromZ() / Math.PI);
    angleZSlider.setValue(tempAngle);
  }

  void updateSurfaceList() {
    //TODO - will update the surface list checking color, etc...or should
    //this just check that the list is complete and other things will be updated
    //as they change?  For starters, we'll just reset it and make it match the
    //contents of the slicer.surfaces list.  May only need ID and color...
    DefaultListModel<SurfaceStatus> listModel = (DefaultListModel<SurfaceStatus>) surfaceList.getModel();
    listModel.removeAllElements();
    int size = slicer.getSurfaces().size();
    for (int i = 0; i < size; i++) {
      listModel.addElement(slicer.getSurfaces().get(i));
    }
  }

  void saveHistory() {
    if (historyFile == null)
      return;
    historyFile.addWindowInfo(histWinName, slicerFrame, null);
    //TODO
    //    prop.setProperty("webMakerInfoWidth", "" + webPanels[0].getInfoWidth());
    //    prop.setProperty("webMakerInfoHeight", "" + webPanels[0].getInfoHeight());
    //    prop.setProperty("webMakerAppletPath", remoteAppletPath);
    //    prop.setProperty("webMakerLocalAppletPath", localAppletPath);
    //    prop.setProperty("webMakerPageAuthorName", pageAuthorName);
    //    historyFile.addProperties(prop);
  }

  /**
   * @param layout
   */
  SurfaceToolGUI(LayoutManager layout) {
    super(layout);
    // TODO
  }

  /**
   * @param isDoubleBuffered
   */
  SurfaceToolGUI(boolean isDoubleBuffered) {
    super(isDoubleBuffered);
    // TODO
  }

  /**
   * @param layout
   * @param isDoubleBuffered
   */
  SurfaceToolGUI(LayoutManager layout, boolean isDoubleBuffered) {
    super(layout, isDoubleBuffered);
    // TODO
  }

  /**
   * @return (JFrame) The frame for the slicerGUI
   */
  JFrame getFrame() {
    return slicerFrame;
  }

  /**
   * Brings the surfaceTool to the front and updates sliders, etc...
   */
  void toFront() {
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
    updateSurfaceList();
    slicerFrame.setVisible(true);
    slicerFrame.toFront();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowFocusListener#windowGainedFocus(java.awt.event.WindowEvent)
   */
  @Override
  public void windowGainedFocus(WindowEvent e) {
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
    updateSurfaceList();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowFocusListener#windowLostFocus(java.awt.event.WindowEvent)
   */
  @Override
  public void windowLostFocus(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
   */
  @Override
  public void windowOpened(WindowEvent e) {
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
    updateSurfaceList();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
   */
  @Override
  public void windowClosing(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
   */
  @Override
  public void windowClosed(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
   */
  @Override
  public void windowIconified(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
   */
  @Override
  public void windowDeiconified(WindowEvent e) {
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
    updateSurfaceList();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
   */
  @Override
  public void windowActivated(WindowEvent e) {
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
    updateSurfaceList();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
   */
  @Override
  public void windowDeactivated(WindowEvent e) {
    // TODO

  }

  class SurfaceListCellRenderer extends JLabel implements ListCellRenderer<Object> {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      setText(" " + ((SurfaceStatus) value).id);
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(new Color(((SurfaceStatus) value).color));
      } else {
        setBackground(list.getBackground());
        setForeground(new Color(((SurfaceStatus) value).color));
      }
      setEnabled(list.isEnabled());
      setFont(list.getFont());
      setOpaque(true);
      return this;
    }
  }
}
