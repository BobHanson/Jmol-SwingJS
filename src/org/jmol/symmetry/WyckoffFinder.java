package org.jmol.symmetry;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.SimpleUnitCell;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.V3d;

public class WyckoffFinder {  
  static class WyckoffPos {
    final static int TYPE_POINT = 1;
    final static int TYPE_LINE  = 2;
    final static int TYPE_PLANE = 3;
    
    private P3d point;
    private V3d line;
    private P4d plane;
    private int type;
    String xyz;
    private static V3d vtemp1 = new V3d();

    public WyckoffPos(String xyz) {
      create(xyz);
    }

    private void create(String p) {
      String[] xyz = PT.split(p, ",");
      int nxyz = 0;
      for (int i = 0; i < 3; i++) {
        if (xyz[i].indexOf('x') >= 0) {
          nxyz |= 1;
        }   
        if (xyz[i].indexOf('y') >= 0) {
          nxyz |= 2;
        }   
        if (xyz[i].indexOf('z') >= 0) {
          nxyz |= 4;
        }   
      }
      P3d v1, v2, v3;
      switch (nxyz) {
      case 0:
        type = TYPE_POINT;
        point = toPoint(p);
        break;
      case 1:
      case 2:
      case 4:
        type = TYPE_LINE;
        v1 = ptFor(p, 0,0,0);
        v2 = ptFor(p, 1d,1.27d,1.64d);
        v2.sub2(v2, v1);
        v2.normalize();
        point = P3d.newP(v1);
        SimpleUnitCell.unitizeDimRnd(3, point, SimpleUnitCell.SLOPDP);
        line = V3d.newV(v2);
        break;
      case 3:
      case 5:
      case 6:
        type = TYPE_PLANE;
        v1 = ptFor(p, 0,0,0);
        v2 = ptFor(p, 1.23d,1.47d,1.86d);
        v3 = ptFor(p, 0.1d,0.2d,0.3d);
        plane = MeasureD.getPlaneThroughPoints(v1, v2, v3, null, null, new P4d());
        break;
      case 7:
        // general position
        break;
      }
    }

    private static P3d ptFor(String p, double x, double y, double z) {

      // create a reference point for x,x,1/4, for example

      String[] v = PT.split(p, ",");
      double a = decodeXYZ(v[0], x, y, z);
      double b = decodeXYZ(v[1], x, y, z);
      double c = decodeXYZ(v[2], x, y, z);
      return P3d.new3(a, b, c);
    }

    private static double decodeXYZ(String s, double x, double y, double z) {
      // -x+1/2 => +-x+1/2 => ["","-x","1/2"] 
      // 2x+-3  =>  ["2*x","-3"]
      // x-1/2
      // 2/3x => "2/3x"
      // x-y
      s = PT.rep(s, "-", "+-");
      s = PT.rep(s, "x", "*x");
      s = PT.rep(s, "y", "*y");
      s = PT.rep(s, "z", "*z");
      s = PT.rep(s, "-*", "-");
      s = PT.rep(s, "+*", "+");
      double r = 0;
      String[] parts = PT.split(s, "+");
      for (int p = parts.length; --p >= 0;) {
        s = parts[p];
        if (s.length() == 0)
          continue;
        if (s.indexOf('.') >= 0) {
          r += PT.parseDouble(s);
          continue;
        }
        double v = 0;
        int f2 = 0;
        int i0 = 0;
        double f = 1;
        switch (s.charAt(0)) {
        case '-':
          f = -1;
          //$FALL-THROUGH$
        case '*':
          i0++;
          break;
        }
        for (int i = s.length(); --i >= i0;) {
          char c = s.charAt(i);
          switch (c) {
          case 'x':
            v = x;
            break;
          case 'y':
            v = y;
            break;
          case 'z':
            v = z;
            break;
          case '/':
            v = 1 / v;
            //$FALL-THROUGH$
          case '*':
            f *= v;
            v = 0;
            break;
          default:
            int u = "0123456789".indexOf(c);
            if (u < 0)
              System.err.println("WH ????");
            if (v == 0) {
              v = u;
            } else {
              f2 = (f2 == 0 ? 10 : f2 * 10);
              v += f2 * u;
            }
            break;
          }
        }
        r += f * v;
      }
      return r;
    }
    //
    //    static {
    //      System.out.println(decodeXYZ("x", 1,2,3) == 1);
    //      System.out.println(decodeXYZ("x", 1,2,3) == 1);
    //      System.out.println(decodeXYZ("x+y", 1,2,4) == 3);
    //      System.out.println(decodeXYZ("x+y", 1,2,4) == 3);
    //      System.out.println(decodeXYZ("-x-y", 1,2,3) == -3);
    //      System.out.println(decodeXYZ("-x-y", 1,2,3) == -3);
    //      System.out.println(decodeXYZ("z+1/2", 1,2,3) == 3.5);
    //      System.out.println(decodeXYZ("z+1/2", 1,2,3) == 3.5);
    //      System.out.println(decodeXYZ("x-1/2", 1,2,3) == 0.5);
    //      System.out.println(decodeXYZ("x-1/2", 1,2,3) == 0.5);
    //      System.out.println(decodeXYZ("1/2z", 1,2,3) == 1.5);
    //      System.out.println(decodeXYZ("1/2z", 1,2,3) == 1.5);
    //      System.out.println(decodeXYZ("-1/3z+0.27778", 1,2,3) == -1./3*3+0.27778);
    //      System.out.println(decodeXYZ("-1/3z+0.27778", 1,2,3) == -1./3*3+0.27778);
    //      
    //    }

