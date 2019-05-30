package org.jmol.awtjs;

public class Event {

  public static final int MOUSE_LEFT   =  16;
  public static final int MOUSE_MIDDLE =   8; //Event.ALT_MASK;
  public static final int MOUSE_RIGHT  =   4; //Event.META_MASK;
  public static final int MOUSE_WHEEL  =  32;
  
  public final static int MAC_COMMAND = MOUSE_LEFT | MOUSE_RIGHT;
  public final static int BUTTON_MASK = MOUSE_LEFT | MOUSE_MIDDLE | MOUSE_RIGHT;

  public static final int MOUSE_DOWN   = 501; //InputEvent.MOUSE_DOWN;
  public static final int MOUSE_UP     = 502; //Event.MOUSE_UP;
  public static final int MOUSE_MOVE   = 503; //Event.MOUSE_MOVE;
  public static final int MOUSE_ENTER  = 504; //Event.MOUSE_ENTER;
  public static final int MOUSE_EXIT   = 505; //Event.MOUSE_EXIT;
  public static final int MOUSE_DRAG   = 506; //Event.MOUSE_DRAG;
    
  
  public static final int SHIFT_MASK =  1;//InputEvent.SHIFT_MASK;
  public static final int ALT_MASK   =  8;//InputEvent.ALT_MASK;
  public static final int CTRL_MASK  =  2;//InputEvent.CTRL_MASK;
  
  public final static int CTRL_ALT = CTRL_MASK | ALT_MASK;
  public final static int CTRL_SHIFT = CTRL_MASK | SHIFT_MASK;

  public static final int META_MASK  =  4;//InputEvent.META_MASK;
  public static final int VK_SHIFT   = 16;//KeyEvent.VK_SHIFT;
  public static final int VK_ALT     = 18;//KeyEvent.VK_ALT;
  public static final int VK_CONTROL = 17;//KeyEvent.VK_CONTROL;
  public static final int VK_META    = 157; // KeyEvent.VK_META;
  public static final int VK_LEFT    = 37;//KeyEvent.VK_LEFT;
  public static final int VK_RIGHT   = 39;//KeyEvent.VK_RIGHT;
  public static final int VK_PERIOD  = 46;//KeyEvent.VK_PERIOD;
  public static final int VK_SPACE   = 32;//KeyEvent.VK_SPACE;
  public static final int VK_DOWN    = 40;//KeyEvent.VK_DOWN;
  public static final int VK_UP      = 38;//KeyEvent.VK_UP;
  public static final int VK_ESCAPE  = 27;//KeyEvent.VK_ESCAPE;
  public static final int VK_DELETE  = 127;//KeyEvent.VK_DELETE;
  public static final int VK_BACK_SPACE = 8;//KeyEvent.VK_BACK_SPACE;
  public static final int VK_PAGE_DOWN = 34;//KeyEvent.VK_PAGE_DOWN;
  public static final int VK_PAGE_UP   = 33;//KeyEvent.VK_PAGE_UP;

  // for status messages:
  public final static int MOVED = 0;
  public final static int DRAGGED = 1;
  public final static int CLICKED = 2;
  public final static int WHEELED = 3;
  public final static int PRESSED = 4;
  public final static int RELEASED = 5;

}
