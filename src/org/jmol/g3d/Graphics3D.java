/* $RCSfile$
 *  * $Author: hansonr $
 * $Date: 2017-02-12 13:55:04 -0600 (Sun, 12 Feb 2017) $
 * $Revision: 21428 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.jmol.api.GenericPlatform;
import org.jmol.api.Interface;
import org.jmol.api.JmolRendererInterface;
import org.jmol.c.STER;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;
import org.jmol.util.Normix;
import org.jmol.util.Rgb16;
import org.jmol.util.Shader;
import org.jmol.viewer.Viewer;

import javajs.util.AU;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.T3;
import javajs.util.V3;

/**
 * Provides high-level graphics primitives for 3D visualization for the software
 * renderers. These methods should not have to be used with WebGL or OpenGL or
 * other hardware accelerators.
 * 
 * This module is linked to via reflection from org.jmol.viewer.Viewer
 * 
 * Bob Hanson 9/2/2012
 * 
 * Note added 4/2015 BH:
 * 
 * Well, it turns out that the calculation of the intermediate pixel z value
 * in all methods involving rasterization of lines is incorrect and has been
 * incorrect since Jmol's inception. I noticed long ago that large triangles such as
 * produced in DRAW could incorrectly overlay/underlay other objects, but I could
 * never determine why. It turns out that the assumption that z-value is linear
 * across a line when perspectiveDepth is TRUE is simply incorrect. 
 * 
 * Basically, the function z(x) is non-linear. Treating it as simply a 
 * linear function results in oddities where lines and planes
 *  -- particularly created using DRAW and large distances -- appear
 * to be where they are not. 
 *  
 * Through Jmol 13.3.13 we had the standard linear relationship:
 * 
 *   z = (x - xa) / (xb - xa) * (zb - za) + za
 * 
 * I worked it out, and, amazingly, it should be
 * 
 *   z = (xb - xa) * za * zb / ((xb - x) * zb + (x - xa) * za)
 * 
 * Note that it is still true that when x = xb, z = zb 
 * and when x = xa, z = za, as required.
 * 
 * This equation can be rearranged to 
 * 
 *   z = a / (b - x)
 *   
 * where 
 * 
 *  a = (xb - xa) * za * (zb / (zb - za))
 *  
 * and
 * 
 *  b = (xb * zb - xa * za) / (zb - za)
 * 
 * These values must be floats, not integers, to work properly, because
 * these are extrapolations from long distances in some cases. So there is
 * considerable overhead there. It will take some experimentation to figure this
 * out.
 * 
 * The practical implications are for line, cylinder, and triangle drawing.
 * First-pass corrections are for axes and DRAW objects. They tend to be the
 * larger objects that result in the issue. 
 * 
 * Also affected is POV-Ray output, because right now POV-Ray is created using
 * perspective on and plotted as though it were orthographic, but although that
 * works in x and y, it does not work in z!
 * 
 * 
 * <p>
 * A pure software implementation of a 3D graphics engine. No hardware required.
 * Depending upon what you are rendering ... some people say it is <i>pretty
 * fast</i>.
 * 
 * @author Miguel, miguel@jmol.org
 * 
 *         with additions by Bob Hanson hansonr@stolaf.edu
 * 
 *         The above is an understatement to say the least.
 * 
 *         This is a two-pass rendering system. In the first pass, all opaque
 *         objects are rendered. In the second pass, all translucent objects are
 *         rendered.
 * 
 *         If there are no translucent objects, then that is found in the first
 *         pass as follows:
 * 
 *         The renderers first try to set the color index of the object to be
 *         rendered using setColix(short colix), and that method returns false
 *         if we are in the wrong pass for that type of object.
 * 
 *         In addition, setColix records in the boolean haveTranslucentObjects
 *         whether a translucent object was seen in the first pass.
 * 
 *         The second pass is skipped if this flag is not set. This saves
 *         immensely on rendering time when there are no translucent objects.
 * 
 *         THUS, IT IS CRITICAL THAT ALL RENDERING OPTIONS CHECK THE COLIX USING
 *         g3d.setColix(short colix) PRIOR TO RENDERING.
 * 
 *         Translucency is rendered only approximately. We can't maintain a full
 *         buffer of all translucent objects. Instead, we "cheat" by maintaining
 *         one translucent z buffer. When a translucent pixel is to be written,
 *         its z position is checked and...
 * 
 *         ...if it is behind or at the z position of any pixel, it is ignored
 *         ...if it is in front of a translucent pixel, it is added to the
 *         translucent buffer ...if it is between an opaque and translucent
 *         pixel, the translucent pixel is turned opaque, and the new pixel is
 *         added to the translucent buffer
 * 
 *         This guarantees accurate translucency when there are no more than two
 *         translucent pixels between the user and an opaque pixel. It's a
 *         fudge, for sure. But it is pretty good, and certainly fine for
 *         "draft" work.
 * 
 *         Users needing more accurate translucencty are encouraged to use the
 *         POV-Ray export facility for production-level work.
 * 
 *         Antialiasing is accomplished as full scene antialiasing. This means
 *         that the width and height are doubled (both here and in
 *         TransformManager), the scene is rendered, and then each set of four
 *         pixels is averaged (roughly) as the final pixel in the width*height
 *         buffer.
 * 
 *         Antialiasing options allow for antialiasing of all objects:
 * 
 *         antialiasDisplay = true antialiasTranslucent = true
 * 
 *         or just the opaque ones:
 * 
 *         antialiasDisplay = true antialiasTranslucent = false
 * 
 *         or not at all:
 * 
 *         antialiasDisplay = false
 * 
 *         The difference will be speed and memory. Adding translucent objects
 *         doubles the buffer requirement, and adding antialiasing quadruples
 *         the buffer requirement.
 * 
 *         So we have:
 * 
 *         Memory requirements are significant, in multiples of (width) *
 *         (height) 32-bit integers:
 * 
 *         antialias OFF ON/opaque only ON/all objects
 * 
 *         no translucent 1p + 1z = 2 4p + 4z = 8 4p + 4z = 8 objects
 * 
 *         with translucent 2p + 2z = 4 5p + 5z = 10 8p + 8z = 16 objects
 * 
 *         Note that no antialising at all is required for POV-Ray output.
 *         POV-Ray will do antialiasing on its own.
 * 
 *         In principle we could save a bit in the case of antialiasing of just
 *         opaque objects and reuse the p and z buffers for the translucent
 *         buffer, but this hasn't been implemented because the savings isn't
 *         that great, and if you are going to the trouble of having
 *         antialiasing, you probably what it all.
 * 
 * 
 */

final public class Graphics3D extends GData implements JmolRendererInterface {

  Platform3D platform;
  LineRenderer line3d;
  private SphereRenderer sphere3d;
  private CylinderRenderer cylinder3d;

  // loaded only if needed
  private G3DRenderer triangle3d;
  private G3DRenderer circle3d;
  private G3DRenderer hermite3d;

  private boolean isFullSceneAntialiasingEnabled;
  private boolean antialias2;

  private TextString[] strings = null;
  private int stringCount;

  @Override
  public boolean isWebGL() {
    return false;
  }

  @Override
  public void clear() {
    stringCount = 0;
    strings = null;
    TextRenderer.clearFontCache();
  }

  @Override
  public void destroy() {
    releaseBuffers();
    platform = null;
    pixel = pixel0 = pixelShaded = null;
    pixelT0 = null;
    pixelScreened = null;
    graphicsForMetrics = null;
  }

  private byte[] anaglyphChannelBytes;

  private boolean twoPass = false;

  private boolean haveTranslucentObjects;
  protected int[] pbuf;
  protected int[] pbufT;
  protected int[] zbuf;
  protected int[] zbufT;
  protected int translucencyMask;
  private boolean renderLow;

  private int[] shadesCurrent;
  private int anaglyphLength;

  Pixelator pixel;
  Pixelator pixel0;
  private PixelatorT pixelT0;
  private PixelatorScreened pixelScreened;
  private PixelatorShaded pixelShaded;

  protected int zMargin;
//  private int[] aobuf;

  void setZMargin(int dz) {
    zMargin = dz;
  }

  public Graphics3D() {
    for (int i = normixCount; --i >= 0;)
      transformedVectors[i] = new V3();
  }

  @Override
  public void initialize(Viewer vwr, GenericPlatform apiPlatform) {
    this.vwr = vwr;
    this.apiPlatform = apiPlatform;
    platform = new Platform3D(apiPlatform);
    pixel = pixel0 = new Pixelator(this);
    graphicsForMetrics = platform.getGraphicsForMetrics();

    line3d = new LineRenderer(this);
    sphere3d = new SphereRenderer(this);
    cylinder3d = new CylinderRenderer(this);
  }

