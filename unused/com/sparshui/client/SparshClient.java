package com.sparshui.client;

import java.util.List;

import com.sparshui.GestureType;
import com.sparshui.common.Event;
import com.sparshui.common.Location;

/**
 * The Client interface must be implemented in all clients wishing
 * to connect to the Sparsh-UI gesture server.  It requires the client
 * to implement three methods that the gesture server will request.
 * 
 * @author Jay Roltgen
 *
 */
public interface SparshClient {
	
	/**
	 * This method must return an ID for a group of points that should
	 * be processed together.  If a constant value is returned, all
	 * points will be processed together and gestures will be recognized
	 * on the entire screen.  Otherwise, if a unique ID is returned,
	 * gestures will be recognized locally for each ID.
	 * 
	 * @param location
	 * 		The location with coordinate values between 0 and 1 
	 * 		of the point we are requesting the groupID for.
	 * @return
	 * 		The groupID that is associated with the current location.
	 */
	public int getGroupID(Location location);
	
	/**
	 * This method must return the allowed Gestures for the specified
	 * group.  Gestures IDs are given in GestureType.java.  This
	 * method should construct a list of valid gesture IDs for the 
	 * specified groupID and return it.  The client will then begin 
	 * receiving events that are appropriate to the list of allowed
	 * gestures it returned.
	 * 
	 * @param groupID
	 * 		The groupID we wish to obtain the allowed gestures for.
	 * @return
	 * 		A list of allowed Gesture IDs
	 */
	public List<GestureType> getAllowedGestures(int groupID);
	
	/**
	 * This method processes events from the Gesture Server.  It
	 * should handle each event by sending it to the appropriate
	 * group specified by groupID. If groupID == Integer.MAX_VALUE
	 * and event is null, then the server connection has been lost.
	 * 
	 * @param groupID
	 * 		The group that this event was detected on.
	 * @param event
	 * 		The event that should be processed.
	 */
	public void processEvent(int groupID, Event event);
}
