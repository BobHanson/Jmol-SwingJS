/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.util;

import java.util.Map;

import javajs.util.BS;


final public class BSUtil {

  public final static BS emptySet = new BS();

  public static BS newAndSetBit(int i) {
    BS bs = BS.newN(i + 1);
    bs.set(i);
    return bs;
  }
  
  public static boolean areEqual(BS a, BS b) {
    return (a == null || b == null ? a == null && b == null : a.equals(b));
  }

  public static boolean haveCommon(BS a, BS b) {
    return (a == null || b == null ? false : a.intersects(b));
  }

  /**
   * cardinality = "total number of set bits"
   * @param bs
   * @return number of set bits
   */
  public static int cardinalityOf(BS bs) {
    return (bs == null ? 0 : bs.cardinality());
  }

  public static BS newBitSet2(int i0, int i1) {
    BS bs = BS.newN(i1);
    bs.setBits(i0, i1);
    return bs;
  }
  
  public static BS setAll(int n) {
    BS bs = BS.newN(n);
    bs.setBits(0, n);
    return bs;
  }

  public static BS andNot(BS a, BS b) {
    if (b != null && a != null)
      a.andNot(b);
    return a;
  }

  public static BS copy(BS bs) {
    return bs == null ? null : (BS) bs.clone();
  }

  public static BS copy2(BS a, BS b) {
    if (a == null || b == null)
      return null;
    b.clearAll();
    b.or(a);
    return b;
  }
  
  public static BS copyInvert(BS bs, int n) {
    return (bs == null ? null : andNot(setAll(n), bs));
  }
  
  /**
   * inverts the bitset bits 0 through n-1, 
   * and returns a reference to the modified bitset
   * 
   * @param bs
   * @param n
   * @return  pointer to original bitset, now inverted
   */
  public static BS invertInPlace(BS bs, int n) {
    return copy2(copyInvert(bs, n), bs);
  }

  /**
   * a perhaps curious method:
   * 
   * b is a reference set, perhaps all atoms in a certain molecule a is the
   * working set, perhaps representing all displayed atoms
   * 
   * For each set bit in b: a) if a is also set, then clear a's bit UNLESS b) if
   * a is not set, then add to a all set bits of b
   * 
   * Thus, if a equals b --> clear all if a is a subset of b, then --> b if b is
   * a subset of a, then --> a not b if a only intersects with b, then --> a or
   * b if a does not intersect with b, then a or b
   * 
   * In "toggle" mode, when you click on any atom of the molecule, you want
   * either:
   * 
   * (a) all the atoms in the molecule to be displayed if not all are already
   * displayed, or
   * 
   * (b) the whole molecule to be hidden if all the atoms of the molecule are
   * already displayed.
   * 
   * @param a
   * @param b
   * @return a handy pointer to the working set, a
   */
  public static BS toggleInPlace(BS a, BS b) {
    if (a.equals(b)) {
      // all on -- toggle all off
      a.clearAll();
    } else if (andNot(copy(b), a).length() == 0) {
      // b is a subset of a -> remove all b bits from a
      andNot(a, b); 
    } else {
      // may or may not be some overlap
      // combine a and b
      a.or(b);
    }
    return a;
  }
  
  /**
   * this one slides deleted bits out of a pattern.
   * 
   *    deleteBits 101011b, 000011b  --> 1010b
   *    
   *    Java 1.4, not 1.3
   * 
   * @param bs
   * @param bsDelete
   * @return             shorter bitset
   */
  public static BS deleteBits(BS bs, BS bsDelete) {
    if (bs == null || bsDelete == null)
      return bs;
    int ipt = bsDelete.nextSetBit(0);
    if (ipt < 0)
      return bs;
    int len = bs.length();
    int lend = Math.min(len, bsDelete.length());
    int i;
    for (i = bsDelete.nextClearBit(ipt); i < lend && i >= 0; i = bsDelete.nextClearBit(i + 1))
      bs.setBitTo(ipt++, bs.get(i));
    for (i = lend; i < len; i++)
      bs.setBitTo(ipt++, bs.get(i));
    if (ipt < len)
      bs.clearBits(ipt, len);
    return bs;
  }
  
  /**
   * this one slides bits to higher positions based on a pattern.
   * 
   * shiftBits 101011b, 000011b --> 10101100b
   * 
   * @param bs
   * @param bsAdded
   * @param setIfFound 
   * @param iLast
   */
  public static void shiftBits(BS bs, BS bsAdded, boolean setIfFound, int iLast) {
    if (bs == null || bsAdded == null)
      return;
    int n = bsAdded.length();
    BS bsNew = BS.newN(n);
    boolean isFound = false;
    boolean doSet = false;
    boolean checkFound = setIfFound;
    for (int j = 0, i = 0; j < n && i < iLast; j++) {
      if (bsAdded.get(j)) {
        if (doSet)
          bsNew.set(j);
        checkFound = setIfFound;
        isFound = false;
      } else if(bs.get(i++)) {
        bsNew.set(j);
        if(checkFound) {
          checkFound = false;
          isFound = true;
          doSet = true;
        }
      } else if (checkFound && !isFound){
        doSet = false;
      }
    }
    bs.clearAll();
    bs.or(bsNew);
  }


  /**
   * offset the bitset in place by the specified number of bits
   * starting at a given position
   * 
   * @param bs0
   * @param pos    starting position; no change before this
   * @param offset
   */
  public static void offset(BS bs0, int pos, int offset) {
    if (bs0 == null)
      return;
      BS bsTemp = BS.newN(bs0.length() + offset);
    for (int i = bs0.nextSetBit(0); i >= pos; i = bs0.nextSetBit(i + 1))
      bsTemp.set(i + offset);
    copy2(bsTemp, bs0);
  }

  public static void setMapBitSet(Map<String, BS> ht, int i1, int i2, String key) {
    BS bs;
    if (ht.containsKey(key))
      bs = ht.get(key);
    else
      ht.put(key, bs = new BS());
    bs.setBits(i1, i2 + 1);
  }

  
}
