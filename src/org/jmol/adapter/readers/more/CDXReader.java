package org.jmol.adapter.readers.more;

import org.jmol.adapter.readers.xml.XmlCdxReader;
import org.jmol.adapter.readers.xml.XmlReader;
import org.jmol.adapter.writers.CDXMLWriter;

import javajs.util.Rdr;

/**
 * 
 * A reader for ChemDraw binary CDX files. See
 * 
 * https://iupac.github.io/IUPAC-FAIRSpec/cdx_sdk
 * 
 * formerly https://www.cambridgesoft.com/services/documentation/sdk/chemdraw/cdx/
 * 
 * CambridgeSoft did a fabulous job on the specification.
 * 
 * This reader extends CDXMLWriter. It simply passes the CDX binary to
 * CDXMLWriter, which returns the XML. The XML is then passed through the CDX
 * files on the fly to CDXML and then processes those using the
 * XMLChemDrawReader.
 * 
 * Note that CDXMLWriter.fromCDX(), though public, is not exposed generally, as it is only
 * a skeleton class intended to do just enough to be able to run the 
 * structure through SMILES, InChI and 2D or 3D MOL generation. 
 * 
 * Generic Nicknames (e.g. "R" and "X") will become "Xx" -- unknown atom. 
 * 
 * Multiple Attachments will become 0.5 partial bonds.
 * 
 * @author Bob Hanson
 * 
 * @version 1.0
 */

public class CDXReader extends XmlCdxReader {

  @Override
  protected void processXml2(XmlReader parent, Object saxReader) throws Exception {
    isCDX = true;
    // convert binary document to reader!
    // parent is null here because we started with the class already
    String xml = CDXMLWriter.fromCDX(binaryDoc);
    reader = Rdr.getBR(xml);
    super.processXml2(this, saxReader);
    binaryDoc = null;
  }
  
  
}
