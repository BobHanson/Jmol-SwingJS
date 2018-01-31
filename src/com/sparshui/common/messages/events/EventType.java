package com.sparshui.common.messages.events;

/**
 * This is an enumeration of all event types currently available.
 * If you add a new event, you must modify this enumeration as well
 * as the enumeration in the C++ version, should you want to add
 * the event there as well.
 * @author root
 *
 */
public class EventType {
  public static final int DRIVER_NONE = -2;
  public static final int SERVICE_LOST = -1;
	public final static int DRAG_EVENT = 0;
	public final static int ROTATE_EVENT = 1;
	public final static int SPIN_EVENT = 2;
	public final static int TOUCH_EVENT = 3;
	public final static int ZOOM_EVENT = 4;
	public final static int DBLCLK_EVENT = 5;
	public final static int FLICK_EVENT = 6;
	public final static int RELATIVE_DRAG_EVENT = 7;
}
