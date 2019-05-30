package org.jmol.api;


import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;

import org.jmol.awtjs.swing.Font;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.T3;

import org.jmol.viewer.Viewer;

public interface JmolRendererInterface extends JmolGraphicsInterface {

  // exporting  

  public abstract void addRenderer(int tok);

  public abstract boolean checkTranslucent(boolean isAlphaTranslucent);

  public abstract void drawAtom(Atom atom, float radius);

  public abstract void drawBond(P3 atomA, P3 atomB, short colixA,
                                short colixB, byte endcaps, short mad, int bondOrder);

  public abstract void drawDashedLineBits(int run, int rise, P3 screenA,
                                      P3 screenB);

  public abstract boolean drawEllipse(P3 ptAtom, P3 ptX, P3 ptY,
                                      boolean fillArc, boolean wireframeOnly);

  /**
   * draws a ring and filled circle (halos, draw CIRCLE, draw handles)
   * 
   * @param colixRing
   * @param colixFill
   * @param diameter
   * @param x
   *        center x
   * @param y
   *        center y
   * @param z
   *        center z
   */
  public abstract void drawFilledCircle(short colixRing, short colixFill,
                                        int diameter, int x, int y, int z);

  public abstract void drawHermite4(int tension, P3 s0, P3 s1,
                                   P3 s2, P3 s3);

  public abstract void drawHermite7(boolean fill, boolean border, int tension,
                                   P3 s0, P3 s1, P3 s2,
                                   P3 s3, P3 s4, P3 s5,
                                   P3 s6, P3 s7, int aspectRatio, short colixBack);

  public abstract void drawImage(Object image, int x, int y, int z, int zslab,
                                 short bgcolix, int width, int height);

  public abstract void drawLine(short colixA, short colixB, int x1, int y1,
                                int z1, int x2, int y2, int z2);

  public abstract void drawLineAB(P3 pointA, P3 pointB);

  public abstract void drawLineXYZ(int x1, int y1, int z1, int x2, int y2, int z2);

  public abstract void drawPixel(int x, int y, int z);

  public abstract void drawPoints(int count, int[] coordinates, int scale);

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
  public abstract void drawRect(int x, int y, int z, int zSlab, int rWidth,
                                int rHeight);

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
   * @param bgColix TODO
   */

  public abstract void drawString(String str, Font font3d, int xBaseline,
                                  int yBaseline, int z, int zSlab, short bgColix);

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
   * @param bgColix TODO
   */

  public abstract void drawStringNoSlab(String str, Font font3d,
                                        int xBaseline, int yBaseline, int z, short bgColix);

  public abstract void drawSurface(MeshSurface meshSurface, short colix);

  public abstract void drawTriangle3C(P3i screenA, short colixA,
                                    P3i screenB, short colixB,
                                    P3i screenC, short colixC, int check);

  public abstract void fillConeScreen3f(byte endcap, int screenDiameter,
                                     P3 screenBase, P3 screenTip, boolean isBarb);

  public abstract void fillCylinder(byte endcaps, int diameter,
                                    P3i screenA, P3i screenB);

  public abstract void fillCylinderBits(byte endcaps, int diameter,
                                        P3 screenA, P3 screenB);

  public abstract void fillCylinderScreen3I(byte endcaps, 
                                            int diameter, P3 s0f, P3 s1f, 
                                          P3 pt0f, P3 pt1f, float radius);

  public abstract void fillCylinderBits2(short colixA, short colixB, byte endcaps,
                                       int diameter, P3 screenA, P3 screenB);

  public abstract void fillCylinderXYZ(short colixA, short colixB, byte endcaps,
                                    int diameter, int xA, int yA, int zA,
                                    int xB, int yB, int zB);

  public abstract void fillEllipsoid(P3 center, P3[] points, int x,
                                     int y, int z, int diameter,
                                     M3 mToEllipsoidal, double[] coef,
                                     M4 mDeriv, int selectedOctant,
                                     P3[] octantPoints);

  public abstract void fillHermite(int tension, int diameterBeg,
                                   int diameterMid, int diameterEnd,
                                   P3 s0, P3 s1, P3 s2,
                                   P3 s3);

  public abstract void fillQuadrilateral(P3 screenA, P3 screenB,
                                         P3 screenC, P3 screenD, boolean isSolid);

  /**
   * fills background rectangle for label
   *<p>
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
  public abstract void fillTextRect(int x, int y, int z, int zSlab, int widthFill,
                                int heightFill);

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param center
   *        a javax.vecmath.Point3f ... floats are casted to ints
   */
  public abstract void fillSphereBits(int diameter, P3 center);

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param center
   *        javax.vecmath.Point3i defining the center
   */

  public abstract void fillSphereI(int diameter, P3i center);

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
  public abstract void fillSphereXYZ(int diameter, int x, int y, int z);

  public abstract void fillTriangle3CN(P3i screenA, short colixA,
                                    short normixA, P3i screenB,
                                    short colixB, short normixB,
                                    P3i screenC, short colixC, short normixC);

  public abstract void fillTriangle3f(P3 screenA, P3 screenB,
                                    P3 screenC, boolean setNoisy);

  public abstract void fillTriangle3i(P3 screenA, P3 screenB,
                                    P3 screenC, T3 ptA, T3 ptB, T3 ptC, boolean doShade);

  public abstract void fillTriangleTwoSided(short normix, P3 a, P3 b, P3 c);

  public abstract String finalizeOutput();

  public abstract String getExportName();

  public abstract boolean isWebGL();

  public abstract int getExportType();

  public abstract boolean haveTranslucentObjects();

  public abstract Object initializeExporter(Viewer vwr,
                                             double privateKey, GData gdata,
                                             Map<String, Object> params);

  public abstract boolean initializeOutput(Viewer vwr,
                                        double privateKey,
                                        Map<String, Object> params);

  public abstract void plotImagePixel(int argb, int x, int y, int z, byte shade, int bgargb, int width, int  height, int[] zbuf, Object pixel, int transpLog);

  public abstract void plotPixelClippedP3i(P3i a);

  public abstract void renderBackground(JmolRendererInterface jre);

  public abstract void renderCrossHairs(int[] minMax, int screenWidth,
                                        int screenHeight,
                                        P3 navigationOffset,
                                        float navigationDepthPercent);

  /**
   * sets current color from colix color index
   * 
   * @param colix
   *        the color index
   * @return true or false if this is the right pass
   */
  public abstract boolean setC(short colix);

  public abstract void volumeRender(boolean TF);

  public abstract void volumeRender4(int diam, int x, int y, int z);

  public abstract void fillTriangle3CNBits(P3 pA, short colixA, short nA,
                                           P3 pB, short colixB, short nB,
                                           P3 pC, short colixC, short nC, boolean twoSided);

  public abstract void drawLineBits(short colixA, short colixB, P3 pointA, P3 pointB);


}