  /**
   * allows core JavaScript loading to not involve these classes
   * 
   * @param tok
   * 
   */
  @Override
  public void addRenderer(int tok) {
    switch (tok) {
    case T.circle:
      if (circle3d == null)
        circle3d = getRenderer("Circle");
      break;
    case T.hermitelevel:
      if (hermite3d == null)
        hermite3d = getRenderer("Hermite");
      //$FALL-THROUGH$
    case T.triangles:
      if (triangle3d == null) {
        triangle3d = getRenderer("Triangle");
        ((PrecisionRenderer) triangle3d).isOrthographic = !vwr.tm.perspectiveDepth;
      }      
      break;
    }
  }

  private G3DRenderer getRenderer(String type) {
    G3DRenderer r = ((G3DRenderer) Interface.getOption("g3d." + type
        + "Renderer", vwr, "render"));
    if (r == null)
      throw new NullPointerException("Interface");
    r.set(this, this);
    return r;
  }

  @Override
  public void setWindowParameters(int width, int height, boolean antialias) {
    setWinParams(width, height, antialias);
    if (currentlyRendering)
      endRendering();
  }

  @Override
  public boolean checkTranslucent(boolean isAlphaTranslucent) {
    if (isAlphaTranslucent)
      haveTranslucentObjects = true;
    return (!twoPass || twoPass && (isPass2 == isAlphaTranslucent));
  }

  @Override
  public void beginRendering(M3 rotationMatrix, boolean translucentMode,
                             boolean isImageWrite, boolean renderLow) {
    if (currentlyRendering)
      endRendering();
    this.renderLow = renderLow;
    if (windowWidth != newWindowWidth || windowHeight != newWindowHeight
        || newAntialiasing != isFullSceneAntialiasingEnabled) {
      windowWidth = newWindowWidth;
      windowHeight = newWindowHeight;
      isFullSceneAntialiasingEnabled = newAntialiasing;
      releaseBuffers();
    }
    setRotationMatrix(rotationMatrix);
    ((PrecisionRenderer) line3d).isOrthographic = !vwr.tm.perspectiveDepth; 
    if (triangle3d != null)
      ((PrecisionRenderer) triangle3d).isOrthographic = !vwr.tm.perspectiveDepth;
    antialiasEnabled = antialiasThisFrame = newAntialiasing;
    currentlyRendering = true;
    if (strings != null)
      for (int i = Math.min(strings.length, stringCount); --i >= 0;)
        strings[i] = null;
    stringCount = 0;
    twoPass = true; //only for testing -- set false to disallow second pass
    isPass2 = false;
    pass2Flag01 = 0;
    colixCurrent = 0;
    haveTranslucentObjects = wasScreened = false;
    pixel = pixel0;
    pixel.bgcolor = bgcolor;
    translucentCoverOnly = !translucentMode;
    if (pbuf == null) {
      platform.allocateBuffers(windowWidth, windowHeight, antialiasThisFrame,
          isImageWrite);
      pbuf = platform.pBuffer;
      zbuf = platform.zBuffer;
      //aobuf = null;
      pixel0.setBuf();
      if (pixelT0 != null)
        pixelT0.setBuf();
      if (pixelShaded != null)
        pixelShaded.setBuf();
    }
    setWidthHeight(antialiasThisFrame);
    if (pixelScreened != null)
      pixelScreened.width = width;
    platform.clearBuffer();
    if (backgroundImage != null)
      plotImage(Integer.MIN_VALUE, 0, Integer.MIN_VALUE, backgroundImage, null,
          (short) 0, 0, 0);
    textY = 0;
  }

  @Override
  public void setBackgroundTransparent(boolean TF) {
    if (platform != null)
      platform.setBackgroundTransparent(TF);
  }

  private void releaseBuffers() {
    pbuf = null;
    zbuf = null;
    pbufT = null;
    zbufT = null;
    //aobuf = null;
    platform.releaseBuffers();
    line3d.clearLineCache();
  }

  @Override
  public boolean setPass2(boolean antialiasTranslucent) {
    if (!haveTranslucentObjects || !currentlyRendering)
      return false;
    isPass2 = true;
    pass2Flag01 = 1;
    colixCurrent = 0;
    if (pbufT == null || antialias2 != antialiasTranslucent) {
      platform.allocateTBuffers(antialiasTranslucent);
      pbufT = platform.pBufferT;
      zbufT = platform.zBufferT;
    }
    antialias2 = antialiasTranslucent;
    if (antialiasThisFrame && !antialias2)
      downsampleFullSceneAntialiasing(true);
    platform.clearTBuffer();
    if (pixelT0 == null)
      pixelT0 = new PixelatorT(this);
    if (pixel.p0 == null)
      pixel = pixelT0;
    else
      pixel.p0 = pixelT0;
    return true;
  }

  @Override
  public void endRendering() {
    if (!currentlyRendering)
      return;
    if (pbuf != null) {
      if (isPass2 && pbufT != null)
        for (int offset = pbufT.length; --offset >= 0;)
          pbuf[offset] = mergeBufferPixel(pbuf[offset], pbufT[offset], bgcolor);
      
      if (pixel == pixelShaded && pixelShaded.zShadePower == 0)
        pixelShaded.showZBuffer();
      

      //      if (ambientOcclusion != 0) {
      //        if (aobuf == null)
      //          aobuf = new int[pbuf.length];
      //        else
      //          for (int offset = pbuf.length; --offset >= 0;)
      //            aobuf[offset] = 0;
      //        shader
      //            .occludePixels(pbuf, zbuf, aobuf, width, height, ambientOcclusion);
      //      }
      if (antialiasThisFrame)
        downsampleFullSceneAntialiasing(false);
    }
    platform.setBackgroundColor(bgcolor);
    platform.notifyEndOfRendering();
    currentlyRendering = isPass2 = false;
  }

  public static int mergeBufferPixel(int argbA, int argbB,
                                      int bgcolor) {
    if (argbB == 0 || argbA == argbB)
      return argbA;
    if (argbA == 0)
      argbA = bgcolor;
    int rbA = (argbA & 0x00FF00FF);
    int gA = (argbA & 0x0000FF00);
    int rbB = (argbB & 0x00FF00FF);
    int gB = (argbB & 0x0000FF00);
    int logAlpha = (argbB >> 24) & 0xF;
    //just for now:
    //0 or 1=100% opacity, 2=87.5%, 3=75%, 4=50%, 5=50%, 6 = 25%, 7 = 12.5% opacity.
    switch (logAlpha) {
    // 0.0 to 1.0 ==> MORE translucent   
    //                1/8  1/4 3/8 1/2 5/8 3/4 7/8
    //     t           32  64  96  128 160 192 224
    //     t >> 5       1   2   3   4   5   6   7

    case 0: // 8:0
      rbA = rbB;
      gA = gB;
      break;
    case 1: // 7:1
      rbA = (((rbB << 2) + (rbB << 1) + rbB + rbA) >> 3) & 0x00FF00FF;
      gA = (((gB << 2) + +(gB << 1) + gB + gA) >> 3) & 0x0000FF00;
      break;
    case 2: // 3:1
      rbA = (((rbB << 1) + rbB + rbA) >> 2) & 0x00FF00FF;
      gA = (((gB << 1) + gB + gA) >> 2) & 0x0000FF00;
      break;
    case 3: // 5:3
      rbA = (((rbB << 2) + rbB + (rbA << 1) + rbA) >> 3) & 0x00FF00FF;
      gA = (((gB << 2) + gB + (gA << 1) + gA) >> 3) & 0x0000FF00;
      break;
    case 4: // 1:1
      rbA = ((rbA + rbB) >> 1) & 0x00FF00FF;
      gA = ((gA + gB) >> 1) & 0x0000FF00;
      break;
    case 5: // 3:5
      rbA = (((rbB << 1) + rbB + (rbA << 2) + rbA) >> 3) & 0x00FF00FF;
      gA = (((gB << 1) + gB + (gA << 2) + gA) >> 3) & 0x0000FF00;
      break;
    case 6: // 1:3
      rbA = (((rbA << 1) + rbA + rbB) >> 2) & 0x00FF00FF;
      gA = (((gA << 1) + gA + gB) >> 2) & 0x0000FF00;
      break;
    case 7: // 1:7
      rbA = (((rbA << 2) + (rbA << 1) + rbA + rbB) >> 3) & 0x00FF00FF;
      gA = (((gA << 2) + (gA << 1) + gA + gB) >> 3) & 0x0000FF00;
      break;
    }
    return 0xFF000000 | rbA | gA;
  }

  @Override
  public Object getScreenImage(boolean isImageWrite) {

    Object obj = platform.bufferedImage;
    boolean dorelease = /** @j2sNative isImageWrite || */ false;
    /**
     * @j2sNative
     * 
     * obj._img = null;
     * if (!dorelease) obj._buf = true;
     * 
     * 
     */    
    if (dorelease)
      releaseBuffers();
    return obj;
  }
  
