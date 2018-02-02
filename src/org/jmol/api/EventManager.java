package javajs.api;

public interface EventManager {

  boolean keyPressed(int keyCode, int modifiers);

  boolean keyTyped(int keyChar, int modifiers);

  void keyReleased(int keyCode);

  void mouseEnterExit(long time, int x, int y, boolean isExit);
  
  void mouseAction(int mode, long time, int x, int y, int count,
                   int buttonMods);

}
