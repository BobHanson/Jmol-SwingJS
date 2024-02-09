package org.jmol.adapter.readers.cif;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.BinaryDocument;
import javajs.util.CifDataParser;
import javajs.util.Lst;
import javajs.util.MessagePackReader;
import javajs.util.SB;

/**
 * The BCIFParser class, not so much a "parser" really, but an interface between the
 * reader and the decoders.
 * 
 * While the supercall CifDataParser's job is to iterate over CIF file tokens
 * (keys and values), our job here is to make the link between a specific CIF
 * data item and its associated decoder.
 * 
 * Whereas CIF values are always String type, each BCIFDecoder is of one of
 * three types: int, "fixed point" (double in JavaScript and in Jmol-SwingJS and
 * JmolD.jar; float in legacy Jmol.jar). There is no actual parsing, just a
 * process of conversion from byte[] to number or String to String.
 * 
 * All we are doing here is getting calls to set up a new category, managing the
 * retrieval of that category's "column" data as requested by CIFReader.
 * 
 * Whereas CIFReader's tokenizer only delivers String values, here we have the
 * actual data type already taken care of by the decoder. So whereas in
 * CIFReader we just have (String) key and (String) field, here we have (String)
 * key (the column name) and ifield, dfield, and fieldStr. CIFReader's String
 * field variable is fieldStr here, but also fieldStr maintains the "no data"
 * status of '.' and '?' found in CIF files a and handled here as "mask" bits.
 * 
 * 
 */
class BCIFDataParser extends CifDataParser {

  private BCIFReader rdr;

  //// category-level fields
  
  private String categoryName;
  private int rowCount;
  private Object[] columnMaps;
  private BCIFDecoder[] columnDecoders;

  //// "row"-level fields
  
  private int rowPt;

  //// "item"-level fields

  int ifield;
  double dfield;
  String fieldStr;
  boolean fieldIsValid;
  
  public BCIFDataParser(BCIFReader bcifReader, boolean debugging) {
    this.rdr = bcifReader;
    this.debugging = debugging;
  }

  /**
   * 
   * 
   * @param key
   * @param col
   * @param rowCount
   * @param catName
   * @return possibly null, if the mask is all '?' and/or '.'
   */
  private BCIFDecoder getDecoder(String key, Map<String, Object> col,
                                 int rowCount, String catName) {
    SB sb = null;//debugging: new SB();
    BCIFDecoder d = new BCIFDecoder(key, col, sb).setRowCount(rowCount, catName)
        .finalizeDecoding(null);
    return (d == null || d.dataType == BCIFDecoder.IGNORE ? null : d);
  }

  /**
   * Called from BCIFReader.processCategory(), this method saves the column Map
   * list, category name, rowCount, and column names.
   * 
   * There are no decoders yet, though.
   * @param catName
   * @param rowCount
   * @param columns
   */
  @SuppressWarnings("unchecked")
  public void initializeCategory(String catName, int rowCount, Object[] columns) {
    this.columnMaps = columns;
    this.rowCount = rowCount;
    this.categoryName = catName;
    int n = this.columnCount = columns.length;
    if (columnNames == null || columnNames.length < n)
      columnNames = new String[n];
    for (int i = 0; i < n; i++) {
      columnNames[i] = (catName + "_"
          + ((Map<String, Object>) columns[i]).get("name")).toLowerCase();
    }
  }

