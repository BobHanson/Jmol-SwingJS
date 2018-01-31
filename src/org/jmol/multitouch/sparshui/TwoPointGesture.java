package org.jmol.multitouch.sparshui;

import java.util.ArrayList;
import java.util.List;


import org.jmol.multitouch.ActionManagerMT;
import org.jmol.util.Logger;
import javajs.util.V3;

import com.sparshui.common.Event;
import com.sparshui.common.Location;
import com.sparshui.common.TouchState;
import com.sparshui.common.messages.events.DragEvent;
import com.sparshui.common.messages.events.RotateEvent;
import com.sparshui.common.messages.events.ZoomEvent;
import com.sparshui.gestures.Gesture;
import com.sparshui.server.TouchPoint;

/**
 * TWO_POINT_GESTURE
 * 
 * This gesture requires two points of contact, but its type is not initially
 * defined. Instead, its type is determined on-the-fly to be one of ZOOM,
 * ROTATE, or 2-point DRAG based on the direction of motion and relative
 * positions of the starting points. Two traces are obtained, assuming nothing
 * about the ID of the incoming points from the input device but instead
 * operating from position on the screen.
 * 
 * v00 from pt(1,0) to pt(2,0)
 * 
 * ZOOM IN:    <---- x ----->  (at any angle)
 * ZOOM OUT:   ----> x <-----  (at any angle)  
 * 
 * ROTATE CW:    ^        |
 *               |   x    |    (at any angle, not implemented)
 *               |        V
 *    
 * ROTATE CCW:   |        ^
 *               |   x    |    (at any angle, not implemented)
 *               V        |
 *    
 * 2-point drag:
 * 
 *      --------->  
 *     x             (any direction)
 *      --------->    
 *    
 * Bob Hanson 12/13/2009
 * 
 * 
 */
public class TwoPointGesture implements Gesture /*extends StandardDynamicGesture*/ {

  /**
	 * 
	 */

  private int _myType = ActionManagerMT.INVALID_GESTURE;

  protected Location _offset = null;

  protected Location _offsetCentroid = null;
  private List<Location> _traces1 = new ArrayList<Location>();
  private List<Location> _traces2 = new ArrayList<Location>();
  private int _id1 = -1;
  private int _id2 = -1;
  private int _nTraces = 0;
  private float _scale;
  private float _rotation;

  private long time;


  // @override
  @Override
  public String getName() {
    return "TwoPointGesture";
  }

  // @override
  @Override
  public int getGestureType() {
    return _myType;
  }

  //@override
  @Override
  public List<Event> processChange(List<TouchPoint> touchPoints,
      TouchPoint changedPoint) {
    return processChangeSync(changedPoint);
  }
  
  private synchronized List<Event> processChangeSync(TouchPoint changedPoint) {
    List<Event> events = null;
    time = changedPoint.getTime();
    switch(changedPoint.getState()) {
      case TouchState.BIRTH:
        events = processBirth(changedPoint);
        break;
      case TouchState.MOVE:
        events = processMove(changedPoint);
        break;
      case TouchState.DEATH:
        events = processDeath(changedPoint);
        break;
    }
    return (events != null) ? events : new ArrayList<Event>();
  }
  
  // @override
  protected List<Event> processBirth(TouchPoint touchPoint) {
    Location location = touchPoint.getLocation();
    int id = touchPoint.getID();
    switch (_nTraces) {
    case 0:
      _traces1.clear();
      _traces1.add(Location.pixelLocation(location));
      _id1 = id;
      _nTraces = 1;
      break;
    case 1:
      _traces2.clear();
      _traces2.add(Location.pixelLocation(location));
      _id2 = id;
      Location o = _traces1.get(_traces1.size() - 1);
      _traces1.clear();
      _traces1.add(o);
      _nTraces = 2;
      break;
    default:
      Logger.error("TwoPointGesture birth ignored!");
    }
    return null;
  }

  // @override
  protected List<Event> processDeath(TouchPoint touchPoint) {
    int id = touchPoint.getID();
    switch (_nTraces) {
    case 0:
      Logger.error("TwoPointGesture death -- no traces! " + id);
      break;
    case 1:
      _nTraces = 0;
      break;
    case 2:
      if (id == _id1) {
        _id1 = _id2;
        List<Location> v = _traces1;
        _traces1 = _traces2;
        _traces2 = v;
        _traces2.clear();
        _id2 = -1;
        _nTraces = 1;
      } else if (id == _id2) {
        _traces2.clear();
        _id2 = -1;
        _nTraces = 1;
      } else {
        _nTraces = 0;
      }
      break;
    } 
    if (_nTraces == 0) {
      _traces1.clear();
      _traces2.clear();
      _id1 = _id2 = -1;
    }
    _offsetCentroid = null;
    _myType = ActionManagerMT.INVALID_GESTURE;
    //System.out.println("TwoPointGesture death ntraces:" + _nTraces + " ids:"
      //  + _id1 + "," + _id2 + " id:" + id);
    return null;
  }

