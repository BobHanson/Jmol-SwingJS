package org.jmol.symmetry;

import java.util.Map;

import org.jmol.symmetry.SpecialGroup.PlaneGroup;
import org.jmol.viewer.Viewer;

import javajs.util.Lst;

/**
   * A static singleton class to create plane, layer, rod, and frieze groups
   */
  public class SpecialGroupFactory {

    public SpecialGroupFactory() {
      System.out.println("created");
      // for reflection
    }
    
    SpecialGroup createSpecialGroup(SpecialGroup base, Symmetry sym, Map<String, Object> info, int type) {
      SpecialGroup spg;
      switch (type) {
//      case SpaceGroup.TYPE_SPACE:
//        return new SpaceGroup(-1, SpaceGroup.NEW_NO_HALL_GROUP, true);
      case SpaceGroup.TYPE_PLANE:
        spg = new PlaneGroup(sym, info);
        break;
      case SpaceGroup.TYPE_LAYER:
        spg = new SpecialGroup.LayerGroup(sym, info);
        break;
      case SpaceGroup.TYPE_ROD:
        spg = new SpecialGroup.RodGroup(sym, info);
        break;
      case SpaceGroup.TYPE_FRIEZE:
        spg = new SpecialGroup.FriezeGroup(sym, info);
        break;
      default:
        // won't happen
        return null;
      }
      if (base != null)
        spg.embeddingSymmetry = base.embeddingSymmetry;
      return spg;
    }

    /**
     * @param sym 
     * @param vwr 
     * @param name 
     * @param itno 
     * @param itindex  
     * @param isCleg 
     * @param type 
     * @return SpaceGroup 
     */
    @SuppressWarnings("unchecked")
    SpaceGroup getSpecialGroup(Symmetry sym, Viewer vwr, String name, int itno, int itindex, boolean isCleg, int type) {
      Map<String, Object>[] data = (Map<String, Object>[]) Symmetry.getAllITAData(vwr, type, false);
      Map<String, Object> info = null;
      if (itindex > 0) {
        info = (Map<String, Object>) ((Lst<Object>) data[itno - 1].get("its")).get(itindex - 1);    
      } else {
        info = Symmetry.getSpecialSettingJSON(data, name);
      }
      return createSpecialGroup(null, sym, info, type);
    }

    
  }