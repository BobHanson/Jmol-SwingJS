package org.jmol.minimize;

public abstract class MinObject {
  public int[] data;
  public int type;
  public Integer key;
  public double[] ddata;
  
  @Override
  public String toString() {
    return type + " " + data[0] + "," + data[1] 
        + (data.length > 2 ? "," + data[2] + "," + data[3] : "")
        + " " + decodeKey(key);
  }
  
  public static Integer getKey(int type, int a1, int a2, int a3, int a4) {
    // 2^7 is 128; the highest mmff atom type is 99; 
    // key rolls over into negative numbers for a4 > 63
    //System.out.println("getting key for " + type + ": " + a1 + " " + a2 + " " + a3 + " " + a4);
    return Integer.valueOf((((((((a4 << 7) + a3) << 7) + a2) << 7) + a1) << 4) + type);
  }

  public static String decodeKey(Integer key) {
    if (key == null)
      return null;
    int i = key.intValue();
    int type = i & 0xF;
    i >>= 4;
    int a = i & 0x7F;
    i >>= 7;
    int b = i & 0x7F;
    i >>= 7;
    int c = i & 0x7F;
    i >>= 7;
    int d = i & 0x7F;
    return (type < 0 ? type + ": " : "") 
        + (a < 10 ? "  " : " ") + a 
        + (b < 10 ? "  " : " ") + b 
        + (c < 10 ? "  " : " ") + c 
        + (d > 120 ? "" : (d < 10 ? "  " : " ") + d);
  }

}
