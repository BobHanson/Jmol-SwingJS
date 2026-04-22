package fr.orsay.lri.varna.models.rna;

import java.awt.Color;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Hashtable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.rna.ModeleBackboneElement.BackboneType;

public class ModeleBackbone implements Serializable {

  private Hashtable<Integer, ModeleBackboneElement> elems = new Hashtable<Integer, ModeleBackboneElement>();

  BitSet bsStrandEnds = new BitSet();

  private static final long serialVersionUID = -614968737102943216L;

  public void addElement(ModeleBackboneElement mbe) {
    int i = mbe.getIndex();
    if (mbe.getType() == BackboneType.DISCONTINUOUS_TYPE)
      bsStrandEnds.set(i);
    elems.put(Integer.valueOf(i), mbe);
  }
  
  public boolean isStrandEnd(int mbeIndex) {
    return (mbeIndex >= 0 && bsStrandEnds.get(mbeIndex));
  }

  public BackboneType getTypeBefore(int indexBase) {
    return getTypeAfter(indexBase - 1);
  }

  public BackboneType getTypeAfter(int indexBase) {
    ModeleBackboneElement e = elems.get(Integer.valueOf(indexBase));
    BackboneType b = (e == null ? BackboneType.SOLID_TYPE : e.getType());
    if (b == BackboneType.DISCONTINUOUS_TYPE)
      System.out.println("MB?? " + indexBase + " " + e + " " +  isStrandEnd(indexBase));
    return b;
  }

  public Color getColorBefore(int indexBase, Color defCol) {
    return getColorAfter(indexBase - 1, defCol);
  }

  public Color getColorAfter(int indexBase, Color defCol) {
    ModeleBackboneElement e = elems.get(Integer.valueOf(indexBase));
    Color c;
    return (e != null && (c = e.getColor()) != null ? c : defCol);
  }


  public static String XML_ELEMENT_NAME = "backbone";

  public void toXML(TransformerHandler hd) throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    hd.startElement("", "", XML_ELEMENT_NAME, atts);
    for (ModeleBackboneElement bck : elems.values()) {
      bck.toXML(hd);
    }
    hd.endElement("", "", XML_ELEMENT_NAME);
    atts.clear();
  }

  @Override
  public String toString() {
    return elems.toString();
  }
}