  @Override
  public void applyAnaglygh(STER stereoMode, int[] stereoColors) {
    switch (stereoMode) {
    case REDCYAN:
      for (int i = anaglyphLength; --i >= 0;) {
        int blue = anaglyphChannelBytes[i] & 0x000000FF;
        int cyan = (blue << 8) | blue;
        pbuf[i] = pbuf[i] & 0xFFFF0000 | cyan;
      }
      break;
    case CUSTOM:
      //best if complementary, but they do not have to be
      int color1 = stereoColors[0];
      int color2 = stereoColors[1] & 0x00FFFFFF;
      for (int i = anaglyphLength; --i >= 0;) {
        int a = anaglyphChannelBytes[i] & 0x000000FF;
        a = (a | ((a | (a << 8)) << 8)) & color2;
        pbuf[i] = (pbuf[i] & color1) | a;
      }
      break;
    case REDBLUE:
      for (int i = anaglyphLength; --i >= 0;) {
        int blue = anaglyphChannelBytes[i] & 0x000000FF;
        pbuf[i] = (pbuf[i] & 0xFFFF0000) | blue;
      }
      break;
    case REDGREEN:
      for (int i = anaglyphLength; --i >= 0;) {
        int green = (anaglyphChannelBytes[i] & 0x000000FF) << 8;
        pbuf[i] = (pbuf[i] & 0xFFFF0000) | green;
      }
      break;
    case DTI:
    case DOUBLE:
    case NONE:
      break;
    }
  }

  @Override
  public void snapshotAnaglyphChannelBytes() {
    if (currentlyRendering)
      throw new NullPointerException();
    anaglyphLength = windowWidth * windowHeight;
    if (anaglyphChannelBytes == null
        || anaglyphChannelBytes.length != anaglyphLength)
      anaglyphChannelBytes = new byte[anaglyphLength];
    for (int i = anaglyphLength; --i >= 0;)
      anaglyphChannelBytes[i] = (byte) pbuf[i];
  }

  @Override
  public void releaseScreenImage() {
    platform.clearScreenBufferThreaded();
  }

  @Override
  public boolean haveTranslucentObjects() {
    return haveTranslucentObjects;
  }

  @Override
  public void setSlabAndZShade(int slabValue, int depthValue, int zSlab,
                               int zDepth, int zShadePower) {
    setSlab(slabValue);
    setDepth(depthValue);
    if (zSlab < zDepth) {
      if (pixelShaded == null)
        pixelShaded = new PixelatorShaded(this);
      pixel = pixelShaded.set(zSlab, zDepth, zShadePower);
    } else {
      pixel = pixel0;
    }
  }

  private void downsampleFullSceneAntialiasing(boolean downsampleZBuffer) {
    // now is the time we have to put in the correct background color
    // this was a bug in 11.6.0-11.6.2. 

    // we must downsample the Z Buffer if there are translucent
    // objects left to draw and antialiasTranslucent is set false
    // in that case we must fudge the background color, because
    // otherwise a match of the background color with an object
    // will put it in the back -- the "blue tie on a blue screen"
    // television effect. We want to avoid that. Here we can do that
    // because the colors will be blurred anyway.

    int bgcheck = bgcolor;
    if (downsampleZBuffer)
      bgcheck += ((bgcheck & 0xFF) == 0xFF ? -1 : 1);
    downsample2d(pbuf, windowWidth, windowHeight, bgcheck);
    if (downsampleZBuffer) {
      downsample2dZ(pbuf, zbuf, windowWidth, windowHeight, bgcheck);
      antialiasThisFrame = false;
      setWidthHeight(false);
    }
  }

  public static void downsample2d(int[] pbuf, int width, int height, int bgcheck) {
    int width4 = width << 1;
    if (bgcheck != 0) {
      bgcheck &= 0xFFFFFF;
      for (int i = pbuf.length; --i >= 0;)
        if (pbuf[i] == 0)
          pbuf[i] = bgcheck;
    }
    int bg0 = ((bgcheck >> 2) & 0x3F3F3F3F) << 2;
    bg0 += (bg0 & 0xC0C0C0C0) >> 6;

    int offset1 = 0;
    int offset4 = 0;
    for (int i = height; --i >= 0; offset4 += width4)
      for (int j = width; --j >= 0; ++offset1) {

        /* more precise, but of no benefit:

        int a = pbuf[offset4];
        int b = pbuf[offset4++ + width4];
        int c = pbuf[offset4];
        int d = pbuf[offset4++ + width4];
        int argb = ((((a & 0x0f0f0f) + (b & 0x0f0f0f)
           + (c & 0x0f0f0f) + (d & 0x0f0f0f)) >> 2) & 0x0f0f0f)
           + ( ((a & 0xF0F0F0) + (b & 0xF0F0F0) 
           +   (c & 0xF0F0F0) + (d & 0xF0F0F0)
                ) >> 2);
        */

        int argb = ((pbuf[offset4] >> 2) & 0x3F3F3F3F)
            + ((pbuf[offset4++ + width4] >> 2) & 0x3F3F3F3F)
            + ((pbuf[offset4] >> 2) & 0x3F3F3F3F)
            + ((pbuf[offset4++ + width4] >> 2) & 0x3F3F3F3F);
        argb += (argb & 0xC0C0C0C0) >> 6;
        if (argb == bg0)
          argb = bgcheck;

        /**
         * I don't know why this is necessary.
         * 
         * @j2sNative
         * 
         *            pbuf[offset1] = argb & 0x00FFFFFF | 0xFF000000;
         */
        {
          pbuf[offset1] = argb & 0x00FFFFFF;
        }
      }
  }

  private static void downsample2dZ(int[] pbuf, int[] zbuf, int width,
                                    int height, int bgcheck) {
    int width4 = width << 1;
    //we will add the alpha mask later
    int offset1 = 0, offset4 = 0;
    for (int i = height; --i >= 0; offset4 += width4)
      for (int j = width; --j >= 0; ++offset1, ++offset4) {
        int z = Math.min(zbuf[offset4], zbuf[offset4 + width4]);
        z = Math.min(z, zbuf[++offset4]);
        z = Math.min(z, zbuf[offset4 + width4]);
        if (z != Integer.MAX_VALUE)
          z >>= 1;
        zbuf[offset1] = (pbuf[offset1] == bgcheck ? Integer.MAX_VALUE : z);
      }
  }

  public boolean hasContent() {
    return platform.hasContent();
  }

  int currentShadeIndex;
  private int lastRawColor;
  int translucencyLog;
  private boolean wasScreened;

  /**
   * sets current color from colix color index
   * 
   * @param colix
   *        the color index
   * @return true or false if this is the right pass
   */
  @Override
  public boolean setC(short colix) {
    boolean isLast = C.isColixLastAvailable(colix);
    if (!isLast && colix == colixCurrent && currentShadeIndex == -1)
      return true;
    int mask = colix & C.TRANSLUCENT_MASK;
    if (mask == C.TRANSPARENT)
      return false;
    if (renderLow)
      mask = 0;
    boolean isTranslucent = (mask != 0);
    boolean isScreened = (isTranslucent && mask == C.TRANSLUCENT_SCREENED);
    setScreened(isScreened);
    if (!checkTranslucent(isTranslucent && !isScreened))
      return false;
    if (isPass2) {
      translucencyMask = (mask << C.ALPHA_SHIFT) | 0xFFFFFF;
      translucencyLog = mask >> C.TRANSLUCENT_SHIFT;
    } else {
      translucencyLog = 0;
    }
    colixCurrent = colix;
    if (isLast) {
      if (argbCurrent != lastRawColor) {
        if (argbCurrent == 0)
          argbCurrent = 0xFFFFFFFF;
        lastRawColor = argbCurrent;
        shader.setLastColix(argbCurrent, inGreyscaleMode);
      }
    }
    shadesCurrent = getShades(colix);
    currentShadeIndex = -1;
    setColor(getColorArgbOrGray(colix));
    return true;
  }

  Pixelator setScreened(boolean isScreened) {
    if (wasScreened != isScreened) {
      wasScreened = isScreened;
      if (isScreened) {
        if (pixelScreened == null)
          pixelScreened = new PixelatorScreened(this, pixel0);
        if (pixel.p0 == null)
          pixel = pixelScreened;
        else
          pixel.p0 = pixelScreened;
      } else if (pixel.p0 == null || pixel == pixelScreened) {
        pixel = (isPass2 ? pixelT0 : pixel0);
      } else {
        pixel.p0 = (isPass2 ? pixelT0 : pixel0);
      }
    }
    return pixel;
  }

