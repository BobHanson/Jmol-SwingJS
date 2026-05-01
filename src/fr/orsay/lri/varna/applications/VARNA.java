/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.applications;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNARNAListener;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * Formerly VARNAGUI
 * 
 * Adapted by Bob Hanson for Jmol-SwingJS.
 * 
 */
public class VARNA
    implements DropTargetListener, InterfaceVARNAListener, MouseListener {

  protected final static int VARNA_GUI_ALLOW_CREATE = 0x01;
  protected final static int VARNA_GUI_ALLOW_DELETE_DUPLICATE = 0x02;
  protected final static int VARNA_GUI_ALLOW_DOUBLE_CLICK = 0x04;
  protected final static int VARNA_GUI_EDITABLE = 0x08;
  protected final static int VARNA_GUI_SHOW_LISTING = 0x10;
  protected final static int VARNA_GUI_SHOW_TITLE = 0x20;
  protected final static int VARNA_GUI_SHOW_ZOOM_PANEL = 0x40;

  protected final static int VARNA_GUI_FULL_OPTIONS = //
      VARNA_GUI_ALLOW_CREATE | //
          VARNA_GUI_ALLOW_DELETE_DUPLICATE | //
          VARNA_GUI_ALLOW_DOUBLE_CLICK | //
          VARNA_GUI_EDITABLE | //
          VARNA_GUI_SHOW_LISTING | //
          VARNA_GUI_SHOW_ZOOM_PANEL; //

  private int guiOptions;

  private boolean hasOption(int op) {
    return ((guiOptions & op) != 0);
  }

  protected VARNAapp app;

  private JFrame parentFrame;

  protected JFrame frame;

  private static int _nextID = 1;
  @SuppressWarnings("unused")

  private int _algoCode;

  private JScrollBar _vertZoomSlider = new JScrollBar(Adjustable.VERTICAL);
  private JScrollBar _horizZoomSlider = new JScrollBar(Adjustable.HORIZONTAL);

  private JButton _createButton = new JButton("Create");

  private String structureListTitle = "         Structures         ";

  private String frameTitle;

  public VARNA() {
    this("VARNA", VARNA_GUI_FULL_OPTIONS);
    setFrame(null, null, 0, 0);
  }

  protected VARNA(String title, int options) {
    guiOptions = options;
    frameTitle = (title == null ? "VARNA" : "VARNA GUI");
    app = new VARNAapp(hasOption(VARNA_GUI_EDITABLE));
  }

  public VARNAPanel getVarnaPanel() {
    return (app == null ? null : app.getVARNAPanel());
  }

  public String getStruct() {
    return app.getStruct().getText();
  }

  public String getSeq() {
    return app.getSeq().getText();
  }

  public String getInfo() {
    return app.getInfo().getText();
  }

  public JFrame setFrame(JFrame parentFrame, JFrame frame, int width,
                         int height) {
    this.parentFrame = parentFrame;
    this.frame = frame;
    setFrame(width, height);
    if (this.frame != null)
      setupVarnaGUI();
    return frame;
  }

  /**
   * 
   * @param width
   *        use 0 for default, -1 for frameless
   * @param height
   */
  private void setFrame(int width, int height) {
    if (frame == null) {
      if (width == 0) {
        width = VARNAPanel.DEFAULT_WIDTH;
        height = VARNAPanel.DEFAULT_HEIGHT;
      }
      if (width > 0) {
        frame = new JFrame(frameTitle);
        frame.setSize(new Dimension(width, height));
        if (parentFrame != null) {
          frame.setLocationRelativeTo(parentFrame);
          Point loc = frame.getLocation();
          loc.x += frame.getWidth() / 2;
          loc.y += frame.getHeight() / 2;
          frame.setLocation(loc);
        }
        app.setFrame(frame);
      }
    }
  }

  private void setupVarnaGUI() {
    app.setSideList(this);
    setupDemo();
    VARNAPanel vp = app.getVARNAPanel();
    vp.setShowTitle(hasOption(VARNA_GUI_SHOW_TITLE));
    if (frame != null) {
      frame.setBackground(app.getBackgroundColor());
    }
    if (hasOption(VARNA_GUI_SHOW_ZOOM_PANEL))
      app.addZoomWindow();

    JPanel goPanel = null, opsPanel = null;
    if (hasOption(VARNA_GUI_ALLOW_CREATE)) {
      _createButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          app.doCreate(null, null, null, null);
        }
      });
      goPanel = new JPanel();
      goPanel.setLayout(new BorderLayout());
      goPanel.add(_createButton, BorderLayout.CENTER);
    }
    opsPanel = new JPanel(new BorderLayout());
    app.setPanels(goPanel, opsPanel, structureListTitle,
        hasOption(VARNA_GUI_ALLOW_DELETE_DUPLICATE));

    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(app._tools, BorderLayout.NORTH);
    if (hasOption(VARNA_GUI_SHOW_LISTING)) {
      JSplitPane split = getMySplitPanel();
      frame.getContentPane().add(split, BorderLayout.CENTER);
    } else {
      frame.getContentPane().add(vp, BorderLayout.CENTER);
    }
    frame.setVisible(true);

    new DropTarget(vp, this);

    vp.addRNAListener(new InterfaceVARNARNAListener() {

      @Override
      public void onSequenceModified(int index, String oldseq, String newseq) {
      }

      @Override
      public void onStructureModified(Set<ModeleBP> current,
                                      Set<ModeleBP> addedBasePairs,
                                      Set<ModeleBP> removedBasePairs) {
      }

      @Override
      public void onRNALayoutChanged(Hashtable<Integer, Double> previousPositions) {
        app.setHovering(false);
      }

    });

    app.addSelectionListener();

    app.getVARNAPanel().addVARNAListener(this);

    app.startZoomThread();
  }

  /**
   * this could be adapted
   */
  protected void setupDemo() {
    app.setupNoDemo();
  }

  private JSplitPane getMySplitPanel() {
    JPanel vpScroll = new JPanel();
    vpScroll.setLayout(new BorderLayout());
    _horizZoomSlider.setVisible(false);
    _horizZoomSlider.addAdjustmentListener((e -> {
      doAdjustZoomTranslation(e.getValue(), true);
    }));
    _vertZoomSlider.setVisible(false);
    _vertZoomSlider.addAdjustmentListener((e -> {
      doAdjustZoomTranslation(e.getValue(), false);
    }));
    vpScroll.add(_horizZoomSlider, BorderLayout.SOUTH);
    vpScroll.add(_vertZoomSlider, BorderLayout.EAST);
    vpScroll.add(app.getVARNAPanel(), BorderLayout.CENTER);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
        app._listPanel, vpScroll);
    return split;
  }

  public void addRNA(RNA r, VARNAConfig cfg) {
    app.addRNA(r, cfg);
  }

  public static String generateDefaultName() {
    return "User file #" + _nextID++;
  }

  public RNA getRNA() {
    return app.getRNA();
  }

  public String[][] getParameterInfo() {
    return app.getParameterInfo();
  }

  public void init() {
    app.init();
  }

  @Override
  public void dragEnter(DropTargetDragEvent arg0) {

  }

  @Override
  public void dragExit(DropTargetEvent arg0) {

  }

  @Override
  public void dragOver(DropTargetDragEvent arg0) {

  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
    try {
      Transferable tr = dtde.getTransferable();
      DataFlavor[] flavors = tr.getTransferDataFlavors();
      for (int i = 0; i < flavors.length; i++) {
        if (flavors[i].isFlavorJavaFileListType()) {
          dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          Object ob = tr.getTransferData(flavors[i]);
          if (ob instanceof List) {
            List<?> list = (List<?>) ob;
            for (int j = 0; j < list.size(); j++) {
              Object o = list.get(j);

              if (dtde.getSource() instanceof DropTarget) {
                DropTarget dt = (DropTarget) dtde.getSource();
                Component c = dt.getComponent();
                if (c instanceof VARNAPanel) {
                  try {
                    FullBackup bck = VARNAPanel.importSession(o); // BH SwingJS
                    app.addRNA(bck.rna, bck.config, bck.name, true);
                  } catch (ExceptionLoadingFailed e3) {
                    ArrayList<RNA> rnas = RNAFactory.loadSecStr((File) o); // BH SwingJS
                    if (rnas.isEmpty()) {
                      throw new ExceptionFileFormatOrSyntax(
                          "No RNA could be parsed from that source.");
                    }

                    dtde.dropComplete(true);
                    app.getVARNAPanel().getVARNAUI().UIChooseRNAs(rnas);
                    return;
                    /*
                    for(RNA r: rnas)
                    {
                      r.drawRNA(vp.getConfig());
                      String name = r.getName();
                      if (name.equals(""))
                      { 
                    	  name = path.substring(path.lastIndexOf(File.separatorChar)+1);
                      }
                      if (rnas.size()>1)
                      {
                    	  name += " - Molecule# "+id++;
                      }
                      _rnaList.add(vp.getConfig().clone(),r,name,true);
                    }*/
                  }
                }
              }
            }
          }
          // If we made it this far, everything worked.
          dtde.dropComplete(true);
          return;
        }
      }
      // Hmm, the user must not have dropped a file list
      dtde.rejectDrop();
    } catch (Exception e) {
      e.printStackTrace();
      dtde.rejectDrop();
    }

  }

  @Override
  public void dropActionChanged(DropTargetDragEvent arg0) {
  }

  @Override
  public void onStructureRedrawn() {

  }

  @Override
  public void onUINewStructure(VARNAConfig v, RNA r) {
    app.addRNA(r, v, r.getName(), true);
    onZoomLevelChanged();
  }

  @Override
  public void onWarningEmitted(String s) {

  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (hasOption(VARNA_GUI_ALLOW_DOUBLE_CLICK) && e.getClickCount() == 2
        && app.doMouseClicked(frame, e.getPoint())) {
      app.repaint();
    }
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {

  }

  @Override
  public void mouseExited(MouseEvent arg0) {

  }

  @Override
  public void mousePressed(MouseEvent arg0) {
  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
  }

  @Override
  public void onZoomLevelChanged() {
    VARNAPanel vp = app.getVARNAPanel();
    if (vp.getZoom() > 1.02) {
      Rectangle r = vp.getZoomedInTranslationBox();
      _horizZoomSlider.setMinimum(r.x);
      _horizZoomSlider.setMaximum(r.x + r.width + vp.getWidth());
      _horizZoomSlider.getModel().setExtent(vp.getWidth());
      _horizZoomSlider.getModel().setValue(vp.getTranslation().x);
      _horizZoomSlider.doLayout();
      _horizZoomSlider.setVisible(true);

      _vertZoomSlider.setMinimum(r.y);
      _vertZoomSlider.setMaximum(r.y + r.height + vp.getHeight());
      _vertZoomSlider.getModel().setExtent(vp.getHeight());
      _vertZoomSlider.getModel().setValue(vp.getTranslation().y);
      _vertZoomSlider.doLayout();
      _vertZoomSlider.setVisible(true);
    } else {
      _horizZoomSlider.setVisible(false);
      _vertZoomSlider.setVisible(false);
    }
  }

  @Override
  public void onZoomTranslationChanged() {
    VARNAPanel vp = app.getVARNAPanel();
    if (vp.getZoom() > 1.02) {
      int nx = _horizZoomSlider.getMaximum()
          - (vp.getTranslation().x - _horizZoomSlider.getMinimum())
          - vp.getWidth();
      int ny = _vertZoomSlider.getMaximum()
          - (vp.getTranslation().y - _vertZoomSlider.getMinimum())
          - vp.getHeight();
      _horizZoomSlider.getModel().setValue(nx);
      _horizZoomSlider.doLayout();
      _vertZoomSlider.getModel().setValue(ny);
      _vertZoomSlider.doLayout();
    }
  }

  public void doAdjustZoomTranslation(int value, boolean isHorizontal) {
    VARNAPanel vp = app.getVARNAPanel();
    if (isHorizontal) {
      vp.setTranslation(new Point(
          _horizZoomSlider.getMaximum()
              - (value - _horizZoomSlider.getMinimum()) - vp.getWidth(),
          vp.getTranslation().y));
    } else {
      vp.setTranslation(
          new Point(vp.getTranslation().x, _vertZoomSlider.getMaximum()
              - (value - _vertZoomSlider.getMinimum()) - vp.getHeight()));
    }
    vp.repaint();
  }

  public void pack() {
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    List<Image> icons = new ArrayList<Image>();
    //JOptionPane.showMessageDialog(null, ""+Toolkit.getDefaultToolkit().getImage("./VARNA16x16.png"), "Check", JOptionPane.INFORMATION_MESSAGE);
    icons.add(Toolkit.getDefaultToolkit().getImage("./VARNA16x16.png"));
    icons.add(Toolkit.getDefaultToolkit().getImage("./VARNA32x32.png"));
    icons.add(Toolkit.getDefaultToolkit().getImage("./VARNA64x64.png"));
    VARNA d = new VARNA();
    d.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    d.pack();
  }

  public VARNAapp getApp() {
    return app;
  }
}
