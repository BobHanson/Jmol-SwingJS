package com.sparshui.server;

import com.sparshui.common.Location;
import com.sparshui.common.TouchState;

/**
 * Represents a touch point.
 * 
 * @author Tony Ross
 *
 */
public class TouchPoint {
	/**
	 * Used to assign a globally unique id to a new touch point.
	 */
	//private static int nextID = 0;
	
	/**
	 * 
	 */
	//private static Object idLock = new Object();
	
	/**
	 * 
	 */
	private int _id;
	
	/**
	 * 
	 */
	private Location _location;
	
	/**
	 * 
	 */
	private int _state;
	
	/**
	 * 
	 */
	private boolean _changed;
	
	private long _time;
	
	/**
	 * 
	 */
	private Group _group;

	/**
	 * The GestureServer needs to know whether an incoming
	 * touchPoint is bound to a client or not so that it
	 * can tell the input device whether or not to
	 * consume the event.
	 * 
	 * @return whether a client has claimed this touchPoint
	 * 
	 */
	public boolean isClaimed() {
	  return (_group != null);
	}
	/**
	 * 
	 * @param id 
	 * @param location
	 * @param time 
	 */
	public TouchPoint(int id, Location location, long time) {
		//synchronized(idLock) {
		//	_id = nextID++;
		//}
	  _id = id; // for debugging, I need the exact match with device points
		_location = location;
		_time = time;
		_state = TouchState.BIRTH;
	}
	
	/**
	 * Copy constructor
	 * @param tp
	 */
	public TouchPoint(TouchPoint tp) {
		_id = tp._id;
		_location = tp._location;
		_state = tp._state;
		_time = tp._time;
	}

	public long getTime() {
		return _time;
	}
	
	/**
	 * Get the touch point ID.
	 * @return
	 * 		The touch point ID.
	 */
	public int getID() {
		return _id;
	}
	
	/**
	 * Get the touch point location.
	 * @return
	 * 		The location of this touch point.
	 */
	public Location getLocation() {
		return _location;
	}
	
	/**
	 * Get the touch point state.
	 * @return
	 * 		The state of this touch point.
	 */
	public int getState() {
		return _state;
	}
	
  public void setState(int state) {
    _state = state;
  }

  /**
	 * Set the group for this touch point.
	 * 
	 * @param group
	 * 		The group the touch point should belong to.
	 */
	public void setGroup(Group group) {
		_group = group;
		_group.update(this);
	}
	
	/**
	 * Update this touch point with a new location and state.
	 * 
	 * @param location
	 * 		The new location.
	 * @param time 
	 * @param state
	 * 		The new state.
	 */
	public void update(Location location, long time, int state) {
		_location = location;
		_state = state;
		_changed = true;
		_time = time;
		if(_group != null) _group.update(this);
	}
	
	/**
	 * Reset the changed flag.
	 */
	public void resetChanged() {
		_changed = false;
	}
	
	/**
	 * Get the value of the changed flag.
	 * @return
	 * 		True if this touchpoint has changed since the
	 * 		last time resetChanged() was called.
	 */
	public boolean isChanged() {
		return _changed;
	}
	
	@Override
	public Object clone() {
		return new TouchPoint(this);
	}
  public boolean isNear(TouchPoint tp) {
    // figure 1000 x 800 screen, so 1/1000 is about 1 pixel. 
    // let's allow for +/- 5 pixels here, or about 0.005 screen widths.
    return (Math.abs(_location.getX() - tp._location.getX()) < 0.005 
        && Math.abs(_location.getY() - tp._location.getY()) < 0.005);
  }

}
