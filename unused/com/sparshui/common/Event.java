package com.sparshui.common;

import java.io.Serializable;

/**
 * This interface must be implemented by all user-defined events.
 * 
 * @author Jay Roltgen
 *
 */
public interface Event extends Serializable {

	/**
	 * Returns the integer value of this event type.  Event type
	 * values are defined in the enumeration 
	 * com.sparshui.common.messages.events.EventType.java
	 * 
	 * @return
	 * 		The event type
	 */
	public abstract int getEventType();
	
	/**
	 * Serializes this event for transmission over the network.
	 * The user-defined event shall implement this method, as well
	 * as a constructor that takes the serialized byte array as an
	 * input argument.  This method will serialize the event, and
	 * the constructor will "unserialize" it.
	 * 
	 * @return
	 * 		The serialized event, ready for transmission over
	 * 		the network.
	 */
	public abstract byte[] serialize();
		
}
