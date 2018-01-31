package com.sparshui.common.messages.events;

import com.sparshui.common.Event;
import com.sparshui.common.utils.Converter;

public class FlickEvent implements Event {
	
	
 // constructor coding error fixed by Bob Hanson for Jmol 11/29/2009
	
	private static final long serialVersionUID = -2305607021385835330L;		
//	private float _absx;
//	private float _absy;	
	
	//private Velocity[] runningVelocities;
	private float xDirection;
	private float yDirection;
	private float speedLevel;
	
	
	public FlickEvent() {
//		_absx = 0;
//		_absy = 0;
		//runningVelocities = null;
		xDirection = 0;
		yDirection = 0;
		speedLevel = 0;
	}
	
	/**
	 * 
	 * @param absx
	 * @param absy
	 */
	public FlickEvent(float absx, float absy) {
//		_absx = absx;
//		_absy = absy;
	}
	
	public FlickEvent(int _speedLevel, int _xDirection, int _yDirection){
		speedLevel = _speedLevel;
		xDirection = _xDirection;
		yDirection = _yDirection;
	}
	

	/**
	 * Constructs a flickEvent from a complete serialized version of the drag
	 * event.
	 *  - 4 bytes : dx 
	 *  - 4 bytes : dy 
	 *  - 8 bytes total
	 *  
   * @param data
   *            The byte array that represents a serialized Drag Event.
   */
  public FlickEvent(byte[] data) {
    if (data.length < 12) {
      // TODO add error handling
      System.err.println("Error constructing Flick Event.");
    } else {
      speedLevel = Converter.byteArrayToFloat(data, 0);
      xDirection = Converter.byteArrayToFloat(data, 4);
      yDirection = Converter.byteArrayToFloat(data, 8);
    }
  }
	
	public float getSpeedLevel() {
		return speedLevel;
	}

	public float getXdirection() {
		return xDirection;
	}
	public float getYdirection() {
		return yDirection;
	}


	//@override
	@Override
  public int getEventType() {
		return EventType.FLICK_EVENT;
	}

	//@override
	@Override
  public String toString() {
		String ret = "Flick Event";
		return ret;
	}

	/**
	 * Constructs the data packet with this event data. Message format for this
	 * event: 
	 * 
	 * - 4 bytes : EventType 
	 * - 4 bytes : SpeedLevel 
	 * - 4 bytes : X Direction
	 * - 4 bytes : Y Direction
	 * - 16 bytes total
   * @return serialized data
	 */
	@Override
  public byte[] serialize() {
		byte[] data = new byte[16];
    Converter.intToByteArray(data, 0, getEventType());
    Converter.floatToByteArray(data, 4, speedLevel);
    Converter.floatToByteArray(data, 8, xDirection);
    Converter.floatToByteArray(data, 12, yDirection);
		return data;
	}

}
