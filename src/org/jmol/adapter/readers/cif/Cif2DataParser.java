package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.CifDataParser;
import javajs.util.PT;

/**
 * 
 * Fully implemented 2016.12.1
 * 
 * see http://journals.iucr.org/j/issues/2016/01/00/aj5269/index.html
 * 
 * Will deliver JSON versions of the data while file reading and Java List/Map structures when 
 * called by 
 * 
 *    x = getProperty("cifInfo", filename)
 *    
 * Validated using the test-data suite by John Bollinger (John.Bollinger@stjude.org)
 * found at https://github.com/COMCIFS/cif_api  
 * 
 * @author Bob Hanson hansonr@stolaf.edu 
 */
public class Cif2DataParser extends CifDataParser {

  
  @Override
  protected int getVersion() {
    return 2;
  }


//  3.1. Character set and encoding
//
//       - UTF-8
//       CIF 2.0 files are permitted to begin with a Unicode byte-order mark (character U+FEFF)
//
//       - 0xEF,0xBB,0xBF
//
// - Implemented
//  - See org.jmol.adapter.smarter.Resolver; BOM is taken care of in the reader  
//
//
//  3.2. Whitespace and line termination
//  
//  Specifically, CIF 2.0 recognizes and attributes identical meaning as line termination to 
//  three distinct character sequences: \n, \r, or \r\n. CIF 2.0 processors are required to 
//  behave as if each appearance of any of the three equivalent forms of CIF 2.0 line termination 
//  in their inputs had been converted to a line feed prior to analysis.
//  
//  CIF keywords, data block headers, save frame headers, data names and data values all 
//  must be separated from each other by whitespace (an in-line whitespace character or a 
//  line terminator, followed by an arbitrary number of CIF comments, additional in-line 
//  whitespace characters and line terminators). The line terminator immediately prior to
//  a text-field opening delimiter serves both to separate the preceding data name or data
//  value from the text field and to indicate the start of the text field; additional whitespace 
//  prior to that line terminator is not required. Otherwise, whitespace is optional in CIF 2.0 in these positions:
//
//    (a) between the enclosing square brackets ([,]) of a List value (see §3.8[link]) and the 
//           values within, and between the brackets of an empty List;
//
//    (b) between the enclosing braces ({,}) of a Table value (see §3.9[link]) and the entries 
//           within, and between the braces of an empty Table; and
//
//    (c) between a Table key and its associated value.  
//
// - Implemented
//  - GenericLineReader will take care of this automatically.
//
//
//  3.3 version #\#CIF_2.0
//
// - Implemented
//  - Jmol looks for #\#CIF_2 only, figuring that perhaps this will be augmented  
//  - note that this can be preceded by the UTF-8 byte-order-mark, but that will not be seen by the reader anyway.
//
//
//  3.4. Data names, block codes and frame codes  
//
//  CIF 2.0 defines data name, block code and frame code uniqueness in terms 
//  of the Unicode canonical caseless matching algorithm (The Unicode Consortium, 
//  2014b[The Unicode Consortium (2014b). The Unicode Standard, Version 7.0.0, 
//  ch. 3, §3.13. Mountain View: The Unicode Consortium. http://www.unicode.org/versions/Unicode7.0.0/ .]): 
//  no two data blocks in any CIF may have names that are canonical caseless matches 
//  of each other, no two save frames in any data block may have names that are canonical caseless 
//  matches of each other, and no two data names belonging directly to the same block or 
//  frame may be canonical caseless matches of each other.
//
// - Implemented
//  - A Jmol limitation: No critical keys read by Jmol should be unicode characters. 
//  - JSmol in particular does not implement the very complex code necessary to accomplish Unicode case checking.
//    
//  3.5. Quoted and whitespace-delimited strings
//
//  CIF 2.0 ... does not permit quoted data values to embed their delimiter under any circumstance.
//  CIF 2.0 excludes these four characters (',",[, and ]) from appearing anywhere in whitespace-delimited data values.
//
// - Implemented
//
//
//  3.6. Triple-quoted strings
//
//  CIF 2.0 provides a new way to express single- and multi-line data values: triple-quoted strings. 
//  A triple-quoted string begins with a delimiter consisting of three apostrophes (''') or three 
//  quotation marks ("""), and ends with the next subsequent appearance of its opening delimiter.
//
// - Implemented
//
//
//  3.7. Text fields
//
// [CIF 2.0 adopts] a text prefixing protocol (see §5.2[link]) and [incorporates] a version of the CIF 1.1 line-folding protocol
// for text fields into the CIF 2.0 specification proper.
//
// [and 5.2]
//  A `prefix' consists of a sequence of one or more characters that are permitted in a text field, 
//  except for backslash (\) or a CIF line terminator, and it does not begin with a semicolon. The text prefix protocol 
//  applies to text fields whose physical content begins with a prefix, followed by either one or two backslashes, any 
//  number of in-line whitespace characters (including none), and a line terminator or the end of the field, and 
//  whose subsequent lines each begin with the same prefix. The line containing the terminating semicolon is not 
//  accounted part of the content for this purpose. Such a text field is called a `prefixed text field', 
//  and the logical (`un-prefixed') content of such a field is derived from its physical content by the following procedure:
//
// (1) Remove the prefix from each line, including the first.
// (2) If the remaining part of the first line starts with two backslashes then remove the first of them; 
//     otherwise remove the whole first line.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
//
// - Implemented
//
//
//  3.8. List data type
//
//  The new `List' data type provided by CIF 2.0 represents, as a single (compound) value, an ordered sequence of 
//  values of any type or types. Syntactically, a List value takes the form of a whitespace-separated sequence of 
//  data values enclosed in square brackets.
//
// - Implemented
//  - Delivers proper JSON strings for lists. 
//  - Numerical values are converted to integer or float type.
//  - Trailing uncertainties (nn) removed.
//
//
//  3.9. Table data type
//
//  The new `Table' data type provided by CIF 2.0 represents, as a single (compound) value, an unordered collection of 
//  entries representing associations between string keys and data values of any type. This sort of data structure is 
//  also known variously as a `map', `dictionary' or `associative array', among other names. Syntactically, a Table value 
//  takes the form of a whitespace-separated sequence of key–value pairs, enclosed in braces. The values may be any CIF data 
//  value. The keys take the form of quoted or triple-quoted strings as described above, with a colon appended immediately 
//  after the closing delimiter. Keys may be separated from their values by an arbitrary amount of whitespace, including none. 
//
// - Implemented
//  - Delivers proper JSON strings after converting keys and data values
//
// 


