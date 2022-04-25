package org.jmol.util;


import javajs.util.AU;
import javajs.util.M3d;
import javajs.util.P3d;
import javajs.util.P3i;
import javajs.util.T3d;
import javajs.util.V3d;


import org.jmol.api.GenericPlatform;
import org.jmol.api.JmolGraphicsInterface;
import org.jmol.api.JmolRendererInterface;
import org.jmol.c.STER;
import org.jmol.viewer.Viewer;

public class GData implements JmolGraphicsInterface {

  public GenericPlatform apiPlatform;
  public boolean translucentCoverOnly;
  public boolean currentlyRendering;
  public boolean antialiasEnabled;
  
  protected int windowWidth, windowHeight;
  protected int displayMinX, displayMaxX, displayMinY, displayMaxY;
  protected int displayMinX2, displayMaxX2, displayMinY2, displayMaxY2;
  protected boolean antialiasThisFrame;

  protected boolean inGreyscaleMode;

  protected short[] changeableColixMap = new short[16];
  
  protected Object backgroundImage;
  
  protected int newWindowWidth, newWindowHeight;
  protected boolean newAntialiasing;

  public int bgcolor;  
  public int xLast, yLast;
  public int slab, depth;
  public int width, height;
  public int ambientOcclusion;

  protected short colixCurrent;
  public int argbCurrent;
  protected int ht3; // ht * 3, for cylinders
  public boolean isPass2;
  protected int textY;
  
  public int bufferSize;

  public Shader shader;

  protected Viewer vwr;

  public final static byte ENDCAPS_NONE = 0;
  public final static byte ENDCAPS_HIDDEN = 1; // no cap, but interior
  public final static byte ENDCAPS_FLAT = 2;
  public final static byte ENDCAPS_SPHERICAL = 3;
  public final static byte ENDCAPS_OPEN_TO_SPHERICAL = 4;
  public final static byte ENDCAPS_FLAT_TO_SPHERICAL = 5;
  
  /**
   * It is possible to instantiate this class with no Graphics3D. 
   * This will happen in the case of WebGL. 
   */
  public GData() {
    shader = new Shader();
  }
  
  public void initialize(Viewer vwr, GenericPlatform apiPlatform) {
    this.vwr = vwr;
    this.apiPlatform = apiPlatform;
  }

  /**
   * clipping from the front and the back
   *<p>
   * the plane is defined as a percentage from the back of the image to the
   * front
   *<p>
   * for depth values:
   * <ul>
   * <li>0 means 100% is shown
   * <li>25 means the back 25% is <i>not</i> shown
   * <li>50 means the back half is <i>not</i> shown
   * <li>100 means that nothing is shown
   * </ul>
   *<p>
   * 
   * @param depthValue
   *        rear clipping percentage [0,100]
   */
  public void setDepth(int depthValue) {
    depth = depthValue < 0 ? 0 : depthValue;
  }

  /**
   * clipping from the front and the back
   *<p>
   * the plane is defined as a percentage from the back of the image to the
   * front
   *<p>
   * For slab values:
   * <ul>
   * <li>100 means 100% is shown
   * <li>75 means the back 75% is shown
   * <li>50 means the back half is shown
   * <li>0 means that nothing is shown
   * </ul>
   *<p>
   * 
   * @param slabValue
   *        front clipping percentage [0,100]
   */
  @Override
  public void setSlab(int slabValue) {
    slab = Math.max(0, slabValue);
  }

  /**
   * @param zSlab
   *        for zShade
   * @param zDepth
   *        for zShade
   * @param zPower 
   */
  @Override
  public void setSlabAndZShade(int slab, int depth,int zSlab, int zDepth, int zPower) {
    setSlab(slab);
    setDepth(depth);
  }


  protected Object graphicsForMetrics;

  public final static int EXPORT_RAYTRACER = 2;

  public final static int EXPORT_CARTESIAN = 1;

  public final static int EXPORT_NOT = 0;

  public void setAmbientOcclusion(int value) {
    ambientOcclusion = value;
  }


  /**
   * is full scene / oversampling antialiasing in effect
   * 
   * @return the answer
   */
  @Override
  public boolean isAntialiased() {
    return antialiasThisFrame;
  }

