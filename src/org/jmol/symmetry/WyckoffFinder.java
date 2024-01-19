package org.jmol.symmetry;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.MeasureD;
import javajs.util.P3d;
import javajs.util.P4d;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3d;
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
    
    private String label;
    
    String xyz;
    private final static P3d p1 = new P3d(), 
    		p2 = new P3d(), p3 = new P3d(), pc = new P3d();
    private final static V3d vt = new V3d();
    String thisCentering = "";
    
    public WyckoffPos(String xyz, String label) {
      this.xyz = xyz;
      this.label = label;
      create(xyz);
      
    }

    private final static int X = 1;
    private final static int Y = 2;
    private final static int Z = 4;

    private void create(String p) {
      String[] xyz = PT.split(p, ",");

      int nxyz = 0;
      for (int i = 0; i < 3; i++) {
        if (xyz[i].indexOf('x') >= 0) {
          nxyz |= X;
        }
        if (xyz[i].indexOf('y') >= 0) {
          nxyz |= Y;
        }
        if (xyz[i].indexOf('z') >= 0) {
          nxyz |= Z;
        }
      }

      switch (nxyz) {
      case 0:
        type = TYPE_POINT;
        point = toPoint(p);
        break;
      case X:
      case Y:
      case Z:

        // just one 
        type = TYPE_LINE;
//        getMatrix(p, d);
        ptFor(0.19d, 0.53d, 0.71d, p1);
        ptFor(0.51d, 0.27d, 0.64d, p2);
        p2.sub2(p2, p1);
        p2.normalize();
        point = P3d.newP(p1);
//        SimpleUnitCell.unitizeDimRnd(3, point, SimpleUnitCell.SLOPDP);
        line = V3d.newV(p2);
//        System.out.println("draw id '" + label+" "+ p + "' width 0.1 vector @{" + point + ".xyz} @{" + line + ".xyz}");
        break;
      case X|Y:
      case X|Z:
      case Y|Z:
        type = TYPE_PLANE;
 //       getMatrix(p, d);
        ptFor(0.19d, 0.51d, 0.73d, p1);
        ptFor(0.23d, 0.47d, 0.86d, p2);
        ptFor(0.1d, 0.2d, 0.3d, p3);
        plane = MeasureD.getPlaneThroughPoints(p1, p2, p3, null, null,
            new P4d());
        break;
      case 7:
        // general position
        break;
      }
    }

    private void ptFor(double x, double y, double z, T3d p) {
      String[] v = PT.split(xyz, ",");
      if (parts != null) {
        parts[0] = getParts(v[0]);
        parts[1] = getParts(v[1]);
        parts[2] = getParts(v[2]);
      }
      p.set(decodeXYZ(parts[0], x, y, z), decodeXYZ(parts[1], x, y, z), decodeXYZ(parts[2], x, y, z));
    }
    
    private String[] getParts(String s) {
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
      return PT.split(s, "+");
    }

    private String[][] parts = new String[3][];

    private static double decodeXYZ(String[] parts, double x, double y, double z) {
      double r = 0;
      for (int p = parts.length; --p >= 0;) {
        String s = parts[p];
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
            f2 = 1;
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
    
//        static {
//          System.out.println(decodeXYZ("11/12", 1,2,3) == 11/12d);
//          System.out.println(decodeXYZ("x", 1,2,3) == 1);
//          System.out.println(decodeXYZ("x+y", 1,2,4) == 3);
//          System.out.println(decodeXYZ("x+y", 1,2,4) == 3);
//    //      System.out.println(decodeXYZ("-x-y", 1,2,3) == -3);
//    //      System.out.println(decodeXYZ("-x-y", 1,2,3) == -3);
//    //      System.out.println(decodeXYZ("z+1/2", 1,2,3) == 3.5);
//    //      System.out.println(decodeXYZ("z+1/2", 1,2,3) == 3.5);
//    //      System.out.println(decodeXYZ("x-1/2", 1,2,3) == 0.5);
//    //      System.out.println(decodeXYZ("x-1/2", 1,2,3) == 0.5);
//    //      System.out.println(decodeXYZ("1/2z", 1,2,3) == 1.5);
//    //      System.out.println(decodeXYZ("1/2z", 1,2,3) == 1.5);
//    //      System.out.println(decodeXYZ("-1/3z+0.27778", 1,2,3) == -1./3*3+0.27778);
//    //      System.out.println(decodeXYZ("-1/3z+0.27778", 1,2,3) == -1./3*3+0.27778);
//    //      
//        }

    boolean contains(UnitCell uc, P3d p, WyckoffFinder w) {
      double slop = uc.getPrecision();
      thisCentering = null;
      if (containsPt(uc, p, slop, true))
        return true;
      if (w.centerings != null)
        for (int i = w.centerings.length; --i >= 0;) {
          pc.add2(p, w.centerings[i]);
          uc.unitize(pc);
          if (containsPt(uc, pc, slop, true)) {
            thisCentering = w.centeringStr[i];
            return true;
          }
        }
      return false;
    }

    private boolean containsPt(UnitCell uc, P3d p, double slop,
                               boolean doLatticeCheck) {
      if (doLatticeCheck) {
        for (int i = -2; i < 3; i++) {
          for (int j = -2; j < 3; j++) {
            for (int k = -2; k < 3; k++) {
              p3.set(i, j, k);
              p3.add(p);
              if (containsPt(uc, p3, slop, false)) {
                System.out.println(label + " " + xyz + " found for " + i + " "+ j + " "+ k);
                return true;
              }
            }
          }
        }
        return false;
      }
      double d = 1;
      switch (type) {
      case TYPE_POINT:
        // will be unitized
        d = point.distance(p);
        p1.setT(point);
        break;
      case TYPE_LINE:
        p1.setT(p);
        MeasureD.projectOntoAxis(p1, point, line, vt);
        p2.sub2(p1, p);
//        if (label.equals("b"))
//          System.out.println("contains? " + label + " " + xyz + "\n" + p + " "
//              + p1 + "\np2=" + p2);
        d = p1.distance(p);
        break;
      case TYPE_PLANE:
        p1.setT(p);
        d = Math.abs(MeasureD.getPlaneProjection(p1, plane, vt, vt));
        break;
      }
      if (d < slop)
        System.out.println("success! " + label + " " + xyz + " " + d + " " + p);
      return d < slop;
    }

    void set(P3d p) {      
      switch (type) {
      case TYPE_POINT:
        p.setT(point);
        break;
      case TYPE_LINE:        
        MeasureD.projectOntoAxis(p, point, line, vt);
        break;
      case TYPE_PLANE:
        MeasureD.getPlaneProjection(p, plane, vt, vt);
        p.setT(vt);
        break;
      }
    }

//    private static boolean approx0(double d, double slop) {
//      return (Math.abs(d) < slop);
//    }
//    
    SB asString(SB sb, boolean withCentering) {
      if (sb == null)
        sb = new SB();
      wrap(xyz, sb);
      if (withCentering && thisCentering != null) {
        sb.appendC('+');
        wrap(thisCentering, sb);
      }
      return sb;
    }

    @Override
    public String toString() {
      return asString(null, false).toString();
    }
  }

  private final static Map<String, WyckoffFinder> helpers = new Hashtable<String, WyckoffFinder>();
  private Lst<Object> positions;
  int npos, ncent;
  protected P3d[] centerings;
  protected String[] centeringStr;
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
      npos = positions.size();
      Lst<Object> cent = (Lst<Object>) wpos.get("cent");
      if (cent != null) {
        ncent = cent.size();
        centeringStr = new String[ncent];
        centerings = new P3d[ncent];
        for (int i = ncent; --i >= 0;) {
          String s = (String) cent.get(i);
          centeringStr[i] = s;
          centerings[i] = toPoint(s);
        }
      }
    }
  }

  /**
   *  positive numbers will be label characters or '*'
   */
  public final static int WYCKOFF_RET_LABEL = -1;
  public final static int WYCKOFF_RET_COORD = -2;
  public final static int WYCKOFF_RET_COORDS = -3;
  public final static int WYCKOFF_RET_COORDS_ALL = '*';
  
  /**
   * Get string information about this position or space group
   * 
   * @param uc
   * @param p
   * @param returnType label, coord, label*, or *
   * @return label or coordinate or label with centerings and coordinates or full list for space group
   */
  @SuppressWarnings("unchecked")
  String getWyckoffPosition(UnitCell uc, P3d p, int returnType) {
    switch (returnType) {
    case '*':
      // all
      SB sb = new SB();
      getCenteringStr(-1, sb);
      for (int i = npos; --i >= 0;) {
        Map<String, Object> map = (Map<String, Object>) positions.get(i);
        String label = (String) map.get("label");
        sb.appendC('\n').append(label);
        if (i == 0) {
          // general
          sb.append(" (x,y,z)");
        } else {
          getList((Lst<Object>) map.get("coord"), label, sb);
        }
      }
      return sb.toString();
    case WYCKOFF_RET_LABEL:
    case WYCKOFF_RET_COORD:
    case WYCKOFF_RET_COORDS:
      for (int i = positions.size(); --i >= 0;) {
        Map<String, Object> map = (Map<String, Object>) positions.get(i);
        String label = (String) map.get("label");
        if (i == 0) {
          // general
          switch (returnType) {
          case WYCKOFF_RET_LABEL:
            return label;
          case WYCKOFF_RET_COORD:
            return "(x,y,z)";
          case WYCKOFF_RET_COORDS:
            return map.get("label") + "  (x,y,z)";
          }
        }
        Lst<Object> coords = (Lst<Object>) map.get("coord");
        for (int c = 0, n = coords.size(); c < n; c++) {
          WyckoffPos coord = getWyckoffCoord(coords, c, label);
//          System.out.println(label+ " " + coord + " " +  c + " " + p);
          if (coord.contains(uc, p, this)) {
            switch (returnType) {
            case WYCKOFF_RET_LABEL:
              return label;
            case WYCKOFF_RET_COORD:
              return coord.asString(null, true).toString();
            case WYCKOFF_RET_COORDS:
              SB sbc = new SB();
              sbc.append(label).appendC(' ');
              getCenteringStr(-1, sbc).appendC(' ');
              getList(coords, label, sbc);
              return sbc.toString();
            }
          }
        }
      }
      break;
    default:
      String letter = "" + (char) returnType;
      for (int i = npos; --i >= 0;) {
        Map<String, Object> map = (Map<String, Object>) positions.get(i);
        if (map.get("label").equals(letter)) {
          return (i == 0 ? "(x,y,z)" : getList((Lst<Object>) map.get("coord"), letter, null).toString());
        }
      }
      break;
    }
    return "?";
  }

  private SB getCenteringStr(int n, SB sb) {
    if (sb == null)
      sb = new SB();
    if (ncent == 0)
      return sb;
    if (n >= 0) {
      sb.appendC('+');
      return wrap(centeringStr[n], sb);
    }
    for (int i = 0; i < ncent; i++) {
      sb.appendC('+');
      wrap(centeringStr[i], sb);
    }
    return sb;
  }

  protected static SB wrap(String xyz, SB sb) {
    return sb.appendC('(').append(xyz).appendC(')');
  }

  private static SB getList(Lst<Object> coords, String letter, SB sb) {
    if (sb == null)
      sb = new SB();
    for (int c = 0, n = coords.size(); c < n; c++) {
      WyckoffPos coord = getWyckoffCoord(coords, c, letter);
      sb.append(" ");
      coord.asString(sb, false);
    }
    return sb;
  }

  public P3d findPositionFor(P3d p, String letter) {
    if (positions == null)
      return null;
    for (int i = npos; --i >= 0;) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) positions.get(i);
      if (map.get("label").equals(letter)) {
        @SuppressWarnings("unchecked")
        Lst<Object> coords = (Lst<Object>) map.get("coord");
        if (coords != null)
          getWyckoffCoord(coords, 0, letter).set(p);
        return p;
     }
    }
    return null;
  }


  
  
  private static WyckoffPos getWyckoffCoord(Lst<Object> coords, int c, String label) {
    Object coord = coords.get(c);
    if (coord instanceof String) {
      coords.set(c, coord = new WyckoffPos((String) coord, label ));
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
