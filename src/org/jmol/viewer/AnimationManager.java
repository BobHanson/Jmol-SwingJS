/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 10:56:39 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11127 $
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
package org.jmol.viewer;


import java.util.Map;


import org.jmol.script.T;
import org.jmol.thread.JmolThread;
import org.jmol.util.BSUtil;
//import javajs.util.List;

import org.jmol.api.Interface;
import javajs.util.BS;
import org.jmol.modelset.ModelSet;

public class AnimationManager {

  public JmolThread animationThread;
  public Viewer vwr;
  
  AnimationManager(Viewer vwr) {
    this.vwr = vwr;
  }

  // used by AnimationThread, Viewer, or StateCreator:
  
  public boolean animationOn;
  public int animationFps;  // set in stateManager
  public int firstFrameDelayMs;
  public int lastFrameDelayMs;

  public void setAnimationOn(boolean animationOn) {
    if (animationOn == this.animationOn)
      return;
    
    if (!animationOn || vwr.headless) {
      stopThread(false);
      return;
    }
    if (!vwr.tm.spinOn)
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "Anim:setAnimationOn");
    setAnimationRange(-1, -1);
    resumeAnimation();
  }

  public void stopThread(boolean isPaused) {
    boolean stopped = false;
    if (animationThread != null) {
      animationThread.interrupt();
      animationThread = null;
      stopped = true;
    }
    animationPaused = isPaused;
    if (stopped && !vwr.tm.spinOn)
      vwr.refresh(Viewer.REFRESH_SYNC_MASK, "Viewer:setAnimationOff");
    animation(false);
    //stopModulationThread();
    vwr.setStatusFrameChanged(false, false);
    
  }

  public boolean setAnimationNext() {
    return setAnimationRelative(animationDirection);
  }

  public boolean currentIsLast() {
    return (isMovie ? lastFramePainted == caf
        : lastModelPainted == cmi);
  }

  public boolean currentFrameIs(int f) {
    int i = cmi;
    return (morphCount == 0 ? i == f : Math.abs(currentMorphModel - f) < 0.001f);
  }

  // required by Viewer or stateCreator
  
  // used by StateCreator or Viewer:
  
  final static int FRAME_FIRST = -1;
  final static int FRAME_LAST = 1;
  final static int MODEL_CURRENT = 0;

  final BS bsVisibleModels = new BS();

  public int animationReplayMode = T.once;

  BS bsDisplay;

  int[] animationFrames;

  public boolean isMovie;
  boolean animationPaused;
  
  /**
   * current model index
   * 
   */
  public int cmi;
  
  /**
   * current animation frame
   * 
   */
  int caf;
  int morphCount;
  int animationDirection = 1;
  int currentDirection = 1;
  int firstFrameIndex;
  int lastFrameIndex;
  int frameStep;
  int backgroundModelIndex = -1;
  
  float currentMorphModel;
  float firstFrameDelay;
  float lastFrameDelay = 1;
  
  void clear() {
    setMovie(null);
    initializePointers(0);
    setAnimationOn(false);
    setModel(0, true);
    currentDirection = 1;
    cai = -1;
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(T.once, 0, 0);
    initializePointers(0);
  }
  
  String getModelSpecial(int i) {
    switch (i) {
    case FRAME_FIRST:
      if (animationFrames != null)
        return "1";
      i = firstFrameIndex;
      break;
    case MODEL_CURRENT:
      if (morphCount > 0)
        return "-" + (1 + currentMorphModel);
      i = cmi;
      break;
    case FRAME_LAST:
      if (animationFrames != null)
        return "" + animationFrames.length;
      i = lastFrameIndex;
      break;
    }
    return vwr.getModelNumberDotted(i);
  }

  void setDisplay(BS bs) {
    bsDisplay = (bs == null || bs.isEmpty() ? null : BSUtil.copy(bs));
  }

  public void setMorphCount(int n) {
    morphCount = (isMovie ? 0 : n); // for now -- no morphing in movies
  }

  public void morph(float modelIndex) {
    int m = (int) modelIndex;
    if (Math.abs(m - modelIndex) < 0.001f)
      modelIndex = m;
    else if (Math.abs(m - modelIndex) > 0.999f)
      modelIndex = m = m + 1;
    float f = modelIndex - m;
    m -= 1;
    if (f == 0) {
      currentMorphModel = m;
      setModel(m, true);
      return;
    }
    int m1;
    setModel(m, true);
    m1 = m + 1;
    currentMorphModel = m + f;
    if (m1 == m || m1 < 0 || m < 0)
      return;
    vwr.ms.morphTrajectories(m, m1, f);
  }  

  void setModel(int modelIndex, boolean clearBackgroundModel) {
    if (modelIndex < 0)
      stopThread(false);
    int formerModelIndex = cmi;
    ModelSet modelSet = vwr.ms;
    int modelCount = (modelSet == null ? 0 : modelSet.mc);
    if (modelCount == 1)
      cmi = modelIndex = 0;
    else if (modelIndex < 0 || modelIndex >= modelCount)
      modelIndex = -1;
    String ids = null;
    boolean isSameSource = false;
    if (cmi != modelIndex) {
      if (modelCount > 0) {
        ModelSet ms = vwr.ms;
        boolean toDataModel = ms.isJmolDataFrameForModel(modelIndex);
        boolean fromDataModel = ms.isJmolDataFrameForModel(cmi);
        if (fromDataModel)
          ms.setJmolDataFrame(null, -1, cmi);
        if (cmi != -1)
          vwr.saveModelOrientation();
        if (fromDataModel || toDataModel) {
          ids = ms.getJmolFrameType(modelIndex) 
          + " "  + modelIndex + " <-- " 
          + " " + cmi + " " 
          + ms.getJmolFrameType(cmi);
          
          isSameSource = (ms.getJmolDataSourceFrame(modelIndex) == ms
              .getJmolDataSourceFrame(cmi));
        }
      }
      cmi = modelIndex;
      if (ids != null) {
        if (modelIndex >= 0)
          vwr.restoreModelOrientation(modelIndex);
        if (isSameSource && (ids.indexOf("quaternion") >= 0 
            || ids.indexOf("plot") < 0
            && ids.indexOf("ramachandran") < 0
            && ids.indexOf(" property ") < 0)) {
          vwr.restoreModelRotation(formerModelIndex);
        }
      }
    }
    setViewer(clearBackgroundModel);
  }

  void setBackgroundModelIndex(int modelIndex) {
    ModelSet modelSet = vwr.ms;
    if (modelSet == null || modelIndex < 0 || modelIndex >= modelSet.mc)
      modelIndex = -1;
    backgroundModelIndex = modelIndex;
    if (modelIndex >= 0)
      vwr.ms.setTrajectory(modelIndex);
    vwr.setTainted(true);
    setFrameRangeVisible(); 
  }
  
  void initializePointers(int frameStep) {
    firstFrameIndex = 0;
    lastFrameIndex = (frameStep == 0 ? 0 : getFrameCount()) - 1;
    this.frameStep = frameStep;
    vwr.setFrameVariables();
  }

  public void setAnimationDirection(int animationDirection) {
    this.animationDirection = animationDirection;
    //if (animationReplayMode != ANIMATION_LOOP)
      //currentDirection = 1;
  }

  void setAnimationFps(int fps) {
    if (fps < 1)
      fps = 1;
    if (fps > 50)
      fps = 50;
    animationFps = fps;
    vwr.setFrameVariables();
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  
  public void setAnimationReplayMode(int animationReplayMode,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
    firstFrameDelayMs = (int)(this.firstFrameDelay * 1000);
    this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
    lastFrameDelayMs = (int)(this.lastFrameDelay * 1000);
    this.animationReplayMode = animationReplayMode;
    vwr.setFrameVariables();
  }

  void setAnimationRange(int framePointer, int framePointer2) {
    int frameCount = getFrameCount();
    if (framePointer < 0) framePointer = 0;
    if (framePointer2 < 0) framePointer2 = frameCount;
    if (framePointer >= frameCount) framePointer = frameCount - 1;
    if (framePointer2 >= frameCount) framePointer2 = frameCount - 1;
    firstFrameIndex = framePointer;
    currentMorphModel = firstFrameIndex;
    lastFrameIndex = framePointer2;
    frameStep = (framePointer2 < framePointer ? -1 : 1);
    rewindAnimation();
  }

  void pauseAnimation() {
    stopThread(true);
  }
  
  void reverseAnimation() {
    currentDirection = -currentDirection;
    if (!animationOn)
      resumeAnimation();
  }
  
  void repaintDone() {
    lastModelPainted = cmi;
    lastFramePainted = caf;
  }
  
  void resumeAnimation() {
    if(cmi < 0)
      setAnimationRange(firstFrameIndex, lastFrameIndex);
    if (getFrameCount() <= 1) {
      animation(false);
      return;
    }
    animation(true);
    animationPaused = false;
    if (animationThread == null) {
      intAnimThread++;
      animationThread = (JmolThread) Interface.getOption("thread.AnimationThread", vwr, "script");
      animationThread.setManager(this, vwr, new int[] {firstFrameIndex, lastFrameIndex, intAnimThread} );
      animationThread.start();
    }
  }

  void setAnimationLast() {
    setFrame(animationDirection > 0 ? lastFrameIndex : firstFrameIndex);
  }

  void rewindAnimation() {
    setFrame(animationDirection > 0 ? firstFrameIndex : lastFrameIndex);
    currentDirection = 1;
    vwr.setFrameVariables();
  }
  
  boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
  }

  float getAnimRunTimeSeconds() {
    int frameCount = getFrameCount();
    if (firstFrameIndex == lastFrameIndex || lastFrameIndex < 0
        || firstFrameIndex < 0 || lastFrameIndex >= frameCount
        || firstFrameIndex >= frameCount)
      return 0;
    int i0 = Math.min(firstFrameIndex, lastFrameIndex);
    int i1 = Math.max(firstFrameIndex, lastFrameIndex);
    float nsec = 1f * (i1 - i0) / animationFps + firstFrameDelay
        + lastFrameDelay;
    for (int i = i0; i <= i1; i++)
      nsec += vwr.ms.getFrameDelayMs(modelIndexForFrame(i)) / 1000f;
    return nsec;
  }

  /**
   * support for PyMOL movies and 
   * anim FRAMES [....]
   * 
   * currently no support for scripted movies
   * 
   * @param info
   */
  public void setMovie(Map<String, Object> info) {
    isMovie = (info != null && info.get("scripts") == null);
    if (isMovie) {
      animationFrames = (int[]) info.get("frames");
      if (animationFrames == null || animationFrames.length == 0) {
        isMovie = false;
      } else {
        caf = ((Integer) info.get("currentFrame")).intValue();
        if (caf < 0 || caf >= animationFrames.length)
          caf = 0;
      }
      setFrame(caf);
    } 
    if (!isMovie) {
      //movie = null;
      animationFrames = null;
    }
    vwr.setBooleanProperty("_ismovie", isMovie);
    bsDisplay = null;
    currentMorphModel = morphCount = 0;
    vwr.setFrameVariables();
  }

  int modelIndexForFrame(int i) {
    return (isMovie ? animationFrames[i] - 1 : i);
  }

  public int getFrameCount() {
    return (isMovie ? animationFrames.length : vwr.ms.mc);
  }

  public void setFrame(int i) {
    try {
    if (isMovie) {
      int iModel = modelIndexForFrame(i);
      caf = i;
      i = iModel;
    } else {
      caf = i;
    }
    setModel(i, true);
    } catch (Exception e) {
      // ignore
    }
  }

  // private methods and fields
   
  private int lastFramePainted;
  private int lastModelPainted;
  private int intAnimThread;
  private int cai = -1;
  
  public int getUnitCellAtomIndex() {
    return cai;
  }
  
  public void setUnitCellAtomIndex(int iAtom) {
    cai = iAtom;
  }


  private void setViewer(boolean clearBackgroundModel) {
    vwr.ms.setTrajectory(cmi);
    vwr.tm.setFrameOffset(cmi);
    if (cmi == -1 && clearBackgroundModel)
      setBackgroundModelIndex(-1);
    vwr.setTainted(true);
    int nDisplay = setFrameRangeVisible();
    vwr.setStatusFrameChanged(false, false);
    if (!vwr.g.selectAllModels)
      setSelectAllSubset(nDisplay < 2);
  }

  void setSelectAllSubset(boolean justOne) {
    if (vwr.ms != null)
      vwr.slm.setSelectionSubset(justOne ? vwr.ms
          .getModelAtomBitSetIncludingDeleted(cmi, true) : vwr.ms
          .getModelAtomBitSetIncludingDeletedBs(bsVisibleModels));
  }

  private int setFrameRangeVisible() {
    int nDisplayed = 0;
    bsVisibleModels.clearAll();
    if (backgroundModelIndex >= 0) {
      bsVisibleModels.set(backgroundModelIndex);
      nDisplayed = 1;
    }
    if (cmi >= 0) {
      bsVisibleModels.set(cmi);
      return ++nDisplayed;
    }
    if (frameStep == 0)
      return nDisplayed;
    int frameDisplayed = 0;
    nDisplayed = 0;
    for (int iframe = firstFrameIndex; iframe != lastFrameIndex; iframe += frameStep) {
      int i = modelIndexForFrame(iframe); 
      if (!vwr.ms.isJmolDataFrameForModel(i)) {
        bsVisibleModels.set(i);
        nDisplayed++;
        frameDisplayed = iframe;
      }
    }
    int i = modelIndexForFrame(lastFrameIndex);
    if (firstFrameIndex == lastFrameIndex || !vwr.ms.isJmolDataFrameForModel(i)
        || nDisplayed == 0) {
      bsVisibleModels.set(i);
      if (nDisplayed == 0)
        firstFrameIndex = lastFrameIndex;
      nDisplayed = 0;
    }
    if (nDisplayed == 1 && cmi < 0)
      setFrame(frameDisplayed);   
    return nDisplayed;
  }

  private void animation(boolean TF) {
    animationOn = TF; 
    vwr.setBooleanProperty("_animating", TF);
  }
  
  private boolean setAnimationRelative(int direction) {
    int frameStep = getFrameStep(direction);
    int thisFrame = (isMovie ? caf : cmi);
    int frameNext = thisFrame + frameStep;
    float morphStep = 0f, nextMorphFrame = 0f;
    boolean isDone;
    if (morphCount > 0) {
      morphStep = 1f / (morphCount + 1);
      nextMorphFrame = currentMorphModel + frameStep * morphStep;
      isDone = isNotInRange(nextMorphFrame);
    } else {
      isDone = isNotInRange(frameNext);
    }
    if (isDone) {
      switch (animationReplayMode) {
      case T.once:
        return false;
      case T.loop:
        nextMorphFrame = frameNext = (animationDirection == currentDirection ? firstFrameIndex
            : lastFrameIndex);
        break;
      case T.palindrome:
        currentDirection = -currentDirection;
        frameNext -= 2 * frameStep;
        nextMorphFrame -= 2 * frameStep * morphStep;
      }
    }
    if (morphCount < 1) {
      if (frameNext < 0 || frameNext >= getFrameCount())
        return false;
      setFrame(frameNext);
      return true;
    }
    morph(nextMorphFrame + 1);
    return true;
  }

  private boolean isNotInRange(float frameNext) {
    float f = frameNext - 0.001f;
    return (f > firstFrameIndex && f > lastFrameIndex 
        || (f = frameNext + 0.001f) < firstFrameIndex
        && f < lastFrameIndex);
  }

  private int getFrameStep(int direction) {
    return frameStep * direction * currentDirection;
  }


}
