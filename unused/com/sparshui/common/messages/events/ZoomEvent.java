package com.sparshui.common.messages.events;

import com.sparshui.common.Event;
import com.sparshui.common.Location;
import com.sparshui.common.utils.Converter;

public class ZoomEvent implements Event {
	private static final long serialVersionUID = -4658011539863774168L;
	
	private float _scale;
	private Location _center;
  private long _time;
  
	public ZoomEvent() {
		_scale = 1;
		_center = new Location();
	}
	
	public ZoomEvent(float scale, Location center, long time) {
		_scale = scale;
		_center = center;
		_time = time;
		//System.out.println("ZoomEvent Constructed, Scale = " + _scale);
	}
	
	public float getScale() {
		return _scale;
	}
	
  public long getTime() {
    return _time;
  }

  public Location getCenter() {
		return _center;
	}
	
	public void setCenter(Location center) {
		_center = center;
	}
	
  public float getX() {
    return _center.getX();
  }
  
  public float getY() {
    return _center.getY();
  }
  
	/**
	 * Constructs a ZoomEvent from a serialized version of ZoomEvent.
	 *  - 4 bytes : scale 
	 *  - 4 bytes : center - x coordinate 
	 *  - 4 bytes : center - y coordinate
	 *  - 12 bytes total
	 *
   *
   * @param data
   */
  public ZoomEvent(byte[] data) {
    
    if (data.length < 12) {
      // TODO add error handling
      System.err.println("Error constructing Zoom Event.");
      _scale = 1;
      _center = new Location(0, 0);
    } else {
      _scale = Converter.byteArrayToFloat(data, 0);
      _center = new Location(Converter.byteArrayToFloat(data, 4),
          Converter.byteArrayToFloat(data, 8));
    }
  }

	//@override
	@Override
  public int getEventType() {
		return EventType.ZOOM_EVENT;
	}

	/**
	 * Constructs the data packet with this event data. Message format for this
	 * event:
	 *  - 4 bytes : event type
	 *  - 4 bytes : scale 
	 *  - 4 bytes : center - x coordinate 
	 *  - 4 bytes : center - y coordinate
	 *  - 16 bytes total
	 * @return serialized data
	 */
	@Override
  public byte[] serialize() {

		byte[] data = new byte[16];
    Converter.intToByteArray(data, 0, getEventType());
    Converter.floatToByteArray(data, 4, _scale);
    Converter.floatToByteArray(data, 8, _center.getX());
    Converter.floatToByteArray(data, 12, _center.getY());
		return data;
	}
	
	//@override
	@Override
  public String toString() {
		return ("ZOOM Scale: " + _scale + ", Center: " + _center.toString());
	}

}
