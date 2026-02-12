package fr.orsay.lri.varna.models.rna;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.rna.ModeleBackboneElement.BackboneType;
import fr.orsay.lri.varna.utils.XMLUtils;

public class ModeleBackbone  implements Serializable{

	/**
	 * 
	 */
	private Hashtable<Integer,ModeleBackboneElement> elems = new Hashtable<Integer,ModeleBackboneElement>();
	
	private static final long serialVersionUID = -614968737102943216L;

	
	
	public static String XML_ELEMENT_NAME = "backbone";
	
	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		for (ModeleBackboneElement bck:elems.values())
		{
			bck.toXML(hd);
		}
		hd.endElement("","",XML_ELEMENT_NAME);
		atts.clear();
	}

	public void addElement(ModeleBackboneElement mbe)
	{
		elems.put(mbe.getIndex(),mbe);
	}

	 public BackboneType getTypeBefore(int indexBase)
	 {
		 return getTypeAfter(indexBase-1);
	 }
	
	 public BackboneType getTypeAfter(int indexBase)
	 {
		 if (elems.containsKey(indexBase))
			 return elems.get(indexBase).getType();
		 else
			 return BackboneType.SOLID_TYPE;
	 }

	 public Color getColorBefore(int indexBase, Color defCol)
	 {
		 return getColorAfter(indexBase-1,defCol);
	 }
	
	 public Color getColorAfter(int indexBase, Color defCol)
	 {
		 if (elems.containsKey(indexBase))
		 {
			 Color c = elems.get(indexBase).getColor();
			 if (c != null)
				 return c;
		 }
		 return defCol;
	 }
	 
}
