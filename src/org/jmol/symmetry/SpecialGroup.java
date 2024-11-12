package org.jmol.symmetry;

import java.util.Map;

import org.jmol.util.SimpleUnitCell;

import javajs.util.Lst;
import javajs.util.PT;

public abstract class SpecialGroup extends SpaceGroup {
  
  /**
   * the space group and unit cell that this group relates to; may be null
   */
  protected Symmetry embeddingSymmetry;

  /**
   * @param sym the space group and unit cell that this group relates to; may be null
   * @param info ITA/ITE info for building this group 
   * @param type TYPE_PLANE, TYPE_LAYER, TYPE_ROD, TYPE_FRIEZE  
   */
  SpecialGroup(Symmetry sym, Map<String, Object> info, int type) {
    super(-1, null, true);
    embeddingSymmetry = sym;
    groupType = type;
    if (info == null)
      return;
    initSpecial(info);
  }

  private void initSpecial(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    Lst<Object> ops = (Lst<Object>) info.get("gp");
    for (int i = 0; i < ops.size(); i++) {
      addOperation((String) ops.get(i), 0, false);
    }
    setTransform((String) info.get("trm"));
    itaNumber = "" + info.get("sg");
    itaIndex = "" + info.get("set");
    specialPrefix  = getGroupTypePrefix(groupType);
    setHMSymbol((String) info.get("hm"));
    //setClegId(normalizeSpecialCleg((String) info.get("clegId")));
    setITATableNames(null, itaNumber, itaIndex, itaTransform);
  }

  void setTransform(String transform) {
    itaTransform = transform;
  }

// an initial idea that turned out to be more trouble than it was worth
//    private String normalizeSpecialCleg(String cleg) {
//    int pt = cleg.indexOf(":");
//    int itNo = getITNo(cleg, pt);
//    itNo = itNo%100;
//    return itNo + cleg.substring(pt);
//  }
//
  
  /**
   * A 2D spacegroup with two periodic directions, x and y (axes a and b).
   * 
   * https://en.wikipedia.org/wiki/List_of_planar_symmetry_groups
   * 
   */
  static class PlaneGroup extends SpecialGroup {
    protected PlaneGroup(Symmetry sym, Map<String, Object> info) {
      super(sym, info, SpaceGroup.TYPE_PLANE);
      nDim = 2;
      periodicity = 0x1 | 0x2;
    }
  
  
    /**
     * PlaneGroup rules -- only executed for the reference setting
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      int n = (itaNumber == null ? 0 : PT.parseInt(itaNumber));
      boolean toHex = false, isHex = false;
      toHex = (n != 0 && isHexagonalSG(n, null));
      isHex = (toHex && isHexagonalSG(-1, params));
      if (toHex && isHex) {
        allowSame = true;
      }
      ParamCheck pc = new ParamCheck(params, allowSame, false);
      pc.c = 0.5d;
      pc.alpha = pc.beta = 90;
      if (n > (allowSame ? 2 : 0)) {  
        if (toHex) {
            pc.b = pc.a;
            pc.gamma = 120;
        } else if (n >= 10) {
          // tetragonal
          pc.b = pc.a;
          pc.gamma = 90;
        } else if (n >= 3) {
          // orthorhombic
          pc.gamma = 90;
        }
      }  
      return pc.checkNew(params, newParams == null ? params : newParams);
    }
    
    @Override
    public boolean isHexagonalSG(int n, double[] params) {
      return (n < 1 ? SimpleUnitCell.isHexagonal(params)
          : n >= 13);
    }
    
  
  }

  /**
   * A 3D subperiodic group with two periodic directions, x and y (axes a and b).
   * 
   * The c axis will be perpendicular to the layer in most cases.
   * 
   * see https://iopscience.iop.org/article/10.1088/2053-1583/ad3e0c DOI
   * 10.1088/2053-1583/ad3e0c
   * 
   * Symmetry classification of 2D materials: layer groups versus space groups
   * 
   * Jingheng Fu1, Mikael Kuisma, Ask Hjorth Larsen, Kohei Shinohara, Atsushi
   * Togo and Kristian S Thygesen
   * 
   */
  static class LayerGroup extends SpecialGroup {
  
    protected LayerGroup(Symmetry sym, Map<String, Object> info) {
      super(sym, info, SpaceGroup.TYPE_LAYER);
      nDim = 3;
      periodicity = 0x3;
    }
  
    /**
     * LayerGroup rules
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      return checkCompatible(params, newParams, allowSame, 3, 8, 19, 49);
    }
  
    @Override
    public boolean isHexagonalSG(int n, double[] params) {
      return (n < 1 ? SimpleUnitCell.isHexagonal(params)
          : n >= 65);
    }
    
  
  }

  /**
   * A 1D periodic group with the periodic direction z (c axis).
   * 
   * x,y axes will be perpendicular to the linear direction.
   */
  static class RodGroup extends SpecialGroup {
  
