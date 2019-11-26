/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class MyFileFilter extends FileFilter {
  private String[] extensions;
  private String label;

  public MyFileFilter(String[] extensions, String label) {
    this.extensions = extensions;
    this.label = label;
  }

  public MyFileFilter(String extension, String label) {
    String[] s = new String[1];
    s[0] = extension;
    this.extensions = s;
    this.label = label;
  }

  //Accept all directories and all jnc files.
  public boolean accept(File f) {
    if (f.isDirectory()) {
      return true;
    }
    return checkExtension(f);
  }

  //The description of this filter
  public String getDescription() {
    String ext = new String();
    for (int j = 0; j < extensions.length; j++) {
      ext = ext + "." + extensions[j] + " ";
    }

    return label + " ( " + ext + ")";
  }

  public boolean checkExtension(File f) {
    String ext = null;
    String s = f.getName();
    int i = s.lastIndexOf('.');

    if (i > 0 && i < s.length() - 1) {
      ext = s.substring(i + 1).toLowerCase();
    }
    if (ext != null) {
      for (int j = 0; j < extensions.length; j++) {
        if (ext.equals(extensions[j])) {
          return true;
        }
      }
    }

    return false;
  }
}