  public short getChangeableColix(int id, int argb) {
    if (id >= changeableColixMap.length)
      changeableColixMap = AU.arrayCopyShort(changeableColixMap, id + 16);
    if (changeableColixMap[id] == 0)
      changeableColixMap[id] = C.getColix(argb);
    return (short) (id | C.CHANGEABLE_MASK);
  }

  public void changeColixArgb(int id, int argb) {
    if (id < changeableColixMap.length && changeableColixMap[id] != 0)
      changeableColixMap[id] = C.getColix(argb);
  }

  public int getColorArgbOrGray(short colix) {
    if (colix < 0)
      colix = changeableColixMap[colix & C.UNMASK_CHANGEABLE_TRANSLUCENT];
    return (inGreyscaleMode ? C.getArgbGreyscale(colix) : C.getArgb(colix));
  }

  public int[] getShades(short colix) {
    if (colix < 0)
      colix = changeableColixMap[colix & C.UNMASK_CHANGEABLE_TRANSLUCENT];
    return (inGreyscaleMode ? shader.getShadesG(colix) : shader
        .getShades(colix));
  }

  /**
   * controls greyscale rendering
   * 
   * @param greyscaleMode
   *        Flag for greyscale rendering
   */
  public void setGreyscaleMode(boolean greyscaleMode) {
    this.inGreyscaleMode = greyscaleMode;
  }

  public int getSpecularPower() {
    return shader.specularPower;
  }

  /**
   * fractional distance to white for specular dot
   * 
   * @param val
   */
  public synchronized void setSpecularPower(int val) {
    if (val < 0) {
      setSpecularExponent(-val);
      return;
    }
    if (shader.specularPower == val)
      return;
    shader.specularPower = val;
    shader.intenseFraction = val / 100f;
    shader.flushCaches();
  }

  public int getSpecularPercent() {
    return shader.specularPercent;
  }

  /**
   * sf in I = df * (N dot L) + sf * (R dot V)^p not a percent of anything,
   * really
   * 
   * @param val
   */
  public synchronized void setSpecularPercent(int val) {
    if (shader.specularPercent == val)
      return;
    shader.specularPercent = val;
    shader.specularFactor = val / 100f;
    shader.flushCaches();
  }

  public int getSpecularExponent() {
    return shader.specularExponent;
  }

  /**
   * log_2(p) in I = df * (N dot L) + sf * (R dot V)^p for faster calculation of
   * shades
   * 
   * @param val
   */
  public synchronized void setSpecularExponent(int val) {
    if (shader.specularExponent == val)
      return;
    shader.specularExponent = val;
    shader.phongExponent = (int) Math.pow(2, val);
    shader.usePhongExponent = false;
    shader.flushCaches();
  }

  public int getPhongExponent() {
    return shader.phongExponent;
  }

  /**
   * p in I = df * (N dot L) + sf * (R dot V)^p
   * 
   * @param val
   */
  public synchronized void setPhongExponent(int val) {
    if (shader.phongExponent == val && shader.usePhongExponent)
      return;
    shader.phongExponent = val;
    double x = (double) (Math.log(val) / Math.log(2));
    shader.usePhongExponent = (x != (int) x);
    if (!shader.usePhongExponent)
      shader.specularExponent = (int) x;
    shader.flushCaches();
  }

  public int getDiffusePercent() {
    return shader.diffusePercent;
  }

  /**
   * df in I = df * (N dot L) + sf * (R dot V)^p
   * 
   * @param val
   */
  public synchronized void setDiffusePercent(int val) {
    if (shader.diffusePercent == val)
      return;
    shader.diffusePercent = val;
    shader.diffuseFactor = val / 100f;
    shader.flushCaches();
  }

  public int getAmbientPercent() {
    return shader.ambientPercent;
  }

  /**
   * fractional distance from black for ambient color
   * 
   * @param val
   */
  public synchronized void setAmbientPercent(int val) {
    if (shader.ambientPercent == val)
      return;
    shader.ambientPercent = val;
    shader.ambientFraction = val / 100f;
    shader.flushCaches();
  }

  public boolean getSpecular() {
    return shader.specularOn;
  }

  public synchronized void setSpecular(boolean val) {
    if (shader.specularOn == val)
      return;
    shader.specularOn = val;
    shader.flushCaches();
  }

