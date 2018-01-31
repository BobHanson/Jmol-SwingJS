package org.jmol.util;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.script.SV;

import javajs.util.OC;
import javajs.util.PT;
import javajs.util.SB;

public class JSONWriter {
  
  protected OC oc;
  protected int indent = 0;
  /**
   * allows writing of one or more keys different from the original
   */
  private Map<String, String> modifiedKeys;
  
  public void setModifyKeys(Map<String, String> mapNewToOld) {
    modifiedKeys = mapNewToOld; 
  }

  private boolean writeNullAsString = false;
  
  /**
   * Set option to write a null as the string "null" or just null itself.
   * 
   * @param b
   */
  public void setWriteNullAsString(boolean b) {
    writeNullAsString = b;
  }


  private final static String SPACES = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
  

  protected OC append(String s) {
    if (s != null)
      oc.append(SPACES.substring(0, Math.min(indent, SPACES.length()))).append(s);
    return oc;
  }
  

  @SuppressWarnings("resource")
  public void setStream(OutputStream os) {
    oc = new OC().setParams(null,  null, true, os);
  }
 
  public boolean closeStream() {
    oc.closeChannel();
    return true;
  }
  
  @SuppressWarnings("unchecked")
  public void writeObject(Object o) {
    if (o == null) {
      writeNull();
    } else if (o instanceof Map<?, ?>)  {
      writeMap((Map<String,Object>) o);
    } else if (o instanceof List<?>) {
      writeList((List<Object>) o);
    } else if (o instanceof String) {
      writeString((String) o);
    } else if (o instanceof Boolean) {
      writeBoolean((Boolean) o);
    } else if (o instanceof Number) {
      writeNumber((Number) o);
    } else if (o.getClass().isArray()) {
      writeArray(o);
    } else if (o instanceof SV) {
      append(((SV) o).toJSON());
    } else {
      writeString(o.toString());
    }    
  }

  public void writeNull() {
    oc.append(writeNullAsString ? "\"null\"" : "null");
  }

  public void writeNumber(Number o) {
    String s = o.toString();
    if (s.equals("NaN")) {
      writeString(s);
    } else {
      oc.append(s);
    }
  }

  public void writeBoolean(Boolean o) {
    oc.append(o.toString());
  }

  public void writeString(String str) {
    oc.append(PT.esc(str));
  }

  public void writeString(String str, SB sbSym) {
   sbSym.append(PT.esc(str));
  }

  public void writeMap(Map<String, Object> map) {
    if (map.isEmpty()) {
      append("{}");
      return;
    }
    mapOpen();
    {
      String sep = "";
      for (Entry<String, Object> entry : map.entrySet()) {
        String key = entry.getKey();
        Object value = getAndCheckValue(map, key);
        if (value == null)
            continue;
        oc.append(sep);
        mapAddKeyValue(key, value, null);
        sep = ",\n";
      }
    }
    mapClose();
  }
  
  protected Object getAndCheckValue(Map<String, Object> map, String key) {
    return map.get(key);
  }


  public void mapOpen() {
    oc.append("{\n");
    indent++;
  }
  
  public void mapClose() {
    indent--;
    oc.append("\n");
    append("}");
  }

  public void mapAddKey(String key) {
    append("");
    if (modifiedKeys != null && modifiedKeys.containsKey(key))
      key = modifiedKeys.get(key);
    writeString(key);
    oc.append(":");
  }

  public void mapAddKeyValue(String key, Object value, String terminator) {
    mapAddKey(key);
    writeObject(value);
    if (terminator != null)
      oc.append(terminator);
  }

  /**
   * Add a key:value pair where value is already quoted
   * @param key
   * @param value
   * @param terminator TODO
   */
  public void mapAddKeyValueRaw(String key, Object value, String terminator) {
    mapAddKey(key);
    oc.append(value.toString());
    if (terminator != null)
      oc.append(terminator);
  }

  public void mapAddMapAllExcept(String key, Map<String, Object> map,
                                 String except) {
    mapAddKey(key);
    mapOpen();
    {
      String sep = "";
      for (Entry<String, Object> entry : map.entrySet()) {
        String key1 = entry.getKey();
        if (PT.isOneOf(key1, except))
          continue;
        oc.append(sep);
        mapAddKeyValue(key1, entry.getValue(), null);
        sep = ",\n";
      }
    }
    mapClose();
  }

  public void writeList(List<Object> list) {
    int n = list.size();
    arrayOpen(false);
    for (int i = 0; i < n; i++) {
      if (i > 0)
        oc.append(",");
      arrayAdd(list.get(i));
    }
    arrayClose(false);
  }

  public void writeArray(Object o) {
    arrayOpen(false);
    int n = Array.getLength(o);
    for (int i = 0; i < n; i++) {
      if (i > 0)
        oc.append(",");
      arrayAdd(Array.get(o, i));
    }
    arrayClose(false);      
  }

  public void arrayOpen(boolean andIndent) {
    oc.append("[");
    if (andIndent)
      indent++;
  }
  
  public void arrayAdd(Object o) {
    writeObject(o instanceof Float && ((Float)o).isNaN() || o instanceof Double && ((Double)o).isNaN() ? "NaN" : o);
  }
  
  public void arrayClose(boolean andIndent) {
    if (andIndent) {
      indent--;
      append("");
    }
    oc.append("]");
  }

}
