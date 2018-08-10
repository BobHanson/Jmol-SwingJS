/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-09-14 06:56:50 -0500 (Mon, 14 Sep 2015) $
 * $Revision: 20772 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.smarter;


import org.jmol.java.BS;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;

import org.jmol.util.Tensor;
import org.jmol.util.Vibration;

import javajs.util.V3;

public class Atom extends P3 implements Cloneable {
  public int atomSetIndex;
  public int index;
  public BS bsSymmetry;
  public int atomSite;
  public String elementSymbol;
  public short elementNumber = -1;
  public String atomName;
  public int formalCharge = Integer.MIN_VALUE;
  public float partialCharge = Float.NaN;
  public V3 vib; // .x and .y can be used for in-reader purposes as long as vib.z is left Float.NaN
  public float bfactor = Float.NaN;
  public float foccupancy = 1;
  public float radius = Float.NaN;
  public boolean isHetero;
  public int atomSerial = Integer.MIN_VALUE;
  public int chainID; // not public -- set using AtomSetCollectionReader.setChainID
  
  public char altLoc = '\0';
  public String group3;
  public int sequenceNumber = Integer.MIN_VALUE;
  public char insertionCode = '\0';
  public float[] anisoBorU; //[6] = 1 for U, 0 for B; [7] = bFactor
  public Lst<Object> tensors;
  
  public Tensor addTensor(Tensor tensor, String type, boolean reset) {
    if (tensor == null)
      return null;
    if (reset || tensors == null)
      tensors = new Lst<Object>();
    tensors.addLast(tensor);
    if (type != null)
      tensor.setType(type);
    return tensor;
  }

  
  public boolean ignoreSymmetry; // CIF _atom_site_disorder_group -1

  /**
   * @j2sIgnoreSuperConstructor
   * @j2sOverride
   * 
   */
  public Atom() {
   set(Float.NaN, Float.NaN, Float.NaN);
  }

  public Atom getClone() throws CloneNotSupportedException {
    Atom a = (Atom)clone();
    if (vib != null) {
      if (vib instanceof Vibration) {
        a.vib = (Vibration) ((Vibration) vib).clone();
      } else {
        a.vib = V3.newV(a.vib);
      }
    }
    if (anisoBorU != null)
      a.anisoBorU = AU.arrayCopyF(anisoBorU, -1);
    if (tensors != null) {
      a.tensors = new Lst<Object>();
      for (int i = tensors.size(); --i >= 0;)
        a.tensors.addLast(((Tensor)tensors.get(i)).copyTensor());
    }
    return a;
  }

  public String getElementSymbol() {
    if (elementSymbol == null && atomName != null) {
      int len = atomName.length();
      int ichFirst = 0;
      char chFirst = 0;
      while (ichFirst < len
          && !isValidSymChar1(chFirst = atomName.charAt(ichFirst)))
        ++ichFirst;
      switch (len - ichFirst) {
      case 0:
        break;
      default:
        char chSecond = atomName.charAt(ichFirst + 1);
        if (isValidSymNoCase(chFirst, chSecond)) {
          elementSymbol = "" + chFirst + chSecond;
          break;
        }
        //$FALL-THROUGH$
      case 1:
        if (isValidSym1(chFirst))
          elementSymbol = "" + chFirst;
        break;
      }
    }
    return elementSymbol;
  }

