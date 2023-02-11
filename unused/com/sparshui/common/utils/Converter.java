package com.sparshui.common.utils;

/**
 * Contains conversion methods that are needed by several elements of the
 * Gesture Server.
 * 
 *
 * adapted by Bob Hanson for Jmol 11/29/2009 (added several useful methods)
 *  
 * @author Jay Roltgen
 * 
 */
public class Converter {

  /**
	 * Converts an integer intBits into a byte array.
	 * @param intBits The integer in network byte order.
	 * @return A byte array in network byte order.
	 */
	public static byte[] intToByteArray(int intBits) {
		byte[] ret = new byte[4];
		
		ret[0] = (byte) ((intBits & 0xff000000) >> 24);
		ret[1] = (byte) ((intBits & 0x00ff0000) >> 16);
		ret[2] = (byte) ((intBits & 0x0000ff00) >> 8);
		ret[3] = (byte) ((intBits & 0x000000ff) >> 0);
		
		return ret;
	}
	
  public static void intToByteArray(byte[] data, int i, int idata) {
    data[i++] = (byte) ((idata & 0xff000000) >> 24);
    data[i++] = (byte) ((idata & 0x00ff0000) >> 16);
    data[i++] = (byte) ((idata & 0x0000ff00) >> 8);
    data[i] = (byte) ((idata & 0x000000ff) >> 0);
  }
  
  /**
	 * Converts a byte array to an integer.
	 * @param b 
	 * 		A byte array representing an integer in network
	 * 		byte order.
	 * @return
	 * 		An integer that was created from the byte array.
	 */
	public static int byteArrayToInt(byte[] b) {
    return ((b[0] << 24) & 0xFF000000)
    | ((b[1] << 16) & 0x00FF0000)
    | ((b[2] << 8) & 0x0000FF00)
    | (b[3] & 0x000000FF);
  }

  public static int byteArrayToInt(byte[] b, int i) {
    return ((b[i++] << 24) & 0xFF000000)
        | ((b[i++] << 16) & 0x00FF0000)
        | ((b[i++] << 8) & 0x0000FF00)
        | (b[i] & 0x000000FF);
  }

  public static void floatToByteArray(byte[] data, int i, float fdata) {
    intToByteArray(data, i, Float.floatToIntBits(fdata));
  }

  public static float byteArrayToFloat(byte[] data, int i) {
    return Float.intBitsToFloat(byteArrayToInt(data, i));
  }

  public static void longToByteArray(byte[] data, int i, long ldata) {
    intToByteArray(data, i, ((int) (ldata >> 32)));
    intToByteArray(data, i + 4, ((int) (ldata & 0xFFFFFFFF)));
  }
  
  public static long byteArrayToLong(byte[] data, int i) {
    return (((long) byteArrayToInt(data, i)) << 32)
        | byteArrayToInt(data, i + 4) & 0xFFFFFFFFL;
  }

  public static String byteArrayToString(byte[] bytes) {
    char[] chars = new char[bytes.length];
    for (int i = 0; i < chars.length; i++)
      chars[i] = (char) bytes[i];
    return new String(chars);
  }

  public static byte[] stringToByteArray(String s) {
    // no unicode here -- just for simple class names
    char[] chars = s.toCharArray();
    byte[] bytes = new byte[s.length()];
    for (int i = 0; i < chars.length; i++)
      bytes[i] = (byte) chars[i];
    return bytes;
  }

}
