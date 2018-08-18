package org.jmol.modelset;

import javajs.util.SB;

import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;

public class StateScript {
  private int modelIndex;
  private BS bsBonds;
  private BS bsAtoms1;
  private BS bsAtoms2;
  private String script1;
  private String script2;
  public boolean inDefinedStateBlock;

  StateScript(int modelIndex, String script1, BS bsBonds,
      BS bsAtoms1, BS bsAtoms2, String script2,
      boolean inDefinedStateBlock) {
    this.modelIndex = modelIndex;
    this.script1 = script1;
    this.bsBonds = BSUtil.copy(bsBonds);
    this.bsAtoms1 = BSUtil.copy(bsAtoms1);
    this.bsAtoms2 = BSUtil.copy(bsAtoms2);
    this.script2 = script2;
    this.inDefinedStateBlock = inDefinedStateBlock;
  }

  public boolean isValid() {
    return script1 != null && script1.length() > 0
        && (bsBonds == null || bsBonds.nextSetBit(0) >= 0)
        && (bsAtoms1 == null || bsAtoms1.nextSetBit(0) >= 0)
        && (bsAtoms2 == null || bsAtoms2.nextSetBit(0) >= 0);
  }

  @Override
  public String toString() {
    if (!isValid())
      return "";
    
    SB sb = SB.newS(script1);
    if (bsBonds != null)
      sb.append(" ").append(Escape.eBond(bsBonds));
    if (bsAtoms1 != null)
      sb.append(" ").append(Escape.eBS(bsAtoms1));
    if (bsAtoms2 != null)
      sb.append(" ").append(Escape.eBS(bsAtoms2));
    if (script2 != null)
      sb.append(" ").append(script2);
    String s = sb.toString();
    if (!s.endsWith(";"))
      s += ";";
    return s;
  }

  public boolean isConnect() {
    return (script1.indexOf("connect") >= 0);
  }

  public boolean deleteAtoms(int modelIndex, BS bsBonds, BS bsAtoms) {
    //false return means delete this script
    if (modelIndex == this.modelIndex)
      return false;
    if (modelIndex > this.modelIndex) {
      //        this.modelIndex--;
      return true;
    }
    BSUtil.deleteBits(this.bsBonds, bsBonds);
    BSUtil.deleteBits(this.bsAtoms1, bsAtoms);
    BSUtil.deleteBits(this.bsAtoms2, bsAtoms);
    return isValid();
  }

  public void setModelIndex(int index) {
    modelIndex = index; // for creating data frames 
  }
}