  @Override
  public void drawFilledCircle(short colixRing, short colixFill, int diameter,
                               int x, int y, int z) {
    // Halos, Draw handles
    if (isClippedZ(z))
      return;
    int r = (diameter + 1) / 2;
    boolean isClipped = x < r || x + r >= width || y < r || y + r >= height;
    if (isClipped && isClippedXY(diameter, x, y))
      return;
    if (colixRing != 0 && setC(colixRing)) {
      if (isClipped) {
        if (!isClippedXY(diameter, x, y))
          ((CircleRenderer) circle3d).plotCircleCenteredClipped(x, y, z,
              diameter);
      } else {
        ((CircleRenderer) circle3d).plotCircleCenteredUnclipped(x, y, z,
            diameter);
      }
    }
    if (colixFill != 0 && setC(colixFill)) {
      if (isClipped)
        ((CircleRenderer) circle3d).plotFilledCircleCenteredClipped(x, y, z,
            diameter);
      else
        ((CircleRenderer) circle3d).plotFilledCircleCenteredUnclipped(x, y, z,
            diameter);
    }
  }

  @Override
  public void volumeRender4(int diameter, int x, int y, int z) {
    if (diameter == 1) {
      plotPixelClippedArgb(argbCurrent, x, y, z, width, zbuf, pixel);
      return;
    }
    if (isClippedZ(z))
      return;
    int r = (diameter + 1) / 2;
    boolean isClipped = x < r || x + r >= width || y < r || y + r >= height;
    if (isClipped && isClippedXY(diameter, x, y))
      return;
    if (isClipped)
      ((CircleRenderer) circle3d).plotFilledCircleCenteredClipped(x, y, z,
          diameter);
    else
      ((CircleRenderer) circle3d).plotFilledCircleCenteredUnclipped(x, y, z,
          diameter);
  }

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param x
   *        center x
   * @param y
   *        center y
   * @param z
   *        center z
   */
  @Override
  public void fillSphereXYZ(int diameter, int x, int y, int z) {
    switch (diameter) {
    case 1:
      plotPixelClippedArgb(argbCurrent, x, y, z, width, zbuf, pixel);
      return;
    case 0:
      return;
    }
    if (diameter <= (antialiasThisFrame ? SphereRenderer.maxSphereDiameter2
        : SphereRenderer.maxSphereDiameter))
      sphere3d.render(shadesCurrent, diameter, x, y, z, null, null, null, -1,
          null);
  }

  private int saveAmbient, saveDiffuse;

  @Override
  public void volumeRender(boolean TF) {
    if (TF) {
      saveAmbient = getAmbientPercent();
      saveDiffuse = getDiffusePercent();
      setAmbientPercent(100);
      setDiffusePercent(0);
      addRenderer(T.circle);
    } else {
      setAmbientPercent(saveAmbient);
      setDiffusePercent(saveDiffuse);
    }
  }

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param center
   *        javax.vecmath.Point3i defining the center
   */

  @Override
  public void fillSphereI(int diameter, P3i center) {
    fillSphereXYZ(diameter, center.x, center.y, center.z);
  }

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param center
   *        a javax.vecmath.Point3f ... floats are casted to ints
   */
  @Override
  public void fillSphereBits(int diameter, P3 center) {
    // from hermite ribbon
    fillSphereXYZ(diameter, Math.round(center.x), Math.round(center.y),
        Math.round(center.z));
  }

  @Override
  public void fillEllipsoid(P3 center, P3[] points, int x, int y, int z,
                            int diameter, M3 mToEllipsoidal, double[] coef,
                            M4 mDeriv, int selectedOctant, P3[] octantPoints) {
    switch (diameter) {
    case 1:
      plotPixelClippedArgb(argbCurrent, x, y, z, width, zbuf, pixel);
      return;
    case 0:
      return;
    }
    if (diameter <= (antialiasThisFrame ? SphereRenderer.maxSphereDiameter2
        : SphereRenderer.maxSphereDiameter))
      sphere3d.render(shadesCurrent, diameter, x, y, z, mToEllipsoidal, coef,
          mDeriv, selectedOctant, octantPoints);
  }

  /**
   * draws a rectangle
   * 
   * @param x
   *        upper left x
   * @param y
   *        upper left y
   * @param z
   *        upper left z
   * @param zSlab
   *        z for slab check (for set labelsFront)
   * @param rWidth
   *        pixel count
   * @param rHeight
   *        pixel count
   */
  @Override
  public void drawRect(int x, int y, int z, int zSlab, int rWidth, int rHeight) {
    // labels (and rubberband, not implemented) and navigation cursor
    if (zSlab != 0 && isClippedZ(zSlab))
      return;
    int w = rWidth - 1;
    int h = rHeight - 1;
    int xRight = x + w;
    int yBottom = y + h;
    if (y >= 0 && y < height)
      drawHLine(x, y, z, w);
    if (yBottom >= 0 && yBottom < height)
      drawHLine(x, yBottom, z, w);
    if (x >= 0 && x < width)
      drawVLine(x, y, z, h);
    if (xRight >= 0 && xRight < width)
      drawVLine(xRight, y, z, h);
  }

  private void drawHLine(int x, int y, int z, int w) {
    // hover, labels only
    if (w < 0) {
      x += w;
      w = -w;
    }
    if (x < 0) {
      w += x;
      x = 0;
    }
    if (x + w >= width)
      w = width - 1 - x;
    Pixelator p = pixel;
    int c = argbCurrent;
    int offset = x + width * y;
    for (int i = 0; i <= w; i++) {
      if (z < zbuf[offset])
        p.addPixel(offset, z, c);
      offset++;
    }
  }

  private void drawVLine(int x, int y, int z, int h) {
    // hover, labels only
    if (h < 0) {
      y += h;
      h = -h;
    }
    if (y < 0) {
      h += y;
      y = 0;
    }
    if (y + h >= height) {
      h = height - 1 - y;
    }
    int offset = x + width * y;
    Pixelator p = pixel;
    int c = argbCurrent;
    for (int i = 0; i <= h; i++) {
      if (z < zbuf[offset])
        p.addPixel(offset, z, c);
      offset += width;
    }
  }

  /**
   * fills background rectangle for label
   * <p>
   * 
   * @param x
   *        upper left x
   * @param y
   *        upper left y
   * @param z
   *        upper left z
   * @param zSlab
   *        z value for slabbing
   * @param widthFill
   *        pixel count
   * @param heightFill
   *        pixel count
   */
  @Override
  public void fillTextRect(int x, int y, int z, int zSlab, int widthFill,
                       int heightFill) {
    // hover and labels only -- slab at atom or front -- simple Z/window clip
    if (isClippedZ(zSlab))
      return;
    int w = width;
    if (x < 0) {
      widthFill += x;
      if (widthFill <= 0)
        return;
      x = 0;
    }
    if (x + widthFill > w) {
      widthFill = w - x;
      if (widthFill <= 0)
        return;
    }
    if (y < 0) {
      heightFill += y;
      if (heightFill <= 0)
        return;
      y = 0;
    }
    if (y + heightFill > height)
      heightFill = height - y;
    int c = argbCurrent;
    int[] zb = zbuf;
    Pixelator p = pixel;
    while (--heightFill >= 0)
      plotPixelsUnclippedCount(c, widthFill, x, y++, z, w, zb, p);
  }

  /**
   * draws the specified string in the current font. no line wrapping -- axis,
   * labels, measures
   * 
   * @param str
   *        the String
   * @param font3d
   *        the Font3D
   * @param xBaseline
   *        baseline x
   * @param yBaseline
   *        baseline y
   * @param z
   *        baseline z
   * @param zSlab
   *        z for slab calculation
   * @param bgColix
   */

  @Override
  public void drawString(String str, Font font3d, int xBaseline, int yBaseline,
                         int z, int zSlab, short bgColix) {
    //axis, labels, measures, echo    
    currentShadeIndex = 0;
    if (str == null)
      return;
    if (isClippedZ(zSlab))
      return;
    drawStringNoSlab(str, font3d, xBaseline, yBaseline, z, bgColix);
  }

  /**
   * draws the specified string in the current font. no line wrapping -- echo,
   * frank, hover, molecularOrbital, uccage
   * 
   * @param str
   *        the String
   * @param font3d
   *        the Font3D
   * @param xBaseline
   *        baseline x
   * @param yBaseline
   *        baseline y
   * @param z
   *        baseline z
   * @param bgColix
   */

  @Override
  public void drawStringNoSlab(String str, Font font3d, int xBaseline,
                               int yBaseline, int z, short bgColix) {
    // echo, frank, hover, molecularOrbital, uccage
    if (str == null)
      return;
    if (strings == null)
      strings = new TextString[10];
    if (stringCount == strings.length)
      strings = (TextString[]) AU.doubleLength(strings);
    TextString t = new TextString();
    t.setText(str, font3d == null ? currentFont : (currentFont = font3d),
        argbCurrent, C.isColixTranslucent(bgColix) ? // shift colix translucency mask into integer alpha position
        (getColorArgbOrGray(bgColix) & 0xFFFFFF)
            | ((bgColix & C.TRANSLUCENT_MASK) << C.ALPHA_SHIFT)
            : 0, xBaseline, yBaseline, z);
    strings[stringCount++] = t;

  }

