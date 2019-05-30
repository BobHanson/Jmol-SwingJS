/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-09-11 19:29:26 -0500 (Tue, 11 Sep 2012) $
 * $Revision: 17556 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.awtjs2d;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.jmol.api.EventManager;
import org.jmol.api.GenericMouseInterface;
import org.jmol.awtjs.Event;
import org.jmol.script.T;
import org.jmol.util.Logger;

import javajs.util.PT;
import javajs.util.V3;
import org.jmol.viewer.Viewer;

/**
 * JavaScript interface from JmolJSmol.js via handleOldJvm10Event (for now)
 * 
 * 
 */

public class Mouse implements GenericMouseInterface {

  private Viewer vwr;
  private EventManager manager;
  //private double privateKey;

  /**
   * @param privateKey -- not used in JavaScript  
   * @param vwr 
   * @param display 
   */
  public Mouse(double privateKey, Viewer vwr, Object display) {
    //this.privateKey = privateKey; could be used for clipboard access
    this.vwr = vwr;
    manager = this.vwr.acm;
  }

  @Override
  public void clear() {
    // nothing to do here now -- see ActionManager
  }

  @Override
  public void dispose() {
    // nothing to do here
  }

  @Override
  public boolean processEvent(int id, int x, int y, int modifiers, long time) {
    if (id != MouseEvent.MOUSE_WHEEL)
      modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case MouseEvent.MOUSE_WHEEL: // JavaScript
      wheeled(time, x, modifiers);
      break;
    case MouseEvent.MOUSE_PRESSED:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      pressed(time, x, y, modifiers, false);
      break;
    case MouseEvent.MOUSE_DRAGGED:
      dragged(time, x, y, modifiers);
      break;
    case MouseEvent.MOUSE_ENTERED:
      entry(time, x, y, false);
      break;
    case MouseEvent.MOUSE_EXITED:
      entry(time, x, y, true);
      break;
    case MouseEvent.MOUSE_MOVED:
      moved(time, x, y, modifiers);
      break;
    case MouseEvent.MOUSE_RELEASED:
      released(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed
          && modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        clicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }

  /**
   * 
   * called by JSmol as processTwoPointGesture(canvas.touches);
   * 
   * @param touches
   *     [[finger1 touches],[finger2 touches]]
   *     where finger touches are [[x0,y0],[x1,y1],[x2,y2],...] 
   *    
   */
  @Override
  public void processTwoPointGesture(float[][][] touches) {
    
    if (touches[0].length < 2)
      return;
    float[][] t1 = touches[0];
    float[][] t2 = touches[1];
    float[] t1first = t1[0];
    float[] t1last = t1[t2.length - 1];
    float x1first = t1first[0];
    float x1last = t1last[0]; 
    float dx1 = x1last - x1first;
    float y1first = t1first[1];
    float y1last = t1last[1]; 
    float dy1 = y1last - y1first;
    V3 v1 = V3.new3(dx1, dy1, 0);
    float d1 = v1.length();
    float[] t2first = t2[0];
    float[] t2last = t2[t2.length - 1];
    float x2first = t2first[0];
    float x2last = t2last[0]; 
    float dx2 = x2last - x2first;
    float y2first = t2first[1];
    float y2last = t2last[1]; 
    float dy2 = y2last - y2first;
    V3 v2 = V3.new3(dx2, dy2, 0);    
    float d2 = v2.length();
    // rooted finger --> zoom (at this position, perhaps?)
    if (d1 < 1 || d2 < 1)
      return;
    v1.normalize();
    v2.normalize();
    float cos12 = (v1.dot(v2));
    // cos12 > 0.8 (same direction) will be required to indicate drag
    // cos12 < -0.8 (opposite directions) will be required to indicate zoom or rotate
    if (cos12 > 0.8) {
      // two co-aligned motions -- translate
      // just use finger 1, last move
      int deltaX = (int) (x1last - t1[t1.length - 2][0]); 
      int deltaY = (int) (y1last - t1[t1.length - 2][1]); 
      vwr.translateXYBy(deltaX, deltaY);
    } else if (cos12 < -0.8) {
      // two classic zoom motions -- zoom
      v1 = V3.new3(x2first - x1first, y2first - y1first, 0);
      v2 = V3.new3(x2last - x1last, y2last - y1last, 0);
      float dx = v2.length() - v1.length();
      wheeled(System.currentTimeMillis(), dx < 0 ? -1 : 1, Event.MOUSE_WHEEL);
    }
  }
  
  public void mouseClicked(MouseEvent e) {
    clicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .getClickCount());
  }

