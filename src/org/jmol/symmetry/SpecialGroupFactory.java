package org.jmol.symmetry;

import java.util.Map;

import org.jmol.symmetry.SpecialGroup.PlaneGroup;
import org.jmol.viewer.Viewer;

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
      if (base != null) {
        spg.embeddingSymmetry = base.embeddingSymmetry;
        spg.periodicity = base.periodicity;
      }
      return spg;
    }

  /**
   * @param sym
   * @param vwr
   * @param name
   * @param type
   * @return SpaceGroup
   */
  SpaceGroup getSpecialGroup(Viewer vwr, Symmetry sym, String name, int type) {
    return createSpecialGroup(null, sym,
        Symmetry.getSpecialSettingInfo(vwr, name, type), type);
  }

    
  }