  public static Comparator<TextString> sort;

  @Override
  public void renderAllStrings(Object jmolRenderer) {
    if (strings == null)
      return;
    if (stringCount >= 2) {
      if (sort == null)
        sort = new TextString();
      Arrays.sort(strings, sort);
    }
    for (int i = 0; i < stringCount; i++) {
      TextString ts = strings[i];
      plotText(ts.x, ts.y, ts.z, ts.argb, ts.bgargb, ts.text, ts.font,
          (JmolRendererInterface) jmolRenderer);
    }
    strings = null;
    stringCount = 0;
  }

  @Override
  public void plotText(int x, int y, int z, int argb, int bgargb, String text,
                       Font font3d, JmolRendererInterface jmolRenderer) {
    TextRenderer.plot(x, y, z, argb, bgargb, text, font3d, this, jmolRenderer,
        antialiasThisFrame);
  }

  @Override
  public void drawImage(Object objImage, int x, int y, int z, int zSlab,
                        short bgcolix, int width, int height) {
    // overridden in Export
    if (objImage != null && width > 0 && height > 0 && !isClippedZ(zSlab))
      plotImage(x, y, z, objImage, null, bgcolix, width, height);
  }

  @Override
  public void plotImage(int x, int y, int z, Object image,
                        JmolRendererInterface jmolRenderer, short bgcolix,
                        int imageWidth, int imageHeight) {
    // overridden in __Exporter
    setC(bgcolix);
    if (!isPass2)
      translucencyMask = -1;
    if (bgcolix == 0)
      argbCurrent = 0;
    boolean isBackground = (x == Integer.MIN_VALUE);
    int bg = (isBackground ? bgcolor : argbCurrent);
    if (isBackground) {
      x = 0;
      z = Integer.MAX_VALUE - 1;
      imageWidth = width;
      imageHeight = height;
    }
    if (x + imageWidth <= 0 || x >= width || y + imageHeight <= 0
        || y >= height)
      return;
    Object g = (vwr.isJSNoAWT ? null : platform.getGraphicsForTextOrImage(imageWidth, imageHeight));
    int[] buffer = apiPlatform.drawImageToBuffer(g,
        platform.offscreenImage, image, imageWidth, imageHeight,
        isBackground ? bg : 0);
    if (buffer == null)
      return;
    int[] zb = zbuf;
    int w = width;
    Pixelator p = pixel;
    int h = height;
    int t = translucencyLog;
    if (jmolRenderer == null
        && (x >= 0 && x + imageWidth <= w && y >= 0 && y + imageHeight <= h)) {
      // unclipped 
      for (int i = 0, offset = 0, pbufOffset = y * w + x; i < imageHeight; i++, pbufOffset += (w - imageWidth)) {
        for (int j = 0; j < imageWidth; j++,offset++,pbufOffset++) {
          if (z < zb[pbufOffset]) {
            int b = buffer[offset];
            if ((b & 0xFF000000) == (0xFF000000 & 0xFF000000))
              p.addPixel(pbufOffset, z, b);
          }
        }
      }
    } else {
      if (jmolRenderer == null)
        jmolRenderer = this;
      // clipped in some way -- let G3D or Exporter handle it
      for (int i = 0, offset = 0; i < imageHeight; i++)
        for (int j = 0; j < imageWidth; j++) {
          int b = buffer[offset++];
          if ((b & 0xFF000000) == (0xFF000000 & 0xFF000000))
            jmolRenderer.plotImagePixel(b, x + j, y + i, z, (byte) 8, bg, w,
              h, zb, p, t);
        }
    }
  }

  @Override
  public void setFont(Font font3d) {
    currentFont = font3d;
  }

  /*
  private void setRectClip(int x, int y, int width, int height) {
    // not implemented
    if (x < 0)
      x = 0;
    if (y < 0)
      y = 0;
    if (x + width > windowWidth)
      width = windowWidth - x;
    if (y + height > windowHeight)
      height = windowHeight - y;
    clipX = x;
    clipY = y;
    clipWidth = width;
    clipHeight = height;
    if (antialiasThisFrame) {
      clipX *= 2;
      clipY *= 2;
      clipWidth *= 2;
      clipHeight *= 2;
    }
  }
  */

  //mostly public drawing methods -- add "public" if you need to

  /* ***************************************************************
   * points
   * ***************************************************************/

  @Override
  public void drawPixel(int x, int y, int z) {
    // measures - render angle
    plotPixelClippedArgb(argbCurrent, x, y, z, width, zbuf, pixel);
  }

  @Override
  public void drawPoints(int count, int[] coordinates, int scale) {
    // for dots only
    if (scale > 1) {
      float s2 = scale * scale * 0.8f;
      for (int i = -scale; i < scale; i++) {
        for (int j = -scale; j < scale; j++) {
          if (i * i + j * j > s2)
            continue;
          plotPoints(count, coordinates, i, j);
          plotPoints(count, coordinates, i, j);
        }
      }
    } else {
      plotPoints(count, coordinates, 0, 0);
    }
  }

  /* ***************************************************************
   * lines and cylinders
   * ***************************************************************/

  @Override
  public void drawDashedLineBits(int run, int rise, P3 pointA, P3 pointB) {
    if (isAntialiased()) {
      run += run;
      rise += rise;
    }
    setScreeni(pointA, sA);
    setScreeni(pointB, sB);
    line3d.plotLineBits(argbCurrent, argbCurrent, sA, sB, run, rise, true);
    if (isAntialiased()) {
      if (Math.abs(pointA.x - pointB.x) < Math.abs(pointA.y - pointB.y)) {
        sA.x += 1;
        sB.x += 1;
        line3d.plotLineBits(argbCurrent, argbCurrent, sA, sB, run, rise, true);        
      } else {
        sA.y += 1;
        sB.y += 1;
        line3d.plotLineBits(argbCurrent, argbCurrent, sA, sB, run, rise, true);
      }
    }
  }

  private void setScreeni(P3 pt, P3i p) {
    p.x = Math.round(pt.x);
    p.y = Math.round(pt.y);
    p.z = Math.round(pt.z);
  }

  @Override
  public void drawLineXYZ(int x1, int y1, int z1, int x2, int y2, int z2) {
    // stars, text
    line3d.plotLineOld(argbCurrent, argbCurrent, x1, y1, z1, x2, y2, z2);
  }

  @Override
  public void drawLine(short colixA, short colixB, int x1, int y1, int z1,
                       int x2, int y2, int z2) {
    // backbone and sticks
    if (!setC(colixA))
      colixA = 0;
    int argbA = argbCurrent;
    if (!setC(colixB))
      colixB = 0;
    if (colixA != 0 || colixB != 0)
      line3d.plotLineOld(argbA, argbCurrent, x1, y1, z1, x2, y2, z2);
  }

  @Override
  public void drawLineBits(short colixA, short colixB, P3 pointA, P3 pointB) {
    // drawQuadBits, drawTriangleBits
    if (!setC(colixA))
      colixA = 0;
    int argbA = argbCurrent;
    if (!setC(colixB))
      colixB = 0;
    if (colixA != 0 || colixB != 0) {
      setScreeni(pointA, sA);
      setScreeni(pointB, sB);
      line3d.plotLineBits(argbA, argbCurrent, sA, sB, 0, 0, false);
    }
  }

  @Override
  public void drawLineAB(P3 pointA, P3 pointB) {
    // draw quadrilateral and hermite
    setScreeni(pointA, sA);
    setScreeni(pointB, sB);
    line3d.plotLineBits(argbCurrent, argbCurrent, sA, sB, 0, 0, false);
  }

  @Override
  public void fillCylinderXYZ(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB,
                              int yB, int zB) {
    //Backbone, Mps, Sticks
    if (diameter > ht3)
      return;
    int screen = 0;
    currentShadeIndex = 0;
    if (!setC(colixB))
      colixB = 0;
    if (wasScreened)
      screen = 2;
    if (!setC(colixA))
      colixA = 0;
    if (wasScreened)
      screen += 1;
    if (colixA == 0 && colixB == 0)
      return;
    cylinder3d.renderOld(colixA, colixB, screen, endcaps, diameter, xA, yA, zA,
        xB, yB, zB);
  }

//  @Override
//  public void fillCylinderScreen(byte endcaps, int diameter, int xA, int yA,
//                                 int zA, int xB, int yB, int zB) {
//    //measures, vectors, polyhedra
//    if (diameter <= ht3)
//      cylinder3d.renderOld(colixCurrent, colixCurrent, 0, endcaps, diameter, xA, yA,
//          zA, xB, yB, zB);
//  }
//
  @Override
  public void fillCylinderScreen3I(byte endcaps, int diameter, P3 screenA,
                                   P3 screenB, P3 pt0f, P3 pt1f, float radius) {
    //nucleic cartoon, draw arrowhead
    // this needs to be old style, not exact for performance in JavaScript.  
    if (diameter <= ht3)
      cylinder3d.renderOld(colixCurrent, colixCurrent, 0, endcaps, diameter,
          (int) screenA.x, (int) screenA.y, (int) screenA.z, (int) screenB.x, (int) screenB.y, (int) screenB.z);
  }

