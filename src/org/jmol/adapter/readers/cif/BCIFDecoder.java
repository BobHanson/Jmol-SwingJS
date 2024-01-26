package org.jmol.adapter.readers.cif;

import java.util.Map;

import javajs.util.BC;
import javajs.util.SB;

/**
 * A class to quickly (2-15 ms) extract and maintain a set of BCIF decoders from
 * an MessagePack-compressed BCIF file.
 * 
 * Most of this class is a set of debugging methods that helped immensely in
 * understanding how this all works.
 * 
 * Each CIF category (_atom_site, _cell, etc.) is maintained as a set of
 * "columns" (as generally presented as a loop_ in a standard CIF file.
 * 
 * Each column is represented here as a single BCIFDecoder. That deccoder
 * maintains the category's row count.
 * 
 * In addition, each BCIFDecoder contains a data BCIFDecoder and (optionally) a
 * mask BCIFDecoder. The column's raw byte[] data and StringData, if present, is
 * given to the dataDecoder; the raw byte[] mask is given to the maskDecoder.
 * 
 * In the case of String data, the dataDecoder itself maintains two additional
 * decoders -- a data decoder and an offset decoder.
 * 
 * None of these decoders are run initially. They are just present if needed.
 * 
 * When BCIFReader (extends MMCifReader) is initialized, it creates an instance
 * of BCIFDataParser (extends CIFDataParser).
 * 
 * It is BCIFDataParser's job to maintain the list of columns as maps and, as
 * needed, a temporary list of BCIFDecoder objects for the category currently
 * being processed. And it is this parser that CIFReader turns to in order to
 * get its individual int, double, and String data. (In Jmol-SwingJS, it is
 * double; in legacy, these will be floats.
 * 
 * The process works as follows:
 * 
 * BCIFReader is created and associated with a BinaryDocument object.
 * 
 * BCIFReader creates BCIFParser.
 * 
 * BCIFParser extracts a Map<String,Object> from the document using
 * MeassagePackReader.
 * 
 * BCIFParser runs through the extracted categories, selecting only the ones
 * that are relevant for Jmol's purposes.
 * 
 * BCIFReader then calls the same loop- and entry-processing methods that
 * (superclasses) MMCifReader and CifReader call. The difference is that here we
 * can choose to do this in any order, whereas in CIFReader has to take them in
 * the order presented. (Still, we just do it in the order presented, actally.)
 * 
 * The very first thing that happens next (in CIFReader for standard CIF files
 * or in BCIFReader for BCIF files) is to initialize a category processing
 * stage. In this step, the various columns of file data are matched to the
 * various needs of Jmol. For example, the column for _atom_site.cartn_x is
 * matched with the ATOM_SITE_CARTN_X constant.
 * 
 * We could certainly be more efficient in the next stage, where we run through
 * each of the rows of CIF data and construct moleular objects -- atoms, bonds,
 * helices, sheets, etc.. But I opted to keep BCIFReader simple (as I only gave
 * this a couple days for programming) and just feed the BCIF data to CIFReader
 * as though it has read lines of column data from a standard CIF file.
 * 
 * In the end, it works, and it is very fast. Goals met. 
 * 
 * Bob Hanson, 2024.01.26
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
class BCIFDecoder {

  final static int IGNORE = 0;
  final static int INT    = 1;
  final static int FIXED  = 2;
  final static int STRING = 3;

  /**
   * int, double (float in legacy Jmol), or String
   */
  int dataType = INT;

  private String type;
  private String key;
  private Object[] encodings;
  private BCIFDecoder offsetDecoder;
  private BCIFDecoder dataDecoder;
  private BCIFDecoder maskDecoder;
  private Map<String, Object> data;

  private int btype;
  private int byteLen;
  private int srcType;

  /**
   * bit mask for operations of packing, run length compression, and delta
   * values
   */
  private int mode;

  private final static int MODE_DELTA = 1;
  private final static int MODE_RUNLEN = 2;
  private final static int MODE_PACKED = 4;
  private final static int MODE_FIXED = 8;

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
  private int byteCount;
  private int origin = Integer.MIN_VALUE;
  private float factor;

  // inputs

  private byte[] byteData;
  private String stringData;
  private int stringLen;

  // outputs

  private double[] floatDoubleData;
  private int[] intData;
  private int[] indices;
  private int[] mask;
  private int[] offsets;

  boolean isDecoded;
  private char kind;
  private int packingSize;
  private String dtype;

  /**
   * stan
   * @param key
   * @param col
   * @param sb
   */
  public BCIFDecoder(String key, Map<String, Object> col, SB sb) {
    this(sb, key, col, null, null);
  }

  public BCIFDecoder(Map<String, Object> encoding, String ekey, Object byteData, SB sb) {
    this(sb, ekey, encoding, ekey, byteData);
  }

  /**
   * 
   * @param sb
   *        debugging only
   * @param key
   *        debugging only -- map key
   * @param map
   *        originating map value for this decoder
   * @param ekey
   *        key for data, mask, or offset decoder within this map
   * @param byteData
   *        byte data being passed on from above constructor
   */
  @SuppressWarnings("unchecked")
  private BCIFDecoder(SB sb, String key, Map<String, Object> map, String ekey,
      Object byteData) {
    this.key = key;
    Object data = (byteData == null ? map.get("data") : byteData);
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
          maskDecoder = new BCIFDecoder(mask, null, null, sb);
          maskDecoder.dtype = "m";
        }
      }
    } else {
      encodings = (Object[]) map.get(ekey);
      dtype = ekey.substring(0, 1);
    }
    initializeEncodings(encodings, sb);
    if (sb != null) {
      type = debugGetDecoderType(encodings);
      if (maskDecoder != null)
        type += ".mask." + maskDecoder.toString();
      sb.append(this + "\n");
    }
  }

  /**
   * after constructor, added information
   * 
   * @param rowCount
   * @param catName
   *        more than one column constitutes a loop
   * @return this decoder
   */
  BCIFDecoder setRowCount(int rowCount, String catName) {
    this.rowCount = rowCount;
    this.catname = catName;
    return this;
  }

  private void setByteData(Object data) {
    byteData = (byte[]) data;
    byteLen = byteData.length;
  }

  @Override
  public String toString() {
    return type + (dtype == null ? "" : dtype) + (btype == 0 ? "[" + byteLen + "]"
        : "(srcSize/rowcount=" + srcSize + "/" + rowCount + " mode=" + mode
            + " bt=" + btype + " bl=" + byteLen + " sl=" + stringLen + ")")
        + (key == null ? "" : ":" + key);
  }

  private static char getMapChar(Map<String, Object> ht, String key) {
    String s = (String) ht.get(key);
    return (s != null && s.length() > 0 ? s.charAt(0) : 0);
  }

  protected static int geMapInt(Object o) {
    return (o == null ? 0 : ((Number) o).intValue());
  }

  private static boolean getMapBool(Object o) {
    return (o == Boolean.TRUE);
  }

  @SuppressWarnings("unchecked")
  private void initializeEncodings(Object[] encodings, SB sb) {
    int n = encodings.length;
    for (int i = 0; i < n; i++) {
      Map<String, Object> encoding = (Map<String, Object>) encodings[i];
      char kind = getMapChar(encoding, "kind");
      if (encoding.containsKey("min")) {
        // not implemented. 
        kind = 'Q';
        dataType = IGNORE;
      }
      if (this.kind == 0)
        this.kind = kind;
      switch (kind) {
      case 'F':
        factor = geMapInt(encoding.get("factor"));
        dataType = FIXED;
        mode |= MODE_FIXED;
        break;
      case 'D':
        origin = geMapInt(encoding.get("origin"));
        mode |= MODE_DELTA;
        break;
      case 'R':
        mode |= MODE_RUNLEN;
        srcSize = geMapInt(encoding.get("srcSize"));
        break;
      case 'I':
        mode |= MODE_PACKED;
        packingSize = geMapInt(encoding.get("srcSize"));
        if (srcSize == 0)
          srcSize = packingSize;
        unsigned = getMapBool(encoding.get("isUnsigned"));
        continue;
      case 'B':
        btype = geMapInt(encoding.get("type"));
        byteCount = (btype == 33 ? 8 : btype == 32 ? 4 : 1 << (((btype - 1) % 3)));
        if (btype >= 32) {
          dataType = FIXED;          
        }
        continue;
      case 'S':
        dataType = STRING;
        stringData = (String) encoding.get("stringData");
        stringLen = stringData.length();
        dataDecoder = new BCIFDecoder(encoding, "dataEncoding", byteData, sb);
        offsetDecoder = new BCIFDecoder(encoding, "offsetEncoding",
            encoding.get("offsets"), sb);
        continue;
      }
      if (srcType == 0) {
        // DELTA, RUNLEN, and FIXED
        srcType = geMapInt(encoding.get("srcType"));
      }
    }
  }

  public BCIFDecoder finalizeDecoding(SB sb) {
    if (isDecoded)
      return this;
    if (sb != null)
      sb.append("finalizing " + this + "\n");
    isDecoded = true;
    mask = (maskDecoder == null ? null
        : (int[]) maskDecoder.finalizeDecoding(sb).intData);
    if (mask != null && !haveCheckMask(mask)) {
      // no need to contiue;
      if (sb != null)
        sb.append("no valid data (mask completely '.' or '?'" + "\n");
      dataType = IGNORE;
      return null;
    }
    if (mask != null && sb != null)
      sb.append("mask = " + debugToStr(mask) + "\n");
    if (dataDecoder != null) {
      // string data - create indices, offsets. 
      indices = dataDecoder.finalizeDecoding(sb).intData;
      offsets = offsetDecoder.finalizeDecoding(sb).intData;
      if (sb != null) {
        sb.append("stringData = " + debugToStr(stringData) + "\n");
        sb.append("indices = " + debugToStr(indices) + "\n");
        sb.append("offsets = " + debugToStr(offsets) + "\n");
      }

    } else {
      if (sb != null)
        sb.append("bytes->int " + byteData.length / byteCount + " rc="
            + rowCount + " ps=" + packingSize + "\n");
      int[] run = null;
      int len = srcSize;
      if ((mode & MODE_RUNLEN) == MODE_RUNLEN) {
        run = getTemp(srcSize);
        len = this.srcSize;
      }
      if ((mode & MODE_PACKED) == MODE_PACKED) {
        // account for RUNLEN and DELTA origin here
        intData = unpackInts(byteData, byteCount, len, unsigned, origin,
            run);
      } else if (btype == 32 || btype == 33) {
        floatDoubleData = bytesToFixedPt(byteData, btype == 32 ? 4 : 8);
      } else {
        intData = bytesToInt(byteData, byteCount, len, unsigned, origin,
            run);
      }
    }
    return this;
  }

  private static boolean haveCheckMask(int[] mask) {
    for (int i = mask.length; --i >= 0;) {
      if (mask[i] == 0)
        return true;
    }
    // all '?' or '.' -- ignore
    return false;
  }

  final static String nullString = "\0";

  static final int UNKNOWN_INT = Integer.MIN_VALUE;

  public String getStringValue(int row) {
    if (dataType != STRING || mask != null && mask[row] != 0)
      return nullString;
    int pt = indices[row];
    return stringData.substring(offsets[pt++],
        (pt == rowCount ? stringLen : offsets[pt]));
  }

  public int getIntValue(int row) {
    return (dataType != INT || mask != null && mask[row] != 0 ? UNKNOWN_INT
        : intData[row]);
  }

  public double getFixedPtValue(int row) {
    return (dataType != FIXED || mask != null && mask[row] != 0 ? Double.NaN
        : floatDoubleData == null ? intData[row] / factor :
          floatDoubleData[row]);
  }

  private double[] bytesToFixedPt(byte[] b, int byteLen) {
    if (b == null)
      return null;
    int n = b.length / byteLen;
    double[] a = new double[n];
    try {
      switch (byteLen) {
      case 4: // four bytes -> float32
        for (int i = 0, j = 0; i < n; i++, j += 4) {
          a[i] = BC.bytesToFloat(b, j, false);
        }
        break;
      case 8: // 8 bytes -> double
          for (int i = 0, j = 0; i < n; i++, j += 8) {
                a[i] = BC.bytesToDoubleToFloat(b, j, false);
          }
        break;
      }
    } catch (Exception e) {
      // TODO
    }
    return a;
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
   * @return int[]
   */
  public static int[] bytesToInt(byte[] b, int byteLen, int rowCount,
                                 boolean unsigned, int origin, int[] run) {
    if (b == null)
      return null;
    // We need a temporary array for RLE, since, for example, 
    // [10 2 11 1] will end up [10 10 11], 
    // and the writing of the second 10 would overwrite the 11.
    int n = b.length / byteLen;
    int[] ret = new int[rowCount == 0 ? n : rowCount];
    int[] a = (run == null ? ret : run);

    // We operating on a, which might be the return, or not. 
    // We take into account signed or unsigned only for byte and short conversion.
    int ii;
    switch (byteLen) {
    case 1: // byte -> int
      for (int i = 0, j = 0; i < n; i++, j++) {
        ii = b[j] & 0xFF;
        a[i] = (unsigned ? ii : ii > 0xEF ? ii - 0x100 : ii);
      }
      break;
    case 2: // two bytes -> short -> int
      for (int i = 0, j = 0; i < n; i++, j += 2) {
        ii = BC.bytesToShort(b, j, false);
        a[i] = (unsigned ? ii & 0xFFFF : ii);
      }
      break;
    case 4: // four bytes -> int
      for (int i = 0, j = 0; i < n; i++, j += 4) {
        
        a[i] = BC.bytesToInt(b, j, false);
      }
      break;
    }

    // Run-Length Encoding duplication
    if (run != null) {
      for (int p = 0, i = 0; i < n;) {
        int val = a[i++];
        for (int j = a[i++]; --j >= 0;)
          ret[p++] = val;
      }
    }

    // Delta offset from origin
    if (origin != Integer.MIN_VALUE) {
      for (int i = 0; i < rowCount; i++) {
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
   * @return int[]
   */
  public static int[] unpackInts(byte[] b, int byteLen, int srcSize,
                                 boolean unsigned, int origin, int[] run) {
    if (b == null)
      return null;
    int[] ret = new int[srcSize];
    int[] a = (run == null ? ret : run);

    // Note that the maximum value will depend upon whether 
    // the conversion is signed or unsigned.      

    // Packing involves a "max" and a "min" for signed or just a "max" for unsigned. 
    // This byte must be accumulated as an offset before adding the next value.
    // conversion...max unsigned.....max/min signed 
    // ...uint8........0x00FF........0x007F/0x0080
    // ...uint16.......0xFFFF........0x7FFF/0x8000      

    int max;
    switch (byteLen) {
    case 1:
      max = (unsigned ? 0xFF : Byte.MAX_VALUE);
      for (int i = 0, pt = 0, n = b.length, offset = 0; pt < n;) {
        int val = b[pt++];
        if (unsigned)
          val = val & max;
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
      for (int i = 0, pt = 0, n = b.length / 2, offset = 0; pt < n;) {
        int val = BC.bytesToShort(b, (pt++) << 1, false);
        if (unsigned)
          val = val & max;
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

  /**
   * static singleton array for processing runlength temporary array
   */
  private static int[] temp;

  /**
   * Static singleton array for processing runlength temporary array.
   * 
   * @param n
   *        desired size; but not less than 1000
   * @return int[n] at least
   */
  public static int[] getTemp(int n) {
    // supply a temporary array
    if (temp == null || temp.length < n)
      temp = new int[Math.max(n, 1000)];
    return temp;
  }

  /**
   * This could be a VERY large array for some files!
   */
  public static void clearTemp() {
    temp = null;
  }


  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////
  //////////////////////////////// debugging code only ////////////////////////////

  /**
   * Debugging option: Save lots of time by only representing strings as stringData, with
   * associated arrays, not actually creating the strings.
   */
  public static final boolean doBuildStrings = false;


  /**
   * 
   * @j2sIgnore
   * 
   * @param sb 
   * 
   * only for debugging
   * 
   * @return int[], float[], or null (when string)
   */
  public Object debugDecode(SB sb) {
    finalizeDecoding(sb);
    switch (dataType) {
    case INT:
      return getDebugDataIntColumn();
    case FIXED:
      return debugGetDataFloatColumn();
    case STRING:
      return (doBuildStrings ? debugGetDataStringColumn() : stringData);
    }
    return null;
  }

  /**
   * @j2sIgnore
   * 
   * @return int[]
   */
  protected int[] getDebugDataIntColumn() {
    if (dataType != INT)
      return null;
    if (mask != null) {
      for (int i = rowCount; --i >= 0;) {
        if (mask[i] != 0)
          intData[i] = UNKNOWN_INT;
      }
    }
    return intData;
  }

  /**
   * @j2sIgnore
   * 
   * @return double[]
   */
  protected double[] debugGetDataFloatColumn() {
    if ((mode & MODE_FIXED) != MODE_FIXED)
      return null;
    double[] fixedData = new double[rowCount];
    for (int i = rowCount; --i >= 0;)
      fixedData[i] = (mask != null && mask[i] != 0 ? Double.NaN
          : intData[i] / factor);
    return fixedData;
  }

  /**
   * @j2sIgnore
   * 
   * @return String[]
   */
  protected String[] debugGetDataStringColumn() {
    String[] data = new String[rowCount];
    int len = stringData.length();
    for (int i = 0; i < rowCount; i++) {
      switch (mask == null ? 0 : mask[i]) {
      case 0:
        int pt = indices[i];
        data[i] = stringData.substring(offsets[pt++],
            (pt == rowCount ? len : offsets[pt]));
        break;
      case 1:
        data[i] = "\0"; // "."
        break;
      case 2:
        data[i] = "\0"; // "?" 
        break;
      }
    }
    return data;
  }

  /**
   * For debugging it is convenient to have a simple code such as D3I1uB4d
   * that describes the set of encodings used.
   * 
   * 
   * @j2sIgnore
   * 
   * @param encodings
   * @return type string
   */
  @SuppressWarnings("unchecked")
  private String debugGetDecoderType(Object[] encodings) {
    // B F I R D I S  
    //  type Encoding = 
    //      | ByteArray 
    //      | FixedPoint
    //      | IntervalQuantization 
    //      | RunLength 
    //      | Delta 
    //      | IntegerPacking 
    //      | StringArray

    SB sb = new SB();
    int n = encodings.length;
    for (int i = 0; i < n; i++) {
      Map<String, Object> encoding = (Map<String, Object>) encodings[i];
      char kind = getMapChar(encoding, "kind");
      if (encoding.containsKey("min"))
        kind = 'Q'; //????
      sb.appendC(kind);
      switch (kind) {
      case 'B':
        sb.appendI(btype);
        continue;
      case 'F':
      case 'R':
      case 'D':
        break;
      case 'S':
        sb.appendC('.');
        sb.append(dataDecoder.toString());
        sb.appendC('.');
        sb.append(offsetDecoder.toString());
        sb.appendC('.');
        continue;
      case 'I':
        sb.appendI(geMapInt(encoding.get("byteCount")));
        if (unsigned)
          sb.appendC('u');
        continue;
      default:
        sb.appendC('?');
        continue;
      }
      sb.appendI(srcType);
    }
    return sb.toString();
  }

  static private String debugToStr(Object o) {
    if (o instanceof int[])
      return debugToStrI((int[]) o);
    if (o instanceof byte[])
      return debugToStrB((byte[]) o);
    if (o instanceof double[])
      return debugToStrD((double[]) o);
    if (o instanceof String) {
      String s = (String) o;
      return (s.length() < 100 ? s : s.substring(0, 100) + "..." + s.length());
    }
    return debugToStrO((Object[]) o);
  }

  static private String debugToStrO(Object[] o) {
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

  static private String debugToStrI(int[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendI(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...").appendI(o.length);
    sb.appendC(']');
    return sb.toString();
  }

  static private String debugToStrB(byte[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendI(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...").appendI(o.length);
    sb.appendC(']');
    return sb.toString();
  }

  static private String debugToStrD(double[] o) {
    SB sb = new SB();
    char sep = '[';
    int n = Math.min(o.length, 20);
    for (int i = 0; i < n; i++) {
      sb.appendC(sep).appendD(o[i]);
      sep = ',';
    }
    if (n < o.length)
      sb.append("...").appendI(o.length);
    sb.appendC(']');
    return sb.toString();

  }  
  
}


// some notes I kept during deveopment. 

// B F I R D I S  
//  type Encoding = 
//      | ByteArray 
//      | FixedPoint
//      | IntervalQuantization 
//      | RunLength 
//      | Delta 
//      | IntegerPacking 
//      | StringArray

//EBI employs only (from 8glv) 1, 2, 3, 4, 5, and 33:

//ByteArray {
//  kind = "ByteArray"
//  type   1      2       3       4       5                  32       33 
//  type: Int8 | Int16 | Int32 | Uint8 | Uint16 | Uint32 | Float32 | Float64
//}
// bitcount 1      2       4       1       2  == 1 << (((type -1 )% 3));


// in 1crn.bcif: 
//:categories[39]:columns[1]:name=fract_transf_matrix[1][1]
//:categories[39]:columns[1]:data:encoding[0]:type=33
//:categories[39]:columns[1]:data:encoding[0]:kind=ByteArray
//:categories[39]:columns[1]:data:data is 8 [-123, 62, 88, -58, -122, 110, -106, 63]

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

