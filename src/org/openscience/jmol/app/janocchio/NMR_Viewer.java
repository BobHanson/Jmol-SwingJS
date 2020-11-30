package org.openscience.jmol.app.janocchio;

import javax.swing.*;

import java.awt.Image;
import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.quantum.NMRNoeMatrix;
import org.jmol.viewer.Viewer;

/**
 * Implements some lost interfaces. It was a way to streamline the JavaScript;
 * Not an especially good idea, I know.
 * 
 */
public class NMR_Viewer extends Viewer {

  public NMR_Viewer(Map<String, Object> info) {
    super(info);
  }

  public int getDisplayModelIndex() {
    return am.cmi;
  }

  public int getAtomNumber(int atomIndex) {
    return ms.at[atomIndex].getAtomNumber();
  }

  public String getAtomName(int atomIndex) {
    return ms.at[atomIndex].getAtomName();
  }

  public int getModelCount() {
    return ms.mc;
  }

  public int getAtomCount() {
    return ms.ac;
  }

  public int getBondCount() {
    return ms.bondCount;
  }

  public void setSelectionHalos(boolean b) {
    setSelectionHalosEnabled(b);
  }

  public Image getScreenImage() {
    return (Image) getScreenImageBuffer(null, true);
  }

  public Atom getAtomAt(int i) {
    return ms.at[i];
  }

  public int getCurrentModelIndex() {
    return am.cmi;
  }

  public int indexInFrame(Atom atom) {
    return atom.i - ms.am[atom.mi].firstAtomIndex;
  }

  public void setFrameModelInfo(String key, Object value) {
    ms.getModelAuxiliaryInfo(getCurrentModelIndex()).put(key,  value);
  }

  public Object getFrameModelInfo(String key) {
    return ms.getModelAuxiliaryInfo(getCurrentModelIndex()).get(key);
  }

  public int getFrameAtomIndex(int i) {
    return i - getFrameBase(i);
  }

  public int getFrameBase(int i) {
    return ms.am[ms.at[i].mi].firstAtomIndex;
  }


}