  /**
   * There is no need to convert to unicode in CIF 2.0.
   * 
   */
  @Override
  public String toUnicode(String data) {
    return data;
  }

  /**
   * Includes all possible operator tokens
   */
  @Override
  protected boolean isQuote(char ch) {
    switch (ch) {
    case '\1': // preprocessed CIF 1.1 encoded multi-line semicolon-encoded string
    case '\'':
    case '\"':
    case '[':
    case ']':
    case '{':
    case '}':
    case ';':
      return  true;
    }
    return false;
  }

  /**
   * final get for quoted object
   */
  @Override
  protected Object getQuotedStringOrObject(char ch) {
    return processQuotedString();
  }

  /**
   * initial processing; returns a string bounded by \1
   * @throws Exception 
   */
  @Override
  protected String preprocessString() throws Exception {
    line = (ich == 0 ? this.str : this.str.substring(ich));
    return setString(processSemiString());
  }

  /**
   * Handle all forms of quotation, 
   * including '...', "...", '''...''', """...""", and ;...\n...\n; 
   * @return a string or data structure, depending upon setting asObject
   */
  private Object processQuotedString() {
    String str = null;
    char quoteChar = this.str.charAt(ich);
    String tripleChar = null;
    try {
      switch (quoteChar) {
      case '\1':
        // produced from ;....; in a preprocessing stage
        str = this.str.substring(1, (ich = this.str.indexOf("\1", ich + 1)));
        ich++;
        break;
      case '[':
        return readList();
      case ']':
        ich++;
        return  "]";
      case '{':
        return readTable();
      case '}':
        ich++;
        return "}";
      case '\'':
      case '"':
        if (this.str.indexOf("'''") == ich)
          tripleChar = "'''";
        else if (this.str.indexOf("\"\"\"") == ich)
          tripleChar = "\"\"\"";
        int nchar = (tripleChar == null ? 1 : 3);
        int pt = ich + nchar;
        int pt1 = 0;
        // read enough lines from the stream to finish this quote.
        while ((pt1 = (tripleChar == null ? this.str.indexOf(quoteChar, pt) : 
              this.str.indexOf(tripleChar, pt))) < 0) {
          if (readLine() == null)
            break;
          this.str += line;
        }
        ich = pt1 + nchar;
        cch  = this.str.length();
        str = this.str.substring(pt, pt1);
        break;
      }
    } catch (Exception e) {
      System.out.println("exception in Cif2DataParser ; " + e);
    }
    // the global flag cterm can be  ']' or '}', indicating that 
    // we are working within a list or table, where we should not
    // escape the string ever.
    return (cterm == '\0' || asObject ? str : PT.esc(str));
  }

