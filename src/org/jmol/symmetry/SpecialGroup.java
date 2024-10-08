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
    itaTransform = (String) info.get("trm");
    itaNumber = "" + info.get("sg");
    itaIndex = "" + info.get("set");
    setHMSymbol((String) info.get("hm"));
    specialPrefix  = getGroupTypePrefix(groupType);
    //setClegId(normalizeSpecialCleg((String) info.get("clegId")));
    setITATableNames(null, itaNumber, itaIndex, itaTransform);
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
     * PlaneGroup rules
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      if (newParams == null)
        newParams = params;
      double a = params[0];
      double b = params[1];
      double c = -1;
      double alpha = 90;
      double beta = 90;
      double gamma = params[5];
      int n = (itaNumber == null ? 0 : PT.parseInt(itaNumber));
  
      
      boolean toHex = false, isHex = false;
      toHex = (n != 0 && isHexagonalSG(n, null));
      isHex = (toHex && isHexagonalSG(-1, params));
      if (toHex && isHex) {
        allowSame = true;
      }
  
      if (n > (allowSame ? 2 : 0)) {
  
        boolean absame = SimpleUnitCell.approx0(a - b);
  
        if (!allowSame) {
          // make a, b distinct
          if (a > b) {
            double d = a;
            a = b;
            b = d;
          }
          absame = SimpleUnitCell.approx0(a - b);
          if (absame)
            b = a * 1.2d;
  
          if (SimpleUnitCell.approx0(gamma - 90)) {
            gamma = 110;
          }
        }
        
        if (toHex) {
            b = a;
            alpha = beta = 90;
            gamma = 120;
        } else if (n >= 10) {
          // tetragonal
          b = a;
          gamma = 90;
        } else if (n >= 3) {
          // orthorhombic
          gamma = 90;
        }
      }
  
      boolean isNew = !(a == params[0] && b == params[1] && c == params[2]
          && alpha == params[3] && beta == params[4] && gamma == params[5]);
  
      newParams[0] = a;
      newParams[1] = b;
      newParams[2] = c;
      newParams[3] = alpha;
      newParams[4] = beta;
      newParams[5] = gamma;
      return isNew;
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
      periodicity = 0x1 | 0x2;
    }
  
    /**
     * LayerGroup rules
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      if (newParams == null)
        newParams = params;
      double a = params[0];
      double b = params[1];
      double c = params[2];
      double alpha = params[3];
      double beta = params[4];
      double gamma = params[5];
      int n = (itaNumber == null ? 0 : PT.parseInt(itaNumber));
  
      
      boolean toHex = false, isHex = false;
      toHex = (n != 0 && isHexagonalSG(n, null));
      isHex = (toHex && isHexagonalSG(-1, params));
      if (toHex && isHex) {
        allowSame = true;
      }
  
      if (n > (allowSame ? 2 : 0)) {
  
        boolean absame = b > 0 && SimpleUnitCell.approx0(a - b);
        boolean bcsame = c > 0 && SimpleUnitCell.approx0(b - c);
        boolean acsame = c > 0 && SimpleUnitCell.approx0(c - a);
        boolean albesame = SimpleUnitCell.approx0(alpha - beta);
        boolean begasame = SimpleUnitCell.approx0(beta - gamma);
        boolean algasame = SimpleUnitCell.approx0(gamma - alpha);
  
        if (!allowSame) {
          // make a, b, and c all distinct
          if (b > 0 && a > b) {
            double d = a;
            a = b;
            b = d;
          }
          bcsame = c > 0 && SimpleUnitCell.approx0(b - c);
          if (bcsame)
            c = b * 1.5d;
          absame = SimpleUnitCell.approx0(a - b);
          if (absame)
            b = a * 1.2d;
          acsame = SimpleUnitCell.approx0(c - a);
          if (acsame)
            c = a * 1.1d;
  
          // make alpha, beta, and gamma all distinct
  
          if (SimpleUnitCell.approx0(alpha - 90)) {
            alpha = 80;
          }
          if (SimpleUnitCell.approx0(beta - 90)) {
            beta = 100;
          }
          if (SimpleUnitCell.approx0(gamma - 90)) {
            gamma = 110;
          }
          if (alpha > beta) {
            double d = alpha;
            alpha = beta;
            beta = d;
          }
          albesame = SimpleUnitCell.approx0(alpha - beta);
          begasame = SimpleUnitCell.approx0(beta - gamma);
          algasame = SimpleUnitCell.approx0(gamma - alpha);
  
          if (albesame) {
            beta = alpha * 1.2d;
          }
          if (begasame) {
            gamma = beta * 1.3d;
          }
          if (algasame) {
            gamma = alpha * 1.4d;
          }
        }
        if (toHex) {
          b = a;
          alpha = beta = 90;
          gamma = 120;
        } else if (n >= 49) {
          // tetragonal
          b = a;
          if (acsame && !allowSame)
            c = a * 1.5d;
          alpha = beta = gamma = 90;
        } else if (n >= 19) {
          // orthorhombic
          alpha = beta = gamma = 90;
        } else if (n >= 8) {
          // monoclinic rectangular
          beta = gamma = 90;
        } else if (n >= 3) {
          // monoclinic oblique
          alpha = beta = 90;
        }
      }
  
      boolean isNew = !(a == params[0] && b == params[1] && c == params[2]
          && alpha == params[3] && beta == params[4] && gamma == params[5]);
  
      newParams[0] = a;
      newParams[1] = b;
      newParams[2] = c;
      newParams[3] = alpha;
      newParams[4] = beta;
      newParams[5] = gamma;
      return isNew;
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
      periodicity = 0x4; // c only
    }
  
    /**
     * RodGroup rules
     */
    @Override
    public boolean createCompatibleUnitCell(double[] params, double[] newParams,
                                            boolean allowSame) {
      if (newParams == null)
        newParams = params;
      double a = params[0];
      double b = params[1];
      double c = params[2];
      double alpha = params[3];
      double beta = params[4];
      double gamma = params[5];
      int n = (itaNumber == null ? 0 : PT.parseInt(itaNumber));
  
      
      boolean toHex = false, isHex = false;
      toHex = (n != 0 && isHexagonalSG(n, null));
      isHex = (toHex && isHexagonalSG(-1, params));
      if (toHex && isHex) {
        allowSame = true;
      }
  
      if (n > (allowSame ? 2 : 0)) {
  
        boolean absame = b > 0 && SimpleUnitCell.approx0(a - b);
        boolean bcsame = c > 0 && SimpleUnitCell.approx0(b - c);
        boolean acsame = c > 0 && SimpleUnitCell.approx0(c - a);
        boolean albesame = SimpleUnitCell.approx0(alpha - beta);
        boolean begasame = SimpleUnitCell.approx0(beta - gamma);
        boolean algasame = SimpleUnitCell.approx0(gamma - alpha);
  
        if (!allowSame) {
          // make a, b, and c all distinct
          if (b > 0 && a > b) {
            double d = a;
            a = b;
            b = d;
          }
          bcsame = c > 0 && SimpleUnitCell.approx0(b - c);
          if (bcsame)
            c = b * 1.5d;
          absame = SimpleUnitCell.approx0(a - b);
          if (absame)
            b = a * 1.2d;
          acsame = SimpleUnitCell.approx0(c - a);
          if (acsame)
            c = a * 1.1d;
  
          // make alpha, beta, and gamma all distinct
  
          if (SimpleUnitCell.approx0(alpha - 90)) {
            alpha = 80;
          }
          if (SimpleUnitCell.approx0(beta - 90)) {
            beta = 100;
          }
          if (SimpleUnitCell.approx0(gamma - 90)) {
            gamma = 110;
          }
          if (alpha > beta) {
            double d = alpha;
            alpha = beta;
            beta = d;
          }
          albesame = SimpleUnitCell.approx0(alpha - beta);
          begasame = SimpleUnitCell.approx0(beta - gamma);
          algasame = SimpleUnitCell.approx0(gamma - alpha);
  
          if (albesame) {
            beta = alpha * 1.2d;
          }
          if (begasame) {
            gamma = beta * 1.3d;
          }
          if (algasame) {
            gamma = alpha * 1.4d;
          }
        }
        if (toHex) {
          b = a;
          alpha = beta = 90;
          gamma = 120;
        } else if (n >= 23) {
          // tetragonal
          b = a;
          if (acsame && !allowSame)
            c = a * 1.5d;
          alpha = beta = gamma = 90;
        } else if (n >= 13) {
          // orthorhombic
          alpha = beta = gamma = 90;
        } else if (n >= 8) {
          // monoclinic rectangular
          alpha = beta = 90;
        } else if (n >= 3) {
          // monoclinic oblique
          beta = gamma = 90;
        }
      }
  
      boolean isNew = !(a == params[0] && b == params[1] && c == params[2]
          && alpha == params[3] && beta == params[4] && gamma == params[5]);
  
      newParams[0] = a;
      newParams[1] = b;
      newParams[2] = c;
      newParams[3] = alpha;
      newParams[4] = beta;
      newParams[5] = gamma;
      return isNew;
    }
  
    @Override
    public boolean isHexagonalSG(int n, double[] params) {
      return (n < 1 ? SimpleUnitCell.isHexagonal(params)
          : n >= 42);
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
      if (newParams == null)
        newParams = params;
      double a = params[0];
      double b = params[0];
      double c = -1;
      double alpha = 90;
      double beta = 90;
      double gamma = 90;//params[5];
  
      
      boolean isNew = !(a == params[0] && b == params[1] && c == params[2]
          && alpha == params[3] && beta == params[4] && gamma == params[5]);
  
      newParams[0] = a;
      newParams[1] = b;
      newParams[2] = c;
      newParams[3] = alpha;
      newParams[4] = beta;
      newParams[5] = gamma;
      return isNew;
    }
  
  
  }


}