  @Override
  public void fillCylinder(byte endcaps, int diameter, P3i screenA, P3i screenB) {
    // mesh(low-precision)
    if (diameter <= ht3)
      cylinder3d.renderOld(colixCurrent, colixCurrent, 0, endcaps, diameter,
          screenA.x, screenA.y, screenA.z, screenB.x, screenB.y, screenB.z);
  }

  @Override
  public void fillCylinderBits(byte endcaps, int diameter, P3 screenA,
                               P3 screenB) {
    // dipole cross, cartoonRockets, draw line
    if (diameter <= ht3 && screenA.z != 1 && screenB.z != 1) {
      if (diameter == 0 || diameter == 1) {
        setScreeni(screenA, sA);
        setScreeni(screenB, sB);
        line3d.plotLineBits(getColorArgbOrGray(colixCurrent),
            getColorArgbOrGray(colixCurrent), sA, sB, 0, 0, false);
        return;
      }
      // in the case of DRAW, we need precision cylinders, because some are
      // really very flat disks with almost no difference between screenA and screenB.
      cylinder3d.renderBitsFloat(colixCurrent, colixCurrent, 0, endcaps,
          diameter, screenA, screenB);
    }
  }

  @Override
  public void fillCylinderBits2(short colixA, short colixB, byte endcaps,
                                int diameter, P3 screenA, P3 screenB) {
    //Backbone, Mps, Sticks
    if (diameter > ht3)
      return;
    int screen = 0;
    currentShadeIndex = 0;
    if (!setC(colixB))
      colixB = 0;
    if (wasScreened)
      screen = 2;
    if (!setC(colixA))
      colixA = 0;
    if (wasScreened)
      screen += 1;
    if (colixA == 0 && colixB == 0)
      return;
    setScreeni(screenA, sA);
    setScreeni(screenB, sB);
    cylinder3d.renderBits(colixA, colixB, screen, endcaps, diameter, sA, sB);
  }

  @Override
  public void fillConeScreen3f(byte endcap, int screenDiameter, P3 screenBase,
                              P3 screenTip, boolean isBarb) {
    // cartoons, rockets
    if (screenDiameter <= ht3)
      cylinder3d.renderConeOld(colixCurrent, endcap, screenDiameter, screenBase.x,
          screenBase.y, screenBase.z, screenTip.x, screenTip.y, screenTip.z,
          true, isBarb);
  }

  @Override
  public void drawHermite4(int tension, P3 s0, P3 s1, P3 s2, P3 s3) {
    // bioShapeRenderer
    ((HermiteRenderer) hermite3d).renderHermiteRope(false, tension, 0, 0, 0,
        s0, s1, s2, s3);
  }

  @Override
  public void drawHermite7(boolean fill, boolean border, int tension, P3 s0,
                           P3 s1, P3 s2, P3 s3, P3 s4, P3 s5, P3 s6,
                           P3 s7, int aspectRatio, short colixBack) {
    if (colixBack == 0) {
      ((HermiteRenderer) hermite3d).renderHermiteRibbon(fill, border, tension,
          s0, s1, s2, s3, s4, s5, s6, s7, aspectRatio, 0);
      return;
    }
    ((HermiteRenderer) hermite3d).renderHermiteRibbon(fill, border, tension,
        s0, s1, s2, s3, s4, s5, s6, s7, aspectRatio, 1);
    short colix = colixCurrent;
    setC(colixBack);
    ((HermiteRenderer) hermite3d).renderHermiteRibbon(fill, border, tension,
        s0, s1, s2, s3, s4, s5, s6, s7, aspectRatio, -1);
    setC(colix);
  }

  @Override
  public void fillHermite(int tension, int diameterBeg, int diameterMid,
                          int diameterEnd, P3 s0, P3 s1, P3 s2, P3 s3) {
    ((HermiteRenderer) hermite3d).renderHermiteRope(true, tension, diameterBeg,
        diameterMid, diameterEnd, s0, s1, s2, s3);
  }

  @Override
  public void drawTriangle3C(P3i screenA, short colixA, P3i screenB,
                             short colixB, P3i screenC, short colixC, int check) {
    // primary method for mapped Mesh
    if ((check & 1) == 1)
      drawLine(colixA, colixB, screenA.x, screenA.y, screenA.z, screenB.x,
          screenB.y, screenB.z);
    if ((check & 2) == 2)
      drawLine(colixB, colixC, screenB.x, screenB.y, screenB.z, screenC.x,
          screenC.y, screenC.z);
    if ((check & 4) == 4)
      drawLine(colixA, colixC, screenA.x, screenA.y, screenA.z, screenC.x,
          screenC.y, screenC.z);
  }

  @Override
  public void fillTriangleTwoSided(short normix, P3 screenA, P3 screenB,
                                   P3 screenC) {
    // polyhedra
    setColorNoisy(getShadeIndex(normix));
    fillTriangleP3f(screenA, screenB, screenC, false);
  }

  private P3i sA = new P3i(), sB = new P3i(), sC = new P3i();
  
  private void fillTriangleP3f(P3 screenA, P3 screenB, P3 screenC, boolean useGouraud) {
    setScreeni(screenA, sA);
    setScreeni(screenB, sB);
    setScreeni(screenC, sC);
    ((TriangleRenderer) triangle3d).fillTriangle(sA, sB, sC, useGouraud);
  }

  @Override
  public void fillTriangle3f(P3 screenA, P3 screenB, P3 screenC,
                             boolean isSolid) {
    // rocket box, cartoon ribbon
    int i = getShadeIndexP3(screenA, screenB, screenC, isSolid);
    if (i < 0)
      return;
    if (isSolid)
      setColorNoisy(i);
    else
      setColor(shadesCurrent[i]);
    fillTriangleP3f(screenA, screenB, screenC, false);
  }

  @Override
  public void fillTriangle3i(P3 screenA, P3 screenB, P3 screenC, T3 ptA,
                             T3 ptB, T3 ptC, boolean doShade) {
    // cartoon DNA plates; preset color
    if (doShade) {
      V3 v = vectorAB;
      v.set(screenB.x - screenA.x, screenB.y - screenA.y, screenB.z - screenA.z);
      int shadeIndex;
      if (screenC == null) {
        shadeIndex = shader.getShadeIndex(-v.x, -v.y, v.z);
      } else {
        vectorAC.set(screenC.x - screenA.x, screenC.y - screenA.y, screenC.z
            - screenA.z);
        v.cross(v, vectorAC);
        shadeIndex = v.z >= 0 ? shader.getShadeIndex(-v.x, -v.y, v.z) : shader
            .getShadeIndex(v.x, v.y, -v.z);
      }
      if (shadeIndex > Shader.SHADE_INDEX_NOISY_LIMIT)
        shadeIndex = Shader.SHADE_INDEX_NOISY_LIMIT;
      setColorNoisy(shadeIndex);
    }
    fillTriangleP3f(screenA, screenB, screenC, false);
  }

  @Override
  public void fillTriangle3CN(P3i screenA, short colixA, short normixA,
                              P3i screenB, short colixB, short normixB,
                              P3i screenC, short colixC, short normixC) {
    ((TriangleRenderer) triangle3d).fillTriangle(screenA, screenB, screenC,
        checkGouraud(colixA, colixB, colixC, normixA, normixB, normixC));
  }

  @Override
  public void fillTriangle3CNBits(P3 screenA, short colixA, short normixA, P3 screenB,
                                  short colixB, short normixB, P3 screenC, short colixC,
                                  short normixC, boolean twoSided) {
    // mesh, isosurface
    fillTriangleP3f(screenA, screenB, screenC,
        checkGouraud(colixA, colixB, colixC, normixA, normixB, normixC));
  }

  private boolean checkGouraud(short colixA, short colixB, short colixC,
                               short normixA, short normixB, short normixC) {
    if (!isPass2 && normixA == normixB && normixA == normixC
        && colixA == colixB && colixA == colixC) {
      int shadeIndex = getShadeIndex(normixA);
      if (colixA != colixCurrent || currentShadeIndex != shadeIndex) {
        currentShadeIndex = -1;
        setC(colixA);
        setColorNoisy(shadeIndex);
      }
      return false;
    } 
      setTriangleTranslucency(colixA, colixB, colixC);
      ((TriangleRenderer) triangle3d).setGouraud(
          getShades(colixA)[getShadeIndex(normixA)],
          getShades(colixB)[getShadeIndex(normixB)],
          getShades(colixC)[getShadeIndex(normixC)]);
      return true;
    
  }