  public void setCel(boolean val) {
    shader.setCel(val, shader.celPower, bgcolor);
  }

  public boolean getCel() {
    return shader.celOn;
  }

  public int getCelPower() {
    return shader.celPower;
  }

  public void setCelPower(int celPower) {
    shader.setCel(shader.celOn || shader.celPower == 0, celPower, bgcolor);
  }

  public V3d getLightSource() {
    return shader.lightSource;
  }

  public boolean isClipped3(int x, int y, int z) {
    // this is the one that could be augmented with slabPlane
    return (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth);
  }

  public boolean isClipped(int x, int y) {
    return (x < 0 || x >= width || y < 0 || y >= height);
  }

  @Override
  public boolean isInDisplayRange(int x, int y) {
    return (x >= displayMinX && x < displayMaxX && y >= displayMinY && y < displayMaxY);
  }

  @Override
  public boolean isClippedXY(int diameter, int x, int y) {
    int r = (diameter + 1) >> 1;
    return (x < -r || x >= width + r || y < -r || y >= height + r);
  }

  public boolean isClippedZ(int z) {
    return (z != Integer.MIN_VALUE && (z < slab || z > depth));
  }

  final public static int yGT = 1;
  final public static int yLT = 2;
  final public static int xGT = 4;
  final public static int xLT = 8;
  final public static int zGT = 16;
  final public static int zLT = 32;
  final public static int HUGE = -1;

  public int clipCode3(int x, int y, int z) {
    int code = 0;
    if (x < 0)
      code |= (x < displayMinX2 ? HUGE : xLT);
    else if (x >= width)
      code |=  (x > displayMaxX2 ? HUGE : xGT);
    if (y < 0)
      code |=  (y < displayMinY2 ? HUGE : yLT);
    else if (y >= height)
      code |=  (y > displayMaxY2 ? HUGE : yGT);
    if (z < slab)
      code |= zLT;
    else if (z > depth) // note that this is .GT., not .GE.
      code |= zGT;

    return code;
  }

  public int clipCode(int z) {
    int code = 0;
    if (z < slab)
      code |= zLT;
    else if (z > depth) // note that this is .GT., not .GE.
      code |= zGT;
    return code;
  }

  /* ***************************************************************
   * fontID stuff
   * a fontID is a byte that contains the size + the face + the style
   * ***************************************************************/

  public Font getFont3D(double fontSize) {
    return Font.createFont3D(Font.FONT_FACE_SANS, Font.FONT_STYLE_PLAIN,
        fontSize, fontSize, apiPlatform, graphicsForMetrics);
  }

  public Font getFont3DFS(String fontFace, double fontSize) {
    return Font.createFont3D(Font.getFontFaceID(fontFace),
        Font.FONT_STYLE_PLAIN, fontSize, fontSize, apiPlatform, graphicsForMetrics);
  }

  public int getFontFidFS(String fontFace, double fontSize) {
    return getFont3DFSS(fontFace, "Bold", fontSize).fid;
  }

  public Font getFont3DFSS(String fontFace, String fontStyle, double fontSize) {
    int iStyle = Font.getFontStyleID(fontStyle);
    if (iStyle < 0)
      iStyle = 0;
    return Font.createFont3D(Font.getFontFaceID(fontFace), iStyle, fontSize,
        fontSize, apiPlatform, graphicsForMetrics);
  }

  public Font getFont3DScaled(Font font, double scale) {
    // TODO: problem here is that we are assigning a bold font, then not DEassigning it
    double newScale = font.fontSizeNominal * scale;
    return (newScale == font.fontSize ? font : Font.createFont3D(
        font.idFontFace, font.idFontStyle, newScale, font.fontSizeNominal, apiPlatform, graphicsForMetrics));
  }

  public int getFontFidI(double fontSize) {
    return getFont3D(fontSize).fid;
  }

  protected Font currentFont;

  public Font getFont3DCurrent() {
    return currentFont;
  }

  /**
   * @param font3d  
   */
  public void setFont(Font font3d) {
    // see Graphics3D
  }
  
  public void setFontBold(String fontFace, double fontSize) {
    setFont(getFont3DFSS(fontFace, "Bold", fontSize));
  }


