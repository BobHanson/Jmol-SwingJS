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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNARNAListener;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.RNA;

public class VARNAEditor extends JFrame
    implements DropTargetListener, InterfaceVARNAListener, MouseListener {

  /**
   * 
   */
  private static final long serialVersionUID = -790155708306987257L;

  protected VARNAapp app;

  @SuppressWarnings("unused")
  private int _algoCode;

  public VARNAEditor() {
    super("VARNA Editor");
    app = new VARNAapp(false);
    RNAPanelDemoInit();
  }

  private void RNAPanelDemoInit() {
    app.setSideList(this);
    app.setupTwoRNADemo();
    setBackground(app._backgroundColor);

    app.setPanels(null, null, "Structures");
    VARNAPanel vp = app.getVARNAPanel();
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
        app._listPanel, vp);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(split, BorderLayout.CENTER);
    getContentPane().add(app._tools, BorderLayout.NORTH);

    setVisible(true);

    DropTarget dt = new DropTarget(vp, this);

    vp.addRNAListener(new InterfaceVARNARNAListener() {
      @Override
      public void onSequenceModified(int index, String oldseq, String newseq) {
        app.setSequenceText(vp.getRNA().getSeq());
      }

      @Override
      public void onStructureModified(Set<ModeleBP> current,
                                      Set<ModeleBP> addedBasePairs,
                                      Set<ModeleBP> removedBasePairs) {
        app.setStructureText(vp.getRNA().getStructDBN(true));
      }

      @Override
      public void onRNALayoutChanged(Hashtable<Integer, Double> previousPositions) {
      }

    });

    app.addSelectionListener();

  }

  public RNA getRNA() {
    return app.getRNA();
  }

  public String[][] getParameterInfo() {
    return app.getParameterInfo();
  }

  public static void main(String[] args) {
    VARNAEditor d = new VARNAEditor();
    d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    d.pack();
    d.setVisible(true);
  }

  @Override
  public void dragEnter(DropTargetDragEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void dragExit(DropTargetEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void dragOver(DropTargetDragEvent arg0) {
    // TODO Auto-generated method stub

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
                  String path = o.toString();
                  VARNAPanel vp = (VARNAPanel) c;
                  try {
                    FullBackup bck = VARNAPanel.importSession(o); // BH SwingJS
                    app.addRNA(bck.rna, bck.config, bck.name, true);
                  } catch (ExceptionLoadingFailed e3) {
                    Collection<RNA> rnas = RNAFactory.loadSecStr((File) o); // BH SwingJS 
                    if (rnas.isEmpty()) {
                      throw new ExceptionFileFormatOrSyntax(
                          "No RNA could be parsed from that source.");
                    }

                    int id = 1;
                    for (RNA r : rnas) {
                      r.drawRNA(vp.getConfig());
                      String name = r.getName();
                      if (name.equals("")) {
                        name = path.substring(
                            path.lastIndexOf(File.separatorChar) + 1);
                      }
                      if (rnas.size() > 1) {
                        name += " - Molecule# " + id++;
                      }
                      app.addRNA(r, vp.getConfig().clone(), name, true);
                    }
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
    // TODO Auto-generated method stub

  }

  @Override
  public void onUINewStructure(VARNAConfig v, RNA r) {
    app.addRNA(r, v, "", true);
  }

  @Override
  public void onWarningEmitted(String s) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 2 && app.doMouseClicked(this, e.getPoint())) {
    	  repaint();
      }
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mousePressed(MouseEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onZoomLevelChanged() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onTranslationChanged() {
    // TODO Auto-generated method stub

  }
}
