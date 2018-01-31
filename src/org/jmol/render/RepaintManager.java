/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-04-16 16:54:06 -0500 (Sat, 16 Apr 2016) $
 * $Revision: 21051 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.render;

import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolRendererInterface;
import org.jmol.api.JmolRepaintManager;
import org.jmol.api.JmolToJSmolInterface;
import org.jmol.java.BS;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.Rectangle;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public class RepaintManager implements JmolRepaintManager {

  private Viewer vwr;
  private ShapeManager shapeManager;
  private ShapeRenderer[] renderers;

  public RepaintManager() {
    // required for reflection
  }
  
  private final BS bsTranslucent = BS.newN(JC.SHAPE_MAX);
  
  @Override
  public void set(Viewer vwr, ShapeManager shapeManager) {
    this.vwr = vwr;
    this.shapeManager = shapeManager;
  }

  /////////// thread management ///////////
  
  public int holdRepaint = 0;
  private boolean repaintPending;
  
  @Override
  public boolean isRepaintPending() {
    return repaintPending;
  }
  
  @Override
  public void pushHoldRepaint(String why) {
    ++holdRepaint;
  }
  
  @Override
  public void popHoldRepaint(boolean andRepaint, String why) {
    --holdRepaint;
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      if (andRepaint) {
        repaintPending = true;
        //System.out.println("pophold repaintPending "+ why);
        repaintNow(why);
      }
    }
  }

  @SuppressWarnings({ "null", "unused" })
  @Override
  synchronized public void requestRepaintAndWait(String why) {
    JmolToJSmolInterface jmol = null;
    /**
     * @j2sNative
     * 
     *  jmol = (self.Jmol && Jmol._repaint ? Jmol : null); 
     */
    {}
    if (jmol != null) {
      jmol._repaint(vwr.html5Applet,  false);
      repaintDone();
    }

    /**
     * @j2sNative
     *     
     */
    {
      //System.out.println("RM requestRepaintAndWait() " + (test++));
      try {
        repaintNow(why);
        //System.out.println("repaintManager requestRepaintAndWait I am waiting for a repaint: thread=" + Thread.currentThread().getName());
        wait(vwr.g.repaintWaitMs); // more than a second probably means we are locked up here
        if (repaintPending) {
          Logger.error("repaintManager requestRepaintAndWait timeout");
          repaintDone();
        }
      } catch (InterruptedException e) {
        //System.out.println("repaintManager requestRepaintAndWait interrupted thread=" + Thread.currentThread().getName());
      }
    }
    //System.out.println("repaintManager requestRepaintAndWait I am no longer waiting for a repaint: thread=" + Thread.currentThread().getName());
  }

  @Override
  public boolean repaintIfReady(String why) {
    //System.out.println("ifready repaintPending " + why);
    if (repaintPending)
      return false;
    repaintPending = true;
    //System.out.println("ifready repaintPending set TRUE");
    if (holdRepaint == 0)
      repaintNow(why);
    return true;
  }

  /**
   * @param why  
   */
  private void repaintNow(String why) {
    // from RepaintManager to the System
    // -- "Send me an asynchronous update() event!"
    if (!vwr.haveDisplay)
      return;    
      //System.out.println("RepaintMan repaintNow " + why);
      vwr.apiPlatform.repaint(vwr.display);
  }

  @Override
  synchronized public void repaintDone() {
    repaintPending = false;
    //System.out.println("repaintPending false");

    /**
     * @j2sNative
     * 
     * 
     */
    {
      //System.out.println("repaintManager repaintDone thread=" + Thread.currentThread().getName());
      // ignored in JavaScript
      notify(); // to cancel any wait in requestRepaintAndWait()
    }
  }

  
  /////////// renderer management ///////////
  
  
  @Override
  public void clear(int iShape) {
    if (renderers ==  null)
      return;
    if (iShape >= 0)
      renderers[iShape] = null;
    else
      for (int i = 0; i < JC.SHAPE_MAX; ++i)
        renderers[i] = null;
  }

  private ShapeRenderer getRenderer(int shapeID) {
    if (renderers[shapeID] != null)
      return renderers[shapeID];
    String className = JC.getShapeClassName(shapeID, true) + "Renderer";
    ShapeRenderer renderer;
    if ((renderer = (ShapeRenderer) Interface.getInterface(className, vwr, "render")) == null)
      return null;
    renderer.setViewerG3dShapeID(vwr, shapeID);
    return renderers[shapeID] = renderer;
  }

  /////////// actual rendering ///////////

  @Override
  public void render(GData gdata, ModelSet modelSet, boolean isFirstPass,
                     int[] navMinMax) {
    JmolRendererInterface g3d = (JmolRendererInterface) gdata;
    if (renderers == null)
      renderers = new ShapeRenderer[JC.SHAPE_MAX];
    getAllRenderers();
    try {
      boolean logTime = vwr.getBoolean(T.showtiming);
      g3d.renderBackground(null);
      if (isFirstPass) {
        bsTranslucent.clearAll();
        if (navMinMax != null)
          g3d.renderCrossHairs(navMinMax, vwr.getScreenWidth(),
              vwr.getScreenHeight(), vwr.tm.getNavigationOffset(),
              vwr.tm.navigationDepthPercent);
        Rectangle band = vwr.getRubberBandSelection();
        if (band != null && g3d.setC(vwr.cm.colixRubberband))
          g3d.drawRect(band.x, band.y, 0, 0, band.width, band.height);
        vwr.noFrankEcho = true;
      }
      String msg = null;
      for (int i = 0; i < JC.SHAPE_MAX && gdata.currentlyRendering; ++i) {
        Shape shape = shapeManager.getShape(i);
        if (shape == null)
          continue;

        if (logTime) {
          msg = "rendering " + JC.getShapeClassName(i, false);
          Logger.startTimer(msg);
        }
        if ((isFirstPass || bsTranslucent.get(i))
            && getRenderer(i).renderShape(g3d, modelSet, shape))
          bsTranslucent.set(i);
        if (logTime)
          Logger.checkTimer(msg, false);
      }
      g3d.renderAllStrings(null);
    } catch (Exception e) {
      if (!vwr.isJS)
        e.printStackTrace();
      if (vwr.async && "Interface".equals(e.getMessage()))
        throw new NullPointerException();
      Logger.error("rendering error? " + e);
    }
  }
  
  private void getAllRenderers() {
    boolean isOK = true;
    for (int i = 0; i < JC.SHAPE_MAX; ++i) {
      if (shapeManager.getShape(i) == null || getRenderer(i) != null)
        continue;
      isOK = repaintPending = !vwr.async;
    }
    if (!isOK)
      throw new NullPointerException();
  }

  @Override
  public String renderExport(GData gdata, ModelSet modelSet,
                             Map<String, Object> params) {
    boolean isOK;
    shapeManager.finalizeAtoms(null, true);
    JmolRendererInterface exporter3D = vwr.initializeExporter(params);
    isOK = (exporter3D != null);
    if (!isOK) {
      Logger.error("Cannot export " + params.get("type"));
      return null;
    }
    if (renderers == null)
      renderers = new ShapeRenderer[JC.SHAPE_MAX];
    getAllRenderers();
    String msg = null;
    try {
      boolean logTime = vwr.getBoolean(T.showtiming);
      exporter3D.renderBackground(exporter3D);
      for (int i = 0; i < JC.SHAPE_MAX; ++i) {
        Shape shape = shapeManager.getShape(i);
        if (shape == null)
          continue;
        if (logTime) {
          msg = "rendering " + JC.getShapeClassName(i, false);
          Logger.startTimer(msg);
        }
        getRenderer(i).renderShape(exporter3D, modelSet, shape);
        if (logTime)
          Logger.checkTimer(msg, false);
      }
      exporter3D.renderAllStrings(exporter3D);
      msg = exporter3D.finalizeOutput();
    } catch (Exception e) {
      if (!vwr.isJS)
        e.printStackTrace();
      Logger.error("rendering error? " + e);
    }
    return msg;
  }

}
