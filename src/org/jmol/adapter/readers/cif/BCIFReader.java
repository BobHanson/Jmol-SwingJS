/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
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
package org.jmol.adapter.readers.cif;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javajs.util.BC;
import javajs.util.Lst;
import javajs.util.MessagePackReader;
import javajs.util.SB;

/**
 * A very prelimary (1 day's work!) Binary CIF file reader. 
 * 
 * Right now it just opens the file and parses the data into
 * data structures. 
 * 
 * mmCIF files are recognized prior to class creation. Required fields include
 * one of:
 * 
 * _entry _database_PDB_ _pdbx_ _chem_comp.pdbx_type _audit_author.name
 * _atom_site.
 * 
 * see https://gist.github.com/dsehnal/b06f5555fa9145da69fe69abfeab6eaf
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class BCIFReader extends MMCifReader {

  /**
   * Save lots of time by only representing strings as stringData,
   * with associated arrays, not actually creating the strings. 
   */
  public static final boolean doBuildStrings = false;

  protected static int[] temp;

  protected Map<String, Lst<String>> categories = new LinkedHashMap<String, Lst<String>>();

  private Map<String, Decoder> cifMap; // input JSON-like map from MessagePack binary file  

  static class Decoder {

    private String type;
    private String key;
    private Object[] encodings;
    private Decoder offsetDecoder;
    private Decoder dataDecoder;
    private Decoder maskDecoder;
    private Map<String, Object> data;
    private byte[] byteData;

    private int btype;
    private int byteLen;
    private int srcType;

    private int mode;

    private final static int DELTA = 1;
    private final static int RUNLEN = 2;
    private final static int PACKED = 4;
    private final static int FIXED = 8;

//    private final static int B_BYTE = 1;
//    private final static int B_SHORT = 2;
//    private final static int B_INT = 3;
//    private final static int B_U1 = 4;
//    private final static int B_U2 = 5;

    boolean unsigned;

    private int srcSize;

    private int rowCount;
    String catname;
    String name;
    private boolean isLoop;
    private int byteCount;
    private int origin = Integer.MIN_VALUE;
    private float factor;
    
    private String stringData;
    private int[] indices;
    private int[] mask;
    private int[] offsets;
    private double[] fixedData;
    private int[] intData;
    private Object product;

    public Decoder(Map<String, Object> encoding, String ekey, Object byteData,
        Object stringData) {
      this(null, null, encoding, ekey, byteData, stringData);
    }

    public Decoder setRowCount(int rowCount, String catName, boolean isLoop) {
      this.rowCount = rowCount;
      this.isLoop = isLoop && rowCount == 1;
      this.catname = catName;
      if (dataDecoder != null)
        dataDecoder.setRowCount(rowCount, catName, isLoop);
      if (offsetDecoder != null)
        offsetDecoder.setRowCount(rowCount, catName, isLoop);
      if (maskDecoder != null)
        maskDecoder.setRowCount(rowCount, catName, isLoop);
      return this;
    }

    public Decoder(SB sb, String key, Map<String, Object> col) {
      this(sb, key, col, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private Decoder(SB sb, String key, Map<String, Object> map, String ekey,
        Object byteData, Object stringData) {
      this.key = key;
      Object data = (byteData == null ? map.get("data") : byteData);
      this.stringData = (String) stringData;
      if (data instanceof Map) {
        this.data = (Map<String, Object>) data;
        data = this.data.get("data");
      }
      if (data instanceof byte[]) {
        setByteData(data);
      }
      if (ekey == null) {
        if (this.data == null) {
          // mask
          encodings = (Object[]) map.get("encoding");
        } else {
          // data
          name = (String) map.get("name");
          encodings = (Object[]) this.data.get("encoding");

          Map<String, Object> mask = (Map<String, Object>) map.get("mask");
          if (mask != null) {
            // R4B3 or B4  (so byte)
            maskDecoder = new Decoder(mask, null, null, null);
            maskDecoder.key = key + ".mask";
          }
        }
      } else {
        encodings = (Object[]) map.get(ekey);
        stringData = map.get("stringData");
      }
      type = getDecoderType(encodings);

      if (maskDecoder != null)
        type += ".mask." + maskDecoder.toString();
      if (sb != null)
        sb.append(this + "\n");
    }

    private void setByteData(Object data) {
      byteData = (byte[]) data;
      byteLen = byteData.length;
    }

    @Override
    public String toString() {
      return type + (btype == 0 ? "[" + byteLen + "]"
          : "(srcSize/rowcount=" + srcSize + "/" + rowCount + " mode=" + mode + " bt=" + btype + " bl="
              + byteLen + ")")
          + (key == null ? "" : "\t" + key);
    }

    private static char getChar(Map<String, Object> ht, String key) {
      String s = (String) ht.get(key);
      return (s != null && s.length() > 0 ? s.charAt(0) : 0);
    }

    protected static int toInt(Object o) {
      return (o == null ? 0 : ((Number) o).intValue());
    }

    private static boolean toBool(Object o) {
      return (o == Boolean.TRUE);
    }

    @SuppressWarnings("unchecked")
    private String getDecoderType(Object[] encodings) {
      SB sb = new SB();
      int n = encodings.length;
      for (int i = 0; i < n; i++) {
        Map<String, Object> encoding = (Map<String, Object>) encodings[i];
        char kind = getChar(encoding, "kind");
        if (encoding.containsKey("min"))
          kind = 'Q';
        int srcSize = toInt(encoding.get("srcSize"));
        if (this.srcSize == 0)
          this.srcSize = srcSize;
        sb.appendC(kind);
        switch (kind) {
        case 'B':
          btype = toInt(encoding.get("type"));
          byteCount = 1 << (((btype - 1) % 3));
          sb.appendI(btype);
          continue;
        case 'F':
          factor = toInt(encoding.get("factor"));
          mode |= FIXED;
          break;
        case 'R':
          mode |= RUNLEN;
          break;
        case 'D':
          origin = toInt(encoding.get("origin"));
          mode |= DELTA;
          break;
        case 'S':
          dataDecoder = new Decoder(encoding, "dataEncoding", byteData,
              encoding.get("stringData"));
          offsetDecoder = new Decoder(encoding, "offsetEncoding",
              encoding.get("offsets"), null);
          sb.appendC('.');
          sb.append(dataDecoder.toString());
          sb.appendC('.');
          sb.append(offsetDecoder.toString());
          sb.appendC('.');
          continue;
        case 'I':
          mode |= PACKED;
          unsigned = toBool(encoding.get("isUnsigned"));
          sb.appendI(toInt(encoding.get("byteCount")));
          if (unsigned)
            sb.appendC('u');
          continue;
        default:
          sb.appendC('?');
          continue;
        }
        int srcType = toInt(encoding.get("srcType"));
        if (this.srcType == 0)
          this.srcType = srcType;
        sb.appendI(srcType);

      }
      return sb.toString();
    }

    public Object decode() {
      if (product != null)
        return product;
      
      
      if (dataDecoder != null) {
        String s = dataDecoder.stringData;
        if (rowCount == 1) {
          return product = (isLoop ? new String[] { s } : s);
        }
        indices = (int[]) dataDecoder.decode();
        mask = null;
        if (maskDecoder != null) {
          mask = (int[]) maskDecoder.decode();
        }
        offsets = (int[]) offsetDecoder.decode();
        String[] data = new String[rowCount];
        stringData = dataDecoder.stringData;
 
        return (product = (doBuildStrings ? buildStrings() : stringData));
      }
      int mode = this.mode;
      int[] data;
      int[] run = null;
      if ((mode & RUNLEN) == RUNLEN) {
        run = getTemp(rowCount);
      }
      if ((mode & PACKED) == PACKED) {
        // account for RUNLEN and DELTA origin here
        int n = (srcSize == 0 ? rowCount : srcSize);
        data = unpackInts(byteData, byteCount, srcSize, unsigned, origin, run, 0,
            false);
      } else {
        data = bytesToInt(byteData, byteCount, rowCount, unsigned, origin, run,
            0, false);
      }
      if ((mode & FIXED) == FIXED) {
        fixedData= new double[rowCount];
        for (int i = rowCount; --i >= 0;)
          fixedData[i] = data[i] / factor;
        return product = fixedData;
      }
      return product = intData = data;
    }

    private String[] buildStrings() {
      String[] data = new String[rowCount];
      int len = dataDecoder.stringData.length();
      for (int i = 0; i < rowCount; i++) {
        switch (mask == null ? 0 : mask[i]) {
        case 0:
          int pt = indices[i];
          data[i] = dataDecoder.stringData.substring(offsets[pt++],
              (pt == rowCount ? len : offsets[pt]));
          break;
        case 1:
          data[i] = ".";
          break;
        case 2:
          data[i] = "?";
          break;
        }
      }
      return data;
    }

    /**
     * BCIF unpack and expand runlength encoding if need be.
     * 
     * @param b
     * @param byteLen
     * @param rowCount
     * @param unsigned
     * @param origin 
     * @param run 
     * @param pt
     * @param big
     * @return int[]
     */
    public static int[] bytesToInt(byte[] b, int byteLen, int rowCount,
                                   boolean unsigned, int origin, int[] run,
                                   int pt, boolean big) {
      if (b == null)
        return null;
      int n = b.length / byteLen;
      int[] ret = new int[rowCount];
      int[] a = (run == null ? ret : run);
      int ii;
      switch (byteLen) {
      case 1:
        for (int i = 0, j = pt; i < n; i++, j++) {
          ii = b[j] & 0xFF;
          a[i] = (unsigned ? ii : ii > Byte.MAX_VALUE ? ii - 0x100 : ii);
        }
        break;
      case 2:
        for (int i = 0, j = pt; i < n; i++, j += 2) {
          ii = BC.bytesToShort(b, j, big);
          a[i] = (unsigned ? ii & 0xFFFF : ii);
        }
        break;
      case 4:
        for (int i = 0, j = pt; i < n; i++, j += 4)
          a[i] = BC.bytesToInt(b, j, big);
        break;
      }
      if (run != null) {
        // run-length encoded
        for (int p = 0, i = 0; i < n; i++) {
          int val = a[i];
          for (int j = a[++i]; --j >= 0;)
            ret[p++] = val;
        }
      }
      if (origin != Integer.MIN_VALUE) {
        for (int i = 0; i < n; i++) {
          origin = ret[i] = origin + ret[i];
        }
      }
      return ret;
    }

    /**
     * BCIF unpack and expand runlength encoding if need be.
     * 
     * @param b
     * @param byteLen
     * @param srcSize
     * @param unsigned
     * @param origin 
     * @param run 
     * @param pt
     * @param big
     * @return int[]
     */
    public static int[] unpackInts(byte[] b, int byteLen, int srcSize,
                                   boolean unsigned, int origin, int[] run,
                                   int pt, boolean big) {
      if (b == null)
        return null;
      int[] ret = new int[srcSize];
      int[] a = (run == null ? ret : run);
      int max;
      switch (byteLen) {
      case 1:
        max = (unsigned ? 0xFF : Byte.MAX_VALUE);
        for (int i = 0, n = b.length, offset = 0; pt < n;) {
          int val = b[pt++];
          if (unsigned)
            val = val & 0xFF;
          if (val == max || val == Byte.MIN_VALUE) {
            offset += val;
          } else {
            a[i++] = val + offset;
            offset = 0;
          }
        }
        break;
      case 2:
        max = (unsigned ? 0xFFFF : Short.MAX_VALUE);
        for (int i = 0, n = b.length / 2, offset = 0; pt < n;) {
          int val = BC.bytesToShort(b, (pt++) << 1, big);
          if (unsigned)
            val = val & 0xFFFF;
          if (val == max || val == Short.MIN_VALUE) {
            offset += val;
          } else {
            a[i++] = val + offset;
            offset = 0;
            //          if ( (i % 100) == 0)
            //          System.out.println(i + " " + ((ret[i-1])+ 666621)/1000.);
          }
        }
        break;
      }
      if (run != null) {
        // run-length encoded
        for (int p = 0, i = 0; p < srcSize; i++) {
          int val = a[i];
          for (int j = a[++i]; --j >= 0;)
            ret[p++] = val;
        }
      }
      if (origin != Integer.MIN_VALUE) {
        for (int i = 0; i < srcSize; i++) {
          origin = ret[i] = origin + ret[i];
        }
      }
      return ret;
    }


  }

  @Override
  protected void addHeader() {
    // no header for this type
  }

  public static int[] getTemp(int n) {
    // supply a temporary array
    if (temp == null || temp.length < n)
      temp = new int[n];
    return temp;
  }

  public static void clearTemp() {
    temp = null;
  }
  
  /**
   * standard set up
   * 
   * @param fullPath
   * @param htParams
   * @param reader
   */
  @Override
  protected void setup(String fullPath, Map<String, Object> htParams,
                       Object reader) {
    isBinary = true;
    isMMCIF = true;
    iHaveFractionalCoordinates = false;
    setupASCR(fullPath, htParams, reader);
  }

  @Override
  protected void processBinaryDocument() throws Exception {

    // load xxx.mmtf filter "..." options 

    // NODOUBLE -- standard PDB-like structures
    // DSSP2 -- use Jmol's built-in DSSP 2.0, not file's DSSP 1.0 

    isDSSP1 = !checkFilterKey("DSSP2");
    applySymmetryToBonds = true;
    processMap();
  }

  @SuppressWarnings("unchecked")
  private void processMap() throws Exception {
    try {

      long t = System.currentTimeMillis();

      Map<String, Object> msgMap = (new MessagePackReader(binaryDoc, false)).readMap();
      binaryDoc.close();
      Map<String, Object> dataBlock = (Map<String, Object>) ((Object[]) msgMap
          .get("dataBlocks"))[0];

      cifMap = new LinkedHashMap<String, Decoder>();

      SB sb = null;//new SB();
      //dumpMap(sb, dataBlock, "");
      Object[] categories = (Object[]) dataBlock.get("categories");
      for (int j = categories.length; --j >= 0;) {
        Map<String, Object> cat = (Map<String, Object>) categories[j];
        int rowCount = Decoder.toInt(cat.get("rowCount"));
        String catName = ((String) cat.get("name")).toLowerCase();
        Object[] columns = (Object[]) cat.get("columns");
        Lst<String> lst = new Lst<String>();
        this.categories.put(catName, lst);
        int ncol = columns.length;
        for (int k = ncol; --k >= 0;) {
          Map<String, Object> col = (Map<String, Object>) columns[k];
          key = catName + "_" + ((String) col.get("name")).toLowerCase();
          lst.addLast(key);
          Decoder d = new Decoder(sb, key, col).setRowCount(rowCount, catName,
              ncol != 1);
          Object o = d.decode();
          //System.out.println(toStr(o));
          cifMap.put(key, d);
        }  
      }
      
      // [ We now have a completely parsed structure of the contents of the file. ]
      
      
      //vwr.writeTextFile("c:/temp/t.log", sb.toString());

//      double[] cartx = (double[]) getVal("_atom_site_cartn_y");
//
//      Object o;
//      o = getVal("_pdbx_struct_assembly_auth_evidence_assembly_id");

      System.out.println("BCIFReader parsed and decoded ms: " + (System.currentTimeMillis() - t));
      pdbID = (String) getVal("_entry_id");

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private Object getVal(String key) {
    Object o = cifMap.get(key);
    Object val = (o instanceof Decoder ? ((Decoder) o).decode() : o);
    return val;
  }

  Object toStr(Object o) {
    if (o instanceof int[])
      return toStrI((int[]) o);
    if (o instanceof byte[])
      return toStrB((byte[]) o);
    if (o instanceof double[])
      return toStrD((double[]) o);
    if (o instanceof String)
      return o;
      return toStrO((Object[]) o);
  }

  // B F I R D I S  
  //  type Encoding = 
  //      | ByteArray 
  //      | FixedPoint
  //      | IntervalQuantization 
  //      | RunLength 
  //      | Delta 
  //      | IntegerPacking 
  //      | StringArray

  private Object toStrO(Object[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendO(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...").appendI(o.length);
    sb.appendC(']');
    return sb.toString();
  }

  private Object toStrI(int[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendI(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...");
    sb.appendC(']');
    return sb.toString();
  }

  private Object toStrB(byte[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendI(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...");
    sb.appendC(']');
    return sb.toString();
  }

  private Object toStrD(double[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendD(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...");
    sb.appendC(']');
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  void dumpMap(SB sb, Map<String, Object> value, String key) {
    for (String k : value.keySet()) {
      Object v = value.get(k);
      if (v instanceof Map) {
        dumpMap(sb, (Map<String, Object>) v, key + ":" + k);
      } else if (v instanceof String || v instanceof Number
          || v instanceof Boolean) {
        sb.append(key + ":" + k + "=" + v + "\n");
      } else if (v instanceof Object[]) {
        Object[] a = (Object[]) v;
        for (int i = 0; i < a.length; i++)
          dumpMap(sb, (Map<String, Object>) a[i],
              key + ":" + k + "[" + i + "]");
      } else if (k.equals("offsets") || k.equals("data")) {
        sb.append(key + ":" + k + " is " + ((byte[]) v).length + " "
            + Arrays.toString((byte[]) v) + "\n");
      } else if (v instanceof byte[]) {
        sb.append(
            key + ":" + k + " is byte[" + ((byte[]) v).length + "]" + "\n");
      } else {
        sb.append(key + ":" + k + " is " + v.getClass().getName() + "\n");
      }
    }
  }

  @Override
  public void applySymmetryAndSetTrajectory() throws Exception {
    super.applySymmetryAndSetTrajectory();
  }

}

//EBI employs only these (from 8glv)

//ByteArray {
//  kind = "ByteArray"
//  type   1      2       3       4       5
//  type: Int8 | Int16 | Int32 | Uint8 | Uint16 | Uint32 | Float32 | Float64
//}
// bitcount 1      2       4       1       2  == 1 << (((type -1 )% 3));

// B1 B2 B3 B4 B5
// D3
// F33
// I1 I1u I2 I2u
// R3
// S

//or, more specifically:

//  B3          int
//  B4          byte (mask)

//  R3 B3        rl-int              rldecode32
//  R4 B3        byte-rl-int (mask) 
//  R3 I1B1
//  R3 I1uB4
//  R3 I2uB5

//  S         string

//  D3 I1B1    delta-packed-byte
//  D3 I2B2    delta-packed-short
//  D3 I1uB4   delta-packed-ubyte
//  D3 I2uB5   delta-packed-ushort

//  D3R3 B3    delta-rl-int
//  D3R3 I1B1  delta-rl-packed-byte   rldecode32Delta
//  D3R3 I1uB4 delta-rl-packed-ubyte  rldecode32Delta
//  D3R3 I2B2  delta-rl-packed-short  rldecode32DeltaU
//  D3R3 I2uB5 delta-rl-packed-ushort rldecode32DeltaU

//  F33 D3 B3    fixed-delta-int
//  F33 D3 I1B1  fixed-delta-packed-byte
//  F33 D3 I1uB4 fixed-delta-packed-ubyte
//  F33 D3 I2B2  fixed-delta-packed-short
//  F33    I2B2  fixed-packed-short
//  F33 R3 I1uB4 fixed-rl-packed-ubyte
//  F33 R3 I2uB5 fixed-rl-packed-ushort

//  I1uB4       packed-ubyte

// so for unpacking, we have: B3, B4, I1B1, I1uB4, I2B2, I2uB5 

//MessagePackReader:
//  
//  switch (type) {
//  case 1:
//    return getFloats(b, n, 1, 12, big);
//  case 2: // 1-byte
//  case 3: // 2-byte
//  case 4: // 4-byte
//    return getInts(b, n, 12, big);
//  case 5:
//    return rldecode32ToStr(b, 12);
//  case 6:
//    return rldecode32ToChar(b, n, 3, big);
//  case 7:
//    return rldecode32(b, n, 3, big);
//  case 8:
//    return rldecode32Delta(b, n, 3, big);
//  case 9:
//    return rldecodef(b, n, param, 3, big);
//  case 10:
//    return unpack16Deltaf(b, n, param, 6, big);
//  case 11:
//    return getFloats(b, n, param, 12, big);
//  case 12: // two-byte
//    return unpackf(b, 2, n, param, 6, 0, big);
//  case 13: // one-byte
//    return unpackf(b, 1, n, param, 12, 0, big);
//  case 14: // two-byte
//    return unpack(b, 2, n, 6, big);
//  case 15: // one-byte
//    return unpack(b, 1, n, 12, big);