  // @override
  protected List<Event> processMove(TouchPoint touchPoint) {
    //System.out.println("TwoPointGesture move type:" + _myType + " ntraces:" + _nTraces + " ids:" + _id1+","+_id2+ " id:" + touchPoint.getID()  + " sizes: " + _traces1.size() + " " + _traces2.size());
    List<Event> events = new ArrayList<Event>();
    if (!updateLocations(touchPoint))
      return events;
    if (_myType == ActionManagerMT.INVALID_GESTURE)
      checkType();
    Location locationLast = Location.screenLocation(_offsetCentroid);
    if (_myType == ActionManagerMT.INVALID_GESTURE 
        || !updateParameters())
      return events;
    Location location = Location.screenLocation(_offsetCentroid);
    Event event = null;
    switch (_myType) {
    case ActionManagerMT.ZOOM_GESTURE:
      event = new ZoomEvent(_scale, location, time);
      break;
    case ActionManagerMT.ROTATE_GESTURE:
      event = new RotateEvent(_rotation, location, time);
      break;
    case ActionManagerMT.MULTI_POINT_DRAG_GESTURE:
      if (locationLast != null) {
        V3 dxy = locationLast.getVector(location);
        event = new DragEvent(dxy.x, dxy.y, 2, time);
      }
      break;
    }
    if (event != null)
      events.add(event);
    return events;
  }

  private boolean updateLocations(TouchPoint touchPoint) {
    Location location = Location.pixelLocation(touchPoint.getLocation());
    int id = touchPoint.getID();
    // just use last three points.
    if (id == _id1) {
      if (_traces1.size() > 2) {
        while (_traces1.size() > 2) {
          _traces1.remove(0);
        }
      }
      _traces1.add(location);
    } else if (id == _id2) {
      if (_traces2.size() > 2) {
        while (_traces2.size() > 2) {
         _traces2.remove(0);
        }
      }
      _traces2.add(location);
    } else {
      Logger.error("TwoPointGesture updateLocation error: no trace with id " + id);
      return false;
    }
    return (_nTraces == 2 && _traces1.size() == 3 && _traces2.size() == 3);
  }
  
  private void checkType() {
    Location loc10 = _traces1.get(0);
    Location loc11 = _traces1.get(_traces1.size() - 1);
    V3 v1 = loc10.getVector(loc11);
    float d1 = v1.length();
    Location loc20 = _traces2.get(0);
    Location loc21 = _traces2.get(_traces2.size() - 1);
    V3 v2 = loc20.getVector(loc21);
    float d2 = v2.length();
    // rooted finger --> zoom (at this position, perhaps?)
    if (d1 < 3 || d2 < 3)
      return;
    v1.normalize();
    v2.normalize();
    float cos12 = (v1.dot(v2));
    // cos12 > 0.8 (same direction) will be required to indicate drag
    // cos12 < -0.8 (opposite directions) will be required to indicate zoom or rotate
    if (cos12 > 0.8) {
      // two co-aligned motions
      _myType = ActionManagerMT.MULTI_POINT_DRAG_GESTURE;
    } else if (cos12 < -0.8) {
      // to classic zoom motions
      _myType = ActionManagerMT.ZOOM_GESTURE;
    }
/*    if (Logger.debugging)
      Logger.info("TwoPointGesture type=" + _myType
          + "\n v1=" + v1 + " v2=" + v2 + " d1=" + d1 + " d2=" + d2    
          + "\n trace 1: " + loc10 + " -> " + loc11
          + "\n trace 2: " + loc20 + " -> " + loc21
          + " cos12=" + cos12
      );
*/
  }

  private boolean updateParameters() {
    Location loc10 = _traces1.get(0);
    Location loc20 = _traces2.get(0);
    Location loc11 = _traces1.get(_traces1.size() - 1);
    Location loc21 = _traces2.get(_traces2.size() - 1);
    float d1 = loc10.getDistance(loc11);
    float d2 = loc20.getDistance(loc21);
    float d12 = loc11.getDistance(loc21);
    if (d1 < 2 && d2 < 2)
      return false;
    V3 v00 = loc10.getVector(loc20);
    float d00 = v00.length();
    v00.normalize();
    switch (_myType) {
    case ActionManagerMT.ROTATE_GESTURE:
      _offsetCentroid = Location.getCenter(loc10, loc20);
      V3 v1;
      V3 v2;
      if (d2 < 2) {
        v1 = loc20.getVector(loc10);
        v2 = loc20.getVector(loc11);
      } else {
        v1 = loc10.getVector(loc20);
        v2 = loc10.getVector(loc21);
      }
      v1.cross(v1, v2);
      _rotation = (v1.z < 0 ? 1 : -1);
      return true;
    case ActionManagerMT.ZOOM_GESTURE:
      if (Math.abs(d12 - d00) < 2)
        return false;
      _scale = (d12 < d00 ? -1 : 1);
      _offsetCentroid = Location.getCentroid(loc10, loc20, d1 / (d1 + d2));
      return true;
    case ActionManagerMT.MULTI_POINT_DRAG_GESTURE:
      _offsetCentroid = Location.getCenter(loc11, loc21);
      return true;
    default:
      return false;
    }
  }
  
}
