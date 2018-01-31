package com.sparshui.common;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.Serializable;

import javajs.util.V3;

/**
 * Represents a 2D location with float values.
 * 
 * @author Jay Roltgen
 */
public class Location implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3472243250219991476L;
	private float _x;
	private float _y;

	/**
	 * Cosntruct a default location.  Values are initialized
	 * as the coordinates (0, 0).
	 */
	public Location() {
		_x = 0;
		_y = 0;
	}
	
	/**
	 * Construct a specific location.
	 * @param x
	 * 		The x coordinate value of the location.
	 * @param y
	 * 		The y coordinate value of the location.
	 */
	public Location(float x, float y) {
		_x = x;
		_y = y;
	}
	
	/**
	 * 
	 * @return
	 * 		The x coordinate value.
	 */
	public float getX() {
		return _x;
	}
	
	/**
	 * 
	 * @return
	 * 		The y coordinate value.
	 */
	public float getY() {
		return _y;
	}
	
	@Override
  public String toString() {
		return "x = " + _x + ", y = " + _y 
		        + (_x < 1 && _x > 0 ? "(" 
		        + pixelLocation(this).getX() + " " + pixelLocation(this).getY() + ")"
		    : "");
	}
	

  public float getDistance(Location location) {
    float dx, dy;
    return (float) Math.sqrt((dx = _x - location._x) * dx + (dy = _y - location._y) * dy);
  }

  public V3 getVector(Location location) {
    return V3.new3(location._x - _x, location._y - _y, 0);  
  }
  
  public static Location getCenter(Location a, Location b) {
    return getCentroid(a, b, 0.5f);
  }

  /**
   * get weighted average location. w = 0 --> all a; w = 1 --> all b
   * 
   * @param a
   * @param b
   * @param w
   * @return Location
   */
  public static Location getCentroid(Location a, Location b, float w) {
    float w1 = 1 - w;
    return new Location(a._x * w1 + b._x * w, a._y * w1 + b._y * w);
  }

  static final Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();

  public static Location pixelLocation(Location location) {
    return location == null ? null : new Location(location.getX()
        * screenDim.width, location.getY() * screenDim.height);
  }

  public static Location screenLocation(Location location) {
    return (location == null ? null : new Location(location.getX()
        / screenDim.width, location.getY() / screenDim.height));
  }

}