  private static byte nullShadeIndex = 50;

  public int getShadeIndex(short normix) {
    // from Graphics3D.fillTriangle
    return (normix == ~Normix.NORMIX_NULL || normix == Normix.NORMIX_NULL ? nullShadeIndex
        : normix < 0 ? shadeIndexes2Sided[~normix] : shadeIndexes[normix]);
  }
  private void setTriangleTranslucency(short colixA, short colixB, short colixC) {
    if (isPass2) {
      int maskA = colixA & C.TRANSLUCENT_MASK;
      int maskB = colixB & C.TRANSLUCENT_MASK;
      int maskC = colixC & C.TRANSLUCENT_MASK;
      maskA &= ~C.TRANSPARENT;
      maskB &= ~C.TRANSPARENT;
      maskC &= ~C.TRANSPARENT;
      int mask = roundInt((maskA + maskB + maskC) / 3) & C.TRANSLUCENT_MASK;
      translucencyMask = (mask << C.ALPHA_SHIFT) | 0xFFFFFF;
    }
  }

  /* ***************************************************************
   * quadrilaterals
   * ***************************************************************/

  @Override
  public void fillQuadrilateral(P3 screenA, P3 screenB, P3 screenC, P3 screenD, boolean isSolid) {
    // cartoon ribbon, rocket boxes
    int i = getShadeIndexP3(screenA, screenB, screenC, isSolid);
    if (i < 0)
      return;
    setColorNoisy(i);
    fillTriangleP3f(screenA, screenB, screenC, false);
    // don't do the following; it creates a moire pattern in cartoons
    // setColorNoisy(i);
    fillTriangleP3f(screenA, screenC, screenD, false);
  }

  @Override
  public void drawSurface(MeshSurface meshSurface, short colix) {
    // Export3D only
  }

  @Override
  public void plotPixelClippedP3i(P3i screen) {
    // hermite only; export checks for clipping; overridden in Export3D
    plotPixelClippedArgb(argbCurrent, screen.x, screen.y, screen.z, width,
        zbuf, pixel);
  }

  void plotPixelClippedArgb(int argb, int x, int y, int z, int width,
                            int[] zbuf, Pixelator p) {
    // cylinder3d plotRaster
    if (isClipped3(x, y, z))
      return;
    int offset = y * width + x;
    if (z < zbuf[offset])
      p.addPixel(offset, z, argb);
  }

  void plotPixelUnclipped(int argb, int x, int y, int z, int width, int[] zbuf,
                          Pixelator p) {
    // circle (halo)
    int offset = y * width + x;
    if (z < zbuf[offset])
      p.addPixel(offset, z, argb);
  }

  @Override
  public void plotImagePixel(int argb, int x, int y, int z, byte shade,
                             int bgargb, int width, int height, int[] zbuf,
                             Object p, int transpLog) {
    // drawString via text3d.plotClipped; overridden in Export
    if (x < 0 || x >= width || y < 0 || y >= height)
      return;
    ((Pixelator)p).addImagePixel(shade, transpLog, y * width + x, z, argb, bgargb);
  }

