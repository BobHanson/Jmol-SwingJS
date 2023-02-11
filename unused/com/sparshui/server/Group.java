package com.sparshui.server;

import java.io.IOException;

import javajs.util.Lst;

import com.sparshui.GestureType;
import com.sparshui.common.Event;
import com.sparshui.common.TouchState;
import com.sparshui.gestures.Gesture;

/**
 * Represents a group of touch points 
 * for the gesture server.
 * 
 * @author Tony Ross
 * 
 */
public class Group {

	private int _id;
	private Lst<GestureType> _gestureTypes;
	private Lst<Gesture> _gestures;
	private Lst<TouchPoint> _touchPoints;
	private ServerToClientProtocol _clientProtocol;

	/**
	 * Construct a new group with the given gesture IDs and
	 * the given connection to the client.  Groups are associate
	 * with one client only.
	 * @param id 
	 * 
	 * @param gestureTypes
	 * 		The list of gesture types (integer or String class name) 
	 *    that this group should process.
	 * @param clientProtocol
	 * 		Represents the connection to the client.
	 */
	public Group(int id, Lst<GestureType> gestureTypes,
			ServerToClientProtocol clientProtocol) {
		_id = id;
		_gestureTypes = gestureTypes;
		_gestures = new Lst<Gesture>();
		_touchPoints = new Lst<TouchPoint>();
		_clientProtocol = clientProtocol;
		for (int i = 0; i < _gestureTypes.size(); i++) {
		  Gesture gesture = GestureFactory.createGesture(_gestureTypes.get(i));
		  if (gesture != null)
  			_gestures.addLast(gesture);
		}
	}

	/**
	 * 
	 * @return
	 * 		The group ID
	 */
	public int getID() {
		return _id;
	}

	/**
   * Update the given touch point that belongs to this group.
   * 
   * @param changedPoint
   *          The changed touch point.
   */
  public synchronized void update(TouchPoint changedPoint) {
    Lst<Event> events = new Lst<Event>();
    
    int state = changedPoint.getState();

    if (state == TouchState.BIRTH)
      _touchPoints.addLast(changedPoint);

    /*
    System.out.print("Group _touchPoints ");
    for (int i = 0; i < _touchPoints.size(); i++) {
      System.out.print(" / " + i + ": " + (TouchPoint) _touchPoints.get(i));
    }
    System.out.println();
    */
    
    //List<TouchPoint> clonedPoints = null;
    /*
     * // until this is implemented somewhere, why go to the trouble? -- BH
     * 
     * clonedPoints = new Vector(); for (int i = 0; i < nPoints; i++) {
     * TouchPoint touchPoint = (TouchPoint) _touchPoints.get(i); synchronized
     * (touchPoint) { TouchPoint clonedPoint = (TouchPoint) touchPoint.clone();
     * clonedPoints.add(clonedPoint); } }
     */
    for (int i = 0; i < _gestures.size(); i++) {
      Gesture gesture = _gestures.get(i);
      //System.out.println(_gestures.size());
      //System.out.println("Gesture allowed: " + gesture.getName());
      events.addAll(gesture.processChange(
          //clonedPoints == null ? 
          _touchPoints
          //: clonedPoints
          , changedPoint));
      //System.out.println("Got some events - size: " + events.size());
    }

    // moved to after processing. 
    if (state == TouchState.DEATH)
      _touchPoints.removeObj(changedPoint);

    try {
      _clientProtocol.processEvents(_id, events);
    } catch (IOException e) {
      /*
       * Do nothing here. We're ignoring the error because the client will get
       * killed on the next touch point birth and we do not have a reference to
       * the client or the server from group to avoid circular references.
       */
    }
  }

}
