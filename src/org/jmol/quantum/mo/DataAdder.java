package org.jmol.quantum.mo;

import org.jmol.quantum.MOCalculation;

public interface DataAdder {
  boolean addData(MOCalculation calc, boolean havePoints);
}