    protected RodGroup(Symmetry sym, Map<String, Object> info) {
      super(sym, info, SpaceGroup.TYPE_ROD);
      nDim = 3;
      if (info != null)
        periodicity = setRodPeriodicityFromTrm(info);
    }
    
    /**
     * Set the rod group periodicity from the transformation string. This is
     * only done for data, so it is based on the simple look at the string.
     * 
     * Only for monoclinic groups (3-22).
     * 
     *  For example, for r/4 we have:
     * 
     * <pre>
     * 
     * a,b,c  b,-a,c    pc m 1 1 and pc 1 m 1
     * b,c,a  a,-c,b    pb 1 1 m and pb m 1 1 
     * -c,b,a -c,-a,b   pa 1 1 m and pa 1 m 1
     * 
     * </pre>
     * 
     * The location of "c" in the string, meaning "the reference c axis goes to
     * this position" tells us that for b,c,a, the reference "c" axis becomes the
     * "b" axis and the periodicity becomes 0x2.
     * 
     * @param info
     * @return 0x1, 0x2, 0x4 (a, b, or c periodicity)
     */
    private int setRodPeriodicityFromTrm(Map<String, Object> info) {
      int sg = ((Number) info.get("sg")).intValue();
      if (sg < 3 || sg > 22) {
        return 0x4;
      } 
      String trm = (String) info.get("trm");
      if (trm.endsWith("c")) {
        return 0x4;
      }
      return (trm.indexOf('c') < trm.indexOf(',') ? 0x1 : 0x2);       
    }

    /**
     * RodGroup rules
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      return checkCompatible(params, newParams, allowSame, 3, 8, 13, 23);
    }
  
    @Override
    public boolean isHexagonalSG(int n, double[] params) {
      return (n < 1 ? SimpleUnitCell.isHexagonal(params) : n >= 42);
    }
    
  }

  /**
   * A 2D spacegroup with one periodic direction, along a.
   */
  static class FriezeGroup extends SpecialGroup {
  
    protected FriezeGroup(Symmetry sym, Map<String, Object> info) {
      super(sym, info, SpaceGroup.TYPE_FRIEZE);
      nDim = 2;
      periodicity = 0x1;
    }
  
    /**
     * FriezeGroup rules
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      double a = params[0];
      double b = params[0];
      double c = -1;
      double alpha = 90;
      double beta = 90;
      double gamma = 90;
  
      boolean isNew = !(a == params[0] && b == params[1] && c == params[2]
          && alpha == params[3] && beta == params[4] && gamma == params[5]);
  
      if (newParams == null)
        newParams = params;
      newParams[0] = a;
      newParams[1] = b;
      newParams[2] = c;
      newParams[3] = alpha;
      newParams[4] = beta;
      newParams[5] = gamma;
      return isNew;
    }  
  }

  public boolean checkCompatible(double[] params, double[] newParams,
                                 boolean allowSame, int monoclinic_oblique,
                                 int monoclinic_orthogonal, int orthorhombic,
                                 int tetragonal) {
    int n = (itaNumber == null ? 0 : PT.parseInt(itaNumber));
    boolean toHex = (n != 0 && isHexagonalSG(n, null));
    boolean isHex = (toHex && isHexagonalSG(-1, params));
    if (toHex && isHex) {
      allowSame = true;
    }
    ParamCheck pc = new ParamCheck(params, allowSame, true);
    if (n > (allowSame ? 2 : 0)) {
      if (toHex) {
        pc.b = pc.a;
        pc.alpha = pc.beta = 90;
        pc.gamma = 120;
      } else if (n >= tetragonal) {
        pc.b = pc.a;
        if (pc.acsame && !allowSame)
          pc.c = pc.a * 1.5d;
        pc.alpha = pc.beta = pc.gamma = 90;
      } else if (n >= orthorhombic) {
        pc.alpha = pc.beta = pc.gamma = 90;
      } else if (n >= monoclinic_orthogonal) {
        pc.beta = 90; // ac angle is always 90
        if (groupType == TYPE_LAYER) {
          pc.gamma = 90;  // ab angle
        } else {
          pc.alpha = 90; //bc angle
        }
      } else if (n >= monoclinic_oblique) {
        pc.beta = 90; // ac angle is always 90
        if (groupType == TYPE_LAYER) {
          pc.alpha = 90; //bc angle
        } else {
          pc.gamma = 90; //ab angle
        }
      }
    }
    return pc.checkNew(params, newParams == null ? params : newParams);
  }

}