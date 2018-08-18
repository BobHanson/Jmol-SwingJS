package org.jmol.api;

import java.util.Map;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.MeasurementData;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.Tensor;
import javajs.util.V3;
import org.jmol.viewer.Viewer;

public interface JmolNMRInterface {

  public JmolNMRInterface setViewer(Viewer vwr);

  /**
   * Quadrupolar constant, directly proportional to Vzz and dependent on the
   * quadrupolar moment of the isotope considered
   * 
   * @param efg
   * @return float value
   */
  public float getQuadrupolarConstant(Tensor efg);

  /**
   * If t is null, then a1, a2, and type are used to find the appropriate
   * tensor.
   * 
   * @param isIso
   * @param a1
   * @param a2
   * @param type
   * @param t
   * @return 0 if not found
   */
  public float getIsoOrAnisoHz(boolean isIso, Atom a1, Atom a2, String type, Tensor t);

  /**
   * 
   * @param a1
   * @param a2
   * @return desired constant
   */
  public float getDipolarConstantHz(Atom a1, Atom a2);

  /**
   * 
   * @param a1
   * @param a2
   * @param vField
   * @return projected value
   */
  public float getDipolarCouplingHz(Atom a1, Atom a2, V3 vField);

  /**
   * An attempt to find unique atoms using tensors.
   * 
   * @param bs
   * @return bitset of atoms
   */
  public BS getUniqueTensorSet(BS bs);

  /**
   * 
   * @param sym "C" or "14C" or "all"
   * @return list of double[isotopeNumber,g,Q] if no isotope number is given, or a single double[] if it does.
   */
  public Object getInfo(String sym);

  public float getMagneticShielding(Atom atom);

  /**
   * If shift reference has not been set, it defaults to 0 and just
   * displays the negative of magnetic shielding
   *  
   * @param atom
   * @return value
   */
  public float getChemicalShift(Atom atom);  
  public boolean setChemicalShiftReference(String element, float value);

  public Lst<Object> getTensorInfo(String tensorType, String infoType, BS bs);

  public Map<String, Integer> getMinDistances(MeasurementData md);

  public boolean getState(SB sb);
  
}
