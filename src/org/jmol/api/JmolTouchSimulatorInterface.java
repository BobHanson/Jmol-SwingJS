package org.jmol.api;

public interface JmolTouchSimulatorInterface {

  public abstract boolean startSimulator(Object display);

  public abstract void toggleMode();

  public abstract void mousePressed(long time, int x, int y);

  public abstract void mouseReleased(long time, int x, int y);

  public abstract void mouseDragged(long time, int x, int y);

  public abstract void startRecording();
  
  public abstract void endRecording();

  public abstract void dispose();
}
