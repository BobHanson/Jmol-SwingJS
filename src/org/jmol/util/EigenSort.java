package org.jmol.util;

import java.util.Comparator;

/**
 * sort from smallest to largest absolute
 * 
 */
class EigenSort implements Comparator<Object> {
  @Override
  public int compare(Object o1, Object o2) {
    double a = ((Double) ((Object[]) o1)[1]).doubleValue();
    double b = ((Double) ((Object[]) o2)[1]).doubleValue();
    return (a < b ? -1 : a > b ? 1 : 0);
  }
}