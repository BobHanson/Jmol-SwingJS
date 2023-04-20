/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
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
package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.api.JmolAdapter;

import org.jmol.adapter.smarter.Atom;

import javajs.util.BC;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * A reader for CrystalMaker CMDF binary/text files.
 * 
 *
 */

public class CmdfReader extends AtomSetCollectionReader {

  @Override
  public void initializeReader() throws Exception {
      setFractionalCoordinates(true);
  }

  @Override
  protected void processBinaryDocument() throws Exception {
    binaryDoc.setStream(null, false);
    binaryDoc.seek(28);
    int len = binaryDoc.readInt();
    System.out.println("file length: " + len + " " + Integer.toHexString(len));
    seek("CELL", 32);
    double[] uc = new double[6];
    for (int i = 0; i < 6; i++) {
      uc[i] = fixDouble(binaryDoc.readFloat());
    }
    setUnitCell(uc[0], uc[1], uc[2], uc[3], uc[4], uc[5]);
    seek("SYMM", -1);
    //peek(20);
    String sg = fixSpaceGroup(binaryDoc.readString(20));
    setSpaceGroupName(sg);
    System.out.println("Space group is " + sg);
    readAtoms();
    System.out.println("done");
  }
  
  private static String fixSpaceGroup(String sg) {
    int pt = sg.indexOf('\0');
    if (pt == 0)
      System.out.println("SYMM: empty;NO space group??");
    return (pt < 0 ? sg : sg.substring(0, pt)).trim();
  }

  private void readAtoms() throws Exception {
    seek("AUN7", 32);
    //peek(100);
    int nSites = binaryDoc.readInt();
    System.out.println(nSites + " sites");
    for (int i = 0; i < nSites; i++)
      readSite();
    
  }

  private void readSite() throws Exception {
    int nOccupants = binaryDoc.readByte();
    int pt0 = (int) binaryDoc.getPosition();
    //peek(nOccupants * 10 + 40);
    Atom[] atoms = new Atom[nOccupants];
    for (int i = 0; i < nOccupants; i++) {
      Atom a = atoms[i] = new Atom();
      char ch2 = (char) binaryDoc.readByte();
      char ch1 = (char) binaryDoc.readByte();
      a.elementSymbol = getSymbol("" + ch1 + ch2);
      if (JmolAdapter.getElementNumber(a.elementSymbol) == 0) {
        System.out.println("ELEMENT error " + a.elementSymbol + " " + fileName);
      }
      a.foccupancy = fixDouble(binaryDoc.readFloat());
      asc.addAtom(a);
    }
    int elementIndex = binaryDoc.readInt();
    String sym0 = atoms[0].elementSymbol;
    String name = readString();
    int valence = binaryDoc.readInt();
    for (int i = 0; i < nOccupants; i++) {
      atoms[i].atomName = (i == 0 || sym0.length() > name.length() ? name
          : atoms[i].elementSymbol + name.substring(sym0.length()));
//      System.out.println("ATOM " + i + " eindex=" + elementIndex + " " + atoms[i].atomName + " valence " + valence + " occ=" + atoms[i].foccupancy);
    }
    int unk3s = binaryDoc.readShort() & 0xFFFF;
    float x = binaryDoc.readFloat();
    float y = binaryDoc.readFloat();
    float z = binaryDoc.readFloat();
    for (int i = 0; i < nOccupants; i++) {
      setAtomCoordXYZ(atoms[i], fixDouble(x), fixDouble(y), fixDouble(z));
    }
    float index2 = binaryDoc.readInt()/32f; // sometimes
    int unk4b = binaryDoc.readByte() & 0xFF;
    int siteNumber = binaryDoc.readShort();
    int unk5b = binaryDoc.readByte() & 0xFF; // 0
    int wyn = binaryDoc.readInt(); // Wyckoff!
    int wyabc = binaryDoc.readByte(); // 1 is a?
    String wyckoff = "" + wyn + (char) (0x60 + wyabc);
    System.out.println("SITE " + siteNumber + " occ=" + nOccupants 
        + " " + atoms[0].elementSymbol + " " + atoms[0].atomName + " " + wyckoff + " " + atoms[0] + (nOccupants > 1 ? atoms[1].atomName : "")
        + " valence=" + valence
        + " " + index2 
        + " " + Integer.toHexString(unk3s) 
        + " " + Integer.toHexString(unk4b) 
        + " " + Integer.toHexString(unk5b));
    return;
  }

  private byte[] buf = new byte[100];

  private String readString() throws Exception {
    int n = binaryDoc.readByte();
    binaryDoc.readByteArray(buf,  0,  n);
    return new String(buf, 0, n);
  }

  private void peek(int n) {
    long p0 = binaryDoc.getPosition();
    int p = (int) p0;
    byte[] bytes = new byte[4];
    try {
      for (int i = 0; i < n; i++) {
        binaryDoc.seek(p++);
        binaryDoc.readByteArray(bytes, 0, 4);
        int ival = BC.bytesToInt(bytes, 0, false);
        float fval = BC.bytesToFloat(bytes, 0, false);
        System.out.println(p + " "
            + Integer.toHexString(bytes[0] < 0 ? 256 + bytes[0] : bytes[0])
            + " " + (bytes[0] >= '0' && bytes[0] <= 'z' ? (char) bytes[0] : ".")
            + " " + Integer.toHexString(ival) + " " + fval);
      }
    } catch (Exception e) {
      // eof? ignore
    }
    binaryDoc.seek(p0);
  }

//  private void seekTest(float f, int len) throws Exception {
//    len -= 4;
//    for (int i = 0; i < 0; i++) {
//      int pt = i + 140;
//      binaryDoc.seek(pt);
//      float v = binaryDoc.readFloat();
//      if (v == f) {
//        System.out.println(pt + " " + f);
//        return;
//      }
//      
//    }
//  }
//
  private static double fixDouble(double d) {
    return Math.round(d * 1000.) / 1000.;
  }

  private int seek(String label, int where) throws Exception {
    byte[] bytes = label.getBytes();
    if (where > 0)
      binaryDoc.seek(where);
    int p = (where >= 0 ? where : (int) binaryDoc.getPosition());
    System.out.println("looking for " + label + " @" + p);
    int off = 0;
    int n = bytes.length;
    int p0 = p;
    while (off < n) {
      byte b = binaryDoc.readByte();
      p++;
      if (b == bytes[off]) {
        off++; 
      } else if (off > 0) {
        binaryDoc.seek(p = p0 = p0 + 1);
        off = 0;
      }
    }
    System.out.println(label + " found at " + (p - n));
    return p;
  }

  private static String getSymbol(String sym) {
    if (sym == null)
      return "Xx";
    int len = sym.length();
    if (len < 2)
      return sym;
    char ch1 = sym.charAt(1);
    if (ch1 >= 'a' && ch1 <= 'z')
      return sym.substring(0, 2);
    return "" + sym.charAt(0);
  }

}

