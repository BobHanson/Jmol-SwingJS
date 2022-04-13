package org.jmol.adapter.readers.xtal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

import javajs.util.SB;

/**
 * A molecular structure and orbital reader for MolDen files.
 * See http://www.cmbi.ru.nl/molden/molden_format.html
 * 
 * updated by Bob Hanson <hansonr@stolaf.edu> for Jmol 12.0/12.1
 * 
 * adding [spacegroup] [operators] [cell] [cellaxes] for Jmol 14.3.7 
 * 
 * @author Matthew Zwier <mczwier@gmail.com>
 */

@SuppressWarnings("unchecked")
public class OptimadeReader extends AtomSetCollectionReader {
  
  private int modelNo;
  private boolean iHaveDesiredModel;
  private float[] dimensionType;
  private float ndims;
  private boolean isPolymer;
  private boolean isSlab;
  private boolean isMolecular;


  @Override
  protected void initializeReader() throws Exception {
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
    dimensionType = new float[3];
    toFloatArray((List<Number>) map.get("dimension_types"), dimensionType);
    if (!checkDimensionType()) {
      throw new IllegalArgumentException(
          "OptimadeReader does not support dimentionType " + dimensionType[0]
              + " " + dimensionType[1] + " " + dimensionType[2]);
    }
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

  private boolean checkDimensionType() {
    float[] dt = dimensionType;
    isPolymer = isSlab = isMolecular = false;
    ndims = dt[0] + dt[1] + dt[2];
    return ndims == 3 || (isMolecular = (ndims == 0))
        || (isSlab = (dt[0] + dt[1] == 2)) // xy plane only
        || (isPolymer = (dt[0] == 1)); // z only
  }

  private float[] xyz = new float[3];
  
  private boolean readLattice(List<Object> lattice) {
    if (lattice == null)
      return false;
    for (int i = 0; i < 3; i++) {
      if (!toFloatArray((List<Number>) lattice.get(i), xyz)) {
        return false;
      }
      unitCellParams[0] = Float.NaN;
      addExplicitLatticeVector(i, xyz, 0);
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
        float[] conc = new float[nOcc];
        toFloatArray((List<Number>) sp.get("concentration"), conc);
        for (int j = 0; j < conc.length; j++) {
          Atom a = addAtom(xyz, (String) syms.get(j), sname);
          a.foccupancy = conc[j];
        }
      } else {
        addAtom(xyz, (String) syms.get(0), sname);
      }
      
    }
    
  }


  private Atom addAtom(float[] xyz, String sym, String name) {
    Atom atom = asc.addNewAtom();
    if (sym != null)
      atom.elementSymbol = sym;
    if (name != null)
      atom.atomName = name;
    setAtomCoordXYZ(atom, xyz[0], xyz[1], xyz[2]);
    return atom;
  }


  private static boolean toFloatArray(List<Number> list, float[] a) {
    for (int i = a.length; --i >= 0;) {
      Number d = list.get(i);
      if (d == null)
        return false;
      a[i] = list.get(i).floatValue();
    }
    return true;
  }

}
