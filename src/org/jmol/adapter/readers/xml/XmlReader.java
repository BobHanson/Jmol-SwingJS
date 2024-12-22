/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-02 11:48:43 -0500 (Wed, 02 Aug 2006) $
 * $Revision: 5364 $
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
package org.jmol.adapter.readers.xml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Resolver;
import org.jmol.api.Interface;
import org.jmol.util.Logger;

import javajs.util.Rdr;
import javajs.util.SB;

/**
 * A generic XML reader template -- by itself, does nothing.
 * 
 * The actual readers are XmlCmlReader, XmlMolproReader (which is an extension
 * of XmlCmlReader), XmlChem3dReader, and XmlOdysseyReader.
 * 
 * 
 * XmlReader takes all XML streams, whether from a file reader or from DOM.
 * 
 * This class functions as a resolver, since it: (1) identifying the specific
 * strain of XML to be handled, and (2) passing the responsibility on to the
 * correct format-specific XML readers. There are parallel entry points and
 * handler methods for reader and DOM. Each format-specific XML reader then
 * assigns its own handler to manage the parsing of elements.
 * 
 * In addition, this class handles generic XML tag parsing.
 * 
 * XmlHandler extends DefaultHandler is the generic interface to both reader and
 * DOM element parsing.
 * 
 * XmlCmlReader extends XmlReader
 * 
 * XmlMolproReader extends XmlCmlReader. If you feel like expanding on that,
 * feel free.
 * 
 * XmlChem3dReader extends XmlReader. That one is simple; no need to expand on
 * it at this time.
 * 
 * XmlOdysseyReader extends XmlReader. That one is simple; no need to expand on
 * it at this time.
 * 
 * Note that the tag processing routines are shared between SAX and DOM
 * processors. This means that attributes must be transformed from either
 * Attributes (SAX) or JSObjects (DOM) to Hashtable name:value pairs. This is
 * taken care of in JmolXmlHandler for all readers.
 * 
 * TODO 27/8/06:
 * 
 * Several aspects of CifReader are NOT YET implemented here. These include
 * loading a specific model when there are several, applying the symmetry, and
 * loading fractional coordinates. [DONE for CML reader 2/2007 RMH]
 * 
 * 
 * Test files:
 * 
 * molpro: vib.xml odyssey: water.xodydata cml: a wide variety of files in
 * data-files.
 * 
 * -Bob Hanson
 * 
 */

abstract public class XmlReader extends AtomSetCollectionReader {

  protected Atom thisAtom;
  //protected String[] domAttributes;
  public XmlReader parent = this; // was much more complicated. Now much simpler. 
  // No longer: XmlReader itself; to be assigned by the subReader
  public Map<String, String> atts;

  /////////////// file reader option //////////////

  @Override
  public void initializeReader() throws Exception {
    atts = new Hashtable<String, String>();
    setMyError(parseXML());
    continuing = false;
  }

  private void setMyError(String err) {
    if (err != null
        && (asc == null || asc.errorMessage == null)) {
      asc = new AtomSetCollection("xml", this, null, null);
      asc.errorMessage = err;
    }
  }

  private String parseXML() {
    org.xml.sax.XMLReader saxReader = null;
    /**
     * @j2sNative
     * 
     * 
     * 
     */
    {
      try {
        javax.xml.parsers.SAXParserFactory spf = javax.xml.parsers.SAXParserFactory
            .newInstance();
        spf.setNamespaceAware(true);
        javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
        saxReader = saxParser.getXMLReader();

        // otherwise, DTD UTI is treated as a URL, retrieved, and scanned.
        // see https://stackoverflow.com/questions/10257576/how-to-ignore-inline-dtd-when-parsing-xml-file-in-java
        saxReader.setFeature("http://xml.org/sax/features/validation", false);
        saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        
        if (debugging)
          Logger.debug("Using JAXP/SAX XML parser.");
      } catch (Exception e) {
        if (debugging)
          Logger.debug("Could not instantiate JAXP/SAX XML reader: " + e.getMessage());
      }
      if (saxReader == null)
        return "No XML reader found";
    }
    return selectReaderAndGo(saxReader);
  }

  private XmlReader thisReader = null;

  private String selectReaderAndGo(Object saxReader) {
    String className = null;
    int pt = readerName.indexOf("(");
    String name = (pt < 0 ? readerName : readerName.substring(0, pt));
    className = Resolver.getReaderClassBase(name);
    if (className.equals(getClass().getName())) {
      thisReader = parent = this;
    } else {
      asc = new AtomSetCollection(readerName, this, null, null);
      if ((thisReader = (XmlReader) getInterface(className)) == null)
        return "File reader was not found: " + className;
    }
    try {
      thisReader.processXml(this, saxReader);
    } catch (Exception e) {
      e.printStackTrace();
      return "Error reading XML: " + (e.getMessage());
      
    }
    return null;
  }