  /**
   * @param TF  
   */
  public void setBackgroundTransparent(boolean TF) {
  }

  /**
   * sets background color to the specified argb value
   *
   * @param argb an argb value with alpha channel
   */
  public void setBackgroundArgb(int argb) {
    bgcolor = argb;
    setCel(shader.celOn);
    // background of Jmol transparent in front of certain applications (VLC Player)
    // when background [0,0,1]. 
  }

  public void setBackgroundImage(Object image) {
    backgroundImage = image;
  }

  public void setWindowParameters(int width, int height, boolean antialias) {
    setWinParams(width, height, antialias);
  }

  protected void setWinParams(int width, int height, boolean antialias) {
    newWindowWidth = width;
    newWindowHeight = height;
    newAntialiasing = antialias;    
  }

  public void setNewWindowParametersForExport() {
    windowWidth = newWindowWidth;
    windowHeight = newWindowHeight;
    setWidthHeight(false);
  }

  protected void setWidthHeight(boolean isAntialiased) {
    width = windowWidth;
    height = windowHeight;
    if (isAntialiased) {
      width <<= 1;
      height <<= 1;
    }
    xLast = width - 1;
    yLast = height - 1;
    displayMinX = -(width >> 1);
    displayMaxX = width - displayMinX;
    displayMinY = -(height >> 1);
    displayMaxY = height - displayMinY;
    displayMinX2 = displayMinX<<2;
    displayMaxX2 = displayMaxX<<2;
    displayMinY2 = displayMinY<<2;
    displayMaxY2 = displayMaxY<<2;
    ht3 = height * 3;
    bufferSize = width * height;
  }

  /**
   * @param stereoRotationMatrix  
   * @param translucentMode
   * @param isImageWrite 
   * @param renderLow TODO
   */
  public void beginRendering(M3d stereoRotationMatrix, boolean translucentMode, boolean isImageWrite, boolean renderLow) {
  }

  public void endRendering() {
  }

  public void snapshotAnaglyphChannelBytes() {
  }

  /**
   * @param isImageWrite  
   * @return image object
   */
  public Object getScreenImage(boolean isImageWrite) {
    return null;
  }

  public void releaseScreenImage() {
  }

  /**
   * @param stereoMode  
   * @param stereoColors 
   */
  public void applyAnaglygh(STER stereoMode, int[] stereoColors) {
  }

  /**
   * @param antialias  
   * @return true if need a second (translucent) pass
   */
  public boolean setPass2(boolean antialias) {
    return false;
  }

  public void destroy() {
  }

  public void clearFontCache() {
  }

  public void drawQuadrilateralBits(JmolRendererInterface jmolRenderer, short colix, P3d screenA, P3d screenB,
                                    P3d screenC, P3d screenD) {
    //mesh only -- translucency has been checked
    jmolRenderer.drawLineBits(colix, colix, screenA, screenB);
    jmolRenderer.drawLineBits(colix, colix, screenB, screenC);
    jmolRenderer.drawLineBits(colix, colix, screenC, screenD);
    jmolRenderer.drawLineBits(colix, colix, screenD, screenA);
  }

  public void drawTriangleBits(JmolRendererInterface renderer, P3d screenA, short colixA, P3d screenB,
                               short colixB, P3d screenC, short colixC, int check) {
    // primary method for mapped Mesh
    if ((check & 1) == 1)
      renderer.drawLineBits(colixA, colixB, screenA, screenB);
    if ((check & 2) == 2)
      renderer.drawLineBits(colixB, colixC, screenB, screenC);
    if ((check & 4) == 4)
      renderer.drawLineBits(colixC, colixA, screenC, screenA);
  }

  /**
   * @param x  
   * @param y 
   * @param z 
   * @param image 
   * @param jmolRenderer
   * @param bgcolix
   * @param width
   * @param height
   *  
   */
  public void plotImage(int x, int y, int z, Object image,
                        JmolRendererInterface jmolRenderer, short bgcolix,
                        int width, int height) {
  }

  /**
   * @param x  
   * @param y 
   * @param z 
   * @param colorArgbOrGray
   * @param bgColor TODO
   * @param text
   * @param font3d 
   * @param jmolRenderer
   *  
   */
  public void plotText(int x, int y, int z, int colorArgbOrGray, int bgColor,
                       String text, Font font3d, JmolRendererInterface jmolRenderer) {
  }

