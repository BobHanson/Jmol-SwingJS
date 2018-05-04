package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;

import org.jmol.symmetry.CIPChirality.CIPAtom;
import org.jmol.viewer.JC;

/**
 * An optional class to track digraph paths to decisions
 * 
 */
public class CIPDataTracker extends CIPData {

  /**
   * a table to track decision making when only one atom is selected for
   * calculation
   * 
   */
  public Map<String, CIPTracker> htTracker = new Hashtable<String, CIPTracker>();

  @Override
  boolean isTracker() {
    return true;
  }

  class CIPTracker {

    CIPAtom a, b;
    int sphere, score, mode, rule;
    public BS bsa, bsb;

    CIPTracker(int rule, CIPAtom a, CIPAtom b, int sphere, int score, int mode) {
      this.rule = rule;
      this.a = a;
      this.b = b;
      this.sphere = sphere;
      this.score = score;
      this.mode = mode;
      bsa = a.listRS == null ? new BS() : a.listRS[0];
      bsb = b.listRS == null ? new BS() : b.listRS[0];
    }

    String getTrackerLine(CIPAtom b, BS bsb, BS bsS, int n) {
      return "\t"
          + "\t"
          + b.myPath
          + (mode != CIPChirality.TRACK_TERMINAL ? "" : b.isTerminal ? "-o"
              : "-" + b.atoms[0].atom.getAtomName())
          + (rule != CIPChirality.RULE_5 && bsb.length() == 0 ? "" : "\t"
              + getLikeUnlike(bsb, b.listRS, n) + (bsS == null ? "" : "  " + getLikeUnlike(bsS, b.listRS, -n))) + "\n";
    }

    private String getLikeUnlike(BS bsa, BS[] listRS, int n) {
      if (rule != CIPChirality.RULE_5 && rule != CIPChirality.RULE_4b)
        return "";
      String s = (false && rule == CIPChirality.RULE_5 ? ""
          : n > 0 && (rule == CIPChirality.RULE_5 || bsa == listRS[CIPChirality.STEREO_R]) ? "(R)" : "(S)");
      String l = (false && rule == CIPChirality.RULE_5 ? "R" : "l");
      String u = (false && rule == CIPChirality.RULE_5 ? "S" : "u");
      n = Math.abs(n);
      for (int i = 0; i < n; i++)
        s += (bsa.get(i) ? l : u);
      return s;
    }

  }

  @Override
  void track(CIPChirality cip, CIPAtom a, CIPAtom b, int sphere,
             int finalScore, int mode) {
    // don't track intra-ligand setting
    if (a.rootSubstituent == b.rootSubstituent)
      return;
    CIPTracker t;
    CIPAtom a1, b1;
    if (finalScore > 0) {
      a1 = b;
      b1 = a;
    } else {
      a1 = a;
      b1 = b;
    }
    t = new CIPTracker(cip.currentRule, a1, b1, sphere, Math.abs(finalScore),
        mode);
    htTracker.put(getTrackerKey(cip.root, a1, b1), t);
    switch (mode) {
    case CIPChirality.TRACK_RS:
    case CIPChirality.TRACK_ATOM:
      break;
    case CIPChirality.TRACK_DUPLICATE:
    case CIPChirality.TRACK_TERMINAL:
    }
  }

  @Override
  String getRootTrackerResult(CIPAtom root) {
    String s = "";
    for (int i = 0; i < 3; i++) {
      s += "\t" + root.atoms[i] + "\t--------------\n";
      CIPTracker t = htTracker.get(getTrackerKey(root, root.atoms[i],
          root.atoms[i + 1]));
      if (t != null) {
        int n = Math.max(t.bsa.length(), t.bsb.length());
        s += t.getTrackerLine(t.a, t.bsa, (t.rule == CIPChirality.RULE_5 ? t.a.listRS[2] : null), n);
        s += "\t" + "   " + JC.getCIPRuleName(t.rule)
        // + "\t" + t.sphere + "\t" + t.score + "\t" + t.mode
            + "\n";
        s += t.getTrackerLine(t.b, t.bsb, (t.rule == CIPChirality.RULE_5 ? t.b.listRS[2] : null), n);
        //        if (t.mode == TRACK_DUPLICATE)
        //          System.out.println(s);
      }
    }
    s += "\t" + root.atoms[3] + "\t--------------\n";
    System.out.println(root + "\n\n" + s);
    setCIPInfo(s, root.atom.getIndex(), root.atom.getAtomName());
    return s;
  }

  private void setCIPInfo(String s, int index, String name) {
    Map<String, Object> modelInfo = getModelAuxiliaryInfoForAtom(index);
    if (modelInfo != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> cipInfo = (Map<String, Object>) modelInfo
          .get("CIPInfo");
      if (cipInfo == null)
        modelInfo.put("CIPInfo", cipInfo = new Hashtable<String, Object>());
      cipInfo.put(name, s);
    }
  }

  private int lastIndex = -1;
  private Map<String, Object> lastInfo;

  private Map<String, Object> getModelAuxiliaryInfoForAtom(int index) {
    return (index == lastIndex ? lastInfo : (lastInfo = vwr.ms
        .getModelAuxiliaryInfo(vwr.ms.at[lastIndex = index].getModelIndex())));
  }

  private static String getTrackerKey(CIPAtom root, CIPAtom a, CIPAtom b) {
    return (b.rootSubstituent == null ? "" : root.atom.getAtomName() + "."
        + a.rootSubstituent.atom.getAtomName() + "-"
        + b.rootSubstituent.atom.getAtomName());
  }

}
