package org.jmol.multitouch.sparshui;

import java.util.ArrayList;
import java.util.List;
import org.jmol.util.Logger;

import com.sparshui.GestureType;
import com.sparshui.common.Event;
import com.sparshui.common.TouchState;
import com.sparshui.common.messages.events.TouchEvent;
import com.sparshui.gestures.Gesture;
import com.sparshui.server.TouchPoint;

/**
 * SINGLE_POINT_GESTURE
 * 
 * only passes single-touch gestures.
 * allows detection of click and double-click
 * 
 * 
 */
public class SinglePointGesture implements Gesture {

  private static final long MAXIMUM_CLICK_TIME = 200;
  private int _nCurrent, _nMoves, _myId;
  private TouchPoint _birth;

  // @override
  @Override
  public String getName() {
    return "SinglePointGesture";
  }

  // @override
  @Override
  public int getGestureType() {
    return GestureType.TOUCH_GESTURE;
  }
  
  /**
   * 
   * incorporates double-click gesture
   * 
   * @param touchPoints
   * @param changedTouchPoint
   * @return Vector of Events
   * 
   */
  // @override
  
  @Override
  public List<Event> processChange(List<TouchPoint> touchPoints, TouchPoint changedTouchPoint) {
    List<Event> retEvents = new ArrayList<Event>();
    int nPoints = touchPoints.size();
    if (Logger.debugging) {
      Logger.debug("\nSinglePointGesture " + _myId + " nPoints: " + nPoints);
    }
    // idea here is to prevent single-touch action during/after a multi-touch action.
    // most multi-touch operations, though, start with a single-touch action. So
    // what we do is to clear that initial action if two fingers are down. 
    if (nPoints > 1) {
      if (_myId != Integer.MAX_VALUE) {
        _myId = Integer.MAX_VALUE;
        _nMoves = 1000;
        _nCurrent = 0;
        // indicate to clear all single-point mouse activity
        retEvents.add(new TouchEvent());
      }
      return retEvents;
    }
    int id = changedTouchPoint.getID();
    if (Logger.debugging)
      Logger.debug("\nSinglePointGesture id=" + id + " state="
          + changedTouchPoint.getState() + " ncurrent=" + _nCurrent
          + " nMoves=" + _nMoves);
    switch (changedTouchPoint.getState()) {
    case TouchState.BIRTH:
      _myId = id;
      _birth = new TouchPoint(changedTouchPoint);
      _nCurrent = 1;
      _nMoves = 0;
      break;
    case TouchState.MOVE:
      if (id != _myId)
        return retEvents;
      switch (++_nMoves) {
      case 2:
        if (checkClick(changedTouchPoint, retEvents, false))
          return retEvents;
        break;
      }
      break;
    case TouchState.DEATH:
      if (id != _myId)
        return retEvents;
      _nCurrent = 0;
      if (_nMoves < 2 && checkClick(changedTouchPoint, retEvents, true))
        return retEvents;
      break;
    }
    retEvents.add(new TouchEvent(changedTouchPoint));
    return retEvents;
  }

  private boolean checkClick(TouchPoint tpNew, List<Event> retEvents, boolean isDeath) {
    TouchPoint tp;
    long dt = tpNew.getTime() - _birth.getTime();
    boolean isSingleClick = (isDeath && dt < MAXIMUM_CLICK_TIME);
    if (dt < 500 && !isSingleClick)
      return false;
    _nMoves += 2;
    // long (1/2 sec) pause and drag == double-click-drag ==> _translate
    tp = new TouchPoint(_birth);
    tp.setState(TouchState.DEATH);
    retEvents.add(new TouchEvent(tp));
    tp.setState(TouchState.CLICK);
    retEvents.add(new TouchEvent(tp));
    if (isSingleClick)
      return true;
    tp.setState(TouchState.BIRTH);
    retEvents.add(new TouchEvent(tp));
    if (!isDeath)
      return true;
    tp.setState(TouchState.DEATH);
    retEvents.add(new TouchEvent(tp));
    tp.setState(TouchState.CLICK);
    retEvents.add(new TouchEvent(tp));
    return true;
  }

}
