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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.api.JmolAdapter;

import javajs.api.GenericBinaryDocument;
import javajs.util.BC;
import javajs.util.Lst;

/**
 * A preliminary reader for CrystalMaker CMDX binary/text files.
 * 
 *
 */

public class CmdxReader extends CmdfReader {

  private int nSites;
  private int nAIC;
  private int nc;

  @Override
  protected void processBinaryDocument() throws Exception {
    binaryDoc.setStream(null, false);
    dumpFile(binaryDoc);
  }
  
  private void dumpFile(GenericBinaryDocument binaryDoc) {
    int nPre = 0;
    try {
    while(true) {
        long pt = binaryDoc.getPosition();
        String key = binaryDoc.readString(4);
        int len = checkLen(pt, key);
        switch (key) {
        case "WINS":
          //??
          break;
        case "PREV":
          writePreview(++nPre, len);
          break;
        case "STRS":
          nPre = readStructures(nPre, pt + len);
          return;
          default:
            binaryDoc.readBytes(len);
            break;
        }
    }
    } catch (Exception e) {
      System.out.println(binaryDoc.getPosition());
    }
  }

  private void writePreview(int i, int len) throws Exception {
    String fname = (String) htParams.get("fullPathName") + ".preview" + i + ".jpg";
    int p = fname.indexOf("file:/");
    if (p >= 0)
      fname = fname.substring(p + 6);
    String s = vwr.writeBinaryFile(fname, binaryDoc.readBytes(len));
    System.out.println(s);
  }
  
  private int readStructures(int nPre, long endPos) throws Exception {
    long pos = binaryDoc.getPosition();
    long i1 = pos + binaryDoc.readInt();
    System.out.println(i1);
    try {
      while (binaryDoc.getPosition() < i1) {
        long pt = binaryDoc.getPosition();
        String key = binaryDoc.readString(4);
        int len = checkLen(pt, key);
        switch (key) {
        case "PREV":
          writePreview(++nPre, len);
          break;
        case "TYPE":
        case "NAME":
        case "NOTE":
          System.out.println(binaryDoc.readString(len));
          break;
        case "NSIT":
          nSites = binaryDoc.readInt();
          System.out.println(nSites + " sites");
          break;
        case "NAIC":
          nAIC = binaryDoc.readInt();
          System.out.println(nAIC + " naic");
          break;
        case "CNUM":
          nc = binaryDoc.readInt();
          System.out.println(nc + " compounds");
          break;
        case "SITS":
          readSites(len);
          break;
        case "ATMS":
          readAtoms(len);
          break;
        case "BNDS":
          readBonds(len);
          break;
        default:
          binaryDoc.readBytes(len);
          break;
        }
      }
    } catch (Exception e) {
      System.out.println(binaryDoc.getPosition());
    }
    return nPre;
  }

  private void readSites(int len) throws Exception {
    long pos = binaryDoc.getPosition();
    int n = binaryDoc.readInt();
    for (int i = 0; i < n; i++) {
      pos = binaryDoc.getPosition();
      long i1 = pos + binaryDoc.readInt(); // len
      try {
        while (binaryDoc.getPosition() < i1) {
          long pt = binaryDoc.getPosition();
          String key = binaryDoc.readString(4);
          len = checkLen(pt, key);
          switch (key) {
          case "FRAC":
            double x = binaryDoc.readDouble();
            double y = binaryDoc.readDouble();
            double z = binaryDoc.readDouble();
            System.out.println(x + " " +  y + " " + z);
            break;
          case "LABL":
            System.out.println(binaryDoc.readString(len));
            break;
          case "VIS?":
          case "OCCU":
          case "*LBL":
          case "SPH$":
          case "POL$":
          case "COLR":
          case "SRAD":
          case "PRAD":
          case "MULT":
          case "CNUM":  
          default:
            binaryDoc.readBytes(len);
            break;
          }
        }
      } catch (Exception e) {
        System.out.println(binaryDoc.getPosition());
      }
    }
    return;
  }

  private int checkLen(long pt, String key) throws Exception {
    int len = binaryDoc.readInt();
    if (len > 1e6)
      throw new RuntimeException("CMDX len " + len);
    System.out.println(pt + "\t" + key + "\t" + len);
    return len;
  }

  private void readBonds(int len) throws Exception {
    long pos = binaryDoc.getPosition();
    int nBonds = binaryDoc.readInt();
    int i2 = binaryDoc.readInt();
    System.out.println(nBonds + " " + i2);
    long i1 = pos + len;
    int nbonds = 0;
    try {
      while (binaryDoc.getPosition() < i1) {
        long pt = binaryDoc.getPosition();
        String key = binaryDoc.readString(4);
        switch (key) {
        case "\u001c\0\0\0":
          nbonds++;
          System.out.println("NBONDS " + nbonds);
          continue;
        }
        len = checkLen(pt, key);
        switch (key) {
        case "PAIR":
        case "SPEC":
        default:
          binaryDoc.readBytes(len);
          break;
        }
      }
    } catch (Exception e) {
      System.out.println(binaryDoc.getPosition());
    }
    
    
  }

  private static String fixSpaceGroup(String sg) {
    int pt = sg.indexOf('\0');
    if (pt == 0)
      System.out.println("SYMM: empty;NO space group??");
    return (pt < 0 ? sg : sg.substring(0, pt)).trim();
  }

  private void readAtoms(int len) throws Exception {
    long pos = binaryDoc.getPosition();
    long i1 = binaryDoc.readInt();
    System.out.println(i1);
    i1 = pos + len;
    int nAtoms = 0;
      try {
        while (binaryDoc.getPosition() < i1) {
          long pt = binaryDoc.getPosition();
          String key = binaryDoc.readString(4);
          switch (key) {
          case "E\0\0\0":
          case "5\0\0\0":
            nAtoms++;
            System.out.println("NATOMS " + nAtoms);
            continue;
          }
          len = checkLen(pt, key);
          switch (key) {
          case "FRAC":
            double x = binaryDoc.readDouble();
            double y = binaryDoc.readDouble();
            double z = binaryDoc.readDouble();
            System.out.println(x + " " + y + " " + z);
            break;
          case "SITE":
            int site = binaryDoc.readInt();
            System.out.println("site=" + site);
            break;
          case "VISI":
          case "PRAD":
          default:
            binaryDoc.readBytes(len);
            break;
          }
        }
      } catch (Exception e) {
        System.out.println(binaryDoc.getPosition());
      }
    return;
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

