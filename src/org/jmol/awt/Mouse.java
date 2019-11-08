/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-09-25 15:33:17 -0500 (Wed, 25 Sep 2013) $
 * $Revision: 18695 $
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
package org.jmol.awt;

import java.awt.Component;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javajs.util.PT;

import org.jmol.api.EventManager;
import org.jmol.api.GenericMouseInterface;
import org.jmol.api.PlatformViewer;
import org.jmol.awtjs.Event;
import org.jmol.script.T;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * formerly org.jmol.viewer.MouseManager14
 * 
 * methods required by Jmol that access java.awt.event
 * 
 * private to org.jmol.awt
 * 
 */

class Mouse implements MouseWheelListener, MouseListener,
    MouseMotionListener, KeyListener, GenericMouseInterface {

  private Viewer vwr;
  private EventManager manager;

  //double privateKey;
  /**
   * Mouse is the listener for all events. 
   * 
   * @param privateKey  
   * @param vwr 
   * @param odisplay 
   */
  Mouse(double privateKey, PlatformViewer vwr, Object odisplay) {
    //this.privateKey = privateKey;
    this.vwr = (Viewer) vwr;
    manager = this.vwr.acm;
    Component display = (Component) odisplay;
    display.addKeyListener(this);
    display.addMouseListener(this);
    display.addMouseMotionListener(this);
    display.addMouseWheelListener(this);
  }

  @Override
  public void clear() {
    // nothing to do here now -- see ActionManager
  }

  @Override
  public void dispose() {
    Component display = (Component) vwr.display;
    display.removeMouseListener(this);
    display.removeMouseMotionListener(this);
    display.removeMouseWheelListener(this);
    display.removeKeyListener(this);
  }

  @Override
  public boolean processEvent(int id, int x, int y, int modifiers, long time) {
    modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case Event.MOUSE_DOWN:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      mousePressed(time, x, y, modifiers, false);
      break;
    case Event.MOUSE_DRAG:
      mouseDragged(time, x, y);
      break;
    case Event.MOUSE_ENTER:
      mouseEntered(time, x, y);
      break;
    case Event.MOUSE_EXIT:
      mouseExited(time, x, y);
      break;
    case Event.MOUSE_MOVE:
      mouseMoved(time, x, y, modifiers);
      break;
    case Event.MOUSE_UP:
      mouseReleased(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed
          && modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        mouseClicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    mouseClicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .getClickCount());
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    mouseEntered(e.getWhen(), e.getX(), e.getY());
  }

  @Override
  public void mouseExited(MouseEvent e) {
    mouseExited(e.getWhen(), e.getX(), e.getY());
  }

  @Override
  public void mousePressed(MouseEvent e) {
    mousePressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .isPopupTrigger());
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    mouseReleased(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    mouseDragged(e.getWhen(), e.getX(), e.getY());
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    mouseMoved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    e.consume();
    mouseWheel(e.getWhen(), e.getWheelRotation(), e.getModifiers());
  }

  @Override
  public void keyTyped(KeyEvent ke) {
    ke.consume();
    if (!vwr.menuEnabled())
      return;
    char ch = ke.getKeyChar();
    int modifiers = ke.getModifiers();
    // for whatever reason, CTRL may also drop the 6- and 7-bits,
    // so we are in the ASCII non-printable region 1-31
//    if (Logger.debuggingHigh && vwr.getBoolean(T.testflag1))
//      Logger.debug("MouseManager keyTyped: " + ch + " " + (0+ch) + " " + modifiers);
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
        case Event.ALT_MASK:
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
          String ret = vwr.getClipboardText();
          if (ret == null)
            break;
          if (ret.startsWith("http://") && ret.indexOf("\n") < 0)
            ret = "LoAd " + PT.esc(ret);
          if (ret.startsWith("LoAd ")) {
            vwr.evalStringQuietSync(ret, false, true);
            break;
          }
          ret = vwr.loadInlineAppend(ret, false);
          if (ret != null)
            Logger.error(ret);
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
    addKeyBuffer(ke.getModifiers() == Event.SHIFT_MASK ? Character.toUpperCase(ch) : ch);
  }

  @Override
  public void keyPressed(KeyEvent ke) {
    if (vwr.isApplet)
      ke.consume();
    manager.keyPressed(ke.getKeyCode(), ke.getModifiers());
  }

  @Override
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
    if (!vwr.getBooleanProperty("allowKeyStrokes")) {
      if (vwr.atomHighlighted >= 0) {
        checkElementSelected(ch);
      }
      return;
    }
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

  private void checkElementSelected(char ch) {
    if (PT.isUpperCase(ch)) {
      keyBuffer = "";
    }
    keyBuffer += ch;
       int elemno =  Elements.elementNumberFromSymbol(keyBuffer, true);
    if (elemno <= 0)
      elemno = Elements.elementNumberFromSymbol(keyBuffer.toUpperCase(), true);
    System.out.println("CHECKELEME " + ch + " " +  keyBuffer + " " + elemno);
    if (elemno > 0)
      vwr.assignAtom(-1, Elements.elementSymbolFromNumber(elemno), null);
    if (!PT.isUpperCase(ch))
      keyBuffer = "";
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

  private void mouseEntered(long time, int x, int y) {
    wheeling = false;
    manager.mouseEnterExit(time, x, y, false);
  }

  private void mouseExited(long time, int x, int y) {
    wheeling = false;
    manager.mouseEnterExit(time, x, y, true);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param clickCount
   */
  private void mouseClicked(long time, int x, int y, int modifiers, int clickCount) {
    clearKeyBuffer();
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    manager.mouseAction(Event.CLICKED, time, x, y, 1, modifiers);
  }

  private boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
  private boolean wheeling;
  private int modifiersDown;
  
  private void mouseMoved(long time, int x, int y, int modifiers) {
    clearKeyBuffer();
    if (isMouseDown)
      manager.mouseAction(Event.DRAGGED, time, x, y, 0, modifiersDown);
    else
      manager.mouseAction(Event.MOVED, time, x, y, 0, modifiers);
  }

  private void mouseWheel(long time, int rotation, int modifiers) {
    clearKeyBuffer();
    wheeling = true;
    manager.mouseAction(Event.WHEELED, time, 0, rotation, 0, 
        modifiers & ~Event.MOUSE_MIDDLE | Event.MOUSE_WHEEL);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param isPopupTrigger
   */
  private void mousePressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {
    clearKeyBuffer();
    isMouseDown = true;
    modifiersDown = modifiers; // Mac does not transmit these during drag
    wheeling = false;
    manager.mouseAction(Event.PRESSED, time, x, y, 0, modifiers);
  }

  private void mouseReleased(long time, int x, int y, int modifiers) {
    isMouseDown = false;
    modifiersDown = 0;
    wheeling = false;
    manager.mouseAction(Event.RELEASED, time, x, y, 0, modifiers);
  }

  private void mouseDragged(long time, int x, int y) {
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

  @Override
  public void processTwoPointGesture(float[][][] touches) {
    // n/a
    
  }

}
