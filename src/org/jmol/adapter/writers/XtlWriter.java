package org.jmol.adapter.writers;

import javajs.util.PT;

public class XtlWriter {

  protected boolean haveUnitCell = true;
  
  /**
   * in SwingJS this is always true
   * 
   */
  protected boolean isHighPrecision = true;
  
  private static double SLOPD = 0.000000000010;

  private static float SLOPF = 0.0000010f;

  protected String clean(double d) {
    if (!isHighPrecision)
      return cleanF((float)d);
    int t;
    return (!haveUnitCell || (t = twelfthsOf(d)) < 0
        ? PT.formatD(d, 18, 12, false, false)
        : (d < 0 ? "   -" : "    ") + twelfths[t]);
  }
  
  private static int twelfthsOf(double f) {
    if (f == 0)
      return 0;
    f = Math.abs(f * 12);
    int i = (int) Math.round(f);
    return (i <= 12 && Math.abs(f - i) < SLOPD * 12 ? i : -1);
  }

  private static final String[] twelfths = new String[] { "0.000000000000",
      "0.083333333333", "0.166666666667", "0.250000000000", "0.333333333333",
      "0.416666666667", "0.500000000000", "0.583333333333", "0.666666666667",
      "0.750000000000", "0.833333333333", "0.916666666667", "1.000000000000", };

  private String cleanF(float f) {
    int t;
    return (!haveUnitCell || (t = twelfthsOfF(f)) < 0
        ? PT.formatD(f, 12, 7, false, false)
        : (f < 0 ? "   -" : "    ") + twelfthsF[t]);
  }

  private static int twelfthsOfF(float f) {
    if (f == 0)
      return 0;
    f = Math.abs(f * 12);
    int i = Math.round(f);
    return (i <= 12 && Math.abs(f - i) < SLOPF * 12 ? i : -1);
  }

  private static final String[] twelfthsF = new String[] { 
      "0.0000000",
      "0.0833333", "0.1666667", "0.2500000", "0.3333333",
      "0.4166667", "0.5000000", "0.5833333", "0.6666667",
      "0.7500000", "0.8333333", "0.9166667", "1.0000000", 
  };

  /**
   * right-zero-trimmed
   * @param d
   * @return x.xxxx or x.0 with no trailing zeros
   */
  protected String cleanT(double d) {
    String s = clean(d);
    int i = s.length();
    while (--i >= 2 && s.charAt(i) == '0' && s.charAt(i - 1) != '.') {
    }
    return s.substring(0, i + 1);
  }


}