  /**
   * 
   * @param parent
   * @param saxReader
   * @throws Exception
   */
  protected void processXml(XmlReader parent, Object saxReader)
      throws Exception {
    processXml2(parent, saxReader);
  }

  protected void processXml2(XmlReader parent, Object saxReader)
      throws Exception {
    this.parent = parent;
    if (parent != this) {
      asc = parent.asc;
      reader = parent.reader;
      atts = parent.atts;
    }
    BufferedReader rdr = previewXML(reader);
    if (saxReader == null) {
      //domAttributes = getDOMAttributes();
      attribs = new Object[1];
      //attArgs = new Object[1];
      domObj = new Object[1];
      Object o = "";
      byte[] data = null;
      /**
       * 
       * @j2sNative
       * 
       *            o = rdr.lock.lock; 
       *            if (o && o.$in) {
       *              data = o.$in.buf;
       *            } else if ((o=rdr.$in.str) != null) {
       *            } else if (rdr.$in.$in.$in.fd) {
       *              // may need to adjust this;
       *              o = rdr.$in.$in;
       *              data = o.$in.fd._file.秘bytes; 
       *            } else {
       *              data = (o=rdr.$in.$in).$in.buf;
       *            }
       */
      {
      }
      if (o instanceof BufferedInputStream)
        o = new String(data, "utf-8");
      boolean isjs = false;
      /**
       * 
       * @j2sNative
       * 
       *            isjs = true;
       * 
       */
      {
        walkDOMTree();
      }
      if (isjs) {
        domObj[0] = createDomNodeJS("xmlReader", o);
        walkDOMTree();
        createDomNodeJS("xmlReader", null);

      }
    } else {
      ((XmlHandler) Interface.getOption("adapter.readers.xml.XmlHandler", vwr,
          "file")).parseXML(this, saxReader, rdr);
    }
  }
  
  /**
   * An opportunity to fix XML (nmrML issues)
   * @param reader
   * @return reader or a repaired reader
   * @throws IOException 
   */
  protected BufferedReader previewXML(BufferedReader reader) throws IOException {
    return reader;
  }

  /**
   * totally untested, probably useless
   * 
   * @param id  
   * @param data 
   * @return dom object 
   */
  Object createDomNodeJS(String id, Object data) {
    // no doubt there is a more efficient way to do this.
    // Firefox, at least, does not recognize "/>" in HTML blocks
    // that are added this way.
    
    Object d = null;
    /**
     * note that there is no need to actually load it into the document
     * 
     * @j2sNative
     * 
      if (!data)
        return null;
      if (data.indexOf("<?") == 0)
        data = data.substring(data.indexOf("<", 1));
      if (data.indexOf("/>") >= 0) {
        var D = data.split("/>");
        for (var i = D.length - 1; --i >= 0;) {
          var s = D[i];
          var pt = s.lastIndexOf("<") + 1;
          var pt2 = pt;
          var len = s.length;
          var name = "";
          while (++pt2 < len) {
            if (" \t\n\r".indexOf(s.charAt(pt2))>= 0) {
              var name = s.substring(pt, pt2);
              D[i] = s + "></"+name+">";
              break;
            }     
          }
        }
        data = D.join('');
      }
      d = document.createElement("_xml");
      d.innerHTML = data;
     * 
     */
    {
      // only called by j2s
    }
    return d;
  }
  
  @Override
  public void applySymmetryAndSetTrajectory() {
    try {
      if (parent == null || parent == this)
        applySymTrajASCR();
      else
        parent.applySymmetryAndSetTrajectory();
    } catch (Exception e) {
      Logger.error("applySymmetry failed: " + e);
    }
  }

  /////////////// DOM option //////////////

  @Override
  protected void processDOM(Object DOMNode) {
    domObj = new Object[] { DOMNode };
    setMyError(selectReaderAndGo(null));
  }

//  protected String[] getDOMAttributes() {
//    // different subclasses will implement this differently
//    return new String[] { "id" };
//  }

  ////////////////////////////////////////////////////////////////

  /**
   * 
   * @param localName
   * @param nodeName TODO
   */
  protected void processStartElement(String localName, String nodeName) {
    /* 
     * specific to each xml reader
     */
  }

