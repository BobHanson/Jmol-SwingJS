/* Copyright (c) 2007-2008 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.source;

import java.io.BufferedReader;
import java.util.Hashtable;

import javajs.util.SB;



public class XMLParser {

  /*
   * A simple very light-weight XML reader
   * See AnIMLSource.java and CMLSource.java for implementation.
   *
   *  Bob Hanson hansonr@stolaf.edu  8/22/2008
   *
   *
   */

  private XmlEvent thisEvent = new XmlEvent(TAG_NONE);
  private DataBuffer buffer;

  public final static int TAG_NONE = 0;
  public final static int START_ELEMENT = 1;
  public final static int END_ELEMENT = 2;
  public final static int START_END_ELEMENT = 3;
  public final static int CHARACTERS = 4;
  public final static int COMMENT = 6;
  public final static int EOF = 8;


  public XMLParser(BufferedReader br) {
    buffer = new DataBuffer(br);
  }

  public String getBufferData() {
    return (buffer == null ? null : buffer.data.toString().substring(0, buffer.ptr));
  }

  /**
   * for value without surrounding tag
   *
   * @return value
   * @throws Exception
   */
  public String thisValue() throws Exception {
    return buffer.nextEvent().toString().trim();
  }

  /**
   * for &lt;xxxx&gt; value &lt;/xxxx&gt;
   *
   * @return value
   * @throws Exception
   */
  public String qualifiedValue() throws Exception {
    buffer.nextTag();
    String value = buffer.nextEvent().toString().trim();
    buffer.nextTag();
    return value;
  }

  public int peek() throws Exception {
    thisEvent = buffer.peek();
    return thisEvent.getEventType();
  }

  public boolean hasNext() {
    return buffer.hasNext();
  }

  public void nextTag() throws Exception {
    while ((thisEvent = buffer.nextTag()).eventType == XMLParser.COMMENT) {
    }
  }

  public int nextEvent() throws Exception {
    thisEvent = buffer.nextEvent();
    return thisEvent.getEventType();
  }

  public void nextStartTag() throws Exception {
    thisEvent = buffer.nextTag();
    while (!thisEvent.isStartElement())
      thisEvent = buffer.nextTag();
  }

  public String getTagName() {
    return thisEvent.getTagName();
  }

  public int getTagType() {
    return thisEvent.getTagType();
  }

  public String getEndTag() {
    return thisEvent.getTagName();
  }

  public String nextValue() throws Exception {
    buffer.nextTag();
    return buffer.nextEvent().toString().trim();
  }

  public String getAttributeList() {
    return thisEvent.toString().toLowerCase();
  }

  public String getAttrValueLC(String key) {
    return getAttrValue(key).toLowerCase();
  }

  public String getAttrValue(String name) {
    String a = thisEvent.getAttributeByName(name);
    return (a == null ? "" : a);
  }

  public String getCharacters() throws Exception {
    SB sb = new SB();
    thisEvent = buffer.peek();
    int eventType = thisEvent.getEventType();

    while (eventType != CHARACTERS)
      thisEvent = buffer.nextEvent();
    while (eventType == CHARACTERS) {
      thisEvent = buffer.nextEvent();
      eventType = thisEvent.getEventType();
      if (eventType == CHARACTERS)
        sb.append(thisEvent.toString());
    }
    return sb.toString();
  }

  private class DataBuffer extends DataString {

    DataBuffer(BufferedReader br) {
      reader = br;
    }

    boolean hasNext() {
      if (ptr == ptEnd)
        try {
          readLine();
        } catch (Exception e) {
          return false;
        }
      return ptr < ptEnd;
    }

    @Override
    public boolean readLine() throws Exception {
      String s = reader.readLine();
      if (s == null) {
        return false;
      }
      data.append(s + "\n");
      ptEnd = data.length();
      return true;
    }

    XmlEvent peek() throws Exception {
      if (ptEnd - ptr < 2)
        try {
          readLine();
        } catch (Exception e) {
          return new XmlEvent(EOF);
        }
      int pt0 = ptr;
      XmlEvent e = new XmlEvent(this);
      ptr = pt0;
      return e;
    }

    XmlEvent nextTag() throws Exception {
      flush();
      skipTo('<', false);
      XmlEvent e = new XmlEvent(this);
      return e;
    }

    XmlEvent nextEvent() throws Exception {
      flush();
      // cursor is always left after the last element
      return new XmlEvent(this);
    }

  }

  private class DataString {

    SB data;
    protected BufferedReader reader;
    int ptr;
    int ptEnd;

    DataString() {
      this.data = new SB();
    }

    DataString(SB data) {
      this.data = data;
      ptEnd = data.length();
    }

    int getNCharactersRemaining() {
      return ptEnd - ptr;
    }

    protected void flush() {
      if (data.length() < 1000 || ptEnd - ptr > 100)
        return;
      data = new SB().append(data.substring(ptr));
      //System.out.println(data);
      ptr = 0;
      ptEnd = data.length();
      //System.out.println("flush " + ptEnd);
    }

    String substring(int i, int j) {
      return data.toString().substring(i, j);
    }

    int skipOver(char c, boolean inQuotes) throws Exception {
      if (skipTo(c, inQuotes) > 0 && ptr != ptEnd) {
        ptr++;
      }
      return ptr;
    }

    int skipTo(char toWhat, boolean inQuotes) throws Exception {
      if (data == null)
        return -1;
      char ch;
      if (ptr == ptEnd) {
        if (reader == null)
          return -1;
        readLine();
      }
      int ptEnd1 = ptEnd - 1;
      while (ptr < ptEnd && (ch = data.charAt(ptr)) != toWhat) {
        if (inQuotes && ch == '\\' && ptr < ptEnd1) {
          // must escape \" by skipping the quote and
          // must escape \\" by skipping the second \
          if ((ch = data.charAt(ptr + 1)) == '"' || ch == '\\')
            ptr++;
        } else if (ch == '"') {
          ptr++;
          if (skipTo('"', true) < 0)
            return -1;
        }
        if (++ptr == ptEnd) {
          if (reader == null)
            return -1;
          readLine();
        }
      }
      return ptr;
    }

    public boolean readLine() throws Exception {
      return false;
    }
  }

  private class XmlEvent {

    int eventType = TAG_NONE;
    private int ptr = 0;
    private Tag tag;
    private String data;

    @Override
    public String toString() {
      return (data != null ? data : tag != null ? tag.text : null);
    }

    XmlEvent(int eventType) {
      this.eventType = eventType;
    }

    XmlEvent(DataBuffer b) throws Exception {
      ptr = b.ptr;
      int n = b.getNCharactersRemaining();
      eventType = (n == 0 ? EOF : n == 1
          || b.data.charAt(b.ptr) != '<' ? CHARACTERS
          : b.data.charAt(b.ptr + 1) != '/' ? START_ELEMENT : END_ELEMENT);
      if (eventType == EOF)
        return;
      if (eventType == CHARACTERS) {
        b.skipTo('<', false);
        data = b.data.toString().substring(ptr, b.ptr);
      } else {
        b.skipOver('>', false);
        String s = b.data.toString().substring(ptr, b.ptr);
        if (s.startsWith("<!--"))
          eventType = COMMENT;
        //System.out.println("new tag: " + s);
        tag = new Tag(s);
      }
    }

    public int getEventType() {
      return eventType;
    }

    boolean isStartElement() {
      return (eventType & START_ELEMENT) != 0;
    }

    public String getTagName() {
      return (tag == null ? null : tag.getName());
    }

    public int getTagType() {
      return (tag == null ? TAG_NONE : tag.tagType);
    }

    public String getAttributeByName(String name) {
      return (tag == null ? null : tag.getAttributeByName(name));
    }

}

  class Tag {
    int tagType;
    String name;
    String text;
    private Hashtable<String, String> attributes;

    Tag() {
      //System.out.println("tag");
    }

    Tag(String fulltag) {
      text = fulltag;
      tagType = (fulltag.startsWith("<!--") ? COMMENT 
          : fulltag.charAt(1) == '/' ? END_ELEMENT : fulltag
          .charAt(fulltag.length() - 2) == '/' ? START_END_ELEMENT
          : START_ELEMENT);
    }

    String getName() {
      if (name != null)
        return name;
      int ptTemp = (tagType == END_ELEMENT ? 2 : 1);
      int n = text.length() - (tagType == START_END_ELEMENT ? 2 : 1);
      while (ptTemp < n && Character.isWhitespace(text.charAt(ptTemp)))
        ptTemp++;
      int pt0 = ptTemp;
      while (ptTemp < n && !Character.isWhitespace(text.charAt(ptTemp)))
        ptTemp++;
      return name = text.substring(pt0, ptTemp).toLowerCase().trim();
    }

    String getAttributeByName(String attrName) {
      if (attributes == null)
        getAttributes();
      return attributes.get(attrName.toLowerCase());
    }

    private void getAttributes() {
      attributes = new Hashtable<String, String>();
      DataString d = new DataString(
          new SB().append(text));
      try {
        if (d.skipTo(' ', false) < 0)
          return;
        int pt0;
        while ((pt0 = ++d.ptr) >= 0) {
          if (d.skipTo('=', false) < 0)
            return;
          String name = d.substring(pt0, d.ptr).trim().toLowerCase();
          d.skipTo('"', false);
          pt0 = ++d.ptr;
          d.skipTo('"', true);
          String attr = d.substring(pt0, d.ptr);
          attributes.put(name, attr);
          int pt1 = name.indexOf(":");
          if (pt1 >= 0) {
            name = name.substring(pt1).trim();
            attributes.put(name, attr);
          }

        }
      } catch (Exception e) {
        // not relavent
      }
    }

  }

  public boolean requiresEndTag() {
    int tagType = thisEvent.getTagType(); 
    return  tagType != START_END_ELEMENT && tagType != COMMENT;
  }
}
