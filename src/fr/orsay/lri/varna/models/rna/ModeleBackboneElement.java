package fr.orsay.lri.varna.models.rna;

import java.awt.Color;
import java.io.Serializable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.utils.XMLUtils;

public class ModeleBackboneElement implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -614968737102943216L;

	public ModeleBackboneElement(int index, BackboneType t)
	{
		_index=index;
		if (t==BackboneType.CUSTOM_COLOR)
		{
			throw new IllegalArgumentException("Error: Missing Color while constructing Backbone");
		}
		_type=t;
	}
	
	public ModeleBackboneElement(int index, Color c)
	{
		_index=index;
		_type=BackboneType.CUSTOM_COLOR;
		_color = c;
	}
	
	public enum BackboneType{
		SOLID_TYPE ("solid"),
		DISCONTINUOUS_TYPE ("discontinuous"),
		MISSING_PART_TYPE ("missing"),
		CUSTOM_COLOR ("custom");
		
		private String label;
		
		BackboneType(String s)
		{
			label = s;
		}
		
		public String getLabel()
		{
			return label;
		}
		
		
		public static BackboneType getType(String lbl)
		{
			BackboneType[] vals = BackboneType.values();
			for(int i=0;i<vals.length;i++)
			{
				if (vals[i].equals(lbl))
					return vals[i];
			}
			return null;
		}
		
	};
	
	private BackboneType _type;
	private Color _color = null;
	private int _index;

	
	public BackboneType getType()
	{
		return _type;
	}
	
	public int getIndex()
	{
		return _index;
	}
	
	public Color getColor()
	{
		return _color;
	}
	
	public static String XML_ELEMENT_NAME = "BackboneElement";
	public static String XML_VAR_INDEX_NAME = "index";
	public static String XML_VAR_TYPE_NAME = "type";
	public static String XML_VAR_COLOR_NAME = "color";	
	
	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_INDEX_NAME,"CDATA",""+_index);
		atts.addAttribute("","",XML_VAR_TYPE_NAME,"CDATA",""+_type.getLabel());
		if(_type==BackboneType.CUSTOM_COLOR){
			atts.addAttribute("","",XML_VAR_COLOR_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_color));	
		}
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		hd.endElement("","",XML_ELEMENT_NAME);
	}

}