  /**
   * @param jmolRenderer  
   */
  public void renderBackground(JmolRendererInterface jmolRenderer) {
  }

  public int argbNoisyUp, argbNoisyDn;

  public void setColor(int argb) {
    argbCurrent = argbNoisyUp = argbNoisyDn = argb;
  }
  
  /**
   * @param colix  
   * @return TRUE if correct pass (translucent or opaque)
   */
  public boolean setC(short colix) {    
    return true;
  }

  /**
   * @param normix  
   * @return true if front
   */
  public boolean isDirectedTowardsCamera(short normix) {
    // normix < 0 means a double sided normix, so always visible
    return (normix < 0) || (transformedVectors[normix].z > 0);
  }

  /**
   * JavaScript won't really have an integer here after integer division.
   * So we need to round it to the integer between it and zero. 
   * 
   * @param a
   * @return number closest to zero
   */
  public static int roundInt(int a) {
    /**
     * @z2sNative
     *
     *  return a < 0 ? Math.ceil(a) : Math.floor(a);
     *  
     */
    {
      return a;
    }
  }

  public void clear() {
    // only in Graphics3D
  }

  @Override
  public void renderAllStrings(Object jmolRenderer) {
    // only in Graphics3D
  }

  /**
   * @param tok  
   */
  public void addRenderer(int tok) {
    // needed for JavaScript implementation to avoid unnecessary core loading
  }
  
  /**
   * Used by Navigator, BioShapeRenderer, and DrawRenderer
   * 
   * @param tension
   * @param p0
   * @param p1
   * @param p2
   * @param p3
   * @param p4
   * @param list
   * @param index0
   * @param n
   * @param isPt
   */
  public static void getHermiteList(int tension, T3d p0, T3d p1,
                                    T3d p2, T3d p3, T3d p4,
                                    T3d[] list, int index0, int n, boolean isPt) {
    //always deliver ONE MORE than one might expect, to provide a normal
    int nPoints = n + 1;
    double fnPoints = n - 1;
    double x1 = p1.x, y1 = p1.y, z1 = p1.z;
    double x2 = p2.x, y2 = p2.y, z2 = p2.z;
    double xT1 = ((x2 - p0.x) * tension) / 8;
    double yT1 = ((y2 - p0.y) * tension) / 8;
    double zT1 = ((z2 - p0.z) * tension) / 8;
    double xT2 = ((p3.x - x1) * tension) / 8;
    double yT2 = ((p3.y - y1) * tension) / 8;
    double zT2 = ((p3.z - z1) * tension) / 8;
    double xT3 = ((p4.x - x2) * tension) / 8;
    double yT3 = ((p4.y - y2) * tension) / 8;
    double zT3 = ((p4.z - z2) * tension) / 8;
    list[index0] = p1;
    for (int i = 0; i < nPoints; i++) {
      double s = i / fnPoints;
      if (i == nPoints - 1) {
        x1 = x2;
        y1 = y2;
        z1 = z2;
        x2 = p3.x;
        y2 = p3.y;
        z2 = p3.z;
        xT1 = xT2;
        yT1 = yT2;
        zT1 = zT2;
        xT2 = xT3;
        yT2 = yT3;
        zT2 = zT3;
        s -= 1;
      }
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2 * s3 - 3 * s2 + 1;
      double h2 = -2 * s3 + 3 * s2;
      double h3 = s3 - 2 * s2 + s;
      double h4 = s3 - s2;
      double x = (double) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
      double y = (double) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
      double z = (double) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
      list[index0 + i] = (isPt ? P3d.new3(x, y, z) : V3d.new3(x, y, z));
    }
  }

  public void setTextPosition(int y) {
    textY = y;
  }

  public int getTextPosition() {
    return textY;
  }

  protected V3d[] transformedVectors = new V3d[normixCount];

  public V3d[] getTransformedVertexVectors() {
    return transformedVectors;
  }

  protected static short normixCount = Normix.getNormixCount();
 
  @Override
  public void drawLinePixels(P3i sA, P3i sB, int z, int zslab) {
    return;
  }


}