  /*
   *  keepChars is used to signal 
   *  that characters between end tags should be kept
   *  
   */

  protected boolean keepChars;
  protected SB chars = SB.newN(2000);

  protected void setKeepChars(boolean TF) {
    keepChars = TF;
    chars.setLength(0);
  }

  /**
   * 
   * @param localName
   */
  void processEndElement(String localName) {
    /* 
     * specific to each xml reader
     */
  }

  //////////////////// DOM or JavaScript parsing /////////////////

  // walk DOM tree given by JSObject. For every element, call
  // startElement with the appropriate strings etc., and then
  // endElement when the element is closed.

  private Object[] domObj = new Object[1];
  private Object[] attribs;
//  private Object[] attArgs;
//  private Object[] nullObj = new Object[0];

  private void walkDOMTree() {
    String localName;
    /**
     * @j2sNative
     * 
     * localName = "nodeName";
     * 
     */
    {
      localName = "localName";
    }
    String nodeName = ((String) jsObjectGetMember(domObj, localName));
    localName = fixLocal(nodeName);
    if (localName == null)
      return;
    if (localName.equals("#text")) {
      if (keepChars)
        chars.append((String) jsObjectGetMember(domObj, "data"));
      return;
    }
    localName = localName.toLowerCase();
    nodeName = nodeName.toLowerCase();
    attribs[0] = jsObjectGetMember(domObj, "attributes");
    getDOMAttributesA(attribs);
    processStartElement(localName, nodeName);
    boolean haveChildren = false;
    /**
     * @j2sNative
     * 
     *            haveChildren = this.domObj[0].hasChildNodes;
     * 
     */
    {
//      haveChildren = ((Boolean) jsObjectCall(domObj, "hasChildNodes", null))
//          .booleanValue();
    }
    if (haveChildren) {
      Object nextNode = jsObjectGetMember(domObj, "firstChild");
      while (nextNode != null) {
        domObj[0] = nextNode;
        walkDOMTree();
        domObj[0] = nextNode;
        nextNode = jsObjectGetMember(domObj, "nextSibling");
      }
    }
    processEndElement(localName);
  }

  private String fixLocal(String name) {
    /**
     * @j2sNative
     * 
     *            var pt = (name== null ? -1 : name.indexOf(":")); return (pt >=
     *            0 ? name.substring(pt+1) : name);
     */
    {
      return name;
    }
  }

  private class NVPair {
    String name;
    String value;
  }
  
  @SuppressWarnings("unused")
  private void getDOMAttributesA(Object[] attributes) {

    atts.clear();
    if (attributes == null)
      return;

    NVPair[] nodes = null;

    /**
     * @j2sNative
     * 
     * 
     *            nodes = attributes[0];
     * 
     */
    {
//
//      // Java only -- no longer loading only specific values
//
//      Number N = (Number) jsObjectGetMember(attributes, "length");
//      int n = (N == null ? 0 : N.intValue());
//      for (int i = n; --i >= 0;) {
//        attArgs[0] = Integer.valueOf(i);
//        attArgs[0] = jsObjectCall(attributes, "item", attArgs);
//        if (attArgs[0] != null) {
//          String attValue = (String) jsObjectGetMember(attArgs, "value");
//          if (attValue != null)
//            atts.put(
//                ((String) jsObjectGetMember(attArgs, "name")).toLowerCase(),
//                attValue);
//        }
//      }
      if (true)
        return;
    }
    // JavaScript only
    for (int i = nodes.length; --i >= 0;)
      atts.put(fixLocal(nodes[i].name).toLowerCase(), nodes[i].value);

  }
  
  

//  /**
//   * @j2sIgnore
//   * 
//   * @param jsObject
//   * @param method
//   * @param args
//   * @return object
//   */
//  private Object jsObjectCall(Object[] jsObject, String method, Object[] args) {
//    {
//    return parent.vwr.apiPlatform.getJsObjectInfo(jsObject, method,
//        args == null ? nullObj : args);
//    }
//  }
//
  /**
   * @param jsObject  
   * @param name 
   * @return an object
   */
  private Object jsObjectGetMember(Object[] jsObject, String name) {
    /**
     * @j2sNative
     * 
     * return jsObject[0][name]; 
     * 
     */
    {
    return null;//parent.vwr.apiPlatform.getJsObjectInfo(jsObject, name, null);
    }
  }

  public void endDocument() {
    // CML reader uses this
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (thisReader != null && thisReader != this)
      thisReader.finalizeSubclassReader();
    thisReader = null;
  }

}