  /**
   * The primary interface method, called by CIFReader when it thinks it has
   * entered a loop_ structure.
   * 
   * We aren't really "parsing." We are just creating decoders and finalizing
   * them, category by category. That is, we are creating all their int[ ]
   * arrays, including their mask, int[ ] data, string data, and string pointer
   * indexes and offsets. DELTA and RUNLEN decoding has been done, but the
   * values have not been turning into fixed-point values, and no substrings
   * have been created.
   * 
   * For each category, create the decoders that are needed, as listed in the
   * fieldNames array. Finalize the decoders by unpacking their integer arrays
   * and carrying out direct byte[] -> double[] conversions.
   * 
   * 
   * @param fieldNames
   *        static list of fields of interest, such as "_atom_site_label_id"
   * @param key
   *        ignored
   * @param data
   *        ignored
   * @param key2col
   *        !param col2key
   */
  @Override
  public void parseDataBlockParameters(String[] fieldNames, String key,
                                       String data, int[] key2col,
                                       int[] col2key)
      throws Exception {
    haveData = false;
    rowPt = -1;
    for (int i = CifDataParser.KEY_MAX; --i >= 0;) {
      col2key[i] = key2col[i] = NONE;
    }
    if (!htFields.containsKey(fieldNames[0])) {
      for (int i = fieldNames.length; --i >= 0;)
        htFields.put(fieldNames[i], Integer.valueOf(i));
    }
    //System.out.println(fieldNames[0] + " " + columnCount);
    columnDecoders = new BCIFDecoder[columnCount];
    for (int pt = 0; pt < columnCount; pt++) {
      String s = columnNames[pt];
      // some columns do not correspond to fields we are interested in. 
      // so they end up null here.
      Integer iField = htFields.get(s);
      int keyIndex = col2key[pt] = (iField == null ? NONE : iField.intValue());
      BCIFDecoder d = (keyIndex == NONE ? null
          : getDecoder(s, getDataColumn(pt), rowCount, categoryName));
      if (d == null) {
        // either not a field of interest, or its mask is all '?' or '.'
        // System.out.println("BCIFDataParser skipping " + s);
        if (keyIndex >= 0)
          key2col[keyIndex] = EMPTY; 
      } else {
        columnDecoders[pt] = d;
        key2col[keyIndex] = pt;
        //System.out.println(s + " col " + pt + " keyIndex " + keyIndex );
        haveData = true;
      }
    }
  }

  /**
   * secifically for "single-row" items to retrieve a single value and dispose
   * of the decoder immediately. no "row" issue because there is only one row --
   * or these are one-off CIF key/value entries.
   * 
   * @param icol
   */
  void decodeAndGetData(int icol) {
    columnDecoders = new BCIFDecoder[] {
        getDecoder(null, getDataColumn(icol), rowCount, categoryName) };
    getColumnData(0);
    columnDecoders = null;
  }

  /**
   * "next line" or "next atom" function -- just advance the row pointer and let
   * us know when we are done, in which case we can disose of all the column
   * decoders.
   * 
   */
  @Override
  public boolean getData() throws Exception {
    rowPt++;
    boolean done = rowPt >= rowCount;
    if (done) {
      for (int i = columnDecoders.length; --i >= 0;)
        columnDecoders[i] = null;
    }
    return !done;
  }

  /**
   * A column has been requested. These were set up in parseDataBlockParameters,
   * but some of them are null, because they were not part of the collection
   * Jmol needs or all the values were '?' or '.'.
   * 
   * Set up the fields ifield, dfield, and fieldStr. Register if a field is
   * valid.
   * 
   * The decoder methods called here actually do the final RUN-LENGTH, DELTA, or
   * Script data/offset decoding.
   * 
   */
  @Override
  public Object getColumnData(int colPt) {
    rdr.key = getColumnName(colPt);
    //System.out.println(rdr.key);
    ifield = BCIFDecoder.UNKNOWN_INT;
    dfield = Double.NaN;
    if (columnDecoders[colPt] == null) {
      fieldIsValid = false;
      return fieldStr = nullString;
    }
    switch (columnDecoders[colPt].dataType) {
    case BCIFDecoder.INT:
      ifield = columnDecoders[colPt].getIntValue(rowPt);
      fieldStr = (ifield == BCIFDecoder.UNKNOWN_INT ? nullString : "" + ifield);
      break;
    case BCIFDecoder.FIXED:
      dfield = columnDecoders[colPt].getFixedPtValue(rowPt);
      fieldStr = (Double.isNaN(dfield) ? nullString : "_" + dfield);
      break;
    case BCIFDecoder.STRING:
      fieldStr = columnDecoders[colPt].getStringValue(rowPt);
      break;
    }
    fieldIsValid = (fieldStr != nullString);
    return fieldStr;
  }

  /**
   * Report whether this field is of the CIF type '.' or '?' (without
   * distinguishing between those).
   * 
   * @return true if valid
   */
  protected boolean isFieldValid() {
    return fieldIsValid;
  }