  public void mouseEntered(MouseEvent e) {
    entry(e.getWhen(), e.getX(), e.getY(), false);
  }

  public void mouseExited(MouseEvent e) {
    entry(e.getWhen(), e.getX(), e.getY(), true);
  }

  public void mousePressed(MouseEvent e) {
    pressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .isPopupTrigger());
  }

  public void mouseReleased(MouseEvent e) {
    released(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseDragged(MouseEvent e) {
    int modifiers = e.getModifiers();
    /****************************************************************
     * Netscape 4.* Win32 has a problem with mouseDragged if you left-drag then
     * none of the modifiers are selected we will try to fix that here
     ****************************************************************/
    if ((modifiers & Event.BUTTON_MASK) == 0)
      modifiers |= Event.MOUSE_LEFT;
    /****************************************************************/
    dragged(e.getWhen(), e.getX(), e.getY(), modifiers);
  }

  public void mouseMoved(MouseEvent e) {
    moved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    e.consume();
    wheeled(e.getWhen(), e.getWheelRotation(), e.getModifiers());
  }

  public void keyTyped(KeyEvent ke) {
    ke.consume();
    if (!vwr.menuEnabled())
      return;
    char ch = ke.getKeyChar();
    int modifiers = ke.getModifiers();
    // for whatever reason, CTRL may also drop the 6- and 7-bits,
    // so we are in the ASCII non-printable region 1-31
    if (Logger.debuggingHigh)
      Logger.debug("MouseManager keyTyped: " + ch + " " + (0+ch) + " " + modifiers);
    if (modifiers != 0 && modifiers != Event.SHIFT_MASK) {
      switch (ch) {
      case (char) 11:
      case 'k': // keystrokes on/off
        boolean isON = !vwr.getBooleanProperty("allowKeyStrokes");
        switch (modifiers) {
        case Event.CTRL_MASK:
          vwr.setBooleanProperty("allowKeyStrokes", isON);
          vwr.setBooleanProperty("showKeyStrokes", true);
          break;
        case Event.CTRL_ALT:
        case Event.SHIFT_MASK:
          vwr.setBooleanProperty("allowKeyStrokes", isON);
          vwr.setBooleanProperty("showKeyStrokes", false);
          break;
        }
        clearKeyBuffer();
        vwr.refresh(Viewer.REFRESH_SYNC_MASK, "showkey");
        break;
      case 22:
      case 'v': // paste
        switch (modifiers) {
        case Event.CTRL_MASK:
        	break;
        }
        break;
      case 26:
      case 'z': // undo
        switch (modifiers) {
        case Event.CTRL_MASK:
          vwr.undoMoveAction(T.undomove, 1);
          break;
        case Event.CTRL_SHIFT:
          vwr.undoMoveAction(T.redomove, 1);
          break;
        }
        break;
      case 25:
      case 'y': // redo
        switch (modifiers) {
        case Event.CTRL_MASK:
          vwr.undoMoveAction(T.redomove, 1);
          break;
        }
        break;        
      }
      return;
    }
    if (!vwr.getBooleanProperty("allowKeyStrokes"))
      return;
    addKeyBuffer(ke.getModifiers() == Event.SHIFT_MASK ? Character.toUpperCase(ch) : ch);
  }

  public void keyPressed(KeyEvent ke) {
    if (vwr.isApplet)
      ke.consume();
    manager.keyPressed(ke.getKeyCode(), ke.getModifiers());
  }

  public void keyReleased(KeyEvent ke) {
    ke.consume();
    manager.keyReleased(ke.getKeyCode());
  }

  private String keyBuffer = "";

  private void clearKeyBuffer() {
    if (keyBuffer.length() == 0)
      return;
    keyBuffer = "";
    if (vwr.getBooleanProperty("showKeyStrokes"))
      vwr
          .evalStringQuietSync("!set echo _KEYSTROKES; set echo bottom left;echo \"\"", true, true);
  }

  private void addKeyBuffer(char ch) {
    if (ch == 10) {
      sendKeyBuffer();
      return;
    }
    if (ch == 8) {
      if (keyBuffer.length() > 0)
        keyBuffer = keyBuffer.substring(0, keyBuffer.length() - 1);
    } else {
      keyBuffer += ch;
    }
    if (vwr.getBooleanProperty("showKeyStrokes"))
      vwr
          .evalStringQuietSync("!set echo _KEYSTROKES; set echo bottom left;echo "
              + PT.esc("\1" + keyBuffer), true, true);
  }

  private void sendKeyBuffer() {
    String kb = keyBuffer;
    if (vwr.getBooleanProperty("showKeyStrokes"))
      vwr
          .evalStringQuietSync("!set echo _KEYSTROKES; set echo bottom left;echo "
              + PT.esc(keyBuffer), true, true);
    clearKeyBuffer();
    vwr.evalStringQuietSync(kb, false, true);
  }

  private void entry(long time, int x, int y, boolean isExit) {
    wheeling = false;
    isMouseDown = false;
    modifiersDown = 0;
    manager.mouseEnterExit(time, x, y, isExit);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param clickCount
   */
  private void clicked(long time, int x, int y, int modifiers, int clickCount) {
    clearKeyBuffer();
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    manager.mouseAction(Event.CLICKED, time, x, y, 1, modifiers);
  }

  private boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
  private boolean wheeling;
  private int modifiersDown;
  
  private void moved(long time, int x, int y, int modifiers) {
    clearKeyBuffer();
    if (isMouseDown)
      manager.mouseAction(Event.DRAGGED, time, x, y, 0, modifiersDown);
    else
      manager.mouseAction(Event.MOVED, time, x, y, 0, modifiers);
  }

  private void wheeled(long time, int rotation, int modifiers) {
    clearKeyBuffer();
    wheeling = true;
    manager.mouseAction(Event.WHEELED, time, 0, rotation, 0, 
        modifiers & ~Event.BUTTON_MASK | Event.MOUSE_WHEEL);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param isPopupTrigger
   */
  private void pressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {
    clearKeyBuffer();
    isMouseDown = true;
    modifiersDown = modifiers;
    wheeling = false;
    manager.mouseAction(Event.PRESSED, time, x, y, 0, modifiers);
  }

  private void released(long time, int x, int y, int modifiers) {
    isMouseDown = false;
    modifiersDown = 0;
    wheeling = false;
    manager.mouseAction(Event.RELEASED, time, x, y, 0, modifiers);
  }

  private void dragged(long time, int x, int y, int modifiers) {
    if (wheeling)
      return;
    if ((modifiersDown & Event.MAC_COMMAND) == Event.MAC_COMMAND)
      modifiersDown = modifiersDown & ~Event.MOUSE_RIGHT | Event.CTRL_MASK; 
    manager.mouseAction(Event.DRAGGED, time, x, y, 0, modifiersDown);
  }

  private static int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & Event.BUTTON_MASK) == 0) ? (modifiers | Event.MOUSE_LEFT)
        : modifiers;
  }

  private int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

}
