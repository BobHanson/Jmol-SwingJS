package org.jmol.g3d;

/**
 * Note added 4/2015 BH:
 * 
 * Well, it turns out that the calculation of the intermediate pixel z value
 * in all methods involving rasterization of lines is incorrect and has been
 * incorrect since Jmol's inception. I noticed long ago that large triangles such as
 * produced in DRAW could incorrectly overlay/underlay other objects, but I could
 * never determine why. It turns out that the assumption that z-value is linear
 * across a line when perspectiveDepth is TRUE is simply incorrect. 
 * 
 * Basically, the function z(x) is non-linear. Treating it as simply a 
 * linear function results in oddities where lines and planes
 *  -- particularly created using DRAW and large distances -- appear
 * to be where they are not. 
 *  
 * Through Jmol 13.3.13 we had the standard linear relationship:
 * 
 *   z = (x - xa) / (xb - xa) * (zb - za) + za
 * 
 * I worked it out, and, amazingly, it should be
 * 
 *   z = (xb - xa) * za * zb / ((xb - x) * zb + (x - xa) * za)
 * 
 * Note that it is still true that when x = xb, z = zb 
 * and when x = xa, z = za, as required.
 * 
 * This equation can be rearranged to 
 * 
 *   z = a / (b - x)
 *   
 * where 
 * 
 *  a = (xb - xa) * za * (zb / (zb - za))
 *  
 * and
 * 
 *  b = (xb * zb - xa * za) / (zb - za)
 * 
 * These values must be floats, not integers, to work properly, because
 * these are extrapolations from long distances in some cases. So there is
 * considerable overhead there. It will take some experimentation to figure this
 * out.
 * 
 * The practical implications are for line, cylinder, and triangle drawing.
 * First-pass corrections are for axes and DRAW objects. They tend to be the
 * larger objects that result in the issue. 
 * 
 * Also affected is POV-Ray output, because right now POV-Ray is created using
 * perspective on and plotted as though it were orthographic, but although that
 * works in x and y, it does not work in z!
 * 
 */
public class PrecisionRenderer {

  protected float a, b;
  boolean isOrthographic;

  protected int getZCurrent(float a, float b, int x) {
    return Math.round(a == Float.MIN_VALUE ? b : isOrthographic ? a * x + b : a / (b - x));
  }

  
  protected void setRastABFloat(float xa, float za, float xb, float zb) {
    float zdif = (zb - za);
    float xdif = (xb - xa);
    if (zdif == 0 || xdif == 0) {
      a = Float.MIN_VALUE;
      b = za;
      return;
    }
    if (isOrthographic) {
      a = zdif / xdif;
      b = za - a * xa;
    } else {
      a = xdif * za * (zb / zdif);
      b = (xb * zb - xa * za) / zdif;
    }
  }

  protected void setRastAB(int xa, int za, int xb, int zb) {
    float zdif = (zb - za);
    float xdif = (xb - xa);
    if (xa == Float.MIN_VALUE || zdif == 0 || xdif == 0) {
      a = Float.MIN_VALUE;
      b = zb;
      return;
    }
    if (isOrthographic) {
      //
      //   z = (x - xa) / (xb - xa) * (zb - za) + za
      //
      //     = a * x + b
      //
      //   where 
      //   
      //       a = (zb - za) / (xb - xa) 
      //       b = za - a * xa
      //
      a = zdif / xdif;
      b = za - a * xa;
    } else {
//       z = a / (b - x)
//             
//           where 
//           
//       a = (xb - xa) * za * (zb / (zb - za))
//            
//           and
//           
//       b = (xb * zb - xa * za) / (zb - za)

      a = xdif * za * (zb / zdif);
      b = (xb * zb - xa * za) / zdif;
    }
  }

}