  /**
   * Sets a multiline semicolon-eclipsed string to be parsed from the beginning,
   * allowing for CIF 2.0-type prefixed text lines and removing line folding.
   * 
   * ;xxxx\comments here
   * 
   * xxxx
   * 
   * xxxx
   * 
   * xxxx
   * 
   * ;
   * 
   * @return \1...quote....\1
   * @throws Exception
   */
  protected String processSemiString() throws Exception {
    int pt1, pt2;
    String str = preprocessSemiString();
    // note that this string is already escaped with \1...\1 
    if (str.indexOf(';') != 1 // "...and it does not begin with a semicolon" 
        && (pt1 = str.indexOf('\\')) > 1 // ...and it is followed by a backslash 
        && ((pt2 = str.indexOf('\n')) > pt1 // ...and theree is no EOL prior to that 
             || pt2 < 0)  
        ) {
      String prefix = str.substring(1, pt1);
      // (1) Remove the prefix from each line, including the first.
      // (2) If the remaining part of the first line starts with two backslashes then remove the first of them; 
      //     otherwise remove the whole first line. 
      str = PT.rep(str,  "\n" + prefix, "\n");
      // remove just the next \ if it is there, or the first full line
      str = "\1" + str.substring(str.charAt(pt1 + 1) == '\\' ? pt1 + 1 : pt2 < 0 ? str.length() - 1 : pt2 + 1);
      // Note: Jmol's CIF 1.0 CifReader does not already recognize "\\\n" as a line folding indicator. 
    }
    ich = 0;
    return fixLineFolding(str);
  }

  /**
   * Read a CIF 2.0 table into either a JSON string 
   * or a java data structure
   * 
   * @return an Object or String, depending upon settings
   * @throws Exception
   */
  public Object readTable() throws Exception {
    ich++;
    // save the current globals cterm and nullString, 
    // and restore them afterward. 
    // nullString is what is returned for '.' and '?'; 
    // for the Jmol CifReader only, this needs to be "\0"
    char cterm0 = cterm;
    cterm = '}';
    String ns = nullString; 
    nullString = null;
    Map<String, Object> map = (asObject ? new Hashtable<String, Object>()
        : null);
    int n = 0;
    String str = "";
    while (true) {
      // Iteratively pick up all the objects until the closing bracket
      // This is akin to a map "deep copy"
      String key = getNextToken();
      if (key == null || key.equals("}"))
        break;
      while (isSpaceOrColon(ich))
        ich++;
      if (asObject) {
        map.put(key, getNextTokenObject());
      } else {
        if (n++ > 0)
          str += ",";
        str += key + " : " + getNextToken();
      }
    }
    cterm = cterm0;
    nullString = ns;
    return (asObject ? map : "{" + str + "}");
  }
    

