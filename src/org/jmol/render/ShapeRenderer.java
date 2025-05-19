/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2017-04-04 08:46:58 -0500 (Tue, 04 Apr 2017) $
 * $Revision: 21478 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.render;

import org.jmol.api.JmolRendererInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.shape.Shape;
import org.jmol.util.GData;
import org.jmol.viewer.JC;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public abstract class ShapeRenderer {

  public Viewer vwr;
  protected TransformManager tm;
  /**
   * could be vwr.gdata or an exporter
   */
  public JmolRendererInterface g3d;
  protected ModelSet ms;
  protected Shape shape;

  protected int myVisibilityFlag;
  protected int shapeID;
  
  //working values
  public short colix;
  public short mad;
  public int exportType;
  public boolean isExport;
  protected int flags;

  protected void initRenderer() {}

  abstract protected boolean render();

  public final void setViewerG3dShapeID(Viewer vwr, int shapeID) {
    this.vwr = vwr;
    tm = vwr.tm;
    this.shapeID = shapeID;
    myVisibilityFlag = JC.getShapeVisibilityFlag(shapeID);
    initRenderer();
  }

  public boolean renderShape(JmolRendererInterface g3d, ModelSet modelSet, Shape shape, int flags) {
    setup(g3d, modelSet, shape);
    this.flags = flags;
    boolean needsTranslucent = render();
    exportType = GData.EXPORT_NOT;
    isExport = false;
    return needsTranslucent;
  }

  public void setup(JmolRendererInterface g3d, ModelSet modelSet, Shape shape) {
    this.g3d = g3d;
    this.ms = modelSet;
    this.shape = shape;
    exportType = g3d.getExportType();
    isExport = (exportType != GData.EXPORT_NOT);
    
  }

  protected boolean isVisibleForMe(Atom a) {
    return a != null && a.isVisible(myVisibilityFlag | Atom.ATOM_INFRAME_NOTHIDDEN);
  }

 
}

