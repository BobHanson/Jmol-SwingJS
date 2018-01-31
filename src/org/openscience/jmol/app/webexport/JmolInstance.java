/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
package org.openscience.jmol.app.webexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;


import javax.swing.filechooser.FileSystemView;

import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.java.BS;

class JmolInstance {
  String name;
  String javaname;
  String script;
  int width;
  int height;
  int bgColor;
  boolean spinOn;
  String pictFile;
  BS whichWidgets;//true bits are selected widgets
  boolean pictIsScratchFile;
  JmolViewer vwr;

  public static JmolInstance getInstance(JmolViewer vwr, String name,
                                         int width, int height, int widgets) {
    JmolInstance ji = new JmolInstance(vwr, name, width, height, widgets);
    return (ji.script == null ? null : ji);
  }

  private JmolInstance(JmolViewer vwr, String name,
      int width, int height, int nWidgets) {
    this.vwr = vwr;
    this.name = name;
    this.width = width;
    this.height = height;
    script = vwr.getStateInfo();
    spinOn = vwr.getBooleanProperty("_spinning");
    if (script == null) {
      LogPanel.log("Error trying to get Jmol State when saving view/instance.");
      return;
    }
    bgColor = vwr.getBackgroundArgb();
    javaname = name.replaceAll("[^a-zA-Z_0-9-]", "_"); //escape filename characters
    whichWidgets=BS.newN(nWidgets);
    FileSystemView Directories = FileSystemView.getFileSystemView();
    File homedir = Directories.getHomeDirectory();
    String homedirpath = homedir.getPath();
    String scratchpath = homedirpath + "/.jmol_WPM";
    File scratchfile = new File(scratchpath);
    if (!(scratchfile.exists())) {//make the directory if necessary. we will delete when done
      boolean made_scratchdir = scratchfile.mkdir();
      if (!(made_scratchdir)) {
        LogPanel.log(GT._("Attempt to make scratch directory failed."));
      }
    }
    pictFile = scratchpath + "/" + javaname + ".png";
    // note -- the current Jmol State for THIS computer is saved in the PNG
    Map<String, Object> params = new Hashtable<String, Object>();
    params.put("fileName", pictFile);
    params.put("type", "PNG");
    params.put("quality", Integer.valueOf(2));
    params.put("width", Integer.valueOf(width));
    params.put("height", Integer.valueOf(height));
    vwr.outputToFile(params);
    pictIsScratchFile = true;
  }

  boolean movepict(String dirpath) throws IOException {
    String imagename = dirpath + "/" + this.javaname + ".png";
    if (pictFile.equals(imagename))
      return false;
    FileInputStream is = null;
    try {
      is = new FileInputStream(pictFile);
    } catch (IOException ise) {
      throw ise;
    }
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(imagename);
      int pngbyteint = is.read();
      while (pngbyteint != -1) {
        os.write(pngbyteint);
        pngbyteint = is.read();
      }
      os.flush();
      os.close();
      is.close();
    } catch (IOException exc) {
      throw exc;
    }
/* 
 * But if the file is deleted, then the next time this is
 * called, we could end up with a 0-length file.
 * Particularly when we save to a second directory with
 * a different name
 *  
 *     
    if (this.pictIsScratchFile) { //only delete the file if not using file already saved for user.
      File scratchtoerase = new File(scratchname);
      boolean deleteOK = scratchtoerase.delete();
      if (!(deleteOK)) {
        IOException IOe = (new IOException("Failed to delete scratch file "
            + scratchname + "."));
        throw IOe;
      }
    }
    this.pictFile = imagename;
    this.pictIsScratchFile = false;
*/
    return true;
  }
  boolean delete() throws IOException {
    File scratchToErase = new File(pictFile);
    if (scratchToErase.exists() && !scratchToErase.delete())
        throw new IOException("Failed to delete scratch file " + pictFile + ".");
    //delete any other scratch files we create with an instance.
    return true;
  }

  boolean addWidget(int widgetID) {
    if (widgetID > whichWidgets.size())
      return false;// minimalist error checking
    if (widgetID < 0)
      return false;
    whichWidgets.set(widgetID);
    return true;
  }

  boolean deleteWidget(int widgetID) {
    if (widgetID > whichWidgets.size())
      return false;// minimalist error checking
    if (widgetID < 0)
      return false;
    whichWidgets.clear(widgetID);
    return true;
  }

}
