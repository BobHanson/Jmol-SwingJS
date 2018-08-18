/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-09-25 15:33:17 -0500 (Wed, 25 Sep 2013) $
 * $Revision: 18695 $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.awt;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JOptionPane;

import org.jmol.api.JmolDropEditor;
import org.jmol.api.JmolStatusListener;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

/**
 * A simple Dropping class to allow files to be dragged onto a target. It
 * supports drag-and-drop of files from file browsers, and CML text from
 * editors, e.g. jEdit.
 * 
 * <p>
 * Note that multiple drops ARE thread safe.
 * 
 * @author Billy <simon.tyrrell@virgin.net>
 */
public class FileDropper implements DropTargetListener {
  private String fd_oldFileName;
  private PropertyChangeSupport fd_propSupport;

  private Viewer vwr;
  private PropertyChangeListener pcl;
  private JmolStatusListener statusListener;
  private JmolDropEditor dropListener;

  public FileDropper(JmolStatusListener statusListener, Viewer vwr, JmolDropEditor dropListener) {
    this.statusListener = statusListener; // application only
    this.dropListener = dropListener;
    fd_oldFileName = "";
    fd_propSupport = new PropertyChangeSupport(this);
    this.vwr = vwr;
    addPropertyChangeListener((pcl = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        doDrop(evt);
      }
    }));
    Component display = (Component) vwr.display;
    display.setDropTarget(new DropTarget(display, this));
    display.setEnabled(true);
    //System.out.println("File dropper enabled for " + display);
  }

  public void dispose() {
    removePropertyChangeListener(pcl);
    vwr = null;
    //System.out.println("File dropper disposed.");
  }

  private void loadFile(String fname, int x, int y) {
    if (dropListener != null) {
      dropListener.loadFile(fname);
      return;
    }      
    if (fname.endsWith(".URL")) {
//      [InternetShortcut]
//      URL=http://nbo6.chem.wisc.edu/jmol_nborxiv/allyl.47
//      IDList=
//      HotKey=0
//      IconFile=C:\Users\RM\AppData\Local\Mozilla\Firefox\Profiles\r4gp03t7.default\shortcutCache\x76TB2sbngvxLh95XTl2MA==.ico
//      IconIndex=0
      String data = vwr.getAsciiFileOrNull(fname);
      if (data == null || data.indexOf("URL=") < 0)
        return;
      fname = data.substring(data.indexOf("URL=") + 4);
      fname = fname.substring(0,  fname.indexOf("\n"));
    }
    fname = fname.replace('\\', '/').trim();
    if (fname.indexOf("://") < 0)
      fname = (fname.startsWith("/") ? "file://" : "file:///") + fname;
    if (!vwr.setStatusDragDropped(0, x, y, fname))
      return;
    
    int flags = 1; //
    boolean isScript = FileManager.isScriptType(fname);
    boolean isSurface = FileManager.isSurfaceType(fname);
    switch (vwr.ms.ac > 0 && !isScript && !isSurface ? JOptionPane.showConfirmDialog(null, GT.$("Would you like to replace the current model with the selected model?")) : JOptionPane.OK_OPTION) {
    case JOptionPane.CANCEL_OPTION:
      return;
    case JOptionPane.OK_OPTION:
      break;
    default:
      flags += 4; // append
      break;
    }
    if (statusListener != null) {
      try {
        String data = vwr.fm.getEmbeddedFileState(fname, false, "state.spt");
        if (data.indexOf("preferredWidthHeight") >= 0)
          vwr.sm.resizeInnerPanelString(data);
      } catch (Throwable e) {
        // ignore
      }
    }
    vwr.openFileAsyncSpecial(fname, flags);
  }

  private void loadFiles(List<File> fileList) {
    SB sb = new SB();
    for (int i = 0; i < fileList.size(); ++i) {
      File f = fileList.get(i);
      String fname = f.getAbsolutePath();
      fname = fname.replace('\\', '/').trim();
      fname = (fname.startsWith("/") ? "file://" : "file:///") + fname;
      sb.append("load ").append(i == 0 ? "" : "APPEND ").append(
          PT.esc(fname)).append(";\n");
    }
    sb.append("frame *;reset;");
    vwr.script(sb.toString());
  }

  protected void doDrop(PropertyChangeEvent evt) {
    // new event, because we open the file directly. Not sure this has been tested
    if ("inline".equals(evt.getPropertyName())) {
      vwr.openStringInline((String) evt.getNewValue());
    }
  }

  public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
    fd_propSupport.addPropertyChangeListener(l);
  }

  public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
    fd_propSupport.removePropertyChangeListener(l);
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
    if (Logger.debugging)
      Logger.debug("DropOver detected...");
  }

  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
    if (Logger.debugging)
      Logger.debug("DropEnter detected...");
    dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
  }

  @Override
  public void dragExit(DropTargetEvent dtde) {
    if (Logger.debugging)
      Logger.debug("DropExit detected...");
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
    System.out.println("dropactionchanged");
  }

  @Override
  @SuppressWarnings("unchecked")
  public void drop(DropTargetDropEvent dtde) {
    if (Logger.debugging)
      Logger.debug("Drop detected...");
    Transferable t = dtde.getTransferable();
    boolean isAccepted = false;
    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      while (true) {
        Object o = null;
        try {
          dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          o = t.getTransferData(DataFlavor.javaFileListFlavor);
          isAccepted = true;
        } catch (Exception e) {
          Logger.error("transfer failed");
        }
        // if o is still null we had an exception
        if (o instanceof List) {
          List<File> fileList = (List<File>) o;
          final int length = fileList.size();
          if (length == 1) {
            String fileName = fileList.get(0).getAbsolutePath().trim();
            if (fileName.endsWith(".bmp"))
              break; // try another flavor -- Mozilla bug
            dtde.getDropTargetContext().dropComplete(true);
            Point loc = dtde.getLocation();
            loadFile(fileName, loc.x, loc.y);
            return;
          }
          dtde.getDropTargetContext().dropComplete(true);
          loadFiles(fileList);
          return;
        }
        break;
      }
    }

    if (Logger.debugging)
      Logger.debug("browsing supported flavours to find something useful...");
    DataFlavor[] df = t.getTransferDataFlavors();

    if (df == null || df.length == 0)
      return;
    for (int i = 0; i < df.length; ++i) {
      DataFlavor flavor = df[i];
      Object o = null;
      if (true) {
        Logger.info("df " + i + " flavor " + flavor);
        Logger.info("  class: " + flavor.getRepresentationClass().getName());
        Logger.info("  mime : " + flavor.getMimeType());
      }

      if (flavor.getMimeType().startsWith("text/uri-list")
          && flavor.getRepresentationClass().getName().equals(
              "java.lang.String")) {

        /*
         * This is one of the (many) flavors that KDE provides: df 2 flavour
         * java.awt.datatransfer.DataFlavor[mimetype=text/uri-list;
         * representationclass=java.lang.String] java.lang.String String: file
         * :/home/egonw/data/Projects/SourceForge/Jmol/Jmol-HEAD/samples/
         * cml/methanol2.cml
         * 
         * A later KDE version gave me the following. Note the mime!! hence the
         * startsWith above
         * 
         * df 3 flavor java.awt.datatransfer.DataFlavor[mimetype=text/uri-list
         * ;representationclass=java.lang.String] class: java.lang.String mime :
         * text/uri-list; class=java.lang.String; charset=Unicode
         */

        try {
          if (!isAccepted)
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          isAccepted = true;
          o = t.getTransferData(flavor);
        } catch (Exception e) {
          Logger.errorEx(null, e);
        }

        if (o instanceof String) {
          if (Logger.debugging) {
            Logger.debug("  String: " + o.toString());
          }
          loadFile(o.toString(), 0, 0);
          dtde.getDropTargetContext().dropComplete(true);
          return;
        }
      } else if (flavor.getMimeType().equals(
          "application/x-java-serialized-object; class=java.lang.String")) {

        /*
         * This is one of the flavors that jEdit provides:
         * 
         * df 0 flavor java.awt.datatransfer.DataFlavor[mimetype=application/
         * x-java-serialized-object;representationclass=java.lang.String] class:
         * java.lang.String mime : application/x-java-serialized-object;
         * class=java.lang.String String: <molecule title="benzene.mol"
         * xmlns="http://www.xml-cml.org/schema/cml2/core"
         * 
         * But KDE also provides:
         * 
         * df 24 flavor java.awt.datatransfer.DataFlavor[mimetype=application
         * /x-java-serialized-object;representationclass=java.lang.String]
         * class: java.lang.String mime : application/x-java-serialized-object;
         * class=java.lang.String String: file:/home/egonw/Desktop/1PN8.pdb
         */

        try {
          if (!isAccepted)
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          isAccepted = true;
          o = t.getTransferData(df[i]);
        } catch (Exception e) {
          Logger.errorEx(null, e);
        }

        if (o instanceof String) {
          String content = (String) o;
          if (Logger.debugging) {
            Logger.debug("  String: " + content);
          }
          if (content.startsWith("file:/")) {
            loadFile(content, 0, 0);
          } else {
            PropertyChangeEvent pce = new PropertyChangeEvent(this,
                "inline", fd_oldFileName, content);
            fd_propSupport.firePropertyChange(pce);
          }
          dtde.getDropTargetContext().dropComplete(true);
          return;
        }
      }
    }
    if (!isAccepted)
      dtde.rejectDrop();
  }

}
