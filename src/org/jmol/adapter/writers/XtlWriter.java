package org.jmol.adapter.writers;

import javajs.util.PT;

public class XtlWriter {

  protected boolean haveUnitCell = true;

  private static final String[] twelfths = new String[] { "0.000000000000",
      "0.083333333333", "0.166666666667", "0.250000000000", "0.333333333333",
      "0.416666666667", "0.500000000000", "0.583333333333", "0.666666666667",
      "0.750000000000", "0.833333333333", "0.916666666667", "1.000000000000", };

  private static int twelfthsOf(double f) {
    if (f == 0)
      return 0;
    f = Math.abs(f * 12);
    int i = (int) Math.round(f);
    return (i <= 12 && Math.abs(f - i) < 0.00015 * 12 ? i : -1);
  }

  /**
   * Write the double-precision coord, cleaned to twelths. 
   * Do not use this method unless the value really is a float, 
   * because (double) on a float will introduce 5-6 bits of garbage.
   * @param f
   * @return 18-wide column value
   */
  protected String clean(double f) {
    int t;
    return (!haveUnitCell || (t = twelfthsOf(f)) < 0
        ? PT.formatD(f, 18, 12, false, false)
        : (f < 0 ? "   -" : "    ") + twelfths[t]);
  }

  protected String cleanF(float f) {
    int t;
    if (!haveUnitCell || (t = twelfthsOf(f)) < 0) {
      String s =   "    " + f + "             "; 
      return s.substring(0, 18);
    }
    return (f < 0 ? "   -" : "    ") + twelfths[t];
  }


}
