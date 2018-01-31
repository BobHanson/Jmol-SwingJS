package org.jmol.api;

public interface JmolGestureServerInterface {
  public final static int OK = 1;
  public final static int HAS_CLIENT = 2;
  public final static int HAS_DRIVER = 4;
  public abstract void startGestureServer();
  public abstract void dispose();
  public abstract int getState();
}
