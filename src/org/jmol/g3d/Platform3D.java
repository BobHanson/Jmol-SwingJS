/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-11-27 10:42:18 -0600 (Wed, 27 Nov 2013) $
 * $Revision: 19034 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.g3d;

import javajs.api.GenericPlatform;

/**
 *<p>
 * Specifies the API to an underlying int[] buffer of ARGB values that
 * can be converted into an Image object and a short[] for z-buffer depth.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */ 
class Platform3D {

  int windowWidth, windowHeight, windowSize;
  int bufferWidth, bufferHeight, bufferSize, bufferSizeT;

  Object bufferedImage;
  int[] pBuffer, pBufferT;
  int[] zBuffer, zBufferT;

  int widthOffscreen, heightOffscreen;
  Object offscreenImage;
  Object graphicsForTextOrImage;
  
  //final static boolean desireClearingThread = false;
  //boolean useClearingThread = false;

  //private ClearingThread clearingThread;
  GenericPlatform apiPlatform;

  Platform3D(GenericPlatform apiPlatform) {
    //initialize(desireClearingThread);
    this.apiPlatform = apiPlatform;
  }
  
  Object getGraphicsForMetrics() {
    return apiPlatform.getGraphics(allocateOffscreenImage(1, 1));
  }
  
//  final void initialize(boolean useClearingThread) {
//    this.useClearingThread = useClearingThread;
//    if (useClearingThread) {
//      //Logger.debug("using ClearingThread");
//      clearingThread = new ClearingThread();
//      clearingThread.start();
//    }
//  }

  void allocateTBuffers(boolean antialiasTranslucent) {
    bufferSizeT = (antialiasTranslucent ? bufferSize : windowSize);
    zBufferT = new int[bufferSizeT];
    pBufferT = new int[bufferSizeT];    
  }
  
  /**
   * @param width 
   * @param height 
   * @param antialias 
   * @param isImageWrite  
   */
  void allocateBuffers(int width, int height, boolean antialias, boolean isImageWrite) {
    windowWidth = width;
    windowHeight = height;
    windowSize = width * height;
    if (antialias) {
      width *= 2;
      height *= 2;
    }
    bufferWidth = width;
    bufferHeight = height;
    
    bufferSize = bufferWidth * bufferHeight;
    zBuffer = new int[bufferSize];
    pBuffer = new int[bufferSize];
    // original thought was that there is
    // no nebed for any antialiasing on a translucent buffer
    // but that's simply not true.
    // bufferSizeT = windowSize;
    bufferedImage = apiPlatform.allocateRgbImage(windowWidth, windowHeight, pBuffer, windowSize, backgroundTransparent, isImageWrite);
  }
  
  void releaseBuffers() {
    windowWidth = windowHeight = bufferWidth = bufferHeight = bufferSize = -1;
    if (bufferedImage != null) {
      apiPlatform.flushImage(bufferedImage);
      bufferedImage = null;
    }
    pBuffer = null;
    zBuffer = null;
    pBufferT = null;
    zBufferT = null;
  }

  boolean hasContent() {
    for (int i = bufferSize; --i >= 0; )
      if (zBuffer[i] != Integer.MAX_VALUE)
        return true;
    return false;
  }

  void clearScreenBuffer() {
    for (int i = bufferSize; --i >= 0; ) {
      zBuffer[i] = Integer.MAX_VALUE;
      pBuffer[i] = 0;
    }
  }

  void setBackgroundColor(int bgColor) {
    if (pBuffer == null)
      return;
    for (int i = bufferSize; --i >= 0; )
      if (pBuffer[i] == 0)
        pBuffer[i] = bgColor;
  }
  
  void clearTBuffer() {
    for (int i = bufferSizeT; --i >= 0; ) {
      zBufferT[i] = Integer.MAX_VALUE;
      pBufferT[i] = 0;
    }
  }
  
  final void clearBuffer() {
    //if (useClearingThread) {
    //  clearingThread.clearClientBuffer();
    //} else {
      clearScreenBuffer();
    //}
  }

  final void clearScreenBufferThreaded() {
//    if (useClearingThread)
//      clearingThread.releaseBufferForClearing();
  }
  
  void notifyEndOfRendering() {
    apiPlatform.notifyEndOfRendering();
  }

  Object getGraphicsForTextOrImage(int width, int height) {
    if (width > widthOffscreen || height > heightOffscreen) {
      if (offscreenImage != null) {
        apiPlatform.disposeGraphics(graphicsForTextOrImage);
        apiPlatform.flushImage(offscreenImage);
      }
      if (width > widthOffscreen)
        widthOffscreen = width;
      if (height > heightOffscreen)
        heightOffscreen = height;
      offscreenImage = allocateOffscreenImage(widthOffscreen, heightOffscreen);
      graphicsForTextOrImage = apiPlatform.getStaticGraphics(offscreenImage, backgroundTransparent);
    }
    return graphicsForTextOrImage;
  }

  private Object allocateOffscreenImage(int width, int height) {
    return apiPlatform.newOffScreenImage(width, height);
  }

  void setBackgroundTransparent(boolean tf) {
    backgroundTransparent = tf;
  }

//  class ClearingThread extends Thread {
//
//
//    boolean bufferHasBeenCleared = false;
//    boolean clientHasBuffer = false;
//
//    /**
//     * 
//     * @param argbBackground
//     */
//    synchronized void notifyBackgroundChange(int argbBackground) {
//      //Logger.debug("notifyBackgroundChange");
//      bufferHasBeenCleared = false;
//      notify();
//      // for now do nothing
//    }
//
//    synchronized void clearClientBuffer() {
//      //Logger.debug("obtainBufferForClient()");
//      while (! bufferHasBeenCleared)
//        try { wait(); } catch (InterruptedException ie) {}
//      clientHasBuffer = true;
//    }
//
//    synchronized void releaseBufferForClearing() {
//      //Logger.debug("releaseBufferForClearing()");
//      clientHasBuffer = false;
//      bufferHasBeenCleared = false;
//      notify();
//    }
//
//    synchronized void waitForClientRelease() {
//      //Logger.debug("waitForClientRelease()");
//      while (clientHasBuffer || bufferHasBeenCleared)
//        try { wait(); } catch (InterruptedException ie) {}
//    }
//
//    synchronized void notifyBufferReady() {
//      //Logger.debug("notifyBufferReady()");
//      bufferHasBeenCleared = true;
//      notify();
//    }
//
//    @Override
//    public void run() {
//      /*
//      Logger.debug("running clearing thread:" +
//                         Thread.currentThread().getPriority());
//      */
//      while (true) {
//        waitForClientRelease();
//        clearScreenBuffer();
//        notifyBufferReady();
//      }
//    }
//  }

  private static boolean backgroundTransparent = false;
  
}
