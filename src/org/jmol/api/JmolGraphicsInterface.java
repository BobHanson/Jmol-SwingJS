package org.jmol.api;


public interface JmolGraphicsInterface {

  public abstract boolean isAntialiased();

  public abstract boolean isClippedXY(int diameter, int screenX, int screenY);

  public abstract boolean isInDisplayRange(int x, int y);

  public abstract void renderAllStrings(Object jmolRenderer);

  public abstract void setSlab(int slabValue);

  public abstract void setSlabAndZShade(int slabValue, int depthValue, int zSlab, int zDepth, int zPower);

}
