package jspecview.java;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JOptionPane;

import jspecview.api.JSVFileDropper;
import jspecview.common.JSViewer;

import org.jmol.util.Logger;


public class AwtFileDropper implements JSVFileDropper, DropTargetListener {

  private JSViewer vwr;

	@Override
	public AwtFileDropper set(JSViewer viewer) {
    this.vwr = viewer;
    return this;
  }

  //
  //   Abstract methods that are used to perform drag and drop operations
  //

  @Override
	public void dragEnter(DropTargetDragEvent dtde) {
    // Called when the user is dragging and enters this drop target.
    // accept all drags
    dtde.acceptDrag(dtde.getSourceActions());
  }

  @Override
	public void dragOver(DropTargetDragEvent dtde) {
  }

  @Override
	public void dragExit(DropTargetEvent dtde) {
  }

  @Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
    // Called when the user changes the drag action between copy or move
  }

  static int lastSelection = 0;
  
	// Called when the user finishes or cancels the drag operation.
	@Override
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent dtde) {
		Logger.debug("Drop detected...");
		Transferable t = dtde.getTransferable();
		boolean isAccepted = false;
		boolean doAppend = false;
		if (vwr.currentSource != null) {
			Object[] options = { "Replace", "Append", "Cancel" };
			int ret = JOptionPane.showOptionDialog(null, "Select an option",
					"JSpecView File Drop", JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[lastSelection]);

			if (ret < 0 || ret == 2)
				return;
			lastSelection = ret;
			doAppend = (ret == 1);
		}
		String prefix = (doAppend ? "" : "close ALL;");
		String postfix = (doAppend ? "view all;overlayStacked false" : "overlay ALL");
		String cmd = "LOAD APPEND ";
		String fileToLoad = null;
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
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
				List<File> list = (List<File>) o;
				dtde.getDropTargetContext().dropComplete(true);
				dtde = null;
				SB sb = new SB();
				sb.append(prefix);
				for (int i = 0; i < list.size(); i++)
					sb.append(cmd + PT.esc(list.get(i).getAbsolutePath()) + ";");
				sb.append(postfix);
				cmd = sb.toString();
				Logger.info("Drop command = " + cmd);
				vwr.runScript(cmd);
				return;
			}
		}

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
					dtde.getDropTargetContext().dropComplete(true);
					if (Logger.debugging)
						Logger.debug("  String: " + o.toString());
					fileToLoad = o.toString();
					break;
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
					dtde.getDropTargetContext().dropComplete(true);
					if (Logger.debugging)
						Logger.debug("  String: " + content);
					if (content.startsWith("file:/")) {
						fileToLoad = content;
						break;
					}
				}
			}
		}
		if (!isAccepted)
			dtde.rejectDrop();
		if (fileToLoad != null) {
			cmd = prefix + cmd + PT.esc(fileToLoad) + "\";" + postfix;
			Logger.info("Drop command = " + cmd);
			vwr.runScriptNow(cmd);
		}
	}
  
}
