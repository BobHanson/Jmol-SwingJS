package com.sparshui.gestures;

import java.util.List;

import com.sparshui.common.Event;
import com.sparshui.server.TouchPoint;

/**
 * Defines the interface that all user-defined gestures must implement.
 *
 * @author Tony Ross
 *
 */
public interface Gesture {

	/**
	 * Process a touch point change in the gesture.
	 * 
	 * @param touchPoints
	 * 		The list of touch points that currently belong to this gesture.
	 * @param changedTouchPoint
	 * 		The touch point that has changed.
	 * @return
	 * 		An ArrayList of events that will be delivered to the client.
	 * 		
	 */
	public abstract List<Event> processChange(List<TouchPoint> touchPoints,
			TouchPoint changedTouchPoint);

	/**
	 * Get the name of this gesture.
	 * @return
	 * 		The name of this gesture.
	 */
	public abstract String getName();
	
	/**
	 * Get the integer value of this gesture type.  Gesture values
	 * are defined in GestureType.java.
	 * @return
	 * 		The gesture type.
	 */
	public int getGestureType();
}
