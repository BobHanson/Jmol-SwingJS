package org.jmol.adapter.writers;

import javajs.util.PT;

public class XtlWriter {

  protected boolean haveUnitCell = true;

  protected String clean(float f) {
    int t;
    return (!haveUnitCell || (t = twelfthsOf(f)) < 0
        ? PT.formatF(f, 18, 12, false, false)
        : (f < 0 ? "   -" : "    ") + twelfths[t]);
  }

  private static final String[] twelfths = new String[] { "0.000000000000",
      "0.083333333333", "0.166666666667", "0.250000000000", "0.333333333333",
      "0.416666666667", "0.500000000000", "0.583333333333", "0.666666666667",
      "0.750000000000", "0.833333333333", "0.916666666667", "1.000000000000", };

  private static int twelfthsOf(float f) {
    if (f == 0)
      return 0;
    f = Math.abs(f * 12);
    int i = Math.round(f);
    return (i <= 12 && Math.abs(f - i) < 0.00015 * 12 ? i : -1);
  }

}
