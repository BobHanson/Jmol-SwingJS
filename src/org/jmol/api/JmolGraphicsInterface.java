package org.jmol.api;

import javajs.util.P3i;

public interface JmolGraphicsInterface {

  public abstract boolean isAntialiased();

  public abstract boolean isClippedXY(int diameter, int screenX, int screenY);

  public abstract boolean isInDisplayRange(int x, int y);

  public abstract void renderAllStrings(Object jmolRenderer);

  public abstract void setSlab(int slabValue);

  public abstract void setSlabAndZShade(int slabValue, int depthValue, int zSlab, int zDepth, int zPower);

  public abstract void drawLinePixels(P3i sA, P3i sB, int z, int zslab);

}
