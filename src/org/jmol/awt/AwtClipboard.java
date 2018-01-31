/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.awt;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javajs.util.PT;



/**
 * This class is used to transfer text or an image into the clipboard and to get tet from the clipboard.
 * Simplified by Bob Hanson
 * 
 * @author Nicolas Vervelle
 */
public class AwtClipboard implements Transferable {

  /**
   * The image to transfer into the clipboard.
   */
  private Image image;
  private String text;
  
  /**
   * Transfers text or image into the clipboard.
   * 
   * @param textOrImage to transfer into the clipboard.
   */
  public static void setClipboard(Object textOrImage) {
    AwtClipboard sel = new AwtClipboard(textOrImage);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
  }
  
  private AwtClipboard(Object image) {
    if (image instanceof String)
      this.text = (String) image;
    else
      this.image = (Image) image;
  }

  /* (non-Javadoc)
   * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
   */
  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return (text == null ? 
        new DataFlavor[]{ DataFlavor.imageFlavor }
      : new DataFlavor[]{ DataFlavor.stringFlavor });
  }

  /* (non-Javadoc)
   * @see java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
   */
  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return DataFlavor.imageFlavor.equals(flavor);
  }

  /* (non-Javadoc)
   * @see java.awt.datatransfer.Transferable#getTransferData(java.awt.datatransfer.DataFlavor)
   */
  @Override
  public Object getTransferData(DataFlavor flavor)
      throws UnsupportedFlavorException, IOException {
    if (DataFlavor.imageFlavor.equals(flavor)) {
      return image;
    } else     if (DataFlavor.stringFlavor.equals(flavor)) {
      return text;
    }
    throw new UnsupportedFlavorException(flavor);
  }

  /**
   * Get the String residing on the clipboard. Or, if it is a file list, get the
   * load command associated with that. from
   * http://www.javapractices.com/Topic82.cjp
   * 
   * @return any text found on the Clipboard; if none found, return an empty
   *         String.
   */
  @SuppressWarnings("unchecked")
  public static String getClipboardText() {
    String result = null;
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable contents = clipboard.getContents(null);
      if (contents == null)
        return null;
      if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        result = (String) contents.getTransferData(DataFlavor.stringFlavor);
      } else if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        Object o = contents.getTransferData(DataFlavor.javaFileListFlavor);
        List<File> fileList = (List<File>) o;
        final int length = fileList.size();
        if (length == 0)
          return null;
        if (length == 1) {
          result = "LoAd "
              + PT.esc(fileList.get(0).getAbsolutePath().replace('\\', '/'));
          if (result.endsWith(".pse\""))
            result += " filter 'DORESIZE'";
        } else {
          result = "LoAd files ";
          for (int i = 0; i < length; i++)
            result += " "
                + PT.esc(fileList.get(i).getAbsolutePath()
                    .replace('\\', '/'));
        }
      }
    } catch (Exception ex) {
      result = ex.toString();
    }
    return result;
  }
}
