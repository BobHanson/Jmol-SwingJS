package jspecview.app;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.jmol.api.EventManager;
import org.jmol.api.GenericMouseInterface;
import org.jmol.util.Logger;

import org.jmol.awtjs.event.Event;
import jspecview.api.JSVPanel;

public class GenericMouse implements GenericMouseInterface {

	protected EventManager pd;
	protected JSVPanel jsvp;

	public GenericMouse(JSVPanel jsvp) {
		this.jsvp = jsvp;
		pd = jsvp.getPanelData();
	}

	protected int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

	@Override
	public void clear() {
		// nothing to do here now -- see ActionManager
	}

	@Override
	public boolean processEvent(int id, int x, int y, int modifiers, long time) {
		if (pd == null) {
			// blank screen
			if (!disposed && id == Event.MOUSE_DOWN && (modifiers & Event.MOUSE_RIGHT) != 0)
				jsvp.showMenu(x, y);
			return true;
		}
		if (id != MouseEvent.MOUSE_WHEEL)
			modifiers = applyLeftMouse(modifiers);
		switch (id) {
		case MouseEvent.MOUSE_WHEEL: // JavaScript
			wheeled(time, x, modifiers | Event.MOUSE_WHEEL);
			break;
		case Event.MOUSE_DOWN:
			xWhenPressed = x;
			yWhenPressed = y;
			modifiersWhenPressed10 = modifiers;
			pressed(time, x, y, modifiers, false);
			break;
		case Event.MOUSE_DRAG:
			dragged(time, x, y, modifiers);
			break;
		case Event.MOUSE_ENTER:
			entered(time, x, y);
			break;
		case Event.MOUSE_EXIT:
			exited(time, x, y);
			break;
		case Event.MOUSE_MOVE:
			moved(time, x, y, modifiers);
			break;
		case Event.MOUSE_UP:
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

	public void mouseEntered(MouseEvent e) {
		entered(e.getWhen(), e.getX(), e.getY());
	}

	public void mouseExited(MouseEvent e) {
		exited(e.getWhen(), e.getX(), e.getY());
	}

	public void mouseMoved(MouseEvent e) {
		moved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
	}

	public void mousePressed(MouseEvent e) {
		pressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e.isPopupTrigger());
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

	public void mouseReleased(MouseEvent e) {
		released(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
	}

	public void mouseClicked(MouseEvent e) {
    clicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
      .getClickCount());
}

	public void mouseWheelMoved(MouseWheelEvent e) {
		e.consume();
		wheeled(e.getWhen(), e.getWheelRotation(), e.getModifiers()
				| Event.MOUSE_WHEEL);
	}
	
	public void keyTyped(KeyEvent ke) {
		if (pd == null)
			return;
		char ch = ke.getKeyChar();
		int modifiers = ke.getModifiers();
		// for whatever reason, CTRL may also drop the 6- and 7-bits,
		// so we are in the ASCII non-printable region 1-31
		if (Logger.debuggingHigh || true)
			Logger.info("MouseManager keyTyped: " + ch + " " + (0 + ch) + " "
					+ modifiers);
		if (pd.keyTyped(ch, modifiers))
			ke.consume();
	}

	public void keyPressed(KeyEvent ke) {
		if (pd != null && pd.keyPressed(ke.getKeyCode(), ke.getModifiers()))
			ke.consume();
	}

	public void keyReleased(KeyEvent ke) {
		if (pd != null)
			pd.keyReleased(ke.getKeyCode());
	}

	protected void entered(long time, int x, int y) {
		if (pd != null)
			pd.mouseEnterExit(time, x, y, false);
	}

	protected void exited(long time, int x, int y) {
		if (pd != null)
			pd.mouseEnterExit(time, x, y, true);
	}

	/**
	 * 
	 * @param time
	 * @param x
	 * @param y
	 * @param modifiers
	 * @param clickCount
	 */
	protected void clicked(long time, int x, int y, int modifiers, int clickCount) {
		// clickedCount is not reliable on some platforms
		// so we will just deal with it ourselves
		if (pd != null)
			pd.mouseAction(Event.CLICKED, time, x, y, 1, modifiers);
	}

	protected boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT
																	// as drag, only move

	protected void moved(long time, int x, int y, int modifiers) {
		if (pd == null)
			return;
		if (isMouseDown)
			pd.mouseAction(Event.DRAGGED, time, x, y, 0, applyLeftMouse(modifiers));
		else
			pd.mouseAction(Event.MOVED, time, x, y, 0, modifiers & ~Event.BUTTON_MASK);
	}

	protected void wheeled(long time, int rotation, int modifiers) {
		if (pd != null)
			pd.mouseAction(Event.WHEELED, time, 0, rotation, 0, modifiers);
	}

	/**
	 * 
	 * @param time
	 * @param x
	 * @param y
	 * @param modifiers
	 * @param isPopupTrigger
	 */
	protected void pressed(long time, int x, int y, int modifiers,
			boolean isPopupTrigger) {
    if (pd == null) {
      if (!disposed)
      	jsvp.showMenu(x, y);        
      return;
    }
		isMouseDown = true;
		pd.mouseAction(Event.PRESSED, time, x, y, 0, modifiers);
	}

	protected void released(long time, int x, int y, int modifiers) {
		if (pd == null)
			return;
		isMouseDown = false;
		pd.mouseAction(Event.RELEASED, time, x, y, 0, modifiers);
	}

	protected void dragged(long time, int x, int y, int modifiers) {
		if (pd == null)
			return;
		if ((modifiers & Event.MAC_COMMAND) == Event.MAC_COMMAND)
			modifiers = modifiers & ~Event.MOUSE_RIGHT | Event.CTRL_MASK;
		pd.mouseAction(Event.DRAGGED, time, x, y, 0, modifiers);
	}

	protected static int applyLeftMouse(int modifiers) {
		// if neither BUTTON2 or BUTTON3 then it must be BUTTON1
		return ((modifiers & Event.BUTTON_MASK) == 0) ? (modifiers | Event.MOUSE_LEFT)
				: modifiers;
	}

	@Override
	public void processTwoPointGesture(float[][][] touches) {
	}

	private boolean disposed;
	
	@Override
	public void dispose() {
		pd = null;
		jsvp = null;
		disposed = true;
	}

}