  /** used by readTable
   * 
   * @param ich buffer pointer
   * @return true if whitespace or colon 
   */
  private boolean isSpaceOrColon(int ich) {
    if (ich < cch)
      switch (line.charAt(ich)) {
      case ' ':
      case '\t':
      case '\n':
      case ':':
        return true;
      }
    return false;
  }

  /**
   * Handle unquoted value as Integer or Float if we can.
   * 
   */
  @Override
  protected Object unquoted(String s) {
    if (cterm == '\0' && !asObject)
      return s;
    int n = s.length();
    if (n > 0) {
      // look for a leading character in the set {0123456789.-}
      char c = s.charAt(0);
      if (PT.isDigit(c) || c == '-' || c == '.' && n > 1) {
        // trim off (10)  in 1.345(10)
        // other implementers may want to handle this some other way
        int pt = s.indexOf('(');
        // guess type
        boolean isFloat = (s.indexOf(".") >= 0);
        if (n > 1 && pt > 0 && s.indexOf(')', pt + 1) == n - 1)
          s = s.substring(0, pt);
        try {
          if (isFloat) {
            float f = Float.parseFloat(s);
            if (asObject)
              return Float.valueOf(f);
            s = "" + f;
            if (s.indexOf(".") < 0 && s.indexOf("E") < 0)
              s += ".0";
            return s;
          }
          int i = Integer.parseInt(s);
          return (asObject ? Integer.valueOf(i) : "" + i);
        } catch (Throwable e) {
          // ignore
        }
      }
    }
    return (asObject ? s : PT.esc(s));
  }


  /**
   * allow white space between \ and \n
   * 
   * @param str already enclosed in \1.....\1
   * @return fixed line
   */
  private String fixLineFolding(String str) {
    if (str.indexOf('\\') < 0)
        return str;
    int n = str.length();
    if (str.endsWith("\\\1"))
      str = str.substring(0, n - 1) + "\n\1";
    int pt = 0;
    while ((pt = str.indexOf('\\', pt + 1)) >= 0) {
      int eol = str.indexOf('\n', pt);
      if (eol < 0)
        break;
      for (int i = eol; --i > pt;) {
        char ch = str.charAt(i);
        if (!PT.isWhitespace(ch)) {
          if (ch == '\\') {
            pt = i;
            break;
          }
          pt = eol;
          break;
        }
      }
      if (pt < eol)
        str = str.substring(0, pt) + str.substring(eol + 1);
    }
    return str;
  }

  /**
   * turn "[1,2,3]" into [1.0,2.0,3.0]
   * 
   * array will be truncated to n elements, or filled with zeros to pad to n, as necessary.
   * 
   * @param s
   * @param n
   * @return double[] array
   */
  public static double[] getArrayFromStringList(String s, int n) {
    float[] f = new float[n];
    PT.parseFloatArrayInfested(PT.getTokens(s.replace(',', ' ').replace('[', ' ')), f);
    double[] d = new double[n];
    for (int i = 0; i < n; i++)
      d[i] = f[i];
    return d;
  }

  /**
   * turn "[1,2,3]" into [1,2,3]
   * 
   * array will be truncated to n elements, or filled with zeros to pad to n, as necessary.
   * 
   * @param s
   * @param n
   * @return int[] array
   */
  public static int[] getIntArrayFromStringList(String s, int n) {
    float[] f = new float[n];
    PT.parseFloatArrayInfested(PT.getTokens(s.replace(',', ' ').replace('[', ' ')), f);
    int[] a = new int[n];
    for (int i = 0; i < n; i++)
      a[i] = (int) f[i];
    return a;
  }



}