  /**
   * Just a cleaner way of handling the Object data from MessagePackReader.
   * 
   * @param icol
   * @return the Map for this column or null if there is no such column
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getDataColumn(int icol) {
    return (icol >= 0 && icol < columnCount ? (Map<String, Object>) columnMaps[icol]
        : null);
  }

  //// THAT'S IT!! The rest of this file is just for fleshing 
  //// out the GenericCifDataParser interface and debugging
  
  @Override
  public Map<String, Object> getAllCifData() {
    // TODO??
    // not implemented
    return null;
  }


  ///////////////// debugging code only ///////////////
  ///////////////// debugging code only ///////////////
  ///////////////// debugging code only ///////////////
  ///////////////// debugging code only ///////////////
  ///////////////// debugging code only ///////////////
  ///////////////// debugging code only ///////////////

  private Hashtable<String, BCIFDecoder> cifMap;

  public String header;

  /**
   * @j2sIgnore
   * 
   *            Debugging only
   * 
   * @param msgMap
   */
  @SuppressWarnings("unchecked")
  void debugConstructCifMap(Map<String, Object> msgMap) {

    SB sb = new SB();
    try {
      cifMap = new Hashtable<String, BCIFDecoder>();
      String header = (String) msgMap.get("header");
      System.out.println("BCIFDataParser header is " + header);
      Map<String, Object> dataBlock = (Map<String, Object>) ((Object[]) msgMap
          .get("dataBlocks"))[0];

      dumpMap(sb, dataBlock, "");
      Object[] categories = (Object[]) dataBlock.get("categories");
      for (int j = 0; j < categories.length; j++) {
        Map<String, Object> cat = (Map<String, Object>) categories[j];
        if (cat.isEmpty())
          continue;
        int rowCount = BCIFDecoder.geMapInt(cat.get("rowCount"));
        String catName = ((String) cat.get("name")).toLowerCase();
        Object[] columns = (Object[]) cat.get("columns");
        Lst<String> lst = new Lst<String>();
        int ncol = (columns == null ? 0 : columns.length);
        for (int k = ncol; --k >= 0;) {
          Map<String, Object> col = (Map<String, Object>) columns[k];
          String key = catName + "_" + ((String) col.get("name")).toLowerCase();
          lst.addLast(key);
          BCIFDecoder d = new BCIFDecoder(key, col, sb).setRowCount(rowCount,
              catName);
          //Object o = 
          d.debugDecode(sb);
          //System.out.println(toStr(o));
          cifMap.put(key, d);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    // [ We now have a completely parsed structure of the contents of the file. ]
    try {
      FileOutputStream fos = new FileOutputStream("c:/temp/t.log");
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      // TODO
    }

    //      double[] cartx = (double[]) getVal("_atom_site_cartn_y");
    //
    //      Object o;
    //      o = getVal("_pdbx_struct_assembly_auth_evidence_assembly_id");

    //System.out.println((String) debugGetVal("_entry_id"));

  }

  /**
   * @j2sIgnore
   * 
   * @param sb
   * @param value
   * @param key
   */
  @SuppressWarnings("unchecked")
  static void dumpMap(SB sb, Map<String, Object> value, String key) {
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

  /*
   * @j2sIgnore
   * 
   */
  public static void main(String[] args) {
    try {
      String testFile = (args.length == 0 ?
      //         "c:/temp/8glv.bcif" 
          "c:/temp/1cbs.bcif"
          //"c:/temp/1crn.bcif" 
          : args[0]);
      BinaryDocument binaryDoc = new BinaryDocument();
      BufferedInputStream bis = new BufferedInputStream(
          new FileInputStream(testFile));
      binaryDoc.setStream(bis, true); // big-endian
      Map<String, Object> msgMap;
      msgMap = (new MessagePackReader(binaryDoc, false)).readMap(); //Selectively(cb);
      BCIFDataParser parser = new BCIFDataParser(null, true);
      parser.debugConstructCifMap(msgMap);
      binaryDoc.close();
      System.out.println("OK - DONE");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
