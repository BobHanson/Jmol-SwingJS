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
package jspecview.java;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetDragEvent;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.File;
import java.util.List;

import javajs.util.PT;

import org.jmol.api.PlatformViewer;
import org.jmol.util.Logger;

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
public class FileDropperJmol implements DropTargetListener {
  private String fd_oldFileName;
  private PropertyChangeSupport fd_propSupport;

  static public final String FD_PROPERTY_INLINE = "inline";

  PropertyChangeListener pcl;

  /**
	 * @param viewer  
	 */
  public FileDropperJmol(PlatformViewer viewer) {
    fd_oldFileName = "";
    fd_propSupport = new PropertyChangeSupport(this);
    addPropertyChangeListener((pcl = new PropertyChangeListener() {
      @Override
			public void propertyChange(PropertyChangeEvent evt) {
        doDrop(evt);
      }
    }));
    //Component display = (Component) viewer.getDisplay();
    //display.setDropTarget(new DropTarget(display, this));
    //display.setEnabled(true);
  }

  public void dispose() {
    removePropertyChangeListener(pcl);
  }

  private void loadFile(String fname) {
    fname = fname.replace('\\', '/').trim();
    if (fname.indexOf("://") < 0)
      fname = (fname.startsWith("/") ? "file://" : "file:///") + fname;
//    viewer.openFileAsyncPDB(fname, true);
  }

  private void loadFiles(List<File> fileList) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fileList.size(); ++i) {
      File f = fileList.get(i);
      String fname = f.getAbsolutePath();
      fname = fname.replace('\\', '/').trim();
      fname = (fname.startsWith("/") ? "file://" : "file:///") + fname;
      sb.append("load ").append(i == 0 ? "" : "APPEND ").append(
          PT.esc(fname)).append(";\n");
    }
    sb.append("frame *;reset;");
    //viewer.script(sb.toString());
  }

  protected void doDrop(PropertyChangeEvent evt) {
    // new event, because we open the file directly. Not sure this has been tested
    if (evt.getPropertyName() == FD_PROPERTY_INLINE) {
      //viewer.openStringInline((String) evt.getNewValue());
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
            loadFile(fileName);
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
          loadFile(o.toString());
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
            loadFile(content);
          } else {
            PropertyChangeEvent pce = new PropertyChangeEvent(this,
                FD_PROPERTY_INLINE, fd_oldFileName, content);
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
