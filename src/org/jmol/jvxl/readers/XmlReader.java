/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.jvxl.readers;

import java.io.BufferedReader;

import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Escape;


public class XmlReader {
  
  // a relatively simple XML reader requiring "nice" line and tag format.
  

  BufferedReader br;
  String line;

  public String getLine() {
    return line;
  }

  public XmlReader(BufferedReader br) {
    this.br = br;
  }

  public String toTag(String name) throws Exception {
    skipTo("<" + name);
    if (line == null)
      return "";
    int i = line.indexOf("<" + name) + name.length() + 1;
    if (i == line.length())
      return line;
    if (line.charAt(i) == ' ' || line.charAt(i) == '>')
      return line;
    line = null;
    return toTag(name);
  }
  
  public void skipTag(String name) throws Exception {
    skipTo("</" + name + ">");
  }

  /**
   * 
   * @param name
   * @param data
   * @param withTag
   * @param allowSelfCloseOption TODO
   * @return trimmed contents or tag + contents, never closing tag
   * @throws Exception
   */
  public String getXmlData(String name, String data, boolean withTag, boolean allowSelfCloseOption)
      throws Exception {
    return getXmlDataLF(name, data, withTag, allowSelfCloseOption, false);
  }
  public String getXmlDataLF(String name, String data, boolean withTag, boolean allowSelfCloseOption, boolean addLF)
      throws Exception {
    // crude
    String closer = "</" + name + ">";
    String tag = "<" + name;
    if (data == null) {
      SB sb = new SB();
      try {
        if (line == null)
          line = br.readLine();
        while (line.indexOf(tag) < 0) {
          line = br.readLine();
        }
      } catch (Exception e) {
        return null;
      }
      sb.append(line);
      if (addLF)
        sb.append("\n");
      boolean selfClosed = false;
      int pt = line.indexOf("/>");
      int pt1 = line.indexOf(">");
      if (pt1 < 0 || pt == pt1 - 1)
        selfClosed = allowSelfCloseOption;
      while (line.indexOf(closer) < 0 && (!selfClosed  || line.indexOf("/>") < 0)) {
        sb.append(line = br.readLine());
        if (addLF)
          sb.append("\n");
      }
      data = sb.toString();
    }
    return extractTag(data, tag, closer, withTag);
  }

  public static String extractTagOnly(String data, String tag) {
    return extractTag(data, "<" + tag + ">", "</" + tag  + ">", false);
  }
  
  private static String extractTag(String data, String tag, String closer, boolean withTag) {
    int pt1 = data.indexOf(tag);
    if (pt1 < 0)
      return "";
    int pt2 = data.indexOf(closer, pt1);
    if (pt2 < 0) {
      pt2 = data.indexOf("/>", pt1);
      closer = "/>";
    }
    if (pt2 < 0)
      return "";
    if (withTag) {
      pt2 += closer.length();
      return data.substring(pt1, pt2);
    }
    boolean quoted = false;
    for (; pt1 < pt2; pt1++) {
      char ch;
      if ((ch = data.charAt(pt1)) == '"')
        quoted = !quoted;
      else if (quoted && ch == '\\')
        pt1++;
      else if (!quoted && (ch == '>' || ch == '/'))
        break;
    }
    if (pt1 >= pt2)
      return "";
    while (PT.isWhitespace(data.charAt(++pt1))) {
    }
    return unwrapCdata(data.substring(pt1, pt2));
  }

  /**
   * @param s
   * @return   unwrapped text
   */
  public static String unwrapCdata(String s) {
    return (s.startsWith("<![CDATA[") && s.endsWith("]]>") ?
        PT.rep(s.substring(9, s.length()-3),"]]]]><![CDATA[>", "]]>") : s);
  }
  
  public static String getXmlAttrib(String data, String what) {
    // TODO
    // presumes what = "xxxx"
    // no check for spurious "what"; skips check for "=" entirely
    // uses Jmol's decoding, not standard XML decoding (of &xxx;)
    int[] nexta = new int[1];
    int pt = setNext(data, what, nexta, 1);
    if (pt < 2 || (pt = setNext(data, "\"", nexta, 0)) < 2)
      return "";
    int pt1 = setNext(data, "\"", nexta, -1);
    return (pt1 <= 0 ? "" : data.substring(pt, pt1));
  }

  public P3 getXmlPoint(String data, String key) {
    String spt = getXmlAttrib(data, key).replace('(', '{').replace(')', '}');
    Object value = Escape.uP(spt);
    if (value instanceof P3)
      return (P3) value;
    return new P3();
  }

  /**
   * shift pointer to a new tag or field contents
   * 
   * @param data
   *          string of data
   * @param what
   *          tag or field name
   * @param next
   *          current pointer into data
   * @param offset
   *          offset past end of "what" for pointer
   * @return pointer to data
   */
  private static int setNext(String data, String what, int[] next, int offset) {
    int ipt = next[0];
    if (ipt < 0 || (ipt = data.indexOf(what, next[0])) < 0)
      return -1;
    ipt += what.length();
    next[0] = ipt + offset;
    if (offset > 0 && ipt < data.length() && data.charAt(ipt) != '=')
      return setNext(data, what, next, offset);
    return next[0];
  }

  private void skipTo(String key) throws Exception {
    if (line == null)
      line = br.readLine();
    while (line != null && line.indexOf(key) < 0)
      line = br.readLine();
  }

  public boolean isNext(String name) throws Exception {
    if (line == null || line.indexOf("</") >= 0 && line.indexOf("</") == line.indexOf("<"))
      line = br.readLine();
    return (line.indexOf("<" + name) >= 0);
  }

}
