package org.jmol.util;

import java.util.Comparator;

/**
 * sort from smallest to largest absolute
 * 
 */
class EigenSort implements Comparator<Object[]> {
  @Override
  public int compare(Object[] o1, Object[] o2) {
    float a = ((Float) o1[1]).floatValue();
    float b = ((Float) o2[1]).floatValue();
    return (a < b ? -1 : a > b ? 1 : 0);
  }
}