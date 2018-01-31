package org.jmol.modelset;

import org.jmol.java.BS;

public interface Structure {
  public void setAtomBits(BS bs);
  public void setAtomBitsAndClear(BS bs, BS bsOut);
}
