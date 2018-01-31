package com.sparshui.common.messages.events;

import com.sparshui.common.Event;
import com.sparshui.common.Location;
import com.sparshui.common.utils.Converter;

public class RotateEvent implements Event {
	private static final long serialVersionUID = -5467788080845086125L;
	
	/**
	 * Rotation of this component in radians
	 */
	private float _rotation;
	private Location _center;
  private long _time;
  
	public RotateEvent() {
		_rotation = 0;
		_center = new Location();
	}
	
	public RotateEvent(float rotation, Location center, long time) {
		_rotation = rotation;
		_center = center;
		_time = time;
	}
	
	/**
	 * Constructs a new Rotate Event based on a serial representation
	 * of a rotate event.
	 *  - 4 bytes : rotation
	 *  - 4 bytes : center - x coordinate
	 *  - 4 bytes : center - y coordinate
	 *  - 12 bytes total
   *  
   * @param data
   */
  public RotateEvent(byte[] data) {
    if (data.length < 12) {
      // TODO add error handling
      System.err.println("Error constructing Rotate Event.");
      _rotation = 0;
      _center = new Location(0, 0);
    } else {
      _rotation = Converter.byteArrayToFloat(data, 0);
      _center = new Location(Converter.byteArrayToFloat(data, 4),
          Converter.byteArrayToFloat(data, 8));
    }
  }
	

	//@override
	@Override
  public int getEventType() {
		return EventType.ROTATE_EVENT; 
	}
	
	/**
	 * Constructs the data packet with this event data. Message format for this
	 * event:
	 *  - 4 bytes : event type 
	 *  - 4 bytes : rotation
	 *  - 4 bytes : center - x coordinate
	 *  - 4 bytes : center - y coordinate
	 *  - 16 bytes total
   * @return serialized data
	 */
	@Override
  public byte[] serialize() {
		byte[] data = new byte[16];
    Converter.intToByteArray(data, 0, getEventType());
    Converter.floatToByteArray(data, 4, _rotation);
    Converter.floatToByteArray(data, 8, _center.getX());
    Converter.floatToByteArray(data, 12, _center.getY());
		return data;
	}

	//@override
	@Override
  public String toString() {
		return ("Rotate Event - Rotation: " + _rotation + ", Center: " + _center.toString());
	}
	
	public float getRotation() {
		return _rotation;
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
  

}
