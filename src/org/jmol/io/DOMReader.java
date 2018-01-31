/* $RCSfile$
 * $Author$
 * $Date$
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

package org.jmol.io;

import java.util.Map;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public class DOMReader {
  private FileManager fm;
  private Viewer vwr;
  private Object[] aDOMNode = new Object[1];
  private Object atomSetCollection;
  private Map<String, Object> htParams;

  public DOMReader() {}
  
  void set(FileManager fileManager, Viewer vwr, Object DOMNode, Map<String, Object> htParams) {
    fm = fileManager;
    this.vwr = vwr;
    aDOMNode[0] = DOMNode;
    this.htParams = htParams;
  }

  void run() {
    Object info = null;
    
    /**
     * ignored 
     * 
     * @j2sNative
     * 
     * 
     */
    {
      info= vwr.apiPlatform.getJsObjectInfo(aDOMNode, null, null);
    }
    // note that this will not work in JSmol because we don't implement the nameSpaceInfo stuff there
    // and we cannot pass [HTMLUnknownObject]
    if (info != null)
      htParams.put("nameSpaceInfo", info);
    // no append option here
    vwr.zap(false, true, false);
    atomSetCollection = vwr.getModelAdapter().getAtomSetCollectionFromDOM(
        aDOMNode, htParams);
    if (atomSetCollection instanceof String)
      return;
    fm.setFileInfo(new String[] { "JSNode" });
  }
}