package org.jmol.modelsetbio;

import java.util.Map;

public class BasePair {
  Map<String, Object> info;
  private NucleicMonomer g1;
  private NucleicMonomer g2;
  
  private BasePair(){
    
  }

  public static BasePair add(Map<String, Object> info, NucleicMonomer g1,
                         NucleicMonomer g2) {
    if (g1 == null || g2 == null)
      return null; // can happen if one of DSSR pair is not a monomer (HPA in 4fe5) 
    BasePair bp = new BasePair();
    bp.info = info;
    (bp.g1 = g1).addBasePair(bp);
    (bp.g2 = g2).addBasePair(bp);
    return bp;
  }

  public int getPartnerAtom(NucleicMonomer g) {
    return (g == g1 ? g2 : g1).getLeadAtom().i;
  }
  
  @Override
  public String toString() {
    return info.toString();
  }
}