    boolean contains(UnitCell uc, P3d p, P3d[] centerings) {
      double slop = uc.getPrecision();
      if (containsPt(p, slop))
        return true;
      P3d pc = new P3d();
      if (centerings != null)
        for (int i = centerings.length; --i >= 0;) {
          pc.add2(p, centerings[i]);
          uc.unitize(pc);
          if (containsPt(pc, slop))
            return true;
        }
      return false;
    }
    
    private boolean containsPt(P3d p, double slop) {      
      double d = 1;
      switch (type) {
      case TYPE_POINT:
        d = p.distance(point);
        break;
      case TYPE_LINE:        
        P3d p1 = P3d.newP(p);
        MeasureD.projectOntoAxis(p1, point, line, vtemp1);
        d = p.distance(p1);
        break;
      case TYPE_PLANE:
        d = MeasureD.distanceToPlane(plane, p);
        break;
      }
      return approx0(d, slop);
    }

    void set(P3d p) {      
      switch (type) {
      case TYPE_POINT:
        p.setT(point);
        break;
      case TYPE_LINE:        
        MeasureD.projectOntoAxis(p, point, line, vtemp1);
        break;
      case TYPE_PLANE:
        MeasureD.getPlaneProjection(p, plane, vtemp1, vtemp1);
        p.setT(vtemp1);
        break;
      }
    }

    private static boolean approx0(double d, double slop) {
      return (Math.abs(d) < slop);
    }

  }

  private final static Map<String, WyckoffFinder> helpers = new Hashtable<String, WyckoffFinder>();
  private Lst<Object> positions;
  private P3d[] centerings;
  private static WyckoffFinder nullHelper;
 
  public WyckoffFinder() {
    // only used for dynamic instantiation 
  }

  /**
   * effectively static, as this is only accessed from the static helper instance
   * 
  * @param vwr
   * @param sgname
   * @return helper
   */
  @SuppressWarnings("unchecked")
  WyckoffFinder getWyckoffFinder(Viewer vwr, String sgname) {
    WyckoffFinder helper = helpers.get(sgname);
    if (helper == null) {
      int itno = PT.parseInt(PT.split(sgname, ":")[0]);
      if (itno >= 1 && itno <= 230) {
        Map<String, Object> resource = getResource(vwr,
            "ita_" + itno + ".json");
        if (resource != null) {
          Lst<Object> its = (Lst<Object>) resource.get("its");
          if (its != null) {
            for (int i = its.size(); --i >= 0;) {
              Map<String, Object> map = (Map<String, Object>) its.get(i);
              if (sgname.equals(map.get("itaFull"))) {
                helpers.put(sgname, helper = new WyckoffFinder(map));
                return helper;
              }
            }
          }
        }
      }
    }
    if (helper == null) {
      if (nullHelper == null)
        nullHelper = new WyckoffFinder(null);
      helpers.put(sgname, nullHelper);
    }
    return helper;
  }
 
  @SuppressWarnings("unchecked")
  private WyckoffFinder(Map<String, Object> map) {
    if (map != null) {
      Map<String, Object> wpos = (Map<String, Object>) map.get("wpos");
      positions = (Lst<Object>) wpos.get("pos");
      Lst<Object> cent = (Lst<Object>) wpos.get("cent");
      if (cent != null) {
        centerings = new P3d[cent.size()];
        for (int i = cent.size(); --i >= 0;) {
          centerings[i] = toPoint((String) cent.get(i));
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  String getWyckoffPosition(UnitCell uc, P3d p) {
    if (positions == null)
      return "?";
    for (int i = positions.size(); --i >= 0;) {
      Map<String, Object> map = (Map<String, Object>) positions.get(i);
      if (i == 0) {
        // general
        return (String) map.get("label");
      }
      Lst<Object> coords = (Lst<Object>) map.get("coord");
      for (int c = 0, n = coords.size(); c < n; c++) {
        if (getWyckoffCoord(coords, c).contains(uc, p, centerings)) {
          return (String) map.get("label");    
        }
      }      
    }
    // not possible
    return "?";
  }

  public P3d findPositionFor(P3d p, String letter) {
    if (positions == null)
      return null;
    for (int i = positions.size(); --i >= 0;) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) positions.get(i);
      if (map.get("label").equals(letter)) {
        @SuppressWarnings("unchecked")
        Lst<Object> coords = (Lst<Object>) map.get("coord");
        if (coords != null)
          getWyckoffCoord(coords, 0).set(p);
        return p;
     }
    }
    return null;
  }


  
  private static WyckoffPos getWyckoffCoord(Lst<Object> coords, int c) {
    Object coord = coords.get(c);
    if (coord instanceof String) {
      coords.set(c, coord = new WyckoffPos((String) coord));
    }
    return (WyckoffPos) coord;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getResource(Viewer vwr, String resource) {
    try {
      BufferedReader r = FileManager.getBufferedReaderForResource(vwr, this,
          "org/jmol/symmetry/", "sg/json/" + resource);
      String[] data = new String[1];
      if (Rdr.readAllAsString(r, Integer.MAX_VALUE, false, data, 0)) {
        return (Map<String, Object>) new JSJSONParser().parse(data[0], true);
      }
    } catch (Throwable e) {
      System.err.println(e.getMessage());
    }
    return null;
  }

  static P3d toPoint(String xyz) {
    String[] s = PT.split(xyz, ",");
    return P3d.new3(PT.parseDoubleFraction(s[0]), PT.parseDoubleFraction(s[1]), PT.parseDoubleFraction(s[2]));
  }

}
