package org.jmol.awtsw;

import org.jmol.util.Font;

public class Platform extends org.jmol.awt.Platform {

  @Override
  public boolean isSingleThreaded() {
    return true;
  }

  ////// Image 

  @Override
  public Object allocateRgbImage(int windowWidth, int windowHeight,
                                 int[] pBuffer, int windowSize,
                                 boolean backgroundTransparent, boolean isImageWrite) {
    return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer, windowSize, backgroundTransparent);
  }

  /**
   * could be byte[] (from ZIP file) or String (local file name) or URL
   * @param data 
   * @return image object
   * 
   */
  @Override
  public Object createImage(Object data) {
    return Image.createImage(data, vwr);
  }

  @Override
  public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
    return Image.getStaticGraphics(image, backgroundTransparent);
  }

  @Override
  public void disposeGraphics(Object gOffscreen) {
    Image.disposeGraphics(gOffscreen);
  }

  @Override
  public void drawImage(Object g, Object img, int x, int y, int width, int height, boolean isDTI) {
    if (isDTI)
      Image.drawImageDTI(g, img, x, y, width, height);
    else
      Image.drawImage(g, img, x, y, width, height);
  }

  @Override
  public int[] grabPixels(Object imageobj, int width, int height, int[] pixels) {
    return Image.grabPixels(imageobj, width, height, pixels); 
  }

  @Override
  public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                 Object imageobj, int width, int height, int bgcolor) {
    return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width, height, bgcolor);
  }

  @Override
  public int[] getTextPixels(String text, Font font3d, Object gObj,
                             Object image, int width, int height, int ascent) {
    return Image.getTextPixels(text, font3d, gObj, image, width, height, ascent);
  }

  @Override
  public void flushImage(Object imagePixelBuffer) {
    Image.flush(imagePixelBuffer);
  }

  @Override
  public Object getGraphics(Object image) {
    return Image.getGraphics(image);
  }

      
}