  private final static int[] elementCharMasks = {
    //   Ac Ag Al Am Ar As At Au
    1 << ('c' - 'a') |
    1 << ('g' - 'a') |
    1 << ('l' - 'a') |
    1 << ('m' - 'a') |
    1 << ('r' - 'a') |
    1 << ('s' - 'a') |
    1 << ('t' - 'a') |
    1 << ('u' - 'a'),
    // B Ba Be Bh Bi Bk Br
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('e' - 'a') |
    1 << ('h' - 'a') |
    1 << ('i' - 'a') |
    1 << ('k' - 'a') |
    1 << ('r' - 'a'),
    // C Ca Cd Ce Cf Cl Cm Co Cr Cs Cu
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('d' - 'a') |
    1 << ('e' - 'a') |
    1 << ('f' - 'a') |
    1 << ('l' - 'a') |
    1 << ('m' - 'a') |
    1 << ('o' - 'a') |
    1 << ('r' - 'a') |
    1 << ('s' - 'a') |
    1 << ('u' - 'a'),
    //  D Db Dy
    1 << 31 |
    1 << ('b' - 'a') |
    1 << ('y' - 'a'),
    //   Er Es Eu
    1 << ('r' - 'a') |
    1 << ('s' - 'a') |
    1 << ('u' - 'a'),
    // F Fe Fm Fr
    1 << 31 |
    1 << ('e' - 'a') |
    1 << ('m' - 'a') |
    1 << ('r' - 'a'),
    //   Ga Gd Ge
    1 << ('a' - 'a') |
    1 << ('d' - 'a') |
    1 << ('e' - 'a'),
    // H He Hf Hg Ho Hs
    1 << 31 |
    1 << ('e' - 'a') |
    1 << ('f' - 'a') |
    1 << ('g' - 'a') |
    1 << ('o' - 'a') |
    1 << ('s' - 'a'),
    // I In Ir
    1 << 31 |
    1 << ('n' - 'a') |
    1 << ('r' - 'a'),
    //j
    0,
    // K Kr
    1 << 31 |
    1 << ('r' - 'a'),
    //   La Li Lr Lu
    1 << ('a' - 'a') |
    1 << ('i' - 'a') |
    1 << ('r' - 'a') |
    1 << ('u' - 'a'),
    //   Md Mg Mn Mo Mt
    1 << ('d' - 'a') |
    1 << ('g' - 'a') |
    1 << ('n' - 'a') |
    1 << ('o' - 'a') |
    1 << ('t' - 'a'),
    // N Na Nb Nd Ne Ni No Np
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('d' - 'a') |
    1 << ('e' - 'a') |
    1 << ('i' - 'a') |
    1 << ('o' - 'a') |
    1 << ('p' - 'a'),
    // O Os
    1 << 31 |
    1 << ('s' - 'a'),
    // P Pa Pb Pd Pm Po Pr Pt Pu
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('d' - 'a') |
    1 << ('m' - 'a') |
    1 << ('o' - 'a') |
    1 << ('r' - 'a') |
    1 << ('t' - 'a') |
    1 << ('u' - 'a'),
    //q
    0,
    //   Ra Rb Re Rf Rh Rn Ru
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('e' - 'a') |
    1 << ('f' - 'a') |
    1 << ('h' - 'a') |
    1 << ('n' - 'a') |
    1 << ('u' - 'a'),
    // S Sb Sc Se Sg Si Sm Sn Sr
    1 << 31 |
    1 << ('b' - 'a') |
    1 << ('c' - 'a') |
    1 << ('e' - 'a') |
    1 << ('g' - 'a') |
    1 << ('i' - 'a') |
    1 << ('m' - 'a') |
    1 << ('n' - 'a') |
    1 << ('r' - 'a'),
    //  T Ta Tb Tc Te Th Ti Tl Tm
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('c' - 'a') |
    1 << ('e' - 'a') |
    1 << ('h' - 'a') |
    1 << ('i' - 'a') |
    1 << ('l' - 'a') |
    1 << ('m' - 'a'),
    // U
    1 << 31,
    // V
    1 << 31,
    // W
    1 << 31,
    //   Xe Xx
    1 << ('e' - 'a') |
    1 << ('x' - 'a'), // don't know if I should have Xx here or not?
    // Y Yb
    1 << 31 |
    1 << ('b' - 'a'),
    //   Zn Zr
    1 << ('n' - 'a') |
    1 << ('r' - 'a')
  };

  /**
   * 
   * @param ch
   * @return true if matches a one-character symbol X
   */
  public static boolean isValidSym1(char ch) {
    return (ch >= 'A' && ch <= 'Z' && elementCharMasks[ch - 'A'] < 0);
  }

  /**
   * 
   * @param ch1
   * @param ch2
   * @return true if matches a valid symbol Xy
   */
  public static boolean isValidSym2(char ch1, char ch2) {
    return (ch1 >= 'A' && ch1 <= 'Z' && ch2 >= 'a'
        && ch2 <= 'z' && ((elementCharMasks[ch1 - 'A'] >> (ch2 - 'a')) & 1) != 0);
  }

  /**
   * 
   * @param ch1
   * @param ch2
   * @return true if matches a two-character symbol, XX or Xx
   */
  public static boolean isValidSymNoCase(char ch1, char ch2) {
    return isValidSym2(ch1, ch2 < 'a' ? (char)(ch2 + 32) : ch2);
  }

  /**
   *
   * @param ch
   * @return true if matches FIRST character of some symbol Xx
   */
  private static boolean isValidSymChar1(char ch) {
    return (ch >= 'A' && ch <= 'Z' && elementCharMasks[ch - 'A'] != 0);
  }

//  static {
//    System.out.println(isValidSymChar1('A'));
//    System.out.println(isValidSym1('A'));
//    System.out.println(isValidSymNoCase('A', 'l'));
//    System.out.println(isValidSym1('W'));
//    System.out.println(isValidSymNoCase('A', 'L'));
//  }
  
}