  void plotPixelsClippedRaster(int count, int x, int y, int zAtLeft,
                               int zPastRight, Rgb16 rgb16Left, Rgb16 rgb16Right) {
    // cylinder3d.renderFlatEndcap, triangle3d.fillRaster
    int depth, slab;
    if (count <= 0 || y < 0 || y >= height || x >= width
        || (zAtLeft < (slab = this.slab) && zPastRight < slab)
        || (zAtLeft > (depth = this.depth) && zPastRight > depth))
      return;
    int[] zb = zbuf;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled = roundInt(((dz << 10) + (dz >= 0 ? roundFactor
        : -roundFactor)) / count);
    if (x < 0) {
      x = -x;
      zScaled += zIncrementScaled * x;
      count -= x;
      if (count <= 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    Pixelator p = pixel;
    if (rgb16Left == null) {
      int adn = argbNoisyDn;
      int aup = argbNoisyUp;
      int ac = argbCurrent;
      while (--count >= 0) {
        int z = zScaled >> 10;
        if (z >= slab && z <= depth && z < zb[offsetPbuf]) {
          seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
          int bits = (seed >> 16) & 0x07;
          p.addPixel(offsetPbuf, z, bits == 0 ? adn : (bits == 1 ? aup : ac));
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
      }
    } else {
      int rScaled = rgb16Left.r << 8;
      int rIncrement = ((rgb16Right.r - rgb16Left.r) << 8) / count;
      int gScaled = rgb16Left.g;
      int gIncrement = (rgb16Right.g - gScaled) / count;
      int bScaled = rgb16Left.b;
      int bIncrement = (rgb16Right.b - bScaled) / count;
      while (--count >= 0) {
        int z = zScaled >> 10;
        if (z >= slab && z <= depth && z < zb[offsetPbuf])
          p.addPixel(offsetPbuf, z, 0xFF000000 | (rScaled & 0xFF0000)
              | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
        ++offsetPbuf;
        zScaled += zIncrementScaled;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  void plotPixelsUnclippedRaster(int count, int x, int y, int zAtLeft,
                                 int zPastRight, Rgb16 rgb16Left,
                                 Rgb16 rgb16Right) {
    // for isosurface Triangle3D.fillRaster
    if (count <= 0)
      return;
    int seed = ((x << 16) + (y << 1) ^ 0x33333333) & 0x7FFFFFFF;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled = roundInt(((dz << 10) + (dz >= 0 ? roundFactor
        : -roundFactor)) / count);
    int offsetPbuf = y * width + x;
    int[] zb = zbuf;
    Pixelator p = pixel;
    if (rgb16Left == null) {
      int adn = argbNoisyDn;
      int aup = argbNoisyUp;
      int ac = argbCurrent;
      while (--count >= 0) {
        int z = zScaled >> 10;
        if (z < zb[offsetPbuf]) {
          seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
          int bits = (seed >> 16) & 0x07;
          p.addPixel(offsetPbuf, z, bits == 0 ? adn : (bits == 1 ? aup : ac));
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
      }
    } else {
      int rScaled = rgb16Left.r << 8;
      int rIncrement = roundInt(((rgb16Right.r - rgb16Left.r) << 8) / count);
      int gScaled = rgb16Left.g;
      int gIncrement = roundInt((rgb16Right.g - gScaled) / count);
      int bScaled = rgb16Left.b;
      int bIncrement = roundInt((rgb16Right.b - bScaled) / count);
      while (--count >= 0) {
        int z = zScaled >> 10;
        if (z < zb[offsetPbuf])
          p.addPixel(offsetPbuf, z, 0xFF000000 | (rScaled & 0xFF0000)
              | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
        ++offsetPbuf;
        zScaled += zIncrementScaled;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  
  void plotPixelsClippedRasterBits(int count, int x, int y, int zAtLeft,
                               int zPastRight, Rgb16 rgb16Left, Rgb16 rgb16Right, float a, float b) {
    // cylinder3d.renderFlatEndcap, triangle3d.fillRaster
    int depth, slab;

    if (count <= 0 || y < 0 || y >= height || x >= width
        || (zAtLeft < (slab = this.slab) && zPastRight < slab)
        || (zAtLeft > (depth = this.depth) && zPastRight > depth))
      return;
    int[] zb = zbuf;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
    if (x < 0) {
      x = -x;
      count -= x;
      if (count <= 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    Pixelator p = pixel;
    if (rgb16Left == null) {
      int adn = argbNoisyDn;
      int aup = argbNoisyUp;
      int ac = argbCurrent;
  
      while (--count >= 0) {
        int zCurrent = line3d.getZCurrent(a, b, x++);
        if (zCurrent >= slab && zCurrent <= depth && zCurrent < zb[offsetPbuf]) {
          seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
          int bits = (seed >> 16) & 0x07;
            
          p.addPixel(offsetPbuf, zCurrent, bits <2 ? adn :  bits < 6 ? aup : ac);
        }
        ++offsetPbuf;
      }
    } else {
      int rScaled = rgb16Left.r << 8;
      int rIncrement = ((rgb16Right.r - rgb16Left.r) << 8) / count;
      int gScaled = rgb16Left.g;
      int gIncrement = (rgb16Right.g - gScaled) / count;
      int bScaled = rgb16Left.b;
      int bIncrement = (rgb16Right.b - bScaled) / count;
      while (--count >= 0) {
        int zCurrent = line3d.getZCurrent(a, b, x++);
        if (zCurrent >= slab && zCurrent <= depth && zCurrent < zb[offsetPbuf])
          p.addPixel(offsetPbuf, zCurrent, 0xFF000000 | (rScaled & 0xFF0000)
              | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
        ++offsetPbuf;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  void plotPixelsUnclippedRasterBits(int count, int x, int y, 
                                 Rgb16 rgb16Left,
                                 Rgb16 rgb16Right, float a, float b) {
    // for isosurface Triangle3D.fillRaster
    
    if (count <= 0)
      return;
    int seed = ((x << 16) + (y << 1) ^ 0x33333333) & 0x7FFFFFFF;
    // scale the z coordinates;
    int offsetPbuf = y * width + x;
    int[] zb = zbuf;
    Pixelator p = pixel;
    if (rgb16Left == null) {
      int adn = argbNoisyDn;
      int aup = argbNoisyUp;
      int ac = argbCurrent;
      while (--count >= 0) {
        int zCurrent = line3d.getZCurrent(a, b, x++);
        if (zCurrent < zb[offsetPbuf]) {
          seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
          int bits = (seed >> 16) & 0x07;
          int c =(bits == 0 ? adn : bits == 1  ? aup : ac);
          //System.out.println(bits + " " + adn + " " + aup + " " + ac + " " + Integer.toHexString(c));
          p.addPixel(offsetPbuf, zCurrent, c);
        }
        ++offsetPbuf;
      }
    } else {
      int rScaled = rgb16Left.r << 8;
      int rIncrement = roundInt(((rgb16Right.r - rgb16Left.r) << 8) / count);
      int gScaled = rgb16Left.g;
      int gIncrement = roundInt((rgb16Right.g - gScaled) / count);
      int bScaled = rgb16Left.b;
      int bIncrement = roundInt((rgb16Right.b - bScaled) / count);
      while (--count >= 0) {
        int zCurrent = line3d.getZCurrent(a, b, x++);
        if (zCurrent < zb[offsetPbuf])
          p.addPixel(offsetPbuf, zCurrent, 0xFF000000 | (rScaled & 0xFF0000)
              | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
        ++offsetPbuf;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  
  
  void plotPixelsUnclippedCount(int c, int count, int x, int y, int z,
                                int width, int[] zbuf, Pixelator p) {

    // for Cirle3D.plot8Filled and fillRect

    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      if (z < zbuf[offsetPbuf])
        p.addPixel(offsetPbuf, z, c);
      ++offsetPbuf;
    }
  }

  private void plotPoints(int count, int[] coordinates, int xOffset, int yOffset) {
    Pixelator p = pixel;
    int c = argbCurrent;
    int[] zb = zbuf;
    int w = width;
    boolean antialias = antialiasThisFrame;
    for (int i = count * 3; i > 0;) {
      int z = coordinates[--i];
      int y = coordinates[--i] + yOffset;
      int x = coordinates[--i] + xOffset;
      if (isClipped3(x, y, z))
        continue;
      int offset = y * w + x++;
      if (z < zb[offset])
        p.addPixel(offset, z, c);
      if (antialias) {
        offset = y * w + x;
        if (!isClipped3(x, y, z) && z < zb[offset])
          p.addPixel(offset, z, c);
        offset = (++y) * w + x;
        if (!isClipped3(x, y, z) && z < zb[offset])
          p.addPixel(offset, z, c);
        offset = y * w + (--x);
        if (!isClipped3(x, y, z) && z < zb[offset])
          p.addPixel(offset, z, c);
      }

    }
  }

  private final V3 vectorAB = new V3();
  private final V3 vectorAC = new V3();
  private final V3 vectorNormal = new V3();

  void setColorNoisy(int shadeIndex) {
    currentShadeIndex = shadeIndex;
    argbCurrent = shadesCurrent[shadeIndex];
    argbNoisyUp = shadesCurrent[shadeIndex < Shader.SHADE_INDEX_LAST ? shadeIndex + 1
        : Shader.SHADE_INDEX_LAST];
    argbNoisyDn = shadesCurrent[shadeIndex > 0 ? shadeIndex - 1 : 0];
  }

  private int getShadeIndexP3(P3 screenA, P3 screenB, P3 screenC, boolean isSolid) {
    // for fillTriangle and fillQuad.
    vectorAB.sub2(screenB, screenA);
    vectorAC.sub2(screenC, screenA);
    V3 v = vectorNormal;
    v.cross(vectorAB, vectorAC);
    int i = (v.z < 0 ? shader
        .getShadeIndex(v.x, v.y, -v.z)  : isSolid ? -1 : shader.getShadeIndex(-v.x, -v.y, v.z));

    return i;
  }

  //////////////////////////////////////////////////////////

  @Override
  public void renderBackground(JmolRendererInterface jmolRenderer) {
    if (backgroundImage != null)
      plotImage(Integer.MIN_VALUE, 0, Integer.MIN_VALUE, backgroundImage,
          jmolRenderer, (short) 0, 0, 0);
  }

  @Override
  public void drawAtom(Atom atom, float radius) {
    // radius is used for Cartesian Export only
    fillSphereXYZ(atom.sD, atom.sX, atom.sY, atom.sZ);
  }

  // implemented only for Export3D:

  @Override
  public int getExportType() {
    return EXPORT_NOT;
  }

  @Override
  public String getExportName() {
    return null;
  }

  public boolean canDoTriangles() {
    return true;
  }

  public boolean isCartesianExport() {
    return false;
  }

  @Override
  public JmolRendererInterface initializeExporter(Viewer vwr,
                                                  double privateKey, GData g3d,
                                                  Map<String, Object> params) {
    return null;
  }

  @Override
  public String finalizeOutput() {
    return null;
  }

  @Override
  public void drawBond(P3 atomA, P3 atomB, short colixA, short colixB,
                       byte endcaps, short mad, int bondOrder) {
  }

  @Override
  public boolean drawEllipse(P3 ptAtom, P3 ptX, P3 ptY, boolean fillArc,
                             boolean wireframeOnly) {
    return false;
  }

  public double getPrivateKey() {
    // exporter only
    return 0;
  }

  @Override
  public void clearFontCache() {
    TextRenderer.clearFontCache();
  }

  // Normix/Shading related methods

  // only these three instance variables depend upon current orientation:

  private final byte[] shadeIndexes = new byte[normixCount];
  private final byte[] shadeIndexes2Sided = new byte[normixCount];
  public int pass2Flag01;

  public void setRotationMatrix(M3 rotationMatrix) {
    V3[] vertexVectors = Normix.getVertexVectors();
    for (int i = normixCount; --i >= 0;) {
      V3 tv = transformedVectors[i];
      rotationMatrix.rotate2(vertexVectors[i], tv);
      shadeIndexes[i] = shader.getShadeB(tv.x, -tv.y, tv.z);
      shadeIndexes2Sided[i] = (tv.z >= 0 ? shadeIndexes[i] : shader.getShadeB(
          -tv.x, tv.y, -tv.z));
    }
  }


  /////////// special rendering ///////////

  /**
   * @param minMax
   * @param screenWidth
   * @param screenHeight
   * @param navOffset
   * @param navDepth
   */
  @Override
  public void renderCrossHairs(int[] minMax, int screenWidth, int screenHeight,
                               P3 navOffset, float navDepth) {
    // this is the square and crosshairs for the navigator
    boolean antialiased = isAntialiased();
    setC(navDepth < 0 ? C.RED : navDepth > 100 ? C.GREEN : C.GOLD);
    int x = Math.max(Math.min(width, Math.round(navOffset.x)), 0);
    int y = Math.max(Math.min(height, Math.round(navOffset.y)), 0);
    int z = Math.round(navOffset.z) + 1;
    // TODO: fix for antialiasDisplay
    int off = (antialiased ? 8 : 4);
    int h = (antialiased ? 20 : 10);
    int w = (antialiased ? 2 : 1);
    drawRect(x - off, y, z, 0, h, w);
    drawRect(x, y - off, z, 0, w, h);
    drawRect(x - off, y - off, z, 0, h, h);
    off = h;
    h = h >> 1;
    setC(minMax[1] < navOffset.x ? C.YELLOW : C.GREEN);
    drawRect(x - off, y, z, 0, h, w);
    setC(minMax[0] > navOffset.x ? C.YELLOW : C.GREEN);
    drawRect(x + h, y, z, 0, h, w);
    setC(minMax[3] < navOffset.y ? C.YELLOW : C.GREEN);
    drawRect(x, y - off, z, 0, w, h);
    setC(minMax[2] > navOffset.y ? C.YELLOW : C.GREEN);
    drawRect(x, y + h, z, 0, w, h);
  }

  @Override
  public boolean initializeOutput(Viewer vwr, double privateKey,
                                  Map<String, Object> params) {
    // N/A
    return false;
  }

}
