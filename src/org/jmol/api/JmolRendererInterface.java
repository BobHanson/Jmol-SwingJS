package org.jmol.api;


import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.util.Font;
import org.jmol.util.GData;
import org.jmol.util.MeshSurface;

import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.T3d;

import org.jmol.viewer.Viewer;

public interface JmolRendererInterface extends JmolGraphicsInterface {

  // exporting  

  public abstract void addRenderer(int tok);

  public abstract boolean checkTranslucent(boolean isAlphaTranslucent);

  public abstract void drawAtom(Atom atom, double radius);

  public abstract void drawBond(P3d atomA, P3d atomB, short colixA,
                                short colixB, byte endcaps, short mad, int bondOrder);

  public abstract void drawDashedLineBits(int run, int rise, P3d screenA,
                                      P3d screenB);

  public abstract boolean drawEllipse(P3d ptAtom, P3d ptX, P3d ptY,
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

  public abstract void drawHermite4(int tension, P3d s0, P3d s1,
                                   P3d s2, P3d s3);

  public abstract void drawHermite7(boolean fill, boolean border, int tension,
                                   P3d s0, P3d s1, P3d s2,
                                   P3d s3, P3d s4, P3d s5,
                                   P3d s6, P3d s7, int aspectRatio, short colixBack);

  public abstract void drawImage(Object image, int x, int y, int z, int zslab,
                                 short bgcolix, int width, int height);

  public abstract void drawLine(short colixA, short colixB, int x1, int y1,
                                int z1, int x2, int y2, int z2);

  public abstract void drawLineAB(P3d pointA, P3d pointB);

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
                                     P3d screenBase, P3d screenTip, boolean isBarb);

  public abstract void fillCylinder(byte endcaps, int diameter,
                                    P3i screenA, P3i screenB);

  public abstract void fillCylinderBits(byte endcaps, int diameter,
                                        P3d screenA, P3d screenB);

  public abstract void fillCylinderScreen3I(byte endcaps, 
                                            int diameter, P3d s0f, P3d s1f, 
                                          P3d pt0f, P3d pt1f, double radius);

  public abstract void fillCylinderBits2(short colixA, short colixB, byte endcaps,
                                       int diameter, P3d screenA, P3d screenB);

  public abstract void fillCylinderXYZ(short colixA, short colixB, byte endcaps,
                                    int diameter, int xA, int yA, int zA,
                                    int xB, int yB, int zB);

  public abstract void fillEllipsoid(P3d center, P3d[] points, int x,
                                     int y, int z, int diameter,
                                     M3d mToEllipsoidal, double[] coef,
                                     M4d mDeriv, int selectedOctant,
                                     P3d[] octantPoints);

  public abstract void fillHermite(int tension, int diameterBeg,
                                   int diameterMid, int diameterEnd,
                                   P3d s0, P3d s1, P3d s2,
                                   P3d s3);

  public abstract void fillQuadrilateral(P3d screenA, P3d screenB,
                                         P3d screenC, P3d screenD, boolean isSolid);

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
  public abstract void fillSphereBits(int diameter, P3d center);

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

  public abstract void fillTriangle3f(P3d screenA, P3d screenB,
                                    P3d screenC, boolean setNoisy);

  public abstract void fillTriangle3i(P3d screenA, P3d screenB,
                                    P3d screenC, T3d ptA, T3d ptB, T3d ptC, boolean doShade);

  public abstract void fillTriangleTwoSided(short normix, P3d a, P3d b, P3d c);

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
                                        P3d navigationOffset,
                                        double navigationDepthPercent);

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

  public abstract void fillTriangle3CNBits(P3d pA, short colixA, short nA,
                                           P3d pB, short colixB, short nB,
                                           P3d pC, short colixC, short nC, boolean twoSided);

  public abstract void drawLineBits(short colixA, short colixB, P3d pointA, P3d pointB);


}
