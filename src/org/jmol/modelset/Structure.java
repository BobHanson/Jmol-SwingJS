package org.jmol.modelset;

import javajs.util.BS;

public interface Structure {
  public void setAtomBits(BS bs);
  public void setAtomBitsAndClear(BS bs, BS bsOut);
}
