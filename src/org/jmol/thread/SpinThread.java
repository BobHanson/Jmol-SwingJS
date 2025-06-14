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

package org.jmol.thread;


import javajs.util.BS;
import org.jmol.script.T;
import javajs.util.Lst;
import javajs.util.M3d;
import javajs.util.M4d;

import org.jmol.util.Logger;
import javajs.util.P3d;
import org.jmol.viewer.JC;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class SpinThread extends JmolThread {
  /**
   * 
   */
  private TransformManager transformManager;
  private double endDegrees;
  private int nFrames = -1;
  private Lst<P3d> endPositions;
  private P3d[][] centerAndPoints;
  private double[] dihedralList;
  private double nDegrees;
  private BS bsAtoms;
  private boolean isNav;
  private boolean isGesture;
  private double myFps;
  private double angle;
  private boolean haveNotified;
  private int index;
  //private boolean navigatingSurface;
  private BS[] bsBranches;
  private boolean isDone = false;
  private M4d m4;
  private P3d ptemp;
  private M3d vectorMatrix;
  private M3d vm0;
  
  public SpinThread() {}
  
  @SuppressWarnings("unchecked")
  @Override
  public int setManager(Object manager, Viewer vwr, Object params) {
    transformManager = (TransformManager) manager;
    setViewer(vwr, "SpinThread");
    Object[] options = (Object[]) params;
    if (options == null) {
      isNav = true;
    } else {
      endDegrees = ((Number) options[0]).doubleValue();
      endPositions = (Lst<P3d>) options[1];
      dihedralList = (double[]) options[2];
      vectorMatrix = (M3d) options[3];
      bsAtoms = (BS) options[4];
      isGesture = (options[5] != null);
      centerAndPoints = (P3d[][]) options[6];
      if (dihedralList != null)
        bsBranches = vwr.ms.getBsBranches(dihedralList);
      if (centerAndPoints != null) {
        ptemp = new P3d();
        nFrames = ((Integer) options[7]).intValue();
      }
      if (vectorMatrix != null) {
        vm0 = M3d.newM3(vectorMatrix);
        //isScreenZ = (Double.isNaN(vm0.m00) && vm0.m22 == 1);
        angle = endDegrees;
        endDegrees = Double.MAX_VALUE;
        bsAtoms = null;
      }
    }
    return 0;
  }

  /**
   * Java:
   * 
   * run1(INIT) while(!interrupted()) { run1(MAIN) } run1(FINISH)
   * 
   * JavaScript:
   * 
   * run1(INIT) run1(MAIN) --> setTimeout to run1(CHECK) or run1(FINISH) and
   * return run1(CHECK) --> setTimeout to run1(CHECK) or run1(MAIN) or
   * run1(FINISH) and return
   * 
   */

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        myFps = (isNav ? transformManager.navFps : transformManager.spinFps);
        vwr.g.setB(isNav ? "_navigating" : "_spinning", true);
        haveReference = true;
        vwr.startHoverWatcher(false);
        mode = MAIN;
        break;
      case MAIN:
        if (isReset || checkInterrupted(transformManager.spinThread)) {
          mode = FINISH;
          break;
        }
        if (isNav && myFps != transformManager.navFps) {
          myFps = transformManager.navFps;
          index = 0;
          startTime = System.currentTimeMillis();
        } else if (!isNav && myFps != transformManager.spinFps
            && bsAtoms == null) {
          myFps = transformManager.spinFps;
          index = 0;
          startTime = System.currentTimeMillis();
        }
        if (myFps == 0
            || !(isNav ? transformManager.navOn : transformManager.spinOn)) {
          mode = FINISH;
          break;
        }
        //navigatingSurface = vwr.getNavigateSurface();
        boolean refreshNeeded = (endDegrees >= 1e10d ? true : isNav ? //navigatingSurface ||
        transformManager.navX != 0 || transformManager.navY != 0
            || transformManager.navZ != 0
            : transformManager.isSpinInternal
                && transformManager.internalRotationAxis.angle != 0
                || transformManager.isSpinFixed
                && transformManager.fixedRotationAxis.angle != 0
                || !transformManager.isSpinFixed
                && !transformManager.isSpinInternal
                && (transformManager.spinX != 0 || transformManager.spinY != 0 || transformManager.spinZ != 0));
        targetTime = (long) (++index * 1000 / myFps);
        currentTime = System.currentTimeMillis() - startTime;
        sleepTime = (int) (targetTime - currentTime);
        //System.out.println(index + " spin thread " + targetTime + " " + currentTime + " " + sleepTime);
        if (sleepTime < 0) {
          if (!haveNotified)
            Logger.info("spinFPS is set too fast (" + myFps
                + ") -- can't keep up!");
          haveNotified = true;
          startTime -= sleepTime;
          sleepTime = 0;
        }
        boolean isInMotion = (bsAtoms == null && vwr.getInMotion(false));
        if (isInMotion) {
          if (isGesture) {
            mode = FINISH;
            break;
          }
          sleepTime += 1000;
        }
        if (refreshNeeded && !isInMotion
            && (transformManager.spinOn || transformManager.navOn))
          doTransform();
        mode = CHECK1;
        break;
      case CHECK1: // cycling
        while (!checkInterrupted(transformManager.spinThread) && !vwr.getRefreshing())
          if (!runSleep(10, CHECK1))
            return;
        if (bsAtoms != null || vwr.g.waitForMoveTo && endDegrees != Double.MAX_VALUE)
            vwr.requestRepaintAndWait("spin thread");
        else
          vwr.refresh(Viewer.REFRESH_REPAINT, "SpinThread");
        if (nFrames >= 0 && index >= nFrames 
            || (endDegrees >= 1e10d ? nDegrees/endDegrees > 0.99 
            : !isNav && endDegrees >= 0 ? nDegrees >= endDegrees - 0.001
            : -nDegrees <= endDegrees + 0.001)) {
          isDone = true;
          transformManager.setSpinOff();
        }
        if (!runSleep(sleepTime, MAIN))
          return;
        mode = MAIN;
        break;
      case FINISH:
        if (vectorMatrix != null) {
        } else if (dihedralList != null) {
          vwr.setDihedrals(dihedralList, bsBranches, 0F);
        } else if (bsAtoms != null && endPositions != null) {
          // when the standard deviations of the end points was
          // exact, we know that we want EXACTLY those final positions
          vwr.setAtomCoords(bsAtoms, T.xyz, endPositions);
          bsAtoms = null;
          endPositions = null;
        }
        if (!isReset) {
          transformManager.setSpinOff();
          vwr.startHoverWatcher(true);
        }
        stopped = !isDone;
        resumeEval();
        stopped = true;
        return;
      }
  }

  
  private void doTransform() {
    if (vectorMatrix != null) {
      if (Double.isNaN(vectorMatrix.getElement(0,0))) {
        vectorMatrix.setElement(1, 1, nDegrees);
      } else {
        vectorMatrix.mul(vm0);
      }
      vwr.rotateModelSpinVectors(-1, vectorMatrix);
      nDegrees = (nDegrees + angle) % 360;// / myFps;      
    } else if (centerAndPoints != null) {
      double f = index * 1d / nFrames;
      vwr.ms.morphAtoms(bsAtoms, centerAndPoints, 0, f, ptemp);
    } else if (dihedralList != null) {
      double f = 1d / myFps / endDegrees;
      vwr.setDihedrals(dihedralList, bsBranches, f);
      nDegrees += 1d / myFps;
    } else if (isNav) {
      transformManager.setNavigationOffsetRelative();//navigatingSurface);
    } else if (transformManager.isSpinInternal
        || transformManager.isSpinFixed) {
      angle = (transformManager.isSpinInternal ? transformManager.internalRotationAxis
          : transformManager.fixedRotationAxis).angle
          / myFps;
      if (transformManager.isSpinInternal) {
        transformManager.rotateAxisAngleRadiansInternal(angle, bsAtoms, m4, false);
      } else {
        transformManager.rotateAxisAngleRadiansFixed(angle, bsAtoms);
      }
      nDegrees += Math.abs(angle * TransformManager.degreesPerRadian);
      //System.out.println(i + " " + angle + " " + nDegrees);
    } else { // old way: Rx * Ry * Rz
      if (transformManager.spinX != 0) {
        transformManager.rotateXRadians(transformManager.spinX
            * JC.radiansPerDegree / myFps, null);
      }
      if (transformManager.spinY != 0) {
        transformManager.rotateYRadians(transformManager.spinY
            * JC.radiansPerDegree / myFps, null);
      }
      if (transformManager.spinZ != 0) {
        transformManager.rotateZRadians(transformManager.spinZ
            * JC.radiansPerDegree / myFps);
      }
    }
  }

}