package org.jmol.adapter.readers.xtal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.util.SimpleUnitCell;

import javajs.util.SB;

/**
 * A (preliminary) reader for OPTIMADE resources.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
*/

@SuppressWarnings("unchecked")
public class OptimadeReader extends AtomSetCollectionReader {
  
  private int modelNo;
  private boolean iHaveDesiredModel;
  /**
   * values 0, 1, or 2 indicate how to permute the lattice vectors
   * to be of the form [1,0,0] for polymers or [1,1,0] for slabs 
   */
  private int permutation;
  private boolean isPolymer;
  private boolean isSlab;

  @Override
  protected void initializeReader() throws Exception {
    setHighPrecision();    
    super.initializeReader();
    SB sb = new SB();
    try {
      while (rd() != null)
        sb.append(line);
      Map<String, Object> json = vwr.parseJSONMap(sb.toString());
      List<Object> aData = (List<Object>) json.get("data");
      if (aData != null) {
        for (int i = 0; !iHaveDesiredModel && i < aData.size(); i++) {
          Map<String, Object> data = (Map<String, Object>) aData.get(i);
          if ("structures".equals(data.get("type"))) {
            readModel((Map<String, Object>) data.get("attributes"));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    continuing = false;
  }

  
  private void readModel(Map<String, Object> map) throws Exception {
    if (!doGetModel(modelNumber = ++modelNo, null))
      return;
    iHaveDesiredModel = isLastModel(modelNumber);
    applySymmetryAndSetTrajectory();
    asc.newAtomSet();
    setFractionalCoordinates(false);
    double[] dimensionType = new double[3];
    toFloatArray((List<Number>) map.get("dimension_types"), dimensionType);
    checkDimensionType(dimensionType);
    if (!isMolecular) {
      setSpaceGroupName("P1");
      asc.setInfo("symmetryType",
          (isSlab ? "2D - SLAB" : isPolymer ? "1D - POLYMER" : "3D"));
    }
    asc.setAtomSetName((String) map.get("chemical_formula_descriptive"));
    doConvertToFractional = (!isMolecular
        && readLattice((List<Object>) map.get("lattice_vectors")));
    readAtoms((List<Object>) map.get("species"),
        (List<Object>) map.get("species_at_sites"),
        (List<Object>) map.get("cartesian_site_positions"));
  }

  private void checkDimensionType(double[] dt) {
    isPolymer = isSlab = isMolecular = false;
    permutation = 0;
    switch((int) (dt[0] + dt[1]*2 + dt[2]*4)) {
    default:
    case 0: // [0,0,0]
      isMolecular = true;
      break;
    case 1: // [0,0,1]
      isPolymer = true;
      permutation = 1; // to [1,0,0]
      break;
    case 2: // [0,1,0]
      isPolymer = true;
      permutation = 2; // to [1,0,0]
      break;
    case 3: // [0,1,1]
      isSlab = true;
      permutation = 2; // to [1,1,0]
      break;
    case 5: // [1,0,1]
      isSlab = true;
      permutation = 1; // to [1,1,0]
      break;
    case 4: // [1,0,0]
      isPolymer = true;
      break;
    case 6: // [1,1,0]
      isSlab = true;
      break;
    case 7: // [1,1,1]
      break;
    }
  }

  private double[] xyz = new double[3];
  
  private boolean readLattice(List<Object> lattice) {
    if (lattice == null)
      return false;
    double[] abc = new double[3];
    for (int i = 0; i < 3; i++) {
      if (!toFloatArray((List<Number>) lattice.get(i), xyz)) {
        return false;
      }
      // this will set [0-6] to -1
      unitCellParamsD = new double[SimpleUnitCell.PARAM_COUNT];
      unitCellParams[0] = Double.NaN;
      if (isSlab || isPolymer) {
        abc[i] = Math.sqrt(xyz[0]*xyz[0]+xyz[1]*xyz[1]+xyz[2]*xyz[2]);
      }
      if (i == 2) {
        if (isSlab || isPolymer) {
          unitCellParamsD[0] = abc[permutation];
          if (isSlab)
            unitCellParamsD[1] = abc[(permutation + 1)%3];
        }
        
      }
      addExplicitLatticeVectorD((i + permutation)%3, xyz, 0);
    }
    
    doApplySymmetry = true;
    return true;
  }

  private void readAtoms(List<Object> species, List<Object> sites, List<Object> coords) {
    int natoms = sites.size();
    Map<String, Object> speciesByName = new HashMap<String, Object>();
    for (int i = species.size(); --i >= 0;) {
      Map<String, Object> s = (Map<String, Object>) species.get(i);
      speciesByName.put((String) s.get("name"), s);
    }
    for (int i = 0; i < natoms; i++) {
      String sname = (String) sites.get(i);
      toFloatArray((List<Number>) coords.get(i), xyz);
      Map<String, Object> sp = (Map<String, Object>) speciesByName.get(sname);
      List<Object> syms = (List<Object>) sp.get("chemical_symbols");
      int nOcc = syms.size();
      if (nOcc > 1) {
        double[] conc = new double[nOcc];
        toFloatArray((List<Number>) sp.get("concentration"), conc);
        for (int j = 0; j < conc.length; j++) {
          Atom a = addAtom(xyz, (String) syms.get(j), sname);
          a.foccupancy = conc[j]; // todo --- double occupancy
        }
      } else {
        addAtom(xyz, (String) syms.get(0), sname);
      }
      
    }
    
  }


  private Atom addAtom(double[] xyz, String sym, String name) {
    Atom atom = asc.addNewAtom();
    if (sym != null)
      atom.elementSymbol = sym;
    if (name != null)
      atom.atomName = name;
    setAtomCoordXYZ(atom, xyz[0], xyz[1], xyz[2]);
    return atom;
  }


  private static boolean toFloatArray(List<Number> list, double[] a) {
    for (int i = a.length; --i >= 0;) {
      Number d = list.get(i);
      if (d == null)
        return false;
      a[i] = list.get(i).doubleValue();
    }
    return true;
  }

  @Override
  protected void finalizeSubclassSymmetry(boolean haveSymmetry) throws Exception {
    super.finalizeSubclassSymmetry(haveSymmetry);
  }
}
